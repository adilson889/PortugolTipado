# 4. Operadores

## Os operadores

| Grupo | Operadores |
|---|---|
| Aritméticos | `+  -  *  /  %` |
| Atribuição | `=  +=  -=  *=  /=  %=  ++  --` |
| Comparação | `==  !=  >  <  >=  <=` |
| Lógicos | `&&  \|\|  !` |
| Unários | `-` (negativo) e `!` |

## Pontos que apanham quem começa

**A divisão de inteiros corta a parte decimal.** `7 / 2` dá `3`. Se um dos lados for decimal, o resultado é decimal: `7 / 2.0` dá `3.5`.

**O resto (`%`) só vale para inteiros.** Para decimais usa `resto()` da biblioteca `matematica`.

**`=` não é `==`.** Um atribui, o outro compara. Se escreveres `se (x = 5)` quando querias `se (x == 5)`, o resultado muda por completo.

**Textos comparam-se pelo conteúdo.** `==` e `!=` funcionam em `texto` e comparam o que está escrito, não o endereço na memória.

**Só `&&`, `||` e `!`.** Os símbolos `&` e `|` sozinhos não existem, porque não há ponteiros à vista. As formas `e`, `ou` e `nao` pertencem ao [modo clássico](12-portugol-classico.md).

## Quem se calcula primeiro

Da menor para a maior precedência:

1. `||`
2. `&&`
3. `==  !=`
4. `<  >  <=  >=`
5. `+  -`
6. `*  /  %`
7. operadores unários

Se tens dúvidas, põe parênteses. Não custam nada e o código fica mais claro.

## Exemplo

```portugol
inicio() {
    inteiro a = 10
    inteiro b = 3

    inteiro soma = a + b
    inteiro resto = a % b
    logico maior = (a > b)
    logico teste = (a == 10) && (b != 0)

    escreva("soma = ", soma, " resto = ", resto, "\n")
}
```

## Exercício

Escreve um programa que leia um inteiro e diga se é par ou ímpar, usando `%`. Depois faz com que diga também se é maior que 100.

Anterior: [Escrever e ler](03-escrever-e-ler.md). Próximo: [Condições e escolha](05-condicoes-e-escolha.md).
