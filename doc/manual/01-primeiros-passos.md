# 1. Primeiros passos

Um programa em PortugolTipado é um ficheiro de texto com extensão `.port`. O compilador lê-o e gera um ficheiro C, que o `gcc` transforma num programa normal.

## O primeiro programa

Cria um ficheiro `ola.port`:

```portugol
inicio() {
    escreva("Olá, mundo\n")
}
```

`inicio()` é por onde o programa começa. Tudo o que está entre as chaves corre por ordem. Podes também escrevê-lo como `funcao inicio() { ... }`, que dá o mesmo resultado.

Repara que não há `;` no fim da linha. O compilador põe-nos no C por ti.

## Compilar e correr

```
java -jar core/build/libs/port.jar -o ola.c ola.port
gcc ola.c -o ola
./ola
```

O primeiro comando traduz para C, o segundo compila e o terceiro corre. O C gerado é legível: abre o `ola.c` e vê o que saiu.

Se o programa tiver um erro, o compilador diz-to em português e indica a linha, sem chegar ao `gcc`. Mais sobre isto no [capítulo 13](13-erros-e-avisos.md).

## Comentários

```portugol
// uma linha

/*
   várias
   linhas
*/
```

São iguais aos do C e o compilador ignora-os.

## Escrever as palavras

Todas as palavras reservadas escrevem-se por extenso e em português: `inteiro`, `escreva`, `senao`. Não há abreviaturas. Os nomes das tuas variáveis levam letras, dígitos e `_`, e não podem começar por dígito.

## Estrutura de um ficheiro

Fora de `inicio` só podes ter três coisas: `inclua`, `estrutura` e `funcao`. Não há variáveis soltas fora das funções.

Um ficheiro sem `inicio` é um módulo: serve só para ser incluído por outro programa (ver [capítulo 10](10-modulos-e-pacotes.md)).

## Exercício

Escreve um programa que mostre o teu nome e a tua cidade em duas linhas separadas. Compila-o, corre-o e abre o `.c` gerado para ver como o `escreva` ficou.

Próximo: [Tipos e variáveis](02-tipos-e-variaveis.md).
