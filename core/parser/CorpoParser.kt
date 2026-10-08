package co.adilson889.typec.parser

import co.adilson889.typec.ast.ComandoSe
import co.adilson889.typec.ast.No
import co.adilson889.typec.ast.ParteCiclo
import co.adilson889.typec.ast.ParteCorpo
import co.adilson889.typec.ast.ParteSe
import co.adilson889.typec.ast.ParteTexto
import co.adilson889.typec.ast.ParteValor
import co.adilson889.typec.lexer.Lexer
import co.adilson889.typec.lexer.RegrasCorpo

/**
 * Divide o corpo de um 'componente' (texto cru do Lexer) em partes:
 * texto, {valor}, se/senao e ciclos (para, para cada, enquanto).
 * Os cabeçalhos e as expressões são analisados pelo próprio Parser da linguagem.
 */
class CorpoParser(
    private val corpo: String,
    linhaInicial: Int,
    private val fonte: String,
    private val nomeArquivo: String
) {
    private var i = 0
    private var linha = linhaInicial

    fun parsear(): List<ParteCorpo> = lerPartes(aninhado = false)

    private fun erro(mensagem: String, onde: Int = linha) = ErroSintatico(mensagem, onde, fonte, nomeArquivo)

    private fun lerPartes(aninhado: Boolean): List<ParteCorpo> {
        val partes = mutableListOf<ParteCorpo>()
        val texto = StringBuilder()
        var inicioSegmento = 0 // onde começa, em [texto], o que vem desde o início da linha

        fun fecharTexto() {
            if (texto.isNotEmpty()) partes.add(ParteTexto(texto.toString()))
            texto.setLength(0)
            inicioSegmento = 0
        }

        while (i < corpo.length) {
            val c = corpo[i]
            when {
                c == '\\' && i + 1 < corpo.length && (corpo[i + 1] == '{' || corpo[i + 1] == '}') -> {
                    texto.append(corpo[i + 1])
                    i += 2
                }
                c == '<' && RegrasCorpo.comecaTag(corpo, i, "<style") -> {
                    val fim = RegrasCorpo.fimDoStyle(corpo, i)
                    if (fim < 0) throw erro("componente: falta fechar </style> (aberto na linha $linha)")
                    val trecho = corpo.substring(i, fim)
                    texto.append(trecho)
                    linha += trecho.count { it == '\n' }
                    i = fim
                }
                c == '{' -> {
                    val segmento = texto.substring(inicioSegmento)
                    if (RegrasCorpo.CONTROLE.matches(segmento)) {
                        texto.setLength(inicioSegmento)
                        fecharTexto()
                        i++ // o '{' do bloco
                        partes.add(lerControlo(segmento.trim()))
                    } else {
                        fecharTexto()
                        partes.add(lerValor())
                    }
                }
                c == '}' -> {
                    if (!aninhado) {
                        throw erro("'}' a mais no corpo do componente (para escrever uma chaveta usa \\})")
                    }
                    fecharTexto()
                    i++ // o '}' que fecha o bloco
                    return partes
                }
                c == '\n' -> {
                    texto.append(c)
                    linha++
                    i++
                    inicioSegmento = texto.length
                }
                else -> {
                    texto.append(c)
                    i++
                }
            }
        }
        if (aninhado) throw erro("componente: falta fechar um bloco com '}'")
        fecharTexto()
        return partes
    }

    /** O '{' do bloco já foi consumido; [cabecalho] é o texto antes dele (se (...), para (...), ...). */
    private fun lerControlo(cabecalho: String): ParteCorpo {
        val linhaCab = linha
        if (cabecalho.startsWith("senao")) throw erro("'senao' sem um 'se' antes dele", linhaCab)
        val cmd = analisarCabecalho(cabecalho, linhaCab)
        val bloco = lerPartes(aninhado = true)
        if (cmd is ComandoSe) {
            return ParteSe(cmd.condicao, bloco, lerSenao(), linhaCab)
        }
        return ParteCiclo(cmd, bloco, linhaCab)
    }

    /** Depois do '}' de um 'se': se vier 'senao' na mesma linha, lê o bloco (ou o 'senao se' seguinte). */
    private fun lerSenao(): List<ParteCorpo>? {
        var j = i
        while (j < corpo.length && (corpo[j] == ' ' || corpo[j] == '\t')) j++
        if (!corpo.startsWith("senao", j)) return null
        val depois = j + 5
        if (depois < corpo.length && (corpo[depois].isLetterOrDigit() || corpo[depois] == '_')) return null

        val fimLinha = corpo.indexOf('\n', depois).let { if (it < 0) corpo.length else it }
        val chave = corpo.indexOf('{', depois)
        if (chave < 0 || chave > fimLinha) throw erro("esperado '{' depois de 'senao'")
        val entre = corpo.substring(depois, chave).trim()
        i = chave + 1
        if (entre.isEmpty()) return lerPartes(aninhado = true)

        if (!RegrasCorpo.SE_ISOLADO.matches(entre)) {
            throw erro("depois de 'senao' só pode vir '{' ou 'se (...) {'")
        }
        val linhaCab = linha
        val cmd = analisarCabecalho(entre, linhaCab) as ComandoSe
        val entao = lerPartes(aninhado = true)
        return listOf(ParteSe(cmd.condicao, entao, lerSenao(), linhaCab))
    }

    /** {expressao}: tem de fechar na mesma linha; o texto entre aspas pode ter '}' lá dentro. */
    private fun lerValor(): ParteValor {
        val linhaValor = linha
        val inicio = i + 1
        var j = inicio
        while (j < corpo.length && corpo[j] != '}') {
            val ch = corpo[j]
            if (ch == '\n') throw erro("valor sem fechar: falta o '}' na mesma linha", linhaValor)
            if (ch == '"' || ch == '\'') {
                j++
                while (j < corpo.length && corpo[j] != ch && corpo[j] != '\n') {
                    if (corpo[j] == '\\') j++
                    j++
                }
                if (j < corpo.length && corpo[j] == ch) j++
                continue
            }
            j++
        }
        if (j >= corpo.length) throw erro("valor sem fechar: falta o '}'", linhaValor)
        val expr = corpo.substring(inicio, j).trim()
        if (expr.isEmpty()) throw erro("valor vazio: escreve uma expressão entre as chavetas", linhaValor)
        i = j + 1
        return ParteValor(analisarExpressao(expr, linhaValor), linhaValor)
    }

    /** O fonte de cada análise leva linhas vazias à frente, para os erros apontarem a linha certa do ficheiro. */
    private fun comLinhas(texto: String, linhaReal: Int) = "\n".repeat(maxOf(linhaReal - 1, 0)) + texto

    private fun analisarExpressao(texto: String, linhaReal: Int): No {
        val src = comLinhas(texto, linhaReal)
        return Parser(Lexer(src, nomeArquivo).tokenizar(), src, nomeArquivo).parsearExpressaoIsolada()
    }

    private fun analisarCabecalho(cabecalho: String, linhaReal: Int): No {
        val src = comLinhas("$cabecalho { }", linhaReal)
        return Parser(Lexer(src, nomeArquivo).tokenizar(), src, nomeArquivo).parsearComandoIsolado()
    }
}
