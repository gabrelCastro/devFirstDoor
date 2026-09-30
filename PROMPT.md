# Ralph loop — Dev First Door

Você está trabalhando no Dev First Door, um agregador de vagas remotas de **estágio e júnior em Java**.
Esta é UMA iteração de um loop: você começa sem memória das anteriores. A memória do loop é o
`fix_plan.md`. Leia-o antes de qualquer coisa.

## Projeto

- `backend/`: Spring Boot 4 (Java 21, Maven wrapper). Crawlers em `crawler/<fonte>/` implementam
  `VagaCrawler` (client HTTP + parser/DTO + mapper + classifier + properties). `ColetaService`
  roda todos os crawlers e isola as falhas; `DeduplicacaoService` descarta repetidas pelo hash de
  título+empresa+fonte. Toda fonte só aceita vagas que pedem Java (`util/LinguagemJava`).
- `frontend/`: React + Vite, arquivo principal `src/App.jsx`.
- Código, comentários, logs e commits em **português**, seguindo o estilo dos arquivos vizinhos
  (nomes como `coletar`, `buscarTodasAsPaginas`; javadoc curto explicando o *porquê*).

## O que fazer nesta iteração

1. Leia o `fix_plan.md`. Escolha o **primeiro** item com `[ ]`.
   Se não houver nenhum `[ ]`, responda apenas `BACKLOG CONCLUÍDO` e encerre sem mudar nada.
2. Leia o código relacionado antes de mudar qualquer coisa. Não reimplemente o que já existe.
3. Implemente **somente esse item**, com testes.
4. Valide:
   - `cd backend && ./mvnw -q test`
   - se mexeu no frontend: `cd frontend && npm run lint && npm run build`
5. Se passou: marque o item como `[x]` no `fix_plan.md`, com uma linha resumindo o que foi feito, e
   escreva a mensagem de commit (em português, título na 1ª linha, linha em branco, corpo curto) no
   arquivo `.ralph-commit-msg` na raiz do repositório. **Não rode `git add` nem `git commit`**: o
   script do loop roda os testes de novo e faz o commit com essa mensagem.
6. Se não conseguir fazer passar depois de 2 tentativas: desfaça as mudanças do item
   (`git checkout -- . && git clean -fd`, preservando o `fix_plan.md`), marque o item como `[!]` com
   o motivo no `fix_plan.md`, escreva a mensagem em `.ralph-commit-msg` e encerre.

## Regras

- **Nunca** rode `docker`, `docker compose`, `make up` nem suba o backend (`spring-boot:run`,
  `java -jar`). Subir o backend dispara a coleta real.
- **Nunca** faça requisições reais às fontes (LinkedIn, Gupy, RemoteOK, ProgramaThor, Greenhouse,
  Lever), nem com `curl`. Os testes usam HTML/JSON de fixture no próprio teste. O LinkedIn bloqueia
  IPs que fazem requisições demais.
- Nenhum teste pode depender de rede. Coleta no startup fica desligada nos testes
  (`app.crawler.run-on-startup=false` em `src/test/resources/application.yml`), e assim deve continuar.
- Não faça `git push`, não troque de branch, não reescreva histórico.
- Não mexa em itens fora do escolhido, não faça refatorações "de passagem" e não adicione itens
  novos ao backlog.
- Não mude o comportamento já existente sem que o item peça: filtro Java, seção Remoto,
  LinkedIn desligado por padrão, deduplicação.
