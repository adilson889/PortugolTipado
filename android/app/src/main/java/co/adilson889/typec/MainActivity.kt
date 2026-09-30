package co.adilson889.typec

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import co.adilson889.typec.CodeEditor
import co.adilson889.typec.RealceTypeC
import co.adilson889.typec.lexer.Lexer
import co.adilson889.typec.parser.Parser
import co.adilson889.typec.transpilador.Transpilador
import java.io.File
import kotlin.coroutines.startCoroutine

class MainActivity : Activity() {

    private lateinit var editor: CodeEditor
    private lateinit var codigoGeradoView: TextView
    private var ultimaSaidaPreview: String = ""
    private lateinit var abaCodigoBtn: Button
    private lateinit var abaGeradoBtn: Button
    private lateinit var telaCodigo: View
    private lateinit var telaGerado: View

    private lateinit var painelLateral: LinearLayout
    private lateinit var scrimLateral: View
    private lateinit var listaArquivosContainer: LinearLayout
    private var sidebarAberta = false
    private var arquivoAtual: String? = null // nome do arquivo aberto (null = ainda não salvo)

    private val corFundo = Color.parseColor("#0D1117")
    private val corPainel = Color.parseColor("#161B22")
    private val corTexto = Color.parseColor("#E6EDF3")
    private val corRoxo = Color.parseColor("#6200EE")
    private val corVerde = Color.parseColor("#2EA043")
    private val corInativo = Color.parseColor("#21262D")

    private var abaAtual = 0 // 0 = codigo, 1 = gerado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutRaiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(corFundo)
        }

        // Cabeçalho (título + botão de menu pra sidebar)
        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(corRoxo)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 40, 32, 16)
        }
        val botaoMenu = Button(this).apply {
            text = "Menu"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
        }
        val tituloTexto = TextView(this).apply {
            text = "PortugolTipado"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }
        cabecalho.addView(botaoMenu)
        cabecalho.addView(tituloTexto)
        layoutRaiz.addView(cabecalho)
        botaoMenu.setOnClickListener { alternarSidebar() }

        // Abas (2: Código e Gerado — Preview agora é janela flutuante, não aba)
        val linhaAbas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        abaCodigoBtn = criarBotaoAba("Código")
        abaGeradoBtn = criarBotaoAba("Gerado")
        linhaAbas.addView(abaCodigoBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        linhaAbas.addView(abaGeradoBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layoutRaiz.addView(linhaAbas)

        abaCodigoBtn.setOnClickListener { mostrarAba(0) }
        abaGeradoBtn.setOnClickListener { mostrarAba(1) }

        // Container das 2 telas
        val container = FrameLayout(this)
        telaCodigo = construirTelaCodigo()
        telaGerado = construirTelaGerado()
        container.addView(telaCodigo)
        container.addView(telaGerado)
        layoutRaiz.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(construirRaizComSidebar(layoutRaiz))
        mostrarAba(0)
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    // ---- Raiz externa: conteúdo normal + scrim + sidebar sobrepostos ----

    private fun construirRaizComSidebar(conteudo: View): View {
        val raizExterna = FrameLayout(this)
        raizExterna.addView(conteudo)

        scrimLateral = View(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            visibility = View.GONE
            setOnClickListener { fecharSidebar() }
        }
        raizExterna.addView(scrimLateral, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        painelLateral = construirSidebar()
        painelLateral.visibility = View.GONE
        raizExterna.addView(painelLateral, FrameLayout.LayoutParams(dp(260), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START))

        return raizExterna
    }

    private fun construirSidebar(): LinearLayout {
        val painel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(corPainel)
        }

        val tituloSidebar = criarRotulo("Arquivos")

        val botaoNovo = Button(this).apply {
            text = "+ Novo"
            setBackgroundColor(corInativo)
            setTextColor(corTexto)
            setOnClickListener { novoArquivo() }
        }
        val botaoSalvar = Button(this).apply {
            text = "Salvar"
            setBackgroundColor(corRoxo)
            setTextColor(Color.WHITE)
            setOnClickListener { salvarArquivo() }
        }
        val linhaBotoesSidebar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        linhaBotoesSidebar.addView(botaoNovo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        linhaBotoesSidebar.addView(botaoSalvar, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        listaArquivosContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scrollLista = ScrollView(this).apply { addView(listaArquivosContainer) }

        painel.addView(tituloSidebar)
        painel.addView(linhaBotoesSidebar)
        painel.addView(scrollLista, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        return painel
    }

    private fun alternarSidebar() {
        if (sidebarAberta) fecharSidebar() else abrirSidebar()
    }

    private fun abrirSidebar() {
        carregarListaArquivos()
        scrimLateral.visibility = View.VISIBLE
        painelLateral.visibility = View.VISIBLE
        painelLateral.translationX = -dp(260).toFloat()
        painelLateral.animate().translationX(0f).setDuration(200).start()
        sidebarAberta = true
    }

    private fun fecharSidebar() {
        painelLateral.animate().translationX(-dp(260).toFloat()).setDuration(200).withEndAction {
            painelLateral.visibility = View.GONE
            scrimLateral.visibility = View.GONE
        }.start()
        sidebarAberta = false
    }

    // ---- Arquivos: pasta externa do app, visível no explorador do telefone ----
    // Caminho típico: Android/data/co.adilson889.typec/files/typec/

    private fun pastaTypeC(): File {
        val pasta = File(getExternalFilesDir(null), "typec")
        if (!pasta.exists()) pasta.mkdirs()
        return pasta
    }

    private fun carregarListaArquivos() {
        listaArquivosContainer.removeAllViews()
        // ".port" e a extensao atual; ".typec" e aceita so para nao esconder ficheiros antigos
        val arquivos = pastaTypeC().listFiles { f -> f.isFile && (f.name.endsWith(".port") || f.name.endsWith(".typec")) }
        ?.sortedBy { it.name } ?: emptyList()

        if (arquivos.isEmpty()) {
            val vazio = TextView(this).apply {
                text = "Nenhum arquivo salvo ainda"
                setTextColor(Color.parseColor("#6E7681"))
                textSize = 13f
                setPadding(24, 24, 24, 24)
            }
            listaArquivosContainer.addView(vazio)
            return
        }

        for (arquivo in arquivos) {
            val item = TextView(this).apply {
                text = arquivo.name.substringBeforeLast(".")
                setTextColor(corTexto)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setPadding(24, 24, 24, 24)
                setOnClickListener {
                    editor.setText(arquivo.readText())
                    arquivoAtual = arquivo.name
                    fecharSidebar()
                }
            }
            listaArquivosContainer.addView(item)
        }
    }

    private fun novoArquivo() {
        editor.setText("")
        arquivoAtual = null
        fecharSidebar()
    }

    private fun salvarArquivo() {
        val nomeJaExistente = arquivoAtual
        if (nomeJaExistente != null) {
            File(pastaTypeC(), nomeJaExistente).writeText(editor.text.toString())
            Toast.makeText(this, "Salvo: $nomeJaExistente", Toast.LENGTH_SHORT).show()
            carregarListaArquivos()
            return
        }
        pedirNomeEsalvar()
    }

    private fun pedirNomeEsalvar() {
        val campoNome = EditText(this).apply {
            hint = "nome_do_arquivo"
            setTextColor(corTexto)
            setHintTextColor(Color.parseColor("#6E7681"))
        }
        AlertDialog.Builder(this)
        .setTitle("Salvar como")
        .setView(campoNome)
        .setPositiveButton("Salvar") { _, _ ->
            val nome = campoNome.text.toString().trim()
            if (nome.isNotEmpty()) {
                val nomeArquivo = if (nome.endsWith(".port")) nome else "$nome.port"
                File(pastaTypeC(), nomeArquivo).writeText(editor.text.toString())
                arquivoAtual = nomeArquivo
                Toast.makeText(this, "Salvo: $nomeArquivo", Toast.LENGTH_SHORT).show()
                carregarListaArquivos()
            }
        }
        .setNegativeButton("Cancelar", null)
        .show()
    }

    private fun criarBotaoAba(texto: String): Button {
        return Button(this).apply {
            text = texto
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
    }

    private fun mostrarAba(aba: Int) {
        abaAtual = aba
        telaCodigo.visibility = if (aba == 0) View.VISIBLE else View.GONE
        telaGerado.visibility = if (aba == 1) View.VISIBLE else View.GONE
        abaCodigoBtn.setBackgroundColor(if (aba == 0) corRoxo else corInativo)
        abaGeradoBtn.setBackgroundColor(if (aba == 1) corRoxo else corInativo)
    }

    // ---- Aba 1: Código (editor) ----

    private fun construirTelaCodigo(): View {
        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        editor = CodeEditor(this).apply {
            setTextColor(corTexto)
            setBackgroundColor(corFundo)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(16, 32, 32, 32)
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
            maxLines = Integer.MAX_VALUE
            isVerticalScrollBarEnabled = true
            setTextIsSelectable(true)
        }
        editor.addTextChangedListener(RealceTypeC(editor))
        editor.setText(
            "  "
        )

        tela.addView(editor, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val linhaBotoes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val botaoPreview = Button(this).apply {
            text = "▶ Preview"
            setBackgroundColor(corVerde)
            setTextColor(Color.WHITE)
        }
        val botaoConverter = Button(this).apply {
            text = "▶ Converter"
            setBackgroundColor(corRoxo)
            setTextColor(Color.WHITE)
        }
        linhaBotoes.addView(botaoPreview, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        linhaBotoes.addView(botaoConverter, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tela.addView(linhaBotoes)

        botaoConverter.setOnClickListener {
            converter()
            mostrarAba(1)
        }
        botaoPreview.setOnClickListener {
            rodarPreview()
        }

        return tela
    }

    // ---- Preview: janela flutuante estilo desktop (não é mais aba) ----

    private var janelaFlutuante: co.adilson889.typec.terminal.JanelaFlutuante? = null

    // ---- Aba 3: Gerado (código C) ----

    private fun construirTelaGerado(): View {
        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rotulo = criarRotulo("Código C Gerado")
        codigoGeradoView = criarSaida("O codigo C gerado vai aparecer aqui.\n\nUse o botão Converter na aba Código.")
        val scroll = ScrollView(this).apply { addView(codigoGeradoView) }

        val botaoCopiar = criarBotaoCopiar { codigoGeradoView.text.toString() }

        tela.addView(rotulo)
        tela.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        tela.addView(botaoCopiar)

        return tela
    }

    private fun criarBotaoCopiar(obterTexto: () -> String): Button {
        return Button(this).apply {
            text = "Copiar"
            setBackgroundColor(corInativo)
            setTextColor(corTexto)
            setOnClickListener {
                val gerenciador = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                gerenciador.setPrimaryClip(ClipData.newPlainText("PortugolTipado", obterTexto()))
                Toast.makeText(this@MainActivity, "Copiado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun criarRotulo(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(corPainel)
            setPadding(24, 12, 24, 12)
        }
    }

    private fun criarSaida(textoInicial: String): TextView {
        return TextView(this).apply {
            setTextColor(corTexto)
            setBackgroundColor(corPainel)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(32, 24, 32, 24)
            setTextIsSelectable(true)
            text = textoInicial
        }
    }

    private fun converter() {
        val codigoTypeC = editor.text.toString()

        codigoGeradoView.text = try {
            val tokens = Lexer(codigoTypeC).tokenizar()
            val programa = Parser(tokens, codigoTypeC).parsear()
            co.adilson889.typec.validador.Validador(codigoTypeC).validar(programa)
            val codigoC = Transpilador().transpilar(programa)
            codigoC
        } catch (e: co.adilson889.typec.erros.ErroTypeC) {
            e.formatar()
        } catch (e: Exception) {
            "Erro inesperado:\n${e.message}"
        }
    }

    private fun rodarPreview() {
        val codigoTypeC = editor.text.toString()

        if (janelaFlutuante == null) {
            janelaFlutuante = co.adilson889.typec.terminal.JanelaFlutuante(this)
        }
        val janela = janelaFlutuante ?: return

        janela.mostrarSaida("Executando...\n")

        rodarSuspendNaUiThread {
            val resultadoTexto = try {
                val tokens = Lexer(codigoTypeC).tokenizar()
                val programa = Parser(tokens, codigoTypeC).parsear()
                co.adilson889.typec.validador.Validador(codigoTypeC).validar(programa)
                val interpretador = co.adilson889.typec.interpretador.Interpretador(
                    fonteEntrada = janela,
                    aoImprimir = { saidaAteAgora -> janela.atualizarSaidaParcial(saidaAteAgora) },
                    graficos = co.adilson889.typec.graficos.GraficosCanvas(this)
                )
                when (val resultado = interpretador.executar(programa)) {
                    is co.adilson889.typec.interpretador.ResultadoExecucao.Sucesso ->
                    if (resultado.saida.isEmpty()) "(o programa rodou mas nao imprimiu nada)" else resultado.saida
                    is co.adilson889.typec.interpretador.ResultadoExecucao.Erro ->
                    "Erro na linha ${resultado.linha}:\n${resultado.mensagem}"
                    is co.adilson889.typec.interpretador.ResultadoExecucao.Interrompido -> {
                        val aviso = "Aviso na linha ${resultado.linha}:\n${resultado.mensagem}"
                        if (resultado.saida.isEmpty()) aviso else resultado.saida + "\n\n" + aviso
                    }
                }
            } catch (e: co.adilson889.typec.erros.ErroTypeC) {
                e.formatar()
            } catch (e: Exception) {
                "Erro inesperado:\n${e.message}"
            }

            ultimaSaidaPreview = resultadoTexto
            janela.mostrarSaida(resultadoTexto)
        }
    }

    /** Roda um bloco 'suspend' na thread principal (UI), sem depender de
*  kotlinx.coroutines — usa apenas o suporte nativo de corrotinas do
*  Kotlin (kotlin.coroutines), compatível com qualquer versão do Kotlin. */
    private fun rodarSuspendNaUiThread(bloco: suspend () -> Unit) {
        bloco.startCoroutine(object : kotlin.coroutines.Continuation<Unit> {
                override val context = kotlin.coroutines.EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {
                    result.exceptionOrNull()?.printStackTrace()
                }
        })
    }
}
