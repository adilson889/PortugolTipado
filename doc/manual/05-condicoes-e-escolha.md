# 5. Condições e escolha

## `se`, `senao se`, `senao`

```portugol
se (idade >= 18) {
    escreva("maior\n")
} senao se (idade >= 13) {
    escreva("adolescente\n")
} senao {
    escreva("criança\n")
}
```

Os parênteses à volta da condição são obrigatórios. As condições avaliam-se de cima para baixo e só corre o primeiro bloco que servir.

A condição tem de ser um valor `logico` ou o resultado de uma comparação. Com `--tipagem=segura` o compilador exige isso mesmo e recusa, por exemplo, um texto como condição.

## `escolha`

Quando há várias opções para o mesmo valor, o `escolha` é mais claro que uma fila de `senao se`:

```portugol
escolha (opcao) {
    caso 1:
        escreva("um\n")
        pare
    caso 2:
        escreva("dois\n")
        pare
    casocontrario:
        escreva("outro\n")
}
```

O `pare` é obrigatório no fim de cada `caso`. Sem ele, o C continuaria no caso seguinte (é o chamado *fall-through*), que é uma das fontes mais comuns de erros em C. Aqui a palavra aparece escrita para ninguém se esquecer dela.

O `escolha` também funciona com `texto`. Como o `switch` do C não aceita texto, o compilador traduz para uma cadeia de comparações de texto.

## Exercício

Faz um programa que leia um número de 1 a 7 e mostre o dia da semana, com `escolha`. Usa o `casocontrario` para números fora do intervalo. Depois reescreve-o com `senao se` e compara qual fica mais legível.

Anterior: [Operadores](04-operadores.md). Próximo: [Ciclos](06-ciclos.md).
