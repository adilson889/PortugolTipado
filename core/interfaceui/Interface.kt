package co.adilson889.typec.interfaceui

class ErroInterface(mensagem: String) : Exception(mensagem)

/**
 * Contrato da biblioteca 'interface' (componentes HTML/CSS sobre a janela grafica).
 * Kotlin puro: o host (Android) implementa, como acontece com o contrato Graficos.
 */
interface Interface {
    /** Mostra/atualiza o componente de posicao [indice] neste quadro. */
    fun componente(indice: Int, x: Int, y: Int, largura: Int, altura: Int, html: String)

    /** Chamado depois de renderize(): remove componentes nao chamados e avanca a fila de cliques. */
    fun fimQuadro(total: Int)

    fun clique(alvo: String): Boolean
    fun indiceClique(): Int

    /** Texto atual do campo, ou null se o id ainda nao existe. */
    fun leiaCampo(id: String): String?
    fun valor(id: String): String
    fun marcado(id: String): Boolean
    fun definaValor(id: String, texto: String)

    /** Liberta as WebViews (a janela fechou). */
    fun liberar()
}
