# Backlog do Ralph loop

Legenda: `[ ]` a fazer · `[x]` feito · `[!]` travado (motivo ao lado).
Faça sempre o primeiro `[ ]`, um por iteração.

## 1. [x] Coleta agendada

Feito: `runner/ColetaAgendadaRunner` (`@Scheduled` com `app.crawler.intervalo`, padrão 6h, e
`app.crawler.agendamento.enabled`, desligado nos testes); `ColetaService.executarTodos()` usa um
`AtomicBoolean` e ignora (com log, devolvendo mapa vazio) coletas que chegam com outra em andamento;
testes em `ColetaServiceTest`.

Hoje a coleta só roda uma vez, quando o backend sobe (`runner/ColetaInicialRunner`).

- Rodar `ColetaService.executarTodos()` periodicamente com `@Scheduled`, intervalo configurável
  (`app.crawler.intervalo`, padrão 6h) e um liga/desliga (`app.crawler.agendamento.enabled`,
  padrão true; false em `src/test/resources/application.yml`).
- Não permitir duas coletas simultâneas (agendada + `POST /api/vagas/coletar` + inicial): se já
  houver uma rodando, a nova é ignorada com log.
- Testes: a trava de concorrência (sem Spring, chamando o serviço direto com crawlers falsos).

## 2. [x] Filtros e paginação na API (tirar o limite de 50 do frontend)

Feito: `Vaga` persiste `remoto`, `internacional` e `textoBusca` (normalizado) via `ClassificacaoVaga`
(lógica movida de `VagaResponse`); `AtualizacaoVagasAntigasRunner` preenche vagas antigas no startup;
`VagaFiltro` (Specification) + `ConsultaVagasService` atendem `GET /api/vagas?secao&escopo&q` e
`GET /api/vagas/contagens` (total, fontes, contagem por seção/abrangência); frontend usa a API com
"carregar mais" e debounce de 300ms; testes em `VagaControllerTest` (MockMvc + H2) e `ClassificacaoVagaTest`.

O frontend busca `/api/vagas?size=50` e filtra no navegador, então com mais de 50 vagas as abas e
contagens ficam erradas.

- `GET /api/vagas` passa a aceitar `secao` (`TODAS|REMOTO|ESTAGIO`), `escopo`
  (`TODAS|NACIONAL|GRINGA`) e `q` (busca em título/empresa/local, sem diferenciar maiúsculas nem
  acentos), mantendo a paginação.
  Obs.: `remoto` e `internacional` hoje são calculados em `VagaResponse`, não são colunas. Escolha a
  abordagem mais simples que filtre corretamente antes da paginação (ex: persistir `remoto` e
  `internacional` na entidade ao salvar e preencher as vagas antigas numa atualização no startup).
- `GET /api/vagas/contagens` devolve as contagens de cada aba para os filtros atuais.
- Frontend: usa os parâmetros da API, com botão "carregar mais" para a paginação e debounce na busca.
- Testes: controller/repositório com H2 (`@DataJpaTest` ou `@SpringBootTest` + MockMvc).

## 3. [x] Remover vagas encerradas

Feito: `Vaga.dataUltimaVisita` (inicia com a `dataColeta`); `ColetaService` registra a visita das
vagas que reaparecem (`DeduplicacaoService.registrarVisita`, bulk update pelo hash) e, se o crawler
não falhou e devolveu ao menos uma vaga, chama `ExpiracaoVagasService.removerExpiradas(fonte)`, que
apaga as da fonte não vistas há mais de `app.crawler.dias-para-expirar` (7) dias (sem visita conta a
`dataColeta`); `LinkedinCrawler` registra a visita das já salvas que pula; testes em
`ExpiracaoVagasServiceTest` (H2).

Vagas que saíram das fontes continuam no banco para sempre.

- Registrar em cada vaga quando ela foi vista pela última vez (`dataUltimaVisita`), atualizando
  também as já existentes que reaparecem na coleta (hoje a deduplicação só as descarta).
- Vagas não vistas há mais de N dias (`app.crawler.dias-para-expirar`, padrão 7) somem da API.
  Só conta coletas em que a fonte rodou com sucesso, para uma fonte fora do ar não "expirar"
  tudo dela.
- Testes: serviço de expiração e a atualização de `dataUltimaVisita`.

## 4. [x] Testes dos crawlers com cliente falso

Feito: `LinkedinCrawlerTest` e `GupyCrawlerTest` (clients e `RobotsTxtChecker` mockados com Mockito,
`DeduplicacaoService` real sobre `VagaRepository` mockado) cobrem bloqueio na busca e nas descrições,
cache de vagas sem Java, falha de leitura fora do cache e vaga já salva sem leitura (com visita
registrada); `ColetaServiceTest` ganhou o caso de um crawler com exceção sem impedir o outro de salvar.

- `LinkedinCrawler`: bloqueio (`LinkedinBloqueadoException`) no meio da busca e no meio da leitura
  das descrições; vaga sem Java descartada e não relida na coleta seguinte; vaga já no banco não
  gera leitura de descrição.
- `GupyCrawler`: título com Java não lê a página; título sem Java lê a página uma vez só (cache);
  falha de leitura não entra no cache.
- `ColetaService`: um crawler lançando exceção não impede os outros de salvar.
- Use subclasses/stubs dos clients ou Mockito (já vem no `spring-boot-starter-test`); sem rede.

## 5. [ ] Novas fontes: Greenhouse e Lever

APIs públicas de vagas, feitas para consumo externo:
- Greenhouse: `https://boards-api.greenhouse.io/v1/boards/{empresa}/jobs?content=true`
- Lever: `https://api.lever.co/v0/postings/{empresa}?mode=json`

- Um crawler por fonte, no padrão dos existentes (client, DTO, mapper, classifier, properties,
  `RobotsTxtChecker`), com a lista de empresas em `application.yml` (vazia por padrão = crawler não
  faz nada).
- Mesmos filtros das outras fontes: estágio/júnior pelo título, tecnologia, Java (título +
  conteúdo da vaga), só remoto (a partir do local / campo de modalidade de cada API).
- Testes com JSON de fixture escrito à mão no formato documentado de cada API.

## 6. [ ] README

`README.md` na raiz: o que o projeto faz; como subir (`make up`, `.env`); fontes e como cada uma
coleta; filtros (estágio/júnior, Java, remoto); `LINKEDIN_ENABLED` e o aviso de que ele ignora o
robots.txt do LinkedIn (uso pessoal, baixo volume); como rodar os testes; configurações principais
do `application.yml`. Baseie tudo no código, sem inventar recurso.
