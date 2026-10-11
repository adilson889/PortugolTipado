# 7. Funções

## Declarar e chamar

```portugol
funcao somar(inteiro a, inteiro b): inteiro {
    retorne a + b
}

funcao saudar(): vazio {
    escreva("Olá!\n")
}

inicio() {
    saudar()
    inteiro resultado = somar(5, 7)
    escreva("resultado = ", resultado, "\n")
}
```

O nome vem logo a seguir a `funcao`. O tipo de retorno vem no fim, depois de `:`. Quando a função não devolve nada, usa `vazio`, onde o C usaria `void`. O tipo de retorno escreve-se sempre.

Podes declarar as funções por qualquer ordem: uma pode usar outra que só aparece mais abaixo. Dentro de uma função de tipo `vazio`, `retorne` sem valor é aceite antes do `}`.

## Passar por referência: `altere`

Por defeito, o parâmetro é uma cópia. Se a função mudar o valor, a variável de quem chamou fica igual. Com `altere`, a função mexe na variável original:

```portugol
funcao dobrar(altere inteiro x): vazio {
    x = x * 2
}

inicio() {
    inteiro numero = 10
    dobrar(numero)
    escreva(numero, "\n")
}
```

Escreve `20`. Na chamada não se põe `&` e na função não se põe `*`. O compilador trata disso e gera o ponteiro no C.

As formas antigas `altera` e `alterar` ainda funcionam, mas o editor avisa com `obsoleto: use 'altere'`.

## Parâmetros `const`

```portugol
funcao dobro(const inteiro x): inteiro {
    retorne x * 2
}
```

Um parâmetro não pode ser `const` e `altere` ao mesmo tempo. Se tentares, sai o erro `'const' e 'alterar' não podem ser usados juntos no mesmo parâmetro`.

## Funções de bibliotecas C

Uma declaração sem corpo descreve uma função que já existe numa biblioteca C. É o assunto do [capítulo 10](10-modulos-e-pacotes.md).

## Exercício

Escreve `maior(inteiro a, inteiro b): inteiro`. Depois escreve `troca(altere inteiro a, altere inteiro b): vazio` que troque os valores de duas variáveis, e testa-a em `inicio`.

Anterior: [Ciclos](06-ciclos.md). Próximo: [Arrays e estruturas](08-arrays-e-estruturas.md).
