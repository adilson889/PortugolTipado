package co.adilson889.typec.validador

import co.adilson889.typec.ast.*

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
    private fun bits(t: Tipo): Int = when (t.base) {
        TipoDado.INTEIRO_CURTO -> 16
        TipoDado.INTEIRO, TipoDado.INTEIRO_POSITIVO, TipoDado.INTEIRO_LONGO -> 32
        TipoDado.INTEIRO_GIGANTE -> 64
        else -> 0
    }
    private fun mantissa(t: Tipo): Int = when (t.base) {
        TipoDado.REAL -> 24
        TipoDado.DUPLO, TipoDado.DUPLO_LONGO -> 53
        else -> 0
    }

    private fun valorInteiro(n: No): Long? = when (n) {
        is Numero -> if (n.tipo == TipoDado.INTEIRO) n.valor.toLongOrNull() else null
        is OperacaoUnaria -> if (n.operador == "-") valorInteiro(n.operando)?.let { -it } else null
        else -> null
    }

    private fun literalCabe(destino: Tipo, expr: No): Boolean {
        val v = valorInteiro(expr) ?: return false
        return when (destino.base) {
            TipoDado.INTEIRO_CURTO -> v in Short.MIN_VALUE..Short.MAX_VALUE
            TipoDado.INTEIRO -> v in Int.MIN_VALUE..Int.MAX_VALUE
            TipoDado.INTEIRO_POSITIVO -> v in 0L..4294967295L
            TipoDado.INTEIRO_LONGO -> v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
            TipoDado.INTEIRO_GIGANTE -> true
            TipoDado.REAL -> v in -16777216L..16777216L
            TipoDado.DUPLO, TipoDado.DUPLO_LONGO -> v in -9007199254740992L..9007199254740992L
            else -> false
        }
    }

    private fun conversaoSegura(dest: Tipo, origem: Tipo): Boolean {
        if (dest == origem) return true
        if (!escalar(dest) || !escalar(origem)) return false
        if (!numerico(dest) || !numerico(origem)) return false
        if (dest.base in inteiros && origem.base in inteiros) {
            val origUnsigned = origem.base == TipoDado.INTEIRO_POSITIVO
            val destUnsigned = dest.base == TipoDado.INTEIRO_POSITIVO
            if (origUnsigned && destUnsigned) return true
            if (origUnsigned) return !destUnsigned && bits(dest) > bits(origem)
            if (destUnsigned) return false
            return bits(dest) >= bits(origem)
        }
        if (dest.base in flutuantes && origem.base in flutuantes) {
            return mantissa(dest) >= mantissa(origem)
        }
        if (dest.base in flutuantes && origem.base in inteiros) {
            return mantissa(dest) >= bits(origem)
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
