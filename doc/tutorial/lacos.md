Os laços permitem executar um bloco de código repetidas vezes. Existem três estruturas principais: `enquanto`, `faca ... enquanto` (que executa pelo menos uma vez) e `para`.

```portugol
funcao inicio()
{
    escreva("Contagem com enquanto:\n")
    inteiro contador = 1
    enquanto (contador <= 3) {
        escreva(contador, "\n")
        contador++
    }

    escreva("Contagem com para:\n")
    para (inteiro i = 1; i <= 3; i++) {
        escreva(i, "\n")
    }
}
```
