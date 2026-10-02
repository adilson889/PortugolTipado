package co.adilson889.typec.diagnostico

/**
 * Palavras que o editor marca ao vivo, sem passar pelo compilador.
 *
 * Sao as deprecacoes e as formas do Portugol classico que o PortugolTipado
 * prefere escrever de outra maneira. A tabela e usada tanto pelo editor
 * (realce ao vivo) como pelo LexerPortugol (traducao dos tokens), para as
 * duas listas nunca ficarem diferentes.
 *
 * modo:
 *   PORT -> so vale em arquivos .port (PortugolTipado)
 *   POR  -> so vale em arquivos .por  (Portugol classico)
 *   AMBOS-> vale nos dois
 *
 * 'gravidade' decide a cor no editor:
 *   AVISO -> amarelo, sublinhado ondulado (a palavra ainda funciona)
 *   ERRO  -> vermelho, sublinhado ondulado (nao compila)
 */
object TabelaPalavras {

    enum class Modo { PORT, POR, AMBOS }

    data class Entrada(
        val palavra: String,
        val sugestao: String,
        val modo: Modo,
        val gravidade: Gravidade
    )

    // Deprecacoes e formas do Portugol classico.
    // Ordem nao importa: a busca e feita palavra a palavra.
    val entradas: List<Entrada> = listOf(
        // Deprecacoes do PortugolTipado (funcionam, mas a forma nova e preferida)
        Entrada("alterar", "altere", Modo.AMBOS, Gravidade.AVISO),
        Entrada("altera", "altere", Modo.AMBOS, Gravidade.AVISO),

        // Formas do Portugol classico (so fazem sentido em .por)
        Entrada("cadeia", "texto", Modo.POR, Gravidade.AVISO),
        Entrada("caracter", "caractere", Modo.POR, Gravidade.AVISO),
        Entrada("e", "&&", Modo.POR, Gravidade.AVISO),
        Entrada("ou", "||", Modo.POR, Gravidade.AVISO),
        Entrada("nao", "!", Modo.POR, Gravidade.AVISO),
        Entrada("caso contrario", "casocontrario", Modo.POR, Gravidade.AVISO)
    )

    /**
     * Devolve as entradas que valem no modo dado.
     * Se 'ehPortugolClassico' for verdadeiro, o arquivo e .por; senao e .port.
     */
    // Strings, caracteres e comentarios, lidos da esquerda para a direita:
    // dentro deles nao se marca nada.
    private val ignorados = Regex(
        "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)'|//[^\\n]*|/\\*[\\s\\S]*?\\*/"
    )

    // O arquivo comeca (depois de espacos e comentarios) com 'programa {': Portugol classico
    private val abrePrograma = Regex(
        "\\A(?:\\s|//[^\\n]*|/\\*[\\s\\S]*?\\*/)*programa\\s*\\{"
    )

    fun ehClassico(texto: String): Boolean = abrePrograma.containsMatchIn(texto)

    // Uma regex por entrada, montada uma vez ('caso contrario' aceita varios espacos;
    // 'alterar' so conta quando nao e chamada de funcao: alterar(...))
    private class Regra(val entrada: Entrada, val regex: Regex)

    private val regras: List<Regra> by lazy {
        entradas.map { e ->
            val corpo = e.palavra.split(" ").joinToString("\\s+") { Regex.escape(it) }
            val fim = if (e.modo == Modo.POR) "" else "(?!\\s*\\()"
            Regra(e, Regex("\\b$corpo\\b$fim"))
        }
    }

    /**
     * Procura as palavras da tabela no proprio texto (sem Lexer nem Parser):
     * funciona enquanto se digita, mesmo com o codigo incompleto.
     * O modo (classico ou nao) vem do inicio do texto.
     */
    fun analisar(texto: String): List<Diagnostico> {
        if (texto.isEmpty()) return emptyList()

        val mascara = BooleanArray(texto.length)
        ignorados.findAll(texto).forEach { m -> for (i in m.range) mascara[i] = true }

        val classico = ehClassico(texto)
        val saida = ArrayList<Diagnostico>()
        for (r in regras) {
            val valeAqui = when (r.entrada.modo) {
                Modo.AMBOS -> true
                Modo.POR -> classico
                Modo.PORT -> !classico
            }
            if (!valeAqui) continue
            r.regex.findAll(texto).forEach { m ->
                if (mascara[m.range.first]) return@forEach
                val mensagem = if (r.entrada.modo == Modo.POR)
                    "Portugol clássico: use '${r.entrada.sugestao}'"
                else
                    "obsoleto: use '${r.entrada.sugestao}'"
                // linha (a partir de 1) = quebras de linha antes da palavra + 1
                var linha = 1
                for (i in 0 until m.range.first) if (texto[i] == '\n') linha++
                saida.add(Diagnostico(linha, r.entrada.gravidade, mensagem, m.range.first, m.range.last + 1))
            }
        }
        saida.sortBy { it.inicio }
        return saida
    }

    fun paraModo(ehPortugolClassico: Boolean): List<Entrada> =
        entradas.filter {
            when (it.modo) {
                Modo.AMBOS -> true
                Modo.POR -> ehPortugolClassico
                Modo.PORT -> !ehPortugolClassico
            }
        }
}