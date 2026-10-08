package co.adilson889.portugoltipado.apk

import android.content.Context
import co.adilson889.typec.ast.Programa
import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.graficos.GraficosCanvas
import co.adilson889.typec.interpretador.FonteEntrada
import co.adilson889.typec.interpretador.Interpretador
import co.adilson889.typec.interpretador.ResultadoExecucao
import co.adilson889.typec.lexer.tokenizarFonte
import co.adilson889.typec.parser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ErroApk(mensagem: String) : Exception(mensagem)

/**
 * Lê o programa dos assets (projeto/ e programa.txt, postos lá pelo workflow) e corre-o
 * no Interpretador, com a janela gráfica (GraficosCanvas) e os componentes HTML
 * (biblioteca 'interface'). O escreva/leia de texto continua nos diálogos.
 * Quando o programa acaba, a janela fecha: fim do programa é fim da app.
 */
class Executor(private val contexto: Context, private val ui: DialogoUssd) {

    @Volatile private var total = ""   // tudo o que o programa já escreveu
    private var consumido = 0          // quanto disso já foi mostrado num diálogo

    suspend fun correr() {
        val carregado = try {
            withContext(Dispatchers.Default) {
                val modulos = mutableListOf<Programa>()
                val principal = lerAsset("programa.txt").trim().ifEmpty { "principal.port" }
                val programa = carregar(principal, linkedSetOf(principal), modulos)
                Pair(programa, modulos)
            }
        } catch (e: ErroTypeC) {
            ui.mostrar(e.formatar()); return
        } catch (e: Exception) {
            ui.mostrar("Erro: " + (e.message ?: "não foi possível abrir o programa")); return
        }

        val canvas = GraficosCanvas(contexto)
        val interfaceUi = criarInterface(contexto, canvas)
        val interpretador = Interpretador(
            fonteEntrada = Entrada(),
            aoImprimir = { total = it },
            graficos = canvas,
            interfaceUi = interfaceUi
        )

        // O interpretador corre na thread principal: o GraficosCanvas e a InterfaceAndroid
        // mexem em vistas, e renderize/aguarde devolvem o controlo a cada quadro.
        val resultado = try {
            interpretador.executar(carregado.first, carregado.second)
        } finally {
            canvas.fecheJanela()
            interfaceUi.liberar()
        }

        when (resultado) {
            is ResultadoExecucao.Sucesso -> {
                total = resultado.saida
                val resto = pendente()
                if (resto.isNotBlank()) ui.mostrar(resto)
            }
            is ResultadoExecucao.Erro -> {
                ui.mostrar(juntar(pendente(), "Erro na linha ${resultado.linha}: ${resultado.mensagem}"))
            }
            is ResultadoExecucao.Interrompido -> {
                total = resultado.saida
                ui.mostrar(juntar(pendente(), resultado.mensagem))
            }
        }
    }

    private inner class Entrada : FonteEntrada {
        override suspend fun pedirValor(promptLinha: Int): String {
            val resposta = ui.perguntar(pendente())
                ?: throw CancellationException("Cancelado pelo utilizador")
            return resposta
        }
    }

    /** O texto escrito desde o último diálogo. */
    private fun pendente(): String {
        val t = total
        val novo = if (consumido <= t.length) t.substring(consumido) else t
        consumido = t.length
        return novo.trim('\n', '\r', ' ')
    }

    private fun juntar(texto: String, extra: String) =
        if (texto.isBlank()) extra else texto + "\n\n" + extra

    /** Lê e analisa um ficheiro; os módulos 'inclua "x"' entram antes de quem os usa. */
    private fun carregar(caminho: String, vistos: MutableSet<String>, modulos: MutableList<Programa>): Programa {
        val fonte = lerAsset("projeto/$caminho")
        val programa = Parser(tokenizarFonte(fonte, caminho), fonte, caminho).parsear()
        val pasta = caminho.substringBeforeLast('/', "")
        for (inc in programa.includes) {
            if (!inc.ehArquivoLocal) continue
            val rel = inc.nomeLib + ".port"
            val candidatos = listOf(if (pasta.isEmpty()) rel else "$pasta/$rel", rel)
            val achado = candidatos.firstOrNull { existe("projeto/$it") }
                ?: throw ErroApk("Módulo não encontrado: \"${inc.nomeLib}\" (linha ${inc.linha} de $caminho)")
            if (vistos.add(achado)) {
                modulos.add(carregar(achado, vistos, modulos))
            }
        }
        return programa
    }

    private fun existe(caminho: String): Boolean =
        try {
            contexto.assets.open(caminho).close(); true
        } catch (_: IOException) {
            false
        }

    private fun lerAsset(caminho: String): String =
        contexto.assets.open(caminho).bufferedReader(Charsets.UTF_8).use { it.readText() }
            .removePrefix("\uFEFF")
}
