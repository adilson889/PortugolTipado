/*
 * PortugolTipado - Superconjunto de Portugol que compila para C
 * Copyright (C) 2026  Adilson C. Rafael
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Runtime C da biblioteca grafica do PortugolTipado.
 * Implementacao de referencia em SDL2.
 */

#include "graficos.h"

#include <SDL2/SDL.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

/* ------------------------------------------------------------------ */
/* Estado interno                                                      */
/* ------------------------------------------------------------------ */

static struct {
    SDL_Window*   janela;
    SDL_Renderer* renderizador;
    int           criada;   /* abra_janela chamada e feche_janela ainda nao */
    int           aberta;   /* criada e o utilizador ainda nao fechou (X) */
    int           r, g, b;
    int           tamanho_texto;
    Uint32        inicio;
    const Uint8*  teclas;
    int           mouse_x;
    int           mouse_y;
    Uint32        mouse_botoes;
    int           cursor_oculto;
} tc_estado = {0};

/* ------------------------------------------------------------------ */
/* Erro fatal                                                          */
/* ------------------------------------------------------------------ */

static void tc_erro(const char* mensagem) {
    fprintf(stderr, "Erro grafico: %s\n", mensagem);
    exit(1);
}

/* ------------------------------------------------------------------ */
/* Janela                                                              */
/* ------------------------------------------------------------------ */

void tc_abra_janela(const char* titulo, int largura, int altura) {
    if (tc_estado.criada) {
        tc_erro("abra_janela chamada duas vezes");
    }
    if (largura <= 0) tc_erro("abra_janela: a largura deve ser maior que zero");
    if (altura  <= 0) tc_erro("abra_janela: a altura deve ser maior que zero");

    if (SDL_Init(SDL_INIT_VIDEO) != 0) {
        tc_erro(SDL_GetError());
    }

    tc_estado.janela = SDL_CreateWindow(
        titulo,
        SDL_WINDOWPOS_CENTERED,
        SDL_WINDOWPOS_CENTERED,
        largura,
        altura,
        SDL_WINDOW_SHOWN
    );
    if (!tc_estado.janela) tc_erro(SDL_GetError());

    tc_estado.renderizador = SDL_CreateRenderer(
        tc_estado.janela, -1, SDL_RENDERER_ACCELERATED
    );
    if (!tc_estado.renderizador) {
        tc_estado.renderizador = SDL_CreateRenderer(
            tc_estado.janela, -1, SDL_RENDERER_SOFTWARE
        );
    }
    if (!tc_estado.renderizador) tc_erro(SDL_GetError());

    tc_estado.criada = 1;
    tc_estado.aberta = 1;
    tc_estado.teclas = NULL;
    tc_estado.r = 0;
    tc_estado.g = 0;
    tc_estado.b = 0;
    tc_estado.tamanho_texto = 15;
    tc_estado.inicio = SDL_GetTicks();
    tc_estado.cursor_oculto = 0;
}

void tc_feche_janela(void) {
    if (!tc_estado.criada) return;

    if (tc_estado.renderizador) SDL_DestroyRenderer(tc_estado.renderizador);
    if (tc_estado.janela)      SDL_DestroyWindow(tc_estado.janela);
    SDL_Quit();

    tc_estado.janela = NULL;
    tc_estado.renderizador = NULL;
    tc_estado.teclas = NULL;
    tc_estado.criada = 0;
    tc_estado.aberta = 0;
}

void tc_defina_titulo_janela(const char* titulo) {
    if (!tc_estado.criada) tc_erro("defina_titulo_janela chamada antes de abra_janela");
    SDL_SetWindowTitle(tc_estado.janela, titulo);
}

int tc_largura_janela(void) {
    if (!tc_estado.criada) tc_erro("largura_janela chamada antes de abra_janela");
    int w = 0;
    SDL_GetWindowSize(tc_estado.janela, &w, NULL);
    return w;
}

int tc_altura_janela(void) {
    if (!tc_estado.criada) tc_erro("altura_janela chamada antes de abra_janela");
    int h = 0;
    SDL_GetWindowSize(tc_estado.janela, NULL, &h);
    return h;
}

int tc_largura_tela(void) {
    SDL_DisplayMode modo;
    if (SDL_GetCurrentDisplayMode(0, &modo) != 0) return 0;
    return modo.w;
}

int tc_altura_tela(void) {
    SDL_DisplayMode modo;
    if (SDL_GetCurrentDisplayMode(0, &modo) != 0) return 0;
    return modo.h;
}

int tc_janela_aberta(void) {
    return tc_estado.aberta;
}

/* ------------------------------------------------------------------ */
/* Desenho                                                             */
/* ------------------------------------------------------------------ */

void tc_defina_cor(int r, int g, int b) {
    if (!tc_estado.criada) tc_erro("defina_cor chamada antes de abra_janela");
    if (r < 0) r = 0; if (r > 255) r = 255;
    if (g < 0) g = 0; if (g > 255) g = 255;
    if (b < 0) b = 0; if (b > 255) b = 255;
    tc_estado.r = r;
    tc_estado.g = g;
    tc_estado.b = b;
    SDL_SetRenderDrawColor(tc_estado.renderizador, r, g, b, 255);
}

void tc_limpe(void) {
    if (!tc_estado.criada) tc_erro("limpe chamada antes de abra_janela");
    SDL_RenderClear(tc_estado.renderizador);
}

void tc_desenhe_ponto(int x, int y) {
    if (!tc_estado.criada) tc_erro("desenhe_ponto chamada antes de abra_janela");
    SDL_RenderDrawPoint(tc_estado.renderizador, x, y);
}

void tc_desenhe_linha(int x1, int y1, int x2, int y2) {
    if (!tc_estado.criada) tc_erro("desenhe_linha chamada antes de abra_janela");
    SDL_RenderDrawLine(tc_estado.renderizador, x1, y1, x2, y2);
}

void tc_desenhe_retangulo(int x, int y, int largura, int altura) {
    if (!tc_estado.criada) tc_erro("desenhe_retangulo chamada antes de abra_janela");
    SDL_Rect ret = { x, y, largura, altura };
    SDL_RenderDrawRect(tc_estado.renderizador, &ret);
}

void tc_preencha_retangulo(int x, int y, int largura, int altura) {
    if (!tc_estado.criada) tc_erro("preencha_retangulo chamada antes de abra_janela");
    SDL_Rect ret = { x, y, largura, altura };
    SDL_RenderFillRect(tc_estado.renderizador, &ret);
}

void tc_desenhe_elipse(int x, int y, int largura, int altura) {
    if (!tc_estado.criada) tc_erro("desenhe_elipse chamada antes de abra_janela");
    int cx = x + largura / 2;
    int cy = y + altura  / 2;
    int rx = largura / 2;
    int ry = altura  / 2;
    if (rx <= 0 || ry <= 0) return;

    /* Poligono fechado com segmentos suficientes para nao deixar falhas */
    int n = 4 * (rx + ry);
    if (n < 16) n = 16;
    int ant_x = cx + rx;
    int ant_y = cy;
    for (int i = 1; i <= n; i++) {
        double rad = 2.0 * 3.14159265358979 * i / n;
        int px = cx + (int)lround(rx * cos(rad));
        int py = cy + (int)lround(ry * sin(rad));
        SDL_RenderDrawLine(tc_estado.renderizador, ant_x, ant_y, px, py);
        ant_x = px;
        ant_y = py;
    }
}

void tc_preencha_elipse(int x, int y, int largura, int altura) {
    if (!tc_estado.criada) tc_erro("preencha_elipse chamada antes de abra_janela");
    int cx = x + largura / 2;
    int cy = y + altura  / 2;
    int rx = largura / 2;
    int ry = altura  / 2;
    if (rx <= 0 || ry <= 0) return;

    /* Uma linha horizontal por fila */
    for (int py = -ry; py <= ry; py++) {
        double f = 1.0 - (double)(py * py) / (double)(ry * ry);
        if (f < 0.0) f = 0.0;
        int meia = (int)lround(rx * sqrt(f));
        SDL_RenderDrawLine(tc_estado.renderizador,
                           cx - meia, cy + py, cx + meia, cy + py);
    }
}

void tc_defina_tamanho_texto(double tamanho) {
    if (!tc_estado.criada) tc_erro("defina_tamanho_texto chamada antes de abra_janela");
    if (tamanho <= 0.0) tc_erro("defina_tamanho_texto: o tamanho deve ser maior que zero");
    tc_estado.tamanho_texto = (int)tamanho;
}

void tc_desenhe_texto(int x, int y, const char* conteudo) {
    if (!tc_estado.criada) tc_erro("desenhe_texto chamada antes de abra_janela");
    (void)x; (void)y; (void)conteudo;
    /* Renderizacao de texto real exige SDL_ttf e um ficheiro de fonte.
     * Fica pendente como requisito opcional, para nao bloquear a Fase 1. */
}

int tc_largura_texto(const char* conteudo) {
    if (!tc_estado.criada) tc_erro("largura_texto chamada antes de abra_janela");
    (void)conteudo;
    return 0;
}

int tc_altura_texto(const char* conteudo) {
    if (!tc_estado.criada) tc_erro("altura_texto chamada antes de abra_janela");
    (void)conteudo;
    return tc_estado.tamanho_texto;
}

void tc_renderize(void) {
    if (!tc_estado.criada) tc_erro("renderize chamada antes de abra_janela");
    SDL_RenderPresent(tc_estado.renderizador);

    SDL_Event evento;
    while (SDL_PollEvent(&evento)) {
        if (evento.type == SDL_QUIT) {
            tc_estado.aberta = 0;
        }
    }

    tc_estado.teclas = SDL_GetKeyboardState(NULL);

    int mx = 0, my = 0;
    Uint32 botoes = SDL_GetMouseState(&mx, &my);
    tc_estado.mouse_x = mx;
    tc_estado.mouse_y = my;
    tc_estado.mouse_botoes = botoes;
}

/* ------------------------------------------------------------------ */
/* Teclado e mouse                                                     */
/* ------------------------------------------------------------------ */

int tc_tecla_pressionada(int tecla) {
    if (!tc_estado.criada) tc_erro("tecla_pressionada chamada antes de abra_janela");
    /* Janela fechada pelo utilizador (X): responde ESC pressionada, para o
     * laco do programa terminar, igual ao host Android. */
    if (tecla == TC_TECLA_ESC && !tc_estado.aberta) return 1;
    if (!tc_estado.teclas) return 0;

    switch (tecla) {
        case TC_TECLA_ENTER:          return tc_estado.teclas[SDL_SCANCODE_RETURN];
        case TC_TECLA_ESC:            return tc_estado.teclas[SDL_SCANCODE_ESCAPE];
        case TC_TECLA_ESPACO:         return tc_estado.teclas[SDL_SCANCODE_SPACE];
        case TC_TECLA_SETA_ESQUERDA:  return tc_estado.teclas[SDL_SCANCODE_LEFT];
        case TC_TECLA_SETA_ACIMA:     return tc_estado.teclas[SDL_SCANCODE_UP];
        case TC_TECLA_SETA_DIREITA:   return tc_estado.teclas[SDL_SCANCODE_RIGHT];
        case TC_TECLA_SETA_ABAIXO:    return tc_estado.teclas[SDL_SCANCODE_DOWN];
        default: break;
    }
    if (tecla >= 'A' && tecla <= 'Z') {
        SDL_Scancode sc = (SDL_Scancode)(SDL_SCANCODE_A + (tecla - 'A'));
        return tc_estado.teclas[sc];
    }
    if (tecla >= '0' && tecla <= '9') {
        SDL_Scancode sc = (SDL_Scancode)(SDL_SCANCODE_0 + (tecla - '0'));
        return tc_estado.teclas[sc];
    }
    return 0;
}

int tc_mouse_x(void) {
    if (!tc_estado.criada) tc_erro("mouse_x chamada antes de abra_janela");
    return tc_estado.mouse_x;
}

int tc_mouse_y(void) {
    if (!tc_estado.criada) tc_erro("mouse_y chamada antes de abra_janela");
    return tc_estado.mouse_y;
}

int tc_botao_mouse_pressionado(int botao) {
    if (!tc_estado.criada) tc_erro("botao_mouse_pressionado chamada antes de abra_janela");
    switch (botao) {
        case 1: return (tc_estado.mouse_botoes & SDL_BUTTON(SDL_BUTTON_LEFT))   ? 1 : 0;
        case 2: return (tc_estado.mouse_botoes & SDL_BUTTON(SDL_BUTTON_MIDDLE)) ? 1 : 0;
        case 3: return (tc_estado.mouse_botoes & SDL_BUTTON(SDL_BUTTON_RIGHT))  ? 1 : 0;
        default: return 0;
    }
}

void tc_oculte_cursor(void) {
    if (!tc_estado.criada) tc_erro("oculte_cursor chamada antes de abra_janela");
    SDL_ShowCursor(SDL_DISABLE);
    tc_estado.cursor_oculto = 1;
}

void tc_exiba_cursor(void) {
    if (!tc_estado.criada) tc_erro("exiba_cursor chamada antes de abra_janela");
    SDL_ShowCursor(SDL_ENABLE);
    tc_estado.cursor_oculto = 0;
}

/* ------------------------------------------------------------------ */
/* Tempo                                                               */
/* ------------------------------------------------------------------ */

long tc_tempo_decorrido(void) {
    /* Milissegundos desde abra_janela; 0 antes disso (igual ao host Android) */
    if (!tc_estado.criada) return 0;
    return (long)(SDL_GetTicks() - tc_estado.inicio);
}

void tc_aguarde(long ms) {
    if (ms <= 0) return;
    SDL_Delay((Uint32)ms);
}