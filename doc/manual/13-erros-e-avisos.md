# 13. Erros e avisos

Os erros saem em português e antes de chegar ao `gcc`. Aprender a lê-los poupa muito tempo.

## Como é um erro

```
Erro: linha 5
inteiro x =

mensagem do erro

Sugestão: ...
```

Vês sempre a linha do ficheiro e a mensagem. A sugestão só aparece quando há uma. O nome do ficheiro não entra no texto.

## Vários erros de uma vez

A análise de sintaxe não pára no primeiro erro. Regista cada construção com erro, salta-a e continua. Assim vês muitos problemas de uma vez só. O programa só é validado e executado quando já não há erros de sintaxe.

## Erros comuns

| Situação | Mensagem |
|---|---|
| Variável não declarada | `variável 'x' usada mas não foi declarada` |
| Função não declarada | `função 'f' usada mas não foi declarada` |
| Falta o `inclua` | `'raiz' é da biblioteca 'matematica': falta 'inclua matematica' no início do ficheiro` |
| Tipo incompatível num literal | `tipo incompatível: não é possível atribuir um valor do tipo 'TEXTO' a uma variável do tipo 'INTEIRO'` |
| Função gráfica redeclarada | `'desenhe_linha' já é uma função da biblioteca gráfica (inclua graficos) e não pode ser redeclarada` |
| `const` sem valor | `'const' precisa de um valor inicial (ex: const inteiro Z = 0)` |
| Alterar uma constante | `não é possível alterar 'TAXA': foi declarada como 'const'` |

Mais mensagens estão nos capítulos de cada tema: [`const`](02-tipos-e-variaveis.md), [funções](07-funcoes.md), [`.declare.port`](10-modulos-e-pacotes.md) e [gráficos](11-graficos.md).

## Verificação de tipos extra

Por defeito, o compilador faz as verificações descritas neste manual. Há também uma verificação de tipos mais exigente, opcional, que se liga com:

```
java -jar core/build/libs/port.jar --tipagem=segura -o programa.c programa.port
```

Entre outras coisas, rejeita conversões numéricas que podem perder informação, confere os argumentos das chamadas, os retornos das funções, os campos de estruturas e os índices de arrays. Está em fase experimental, e os limites dela estão em [Evolução V2.2](../mais/evolucao-v2-2.md). A opção `--tipagem=legada` desliga-a.

## Avisos no editor

Enquanto escreves, o editor marca palavras sem compilar:

| Cor | O que significa |
|---|---|
| Amarelo | a palavra ainda funciona, mas há uma forma preferida (por exemplo `alterar` em vez de `altere`) |
| Vermelho | não compila |

Dentro de textos, caracteres e comentários não é marcado nada.

## Exercício

Escreve de propósito cinco erros num programa: uma variável não declarada, um `const` sem valor, uma função de `matematica` sem o `inclua`, um parêntese a menos num `se` e um tipo trocado. Compila e confirma que consegues ver mais do que um erro de uma só vez.

Anterior: [Portugol clássico](12-portugol-classico.md). Voltar ao [índice](../README.md).
