# Backlog do Ralph loop: área administrativa

Legenda: `[ ]` a fazer · `[x]` feito · `[!]` travado (motivo ao lado).
Faça sempre o primeiro `[ ]`, um por iteração.

## Decisões de arquitetura (valem para todos os itens)

- **Autenticação**: Spring Security, um único usuário admin com login/senha vindos de
  `ADMIN_USER` / `ADMIN_PASSWORD` (propriedades `app.admin.usuario` / `app.admin.senha`).
  **HTTP Basic, stateless, sem sessão e sem cookie** (por isso CSRF pode ficar desligado). Sem
  `ADMIN_PASSWORD` definido, todo `/api/admin/**` responde 401 (a área admin fica desligada, sem
  senha padrão). Comparação de senha em tempo constante.
- **Rotas**: tudo que é administrativo fica em `/api/admin/**`. A API pública (`GET /api/vagas`,
  `GET /api/vagas/contagens`) continua aberta e não muda de formato.
- **Frontend**: rota `/admin` no mesmo app React, escolhida pelo `window.location.pathname` em
  `main.jsx` (sem roteador). Código admin em `src/admin/`. Credenciais guardadas só em
  `sessionStorage` e enviadas no header `Authorization`. Resposta 401 volta para o login.
  Atualização "ao vivo" por polling (3s enquanto houver coleta rodando, 30s parado).
- **Coletas** disparadas pelo admin rodam em segundo plano: o endpoint responde `202 Accepted` na
  hora, e o andamento é acompanhado pelo painel.
- **Dados de administração** (execuções, configuração, descartes) ficam em tabelas próprias,
  criadas pelo `ddl-auto: update` como o resto do projeto.

---

# Fase 1: acesso, painel dos crawlers e histórico

## 1. [x] Autenticação da área administrativa

> Feito: Spring Security com HTTP Basic stateless (`SecurityConfig`, `AdminAuthenticationProvider` em
> tempo constante, `AdminProperties`), `GET /api/admin/me`, `POST /api/vagas/coletar` virou
> `POST /api/admin/coletas`, 401 sem `WWW-Authenticate` (evita o popup do navegador), variáveis no
> compose/.env.example e testes em `AdminControllerTest`/`AdminDesligadoTest`.

- Adicionar `spring-boot-starter-security` (e o módulo de testes de segurança) ao `pom.xml`.
- Configuração conforme as decisões acima. `GET /api/admin/me` devolve o usuário autenticado (o
  frontend usa para validar o login).
- **Fechar o `POST /api/vagas/coletar`**, que hoje está aberto: ele sai da API pública e passa a
  existir como `POST /api/admin/coletas` (ainda síncrono neste item; vira assíncrono no item 4).
- `ADMIN_USER` e `ADMIN_PASSWORD` no `docker-compose.yml` (repassados ao backend) e no
  `.env.example` (senha em branco, com comentário).
- Verificar que o `CorsConfig` continua funcionando com o Security.
- Testes (MockMvc): API pública aberta; admin sem credencial = 401; credencial errada = 401;
  credencial certa = 200; sem `ADMIN_PASSWORD` configurado = 401 mesmo com qualquer credencial;
  `POST /api/vagas/coletar` não existe mais.

## 2. [x] Histórico de execuções de coleta

> Feito: entidades `ExecucaoColeta`/`ExecucaoFonte` gravadas pelo `HistoricoColetaService` (execução
> criada no início, fonte a fonte, status geral SUCESSO/PARCIAL/ERRO), `executarTodos(OrigemColeta)`
> nos runners e no admin, `VagaCrawler.contarEncontradas` (LinkedIn soma as já salvas),
> `GET /api/admin/execucoes` paginado e testes em `ColetaServiceTest`/`HistoricoColetaServiceTest`/`AdminControllerTest`.

- Entidades `ExecucaoColeta` (id, origem `INICIAL|AGENDADA|MANUAL`, início, fim, status geral) e
  `ExecucaoFonte` (execução, fonte, início, fim, status `SUCESSO|ERRO`, encontradas, novas,
  expiradas, mensagem de erro truncada em ~1000 caracteres).
- `ColetaService.executarTodos` recebe a origem e grava a execução e o resultado de cada fonte
  (os runners e o endpoint admin passam a origem certa).
- **Encontradas ≠ devolvidas**: o LinkedIn só devolve vagas *novas* (as já salvas são puladas antes
  de ler a descrição), então "0 devolvidas" é normal para ele. Crie um jeito simples de o crawler
  informar quantas vagas encontrou na fonte ao todo (incluindo as já salvas) e grave isso em
  `encontradas`. Para os outros crawlers, encontradas = devolvidas.
- `GET /api/admin/execucoes` (paginado, mais recente primeiro, com os resultados por fonte).
- Testes: gravação de sucesso e de erro por fonte; origem; endpoint com e sem credencial.

## 3. [x] Estado ao vivo e saúde dos crawlers

> Feito: `AndamentoColeta` (estado em memória, alimentado pelo `ColetaService`), colaborador
> `ProgressoColeta` via `VagaCrawler.coletar(progresso)` (LinkedIn: termos e descrições; Gupy: termos),
> `VagaCrawler.isLigada()` (Greenhouse/Lever sem empresas = desligados), regras em `SaudeCrawler`,
> próxima coleta no `ColetaAgendadaRunner` e `GET /api/admin/crawlers` (`EstadoCrawlersService`, lista
> fixa de fontes para incluir o LinkedIn sem bean). Testes em `SaudeCrawlerTest`, `AdminControllerTest`,
> `ColetaServiceTest` e nos testes dos crawlers.

- Estado em memória da coleta em andamento: se está rodando, origem, início, fonte atual e um texto
  de progresso. Crawlers podem informar progresso por um colaborador opcional (ex:
  `ProgressoColeta`); implemente pelo menos no LinkedIn ("termo 3/6", "lendo descrições 12/40") e
  na Gupy; os demais mostram só a fonte atual.
- `GET /api/admin/crawlers`: uma entrada por fonte, **incluindo as desligadas** (LinkedIn com
  `LINKEDIN_ENABLED=false`; Greenhouse/Lever sem empresas), com: ligada, rodando agora, progresso,
  última execução, último sucesso, saúde e horário previsto da próxima coleta agendada.
- Regras de saúde: `FALHA` se a última execução da fonte deu erro; `ALERTA` se as 3 últimas
  execuções com sucesso encontraram 0 vagas (sinal de HTML/API que mudou); `OK` caso contrário;
  `SEM_DADOS` se nunca rodou; `DESLIGADA` se desligada.
- Testes das regras de saúde (unitários) e do endpoint.

## 4. [x] Ações sobre as coletas

> Feito: `ColetaService.disparar` (todas ou uma fonte) pega a trava na thread da requisição e roda
> numa thread própria; `POST /api/admin/coletas[/{fonte}]` responde 202/409/404 com `mensagem`;
> `PausaAgendamento` em memória, respeitada pelo `ColetaAgendadaRunner` e exposta como
> `agendamentoPausado` em `GET /api/admin/crawlers`; `POST /api/admin/agendamento/pausar|retomar`.
> Testes em `ColetaServiceTest`, `AdminControllerTest` e `ColetaAgendadaRunnerTest`.

- `POST /api/admin/coletas` (todas) e `POST /api/admin/coletas/{fonte}` (uma fonte) rodam em
  segundo plano e respondem `202`; se já houver coleta rodando, `409` com mensagem.
  Fonte inexistente = `404`; fonte desligada = `409`.
- `POST /api/admin/agendamento/pausar` e `/retomar`: enquanto pausado, a coleta agendada é pulada
  com log (em memória neste item; o item 6 persiste). O estado aparece em `GET /api/admin/crawlers`.
- Testes: 202/409/404, execução de uma fonte só, pausa respeitada pelo runner agendado.

## 5. [x] Frontend admin: login, painel dos crawlers e histórico

> Feito: `Raiz.jsx` escolhe `/admin` pelo pathname e carrega `src/admin/` sob demanda (lazy);
> `admin/api.js` (credencial Basic em `sessionStorage`, 401 → login), `Login`, `Painel` (cards por
> fonte com saúde, progresso, última execução, próxima coleta, coletar todas/por fonte, pausar/retomar,
> polling 3s/30s) e `Historico` (tabela paginada com detalhe por fonte); `useTema` extraído para
> `tema.js`; estilos em `admin/Admin.css` com as variáveis de tema. Lint e build passando.

- `/admin` com tela de login (valida em `GET /api/admin/me`) e botão de sair.
- Painel: um card por fonte com saúde (cor), ligada/desligada, rodando agora + progresso, última
  execução (quando, duração, encontradas/novas/expiradas, erro), próxima coleta. Botões "coletar
  agora" (todas e por fonte) e pausar/retomar agendamento. Polling conforme as decisões acima.
- Histórico: tabela das execuções com paginação e o detalhe por fonte.
- Mesmo visual da página pública (tema claro/escuro, fontes, variáveis de cor). Responsivo.
- `npm run lint && npm run build` passando.

---

# Fase 2: configuração pelo painel e moderação de vagas

## 6. [x] Configuração persistida e lida em tempo de execução (backend)

> Feito: tabela chave→JSON e `ConfiguracaoService` com padrões do yml; fontes, termos, empresas,
> expiração, pausas e agendamento passam a ser lidos dinamicamente; LinkedIn sempre registrado sob
> a chave-mestra; agendamento com trigger dinâmico; `GET/PUT /api/admin/configuracao` validados e testados.

- Tabela de configuração (chave → valor JSON) com os valores do `application.yml` como padrão:
  fonte ligada/desligada, termos de busca (LinkedIn, Gupy), empresas (Greenhouse, Lever), intervalo
  da coleta agendada, dias para expirar, pausas entre requisições do LinkedIn, agendamento pausado.
- Os crawlers e serviços leem a configuração a cada coleta (não só no startup).
- **`LINKEDIN_ENABLED` continua sendo a chave-mestra**: com `false`, o LinkedIn não pode ser ligado
  pelo painel. Para isso o bean do LinkedIn passa a existir sempre e checa se está ligado ao
  coletar (em vez de `@ConditionalOnProperty`), sem fazer nenhuma requisição quando desligado.
- Intervalo da coleta agendada dinâmico (ex: `SchedulingConfigurer` com trigger que lê a config).
- `GET /api/admin/configuracao` e `PUT /api/admin/configuracao` com validação (intervalo mínimo 30
  min, dias para expirar ≥ 1, listas sem itens vazios; erro 400 com mensagem clara).
- Testes: padrão vindo do yml, alteração refletida na coleta seguinte, validação, chave-mestra.

## 7. [x] Testar board do Greenhouse/Lever

> Feito: `POST /api/admin/boards/testar` consulta Greenhouse/Lever pelos clients existentes,
> distingue board inexistente de vazio, aplica os classificadores da coleta e devolve totais e
> até 10 exemplos aprovados/reprovados com motivo; testes usam clients falsos, sem rede.

- `POST /api/admin/boards/testar` com `{ "ats": "GREENHOUSE|LEVER", "empresa": "..." }`: diz se o
  board existe, quantas vagas tem e quantas passariam pelos filtros (estágio/júnior, tech, Java,
  remoto), com até 10 exemplos das que passam e das que não passam (com o motivo).
- Reaproveita os clients e classifiers existentes. Nos testes, client falso (sem rede).

## 8. [x] Moderação de vagas (backend)

> Feito: expiração por status com reativação na visita, API pública restrita às ativas,
> correções manuais preservadas na reclassificação e endpoints admin paginados para listar,
> filtrar, editar e reclassificar vagas, com testes de autenticação e comportamento.

- Expiração vira **soft delete**: status `ATIVA|EXPIRADA|OCULTA` em vez de apagar. Vaga expirada
  que reaparece numa coleta volta a `ATIVA` (atenção: hoje a deduplicação descarta pelo hash, então
  a reativação precisa acontecer no registro de visita). A API pública só mostra `ATIVA`.
- Correção manual de nível e de remoto, marcada como manual para que reclassificações não a
  sobrescrevam.
- `GET /api/admin/vagas` (filtros: status, fonte, busca; paginado), `PATCH /api/admin/vagas/{id}`
  (status, nível, remoto), `POST /api/admin/vagas/reclassificar` (recalcula os campos derivados de
  todas, respeitando correções manuais).
- Testes: soft delete + reativação, API pública ignorando não ativas, correção manual preservada.

## 9. [x] Duplicatas entre fontes

> Feito: `GET /api/admin/vagas/duplicatas` agrupa vagas ativas de fontes distintas por título +
> empresa normalizados; `POST /api/admin/vagas/duplicatas/resolver` mantém a vaga escolhida e oculta
> as demais do grupo, com validação do estado atual e testes de autenticação, agrupamento e resolução.

- A deduplicação só compara vagas da mesma fonte, então a mesma vaga pode aparecer no LinkedIn e na
  Gupy. `GET /api/admin/vagas/duplicatas`: grupos de vagas ativas de fontes diferentes com título +
  empresa normalizados iguais.
- Ação para ocultar as demais de um grupo mantendo uma (`POST /api/admin/vagas/duplicatas/resolver`).
- Não mude a deduplicação automática neste item.
- Testes do agrupamento e da resolução.

## 10. [x] Frontend admin: configuração e moderação

> Feito: navegação ganhou telas responsivas de configuração e vagas; configuração edita fontes,
> termos, empresas e parâmetros com teste de boards; moderação filtra, corrige, oculta/reativa,
> reclassifica e resolve duplicatas; cliente JSON e montagem dos filtros cobertos por testes Node.

- Tela de configuração (itens 6 e 7): formulário por fonte, listas editáveis (termos, empresas),
  intervalo, dias para expirar; botão "testar" ao lado de cada empresa mostrando o resultado do
  item 7; mensagens de validação do backend.
- Tela de vagas (itens 8 e 9): tabela com filtros, ocultar/reativar, corrigir nível e remoto,
  botão "reclassificar tudo" com confirmação, lista de duplicatas com ação de resolver.
- Navegação entre Painel · Histórico · Configuração · Vagas dentro do `/admin`.
- `npm run lint && npm run build` passando.

---

# Fase 3: descartes, métricas e notificações

## 11. [x] Registro de vagas descartadas e motivo

Feito: `DescarteService` registra os descartes dos seis crawlers (link + motivo sem duplicar), apaga registros com mais de 30 dias ao fim de cada coleta e expõe `GET /api/admin/descartes` com filtros e contagem por motivo.

- Os crawlers registram o que descartam, com motivo: `NAO_JAVA`, `NAO_REMOTA`, `NIVEL`
  (pleno/sênior ou sem nível), `FORA_DE_TECNOLOGIA`, `ERRO_LEITURA`. Guardar fonte, título,
  empresa, local, link, motivo e data. Sem duplicar (mesmo link + motivo atualiza a data).
- Retenção: apagar registros com mais de 30 dias ao fim de cada coleta.
- `GET /api/admin/descartes` (filtros: fonte, motivo, busca; paginado) e contagem por motivo.
- Não altere nenhuma regra de filtro, só registre.
- Testes: registro por motivo em pelo menos LinkedIn, Gupy e ProgramaThor; retenção; endpoint.

## 12. [x] Métricas

> Feito: `GET /api/admin/metricas` agrega no banco vagas ativas, novas, exclusivas e expiradas,
> além dos descartes recentes; chaves normalizadas persistidas permitem detectar duplicatas entre
> fontes sem carregar vagas em memória, com cobertura autenticada em H2.

- `GET /api/admin/metricas`: vagas ativas por fonte, nível e modalidade; vagas novas por dia e por
  fonte nos últimos 30 dias; tempo médio no ar das vagas expiradas; vagas "exclusivas" por fonte
  (sem duplicata em outra fonte); descartes por motivo nos últimos 7 dias.
- Consultas agregadas no banco (não carregar todas as vagas em memória).
- Testes com dados de exemplo em H2.

## 13. [ ] Notificações por Telegram

- Configuração (via item 6): ligado/desligado, token do bot e chat id (o token nunca é devolvido
  pela API de configuração: mostre só se está preenchido).
- Após cada coleta com vagas novas: uma mensagem com título, empresa, nível, local e link (agrupar
  se forem muitas; respeitar o limite de tamanho do Telegram).
- Alerta quando a saúde de uma fonte **muda** para `FALHA` ou `ALERTA` e quando volta a `OK`
  (sem repetir o alerta a cada coleta).
- `POST /api/admin/notificacoes/testar` envia uma mensagem de teste.
- Cliente HTTP do Telegram isolado; nos testes, cliente falso (sem rede).

## 14. [ ] Frontend admin: descartes, métricas e notificações

- Tela de descartes com filtros e contagem por motivo.
- Tela de métricas com números e gráficos simples em SVG/CSS (sem biblioteca), legíveis nos dois temas.
- Configuração de notificações (dentro da tela de configuração) com botão "enviar teste".
- `npm run lint && npm run build` passando.

## 15. [ ] README da área administrativa

- Como habilitar (`ADMIN_USER`/`ADMIN_PASSWORD`), o que cada tela faz, endpoints `/api/admin/**`,
  configuração do Telegram, e o aviso de que sem senha a área admin fica desligada. Baseie tudo no
  código, sem inventar recurso.
