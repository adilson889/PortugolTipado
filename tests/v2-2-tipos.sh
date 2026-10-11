#!/usr/bin/env bash
set -euo pipefail

PORT_JAR="core/build/libs/port.jar"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

rejeitar() {
    local caso="$1"
    local codigo="$2"
    printf '%s\n' "$codigo" > "$TMP/$caso.port"
    if java -jar "$PORT_JAR" --tipagem=segura -o "$TMP/$caso.c" "$TMP/$caso.port" >"$TMP/$caso.log" 2>&1; then
        echo "FALHOU: caso inseguro '$caso' aceito" >&2
        exit 1
    fi
    if ! grep -q 'tipagem segura' "$TMP/$caso.log"; then
        echo "FALHOU: '$caso' foi rejeitado fora do analisador seguro" >&2
        cat "$TMP/$caso.log" >&2
        exit 1
    fi
    echo "OK (rejeitado): $caso"
}

aceitar() {
    local caso="$1"
    local codigo="$2"
    local esperado="$3"
    printf '%s\n' "$codigo" > "$TMP/$caso.port"
    java -jar "$PORT_JAR" --tipagem=segura -o "$TMP/$caso.c" "$TMP/$caso.port"
    gcc -Wall -Wextra -o "$TMP/$caso" "$TMP/$caso.c" -lm
    local recebido
    recebido="$("$TMP/$caso")"
    if [ "$recebido" != "$esperado" ]; then
        echo "FALHOU: $caso: resultado '$recebido', esperado '$esperado'" >&2
        exit 1
    fi
    echo "OK (executado): $caso"
}

aceitar "funcao" '
funcao dobro(inteiro x): inteiro {
    retorne x * 2
}
funcao inicio() {
    inteiro i = 5
    escreva(dobro(i))
}' "10"

aceitar "estrutura" '
estrutura Pessoa {
    inteiro idade
    texto nome
}
funcao inicio() {
    estrutura Pessoa p = {25, "Ana"}
    escreva(p.idade)
}' "25"

aceitar "array" '
funcao inicio() {
    inteiro notas[3] = {10, 20, 30}
    inteiro soma = notas[0] + notas[1] + notas[2]
    escreva(soma)
}' "60"

aceitar "retorno_condicional" '
funcao verificaPositivo(inteiro x): inteiro {
    se (x > 0) {
        retorne 1
    } senao {
        retorne 0
    }
}
funcao inicio() {
    escreva(verificaPositivo(5))
}' "1"

aceitar "promocao_longo" '
funcao dobro(inteiro longo x): inteiro longo {
    retorne x * 2
}
funcao inicio() {
    inteiro longo valor = 11
    escreva(dobro(valor))
}' "22"

rejeitar "real_em_inteiro" '
funcao inicio() {
    inteiro x = 4
    x = 3.9
}'

rejeitar "expressao_decimal_em_inteiro" '
funcao inicio() {
    inteiro x = 4
    x = 1 + 2.5
}'

rejeitar "retorno_decimal" '
funcao exemplo(): inteiro {
    retorne 4.5
}
funcao inicio() {}'

rejeitar "retorno_faltante" '
funcao exemplo(): inteiro {
    inteiro x = 2
}
funcao inicio() {}'

rejeitar "vazio_com_valor" '
funcao exemplo(): vazio {
    retorne 7
}
funcao inicio() {}'

rejeitar "retorno_sem_valor" '
funcao exemplo(): inteiro {
    retorne
}
funcao inicio() {}'

rejeitar "funcao_argumento_decimal" '
funcao eco(inteiro x): inteiro {
    retorne x
}
funcao inicio() {
    escreva(eco(2.9))
}'

rejeitar "funcao_argumento_contagem" '
funcao eco(inteiro x): inteiro {
    retorne x
}
funcao inicio() {
    escreva(eco())
}'

rejeitar "altere_literal" '
funcao trocar(altere inteiro x): vazio {
    x = 5
}
funcao inicio() {
    trocar(2)
}'

rejeitar "condicao_numerica" '
funcao inicio() {
    se (10) {
        escreva("nao")
    }
}'

rejeitar "campo_inexistente" '
estrutura Pessoa {
    inteiro idade
}
funcao inicio() {
    estrutura Pessoa p = {20}
    escreva(p.nome)
}'

rejeitar "campo_tipo_errado" '
estrutura Pessoa {
    inteiro idade
}
funcao inicio() {
    estrutura Pessoa p = {"vinte"}
    escreva(p.idade)
}'

rejeitar "array_tamanho" '
funcao inicio() {
    inteiro notas[2] = {1, 2, 3}
}'

rejeitar "array_elemento" '
funcao inicio() {
    inteiro notas[2] = {1, "dois"}
}'

rejeitar "array_indice_decimal" '
funcao inicio() {
    inteiro notas[2] = {1, 2}
    escreva(notas[1.5])
}'

rejeitar "array_atribuicao" '
funcao inicio() {
    inteiro notas[2] = {1, 2}
    notas = notas
}'

rejeitar "operacao_logica_invalida" '
funcao inicio() {
    logico x = verdadeiro && 9
    escreva(x)
}'

echo "V2.2: todos os casos foram verificados."
