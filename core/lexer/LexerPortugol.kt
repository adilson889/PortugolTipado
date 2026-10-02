package co.adilson889.typec.lexer

import co.adilson889.typec.erros.ErroTypeC

/**
 * Aliado do Lexer para o Portugol classico (arquivos .por).
 *
 * Usa o Lexer normal e adapta a sequencia de tokens para a mesma forma que o
 * Parser ja entende, de modo que o resto do motor (Validador, Transpilador,
 * Interpretador) nao sabe de onde o programa veio:
 *
 *   programa { ... }      -> o envoltorio e removido
 *   cadeia                -> texto
 *   caracter              -> caractere
 *   e / ou / nao          -> && / || / !
 *   caso contrario        -> casocontrario
 *
 * Aceita o que o Portugol Studio gera; o que for do PortugolTipado continua valendo.
 */
class LexerPortugol(
    private val fonte: String,
    private val nomeArquivo: String = "main.por"
) {

    fun tokenizar(): List<Token> {
        val base = Lexer(fonte, nomeArquivo).tokenizar()
        return removerEnvoltorio(adaptarPalavras(base))
    }

    private fun adaptarPalavras(tokens: List<Token>): List<Token> {
        val saida = ArrayList<Token>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val seguinte = tokens.getOrNull(i + 1)
            if (t.tipo == TipoToken.CASO &&
                seguinte != null &&
                seguinte.tipo == TipoToken.IDENTIFICADOR &&
                seguinte.texto == "contrario"
            ) {
                saida.add(Token(TipoToken.PADRAO, "casocontrario", t.linha))
                i += 2
                continue
            }
            saida.add(if (t.tipo == TipoToken.IDENTIFICADOR) traduzir(t) else t)
            i++
        }
        return saida
    }

    private fun traduzir(t: Token): Token = when (t.texto) {
        "cadeia" -> Token(TipoToken.TEXTO_TIPO, "texto", t.linha)
        "caracter" -> Token(TipoToken.CARACTERE_TIPO, "caractere", t.linha)
        "e" -> Token(TipoToken.E_LOGICO, "&&", t.linha)
        "ou" -> Token(TipoToken.OU_LOGICO, "||", t.linha)
        "nao" -> Token(TipoToken.NEGACAO, "!", t.linha)
        else -> t
    }

    /** Remove 'programa' '{' no inicio e a '}' que o fecha (a ultima do arquivo). */
    private fun removerEnvoltorio(tokens: List<Token>): List<Token> {
        if (!comecaComPrograma(tokens)) return tokens

        var profundidade = 0
        var fechamento = -1
        for (i in 1 until tokens.size) {
            val tipo = tokens[i].tipo
            if (tipo == TipoToken.CHAVE_ESQ) {
                profundidade++
            } else if (tipo == TipoToken.CHAVE_DIR) {
                profundidade--
                if (profundidade == 0) {
                    fechamento = i
                    break
                }
            }
        }
        if (fechamento == -1) {
            throw ErroLexico("'programa' sem a '}' que o fecha", tokens[0].linha, fonte, nomeArquivo)
        }

        val saida = ArrayList<Token>(tokens.size)
        for (i in 2 until tokens.size) {
            if (i != fechamento) saida.add(tokens[i])
        }
        return saida
    }
}

private fun comecaComPrograma(tokens: List<Token>): Boolean =
    tokens.size >= 2 &&
        tokens[0].tipo == TipoToken.IDENTIFICADOR &&
        tokens[0].texto == "programa" &&
        tokens[1].tipo == TipoToken.CHAVE_ESQ

/** True se o codigo comeca com 'programa {', a marca do Portugol classico. */
fun ehPortugolClassico(fonte: String): Boolean {
    val tokens = try {
        Lexer(fonte).tokenizar()
    } catch (e: ErroTypeC) {
        return false
    }
    return comecaComPrograma(tokens)
}

/** Escolhe o Lexer certo: Portugol classico (comeca com 'programa {') ou PortugolTipado. */
fun tokenizarFonte(fonte: String, nomeArquivo: String = "main.port"): List<Token> =
    if (ehPortugolClassico(fonte)) LexerPortugol(fonte, nomeArquivo).tokenizar()
    else Lexer(fonte, nomeArquivo).tokenizar()
