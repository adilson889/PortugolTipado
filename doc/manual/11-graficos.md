# 11. Gráficos

A biblioteca `graficos` dá-te uma janela onde desenhar, e acesso ao teclado e ao rato. Funciona no Preview da app e no programa compilado, que usa SDL2.

## Um programa completo

```portugol
inclua graficos

funcao inicio() {
    abra_janela("Teste", 800, 600)

    enquanto (!tecla_pressionada(TECLA_ESC)) {
        defina_cor(30, 30, 40)
        limpe()

        defina_cor(255, 100, 100)
        preencha_retangulo(400, 300, 50, 50)

        renderize()
        aguarde(16)
    }

    feche_janela()
}
```

A estrutura é quase sempre esta: abrir a janela, repetir um ciclo onde pintas o fundo, desenhas, chamas `renderize()` e esperas um pouco, e por fim fechar. O `aguarde(16)` dá cerca de 60 imagens por segundo.

Duas coisas que convém saber desde já:

- A origem `(0, 0)` é o canto superior esquerdo, e o `y` cresce para baixo.
- Nada aparece no ecrã antes de `renderize()`. Tudo o que desenhas antes fica à espera.

## Funções

| Grupo | Funções |
|---|---|
| Janela | `abra_janela(titulo, largura, altura)`, `feche_janela()`, `defina_titulo_janela(titulo)`, `janela_aberta()`, `largura_janela()`, `altura_janela()`, `largura_tela()`, `altura_tela()` |
| Desenho | `defina_cor(r, g, b)`, `limpe()`, `desenhe_ponto(x, y)`, `desenhe_linha(x1, y1, x2, y2)`, `desenhe_retangulo(x, y, l, a)`, `preencha_retangulo(x, y, l, a)`, `desenhe_elipse(x, y, l, a)`, `preencha_elipse(x, y, l, a)`, `renderize()` |
| Texto | `defina_tamanho_texto(tamanho)`, `desenhe_texto(x, y, conteudo)`, `largura_texto(conteudo)`, `altura_texto(conteudo)` |
| Entrada | `tecla_pressionada(tecla)`, `mouse_x()`, `mouse_y()`, `botao_mouse_pressionado(botao)`, `oculte_cursor()`, `exiba_cursor()` |
| Tempo | `aguarde(ms)`, `tempo_decorrido()` |

## Cores

`defina_cor(r, g, b)` define a cor de tudo o que desenhares a seguir. Cada canal vai de 0 a 255. Valores fora deste intervalo são cortados para os limites, sem erro.

## Teclado e rato

`tecla_pressionada` e `botao_mouse_pressionado` devolvem `logico`. Os botões do rato são 1 (esquerdo), 2 (meio) e 3 (direito).

Constantes de teclas:

- `TECLA_ENTER` (13), `TECLA_ESC` (27), `TECLA_ESPACO` (32)
- `TECLA_SETA_ESQUERDA` (37), `TECLA_SETA_ACIMA` (38), `TECLA_SETA_DIREITA` (39), `TECLA_SETA_ABAIXO` (40)
- `TECLA_A` até `TECLA_Z`
- `TECLA_0` até `TECLA_9`

Para mover um quadrado com as setas:

```portugol
inclua graficos

funcao inicio() {
    inteiro x = 100
    inteiro y = 100

    abra_janela("Mover", 400, 300)

    enquanto (janela_aberta()) {
        se (tecla_pressionada(TECLA_SETA_ESQUERDA)) { x = x - 3 }
        se (tecla_pressionada(TECLA_SETA_DIREITA)) { x = x + 3 }
        se (tecla_pressionada(TECLA_SETA_ACIMA)) { y = y - 3 }
        se (tecla_pressionada(TECLA_SETA_ABAIXO)) { y = y + 3 }

        defina_cor(20, 20, 30)
        limpe()
        defina_cor(80, 200, 120)
        preencha_retangulo(x, y, 40, 40)

        renderize()
        aguarde(16)
    }
}
```

## Regras e limites

- Não podes declarar uma função com o nome de uma função da biblioteca gráfica. O erro é `'desenhe_linha' já é uma função da biblioteca gráfica (inclua graficos) e não pode ser redeclarada`.
- Número errado de argumentos dá, por exemplo, `'desenhe_linha' espera 4 argumento(s), mas recebeu 3`.
- No Preview, o programa é interrompido com um aviso se correr 2 000 000 passos seguidos sem chamar `renderize()` nem `aguarde()`. Serve para apanhar ciclos que nunca devolvem o controlo.
- O texto na janela só aparece no Preview, por enquanto.

## Compilar

No C gerado, as funções ganham o prefixo `tc_` e as constantes `TC_`, para não colidirem com nomes teus. Para compilar:

```
gcc programa.c graficos.c -o programa $(sdl2-config --cflags --libs) -lm
```

Precisas do SDL2 instalado.

## Exercício

Faz um quadrado que se mova sozinho e rebata nas bordas da janela. Usa `largura_janela()` e `altura_janela()` para saberes onde ficam as bordas. Depois faz com que ele pare ao carregar em `TECLA_ESPACO`.

Anterior: [Módulos e pacotes](10-modulos-e-pacotes.md). Próximo: [Portugol clássico](12-portugol-classico.md).
