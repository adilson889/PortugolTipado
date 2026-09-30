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

import co.adilson889.typec.ast.TipoDado

/**
* Tabela única da biblioteca gráfica (`inclua graficos`): nomes,
* assinaturas, constantes e execução sobre a interface [Graficos].
*
* É a fonte de verdade para:
* - Validador: [LibGraficos.existe], [LibGraficos.funcoes] (nomes,
*   número e tipos de parâmetros).
* - Transpilador: tipo de retorno ([FuncaoGrafica.retorno]) e nome na
*   runtime C ([FuncaoGrafica.nomeC], [ConstanteGrafica.nomeC]).
* - Interpretador: [LibGraficos.chamar].
*
* Os argumentos e o resultado de [LibGraficos.chamar] usam tipos
* simples do Kotlin, para esta biblioteca não depender do
* Interpretador:
*
* - inteiro  -> Long
* - real     -> Double
* - texto    -> String
* - logico   -> Boolean
* - vazio    -> null (resultado)
*/

/** Um parâmetro de função gráfica. */
data class ParametroGrafico(val nome: String, val tipo: TipoDado)

/** Uma função da biblioteca gráfica. */
class FuncaoGrafica(
    val nome: String,
    val parametros: List<ParametroGrafico>,
    val retorno: TipoDado,
    private val corpo: suspend (Graficos, List<Any>) -> Any?
) {
    /** Nome da função correspondente na runtime C (graficos.h). */
    val nomeC: String get() = "tc_$nome"

    suspend fun executar(g: Graficos, args: List<Any>): Any? {
        if (args.size != parametros.size) {
            throw ErroGrafico(
                "$nome espera ${parametros.size} argumento(s), mas recebeu ${args.size}"
            )
        }
        try {
            return corpo(g, args)
        } catch (e: ClassCastException) {
            throw ErroGrafico("$nome recebeu um argumento de tipo inválido")
        }
    }
}

/** Uma constante da biblioteca gráfica (ex.: TECLA_ESC). */
class ConstanteGrafica(val nome: String, val valor: Long) {
    /** Nome da macro correspondente na runtime C (graficos.h). */
    val nomeC: String get() = "TC_$nome"
}

object LibGraficos {

    /** Nome usado em `inclua graficos`. */
    const val NOME_INCLUDE = "graficos"

    val funcoes: Map<String, FuncaoGrafica> =
    criarFuncoes().associateBy { it.nome }

    val constantes: Map<String, ConstanteGrafica> =
    criarConstantes().associateBy { it.nome }

    fun ehFuncao(nome: String): Boolean = nome in funcoes

    fun ehConstante(nome: String): Boolean = nome in constantes

    /** Verdadeiro se [nome] é função ou constante desta biblioteca. */
    fun existe(nome: String): Boolean = ehFuncao(nome) || ehConstante(nome)

    /**
     * Executa a função [nome] sobre [g].
     * Lança [ErroGrafico] em caso de uso inválido.
     */
    suspend fun chamar(nome: String, g: Graficos, args: List<Any>): Any? {
        val funcao = funcoes[nome]
        ?: throw ErroGrafico("função gráfica desconhecida: $nome")
        return funcao.executar(g, args)
    }
}

// ---------------------------------------------------------------------
// Construção da tabela
// ---------------------------------------------------------------------

private fun inteiro(nome: String) = ParametroGrafico(nome, TipoDado.INTEIRO)
private fun real(nome: String) = ParametroGrafico(nome, TipoDado.REAL)
private fun texto(nome: String) = ParametroGrafico(nome, TipoDado.TEXTO)

private fun List<Any>.int(i: Int): Int = (this[i] as Number).toInt()
private fun List<Any>.lng(i: Int): Long = (this[i] as Number).toLong()
private fun List<Any>.dbl(i: Int): Double = (this[i] as Number).toDouble()
private fun List<Any>.txt(i: Int): String = this[i] as String

/** Canais de cor fora de 0 a 255 são limitados, sem erro. */
private fun canal(valor: Int): Int = valor.coerceIn(0, 255)

private fun exigirPositivo(valor: Int, funcao: String, campo: String) {
    if (valor <= 0) throw ErroGrafico("$funcao: $campo deve ser maior que zero")
}

/** Função sem retorno (vazio). */
private fun acao(
    nome: String,
    vararg parametros: ParametroGrafico,
    corpo: suspend (Graficos, List<Any>) -> Unit
): FuncaoGrafica =
FuncaoGrafica(nome, parametros.toList(), TipoDado.VAZIO) { g, a ->
    corpo(g, a)
    null
}

/** Função com retorno. */
private fun consulta(
    nome: String,
    retorno: TipoDado,
    vararg parametros: ParametroGrafico,
    corpo: suspend (Graficos, List<Any>) -> Any
): FuncaoGrafica =
FuncaoGrafica(nome, parametros.toList(), retorno) { g, a -> corpo(g, a) }

private fun criarFuncoes(): List<FuncaoGrafica> = listOf(

    // ---- Janela ----
    acao("abra_janela", texto("titulo"), inteiro("largura"), inteiro("altura")) { g, a ->
        exigirPositivo(a.int(1), "abra_janela", "a largura")
        exigirPositivo(a.int(2), "abra_janela", "a altura")
        g.abraJanela(a.txt(0), a.int(1), a.int(2))
    },
    acao("feche_janela") { g, _ -> g.fecheJanela() },
    acao("defina_titulo_janela", texto("titulo")) { g, a ->
        g.definaTituloJanela(a.txt(0))
    },
    consulta("largura_janela", TipoDado.INTEIRO) { g, _ -> g.larguraJanela().toLong() },
    consulta("altura_janela", TipoDado.INTEIRO) { g, _ -> g.alturaJanela().toLong() },
    consulta("largura_tela", TipoDado.INTEIRO) { g, _ -> g.larguraTela().toLong() },
    consulta("altura_tela", TipoDado.INTEIRO) { g, _ -> g.alturaTela().toLong() },
    consulta("janela_aberta", TipoDado.LOGICO) { g, _ -> g.janelaAberta() },

    // ---- Desenho ----
    acao("defina_cor", inteiro("r"), inteiro("g"), inteiro("b")) { g, a ->
        g.definaCor(canal(a.int(0)), canal(a.int(1)), canal(a.int(2)))
    },
    acao("limpe") { g, _ -> g.limpe() },
    acao("desenhe_ponto", inteiro("x"), inteiro("y")) { g, a ->
        g.desenhePonto(a.int(0), a.int(1))
    },
    acao(
        "desenhe_linha",
        inteiro("x1"), inteiro("y1"), inteiro("x2"), inteiro("y2")
    ) { g, a ->
        g.desenheLinha(a.int(0), a.int(1), a.int(2), a.int(3))
    },
    acao(
        "desenhe_retangulo",
        inteiro("x"), inteiro("y"), inteiro("largura"), inteiro("altura")
    ) { g, a ->
        g.desenheRetangulo(a.int(0), a.int(1), a.int(2), a.int(3))
    },
    acao(
        "preencha_retangulo",
        inteiro("x"), inteiro("y"), inteiro("largura"), inteiro("altura")
    ) { g, a ->
        g.preenchaRetangulo(a.int(0), a.int(1), a.int(2), a.int(3))
    },
    acao(
        "desenhe_elipse",
        inteiro("x"), inteiro("y"), inteiro("largura"), inteiro("altura")
    ) { g, a ->
        g.desenheElipse(a.int(0), a.int(1), a.int(2), a.int(3))
    },
    acao(
        "preencha_elipse",
        inteiro("x"), inteiro("y"), inteiro("largura"), inteiro("altura")
    ) { g, a ->
        g.preenchaElipse(a.int(0), a.int(1), a.int(2), a.int(3))
    },
    acao("defina_tamanho_texto", real("tamanho")) { g, a ->
        if (a.dbl(0) <= 0.0) {
            throw ErroGrafico("defina_tamanho_texto: o tamanho deve ser maior que zero")
        }
        g.definaTamanhoTexto(a.dbl(0))
    },
    acao("desenhe_texto", inteiro("x"), inteiro("y"), texto("conteudo")) { g, a ->
        g.desenheTexto(a.int(0), a.int(1), a.txt(2))
    },
    consulta("largura_texto", TipoDado.INTEIRO, texto("conteudo")) { g, a ->
        g.larguraTexto(a.txt(0)).toLong()
    },
    consulta("altura_texto", TipoDado.INTEIRO, texto("conteudo")) { g, a ->
        g.alturaTexto(a.txt(0)).toLong()
    },
    acao("renderize") { g, _ -> g.renderize() },

    // ---- Teclado e mouse ----
    consulta("tecla_pressionada", TipoDado.LOGICO, inteiro("tecla")) { g, a ->
        g.teclaPressionada(a.int(0))
    },
    consulta("mouse_x", TipoDado.INTEIRO) { g, _ -> g.mouseX().toLong() },
    consulta("mouse_y", TipoDado.INTEIRO) { g, _ -> g.mouseY().toLong() },
    consulta("botao_mouse_pressionado", TipoDado.LOGICO, inteiro("botao")) { g, a ->
        g.botaoMousePressionado(a.int(0))
    },
    acao("oculte_cursor") { g, _ -> g.oculteCursor() },
    acao("exiba_cursor") { g, _ -> g.exibaCursor() },

    // ---- Tempo ----
    consulta("tempo_decorrido", TipoDado.INTEIRO) { g, _ -> g.tempoDecorrido() },
    acao("aguarde", inteiro("ms")) { g, a -> g.aguarde(a.lng(0)) }
)

private fun criarConstantes(): List<ConstanteGrafica> {
    val lista = mutableListOf(
        ConstanteGrafica("TECLA_ENTER", Tecla.ENTER.toLong()),
        ConstanteGrafica("TECLA_ESC", Tecla.ESC.toLong()),
        ConstanteGrafica("TECLA_ESPACO", Tecla.ESPACO.toLong()),
        ConstanteGrafica("TECLA_SETA_ESQUERDA", Tecla.SETA_ESQUERDA.toLong()),
        ConstanteGrafica("TECLA_SETA_ACIMA", Tecla.SETA_ACIMA.toLong()),
        ConstanteGrafica("TECLA_SETA_DIREITA", Tecla.SETA_DIREITA.toLong()),
        ConstanteGrafica("TECLA_SETA_ABAIXO", Tecla.SETA_ABAIXO.toLong())
    )
    // Letras e dígitos coincidem com o ASCII (A = 65 ... Z = 90, 0 = 48 ... 9 = 57)
    for (c in 'A' .. 'Z') lista.add(ConstanteGrafica("TECLA_$c", c.code.toLong()))
    for (c in '0' .. '9') lista.add(ConstanteGrafica("TECLA_$c", c.code.toLong()))
    return lista
}
