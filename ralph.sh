#!/usr/bin/env bash
# Ralph loop: roda o PROMPT.md em iterações com contexto limpo até o fix_plan.md não ter
# mais itens "[ ]", até MAX_ITERACOES, ou até duas iterações seguidas sem commit novo.
# Uso: ./ralph.sh [max_iteracoes]   (padrão 30)
set -uo pipefail
cd "$(dirname "$0")"

MAX_ITERACOES="${1:-30}"
MSG=.ralph-commit-msg

if ! command -v codex >/dev/null 2>&1; then
    echo "Codex CLI não encontrado no PATH."
    exit 127
fi

# O commit fica com o script para manter cada iteração atômica: só commita se a
# iteração deixou mensagem, há mudanças e os testes passam aqui também.
commitar_iteracao() {
    if [[ ! -s "$MSG" ]]; then
        echo "Iteração $1 não deixou $MSG; nada a commitar."
        return
    fi
    if [[ -z "$(git status --porcelain -- . ":!$MSG")" ]]; then
        echo "Iteração $1 sem mudanças; nada a commitar."
        rm -f "$MSG"
        return
    fi
    if ! (cd backend && ./mvnw -q test >/dev/null 2>&1); then
        echo "Testes do backend falharam após a iteração $1; mudanças mantidas sem commit para revisão."
        rm -f "$MSG"
        exit 1
    fi
    if git diff --name-only HEAD -- frontend | grep -q . || git ls-files --others --exclude-standard frontend | grep -q .; then
        if ! (cd frontend && npm run lint >/dev/null 2>&1 && npm run build >/dev/null 2>&1); then
            echo "Lint/build do frontend falhou após a iteração $1; mudanças mantidas sem commit para revisão."
            rm -f "$MSG"
            exit 1
        fi
    fi
    { cat "$MSG"; printf '\nCo-Authored-By: OpenAI Codex <noreply@openai.com>\n'; } > "$MSG.final"
    rm -f "$MSG"
    git add -A && git commit -q -F "$MSG.final" && git log --oneline -1
    rm -f "$MSG.final"
}
mkdir -p ralph-logs
sem_progresso=0

for ((i = 1; i <= MAX_ITERACOES; i++)); do
    if ! grep -q '^## [0-9]*\. \[ \]' fix_plan.md; then
        echo "Nenhum item [ ] restante no fix_plan.md. Fim."
        break
    fi

    antes=$(git rev-parse HEAD)
    log="ralph-logs/iteracao-$(date +%Y%m%d-%H%M%S)-$i.log"
    echo "=== Iteração $i/$MAX_ITERACOES ($(date +%H:%M:%S)) → $log"

    if ! codex exec \
            --ephemeral \
            --approve-for-me \
            --cd "$PWD" \
            - < PROMPT.md 2>&1 | tee "$log"; then
        echo "Codex encerrou com erro. Mudanças mantidas sem commit para revisão."
        break
    fi

    commitar_iteracao "$i"

    if grep -q "BACKLOG CONCLUÍDO" "$log"; then
        echo "Backlog concluído."
        break
    fi

    if [[ "$(git rev-parse HEAD)" == "$antes" ]]; then
        ((sem_progresso++))
        echo "Iteração sem commit novo ($sem_progresso seguida(s))."
        if ((sem_progresso >= 2)); then
            echo "Duas iterações seguidas sem progresso. Parando para revisão manual."
            break
        fi
    else
        sem_progresso=0
    fi
done

echo
git log --oneline "$(git merge-base HEAD master)"..HEAD
