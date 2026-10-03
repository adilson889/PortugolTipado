No PortugolTipado, a tipagem é estrita. Cada variável precisa ter o seu tipo declarado antes de ser usada. Os tipos fundamentais são:

* **inteiro**: Números inteiros (ex: 1, -5, 100).
* **real**: Números decimais de precisão simples (ex: 3.14, -0.5).
* **duplo**: Números decimais de precisão dupla para cálculos maiores.
* **caractere**: Um único caractere entre aspas simples (ex: 'A', '7').
* **texto**: Uma sequência de caracteres entre aspas duplas (ex: "Olá mundo").
* **logico**: Guarda estados booleanos, sendo `1` para verdadeiro e `0` para falso (ou `verdadeiro` e `falso`).

```portugol
funcao inicio()
{
    inteiro idade = 25
    real altura = 1.75
    texto nome = "Kixikila"
    caractere inicial = 'K'
    logico ativo = 1

    escreva("Nome: ", nome, "\n")
    escreva("Idade: ", idade, " anos\n")
}
```
