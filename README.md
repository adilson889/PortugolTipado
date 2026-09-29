# PortugolTipado

> Superconjunto de Portugol, fortemente tipado, que compila para C nativo e legível.

**PortugolTipado** leva a ideia do **Portugol** para além do ambiente educacional, adicionando **tipagem forte** e **compilação pra C nativo** — código real, legível e compilável, não interpretado.

Mantém a sintaxe familiar do Portugol, mas permite construir software de verdade usando todo o **ecossistema C**.

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
## Exemplo

```
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
## Estado

🚧 **Em desenvolvimento.**
* 40% Concluído 


## Licença

MIT