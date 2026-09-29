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
funcao dobro(const inteiro x): inteiro {
    retorne x * 2
}

funcao inicio() {
    const inteiro TAXA = 85
    const inteiro notas[3] = {1, 2, 3}
    inteiro y = dobro(TAXA)
    escreva(y)
}
```

Gera:

```c
#include <stdio.h>

int dobro(const int);

int dobro(const int x) {
    return x * 2;
}

int main() {
    const int TAXA = 85;
    const int notas[3] = {1, 2, 3};
    int y = dobro(TAXA);
    printf("%d", y);
    return 0;
}
```

---
## Estado

🚧 **Em desenvolvimento.**
* 50% Concluído 


## Licença

MIT