package co.adilson889.typec.port

import co.adilson889.typec.lexer.tokenizarFonte
import co.adilson889.typec.parser.Parser
import co.adilson889.typec.interpretador.*
import java.io.File
import kotlin.coroutines.*

/**
 * Pequeno executor JVM para verificar o interpretador comum à aplicação e ao APK,
 * sem depender de um emulador Android. Não altera a interface do programa.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Indique o ficheiro .port a executar." }
    val ficheiro = File(args[0])
    val fonte = ficheiro.readText()
    val entradas = if (args.size > 1) File(args[1]).readLines() else emptyList()
    val fila = java.util.ArrayDeque(entradas)
    val entrada = object : FonteEntrada {
        override suspend fun pedirValor(promptLinha: Int): String {
            if (fila.isEmpty()) error("Faltam dados de entrada na linha $promptLinha.")
            return fila.removeFirst()
        }
    }
    val programa = Parser(tokenizarFonte(fonte), fonte, ficheiro.name).parsear()
    var resposta: ResultadoExecucao? = null
    var falha: Throwable? = null
    val tarefa: suspend () -> ResultadoExecucao = {
        Interpretador(fonteEntrada = entrada).executar(programa)
    }
    tarefa.startCoroutine(object : Continuation<ResultadoExecucao> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<ResultadoExecucao>) {
            result.fold({ resposta = it }, { falha = it })
        }
    })
    falha?.let { throw it }
    when (val r = resposta ?: error("A execução ficou suspensa inesperadamente.")) {
        is ResultadoExecucao.Sucesso -> print(r.saida)
        is ResultadoExecucao.Erro -> error("Linha ${r.linha}: ${r.mensagem}")
        is ResultadoExecucao.Interrompido -> error("Execução interrompida: ${r.mensagem}")
    }
}
