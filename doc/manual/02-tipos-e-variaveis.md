# 2. Tipos e variáveis

Em PortugolTipado toda a variável tem um tipo, e o tipo escreve-se sempre. Não há inferência: o compilador não adivinha.

## Os tipos

| PortugolTipado | C | Guarda |
|---|---|---|
| `inteiro` | `int` | número inteiro |
| `real` | `float` | decimal |
| `duplo` | `double` | decimal de precisão dupla |
| `texto` | `char*` | cadeia de caracteres |
| `caractere` | `char` | um caractere |
| `logico` | `int` (0 ou 1) | verdadeiro ou falso |
| `vazio` | `void` | só serve como tipo de retorno |
| `tempo` | `time_t` | um instante |

O C não tem booleano. O `logico` é um `int` onde 0 é falso e 1 é verdadeiro, e o PortugolTipado não inventa mais do que isso.

Alguns tipos levam duas palavras, como em C:

| PortugolTipado | C |
|---|---|
| `inteiro longo` | `long int` |
| `inteiro longo longo` | `long long int` |
| `inteiro curto` | `short int` |
| `inteiro positivo` | `unsigned int` |
| `duplo longo` | `long double` |

## Declarar variáveis

```portugol
inteiro x = 10
real altura = 1.75
texto nome = "Adilson"
caractere letra = 'A'
logico ativo = verdadeiro
```

Alguns pontos a ter presentes:

- Os decimais usam ponto: `1.75`.
- `verdadeiro` e `falso` também se escrevem `V` e `F`. Por isso `V` e `F` são reservados e não servem de nome de variável.
- Texto vai entre aspas duplas, um caractere entre aspas simples.

## Várias de uma vez

Do mesmo tipo, separadas por vírgulas:

```portugol
inteiro a, b, c
inteiro contador = 0, limite = 10
```

Ou em grupo, escrevendo o tipo uma só vez. A vírgula entre itens é opcional:

```portugol
inteiro { x, y, z }
real { r = 1.5, s }

const inteiro {
    LARGURA = 800
    ALTURA = 600
}
```

As chaves do grupo não criam um bloco novo: as variáveis valem no sítio onde o grupo está. Num grupo `const`, todos os itens precisam de valor, e um grupo vazio é erro.

## Constantes

`const` vai antes do tipo e exige valor inicial:

```portugol
const inteiro TAXA = 85
const inteiro notas[3] = {1, 2, 3}
```

Se te enganares, o compilador avisa:

| O que fizeste | Mensagem |
|---|---|
| `const` sem valor | `'const' precisa de um valor inicial (ex: const inteiro Z = 0)` |
| `const` na inicialização do `para` | `'const' não pode ser usado na inicialização do 'para' (a variável de controle muda a cada volta)` |
| Alterar uma constante (`TAXA = 2`, `TAXA++`, `notas[0] = 9`) | `não é possível alterar 'TAXA': foi declarada como 'const'` |
| Passar uma constante a um parâmetro `altere` | `'TAXA' foi declarada como 'const' e não pode ser passada para o parâmetro 'x' (alterar) da função 'dobrar'` |

Um limite: `const` não vale em campos de `estrutura`.

## Texto

Uma variável `texto` local recebe um espaço fixo de 256 caracteres. Não há `malloc` nem memória a libertar. Como ler texto do teclado está no [capítulo 3](03-escrever-e-ler.md).

## Exercício

Declara uma variável de cada tipo da primeira tabela (menos `vazio` e `tempo`) e tenta alterar uma delas depois de a marcares com `const`. Lê a mensagem de erro que sai.

Anterior: [Primeiros passos](01-primeiros-passos.md). Próximo: [Escrever e ler](03-escrever-e-ler.md).
