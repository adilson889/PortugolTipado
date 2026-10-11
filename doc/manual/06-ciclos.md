# 6. Ciclos

## `enquanto`

Repete enquanto a condição for verdadeira. Se à partida for falsa, não corre nenhuma vez.

```portugol
inteiro contador = 0
enquanto (contador < 5) {
    contador++
}
```

## `faca ... enquanto`

Corre pelo menos uma vez e só depois testa a condição. É o ciclo certo para pedir uma resposta e validá-la depois de a receber.

```portugol
faca {
    escreva("Idade (18 ou mais): ")
    leia(idade)
} enquanto (idade < 18)
```

## `para`

Para quando sabes quantas voltas dar. As três partes ficam à vista: início, condição e passo.

```portugol
para (inteiro i = 0; i < 10; i++) {
    escreva("i = ", i, "\n")
}
```

O passo aceita `i = i + 1`, `i++` ou `i--`.

## `para cada`

Percorre os valores de um array, sem controlares o índice:

```portugol
inteiro[] numeros = {10, 20, 30}

para cada (inteiro n em numeros) {
    escreva("n = ", n, "\n")
}
```

O C não tem esta construção. O compilador gera um `for` com índice por ti.

## `pare` e `continue`

- `pare` sai do ciclo de vez (ou do `caso`, num `escolha`).
- `continue` salta só o resto da volta atual e passa à seguinte.

## Qual escolher

Usa `para` quando o número de voltas é conhecido. Usa `enquanto` quando depende de uma condição que pode nunca acontecer. Usa `faca ... enquanto` quando o bloco tem de correr pelo menos uma vez.

## Exercício

Escreve a tabuada de um número lido do teclado, de 1 a 10, com `para`. Depois faz um programa que peça números até o utilizador escrever 0, e mostre a soma de todos (usa `faca ... enquanto`).

Anterior: [Condições e escolha](05-condicoes-e-escolha.md). Próximo: [Funções](07-funcoes.md).
