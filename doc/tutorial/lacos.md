# Laços de Repetição em C, e a Ponte do PortugolTipado

Em C, um laço de repetição permite executar um bloco de código várias vezes, sem duplicar essas instruções no texto do programa. Existem três estruturas principais, cada uma adequada a um cenário diferente: `while`, `do...while` e `for`.

## `while`: repetição condicional

O laço `while` testa a condição **antes** de cada execução do bloco. Se a condição for falsa logo à partida, o bloco nunca chega a executar:

```c
int contador = 1;
while (contador <= 3) {
    printf("%d\n", contador);
    contador++;
}
```

## `do...while`: executa pelo menos uma vez

O laço `do...while` testa a condição **depois** de cada execução, o que garante que o bloco corre pelo menos uma vez, mesmo que a condição já comece falsa:

```c
int contador = 1;
do {
    printf("%d\n", contador);
    contador++;
} while (contador <= 3);
```

## `for`: contagem com inicialização, condição e incremento

O laço `for` reúne três partes numa só linha: a inicialização da variável de controlo, a condição de paragem, e o incremento — tornando-o a escolha natural quando se sabe, à partida, quantas vezes o laço deve repetir:

```c
for (int i = 1; i <= 3; i++) {
    printf("%d\n", i);
}
```

## A mesma lógica, em PortugolTipado

```portugol
funcao inicio()
{
    escreva("Contagem com enquanto:\n")
    inteiro contador = 1
    enquanto (contador <= 3) {
        escreva(contador, "\n")
        contador++
    }

    escreva("Contagem com para:\n")
    para (inteiro i = 1; i <= 3; i++) {
        escreva(i, "\n")
    }
}
```

A correspondência é directa: `enquanto` é `while`, `faca ... enquanto` é `do...while`, e `para` é `for` — com a mesma estrutura de três partes entre parênteses. Ao compilar, cada laço em PortugolTipado gera o laço C equivalente, com o mesmo comportamento e o mesmo desempenho.

## Escolhendo o laço certo

A escolha entre `while`, `do...while` e `for` não é arbitrária: reflecte a natureza do problema. Usa `para`/`for` quando o número de repetições é conhecido à partida; usa `enquanto`/`while` quando a repetição depende de uma condição que pode nunca se verificar; usa `faca...enquanto`/`do...while` quando o bloco precisa de correr pelo menos uma vez antes de qualquer verificação — por exemplo, ao pedir dados a um utilizador e validar a resposta depois de a receber.