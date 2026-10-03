Vetores (arrays) guardam múltiplos valores do mesmo tipo. As matrizes são vetores de duas dimensões (tabelas). O índice inicial é sempre `0`.

```portugol
funcao inicio()
{
    // Vetor unidimensional
    inteiro[] numeros = {10, 20, 30}
    escreva("Primeiro número: ", numeros[0], "\n")

    // Matriz bidimensional (2 linhas, 2 colunas)
    inteiro[][] matriz = {
        {1, 2},
        {3, 4}
    }
    escreva("Elemento na linha 1, coluna 0: ", matriz[1][0], "\n")
    
    // Percorrendo vetores com "para cada"
    para cada (inteiro n em numeros) {
        escreva("Valor: ", n, "\n")
    }
}
```
