# 12. Portugol clássico

Se vens do Portugol Studio, os teus programas correm no PortugolTipado tal como estão, com alguns avisos.

Um ficheiro é tratado como clássico quando tem a extensão `.por`, ou quando começa (depois de espaços e comentários) por `programa {`.

## Exemplo

```portugol
programa {
    funcao inicio() {
        real nota

        faca {
            escreva("Digite a nota (0 a 10): ")
            leia(nota)
        } enquanto (nota < 0.0 ou nota > 10.0)

        escreva("Nota registrada: ", nota)
    }
}
```

## O que muda em relação ao PortugolTipado

| Portugol clássico | PortugolTipado |
|---|---|
| `programa { ... }` a envolver tudo | não existe |
| `cadeia` | `texto` |
| `caracter` | `caractere` |
| `e`, `ou`, `nao` | `&&`, `\|\|`, `!` |
| `caso contrario:` | `casocontrario:` |

No editor, estas formas aparecem com um aviso amarelo (`Portugol clássico: use '...'`). Funcionam, mas é melhor mudares para a forma nova quando mexeres no código.

## O que ainda não é aceite

No modo clássico, ainda não podes usar:

- o tipo de retorno antes do nome (`funcao inteiro soma(...)`); escreve `funcao soma(...): inteiro`;
- variáveis globais;
- `&` para passar por referência; usa `altere`;
- `inclua biblioteca X --> apelido`.

## Porquê tanta rigidez

O PortugolTipado não quer ser só um ambiente de ensino. Por isso código solto, típico de um ambiente educacional, gera erros de propósito, tal como acontece quando abres JavaScript num projeto TypeScript. O modo clássico existe para te deixar migrar com calma.

## Exercício

Pega num programa teu do Portugol Studio, guarda-o como `.por` e corre-o. Depois converte-o para `.port`, trocando as formas clássicas pelas novas, e confirma que o resultado é o mesmo.

Anterior: [Gráficos](11-graficos.md). Próximo: [Erros e avisos](13-erros-e-avisos.md).
