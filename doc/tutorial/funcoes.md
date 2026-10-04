# Funções e Procedimentos em C, e a Ponte do PortugolTipado

Em C, funções são a unidade fundamental de organização e reutilização de código. Uma função pode devolver um valor ao código que a chamou, através da instrução `return`, ou pode executar uma tarefa sem devolver nada — nesse caso, o seu tipo de retorno é `void`.

```c
void saudacao(char* nome) {
    printf("Ola, %s!\n", nome);
}

int somar(int a, int b) {
    return a + b;
}
```

A assinatura de uma função em C declara três coisas: o **tipo de retorno** (o que ela devolve, ou `void` se não devolver nada), o **nome** da função, e os **parâmetros** que recebe, cada um com o seu próprio tipo.

## Chamando uma função

Uma função só é executada quando é chamada. No caso de `somar`, o valor devolvido pode ser guardado numa variável:

```c
saudacao("Adilson");
int resultado = somar(5, 7);
printf("O resultado e: %d\n", resultado);
```

## A mesma lógica, em PortugolTipado

```portugol
funcao saudacao(texto nome): vazio
{
    escreva("Olá, ", nome, "!\n")
}

funcao somar(inteiro a, inteiro b): inteiro
{
    retorne a + b
}

funcao inicio()
{
    saudacao("Adilson")
    inteiro resultado = somar(5, 7)
    escreva("O resultado é: ", resultado, "\n")
}
```

A correspondência é directa: o tipo de retorno em C aparece antes do nome da função; em PortugolTipado, aparece depois dos parênteses, separado por dois pontos (`: inteiro`, `: vazio`). `retorne` é `return`. Uma função sem retorno usa `vazio`, exactamente onde C usaria `void`. Ao compilar, cada função em PortugolTipado gera a assinatura C equivalente, byte a byte compatível com o que um programador C escreveria à mão.

## Boas práticas

Separar um programa em funções pequenas e com um propósito único — como `saudacao` e `somar` — torna o código mais fácil de ler, testar e reutilizar. É uma disciplina válida em qualquer linguagem derivada de C, e vale a pena praticá-la desde o início, mesmo em programas pequenos.