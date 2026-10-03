O escopo define onde uma variável pode ser acessada. Variáveis criadas fora das funções são globais. Variáveis criadas dentro, são locais.
Para valores que não podem mudar durante a execução do programa, usa a palavra `const`.

```portugol
// Variável global constante
const inteiro LIMITE = 100
const real PI = 3.14159

funcao inicio()
{
    // Variável local
    inteiro valor = 50
    
    se (valor < LIMITE) {
        escreva("Dentro do limite permitido.\n")
    }
    
    escreva("Valor de PI: ", PI, "\n")
    
    // LIMITE = 200 // Erro: Não se pode alterar uma constante
}
```
