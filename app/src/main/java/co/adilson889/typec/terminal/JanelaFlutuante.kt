package co.adilson889.typec.terminal

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import co.adilson889.typec.interpretador.FonteEntrada
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
* Janela flutuante estilo desktop (arrastável pela barra de título,
* redimensionável pela borda inferior-direita), que exibe uma
* JanelaTerminalView dentro. Sobrepõe a tela do app, como um Dialog.
*
* Também implementa FonteEntrada: quando o Interpretador executa um
* ler(), a janela mostra um campo de texto + botão "Enviar" logo
* abaixo do terminal, e a execução do programa fica pausada de
* verdade (suspend) até o usuário confirmar o valor digitado.
*/
class JanelaFlutuante(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar), FonteEntrada {

    private lateinit var terminalView: JanelaTerminalView
    private lateinit var scrollVertical: ScrollView
    private lateinit var containerJanela: FrameLayout
    private lateinit var layoutParamsJanela: FrameLayout.LayoutParams
    private lateinit var corpoJanela: View // parte com o conteúdo (some ao minimizar)
    private lateinit var linhaEntrada: LinearLayout
    private lateinit var campoEntrada: EditText

    private var continuacaoPendente: Continuation<String>? = null

    private val corBarraTitulo = Color.parseColor("#6200EE")
    private val corBordaJanela = Color.parseColor("#30363D")
    private val corFundoJanela = Color.parseColor("#0D1117")

    private var larguraJanela = 900
    private var alturaJanela = 600

    // Guarda a última geometria "normal" para restaurar depois de expandir/minimizar
    private var larguraAnterior = larguraJanela
    private var alturaAnterior = alturaJanela
    private var margemEsquerdaAnterior = 0
    private var margemTopoAnterior = 0
    private var estaMinimizada = false
    private var estaExpandida = false

    // true = terminal em tela cheia (estilo Pydroid); false = janela flutuante arrastável/redimensionável
    private val telaCheia = true

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(
            ColorDrawable(if (telaCheia) corFundoJanela else Color.parseColor("#66000000"))
        )
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        // Teclado empurra o conteúdo (a barra de entrada sobe junto) em vez de cobrir o terminal
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setCancelable(true)
        construirLayout(context)

        // Ao fechar por qualquer caminho (botão X, voltar, toque fora), libera um ler()
        // que esteja à espera, para o Interpretador não ficar suspenso para sempre.
        setOnDismissListener { liberarEntradaPendente("") }
    }

    private fun construirLayout(context: Context) {
        // Raiz ocupa a tela inteira (para permitir posicionar a janela livremente)
        val raiz = FrameLayout(context)

        containerJanela = FrameLayout(context).apply {
            setBackgroundColor(corFundoJanela)
        }

        val colunaJanela = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(corBordaJanela)
            setPadding(2, 2, 2, 2)
        }

        // Barra de título (arrastável)
        val barraTitulo = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(corBarraTitulo)
            setPadding(24, 16, 16, 16)
        }
        val tituloTexto = TextView(context).apply {
            text = "   "
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val botaoMinimizar = criarBotaoJanela(TipoIcone.MINIMIZAR, Color.TRANSPARENT) { minimizar() }
        val botaoExpandir = criarBotaoJanela(TipoIcone.EXPANDIR, Color.TRANSPARENT) { alternarExpandir() }
        val botaoFechar = criarBotaoJanela(TipoIcone.FECHAR, Color.parseColor("#E5484D")) { dismiss() }

        barraTitulo.addView(tituloTexto, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (!telaCheia) {
            barraTitulo.addView(botaoMinimizar)
            barraTitulo.addView(botaoExpandir)
        }
        barraTitulo.addView(botaoFechar)

        // Corpo: terminal com scroll nos dois eixos
        terminalView = JanelaTerminalView(context)
        val scrollV = ScrollView(context).apply {
            setBackgroundColor(corFundoJanela) // mesma cor do terminal: sem faixa cinza quando a saída é curta
            if (telaCheia) {
                // Terminal ocupa a largura da tela e quebra linha nela (sem scroll horizontal)
                addView(terminalView)
            } else {
                addView(HorizontalScrollView(context).apply {
                        setBackgroundColor(corFundoJanela)
                        addView(terminalView)
                })
            }
        }
        scrollVertical = scrollV
        scrollV.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            // área ficou menor (teclado abriu): mantém o fim da saída visível, junto da barra de entrada
            if ((bottom - top) < (oldBottom - oldTop)) rolarParaFim()
        }
        corpoJanela = scrollV

        // Linha de entrada (campo + botão Enviar), escondida até um ler() pedir input
        campoEntrada = EditText(context).apply {
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            hint = "digite o valor..."
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(24, 16, 24, 16)
            setSingleLine(true)
        }
        val botaoEnviar = TextView(context).apply {
            text = "Enter"
            setTextColor(Color.WHITE)
            setBackgroundColor(corBarraTitulo)
            setPadding(32, 16, 32, 16)
            setOnClickListener { confirmarEntrada() }
        }
        campoEntrada.setOnEditorActionListener { _, _, _ -> confirmarEntrada(); true }

        linhaEntrada = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        linhaEntrada.addView(campoEntrada, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        linhaEntrada.addView(botaoEnviar)

        colunaJanela.addView(barraTitulo)
        colunaJanela.addView(scrollV, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        colunaJanela.addView(linhaEntrada)

        containerJanela.addView(colunaJanela)

        // Alça de redimensionar (canto inferior-direito) — só na janela flutuante
        val alcaRedimensionar = View(context).apply {
            setBackgroundColor(Color.parseColor("#8B949E"))
        }
        val tamanhoAlca = 48
        if (!telaCheia) {
            containerJanela.addView(
                alcaRedimensionar,
                FrameLayout.LayoutParams(tamanhoAlca, tamanhoAlca, Gravity.BOTTOM or Gravity.END)
            )
        }

        layoutParamsJanela = if (telaCheia) {
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            FrameLayout.LayoutParams(larguraJanela, alturaJanela).apply { gravity = Gravity.CENTER }
        }
        raiz.addView(containerJanela, layoutParamsJanela)

        setContentView(raiz)

        if (!telaCheia) {
            configurarArrastar(barraTitulo)
            configurarRedimensionar(alcaRedimensionar)
        }
    }

    // -------------------------------------------------------------
    // Arrastar pela barra de título
    // -------------------------------------------------------------

    private fun configurarArrastar(barraTitulo: View) {
        var offsetX = 0f
        var offsetY = 0f

        barraTitulo.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    offsetX = event.rawX - layoutParamsJanela.leftMargin
                    offsetY = event.rawY - layoutParamsJanela.topMargin
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParamsJanela.gravity = Gravity.NO_GRAVITY
                    layoutParamsJanela.leftMargin = (event.rawX - offsetX).toInt().coerceAtLeast(0)
                    layoutParamsJanela.topMargin = (event.rawY - offsetY).toInt().coerceAtLeast(0)
                    containerJanela.layoutParams = layoutParamsJanela
                    containerJanela.requestLayout()
                    true
                }
                else -> false
            }
        }
    }

    // -------------------------------------------------------------
    // Redimensionar pela alça no canto inferior-direito
    // -------------------------------------------------------------

    private fun configurarRedimensionar(alca: View) {
        var inicioX = 0f
        var inicioY = 0f
        var larguraInicial = 0
        var alturaInicial = 0

        alca.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioX = event.rawX
                    inicioY = event.rawY
                    larguraInicial = layoutParamsJanela.width
                    alturaInicial = layoutParamsJanela.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val novaLargura = (larguraInicial + (event.rawX - inicioX)).toInt().coerceAtLeast(300)
                    val novaAltura = (alturaInicial + (event.rawY - inicioY)).toInt().coerceAtLeast(200)
                    layoutParamsJanela.width = novaLargura
                    layoutParamsJanela.height = novaAltura
                    containerJanela.layoutParams = layoutParamsJanela
                    containerJanela.requestLayout()
                    true
                }
                else -> false
            }
        }
    }

    /** Define o texto de saída (com escapes ANSI) a mostrar no terminal e exibe a janela. */
    fun mostrarSaida(textoComAnsi: String) {
        terminalView.mostrarSaida(textoComAnsi)
        rolarParaFim()
        show()
    }

    /** Leva o scroll pro fim da saída (depois do layout, que é quando a altura nova existe). */
    private fun rolarParaFim() {
        scrollVertical.post { scrollVertical.fullScroll(View.FOCUS_DOWN) }
    }

    // -------------------------------------------------------------
    // FonteEntrada: implementação real de ler(), pausa a execução
    // do Interpretador até o usuário digitar e confirmar um valor.
    // -------------------------------------------------------------

    override suspend fun pedirValor(promptLinha: Int): String {
        // Se a janela não está visível, ninguém poderia responder: não suspende
        if (!isShowing) return ""

        // Se já havia um pedido pendente (não deveria acontecer), liberta o anterior
        liberarEntradaPendente("")

        linhaEntrada.visibility = View.VISIBLE
        campoEntrada.setText("")
        campoEntrada.requestFocus()
        rolarParaFim()
        // abre o teclado direto (sem o usuário precisar tocar no campo)
        campoEntrada.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(campoEntrada, InputMethodManager.SHOW_IMPLICIT)
        }

        val valor = suspendCoroutine<String> { continuacao ->
            continuacaoPendente = continuacao
        }

        linhaEntrada.visibility = View.GONE
        return valor
    }

    private fun confirmarEntrada() {
        liberarEntradaPendente(campoEntrada.text.toString())
    }

    /** Retoma, com o valor dado, um ler() que esteja à espera (no máximo uma vez). */
    private fun liberarEntradaPendente(valor: String) {
        val continuacao = continuacaoPendente ?: return
        continuacaoPendente = null
        continuacao.resume(valor)
    }

    /** Atualiza o terminal em tempo real durante a execução (chamado pelo Interpretador
*  via callback de imprimir, se disponível), sem esperar o programa terminar. */
    fun atualizarSaidaParcial(textoComAnsi: String) {
        terminalView.mostrarSaida(textoComAnsi)
        rolarParaFim()
    }

    // -------------------------------------------------------------
    // Botões de janela: minimizar, expandir, fechar — ícones desenhados via Canvas
    // -------------------------------------------------------------

    private enum class TipoIcone { MINIMIZAR, EXPANDIR, FECHAR }

    private inner class IconeBotaoJanela(
        ctx: Context,
        private val tipo: TipoIcone
    ) : View(ctx) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        init {
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.style = android.graphics.Paint.Style.STROKE
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val margem = width * 0.3f
            val esquerda = margem
            val direita = width - margem
            val topo = height * 0.3f
            val baixo = height - height * 0.3f

            when (tipo) {
                TipoIcone.MINIMIZAR -> {
                    canvas.drawLine(esquerda, baixo, direita, baixo, paint)
                }
                TipoIcone.EXPANDIR -> {
                    canvas.drawRect(esquerda, topo, direita, baixo, paint)
                }
                TipoIcone.FECHAR -> {
                    canvas.drawLine(esquerda, topo, direita, baixo, paint)
                    canvas.drawLine(direita, topo, esquerda, baixo, paint)
                }
            }
        }
    }

    private fun criarBotaoJanela(tipo: TipoIcone, corFundo: Int, aoClicar: () -> Unit): View {
        val tamanho = 88
        val icone = IconeBotaoJanela(context, tipo)
        val botao = FrameLayout(context).apply {
            setBackgroundColor(corFundo)
            addView(icone, FrameLayout.LayoutParams(tamanho, tamanho))
            setOnClickListener { aoClicar() }
        }
        return botao
    }
    /** Minimiza: encolhe a janela para uma barra pequena no canto, escondendo o corpo. */
    private fun minimizar() {
        if (estaMinimizada) {
            // já minimizada -> restaura
            corpoJanela.visibility = View.VISIBLE
            layoutParamsJanela.width = larguraAnterior
            layoutParamsJanela.height = alturaAnterior
            estaMinimizada = false
        } else {
            guardarGeometriaAtual()
            corpoJanela.visibility = View.GONE
            layoutParamsJanela.width = 320
            layoutParamsJanela.height = LinearLayout.LayoutParams.WRAP_CONTENT
            estaMinimizada = true
            estaExpandida = false
        }
        containerJanela.layoutParams = layoutParamsJanela
        containerJanela.requestLayout()
    }

    /** Expande: ocupa a tela quase inteira. Chamar de novo restaura o tamanho anterior. */
    private fun alternarExpandir() {
        if (estaExpandida) {
            layoutParamsJanela.width = larguraAnterior
            layoutParamsJanela.height = alturaAnterior
            layoutParamsJanela.leftMargin = margemEsquerdaAnterior
            layoutParamsJanela.topMargin = margemTopoAnterior
            layoutParamsJanela.gravity = Gravity.NO_GRAVITY
            estaExpandida = false
        } else {
            guardarGeometriaAtual()
            corpoJanela.visibility = View.VISIBLE
            val metricas = context.resources.displayMetrics
            layoutParamsJanela.width = (metricas.widthPixels * 0.95).toInt()
            layoutParamsJanela.height = (metricas.heightPixels * 0.85).toInt()
            layoutParamsJanela.leftMargin = 0
            layoutParamsJanela.topMargin = 0
            layoutParamsJanela.gravity = Gravity.CENTER
            estaExpandida = true
            estaMinimizada = false
        }
        containerJanela.layoutParams = layoutParamsJanela
        containerJanela.requestLayout()
    }

    private fun guardarGeometriaAtual() {
        if (!estaMinimizada && !estaExpandida) {
            larguraAnterior = layoutParamsJanela.width
            alturaAnterior = layoutParamsJanela.height
            margemEsquerdaAnterior = layoutParamsJanela.leftMargin
            margemTopoAnterior = layoutParamsJanela.topMargin
        }
    }
}
