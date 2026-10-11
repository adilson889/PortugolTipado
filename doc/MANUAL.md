# Manual de referência

Uma página para consulta rápida. Para aprender, segue os capítulos em [manual/](manual/) a partir do [índice](README.md).

Regra de ouro: quando o Portugol clássico e o C discordam, vale o C. Os erros saem em português, antes de chegar ao `gcc`.

## Programa mínimo

```portugol
inicio() {
    escreva("Olá, mundo\n")
}
```

Também se aceita `funcao inicio() { ... }`. Não se escreve `;`. Fora de `inicio` só existem `inclua`, `estrutura` e `funcao`.

## Tipos

| PortugolTipado | C | Guarda |
|---|---|---|
| `inteiro` | `int` | número inteiro |
| `real` | `float` | decimal |
| `duplo` | `double` | decimal de precisão dupla |
| `texto` | `char*` | cadeia de caracteres |
| `caractere` | `char` | um caractere |
| `logico` | `int` (0/1) | `verdadeiro` ou `falso` (também `V` e `F`) |
| `vazio` | `void` | só como retorno |
| `tempo` | `time_t` | um instante |

Compostos: `inteiro longo`, `inteiro longo longo`, `inteiro curto`, `inteiro positivo`, `duplo longo`.

## Declarações

```portugol
inteiro x = 10
inteiro a, b, c
inteiro { x, y, z }
const inteiro TAXA = 85
inteiro[] numeros = {1, 2, 3}
inteiro grade[2][2]
estrutura Pessoa p = {"Ana", 25}
```

## Operadores

| Grupo | Operadores |
|---|---|
| Aritméticos | `+  -  *  /  %` |
| Atribuição | `=  +=  -=  *=  /=  %=  ++  --` |
| Comparação | `==  !=  >  <  >=  <=` |
| Lógicos | `&&  \|\|  !` |

Precedência, da menor para a maior: `||`, `&&`, `== !=`, `< > <= >=`, `+ -`, `* / %`, unários.

## Controlo de fluxo

```portugol
se (c) { } senao se (c) { } senao { }
escolha (x) { caso 1: ... pare  casocontrario: ... }
enquanto (c) { }
faca { } enquanto (c)
para (inteiro i = 0; i < n; i++) { }
para cada (inteiro v em vetor) { }
pare
continue
```

## Funções

```portugol
funcao somar(inteiro a, inteiro b): inteiro {
    retorne a + b
}

funcao dobrar(altere inteiro x): vazio {
    x = x * 2
}
```

Função externa, com apelido: `funcao raiz(real x): real = sqrt`.

## Bibliotecas

| `inclua` | Capítulo |
|---|---|
| `matematica`, `texto`, `caracteres`, `tempo` | [9](manual/09-bibliotecas-padrao.md) |
| `graficos` | [11](manual/11-graficos.md) |
| `"ficheiro"`, `nome` de pacote, biblioteca C | [10](manual/10-modulos-e-pacotes.md) |

`stdlib` e `stdio` são automáticas: nunca se escreve `inclua` para elas.

## Compilar

```
java -jar core/build/libs/port.jar -o programa.c programa.port
gcc programa.c -o programa
```

Opções do compilador: `-o saida.c`, `-p pasta_pacotes`, `--tipagem=segura` (verificação de tipos extra, opcional).
