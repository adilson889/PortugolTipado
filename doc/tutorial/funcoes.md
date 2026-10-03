Para organizar o código, podes criar blocos reutilizáveis. Funções retornam valores através do comando `retorne`. Procedimentos (que não devolvem valores) usam o tipo de retorno `vazio`.

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
