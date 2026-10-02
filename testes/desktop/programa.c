#include "graficos.h"

int main() {
    tc_abra_janela("Teste desktop", 800, 600);

    while (tc_janela_aberta()) {
        tc_defina_cor(30, 30, 40);
        tc_limpe();

        tc_defina_cor(255, 200, 50);
        tc_desenhe_linha(0, 0, 800, 600);
        tc_desenhe_linha(0, 600, 800, 0);

        tc_defina_cor(255, 100, 100);
        tc_preencha_retangulo(400, 300, 50, 50);

        tc_renderize();
        tc_aguarde(16);
    }

    tc_feche_janela();
    return 0;
}