# 9. Bibliotecas padrão

As bibliotecas padrão trazem funções do C com nomes em português. Para usar uma, escreves `inclua` no início do ficheiro:

```portugol
inclua matematica
```

O nome do C direto (`inclua math`) também é aceite. Se usares uma função sem o `inclua` certo, o compilador diz-te qual falta:

```
'raiz' é da biblioteca 'matematica': falta 'inclua matematica' no início do ficheiro
```

| `inclua` | Cabeçalho C | Conteúdo |
|---|---|---|
| `matematica` | `math.h` | contas e funções matemáticas |
| `texto` | `string.h` | tamanho, cópia, junção e comparação de texto |
| `caracteres` | `ctype.h` | testar e converter caracteres |
| `tempo` | `time.h` | tempo e datas |
| `graficos` | `graficos.h` | janela e desenho ([capítulo 11](11-graficos.md)) |

As bibliotecas `stdlib` e `stdio` são automáticas e não se incluem. As funções delas estão no fim deste capítulo.

## `matematica`

Só o nome muda. As assinaturas seguem o C e trabalham com `duplo`.

| PortugolTipado | C | Faz |
|---|---|---|
| `raiz(x)` | `sqrt` | raiz quadrada |
| `raiz_cubica(x)` | `cbrt` | raiz cúbica |
| `potencia(x, y)` | `pow` | x elevado a y |
| `absoluto(x)` | `fabs` | valor absoluto (decimal) |
| `piso(x)` | `floor` | arredonda para baixo |
| `teto(x)` | `ceil` | arredonda para cima |
| `arredondar(x)` | `round` | arredonda ao mais próximo |
| `truncar(x)` | `trunc` | corta a parte decimal |
| `seno(x)` | `sin` | seno (radianos) |
| `cosseno(x)` | `cos` | cosseno |
| `tg(x)` | `tan` | tangente |
| `arcsen(x)` | `asin` | arco seno |
| `arcos(x)` | `acos` | arco cosseno |
| `arctg(x)` | `atan` | arco tangente |
| `arctg2(y, x)` | `atan2` | arco tangente de y/x |
| `senh(x)` | `sinh` | seno hiperbólico |
| `cosh(x)` | `cosh` | cosseno hiperbólico |
| `tgh(x)` | `tanh` | tangente hiperbólica |
| `exponencial(x)` | `exp` | e elevado a x |
| `logaritmo(x)` | `log` | logaritmo natural |
| `logaritmo10(x)` | `log10` | logaritmo na base 10 |
| `logaritmo2(x)` | `log2` | logaritmo na base 2 |
| `resto(x, y)` | `fmod` | resto da divisão decimal |
| `hipotenusa(x, y)` | `hypot` | raiz de x² + y² |
| `copiar_sinal(x, y)` | `copysign` | x com o sinal de y |
| `eh_nan(x)` | `isnan` | é "não-número"? |
| `eh_infinito(x)` | `isinf` | é infinito? |
| `eh_finito(x)` | `isfinite` | é finito? |

Também ficam disponíveis as constantes `PI` (`M_PI`) e `E` (`M_E`), que igualmente exigem `inclua matematica`.

```portugol
inclua matematica

inicio() {
    duplo raio = 5.0
    duplo area = PI * potencia(raio, 2.0)
    escreva("área = ", area, "\n")
}
```

## `texto`

| PortugolTipado | C | Faz |
|---|---|---|
| `tamanho(s)` | `strlen` | tamanho do texto |
| `copie(destino, fonte)` | `strcpy` | copia |
| `junte(destino, fonte)` | `strcat` | acrescenta no fim |
| `compare(a, b)` | `strcmp` | compara (0 quando são iguais) |

O `==` e o `!=` também comparam textos pelo conteúdo, por isso muitas vezes nem precisas do `compare`.

O destino de `copie` e `junte` tem de ser uma variável `texto` local declarada sem valor inicial, para o compilador saber o espaço que tem. Em C isto evita escrever para lá do fim do texto.

```portugol
inclua texto

inicio() {
    texto frase
    copie(frase, "Olá")
    junte(frase, ", mundo")
    escreva(frase, " (", tamanho(frase), " letras)\n")
}
```

## `caracteres`

Recebem um `caractere`.

| PortugolTipado | C | Faz |
|---|---|---|
| `eh_letra(c)` | `isalpha` | é letra? |
| `eh_numero(c)` | `isdigit` | é dígito? |
| `eh_letra_ou_numero(c)` | `isalnum` | letra ou dígito? |
| `eh_espaco(c)` | `isspace` | espaço, tab ou quebra de linha? |
| `eh_maiuscula(c)` | `isupper` | maiúscula? |
| `eh_minuscula(c)` | `islower` | minúscula? |
| `eh_pontuacao(c)` | `ispunct` | pontuação? |
| `eh_controle(c)` | `iscntrl` | caractere de controlo? |
| `eh_imprimivel(c)` | `isprint` | visível? |
| `eh_grafico(c)` | `isgraph` | visível e não espaço? |
| `eh_hexadecimal(c)` | `isxdigit` | dígito hexadecimal? |
| `eh_branco(c)` | `isblank` | espaço ou tab? |
| `maiusculo(c)` | `toupper` | converte para maiúscula |
| `minusculo(c)` | `tolower` | converte para minúscula |

## `tempo`

O tipo `tempo` guarda um instante (`time_t`).

| PortugolTipado | C | Faz |
|---|---|---|
| `relogio()` | `clock` | ciclos de processador desde o início |
| `diferenca_tempo(fim, inicio)` | `difftime` | diferença em segundos |
| `data_texto(t)` | `ctime` | tempo em texto legível |

## Funções automáticas (`stdlib` e `stdio`)

Não precisam de `inclua`.

**Conversão e números aleatórios**

| PortugolTipado | C | Faz |
|---|---|---|
| `absoluto_int(x)` | `abs` | valor absoluto inteiro |
| `converta_inteiro(s)` | `atoi` | texto para inteiro |
| `converta_decimal(s)` | `atof` | texto para decimal |
| `converta_longo(s)` | `atol` | texto para inteiro longo |
| `aleatorio()` | `rand` | número aleatório |
| `semente(n)` | `srand` | define a semente |

**Memória (avançado)**

| PortugolTipado | C |
|---|---|
| `aloque(tamanho)` | `malloc` |
| `aloque_zerado(qtd, tamanho)` | `calloc` |
| `realoque(ponteiro, tamanho)` | `realloc` |
| `libere(ponteiro)` | `free` |

**Processo**

| PortugolTipado | C |
|---|---|
| `sair(codigo)` | `exit` |
| `aborte()` | `abort` |
| `variavel_ambiente(nome)` | `getenv` |
| `execute(comando)` | `system` |

**Busca e ordenação**

| PortugolTipado | C |
|---|---|
| `ordene(array, qtd, tamanho, comparador)` | `qsort` |
| `busque(chave, array, qtd, tamanho, comparador)` | `bsearch` |

**Entrada e saída**

| PortugolTipado | C | Faz |
|---|---|---|
| `leia_caractere()` | `getchar` | lê um caractere |
| `escreva_caractere(c)` | `putchar` | escreve um caractere |
| `escreva_linha(texto)` | `puts` | escreve com quebra de linha |
| `leia_linha(variavel)` | `fgets` | lê uma linha de texto |

## Exercício

Lê um caractere e diz se é letra, dígito ou outra coisa. Se for letra, mostra-a em maiúscula. Depois lê um número real e mostra a raiz quadrada e o quadrado dele, com `raiz` e `potencia`.

Anterior: [Arrays e estruturas](08-arrays-e-estruturas.md). Próximo: [Módulos e pacotes](10-modulos-e-pacotes.md).
