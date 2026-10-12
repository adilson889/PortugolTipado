#!/usr/bin/env bash
set -euo pipefail
JAR="core/build/libs/port.jar"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
INTERPRETADOR="co.adilson889.typec.port.VerificarInterpretadorTextoKt"

comparar() {
    local nome="$1" esperado="$2" entrada="${3:-}"
    cat > "$TEMP/$nome.port"
    printf '%s' "$entrada" > "$TEMP/$nome.in"
    java -jar "$JAR" -o "$TEMP/$nome.c" "$TEMP/$nome.port"
    gcc -Wall -Wextra -Werror -o "$TEMP/$nome.exe" "$TEMP/$nome.c" -lm
    local c_preview kotlin_preview
    c_preview="$("$TEMP/$nome.exe" < "$TEMP/$nome.in")"
    kotlin_preview="$(java -cp "$JAR" "$INTERPRETADOR" "$TEMP/$nome.port" "$TEMP/$nome.in")"
    if [[ "$c_preview" != "$esperado" || "$kotlin_preview" != "$esperado" ]]; then
        echo "FALHOU $nome: C=[$c_preview] interpretador=[$kotlin_preview] esperado=[$esperado]" >&2
        exit 1
    fi
    echo "OK C + interpretador: $nome"
}

seguro_rejeita() {
    local nome="$1" frase="$2"
    cat > "$TEMP/$nome.port"
    if java -jar "$JAR" --tipagem=segura -o "$TEMP/$nome.c" "$TEMP/$nome.port" > "$TEMP/$nome.log" 2>&1; then
        echo "FALHOU $nome: aceite no modo seguro" >&2
        exit 1
    fi
    if ! grep -q "$frase" "$TEMP/$nome.log"; then
        cat "$TEMP/$nome.log" >&2
        echo "FALHOU $nome: mensagem esperada '$frase'" >&2
        exit 1
    fi
    echo "OK rejeitado no modo seguro: $nome"
}

comparar "ler_texto_inicializado" "Bia" $'Bia\n' <<'PORT'
funcao inicio() {
    texto nome = "Ana"
    leia(nome)
    escreva(nome)
}
PORT

comparar "copiar_e_juntar_inicializado" "Bia!" <<'PORT'
inclua texto
funcao inicio() {
    texto nome = "Ana"
    copie(nome, "Bia")
    junte(nome, "!")
    escreva(nome)
}
PORT

comparar "copiar_e_juntar_sem_valor" "AnaAna" <<'PORT'
inclua texto
funcao inicio() {
    texto nome
    copie(nome, "Ana")
    junte(nome, nome)
    escreva(nome)
}
PORT

comparar "atribuicao_a_texto_mutavel" "Beatriz!" <<'PORT'
inclua texto
funcao inicio() {
    texto nome = "Ana"
    nome = "Beatriz"
    junte(nome, "!")
    escreva(nome)
}
PORT

comparar "preserva_espacos" "  Maria  " $'  Maria  \n' <<'PORT'
funcao inicio() {
    texto nome = "Ana"
    leia(nome)
    escreva(nome)
}
PORT

comparar "leitura_longa_repete" "Bia" "$(printf '%0260d\nBia\n' 0)" <<'PORT'
funcao inicio() {
    texto nome
    leia(nome)
    escreva(nome)
}
PORT

comparar "copia_sobreposta" "AnaAna" <<'PORT'
inclua texto
funcao inicio() {
    texto nome = "Ana"
    junte(nome, nome)
    copie(nome, nome)
    escreva(nome)
}
PORT

seguro_rejeita "ler_literal_seguro" "Não é possível ler" <<'PORT'
funcao inicio() {
    texto nome = "Ana"
    leia(nome)
}
PORT

seguro_rejeita "copiar_literal_seguro" "Não é possível alterar" <<'PORT'
inclua texto
funcao inicio() {
    texto nome = "Ana"
    copie(nome, "Bia")
}
PORT

seguro_rejeita "juntar_literal_seguro" "Não é possível alterar" <<'PORT'
inclua texto
funcao inicio() {
    texto nome = "Ana"
    junte(nome, "Bia")
}
PORT

echo "Compatibilidade de texto: testes C e interpretador concluídos."
