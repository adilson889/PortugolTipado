# Vetores e Matrizes em C, e a Ponte do PortugolTipado

Em C, um vetor (array) guarda múltiplos valores do mesmo tipo em posições de memória contíguas, acedidas através de um índice. O índice do primeiro elemento é sempre `0` — uma convenção que C herdou da forma como o endereço de cada elemento é calculado internamente: posição 0 é o próprio início do bloco de memória, sem deslocamento.

```c
int numeros[] = {10, 20, 30};
printf("Primeiro numero: %d\n", numeros[0]);
```

Esquecer que a contagem começa em `0`, e não em `1`, é um erro comum para quem vem de outras convenções — tentar aceder a `numeros[3]` neste exemplo seria um erro, já que as posições válidas são `0`, `1` e `2`.

## Matrizes: vetores de duas dimensões

Uma matriz é, na prática, um vetor de vetores — organizado em linhas e colunas, acedido com dois índices:

```c
int matriz[2][2] = {
    {1, 2},
    {3, 4}
};
printf("Elemento na linha 1, coluna 0: %d\n", matriz[1][0]);
```

O primeiro índice selecciona a linha, o segundo selecciona a coluna dentro dessa linha — ambos a contar a partir de `0`.

## Percorrendo um vetor

Para processar todos os elementos de um vetor, C recorre tipicamente a um laço `for` indexado:

```c
for (int i = 0; i < 3; i++) {
    printf("Valor: %d\n", numeros[i]);
}
```

## A mesma lógica, em PortugolTipado

```portugol
funcao inicio()
{
    // Vetor unidimensional
    inteiro[] numeros = {10, 20, 30}
    escreva("Primeiro número: ", numeros[0], "\n")

    // Matriz bidimensional (2 linhas, 2 colunas)
    inteiro[][] matriz = {
        {1, 2},
        {3, 4}
    }
    escreva("Elemento na linha 1, coluna 0: ", matriz[1][0], "\n")

    // Percorrendo vetores com "para cada"
    para cada (inteiro n em numeros) {
        escreva("Valor: ", n, "\n")
    }
}
```

A correspondência é directa: `inteiro[]` declara um vetor exactamente como `int[]` em C, e `inteiro[][]` declara uma matriz como `int[][]`. O acesso por índice, `numeros[0]` e `matriz[1][0]`, é idêntico nas duas linguagens — é literalmente a mesma notação em C. A diferença surge no `para cada`: esta construção não existe em C puro, mas o compilador do PortugolTipado gera, por trás, o mesmo laço `for` indexado do exemplo acima — poupando a escrita manual do índice sem esconder o que realmente acontece na memória.

## Porque isto importa

Vetores e matrizes são a base de quase toda a estrutura de dados mais complexa em C: desde uma simples lista de notas até uma imagem representada como matriz de pixels. Compreender que o índice começa em `0`, e que uma matriz é, no fundo, um vetor organizado em duas dimensões, é essencial para evitar os erros de acesso fora dos limites que são uma das causas mais frequentes de falhas em programas C.