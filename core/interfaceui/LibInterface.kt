package co.adilson889.typec.interfaceui

import co.adilson889.typec.ast.TipoDado

/**
 * Mapa de funcoes da biblioteca 'interface', no molde do LibGraficos.
 * leia(variavel, "id") e leia("id") nao estao aqui: sao nos da AST (LeiaCampo).
 */
object LibInterface {
    const val NOME_INCLUDE = "interface"

    class Funcao(val nome: String, val parametros: List<TipoDado>, val retorno: TipoDado)

    val funcoes: Map<String, Funcao> = listOf(
        Funcao("clique", listOf(TipoDado.TEXTO), TipoDado.LOGICO),
        Funcao("indice_clique", emptyList(), TipoDado.INTEIRO),
        Funcao("valor", listOf(TipoDado.TEXTO), TipoDado.TEXTO),
        Funcao("marcado", listOf(TipoDado.TEXTO), TipoDado.LOGICO),
        Funcao("defina_valor", listOf(TipoDado.TEXTO, TipoDado.TEXTO), TipoDado.VAZIO)
    ).associateBy { it.nome }

    fun ehFuncao(nome: String): Boolean = nome in funcoes

    fun chamar(nome: String, ui: Interface, args: List<Any>): Any? {
        fun texto(i: Int) = args.getOrNull(i)?.toString() ?: ""
        return when (nome) {
            "clique" -> {
                val alvo = texto(0)
                if (alvo.isBlank() || alvo == ".") {
                    throw ErroInterface("clique: o alvo não pode ser vazio (use um id ou \".classe\")")
                }
                ui.clique(alvo)
            }
            "indice_clique" -> ui.indiceClique().toLong()
            "valor" -> ui.valor(idNaoVazio(nome, texto(0)))
            "marcado" -> ui.marcado(idNaoVazio(nome, texto(0)))
            "defina_valor" -> { ui.definaValor(idNaoVazio(nome, texto(0)), texto(1)); null }
            else -> throw ErroInterface("função '$nome' não existe na biblioteca interface")
        }
    }

    private fun idNaoVazio(nome: String, id: String): String {
        if (id.isBlank()) throw ErroInterface("$nome: o id não pode ser vazio")
        return id
    }
}
