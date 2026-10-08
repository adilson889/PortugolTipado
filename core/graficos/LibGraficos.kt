/*
* PortugolTipado - Superconjunto de Portugol que compila para C
* Copyright (C) 2026  Adilson C. Rafael
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
* A ideia da biblioteca gráfica e o seu conjunto de operações vêm da
* biblioteca Graficos do Portugol Studio (GPLv3).
*/
package co.adilson889.typec.graficos

/**
 * Erro de uso da biblioteca gráfica (ex.: desenhar antes de abrir a
 * janela). A mensagem vai em português. Quem chama (o Interpretador)
 * converte para o erro do programa, acrescentando a linha.
 */
class ErroGrafico(mensagem: String) : Exception(mensagem)

/**
 * Códigos de tecla do contrato. Seguem a convenção de códigos do
 * JavaScript, para letras e dígitos coincidirem com o ASCII:
 * A a Z = 65 a 90 e 0 a 9 = 48 a 57.
 *
 * Cada host converte as teclas da sua plataforma para estes códigos.
 * A runtime C converte os códigos do SDL para estes mesmos valores.
 */
object Tecla {
    const val ENTER = 13
    const val ESC = 27
    const val ESPACO = 32
    const val SETA_ESQUERDA = 37
    const val SETA_ACIMA = 38
    const val SETA_DIREITA = 39
    const val SETA_ABAIXO = 40
}

/** Botões do mouse do contrato. */
object BotaoMouse {
    const val ESQUERDO = 1
    const val MEIO = 2
    const val DIREITO = 3
}

/**
 * Contrato de UI do motor. O motor não desenha nada: cada host
 * (Android, desktop, web...) implementa esta interface, e o
 * Interpretador chama-a. O executável em C tem o equivalente na
 * runtime SDL2 (graficos.h/.c), com o mesmo comportamento.
 *
 * Regras comuns a todas as implementações:
 *
 * - Origem (0, 0) no canto superior esquerdo; x cresce para a direita
 *   e y para baixo. Tudo em pixels inteiros.
 * - Os canais de cor chegam já entre 0 e 255 (o limite é aplicado
 *   antes de chamar a interface).
 * - Existe uma única janela implícita por programa.
 * - Desenhar, consultar teclado/mouse ou chamar [renderize] antes de
 *   [abraJanela] deve lançar [ErroGrafico] com mensagem clara.
 * - Texto: (x, y) é o canto superior esquerdo da caixa de texto, não a
 *   linha de base. Cada implementação faz a conversão.
 * - O tamanho de janela pedido é limitado ao ecrã disponível.
 * - Nada garante que o desenho apareça antes de [renderize]; os
 *   programas não devem depender disso.
 *
 * Um host pode observar as chamadas a [teclaPressionada] para, por
 * exemplo, mostrar botões virtuais só para as teclas que o programa
 * consulta.
 */
interface Graficos {

    // ------------------------------------------------------------
    // Janela
    // ------------------------------------------------------------

    fun abraJanela(titulo: String, largura: Int, altura: Int)
    fun fecheJanela()
    fun definaTituloJanela(titulo: String)
    fun larguraJanela(): Int
    fun alturaJanela(): Int

    /** Dimensões do ecrã disponível para a janela. */
    fun larguraTela(): Int
    fun alturaTela(): Int

    /** Falso depois de o utilizador fechar a janela. */
    fun janelaAberta(): Boolean

    // ------------------------------------------------------------
    // Desenho
    // ------------------------------------------------------------

    /** Define a cor usada por limpe e por todos os desenhos seguintes. */
    fun definaCor(r: Int, g: Int, b: Int)

    /** Preenche toda a janela com a cor atual. */
    fun limpe()

    fun desenhePonto(x: Int, y: Int)
    fun desenheLinha(x1: Int, y1: Int, x2: Int, y2: Int)

    /** Contorno. */
    fun desenheRetangulo(x: Int, y: Int, largura: Int, altura: Int)
    fun preenchaRetangulo(x: Int, y: Int, largura: Int, altura: Int)

    /** A elipse cabe no retângulo (x, y, largura, altura). */
    fun desenheElipse(x: Int, y: Int, largura: Int, altura: Int)
    fun preenchaElipse(x: Int, y: Int, largura: Int, altura: Int)

    fun definaTamanhoTexto(tamanho: Double)
    fun desenheTexto(x: Int, y: Int, conteudo: String)
    fun larguraTexto(conteudo: String): Int
    fun alturaTexto(conteudo: String): Int

    /**
     * Fecha o quadro atual. A implementação atualiza o estado de
     * teclado e mouse e devolve o controlo ao sistema (por isso é
     * suspend), para a UI poder redesenhar e o programa não a bloquear.
     */
    suspend fun renderize()

    // ------------------------------------------------------------
    // Teclado e mouse (consulta a cada quadro)
    // ------------------------------------------------------------

    /** [tecla] é um dos códigos de [Tecla] (ou letra/dígito ASCII). */
    fun teclaPressionada(tecla: Int): Boolean

    fun mouseX(): Int
    fun mouseY(): Int

    /** [botao] é um dos valores de [BotaoMouse]. */
    fun botaoMousePressionado(botao: Int): Boolean

    fun oculteCursor()
    fun exibaCursor()

    // ------------------------------------------------------------
    // Tempo
    // ------------------------------------------------------------

    /** Milissegundos desde o início do programa. */
    fun tempoDecorrido(): Long

    /** Espera [ms] milissegundos sem bloquear a UI. */
    suspend fun aguarde(ms: Long)
}
