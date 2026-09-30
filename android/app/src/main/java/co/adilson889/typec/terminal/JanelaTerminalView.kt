package co.adilson889.typec.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View

/**
* View Android que desenha a grade de um TerminalANSI na tela,
* célula por célula, com fonte monoespaçada — visual de terminal real.
* Não conhece Lexer/Parser/Interpretador, só sabe desenhar um TerminalANSI.
*/
class JanelaTerminalView(context: Context) : View(context) {

    private var terminal: TerminalANSI = TerminalANSI(24, 80)
    private var ultimoTexto: String = "" // guardado pra reprocessar se a largura (nº de colunas) mudar

    private val paletaTexto = mapOf(
        CorAnsi.PRETO to Color.parseColor("#000000"),
        CorAnsi.VERMELHO to Color.parseColor("#F14C4C"),
        CorAnsi.VERDE to Color.parseColor("#23D18B"),
        CorAnsi.AMARELO to Color.parseColor("#F5F543"),
        CorAnsi.AZUL to Color.parseColor("#3B8EEA"),
        CorAnsi.MAGENTA to Color.parseColor("#D670D6"),
        CorAnsi.CIANO to Color.parseColor("#29B8DB"),
        CorAnsi.BRANCO to Color.parseColor("#E5E5E5"),
        CorAnsi.PADRAO to Color.parseColor("#E6EDF3")
    )
    private val paletaFundo = paletaTexto + (CorAnsi.PADRAO to Color.parseColor("#0D1117"))

    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 32f
    }
    private val paintFundo = Paint()

    // Reutilizado no onDraw para não criar uma String por célula
    private val bufferChar = CharArray(1)

    private var larguraCelula = 0f
    private var alturaCelula = 0f

    init {
        recalcularMetricas()
    }

    private fun recalcularMetricas() {
        larguraCelula = paintTexto.measureText("M")
        val fm = paintTexto.fontMetrics
        alturaCelula = fm.descent - fm.ascent
    }

    /** Quantas colunas cabem na largura dada (quebra de linha na borda da tela, como no Pydroid). */
    private fun colunasPara(larguraPx: Int): Int {
        val util = larguraPx - paddingLeft - paddingRight
        return (util / larguraCelula).toInt().coerceAtLeast(20)
    }

    /** Define o texto de saída (com escapes ANSI) e redesenha a janela. */
    fun mostrarSaida(textoComAnsi: String) {
        ultimoTexto = textoComAnsi
        reconstruir(if (width > 0) width else resources.displayMetrics.widthPixels)
    }

    private fun reconstruir(larguraPx: Int) {
        terminal = TerminalANSI(24, colunasPara(larguraPx))
        terminal.processar(ultimoTexto)
        invalidate()
        requestLayout()
    }

    fun limpar() {
        ultimoTexto = ""
        terminal = TerminalANSI(24, terminal.colunas)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // largura real conhecida: se o nº de colunas mudou, reprocessa o texto com a nova largura
        if (w > 0 && colunasPara(w) != terminal.colunas) reconstruir(w)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val larguraDesejada = kotlin.math.ceil(larguraCelula * terminal.colunas).toInt() + paddingLeft + paddingRight
        val alturaDesejada = (alturaCelula * terminal.linhasUsadas).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(larguraDesejada, widthMeasureSpec),
            resolveSize(alturaDesejada, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Fundo geral da janela (cor padrão do terminal)
        paintFundo.color = paletaFundo.getValue(CorAnsi.PADRAO)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFundo)

        val fm = paintTexto.fontMetrics

        // Só percorre as linhas que aparecem na área visível (o buffer pode ter milhares)
        val visivel = canvas.clipBounds
        val primeiraLinha = ((visivel.top - paddingTop) / alturaCelula).toInt().coerceAtLeast(0)
        val ultimaLinha = (((visivel.bottom - paddingTop) / alturaCelula).toInt() + 1)
        .coerceAtMost(terminal.linhasUsadas - 1)
        .coerceAtMost(terminal.linhas - 1)

        for (linha in primeiraLinha .. ultimaLinha) {
            val y = paddingTop + (linha * alturaCelula)
            for (coluna in 0 until terminal.colunas) {
                val celula = terminal.celula(linha, coluna)
                val x = paddingLeft + (coluna * larguraCelula)

                // Fundo da célula, só desenha se for diferente do padrão (economiza desenho)
                if (celula.corFundo != CorAnsi.PADRAO) {
                    paintFundo.color = paletaFundo.getOrElse(celula.corFundo) { paletaFundo.getValue(CorAnsi.PADRAO) }
                    canvas.drawRect(x, y, x + larguraCelula, y + alturaCelula, paintFundo)
                }

                if (celula.caractere != ' ') {
                    paintTexto.color = paletaTexto.getOrElse(celula.corTexto) { paletaTexto.getValue(CorAnsi.PADRAO) }
                    paintTexto.isFakeBoldText = celula.negrito
                    bufferChar[0] = celula.caractere
                    canvas.drawText(bufferChar, 0, 1, x, y - fm.ascent, paintTexto)
                }
            }
        }
    }
}
