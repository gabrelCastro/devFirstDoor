# Dev First Door

Agregador de vagas remotas de **estágio e júnior em Java**. Um backend Spring Boot coleta vagas de
várias fontes, filtra só as que interessam, descarta repetidas e expõe uma API; um frontend React
lista as vagas com abas, busca e paginação.

- `backend/`: Spring Boot 4, Java 21, Maven wrapper. Crawlers em `crawler/<fonte>/`.
- `frontend/`: React + Vite (servido por nginx no Docker, que repassa `/api/` ao backend).

## Como subir

Pré-requisito: Docker com Compose.

```bash
cp .env.example .env   # ajuste a senha do banco e, se quiser, LINKEDIN_ENABLED
make up                # docker compose up --build
```

Sobe três containers: PostgreSQL 16, backend (perfil `docker`) e frontend.

- Frontend: <http://localhost:3000> (`FRONTEND_PORT`)
- API: <http://localhost:8080/api/vagas> (`BACKEND_PORT`)

Outros alvos do `Makefile`: `make down`, `make logs`, `make build`, `make test`.

Variáveis do `.env`:

| Variável | Uso |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Banco PostgreSQL do Compose |
| `BACKEND_PORT`, `FRONTEND_PORT` | Portas expostas no host (8080 e 3000) |
| `LINKEDIN_ENABLED` | Liga o crawler do LinkedIn (padrão `false`; veja abaixo) |

Assim que o backend sobe, uma coleta roda em segundo plano (`ColetaInicialRunner`), então o banco
já fica populado sem passo manual. Depois disso a coleta se repete a cada `app.crawler.intervalo`
(padrão 6h). Também dá para disparar uma coleta na hora com `POST /api/vagas/coletar`; se já houver
uma coleta rodando, a nova é ignorada.

Fora do Docker, o backend usa H2 em arquivo (`backend/data/`) e o frontend pode rodar com
`npm run dev`, que repassa `/api` para `http://localhost:8080` (ou `VITE_API_PROXY_TARGET`).

## Fontes

Todas as fontes, exceto o LinkedIn, consultam o `robots.txt` do site antes de coletar e abortam se
o caminho não for permitido. As requisições têm uma pausa entre si (`request-delay-ms`).

| Fonte | Como coleta |
|---|---|
| **Gupy** | API pública do portal (`employability-portal.gupy.io/api/v1/jobs`), buscando por uma lista de termos (`termos-busca`), até `max-paginas-por-termo` páginas cada. Estágio vem do tipo de contrato; júnior, de contrato efetivo com "júnior/jr" no título. Remoto pelo campo `isRemoteWork`. Quando o título não cita Java, lê a página da vaga atrás dos requisitos (resultado guardado em memória para não reler). |
| **ProgramaThor** | HTML do quadro de vagas, com os filtros do próprio site: estágio remoto e júnior remoto. Java pelas tags ou pelo título. |
| **RemoteOK** | API pública (`/api` e os feeds de dev, internship e junior). Nível e área pelo título; Java pelas tags ou pelo título. Vagas de outros países que não aceitam o Brasil vão para a aba "gringa". |
| **Greenhouse** | Job Board API pública (`boards-api.greenhouse.io/v1/boards/{empresa}/jobs?content=true`), uma chamada por empresa listada em `app.crawler.greenhouse.empresas`. Remoto pelo local; Java pelo título ou conteúdo da vaga. |
| **Lever** | Postings API pública (`api.lever.co/v0/postings/{empresa}?mode=json`), uma chamada por empresa listada em `app.crawler.lever.empresas`. Remoto pelo `workplaceType` (ou pelo local, se não vier); Java pelo título ou descrição. |
| **LinkedIn** | Busca pública de vagas (a de um visitante deslogado), com filtro de remoto do próprio LinkedIn e termos que já trazem o nível. Cada vaga nova tem a página de detalhe lida para confirmar Java e a modalidade. **Desligado por padrão.** |

Greenhouse e Lever não têm busca entre empresas: é preciso listar os boards. Com a lista vazia
(o padrão), o crawler não faz nada, nem consulta o `robots.txt`.

### LinkedIn (`LINKEDIN_ENABLED`)

O `robots.txt` do LinkedIn proíbe os caminhos da busca de vagas, e este crawler **não o consulta**.
Por isso ele só é registrado com `LINKEDIN_ENABLED=true` (`app.crawler.linkedin.enabled`): é uma
escolha explícita, para **uso pessoal e em baixo volume**. Para reduzir as requisições:

- pausa de 3s + até 2s aleatórios entre requisições, e no máximo 3 páginas por termo;
- vagas já salvas e vagas já lidas sem Java não têm a descrição lida de novo;
- se o LinkedIn limitar o acesso (HTTP 429/999), a coleta daquela execução para em vez de insistir.

O LinkedIn bloqueia IPs que fazem requisições demais; use com moderação.

## Filtros

Uma vaga só é salva se passar por todos:

- **Estágio ou júnior**: pelo tipo de contrato ou filtro da fonte (Gupy, ProgramaThor) ou por
  palavras no título (`palavras-estagio`, `palavras-junior`).
- **Tecnologia**: título com alguma das `palavras-tech` da fonte (a ProgramaThor já é só de
  programação).
- **Java**: "java" como palavra no título, tags ou conteúdo da vaga, sem confundir com JavaScript
  (`util/LinguagemJava`).
- **Remoto**: Gupy, ProgramaThor, RemoteOK, Greenhouse e Lever só coletam vagas remotas. O LinkedIn
  pode trazer vagas cuja descrição não confirma o remoto; elas aparecem na aba "Todas", mas não na
  aba "Remoto".

Depois disso:

- **Deduplicação**: vagas com o mesmo título + empresa + fonte (hash) são descartadas; as que
  reaparecem só têm a data da última visita atualizada.
- **Expiração**: vagas que não reaparecem numa coleta bem-sucedida da fonte há mais de
  `app.crawler.dias-para-expirar` dias (padrão 7) são removidas. Uma fonte fora do ar não expira
  as vagas dela.

## API

- `GET /api/vagas?secao=TODAS|REMOTO|ESTAGIO&escopo=TODAS|NACIONAL|GRINGA&q=...&page=0&size=20`:
  lista paginada. `q` busca em título, empresa e local, sem diferenciar maiúsculas nem acentos.
- `GET /api/vagas/contagens` (mesmos filtros): total, fontes e contagem de cada aba.
- `POST /api/vagas/coletar`: roda uma coleta agora e devolve quantas vagas novas cada fonte salvou
  (`-1` para a fonte que falhou).

## Testes

```bash
make test                      # ou: cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```

Os testes não usam rede: os crawlers são testados com HTML/JSON de fixture e clientes falsos, e o
banco é H2 em memória. A coleta no startup e a agendada ficam desligadas em
`backend/src/test/resources/application.yml`.

## Configurações principais (`backend/src/main/resources/application.yml`)

| Propriedade | Padrão | Descrição |
|---|---|---|
| `app.crawler.run-on-startup` | `true` | Coleta assim que o backend sobe |
| `app.crawler.agendamento.enabled` | `true` | Liga a coleta periódica |
| `app.crawler.intervalo` | `6h` | Intervalo entre coletas agendadas (ex: `30m`, `PT6H`) |
| `app.crawler.dias-para-expirar` | `7` | Dias sem ser vista até a vaga ser removida |
| `app.crawler.gupy.page-size` | `100` | Vagas por página na API da Gupy |
| `app.crawler.gupy.max-paginas-por-termo` | `5` | Limite de páginas por termo de busca |
| `app.crawler.gupy.request-delay-ms` | `1000` | Pausa entre requisições à Gupy |
| `app.crawler.greenhouse.empresas` | `[]` | Boards do Greenhouse (`boards.greenhouse.io/{empresa}`) |
| `app.crawler.lever.empresas` | `[]` | Empresas do Lever (`jobs.lever.co/{empresa}`) |
| `app.crawler.linkedin.enabled` | `${LINKEDIN_ENABLED:false}` | Liga o crawler do LinkedIn |

Cada fonte tem mais opções nas classes `*CrawlerProperties` (termos de busca, palavras de
classificação, pausas, limites de páginas), todas sob `app.crawler.<fonte>`.

No perfil `docker`, o banco passa a ser o PostgreSQL configurado pelas variáveis `DB_*`.
