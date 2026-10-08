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
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import co.adilson889.typec.interfaceui.InterfaceAndroid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementacao Android da interface Graficos, usada pelo Preview.
 *
 * Sincronizacao: o Interpretador corre na thread de UI (ver MainActivity),
 * por isso NENHUMA chamada aqui pode bloquear a thread. abra_janela so lanca
 * a Activity e volta logo; as operacoes de desenho feitas antes da View
 * estar pronta ficam guardadas e sao passadas a View quando ela liga. O
 * renderize (suspend) espera a View sem bloquear, e depois aplica o quadro,
 * mesmo modelo do SDL_RenderPresent.
 *
 * O motor do PortugolTipado continua Kotlin puro: esta classe e a
 * unica que conhece android.content.Intent.
 */
class GraficosCanvas(
    private val context: Context
) : Graficos {

    // Estado da janela
    @Volatile private var abriu = false     // abra_janela ja foi chamada
    @Volatile private var aberta = false
    @Volatile private var fechada = false

    // Completo quando a Activity liga a View
    private val pronto = CompletableDeferred<Unit>()

    // Biblioteca 'interface' (componentes HTML sobre a janela). O host passa-a ao
    // Interpretador; a GraficosActivity liga-a a View quando a janela abre.
    val interfaceUi = InterfaceAndroid()

    // View ativa (ligada pela Activity)
    @Volatile private var view: GraficosView? = null

    // Operacoes de desenho feitas antes da View estar ligada
    private val pendentes = ArrayList<GraficosView.Operacao>()

    // Dimensoes
    private var larguraJanela = 0
    private var alturaJanela = 0

    // Tamanho de texto atual (para larguraTexto/alturaTexto)
    private var tamanhoTexto = 15.0

    // -------------------------------------------------------------
    // Ligacao com a Activity (chamado pela GraficosActivity)
    // -------------------------------------------------------------

    fun ligarView(v: GraficosView) {
        synchronized(pendentes) {
            for (op in pendentes) v.enfileirar(op)
            pendentes.clear()
            view = v
        }
        if (fechada) {
            // O programa ja chamou feche_janela antes da Activity ficar pronta
            (v.context as? Activity)?.finish()
        }
        // Acorda o renderize so depois do onCreate terminar
        Handler(Looper.getMainLooper()).post { pronto.complete(Unit) }
    }

    fun marcarJanelaAtiva() {
        if (fechada) return
        aberta = true
    }

    fun marcarJanelaInativa() {
        aberta = false
    }

    fun marcarFechada() {
        fechada = true
        aberta = false
    }

    // -------------------------------------------------------------
    // Graficos: Janela
    // -------------------------------------------------------------

    override fun abraJanela(titulo: String, largura: Int, altura: Int) {
        if (abriu) {
            throw ErroGrafico("abra_janela chamada duas vezes")
        }
        if (largura <= 0) throw ErroGrafico("abra_janela: a largura deve ser maior que zero")
        if (altura <= 0)  throw ErroGrafico("abra_janela: a altura deve ser maior que zero")

        larguraJanela = largura
        alturaJanela = altura
        abriu = true
        aberta = true
        fechada = false

        GraficosActivity.canvasAtivo = this

        val intent = Intent(context, GraficosActivity::class.java).apply {
            putExtra(GraficosActivity.EXTRA_TITULO, titulo)
            putExtra(GraficosActivity.EXTRA_LARGURA, largura)
            putExtra(GraficosActivity.EXTRA_ALTURA, altura)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Nao espera pela Activity: a thread de UI tem de ficar livre para a criar
        context.startActivity(intent)
    }

    override fun fecheJanela() {
        if (!abriu) return
        marcarFechada()
        // Fecha a Activity de verdade (na thread de UI). Sem isto a janela
        // ficaria no ecra mesmo depois do programa chamar feche_janela.
        val activity = view?.context as? Activity ?: return
        activity.runOnUiThread {
            if (!activity.isFinishing) activity.finish()
        }
    }

    override fun definaTituloJanela(titulo: String) {
        // O titulo so pode ser mudado na thread de UI; como a janela
        // ja esta aberta, isto e apenas um pedido sem efeito no Android,
        // que mostra o titulo na barra do sistema.
        // Mantido por compatibilidade com o contrato.
    }

    override fun larguraJanela(): Int = larguraJanela

    override fun alturaJanela(): Int = alturaJanela

    override fun larguraTela(): Int {
        return context.resources.displayMetrics.widthPixels
    }

    override fun alturaTela(): Int {
        return context.resources.displayMetrics.heightPixels
    }

    override fun janelaAberta(): Boolean = aberta && !fechada

    // -------------------------------------------------------------
    // Graficos: Desenho (enfileira na View, ou guarda ate ela ligar)
    // -------------------------------------------------------------

    private fun exigirAberta(nome: String) {
        if (!abriu) throw ErroGrafico("$nome chamada antes de abra_janela")
    }

    private fun enfileirar(op: GraficosView.Operacao) {
        synchronized(pendentes) {
            val v = view
            if (v != null) v.enfileirar(op) else pendentes.add(op)
        }
    }

    override fun definaCor(r: Int, g: Int, b: Int) {
        exigirAberta("defina_cor")
        enfileirar(GraficosView.Operacao.Cor(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
    }

    override fun limpe() {
        exigirAberta("limpe")
        enfileirar(GraficosView.Operacao.Limpe)
    }

    override fun desenhePonto(x: Int, y: Int) {
        exigirAberta("desenhe_ponto")
        enfileirar(GraficosView.Operacao.Ponto(x, y))
    }

    override fun desenheLinha(x1: Int, y1: Int, x2: Int, y2: Int) {
        exigirAberta("desenhe_linha")
        enfileirar(GraficosView.Operacao.Linha(x1, y1, x2, y2))
    }

    override fun desenheRetangulo(x: Int, y: Int, largura: Int, altura: Int) {
        exigirAberta("desenhe_retangulo")
        enfileirar(GraficosView.Operacao.Retangulo(x, y, largura, altura, false))
    }

    override fun preenchaRetangulo(x: Int, y: Int, largura: Int, altura: Int) {
        exigirAberta("preencha_retangulo")
        enfileirar(GraficosView.Operacao.Retangulo(x, y, largura, altura, true))
    }

    override fun desenheElipse(x: Int, y: Int, largura: Int, altura: Int) {
        exigirAberta("desenhe_elipse")
        enfileirar(GraficosView.Operacao.Elipse(x, y, largura, altura, false))
    }

    override fun preenchaElipse(x: Int, y: Int, largura: Int, altura: Int) {
        exigirAberta("preencha_elipse")
        enfileirar(GraficosView.Operacao.Elipse(x, y, largura, altura, true))
    }

    override fun definaTamanhoTexto(tamanho: Double) {
        exigirAberta("defina_tamanho_texto")
        if (tamanho <= 0.0) throw ErroGrafico("defina_tamanho_texto: o tamanho deve ser maior que zero")
        tamanhoTexto = tamanho
        enfileirar(GraficosView.Operacao.TamanhoTexto(tamanho.toFloat()))
    }

    override fun desenheTexto(x: Int, y: Int, conteudo: String) {
        exigirAberta("desenhe_texto")
        enfileirar(GraficosView.Operacao.Texto(x, y, conteudo))
    }

    override fun larguraTexto(conteudo: String): Int =
        if (conteudo.isEmpty()) 0 else (conteudo.length * tamanhoTexto * 0.6).toInt()

    override fun alturaTexto(conteudo: String): Int = tamanhoTexto.toInt()

    override suspend fun renderize() {
        exigirAberta("renderize")
        if (view == null) {
            // Espera a Activity ligar a View, sem bloquear a thread de UI
            val ok = withTimeoutOrNull(5000) { pronto.await() }
            if (ok == null) {
                throw ErroGrafico("a janela grafica nao abriu (timeout)")
            }
        }
        val v = view ?: return
        withContext(Dispatchers.Main) {
            v.renderizar()
        }
    }

    // -------------------------------------------------------------
    // Graficos: Teclado e mouse
    // -------------------------------------------------------------

    override fun teclaPressionada(tecla: Int): Boolean {
        // Janela fechada (botao Voltar ou X): responde "ESC pressionada" para o
        // laco do programa terminar, como acontece com o X no SDL.
        if (tecla == Tecla.ESC && fechada) return true
        val v = view ?: return false
        return v.teclaPressionada(tecla)
    }

    override fun mouseX(): Int = view?.mouseX() ?: 0

    override fun mouseY(): Int = view?.mouseY() ?: 0

    override fun botaoMousePressionado(botao: Int): Boolean {
        return view?.botaoMousePressionado(botao) ?: false
    }

    override fun oculteCursor() {
        // No Android nao ha cursor visivel por padrao no SurfaceView.
    }

    override fun exibaCursor() {
        // Idem.
    }

    // -------------------------------------------------------------
    // Graficos: Tempo
    // -------------------------------------------------------------

    override fun tempoDecorrido(): Long = view?.tempoDecorrido() ?: 0L

    override suspend fun aguarde(ms: Long) {
        if (ms <= 0) return
        withContext(Dispatchers.Main) {
            kotlinx.coroutines.delay(ms)
        }
    }
}
