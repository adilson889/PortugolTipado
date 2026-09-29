package co.adilson889.typec.erros

/**
* Erro do PortugolTipado, com formato fixo e simples:
*
* Erro: linha 5
* inteiro x =
* Ficheiro: main.port
*
* 'contexto' é o texto da própria linha onde o erro ocorreu (sem indentação
* extra), para o Dev ver imediatamente onde está o problema sem sair do editor.
*/
open class ErroTypeC(
val mensagem: String,
val linha: Int,
val contexto: String,
val nomeFicheiro: String = "main.port"
) : Exception(mensagem) {

fun formatar(): String {
return "Erro: linha $linha\n$contexto\nFicheiro: $nomeFicheiro\n\n$mensagem"
}
}

/**
* Extrai a linha de texto correspondente a 'numeroLinha' (1-indexada)
* a partir do código-fonte completo. Usado para montar o 'contexto' do erro.
*/
fun extrairLinha(fonte: String, numeroLinha: Int): String {
val linhas = fonte.split("\n")
val indice = numeroLinha - 1
return if (indice in linhas.indices) linhas[indice].trim() else ""
}