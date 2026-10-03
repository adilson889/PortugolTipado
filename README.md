
# PortugolTipado

> Superconjunto de Portugol, fortemente tipado, que compila para C nativo e legível.

**PortugolTipado** leva a ideia do **Portugol** para além do ambiente educacional, adicionando **tipagem forte** e **compilação pra C nativo** — código real, legível e compilável, não interpretado.

Mantém a sintaxe familiar do Portugol, mas permite construir software de verdade usando todo o **ecossistema C**.

Assim como o TypeScript trouxe tipagem estática ao JavaScript sem abandonar sua base, o PortugolTipado traz tipagem forte e estrutura mais próxima de C/Java ao Portugol — e compila para C nativo legível.

---
## Objetivos

| Objetivo | Descrição |
|---|---|
| **Superconjunto de Portugol** | Sintaxe familiar, sem curva de aprendizado |
| **Tipagem forte** | Todo valor tem tipo explícito, sem inferência mágica |
| **Compila para C nativo** | Gera código legível e compilável por `gcc` |
| **Compatível com o ecossistema C** | Usa bibliotecas C reais (SDL2, SQLite, math, etc.) |
| **Gera binário nativo** | Roda em qualquer plataforma com `gcc` |

---
## Comparação

| Aspecto | Portugol Studio | VisuAlg | Delégua | PortugolTipado |
|---|---|---|---|---|
| Sintaxe base | Português estruturado | Português (estilo Pascal) | Português multidialeto | Português tipado (estilo C/Java) |
| Tipagem | Fraca / implícita em vários casos | Tipada no dialeto VisuAlg | Dinâmica (com suporte a dialetos tipados) | Forte e explícita |
| Execução | Interpretado (JVM) | Interpretado | Interpretado (JS/TS) | Compila para C nativo |
| Código gerado | Não gera C | Não gera C | Pode traduzir em alguns dialetos | C legível, compilável com `gcc` |
| Entrada (`leia`) | Básica | Básica | Básica | Robusta (validação, retry, rejeita NaN/Inf) |
| Structs / registros | Limitado | Sim | Sim (conforme dialeto) | Sim (`estrutura`) |
| Passagem por referência | `&` em parâmetros | Sim | Conforme dialeto | `altere` (vira ponteiro no C) |
| Constantes | Parcial | Sim | Sim | `const` preservado no C gerado |
| Bibliotecas | Próprias (Graficos, Util, etc.) | Próprias | Dialetos + JS | Libs C reais + lib gráfica própria |
| Alias de biblioteca | `inclua biblioteca X --> g` | — | — | Forma curta: `inclua graficos` |
| Módulos / includes | Limitado | — | Sim | `inclua` (libs padrão e arquivos locais) |
| Público principal | Ensino de lógica | Ensino de lógica | Ensino e multidialeto | Além do educacional: software real em português |
| Compatibilidade com Portugol clássico | — | — | Parcial (dialetos) | Código simples roda; código solto gera erros de propósito |

---

## Exemplo

```portugol
estrutura Aluno {
    texto nome
    inteiro idade
}

funcao dobro(const inteiro x): inteiro {
    retorne x * 2
}

funcao acumular(altere inteiro total, const inteiro valor): vazio {
    total = total + valor
}

funcao inicio() {
    const inteiro TAXA = 85
    const inteiro notas[3] = {10, 15, 20}
    inteiro soma = 0

    para (inteiro i = 0; i < 3; i++) {
        acumular(soma, notas[i])
    }

    estrutura Aluno a = {"Ana", 25}

    escreva(a.nome, " tem ", a.idade, " anos\n")
    escreva("soma = ", soma, "\n")
    escreva("taxa em dobro = ", dobro(TAXA), "\n")

    se (soma > 40) {
        escreva("aprovado\n")
    } senao {
        escreva("reprovado\n")
    }
}
```

**Gera:**

```c
#include <stdio.h>

struct Aluno {
    char* nome;
    int idade;
};

int dobro(const int);
void acumular(int*, const int);

int dobro(const int x) {
    return x * 2;
}

void acumular(int *total, const int valor) {
    *total = *total + valor;
}

int main() {
    const int TAXA = 85;
    const int notas[3] = {10, 15, 20};
    int soma = 0;
    for (int i = 0; i < 3; i++) {
        acumular(&soma, notas[i]);
    }
    struct Aluno a = {"Ana", 25};
    printf("%s tem %d anos\n", a.nome, a.idade);
    printf("soma = %d\n", soma);
    printf("taxa em dobro = %d\n", dobro(TAXA));
    if (soma > 40) {
        printf("aprovado\n");
    } else {
        printf("reprovado\n");
    }
    return 0;
}
```

**Saída:**

```
Ana tem 25 anos
soma = 45
taxa em dobro = 170
aprovado
```

---
## C gerado com qualidade

O transpilador não faz só uma tradução sintática. Em vários pontos ele gera C mais robusto do que o código típico escrito à mão:

- `leia` vira funções auxiliares (`tc_ler_real`, `tc_ler_caractere`, etc.) com validação, rejeição de NaN/Inf e pedido de nova entrada em caso de erro — em vez de um `scanf` frágil
- `altere` vira ponteiro de forma explícita e legível
- `const` é preservado no C
- Funções da biblioteca gráfica recebem prefixo `tc_` para evitar conflitos de nome
- O resultado continua legível e compilável com `gcc`

---
## Relação com o Portugol Studio

O PortugolTipado herda as palavras-chave e a estrutura básica do Portugol Studio (`escreva`, `leia`, `inicio`, `se`, `enquanto`, etc.).

Código Portugol simples continua funcionando:

```
funcao inicio() {
    escreva("ola mundo")
}
```

Código mais solto, típico do ambiente educacional, passa a gerar erros de propósito — no mesmo espírito de abrir JavaScript em um projeto TypeScript. A tipagem forte e as regras mais rígidas são deliberadas: o objetivo é ir além do uso puramente educacional.

---

## Licença

GPL v3.0