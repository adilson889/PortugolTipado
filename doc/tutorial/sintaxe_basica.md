# Estrutura Básica de um Programa em C , que e a Ponte do PortugolTipado

Em C, todo programa começa obrigatoriamente pela função `main()`. É o ponto de entrada: o sistema operativo procura exactamente por esta função para saber onde começar a executar o código. Os blocos de código são delimitados por chaves `{ }`, e **cada instrução termina obrigatoriamente com ponto e vírgula** `;` — omiti-lo é um dos erros de sintaxe mais comuns para quem começa em C.

```c
int main() {
    printf("O programa comeca aqui!\n");
    return 0;
}
```

Repare também no `return 0;` no final: por convenção, `main()` devolve um `int` ao sistema operativo, onde `0` significa que o programa terminou sem erros.

## Comentários em C

Comentários documentam o código e são completamente ignorados pelo compilador — servem apenas para quem lê o código, nunca afectam o programa em execução. C suporta duas formas:

```c
// Isto e um comentario de uma linha

/*
   Isto e um comentario
   de multiplas linhas
*/
```

## A mesma lógica, em PortugolTipado

```portugol
// Este é um comentário de uma linha

/* 
   Este é um comentário
   de múltiplas linhas 
*/

funcao inicio()
{
    escreva("O programa começa aqui!\n")
}
```

A correspondência é directa: `funcao inicio()` é o equivalente a `int main()`, e ao compilar gera exactamente essa assinatura em C, incluindo o `return 0;` final, acrescentado automaticamente. Os comentários `//` e `/* */` são idênticos nas duas linguagens — são, de facto, o mesmo mecanismo, sem qualquer tradução. A única diferença visível é o ponto e vírgula: o PortugolTipado não o exige no fim de cada linha, mas o compilador insere-o correctamente em cada instrução ao gerar o código C.

## Porque isto importa

A estrutura mínima de um programa — onde começa, onde termina, como se documenta — é a primeira coisa que qualquer programador C precisa de interiorizar, porque está presente em absolutamente todos os programas, dos mais simples aos mais complexos. Aprender esta estrutura em PortugolTipado significa aprender, desde a primeira linha, a forma exacta como um programa C é organizado — sem a fricção inicial da pontuação obrigatória, mas sem esconder o que realmente acontece por baixo.