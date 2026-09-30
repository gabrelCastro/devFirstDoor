#!/usr/bin/env bash
# Ralph loop: roda o PROMPT.md em iterações com contexto limpo até o fix_plan.md não ter
# mais itens "[ ]", até MAX_ITERACOES, ou até duas iterações seguidas sem commit novo.
# Uso: ./ralph.sh [max_iteracoes]   (padrão 20)
set -uo pipefail
cd "$(dirname "$0")"

MAX_ITERACOES="${1:-20}"
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

    claude -p "$(cat PROMPT.md)" \
        --permission-mode acceptEdits \
        --allowedTools "Bash(cd:*)" "Bash(./mvnw:*)" "Bash(npm run:*)" "Bash(npm install:*)" \
                       "Bash(git add:*)" "Bash(git commit:*)" "Bash(git status:*)" "Bash(git diff:*)" \
                       "Bash(git log:*)" "Bash(git checkout -- :*)" "Bash(git clean -fd:*)" \
        --disallowedTools "Bash(docker:*)" "Bash(make:*)" "Bash(curl:*)" "Bash(wget:*)" \
                          "Bash(git push:*)" "Bash(java:*)" "WebFetch" \
        2>&1 | tee "$log"

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
