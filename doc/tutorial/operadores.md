# Operadores em C, e a Ponte do PortugolTipado

Em C, operadores permitem realizar cálculos, comparar valores e avaliar condições lógicas. Dividem-se em três famílias principais: aritméticos, relacionais e lógicos — e cada uma tem um papel distinto na construção de expressões.

## Operadores aritméticos

| Operador em C | Operação | Exemplo |
|---|---|---|
| `+` | Soma | `a + b` |
| `-` | Subtração | `a - b` |
| `*` | Multiplicação | `a * b` |
| `/` | Divisão | `a / b` |
| `%` | Resto da divisão (módulo) | `a % b` |

O operador `%` é frequentemente esquecido por quem começa em C, mas é essencial em tarefas como verificar se um número é par (`n % 2 == 0`) ou limitar um valor a um intervalo.

## Operadores relacionais

| Operador em C | Significado | Exemplo |
|---|---|---|
| `==` | Igual a | `a == b` |
| `!=` | Diferente de | `a != b` |
| `>` | Maior que | `a > b` |
| `<` | Menor que | `a < b` |
| `>=` | Maior ou igual a | `a >= b` |
| `<=` | Menor ou igual a | `a <= b` |

Um erro clássico em C é confundir `=` (atribuição) com `==` (comparação) — o compilador aceita ambos numa condição, mas o resultado é completamente diferente. Esta distinção é uma das razões pelas quais a tipagem estrita do PortugolTipado ajuda: obriga a pensar com clareza sobre o que cada expressão representa.

## Operadores lógicos

| Operador em C | Significado | Exemplo |
|---|---|---|
| `&&` | E lógico (verdadeiro se ambos forem verdadeiros) | `a && b` |
| `\|\|` | OU lógico (verdadeiro se pelo menos um for verdadeiro) | `a \|\| b` |
| `!` | Negação (inverte o valor lógico) | `!a` |

## Exemplo em C

```c
int a = 10;
int b = 3;

int soma = a + b;
int resto = a % b;

int maior = (a > b);
int teste = (a == 10) && (b != 0);

printf("Soma: %d\n", soma);
```

## A mesma lógica, em PortugolTipado

```portugol
funcao inicio()
{
    inteiro a = 10
    inteiro b = 3

    inteiro soma = a + b
    inteiro resto = a % b

    logico maior = (a > b)
    logico teste = (a == 10) e (b != 0)

    escreva("Soma: ", soma, "\n")
}
```

A correspondência é directa: todos os operadores aritméticos e relacionais são idênticos aos de C — `+`, `-`, `*`, `/`, `%`, `==`, `!=`, `>`, `<`, `>=`, `<=` funcionam exactamente da mesma forma nas duas linguagens. A única diferença visível está nos operadores lógicos, onde o PortugolTipado aceita tanto a forma simbólica de C (`&&`, `||`, `!`) como a forma por extenso em português (`e`, `ou`, `nao`) — ambas geram o símbolo C correspondente ao compilar.

## Porque isto importa

Dominar operadores é dominar a gramática básica de qualquer linguagem derivada de C. São eles que transformam simples variáveis em cálculos, decisões e condições — a base sobre a qual se constroem estruturas de controlo, laços e toda a lógica de um programa. Praticar estes operadores em PortugolTipado, com a mesma precisão exigida por C, prepara directamente para o dia em que o código for escrito em C puro, Java, ou qualquer outra linguagem da mesma família.