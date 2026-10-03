As estruturas (`estrutura`) permitem criar tipos de dados personalizados, agrupando variáveis de tipos diferentes debaixo de um único nome.

```portugol
estrutura Produto {
    texto nome
    real preco
    inteiro estoque
}

funcao inicio()
{
    estrutura Produto p = {"Teclado", 150.50, 20}
    
    escreva("Produto: ", p.nome, "\n")
    escreva("Preço: Kz ", p.preco, "\n")
    
    // Alterando um campo
    p.estoque = p.estoque - 1
    escreva("Estoque atualizado: ", p.estoque, "\n")
}
```
