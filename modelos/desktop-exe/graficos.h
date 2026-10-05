#ifndef PORTUGOLTIPADO_GRAFICOS_H
#define PORTUGOLTIPADO_GRAFICOS_H

#ifdef __cplusplus
extern "C" {
#endif

#define TC_TECLA_ENTER          10
#define TC_TECLA_ESC            27
#define TC_TECLA_ESPACO         32
#define TC_TECLA_SETA_ESQUERDA  37
#define TC_TECLA_SETA_ACIMA     38
#define TC_TECLA_SETA_DIREITA   39
#define TC_TECLA_SETA_ABAIXO    40

void tc_abra_janela(const char* titulo, int largura, int altura);
void tc_feche_janela(void);
void tc_defina_titulo_janela(const char* titulo);
int  tc_largura_janela(void);
int  tc_altura_janela(void);
int  tc_largura_tela(void);
int  tc_altura_tela(void);
int  tc_janela_aberta(void);

void tc_defina_cor(int r, int g, int b);
void tc_limpe(void);
void tc_desenhe_ponto(int x, int y);
void tc_desenhe_linha(int x1, int y1, int x2, int y2);
void tc_desenhe_retangulo(int x, int y, int largura, int altura);
void tc_preencha_retangulo(int x, int y, int largura, int altura);
void tc_desenhe_elipse(int x, int y, int largura, int altura);
void tc_preencha_elipse(int x, int y, int largura, int altura);
void tc_defina_tamanho_texto(double tamanho);
void tc_desenhe_texto(int x, int y, const char* conteudo);
int  tc_largura_texto(const char* conteudo);
int  tc_altura_texto(const char* conteudo);
void tc_renderize(void);

int  tc_tecla_pressionada(int tecla);
int  tc_mouse_x(void);
int  tc_mouse_y(void);
int  tc_botao_mouse_pressionado(int botao);
void tc_oculte_cursor(void);
void tc_exiba_cursor(void);

long tc_tempo_decorrido(void);
void tc_aguarde(long ms);

#ifdef __cplusplus
}
#endif

#endif