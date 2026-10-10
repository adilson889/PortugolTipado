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
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout

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
        ocultarBarras()

        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Graficos"
        val largura = intent.getIntExtra(EXTRA_LARGURA, 800)
        val altura = intent.getIntExtra(EXTRA_ALTURA, 600)

        if (titulo.isNotEmpty()) {
            title = titulo
        }

        // O teclado do sistema (campos HTML da biblioteca 'interface') nao pode
        // redimensionar a janela: so desloca, para a escala do desenho nao mudar.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        view = GraficosView(this, largura, altura)

        // Camada dos componentes HTML, por cima do desenho. Nao e clicavel, para os
        // toques fora dos componentes passarem a GraficosView.
        val raiz = FrameLayout(this).apply {
            isClickable = false
            isFocusable = false
            clipChildren = true
        }
        val container = FrameLayout(this)
        container.setBackgroundColor(Color.rgb(240, 238, 232))
        container.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        container.addView(raiz, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(container)

        // Liga a View ao canvas que lancou a Activity
        canvasAtivo?.let { it.interfaceUi.ligar(raiz, view) }
        canvasAtivo?.ligarView(view)
    }

    override fun onWindowFocusChanged(temFoco: Boolean) {
        super.onWindowFocusChanged(temFoco)
        // O sistema repoe as barras ao voltar do teclado ou de um dialogo
        if (temFoco) ocultarBarras()
    }

    /** Ecra inteiro: sem barra de estado (hora, bateria, operadora) nem barra de navegacao. */
    private fun ocultarBarras() {
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        canvasAtivo?.let { it.interfaceUi.liberar() }
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