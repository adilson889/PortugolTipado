# Controlo de Fluxo em C — e como o PortugolTipado te leva até lá

Em C, o programa executa instruções sequencialmente, de cima para baixo, a menos que uma estrutura de controlo de fluxo altere esse percurso. As duas famílias principais são a decisão condicional (`if`/`else if`/`else`) e a seleção múltipla (`switch`/`case`), e ambas existem em PortugolTipado com os mesmos comportamentos, só com palavras-chave em português.

## Decisão condicional: `if` / `else if` / `else`

Em C, uma condição é avaliada, e o bloco correspondente é executado consoante o resultado seja verdadeiro ou falso:

```c
if (nota >= 10) {
    printf("Aluno aprovado!\n");
} else if (nota >= 7) {
    printf("Aluno em recurso.\n");
} else {
    printf("Aluno reprovado.\n");
}
```

Repare na ordem de avaliação: C testa as condições de cima para baixo, e executa apenas o **primeiro** bloco cuja condição seja verdadeira — os restantes são ignorados, mesmo que também fossem verdadeiros.

## A mesma estrutura, em PortugolTipado

```portugol
se (nota >= 10) {
    escreva("Aluno aprovado!\n")
} senao se (nota >= 7) {
    escreva("Aluno em recurso.\n")
} senao {
    escreva("Aluno reprovado.\n")
}
```

A correspondência é direta: `se` é `if`, `senao se` é `else if`, `senao` é `else`. A ordem de avaliação e o comportamento são exactamente os mesmos de C.

## Seleção múltipla: `switch` / `case`

Quando há várias opções possíveis para um único valor, C oferece o `switch`, que evita uma cadeia longa de `else if`:

```c
switch (opcao) {
    case 1:
        printf("Opcao 1 selecionada\n");
        break;
    case 2:
        printf("Opcao 2 selecionada\n");
        break;
    default:
        printf("Opcao invalida\n");
}
```

Um ponto que costuma confundir quem está a aprender C: **sem `break`, a execução continua para o caso seguinte** (um comportamento chamado *fall-through*). Esquecer o `break` é um dos erros mais comuns em código C, e é por isso que o PortugolTipado torna essa palavra-chave explícita e obrigatória em cada bloco.

## A mesma estrutura, em PortugolTipado

```portugol
escolha (opcao) {
    caso 1:
        escreva("Opcao 1 selecionada\n")
        pare
    caso 2:
        escreva("Opcao 2 selecionada\n")
        pare
    casocontrario:
        escreva("Opcao invalida\n")
}
```

`escolha` é `switch`, `caso` é `case`, `pare` é `break`, e `casocontrario` é `default`. A exigência de escrever `pare` em cada bloco não é uma limitação — é o PortugolTipado a lembrar-te, de forma explícita, de um detalhe que em C é fácil de esquecer e difícil de depurar depois.

## Exemplo completo

```portugol
funcao inicio()
{
    inteiro nota = 14

    se (nota >= 10) {
        escreva("Aluno aprovado!\n")
    } senao se (nota >= 7) {
        escreva("Aluno em recurso.\n")
    } senao {
        escreva("Aluno reprovado.\n")
    }

    inteiro opcao = 2
    escolha (opcao) {
        caso 1:
            escreva("Opcao 1 selecionada\n")
            pare
        caso 2:
            escreva("Opcao 2 selecionada\n")
            pare
        casocontrario:
            escreva("Opcao invalida\n")
    }
}
```

## Porque isto importa

Ao escrever `se`/`escolha` em PortugolTipado, estás a praticar exactamente a mesma lógica de decisão que usarás em C, Java, JavaScript ou qualquer outra linguagem com sintaxe próxima de C. O nome das palavras-chave muda; a estrutura de pensamento — avaliar uma condição, escolher um caminho, lembrar do `break` — é universal.