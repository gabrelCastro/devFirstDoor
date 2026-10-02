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
| `ADMIN_USER`, `ADMIN_PASSWORD` | Conta admin criada no banco ao subir; sem senha nenhum admin é criado |
| `CADASTRO_ABERTO` | Permite o cadastro público de contas comuns (padrão `true`) |

Assim que o backend sobe, uma coleta roda em segundo plano (`ColetaInicialRunner`), então o banco
já fica populado sem passo manual. Depois disso a coleta se repete a cada `app.crawler.intervalo`
(padrão 6h). A área administrativa permite disparar uma coleta na hora e acompanhar o andamento.

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
Por isso `LINKEDIN_ENABLED=true` (`app.crawler.linkedin.enabled`) é a chave-mestra: sem ela, nem o
painel administrativo pode ligar a fonte. É uma escolha explícita, para **uso pessoal e em baixo
volume**. Para reduzir as requisições:

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
  `app.crawler.dias-para-expirar` dias (padrão 7) recebem o status `EXPIRADA` e deixam a API
  pública. Se reaparecerem, voltam a `ATIVA`. Uma fonte fora do ar não expira as vagas dela.

## Contas e papéis

As contas ficam na tabela `usuario`, com a senha em hash BCrypt, e têm um de dois papéis:

- **`USUARIO`**: conta comum, criada pelo cadastro público. Acompanha as próprias candidaturas.
- **`ADMIN`**: faz o mesmo e também acessa a área administrativa e `/api/admin/**`.

### Login e sessão

O site entra por `POST /api/auth/login` e recebe um token de sessão aleatório, enviado depois como
`Authorization: Bearer <token>`. O banco guarda só o hash SHA-256 do token (tabela `sessao`); ele
vale 30 dias (`app.sessao.duracao`), é revogado no logout e as outras sessões da conta caem quando
a senha é trocada. O frontend guarda o token no `localStorage`, nunca a senha. Não há cookie, então
CSRF não se aplica.

O filtro de token relê a conta a cada requisição: rebaixar ou desativar alguém vale na hora, mesmo
para tokens já emitidos. HTTP Basic continua aceito, para scripts. O login não diferencia
maiúsculas. Uma conta comum que chama `/api/admin/**` recebe `403`; credencial ausente, errada,
expirada ou de conta desativada recebe `401`.

Ao subir, o backend cria ou sincroniza a conta de `ADMIN_USER`/`ADMIN_PASSWORD` (ativa, papel
`ADMIN` e a senha do `.env`). O `.env` é a fonte da verdade dessa conta: trocar a senha pela API ou
rebaixá-la no painel dura só até o próximo restart. Sem `ADMIN_PASSWORD` nenhuma conta é criada a
partir da configuração, mas contas `ADMIN` que já existem no banco continuam valendo.

| Método e rota | Acesso | Uso |
|---|---|---|
| `POST /api/conta` | público | Cadastra `usuario` (3 a 40 caracteres: letras, números, `.`, `_`, `-`) e `senha` (8 a 72); sempre com papel `USUARIO`. `409` se o login já existir, `403` com `CADASTRO_ABERTO=false`. |
| `POST /api/auth/login` | público | Troca `usuario` e `senha` por `{token, expiraEm, usuario}`. Falha sempre com `401` e a mesma mensagem. |
| `POST /api/auth/logout` | qualquer conta | Revoga o token usado na requisição. |
| `GET /api/conta` | qualquer conta | Devolve `id`, `usuario`, `papel`, `ativo` e `criadoEm`. |
| `PUT /api/conta/senha` | qualquer conta | Troca a senha (exige `senhaAtual` e `novaSenha`) e encerra as outras sessões. |

## Candidaturas

Em <http://localhost:3000/candidaturas> cada conta tem um quadro pessoal das vagas a que está se
candidatando. Na página de vagas, o botão **acompanhar** coloca a vaga no quadro (sem login, ele
abre o modal de entrar e salva a vaga logo depois); vagas já acompanhadas mostram a etapa atual.

- **Etapas**: interesse → candidatei → teste técnico → entrevista → oferta. O processo termina
  como contratado, reprovado, desisti ou sem retorno.
- **Quadro e lista**: no desktop, colunas com arrastar e soltar; no celular (ou pela aba
  *lista*), uma lista agrupada com seletor de etapa.
- **Painel de detalhes**: trilho de etapas, data do envio (preenchida ao mover para *candidatei*),
  próximo passo com data, anotações e linha do tempo das mudanças de etapa.
- **Candidaturas externas**: vagas que o site não coletou, com título, empresa, local e link.
- **Resumo**: em andamento, enviadas no mês, taxa de resposta, tempo médio até a primeira
  resposta, envios por semana e quantas *pedem atenção*: paradas em *candidatei* há 14 dias ou com
  o próximo passo vencido.
- **Exportação** em CSV (separado por `;`, com BOM, pronto para o Excel).

Cada candidatura guarda uma cópia de título, empresa, local e link: se a vaga expirar ou sair da
fonte, ela continua no quadro, marcada como *saiu do ar*. Cada conta vê só as próprias
candidaturas (a de outra conta responde `404`); o limite é de 500 por conta.

| Método e rota | Uso |
|---|---|
| `GET /api/candidaturas` | Todas as candidaturas da conta, com `vagaStatus` da vaga de origem. |
| `POST /api/candidaturas` | `{vagaId}` acompanha uma vaga coletada (`409` com o `id` existente se repetida); sem `vagaId`, cria uma externa com `titulo`, `empresa`, `local`, `link` (só `http(s)`), `etapa` e `dataCandidatura`. |
| `GET /api/candidaturas/{id}` | Candidatura e linha do tempo (`eventos`). |
| `PATCH /api/candidaturas/{id}` | Altera `etapa` e `dataCandidatura`; nas externas, também os dados da vaga. |
| `PUT /api/candidaturas/{id}/proximo-passo` | Define `texto` e `data` do próximo passo; nulos limpam. |
| `POST /api/candidaturas/{id}/notas` | Adiciona uma anotação à linha do tempo. |
| `DELETE /api/candidaturas/{id}/notas/{notaId}` | Apaga uma anotação. |
| `DELETE /api/candidaturas/{id}` | Exclui a candidatura e a linha do tempo. |
| `GET /api/candidaturas/resumo` | Números do resumo e envios das últimas 12 semanas. |
| `GET /api/candidaturas/exportar` | CSV com todas as candidaturas. |

## Área administrativa

O painel fica em <http://localhost:3000/admin> e aceita qualquer conta com papel `ADMIN`. Ele usa
a mesma sessão do site: quem entrou na página de vagas com uma conta admin já entra direto.

Para criar o primeiro admin, defina no `.env` e recrie o backend:

```dotenv
ADMIN_USER=admin
ADMIN_PASSWORD=uma_senha_forte
```

`ADMIN_USER` tem o padrão `admin`, mas **não existe senha padrão**. Depois disso, outros admins
podem ser promovidos na tela **Usuários**. A API pública de vagas não exige login.

### Telas

- **Painel**: mostra saúde, progresso, última execução e próxima coleta de cada crawler. Permite
  coletar todas as fontes ou apenas uma e pausar ou retomar o agendamento. Os disparos rodam em
  segundo plano; uma coleta concorrente é recusada.
- **Histórico**: lista as execuções iniciais, agendadas e manuais, com duração, status e totais, e
  abre o resultado individual de cada fonte.
- **Configuração**: liga fontes, edita os termos da Gupy e do LinkedIn, os boards do Greenhouse e
  do Lever, o intervalo agendado, a expiração e as pausas do LinkedIn. Também testa boards antes de
  salvá-los e configura o Telegram. As alterações persistidas valem para a coleta seguinte.
- **Vagas**: filtra por status, fonte e texto; corrige nível e modalidade, oculta ou reativa vagas,
  reclassifica todas preservando correções manuais e resolve duplicatas entre fontes.
- **Descartes**: consulta os descartes dos últimos 30 dias por fonte, motivo ou texto e mostra a
  contagem por motivo.
- **Métricas**: resume vagas ativas por fonte, nível e modalidade, novas nos últimos 30 dias,
  exclusivas por fonte, tempo médio no ar e descartes dos últimos 7 dias.
- **Usuários**: lista as contas, promove ou rebaixa entre `USUARIO` e `ADMIN` e desativa ou
  reativa contas. A própria conta não pode ser alterada, então sempre resta um admin ativo.

### Notificações pelo Telegram

Na tela **Configuração**, informe o token do bot e o chat id, salve e use **enviar teste**. Só
depois ligue as notificações. O teste exige token e chat id salvos, mas não exige que as
notificações estejam ligadas. Quando ativas, elas avisam sobre vagas novas e sobre mudanças da
saúde de um crawler para `FALHA`/`ALERTA` ou de volta para `OK`.

O token salvo nunca é devolvido pela API: o painel informa apenas se ele está preenchido. Deixar o
campo do token sem alteração preserva o valor existente; para removê-lo, salve o campo vazio com
as notificações desligadas.

### API administrativa

Todos os endpoints abaixo exigem uma conta ativa com papel `ADMIN` (token de sessão ou HTTP Basic).

| Método e rota | Uso |
|---|---|
| `GET /api/admin/me` | Valida a credencial e devolve o usuário autenticado. |
| `GET /api/admin/crawlers` | Estado ao vivo, saúde e próxima coleta de todas as fontes. |
| `POST /api/admin/coletas` | Dispara todas as fontes em segundo plano (`202`; `409` se já houver coleta). |
| `POST /api/admin/coletas/{fonte}` | Dispara uma fonte (`202`, `404` se inexistente, `409` se desligada ou ocupada). |
| `POST /api/admin/agendamento/pausar` | Pausa as próximas coletas agendadas. |
| `POST /api/admin/agendamento/retomar` | Retoma as coletas agendadas. |
| `GET /api/admin/execucoes` | Histórico paginado, mais recente primeiro, com resultados por fonte. |
| `GET /api/admin/configuracao` | Lê a configuração persistida e os padrões ainda não substituídos. |
| `PUT /api/admin/configuracao` | Substitui a configuração editável; intervalo mínimo de 30 minutos. |
| `POST /api/admin/boards/testar` | Testa sem salvar um board; `ats` aceita `GREENHOUSE` ou `LEVER`, além de `empresa`. |
| `POST /api/admin/notificacoes/testar` | Envia a mensagem de teste do Telegram. |
| `GET /api/admin/vagas` | Lista paginada; aceita `status`, `fonte`, `busca`, `page` e `size`. |
| `PATCH /api/admin/vagas/{id}` | Altera `status`, `nivel` e/ou `remoto`. |
| `POST /api/admin/vagas/reclassificar` | Recalcula os campos derivados, preservando correções manuais. |
| `GET /api/admin/vagas/duplicatas` | Lista vagas ativas equivalentes encontradas em fontes diferentes. |
| `POST /api/admin/vagas/duplicatas/resolver` | Mantém `vagaMantidaId` e oculta as demais do grupo. |
| `GET /api/admin/descartes` | Lista paginada e contagens; aceita `fonte`, `motivo`, `busca`, `page` e `size`. |
| `GET /api/admin/metricas` | Devolve os agregados exibidos na tela de métricas. |
| `GET /api/admin/usuarios` | Lista paginada das contas, em ordem de login; nunca inclui o hash da senha. |
| `PATCH /api/admin/usuarios/{id}` | Altera `papel` e/ou `ativo` de outra conta (`400` para a própria). |

## API

- `GET /api/vagas?secao=TODAS|REMOTO|ESTAGIO&escopo=TODAS|NACIONAL|GRINGA&q=...&page=0&size=20`:
  lista paginada. `q` busca em título, empresa e local, sem diferenciar maiúsculas nem acentos.
- `GET /api/vagas/contagens` (mesmos filtros): total, fontes e contagem de cada aba.

Os endpoints administrativos estão documentados na seção anterior; a API pública continua
acessível sem credencial.

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
| `app.crawler.dias-para-expirar` | `7` | Dias sem ser vista até a vaga receber o status `EXPIRADA` |
| `app.crawler.gupy.page-size` | `100` | Vagas por página na API da Gupy |
| `app.crawler.gupy.max-paginas-por-termo` | `5` | Limite de páginas por termo de busca |
| `app.crawler.gupy.request-delay-ms` | `1000` | Pausa entre requisições à Gupy |
| `app.crawler.greenhouse.empresas` | 8 empresas no `application.yml` | Boards do Greenhouse (`boards.greenhouse.io/{empresa}`) |
| `app.crawler.lever.empresas` | 2 empresas no `application.yml` | Empresas do Lever (`jobs.lever.co/{empresa}`) |
| `app.crawler.linkedin.enabled` | `${LINKEDIN_ENABLED:false}` | Liga o crawler do LinkedIn |

Cada fonte tem mais opções nas classes `*CrawlerProperties` (termos de busca, palavras de
classificação, pausas, limites de páginas), todas sob `app.crawler.<fonte>`.

No perfil `docker`, o banco passa a ser o PostgreSQL configurado pelas variáveis `DB_*`.
