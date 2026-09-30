package co.adilson889.typec.lexer

import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.erros.extrairLinha

enum class TipoToken {
    // Literais
    NUMERO_INTEIRO, NUMERO_DECIMAL, TEXTO, CARACTERE, IDENTIFICADOR,

    // Palavras-chave de tipo
    INTEIRO, REAL, DUPLO, TEXTO_TIPO, CARACTERE_TIPO, LOGICO, VAZIO, TEMPO,

    // Modificadores de tipo (combinam com INTEIRO/DUPLO: "inteiro longo", "duplo longo", "inteiro longo longo")
    LONGO, CURTO, POSITIVO,

    // Palavras-chave estruturais
    INICIO, INCLUA, FUNCAO, RETORNA, ALTERAR, ESTRUTURA, CONST,

    // Palavras-chave de controle
    SE, SENAO, ENQUANTO, PARA, CADA, EM, DISPENSAR, IGNORAR,
    ESCOLHER, CASO, PADRAO,

    // Palavras-chave de I/O
    IMPRIMIR, LER,

    // Literais lógicos
    VERDADEIRO, FALSO,

    // Símbolos
    CHAVE_ESQ, CHAVE_DIR, // { }
    PARENTESE_ESQ, PARENTESE_DIR, // ( )
    COLCHETE_ESQ, COLCHETE_DIR, // [ ]
    PONTO, VIRGULA, PONTO_E_VIRGULA, DOIS_PONTOS,

    // Operadores
    MAIS, MENOS, VEZES, DIVIDIR, RESTO,
    IGUAL, MAIS_IGUAL, MENOS_IGUAL, VEZES_IGUAL, DIVIDIR_IGUAL, RESTO_IGUAL,
    INCREMENTO, DECREMENTO,
    IGUAL_IGUAL, DIFERENTE, MAIOR, MENOR, MAIOR_IGUAL, MENOR_IGUAL,
    E_LOGICO, OU_LOGICO, NEGACAO,

    // Especiais
    FIM_ARQUIVO
}

data class Token(
    val tipo: TipoToken,
    val texto: String,
    val linha: Int
)

class ErroLexico(mensagem: String, linha: Int, fonte: String, nomeArquivo: String = "main.port") :
ErroTypeC(mensagem, linha, extrairLinha(fonte, linha), nomeArquivo)

class Lexer(
    private val fonte: String,
    private val nomeArquivo: String = "main.port" // usado nas mensagens de erro (ver ErroTypeC) — importante em módulos, onde o arquivo com erro nem sempre é o principal
) {

    private var posicao = 0
    private var linha = 1
    private val tokens = mutableListOf<Token>()

    companion object {
        val PALAVRAS_CHAVE = mapOf(
            "inteiro" to TipoToken.INTEIRO,
            "real" to TipoToken.REAL,
            "duplo" to TipoToken.DUPLO,
            "texto" to TipoToken.TEXTO_TIPO,
            "caractere" to TipoToken.CARACTERE_TIPO,
            "logico" to TipoToken.LOGICO,
            "vazio" to TipoToken.VAZIO,
            "tempo" to TipoToken.TEMPO,

            // modificadores de tipo — combinam com o tipo anterior (ver Parser)
            "longo" to TipoToken.LONGO,
            "curto" to TipoToken.CURTO,
            "positivo" to TipoToken.POSITIVO,

            "inicio" to TipoToken.INICIO,
            "inclua" to TipoToken.INCLUA,
            "funcao" to TipoToken.FUNCAO,
            "retorne" to TipoToken.RETORNA,
            "altere" to TipoToken.ALTERAR, // ← imperativo (forma nova)
            "altera" to TipoToken.ALTERAR, // ← alias antigo (deprecado)
            "alterar" to TipoToken.ALTERAR,
            "estrutura" to TipoToken.ESTRUTURA,
            "const" to TipoToken.CONST,

            "se" to TipoToken.SE,
            "senao" to TipoToken.SENAO,
            "enquanto" to TipoToken.ENQUANTO,
            "para" to TipoToken.PARA,
            "cada" to TipoToken.CADA,
            "em" to TipoToken.EM,
            "pare" to TipoToken.DISPENSAR,
            "continue" to TipoToken.IGNORAR,
            "escolha" to TipoToken.ESCOLHER,
            "caso" to TipoToken.CASO,
            "casocontrario" to TipoToken.PADRAO,

            "escreva" to TipoToken.IMPRIMIR,
            "leia" to TipoToken.LER,

            "verdadeiro" to TipoToken.VERDADEIRO,
            "V" to TipoToken.VERDADEIRO,
            "falso" to TipoToken.FALSO,
            "F" to TipoToken.FALSO

        )
    }

    fun tokenizar(): List<Token> {
        while (!fimDoArquivo()) {
            pularEspacosEComentarios()
            if (fimDoArquivo()) break

            val c = caractereAtual()

            when {
                c.isDigit() -> lerNumero()
                c.isLetter() || c == '_' -> lerIdentificadorOuPalavraChave()
                c == '"' -> lerTexto()
                c == '\'' -> lerCaractere()
                else -> lerSimbolo()
            }
        }
        tokens.add(Token(TipoToken.FIM_ARQUIVO, "", linha))
        return tokens
    }

    private fun fimDoArquivo(): Boolean = posicao >= fonte.length

    private fun caractereAtual(): Char = fonte[posicao]

    private fun caractereSeguinte(): Char =
    if (posicao + 1 < fonte.length) fonte[posicao + 1] else '\u0000'

    private fun avancar(): Char {
        val c = fonte[posicao]
        posicao++
        if (c == '\n') linha++
        return c
    }

    private fun pularEspacosEComentarios() {
        while (!fimDoArquivo()) {
            val c = caractereAtual()
            when {
                c == ' ' || c == '\t' || c == '\r' || c == '\n' -> avancar()
                c == '/' && caractereSeguinte() == '/' -> {
                    // comentário de uma linha
                    while (!fimDoArquivo() && caractereAtual() != '\n') avancar()
                }
                c == '/' && caractereSeguinte() == '*' -> {
                    // comentário de várias linhas
                    avancar(); avancar()
                    while (!fimDoArquivo() && !(caractereAtual() == '*' && caractereSeguinte() == '/')) {
                        avancar()
                    }
                    if (!fimDoArquivo()) { avancar(); avancar() }
                }
                else -> return
            }
        }
    }

    private fun lerNumero() {
        val linhaInicio = linha
        val inicio = posicao
        var ehDecimal = false

        while (!fimDoArquivo() && caractereAtual().isDigit()) avancar()

        if (!fimDoArquivo() && caractereAtual() == '.' && caractereSeguinte().isDigit()) {
            ehDecimal = true
            avancar() // consome o ponto
            while (!fimDoArquivo() && caractereAtual().isDigit()) avancar()
        }

        val texto = fonte.substring(inicio, posicao)
        tokens.add(Token(
                if (ehDecimal) TipoToken.NUMERO_DECIMAL else TipoToken.NUMERO_INTEIRO,
                texto,
                linhaInicio
        ))
    }

    private fun lerIdentificadorOuPalavraChave() {
        val linhaInicio = linha
        val inicio = posicao
        while (!fimDoArquivo() && (caractereAtual().isLetterOrDigit() || caractereAtual() == '_')) {
            avancar()
        }
        val texto = fonte.substring(inicio, posicao)
        val tipo = PALAVRAS_CHAVE[texto] ?: TipoToken.IDENTIFICADOR
        tokens.add(Token(tipo, texto, linhaInicio))
    }

    private fun lerTexto() {
        val linhaInicio = linha
        avancar() // consome a "
        val sb = StringBuilder()
        while (!fimDoArquivo() && caractereAtual() != '"') {
            if (caractereAtual() == '\\' && !fimDoArquivo()) {
                sb.append(avancar()) // mantém a barra
                if (!fimDoArquivo()) sb.append(avancar()) // mantém o caractere escapado (n, t, etc.)
            } else {
                sb.append(avancar())
            }
        }
        if (fimDoArquivo()) {
            throw ErroLexico("string não fechada, faltou o \" no final", linhaInicio, fonte, nomeArquivo)
        }
        avancar() // consome a " final
        tokens.add(Token(TipoToken.TEXTO, sb.toString(), linhaInicio))
    }

    private fun lerCaractere() {
        val linhaInicio = linha
        avancar() // consome a '
        if (fimDoArquivo()) {
            throw ErroLexico("caractere não fechado, faltou o ' no final", linhaInicio, fonte, nomeArquivo)
        }
        var valor = avancar()
        if (valor == '\\' && !fimDoArquivo()) {
            valor = avancar() // caractere de escape (n, t, etc.) — simplificado
        }
        if (fimDoArquivo() || caractereAtual() != '\'') {
            throw ErroLexico("caractere deve ter exatamente um símbolo entre aspas simples", linhaInicio, fonte, nomeArquivo)
        }
        avancar() // consome a ' final
        tokens.add(Token(TipoToken.CARACTERE, valor.toString(), linhaInicio))
    }

    private fun lerSimbolo() {
        val linhaInicio = linha
        val c = avancar()

        fun proximoEh(esperado: Char): Boolean =
        !fimDoArquivo() && caractereAtual() == esperado

        val (tipo, texto) = when (c) {
            '{' -> TipoToken.CHAVE_ESQ to "{"
            '}' -> TipoToken.CHAVE_DIR to "}"
            '(' -> TipoToken.PARENTESE_ESQ to "("
            ')' -> TipoToken.PARENTESE_DIR to ")"
            '[' -> TipoToken.COLCHETE_ESQ to "["
            ']' -> TipoToken.COLCHETE_DIR to "]"
            '.' -> TipoToken.PONTO to "."
            ',' -> TipoToken.VIRGULA to ","
            ';' -> TipoToken.PONTO_E_VIRGULA to ";"
            ':' -> TipoToken.DOIS_PONTOS to ":"

            '+' -> when {
                proximoEh('+') -> { avancar(); TipoToken.INCREMENTO to "++" }
                proximoEh('=') -> { avancar(); TipoToken.MAIS_IGUAL to "+=" }
                else -> TipoToken.MAIS to "+"
            }
            '-' -> when {
                proximoEh('-') -> { avancar(); TipoToken.DECREMENTO to "--" }
                proximoEh('=') -> { avancar(); TipoToken.MENOS_IGUAL to "-=" }
                else -> TipoToken.MENOS to "-"
            }
            '*' -> when {
                proximoEh('=') -> { avancar(); TipoToken.VEZES_IGUAL to "*=" }
                else -> TipoToken.VEZES to "*"
            }
            '/' -> when {
                proximoEh('=') -> { avancar(); TipoToken.DIVIDIR_IGUAL to "/=" }
                else -> TipoToken.DIVIDIR to "/"
            }
            '%' -> when {
                proximoEh('=') -> { avancar(); TipoToken.RESTO_IGUAL to "%=" }
                else -> TipoToken.RESTO to "%"
            }
            '=' -> when {
                proximoEh('=') -> { avancar(); TipoToken.IGUAL_IGUAL to "==" }
                else -> TipoToken.IGUAL to "="
            }
            '!' -> when {
                proximoEh('=') -> { avancar(); TipoToken.DIFERENTE to "!=" }
                else -> TipoToken.NEGACAO to "!"
            }
            '>' -> when {
                proximoEh('=') -> { avancar(); TipoToken.MAIOR_IGUAL to ">=" }
                else -> TipoToken.MAIOR to ">"
            }
            '<' -> when {
                proximoEh('=') -> { avancar(); TipoToken.MENOR_IGUAL to "<=" }
                else -> TipoToken.MENOR to "<"
            }
            '&' -> when {
                proximoEh('&') -> { avancar(); TipoToken.E_LOGICO to "&&" }
                else -> throw ErroLexico("símbolo '&' sozinho não é permitido no TypeC (sem ponteiros)", linhaInicio, fonte, nomeArquivo)
            }
            '|' -> when {
                proximoEh('|') -> { avancar(); TipoToken.OU_LOGICO to "||" }
                else -> throw ErroLexico("símbolo '|' sozinho não é reconhecido", linhaInicio, fonte, nomeArquivo)
            }
            else -> throw ErroLexico("símbolo desconhecido: '$c'", linhaInicio, fonte, nomeArquivo)
        }

        tokens.add(Token(tipo, texto, linhaInicio))
    }
}
