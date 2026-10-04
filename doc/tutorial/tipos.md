Agora entendi a virada de perspectiva — o protagonista do texto é **C**, e o PortugolTipado entra como a "tradução"/dicionário que ajuda a entender C mais rápido. É o inverso do que escrevi: em vez de "aqui está Portugol, e isto é o que ele vira em C", é **"aqui está C de verdade, e o PortugolTipado é como você chega lá mais fácil"**.

# Tipos em C — e como o PortugolTipado te leva até lá

C é uma linguagem de tipagem estática: toda variável precisa de um tipo declarado em tempo de compilação, e esse tipo nunca muda durante a execução. É uma das razões pelas quais C é rápido e previsível — o compilador sabe exatamente quanto espaço de memória cada variável ocupa, antes mesmo do programa rodar.

O problema é que a sintaxe de C para declarar tipos pode ser árida pra quem está começando: `char*`, `float`, especificadores de formato como `%d`/`%f`/`%s` no `printf`. O **PortugolTipado** existe como ponte — você escreve a mesma lógica, com os mesmos tipos reais de C, só que com nomes em português. Não é uma linguagem "de brincadeira" por cima: é um dicionário vivo que gera C de verdade, linha a linha.

## Os tipos nativos de C

| Tipo em C | Tamanho típico | O que guarda | Como se escreve em PortugolTipado |
|---|---|---|---|
| `int` | 4 bytes | Números inteiros | `inteiro` |
| `float` | 4 bytes | Decimal de precisão simples | `real` |
| `double` | 8 bytes | Decimal de precisão dupla | `duplo` |
| `char*` | ponteiro (8 bytes em 64-bit) | Cadeia de caracteres | `texto` |
| `char` | 1 byte | Um único caractere | `caractere` |
| `int` (como booleano) | 4 bytes | `1` (verdadeiro) ou `0` (falso) | `logico` |

Repare que C **não tem** um tipo booleano nativo — usa `int` com a convenção de que zero é falso e qualquer outro valor é verdadeiro. O PortugolTipado preserva essa realidade: `logico` vira `int` no C gerado, não inventa um tipo que C não tem.

## Declarando em C

```c
int idade = 25;
float altura = 1.75;
double precisao = 3.1415926535;
char* nome = "Adilson";
char inicial = 'A';
int ativo = 1;
```

## A mesma declaração, em PortugolTipado

```portugol
inteiro idade = 25
real altura = 1.75
duplo precisao = 3.1415926535
texto nome = "Adilson"
caractere inicial = 'A'
logico ativo = 1
```

Sem ponto e vírgula, sem `char*` pra lembrar, mas gerando exatamente a mesma declaração de C ao compilar.

## Exibindo valores: `printf` e seus especificadores

Em C, cada tipo exige um especificador de formato específico no `printf` — errar o especificador é um dos bugs mais comuns de quem está aprendendo:

```c
printf("Nome: %s\n", nome);         // %s para char*
printf("Idade: %d\n", idade);       // %d para int
printf("Altura: %f\n", altura);     // %f para float
printf("Precisao: %lf\n", precisao); // %lf para double
printf("Inicial: %c\n", inicial);   // %c para char
```

No PortugolTipado, a função `escreva` cuida disso por você — ela sabe o tipo de cada variável e escolhe o especificador certo automaticamente:

```portugol
escreva("Nome: ", nome, "\n")
escreva("Idade: ", idade, "\n")
escreva("Altura: ", altura, "\n")
escreva("Precisao: ", precisao, "\n")
escreva("Inicial: ", inicial, "\n")
```

## Por que isso importa

Quem aprende tipos através do PortugolTipado não está aprendendo uma linguagem paralela: está aprendendo **os tipos reais de C**, com os tamanhos reais, as conversões reais, os mesmos especificadores de formato por trás dos panos. Quando o momento de ler ou escrever C puro chegar, o conceito já está formado — só muda o nome da palavra-chave.