package co.adilson889.typec.validador

import co.adilson889.typec.ast.*
import java.math.BigInteger

/**
 * Verificacao adicional, opcional, das conversoes escalares em PortugolTipado.
 * Nao altera a AST nem desabilita o validador existente. Casos ainda sem
 * informacoes de tipos (ex.: algumas bibliotecas C) ficam com o backend.
 *
 * Projeto derivado de PortugolTipado, GPL-3.0-or-later.
 */
class ChecagemSeguraV2(private val fonte: String) {
    private val escopos = mutableListOf<MutableMap<String, Tipo>>()
    private val funcoes = mutableMapOf<String, DeclaracaoFuncao>()
    private val estruturas = mutableMapOf<String, DeclaracaoStruct>()
    private var emInicio = false
    private var retorno: Tipo? = null

    private fun erro(n: No, mensagem: String): Nothing =
        throw ErroValidacao("tipagem segura: " + mensagem, n.linha, fonte)

    private fun entrar() { escopos.add(mutableMapOf()) }
    private fun sair() { escopos.removeAt(escopos.lastIndex) }
    private fun salvar(nome: String, tipo: Tipo) { escopos.last()[nome] = tipo }
    private fun procurar(nome: String): Tipo? {
        for (i in escopos.indices.reversed()) {
            escopos[i][nome]?.let { return it }
        }
        return null
    }

    private fun escalar(t: Tipo) = !t.ehArray && !t.ehArray2D && t.nomeStruct == null
    private val inteiros = setOf(TipoDado.INTEIRO_CURTO, TipoDado.INTEIRO,
        TipoDado.INTEIRO_POSITIVO, TipoDado.INTEIRO_LONGO, TipoDado.INTEIRO_GIGANTE)
    private val flutuantes = setOf(TipoDado.REAL, TipoDado.DUPLO, TipoDado.DUPLO_LONGO)
    private fun numerico(t: Tipo) = escalar(t) && (t.base in inteiros || t.base in flutuantes)
    private fun ordemInteiro(t: TipoDado): Int = when(t) {
        TipoDado.INTEIRO_CURTO -> 0
        TipoDado.INTEIRO, TipoDado.INTEIRO_POSITIVO -> 1
        TipoDado.INTEIRO_LONGO -> 2
        TipoDado.INTEIRO_GIGANTE -> 3
        else -> -1
    }
    private fun ordemReal(t: TipoDado): Int = when(t) {
        TipoDado.REAL -> 1
        TipoDado.DUPLO -> 2
        TipoDado.DUPLO_LONGO -> 3
        else -> -1
    }

    private fun valorInteiro(n: No): BigInteger? = when (n) {
        is Numero -> if (n.tipo == TipoDado.INTEIRO) n.valor.toBigIntegerOrNull() else null
        is OperacaoUnaria -> if (n.operador == "-") valorInteiro(n.operando)?.negate() else null
        else -> null
    }

    // Limites minimos garantidos por C; 'long' nao e necessariamente 64 bits.
    private fun literalCabe(destino: Tipo, expr: No): Boolean {
        if (!escalar(destino)) return false
        val v = valorInteiro(expr) ?: return false
        val max = when (destino.base) {
            TipoDado.INTEIRO_CURTO -> BigInteger.valueOf(32767)
            TipoDado.INTEIRO, TipoDado.INTEIRO_LONGO -> BigInteger.valueOf(Int.MAX_VALUE.toLong())
            TipoDado.INTEIRO_POSITIVO -> BigInteger("4294967295")
            TipoDado.INTEIRO_GIGANTE -> BigInteger.valueOf(Long.MAX_VALUE)
            TipoDado.REAL -> BigInteger.valueOf(16777216)
            TipoDado.DUPLO, TipoDado.DUPLO_LONGO -> BigInteger.valueOf(9007199254740992L)
            else -> return false
        }
        val min = when (destino.base) {
            TipoDado.INTEIRO_CURTO -> BigInteger.valueOf(-32768)
            TipoDado.INTEIRO, TipoDado.INTEIRO_LONGO ->
                BigInteger.valueOf(Int.MIN_VALUE.toLong())
            TipoDado.INTEIRO_POSITIVO -> BigInteger.ZERO
            TipoDado.INTEIRO_GIGANTE -> BigInteger.valueOf(Long.MIN_VALUE)
            else -> max.negate()
        }
        return v >= min && v <= max
    }

    private fun conversaoSegura(dest: Tipo, origem: Tipo): Boolean {
        if (dest == origem) return true
        if (!escalar(dest) || !escalar(origem) || !numerico(dest) || !numerico(origem))
            return false
        val d = dest.base
        val o = origem.base
        if (d in inteiros && o in inteiros) {
            if (d == TipoDado.INTEIRO_POSITIVO) return false
            if (o == TipoDado.INTEIRO_POSITIVO) return d == TipoDado.INTEIRO_GIGANTE
            return ordemInteiro(d) >= ordemInteiro(o)
        }
        if (d in flutuantes && o in flutuantes)
            return ordemReal(d) >= ordemReal(o)
        if (d in flutuantes && o in inteiros) {
            return when(o) {
                TipoDado.INTEIRO_CURTO -> true
                TipoDado.INTEIRO, TipoDado.INTEIRO_POSITIVO ->
                    d == TipoDado.DUPLO || d == TipoDado.DUPLO_LONGO
                else -> false
            }
        }
        return false
    }

    private fun conferir(dest: Tipo, origem: Tipo?, expr: No, contexto: String) {
        if (origem == null) return // Expressao de biblioteca ainda sem assinatura.
        if (!conversaoSegura(dest, origem) && !literalCabe(dest, expr))
            erro(expr, contexto + ": conversao implicita potencialmente insegura de " +
                origem.base.toString().lowercase() + " para " +
                dest.base.toString().lowercase())
    }

    private fun tipo(expr: No): Tipo? = when (expr) {
        is Numero -> Tipo(expr.tipo)
        is Texto -> Tipo(TipoDado.TEXTO)
        is Caractere -> Tipo(TipoDado.CARACTERE)
        is Logico -> Tipo(TipoDado.LOGICO)
        is Identificador -> procurar(expr.nome)
        is OperacaoUnaria -> {
            val t = tipo(expr.operando)
            if (t != null) {
                if (expr.operador == "!" && t.base != TipoDado.LOGICO)
                    erro(expr, "negacao exige valor logico")
                if (expr.operador == "-" && !numerico(t))
                    erro(expr, "sinal negativo exige numero")
            }
            t
        }
        is OperacaoBinaria -> {
            val a = tipo(expr.esquerda)
            val b = tipo(expr.direita)
            if (a == null || b == null) null
            else when (expr.operador) {
                "&&", "||" -> {
                    if (a.base != TipoDado.LOGICO || b.base != TipoDado.LOGICO)
                        erro(expr, "operacao logica exige valores logicos")
                    Tipo(TipoDado.LOGICO)
                }
                "==", "!=", "<", ">", "<=", ">=" -> {
                    if (a != b && (!numerico(a) || !numerico(b)))
                        erro(expr, "comparacao entre tipos incompativeis")
                    Tipo(TipoDado.LOGICO)
                }
                "+", "-", "*", "/", "%" -> {
                    if (!numerico(a) || !numerico(b))
                        erro(expr, "operacao aritmetica exige numeros")
                    if (expr.operador == "%" && (a.base !in inteiros || b.base !in inteiros))
                        erro(expr, "operador de resto exige inteiros")
                    when {
                        a.base == TipoDado.DUPLO || b.base == TipoDado.DUPLO ||
                            a.base == TipoDado.DUPLO_LONGO || b.base == TipoDado.DUPLO_LONGO ->
                            Tipo(TipoDado.DUPLO)
                        a.base == TipoDado.REAL || b.base == TipoDado.REAL -> Tipo(TipoDado.REAL)
                        else -> Tipo(TipoDado.INTEIRO)
                    }
                }
                else -> null
            }
        }
        is ChamadaFuncao -> {
            val f = funcoes[expr.nome]
            expr.argumentos.forEach { tipo(it) }
            if (f != null) {
                if (expr.argumentos.size != f.parametros.size)
                    erro(expr, "quantidade incorreta de argumentos em " + expr.nome)
                for ((i, parametro) in f.parametros.withIndex())
                    conferir(parametro.tipo, tipo(expr.argumentos[i]), expr.argumentos[i],
                        "argumento " + (i + 1) + " de " + expr.nome)
            }
            f?.tipoRetorno
        }
        is AcessoIndice -> {
            tipo(expr.indice)
            val t = tipo(expr.array)
            if (t != null && t.ehArray && !t.ehArray2D) t.copy(ehArray=false, tamanhoFixo=null)
            else null
        }
        else -> null // Outros nos permanecem sob validacao legada.
    }

    private fun bloco(comandos: List<No>, novoEscopo: Boolean = true) {
        if (novoEscopo) entrar()
        for (n in comandos) comando(n)
        if (novoEscopo) sair()
    }
    private fun comando(n: No) {
        when (n) {
            is DeclaracaoVariavel -> {
                n.valorInicial?.let { conferir(n.tipo, tipo(it), it, "declaracao de " + n.nome) }
                salvar(n.nome, n.tipo)
            }
            is ComandoDeclaracoes -> n.declaracoes.forEach { comando(it) }
            is Atribuicao -> {
                val d = when (val alvo = n.alvo) {
                    is Identificador -> procurar(alvo.nome)
                    else -> tipo(alvo)
                }
                if (d != null) conferir(d, tipo(n.valor), n.valor, "atribuicao")
            }
            is ComandoRetorna -> {
                val esperado = retorno
                val expr = n.valor
                if (esperado != null && expr != null) conferir(esperado, tipo(expr), expr, "retorno")
            }
            is ComandoImprimir -> n.argumentos.forEach { tipo(it) }
            is ExpressaoComando -> tipo(n.expressao)
            is ComandoSe -> {
                tipo(n.condicao); bloco(n.entao)
                n.senaoSe.forEach { tipo(it.primeiro); bloco(it.segundo) }
                n.senao?.let { bloco(it) }
            }
            is ComandoEnquanto -> { tipo(n.condicao); bloco(n.corpo) }
            is ComandoFacaEnquanto -> { bloco(n.corpo); tipo(n.condicao) }
            is ComandoPara -> {
                entrar()
                n.inicializacao?.let { comando(it) }
                n.condicao?.let { tipo(it) }
                n.incremento?.let { comando(it) }
                bloco(n.corpo)
                sair()
            }
            is ComandoParaCada -> {
                tipo(n.array); entrar(); salvar(n.nomeElemento,n.tipoElemento); bloco(n.corpo); sair()
            }
            is ComandoEscolher -> {
                tipo(n.valor)
                n.casos.forEach { tipo(it.valor); bloco(it.corpo) }
                n.padrao?.let { bloco(it) }
            }
            else -> Unit
        }
    }

    fun validar(programa: Programa, modulos: List<Programa> = emptyList()) {
        escopos.clear(); funcoes.clear()
        for (p in modulos + programa) for (d in p.declaracoesGlobais)
            if (d is DeclaracaoFuncao) funcoes[d.nome] = d
        for (d in programa.declaracoesGlobais) if (d is DeclaracaoFuncao && d.corpo != null) {
            entrar()
            d.parametros.forEach { salvar(it.nome, it.tipo) }
            retorno = d.tipoRetorno
            bloco(d.corpo, false)
            sair()
        }
        retorno = Tipo(TipoDado.INTEIRO)
        programa.inicio?.let { bloco(it.comandos) }
        retorno = null
    }
}
