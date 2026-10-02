package co.adilson889.typec.diagnostico

/**
 * Aviso ou erro detetado no codigo do editor.
 *
 * A gravidade decide o icone e a cor com que o editor marca a linha:
 *   AVISO -> triangulo amarelo
 *   ERRO  -> circulo vermelho
 *
 * O campo 'linha' e 1-indexado (mesma convencao das mensagens do Parser
 * e do Validador).
 */
data class Diagnostico(
    val linha: Int,
    val gravidade: Gravidade,
    val mensagem: String,
    /** Posicao da palavra marcada no texto; -1 = marca a linha inteira. */
    val inicio: Int = -1,
    val fim: Int = -1,
    /** Forma preferida, mostrada junto da mensagem (opcional). */
    val sugestao: String? = null
) {
    val ehDePalavra: Boolean get() = inicio >= 0 && fim > inicio
}

enum class Gravidade {
    AVISO,
    ERRO
}