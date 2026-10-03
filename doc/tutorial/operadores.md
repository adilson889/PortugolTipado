Os operadores permitem fazer cálculos, comparações e avaliar condições.

* **Aritméticos:** `+` (soma), `-` (subtração), `*` (multiplicação), `/` (divisão), `%` (resto da divisão).
* **Relacionais:** `==` (igual), `!=` (diferente), `>` (maior), `<` (menor), `>=` (maior ou igual), `<=` (menor ou igual).
* **Lógicos:** `e` / `&&` (E lógico), `ou` / `||` (OU lógico), `nao` / `!` (Negação).

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
