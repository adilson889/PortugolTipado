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
 */

package co.adilson889.typec.graficos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.view.WindowManager

/**
 * Activity dedicada ao modo grafico do PortugolTipado.
 *
 * Lancada pelo GraficosCanvas quando o programa chama abra_janela.
 * Hospeda uma GraficosView em ecra cheio e permanece ativa enquanto
 * o programa desenha. Fecha-se quando:
 *   - o programa chama feche_janela
 *   - o utilizador toca no botao de voltar
 *   - o sistema pede para terminar (raro)
 *
 * Toda a comunicacao com o motor passa pelo GraficosCanvas, que
 * bloqueia a coroutine do Interpretador ate a Activity estar pronta.
 */
class GraficosActivity : Activity() {

    companion object {
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_LARGURA = "largura"
        const val EXTRA_ALTURA = "altura"

        @Volatile
        var canvasAtivo: GraficosCanvas? = null
    }

    private lateinit var view: GraficosView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Graficos"
        val largura = intent.getIntExtra(EXTRA_LARGURA, 800)
        val altura = intent.getIntExtra(EXTRA_ALTURA, 600)

        if (titulo.isNotEmpty()) {
            title = titulo
        }

        // Janela mais larga que alta -> ecra na horizontal; senao, na vertical.
        // (O manifesto tem configChanges, entao a Activity nao e recriada ao rodar.)
        requestedOrientation = if (largura >= altura) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        view = GraficosView(this, largura, altura)
        setContentView(view)

        // Liga a View ao canvas que lancou a Activity
        canvasAtivo?.ligarView(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        view.encerrar()
        canvasAtivo?.marcarFechada()
        canvasAtivo = null
    }

    override fun onBackPressed() {
        // Botao de voltar equivale a pedir fecho
        canvasAtivo?.marcarFechada()
        finish()
    }
}