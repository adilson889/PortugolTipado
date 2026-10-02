package co.adilson889.typec.erros

/**
 * Erro do PortugolTipado, com formato fixo e simples:
 *
 * Erro: linha 5
 * inteiro x =
 *
 * 'contexto' e o texto da propria linha onde o erro ocorreu (sem indentacao
 * extra), para o Dev ver imediatamente onde esta o problema sem sair do editor.
 *
 * 'palavra' e 'sugestao' sao opcionais: usados quando o erro aponta para uma
 * palavra especifica (deprecacao, nome repetido, etc), para o editor poder
 * marcar a palavra exata em vez de so a linha.
 *
 * O nome do ficheiro nao aparece no texto do erro: o editor ja mostra o
 * ficheiro aberto, e o nome fixo ("main.port") nao dizia nada de util.
 */
open class ErroTypeC(
    val mensagem: String,
    val linha: Int,
    val contexto: String,
    val palavra: String? = null,
    val sugestao: String? = null
) : Exception(mensagem) {

    fun formatar(): String {
        val sb = StringBuilder()
        sb.append("Erro: linha $linha\n")
        sb.append("$contexto\n\n")
        sb.append(mensagem)
        if (sugestao != null) {
            sb.append("\n\nSugestão: $sugestao")
        }
        return sb.toString()
    }
}

/**
 * Extrai a linha de texto correspondente a 'numeroLinha' (1-indexada)
 * a partir do codigo-fonte completo. Usado para montar o 'contexto' do erro.
 */
fun extrairLinha(fonte: String, numeroLinha: Int): String {
    val linhas = fonte.split("\n")
    val indice = numeroLinha - 1
    return if (indice in linhas.indices) linhas[indice].trim() else ""
}