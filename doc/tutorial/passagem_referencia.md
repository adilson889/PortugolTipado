Por padrão, quando passas uma variável para uma função, o PortugolTipado faz uma cópia do valor. Se precisares que a função modifique a variável original externa, usa a palavra reservada `altere` nos parâmetros.

```portugol
funcao dobrar(altere inteiro valor): vazio
{
    // Modifica diretamente a variável original
    valor = valor * 2
}

funcao inicio()
{
    inteiro numero = 10
    escreva("Antes: ", numero, "\n")
    
    dobrar(numero) // Não é necessário usar símbolos especiais na chamada
    
    escreva("Depois: ", numero, "\n")
}
```
