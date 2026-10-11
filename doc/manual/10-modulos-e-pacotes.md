# 10. Módulos e pacotes

Um programa raramente vive num ficheiro só. Este capítulo mostra como dividir o código em ficheiros, usar bibliotecas C e descrever bibliotecas para o PortugolTipado as entender.

## As formas de `inclua`

| Forma | O que faz |
|---|---|
| `inclua matematica` | biblioteca padrão ([capítulo 9](09-bibliotecas-padrao.md)) |
| `inclua "base/utilitarios"` | ficheiro `.port` teu, relativo ao ficheiro que inclui |
| `inclua "sqlite.declare"` | ficheiro de declarações (`sqlite.declare.port`) |
| `inclua nome` | pacote instalado em `pacotes/nome/` |
| `inclua SDL2/SDL` | biblioteca C que não está no mapa padrão |

Quando escreves `inclua nome` sem aspas, o compilador tenta por esta ordem: biblioteca padrão, pacote instalado e, por fim, trata como biblioteca C (gera `#include <nome.h>`).

## Módulos locais

Um ficheiro `.port` sem `inicio` é um módulo. Inclui-se com o caminho entre aspas e sem a extensão:

```portugol
inclua "base/core/base_core"
```

As funções e estruturas do módulo ficam visíveis no ficheiro que o inclui. Um módulo pode incluir outros.

## Bibliotecas C

Uma biblioteca C que não está no mapa padrão entra pelo nome do cabeçalho, sem `.h`. Pode ter barras, e o nome fica como o escreveste:

```portugol
inclua SDL2/SDL
```

Gera `#include <SDL2/SDL.h>`. Para chamares as funções dela, tens de as declarar.

## Funções externas

Uma declaração sem corpo (sem `{}`) diz ao compilador que a função já existe numa biblioteca C trazida por `inclua`. Não gera protótipo nem implementação: a função já está no cabeçalho real.

Sem apelido, só regista o tipo e a chamada usa o nome real:

```portugol
funcao SDL_Init(inteiro flags): inteiro
```

### Apelidos

Com apelido, dás à função externa um nome teu, curto ou em português. O nome real fica depois do `=`:

```
funcao <apelido>(<tipo> <param>, ...): <tipo_retorno> = <NomeRealNaBiblioteca>
```

Exemplo completo:

```portugol
inclua SDL2/SDL

funcao SDL_inicie(inteiro flags): inteiro = SDL_Init
funcao SDL_crie_janela(texto titulo, inteiro x, inteiro y, inteiro w, inteiro h, inteiro flags): inteiro = SDL_CreateWindow
funcao SDL_destrua_janela(inteiro janela): vazio = SDL_DestroyWindow

inicio() {
    SDL_inicie(32)
    inteiro janela = SDL_crie_janela("Meu Jogo", 100, 100, 800, 600, 4)
    SDL_destrua_janela(janela)
}
```

O C gerado usa os nomes reais:

```c
#include <SDL2/SDL.h>

int main() {
    SDL_Init(32);
    int janela = SDL_CreateWindow("Meu Jogo", 100, 100, 800, 600, 4);
    SDL_DestroyWindow(janela);
    return 0;
}
```

Regras dos apelidos:

- O nome real, à direita do `=`, tem de ser exatamente igual ao da biblioteca, com as mesmas maiúsculas e minúsculas. O PortugolTipado não traduz nem adivinha. Se errares, quem reclama é o linker do C e não o PortugolTipado.
- Não há apelidos automáticos. Escreves um por cada função que queiras usar.
- O apelido pode ser qualquer identificador válido, com ou sem o prefixo da biblioteca, em português ou noutra língua.

## Ficheiros `.declare.port`

Se vais usar muitas funções de uma biblioteca, repetir as declarações em cada programa cansa. Um ficheiro `.declare.port` junta-as num só sítio. Serve para descrever uma biblioteca C sem a implementar.

Um ficheiro `matematica_c.declare.port`, por exemplo:

```portugol
funcao raiz(real x): real = sqrt
funcao potencia(real x, real y): real = pow
```

O que aceita:

- declarações de funções externas, sem corpo, com ou sem apelido;
- estruturas.

O que recusa, com erro:

- `inicio`: `um ficheiro '.declare.port' só declara funções de biblioteca: não pode ter 'inicio'`;
- uma função com corpo: `a função 'X' tem corpo: num ficheiro '.declare.port' só se declara a função, sem corpo`.

Para o usares num programa, inclui-o com aspas e sem a extensão `.port`:

```portugol
inclua "matematica_c.declare"
```

O compilador carrega `matematica_c.declare.port`, relativo ao ficheiro que o inclui. Não gera `.h` próprio para ele, porque a biblioteca real já tem o seu cabeçalho.

## Pacotes

Um pacote é uma pasta que traz uma biblioteca já descrita, pronta a incluir só pelo nome. Quando escreves `inclua nome`, o compilador procura `pacotes/nome/`. Essa pasta fica ao lado do teu `.port`, ou noutro sítio se usares a opção `-p`:

```
java -jar core/build/libs/port.jar -p /caminho/dos/pacotes -o programa.c programa.port
```

Dentro da pasta do pacote há um `pacote.json`:

```json
{
  "versao": "1.0",
  "cabecalho": "sqlite3",
  "requisitos": ["-lsqlite3"],
  "declara": ["sqlite.declare.port"]
}
```

| Campo | Para quê |
|---|---|
| `versao` | a versão do pacote |
| `cabecalho` | o cabeçalho C que o programa passa a incluir, escrito como num `inclua` (sem `.h`) |
| `requisitos` | o que a ligação do C precisa, como `-lsqlite3`. Aparece num comentário no topo do C gerado |
| `declara` | os ficheiros `.declare.port` do pacote. Se faltar, o compilador procura `nome.declare.port` |

Com várias versões, cada uma numa subpasta com o seu `pacote.json`, o compilador usa a mais recente.

O nome do pacote só pode ter letras, números e `_`. Os ficheiros de `declara` têm de estar dentro da pasta do pacote, senão o compilador recusa.

Os requisitos de ligação são o que precisas de passar ao `gcc`. Se o programa saiu com `-lsqlite3` no topo, compilas assim:

```
gcc programa.c -o programa -lsqlite3
```

## Exercício

Escreve um módulo `util.port` com uma função `quadrado(inteiro x): inteiro` e inclui-o num programa com `inclua "util"`. Depois cria `minha.declare.port` com a linha `funcao raiz_de(duplo x): duplo = sqrt`, inclui-o com `inclua "minha.declare"` e usa `raiz_de(16.0)` no programa.

Anterior: [Bibliotecas padrão](09-bibliotecas-padrao.md). Próximo: [Gráficos](11-graficos.md).
