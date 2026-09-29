package co.adilson889.typec

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText

// -------------------------------------------------------------
// Editor de código com calha de números de linha. Puramente
// apresentação — não conhece Lexer/Parser/Transpilador, só EditText.
// -------------------------------------------------------------
class CodeEditor(context: Context) : EditText(context) {

    private val corNumero = Color.parseColor("#6E7681")
    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = corNumero
        textAlign = Paint.Align.RIGHT
    }
    private var larguraCalha = 0

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    fun atualizarCalha() {
        gutterPaint.textSize = textSize
        val digitos = lineCount.toString().length.coerceAtLeast(2)
        val nova = (gutterPaint.measureText("0".repeat(digitos)) + dp(28)).toInt()
        if (nova != larguraCalha) {
            larguraCalha = nova
            setPadding(larguraCalha, paddingTop, paddingRight, paddingBottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val layoutAtual = layout
        if (layoutAtual != null && larguraCalha > 0) {
            gutterPaint.textSize = textSize
            for (i in 0 until lineCount) {
                val baseline = layoutAtual.getLineBaseline(i) + paddingTop
                canvas.drawText((i + 1).toString(), (larguraCalha - dp(10)).toFloat(), baseline.toFloat(), gutterPaint)
            }
        }
        super.onDraw(canvas)
    }
}

// -------------------------------------------------------------
// Realce de sintaxe + indentação automática para TypeC, no estilo
// de cores de um editor C comum (palavra-chave, tipo, modificador,
// I/O, booleano, constante, string, número, comentário, chamada de
// função, operador).
// -------------------------------------------------------------
class RealceTypeC(private val editor: CodeEditor) : TextWatcher {

    private val corPalavraChave = Color.parseColor("#FF7B9C")   // se, senao, enquanto, para, escolha, caso, casocontrario, retorne, pare, continue, alterar, estrutura, funcao, inicio, inclua
    private val corTipo = Color.parseColor("#569CD6")           // inteiro, real, duplo, texto, caractere, logico, vazio, tempo
    private val corModificador = Color.parseColor("#4EC9B0")    // longo, curto, positivo (modificadores de tipo composto)
    private val corTexto = Color.parseColor("#CE9178")          // strings e caracteres
    private val corNumero = Color.parseColor("#B5CEA8")
    private val corComentario = Color.parseColor("#6A9955")
    private val corFuncao = Color.parseColor("#DCDCAA")         // chamadas de função, incluindo escreva/leia e libs
    private val corConstante = Color.parseColor("#4FC1FF")      // PI, E
    private val corBooleano = Color.parseColor("#569CD6")       // verdadeiro, falso
    private val corOperador = Color.parseColor("#D4D4D4")       // + - * / % = == != > < >= <= && || !

    private val palavrasChave = listOf(
        "se", "senao", "enquanto", "para", "cada", "em", "escolha", "caso", "casocontrario",
        "pare", "continue", "retorne", "altere", "estrutura", "funcao", "inicio", "inclua"
    )
    private val tipos = listOf(
        "inteiro", "real", "duplo", "texto", "caractere", "logico", "vazio", "tempo"
    )
    private val modificadoresTipo = listOf("longo", "curto","inicio", "positivo")
    private val booleanos = listOf("verdadeiro", "falso")

    // nomes que não devem ganhar cor de "chamada de função" mesmo seguidos de '(' —
    // já são coloridos como palavra-chave (evita colorir duas vezes)
    private val palavrasControle = setOf(
        "se", "senao", "enquanto", "para", "escolha", "retorne"
    )

    private var atualizando = false
    private var comprimentoAntes = 0

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        comprimentoAntes = s?.length ?: 0
    }
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable) {
        if (atualizando) return
        atualizando = true
        try {
            val inseriu = editable.length > comprimentoAntes
            if (inseriu) {
                autoDedentChave(editable)
                autoIndentEnter(editable)
            }
            aplicarRealce(editable)
            editor.atualizarCalha()
        } finally {
            atualizando = false
        }
    }

    // ---- indentação automática ----

    private fun autoIndentEnter(editable: Editable) {
        val cursor = editor.selectionStart
        if (cursor < 1 || cursor > editable.length) return
        if (editable[cursor - 1] != '\n') return

        var inicioLinhaAnterior = cursor - 1
        while (inicioLinhaAnterior > 0 && editable[inicioLinhaAnterior - 1] != '\n') inicioLinhaAnterior--
        val linhaAnterior = editable.substring(inicioLinhaAnterior, cursor - 1)
        val indentAtual = Regex("^[ ]*").find(linhaAnterior)?.value ?: ""

        var indentNova = indentAtual
        if (linhaAnterior.trimEnd().endsWith("{")) indentNova += "    "

        if (indentNova.isNotEmpty()) {
            editable.insert(cursor, indentNova)
            editor.setSelection(cursor + indentNova.length)
        }
    }

    private fun autoDedentChave(editable: Editable) {
        val cursor = editor.selectionStart
        if (cursor < 1 || cursor > editable.length) return
        if (editable[cursor - 1] != '}') return

        var inicioLinha = cursor - 1
        while (inicioLinha > 0 && editable[inicioLinha - 1] != '\n') inicioLinha--
        val antesDaChave = editable.substring(inicioLinha, cursor - 1)
        if (antesDaChave.isNotEmpty() && antesDaChave.isBlank()) {
            val remover = minOf(4, antesDaChave.length)
            editable.delete(inicioLinha, inicioLinha + remover)
            editor.setSelection(cursor - remover)
        }
    }

    // ---- realce de sintaxe ----

    private fun aplicarRealce(editable: Editable) {
        val texto = editable.toString()
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java).forEach {
            editable.removeSpan(it)
        }

        fun pintar(regex: Regex, cor: Int) {
            regex.findAll(texto).forEach { m ->
                editable.setSpan(ForegroundColorSpan(cor), m.range.first, m.range.last + 1, Editable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // ordem importa pouco (spans não se sobrepõem em texto/número/comentário/palavra),
        // mas comentário e string por último evita que o conteúdo deles seja repintado
        pintar(Regex("\\b\\d+(\\.\\d+)?\\b"), corNumero)
        pintar(Regex("\\b(${palavrasChave.joinToString("|")})\\b"), corPalavraChave)
        pintar(Regex("\\b(${tipos.joinToString("|")})\\b"), corTipo)
        pintar(Regex("\\b(${modificadoresTipo.joinToString("|")})\\b"), corModificador)
        pintar(Regex("\\b(${booleanos.joinToString("|")})\\b"), corBooleano)
        pintar(Regex("\\b(PI|E)\\b"), corConstante)
        pintar(Regex("(\\+\\+|--|[+\\-*/%]=?|==|!=|>=|<=|&&|\\|\\||[!><=])"), corOperador)

        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()").findAll(texto).forEach { m ->
            val nome = m.groupValues[1]
            if (nome !in palavrasControle) {
                val faixa = m.groups[1]!!.range
                editable.setSpan(ForegroundColorSpan(corFuncao), faixa.first, faixa.last + 1, Editable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        pintar(Regex("'([^'\\\\]|\\\\.)'"), corTexto)
        pintar(Regex("\"([^\"\\\\]|\\\\.)*\""), corTexto)
        pintar(Regex("//.*"), corComentario)
        pintar(Regex("/\\*[\\s\\S]*?\\*/"), corComentario)
    }
}
