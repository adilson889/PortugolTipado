package co.adilson889.typec.lexer

/**
 * Regras partilhadas pelo Lexer e pelo CorpoParser para ler o corpo de um
 * 'componente' (biblioteca 'interface'): quem é controlo, onde acaba um <style>.
 */
object RegrasCorpo {

    /**
     * Texto que vem antes de um '{' e o torna um bloco de controlo (e não um valor):
     * se (...), senao, senao se (...), enquanto (...), para (...), para cada (...).
     * O texto avaliado é o que há desde o início da linha (ou do último '{' / '}').
     */
    val CONTROLE = Regex("""^\s*((se|enquanto|para(\s+cada)?)\s*\(.*\)|senao(\s+se\s*\(.*\))?)\s*$""")

    /** O que pode vir entre 'senao' e '{': só um 'se (...)'. */
    val SE_ISOLADO = Regex("""^se\s*\(.*\)$""")

    /** Verdadeiro se em [pos] começa a tag [tag] (por exemplo "<style"), sem confundir com "<styles". */
    fun comecaTag(texto: String, pos: Int, tag: String): Boolean {
        if (!texto.regionMatches(pos, tag, 0, tag.length, ignoreCase = true)) return false
        val seguinte = texto.getOrElse(pos + tag.length) { '>' }
        return seguinte == '>' || seguinte == '/' || seguinte.isWhitespace()
    }

    /** Índice logo depois do </style> que fecha o <style> em [pos], ou -1 se não fecha. */
    fun fimDoStyle(texto: String, pos: Int): Int {
        val i = texto.indexOf("</style>", pos, ignoreCase = true)
        return if (i < 0) -1 else i + "</style>".length
    }
}
