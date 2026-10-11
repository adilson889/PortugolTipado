# 3. Escrever e ler

## `escreva`

Junta texto e valores separados por vírgulas. O formato do C (`%d`, `%f`, ...) é escolhido pelo tipo de cada valor, por isso não tens de o escrever.

```portugol
inteiro x = 10
escreva("x = ", x, "\n")
```

| Tipo | Formato no C |
|---|---|
| `inteiro` | `%d` |
| `real` | `%f` |
| `duplo` | `%lf` |
| `texto` | `%s` |
| `caractere` | `%c` |

`\n` muda de linha e `\t` põe um tabulador, como no C.

## `leia`

```portugol
inteiro idade
leia(idade)
```

O `leia` lê uma linha inteira e confere se tem o formato do tipo e se cabe no intervalo dele. Se a entrada for inválida, escreve `Entrada invalida, tente novamente: ` e pede outra vez. Se a entrada acabar (por exemplo um ficheiro sem mais linhas), o programa termina com erro.

Por trás, o compilador gera funções auxiliares com nomes como `tc_ler_real`, em vez de um `scanf` frágil. Também rejeita valores como NaN e infinito.

### Ler texto

Para ler texto, declara a variável sem valor inicial:

```portugol
inicio() {
    texto nome
    escreva("Como te chamas? ")
    leia(nome)
    escreva("Olá, ", nome, "\n")
}
```

Isto importa porque uma variável `texto` com valor inicial (`texto nome = "Ana"`) aponta para um texto fixo que não se pode escrever por cima. Sem valor inicial, o compilador reserva um espaço próprio de 256 caracteres onde o `leia` pode gravar. Se a linha escrita for maior que o espaço, o programa pede outra vez.

## Repetir até acertar

Um padrão que usarás muitas vezes é pedir até a resposta ser válida:

```portugol
inteiro idade
faca {
    escreva("Idade (18 ou mais): ")
    leia(idade)
} enquanto (idade < 18)
```

## Exercício

Pede dois números inteiros e mostra a soma. Depois pede um nome e mostra-o numa frase. Experimenta escrever letras onde devia ir um número e vê o que acontece.

Anterior: [Tipos e variáveis](02-tipos-e-variaveis.md). Próximo: [Operadores](04-operadores.md).
