# 8. Arrays e estruturas

## Vetores

```portugol
inteiro[] numeros = {1, 2, 3, 4, 5}
inteiro turma[5]
```

A primeira forma deduz o tamanho do literal. A segunda reserva 5 posições sem valores. O índice começa em `0`, por isso `numeros[0]` é o primeiro elemento e `numeros[4]` o último. Ler fora destes limites é um dos erros mais comuns em C.

## Matrizes

Duas dimensões:

```portugol
inteiro[][] matriz = {{1, 2}, {3, 4}}
inteiro grade[2][2]

escreva("valor = ", matriz[1][0], "\n")
```

O tamanho pode vir do literal. Uma matriz literal tem de ser retangular: todas as linhas com o mesmo número de elementos.

## Estruturas

Uma `estrutura` junta valores de tipos diferentes numa só coisa:

```portugol
estrutura Pessoa {
    texto nome
    inteiro idade
    inteiro notas[10]
}

inicio() {
    estrutura Pessoa p1 = {"Ana", 25}
    escreva(p1.nome, " tem ", p1.idade, " anos\n")
    p1.idade = p1.idade + 1
}
```

- Os campos aceitam tamanho fixo, como `inteiro notas[10]`.
- Um array de estruturas escreve-se `estrutura Pessoa[] turma`.
- Acedes a um campo com `.`, igual ao C.
- A inicialização com chaves segue a ordem dos campos.

As estruturas declaram-se fora das funções, no topo do ficheiro.

## Exercício

Cria uma estrutura `Produto` com nome, preço (`real`) e stock (`inteiro`). Guarda três produtos num array e percorre-o com `para cada`, mostrando os que têm stock acima de zero.

Anterior: [Funções](07-funcoes.md). Próximo: [Bibliotecas padrão](09-bibliotecas-padrao.md).
