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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * SurfaceView que desenha o conteudo do contrato Graficos.
 *
 * O desenho acontece na thread de UI, mas controlado pelo GraficosCanvas.
 * Cada quadro e aplicado quando o motor chama renderize, igual ao
 * SDL_RenderPresent da runtime C.
 *
 * A janela tem tamanho logico fixo (largura x altura). O quadro e escalado
 * para caber no ecra, centrado e com barras pretas, e as coordenadas do
 * toque sao convertidas de volta para o tamanho logico. Assim o programa
 * ve as mesmas coordenadas que veria no SDL.
 *
 * Estado de teclado e mouse mantido em campos, lido por polling
 * (tecla_pressionada / mouse_x / mouse_y), sem callbacks.
 */
class GraficosView(
    context: Context,
    val largura: Int,
    val altura: Int
) : SurfaceView(context), SurfaceHolder.Callback {

    private val paint = Paint()
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Mesma fonte do executavel C (DejaVu Sans Mono), para o texto sair igual
        typeface = try {
            Typeface.createFromAsset(context.assets, "fontes/ttf/DejaVuSansMono.ttf")
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
        textSize = 15f
    }

    // Cor atual
    private var corR = 0
    private var corG = 0
    private var corB = 0

    // Tamanho de texto
    private var tamanhoTexto = 15f

    // Transformacao logico -> ecra (atualizada a cada renderize)
    @Volatile private var escala = 1f
    @Volatile private var deslocX = 0f
    @Volatile private var deslocY = 0f

    // Estado de entrada (escrito na thread de UI, lido pelo Interpretador)
    private val teclasPressionadas: MutableSet<Int> =
    java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    @Volatile private var mouseX = 0
    @Volatile private var mouseY = 0
    @Volatile private var botaoEsquerdo = false
    @Volatile private var botaoMeio = false
    @Volatile private var botaoDireito = false

    // Buffer de operacoes (preenchido pelo GraficosCanvas, aplicado no renderize)
    private val operacoes = ArrayList<Operacao>()

    // Callback chamado depois de cada renderize, para o GraficosCanvas
    var aoRenderizar: (() -> Unit)? = null

    // Inicio em milissegundos
    private val inicioMs = System.currentTimeMillis()

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // -------------------------------------------------------------
    // SurfaceHolder.Callback
    // -------------------------------------------------------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        requestFocus()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Nada a fazer: a escala e recalculada em cada renderize
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Nada a fazer
    }

    // -------------------------------------------------------------
    // Fila de operacoes
    // -------------------------------------------------------------

    sealed class Operacao {
        data class Cor(val r: Int, val g: Int, val b: Int) : Operacao()
        object Limpe : Operacao()
        data class Ponto(val x: Int, val y: Int) : Operacao()
        data class Linha(val x1: Int, val y1: Int, val x2: Int, val y2: Int) : Operacao()
        data class Retangulo(val x: Int, val y: Int, val l: Int, val a: Int, val preenchido: Boolean) : Operacao()
        data class Elipse(val x: Int, val y: Int, val l: Int, val a: Int, val preenchido: Boolean) : Operacao()
        data class TamanhoTexto(val tamanho: Float) : Operacao()
        data class Texto(val x: Int, val y: Int, val conteudo: String) : Operacao()
    }

    fun enfileirar(op: Operacao) {
        synchronized(operacoes) {
            operacoes.add(op)
        }
    }

    /**
     * Aplica todas as operacoes pendentes num Canvas novo e apresenta.
     * Chamado pelo GraficosCanvas quando o programa chama renderize.
     */
    fun renderizar() {
        val holder = holder
        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return
        try {
            // Fundo do ecra (barras fora da janela logica)
            canvas.drawColor(Color.BLACK)

            val e = minOf(canvas.width.toFloat() / largura, canvas.height.toFloat() / altura)
            val dx = (canvas.width - largura * e) / 2f
            val dy = (canvas.height - altura * e) / 2f
            escala = e
            deslocX = dx
            deslocY = dy

            canvas.save()
            canvas.translate(dx, dy)
            canvas.scale(e, e)
            canvas.clipRect(0f, 0f, largura.toFloat(), altura.toFloat())

            synchronized(operacoes) {
                for (op in operacoes) {
                    aplicar(canvas, op)
                }
                operacoes.clear()
            }

            canvas.restore()
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
        aoRenderizar?.invoke()
    }

    private fun aplicar(canvas: Canvas, op: Operacao) {
        when (op) {
            is Operacao.Cor -> {
                corR = op.r
                corG = op.g
                corB = op.b
                paint.color = Color.rgb(corR, corG, corB)
            }
            is Operacao.Limpe -> {
                canvas.drawColor(Color.rgb(corR, corG, corB))
            }
            is Operacao.Ponto -> {
                canvas.drawPoint(op.x.toFloat(), op.y.toFloat(), paint)
            }
            is Operacao.Linha -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawLine(op.x1.toFloat(), op.y1.toFloat(), op.x2.toFloat(), op.y2.toFloat(), paint)
            }
            is Operacao.Retangulo -> {
                paint.style = if (op.preenchido) Paint.Style.FILL else Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(op.x.toFloat(), op.y.toFloat(), (op.x + op.l).toFloat(), (op.y + op.a).toFloat(), paint)
            }
            is Operacao.Elipse -> {
                paint.style = if (op.preenchido) Paint.Style.FILL else Paint.Style.STROKE
                paint.strokeWidth = 1f
                val r = RectF(op.x.toFloat(), op.y.toFloat(), (op.x + op.l).toFloat(), (op.y + op.a).toFloat())
                canvas.drawOval(r, paint)
            }
            is Operacao.TamanhoTexto -> {
                tamanhoTexto = op.tamanho
                paintTexto.textSize = tamanhoTexto
            }
            is Operacao.Texto -> {
                paintTexto.color = paint.color
                canvas.drawText(op.conteudo, op.x.toFloat(), op.y.toFloat() + tamanhoTexto, paintTexto)
            }
        }
    }

    // -------------------------------------------------------------
    // Entrada (consulta, sem callbacks de negocio)
    // -------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val tecla = converterTecla(keyCode)
        // Teclas desconhecidas (volume, voltar...) seguem o caminho normal do sistema
        if (tecla == -1) return super.onKeyDown(keyCode, event)
        teclasPressionadas.add(tecla)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val tecla = converterTecla(keyCode)
        if (tecla == -1) return super.onKeyUp(keyCode, event)
        teclasPressionadas.remove(tecla)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Converte do ecra para o tamanho logico da janela
        mouseX = ((event.x - deslocX) / escala).toInt()
        mouseY = ((event.y - deslocY) / escala).toInt()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                botaoEsquerdo = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                botaoEsquerdo = false
            }
        }
        return true
    }

    /**
     * O botao Voltar nao e mapeado aqui: quem o trata e a GraficosActivity,
     * que marca a janela como fechada (e o GraficosCanvas responde "ESC
     * pressionada" enquanto a janela estiver fechada).
     */
    private fun converterTecla(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> Tecla.ENTER
            KeyEvent.KEYCODE_ESCAPE -> Tecla.ESC
            KeyEvent.KEYCODE_SPACE -> Tecla.ESPACO
            KeyEvent.KEYCODE_DPAD_LEFT -> Tecla.SETA_ESQUERDA
            KeyEvent.KEYCODE_DPAD_UP -> Tecla.SETA_ACIMA
            KeyEvent.KEYCODE_DPAD_RIGHT -> Tecla.SETA_DIREITA
            KeyEvent.KEYCODE_DPAD_DOWN -> Tecla.SETA_ABAIXO
            in KeyEvent.KEYCODE_A .. KeyEvent.KEYCODE_Z -> 'A'.code + (keyCode - KeyEvent.KEYCODE_A)
            in KeyEvent.KEYCODE_0 .. KeyEvent.KEYCODE_9 -> '0'.code + (keyCode - KeyEvent.KEYCODE_0)
            else -> -1
        }
    }

    fun teclaPressionada(tecla: Int): Boolean = tecla in teclasPressionadas
    fun mouseX(): Int = mouseX
    fun mouseY(): Int = mouseY
    fun botaoMousePressionado(botao: Int): Boolean = when (botao) {
        1 -> botaoEsquerdo
        2 -> botaoMeio
        3 -> botaoDireito
        else -> false
    }

    fun tempoDecorrido(): Long = System.currentTimeMillis() - inicioMs

    fun encerrar() {
        synchronized(operacoes) {
            operacoes.clear()
        }
        teclasPressionadas.clear()
    }
}
