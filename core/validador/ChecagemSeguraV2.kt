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
        if (expr is ArrayLiteral) {
            validarAgregado(dest, expr, contexto)
            return
        }
        if (origem == null) return // Chamada externa sem assinatura tipada.
        if (!conversaoSegura(dest, origem) && !literalCabe(dest, expr))
            erro(expr, contexto + ": conversao implicita insegura de " +
                origem.base.toString().lowercase() + " para " +
                dest.base.toString().lowercase())
    }

    private fun validarAgregado(dest: Tipo, expr: ArrayLiteral, contexto: String) {
        if (dest.nomeStruct != null && !dest.ehArray && !dest.ehArray2D) {
            val estrutura = estruturas[dest.nomeStruct]
                ?: erro(expr, "estrutura '" + dest.nomeStruct + "' nao declarada")
            if (expr.valores.size != estrutura.campos.size)
                erro(expr, contexto + ": a estrutura " + estrutura.nome + " exige " +
                    estrutura.campos.size + " campo(s)")
            for ((i, valor) in expr.valores.withIndex())
                conferir(estrutura.campos[i].tipo, tipo(valor), valor,
                    contexto + ", campo " + estrutura.campos[i].nome)
            return
        }
        if (!dest.ehArray && !dest.ehArray2D)
            erro(expr, contexto + ": literal com chaves exige array ou estrutura")
        if (dest.tamanhoFixo != null && expr.valores.size > dest.tamanhoFixo)
            erro(expr, contexto + ": mais elementos que a capacidade do array")
        val elemento = if (dest.ehArray2D)
            dest.copy(ehArray2D=false, ehArray=true, tamanhoFixo=dest.tamanhoFixo2,
                tamanhoFixo2=null)
        else dest.copy(ehArray=false, tamanhoFixo=null)
        for ((i, valor) in expr.valores.withIndex())
            conferir(elemento, tipo(valor), valor, contexto + ", elemento " + (i+1))
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
                    if (!escalar(a) || !escalar(b) ||
                        a.base != TipoDado.LOGICO || b.base != TipoDado.LOGICO)
                        erro(expr, "operacao logica exige valores logicos")
                    Tipo(TipoDado.LOGICO)
                }
                "==", "!=", "<", ">", "<=", ">=" -> {
                    val saoNumeros = numerico(a) && numerico(b)
                    val mesmosEscalares = escalar(a) && escalar(b) && a.base == b.base
                    if (!saoNumeros && !mesmosEscalares)
                        erro(expr, "comparacao entre tipos incompativeis")
                    if (expr.operador !in listOf("==","!=") && !saoNumeros &&
                        a.base != TipoDado.CARACTERE)
                        erro(expr, "comparacao relacional exige numeros ou caracteres")
                    Tipo(TipoDado.LOGICO)
                }
                "+", "-", "*", "/", "%" -> {
                    if (!numerico(a) || !numerico(b))
                        erro(expr, "operacao aritmetica exige numeros")
                    if (expr.operador == "%" &&
                        (a.base !in inteiros || b.base !in inteiros))
                        erro(expr, "operador de resto exige inteiros")
                    val positivo = TipoDado.INTEIRO_POSITIVO
                    if (a.base in inteiros && b.base in inteiros &&
                        (a.base == positivo) != (b.base == positivo))
                        erro(expr, "mistura de inteiro positivo e assinado exige conversao explicita")
                    val res = when {
                        a.base in flutuantes || b.base in flutuantes ->
                            listOf(a.base,b.base).filter { it in flutuantes }
                                .maxBy { ordemReal(it) }
                        a.base == positivo -> positivo
                        else -> listOf(a.base,b.base).maxBy { ordemInteiro(it) }
                    }
                    Tipo(if (res == TipoDado.INTEIRO_CURTO) TipoDado.INTEIRO else res)
                }
                else -> null
            }
        }
        is ChamadaFuncao -> {
            val f = funcoes[expr.nome]
            if (f != null) {
                if (expr.argumentos.size != f.parametros.size)
                    erro(expr, "funcao '" + expr.nome + "' espera " +
                        f.parametros.size + " argumento(s), recebeu " +
                        expr.argumentos.size)
                for ((i, parametro) in f.parametros.withIndex()) {
                    val arg = expr.argumentos[i]
                    if (parametro.ehAlterar) {
                        if (arg !is Identificador && arg !is AcessoCampo &&
                            arg !is AcessoIndice)
                            erro(arg, "'altere' exige variavel, campo ou indice atribuivel")
                        val t = tipo(arg)
                        if (t != null && (t.base != parametro.tipo.base ||
                            t.ehArray != parametro.tipo.ehArray ||
                            t.nomeStruct != parametro.tipo.nomeStruct))
                            erro(arg, "argumento por referencia exige o tipo exato")
                    } else conferir(parametro.tipo, tipo(arg), arg,
                        "argumento " + (i+1) + " de " + expr.nome)
                }
            } else expr.argumentos.forEach { tipo(it) }
            f?.tipoRetorno
        }
        is AcessoIndice -> {
            val indice = tipo(expr.indice)
            if (indice != null && !escalar(indice) || indice != null &&
                indice.base !in inteiros)
                erro(expr.indice, "indice de array deve ser inteiro")
            val t = tipo(expr.array)
            when {
                t == null -> null
                t.ehArray2D -> t.copy(ehArray2D=false, ehArray=true,
                    tamanhoFixo=t.tamanhoFixo2,tamanhoFixo2=null)
                t.ehArray -> t.copy(ehArray=false,tamanhoFixo=null)
                t.base == TipoDado.TEXTO && escalar(t) -> Tipo(TipoDado.CARACTERE)
                else -> erro(expr, "acesso por indice exige array ou texto")
            }
        }
        is AcessoCampo -> {
            val t = tipo(expr.objeto)
            if (t == null) null else {
                if (t.nomeStruct == null || t.ehArray || t.ehArray2D)
                    erro(expr, "acesso por campo exige uma estrutura")
                val est = estruturas[t.nomeStruct]
                    ?: erro(expr, "estrutura '" + t.nomeStruct + "' nao declarada")
                est.campos.find { it.nome == expr.campo }?.tipo
                    ?: erro(expr, "campo '" + expr.campo + "' nao existe na estrutura " + est.nome)
            }
        }
        is ArrayLiteral -> {
            expr.valores.forEach { tipo(it) }
            null // Contexto do agregado e obtido a partir do tipo de destino.
        }
        else -> null // Outros nos permanecem sob validacao legada.
    }

    private fun bloco(comandos: List<No>, novoEscopo: Boolean = true) {
        if (novoEscopo) entrar()
        try { comandos.forEach { comando(it) } }
        finally { if (novoEscopo) sair() }
    }

    private fun exigirCondicao(expr: No) {
        val t = tipo(expr)
        if (t != null && (!escalar(t) || t.base != TipoDado.LOGICO))
            erro(expr, "condicao exige logico (exemplo: idade > 18)")
    }

    private fun conferirAtribuicao(a: Atribuicao) {
        val destino = tipo(a.alvo) ?: return
        if (destino.ehArray || destino.ehArray2D)
            erro(a, "array inteiro nao pode receber atribuicao; use um indice")
        if (a.operador == "=") conferir(destino,tipo(a.valor),a.valor,"atribuicao")
        else {
            val combinacao = OperacaoBinaria(a.alvo,a.operador.removeSuffix("="),a.valor,a.linha)
            conferir(destino,tipo(combinacao),combinacao,"atribuicao composta")
        }
    }

    private fun retornaSempre(comandos: List<No>): Boolean = comandos.any { cmd ->
        when (cmd) {
            is ComandoRetorna -> true
            is ComandoSe -> cmd.senao != null && retornaSempre(cmd.entao) &&
                cmd.senaoSe.all { retornaSempre(it.segundo) } && retornaSempre(cmd.senao)
            else -> false
        }
    }

    private fun comando(n: No) {
        when (n) {
            is DeclaracaoVariavel -> {
                n.valorInicial?.let { conferir(n.tipo, tipo(it), it, "declaracao de " + n.nome) }
                salvar(n.nome, n.tipo)
            }
            is ComandoDeclaracoes -> n.declaracoes.forEach { comando(it) }
            is Atribuicao -> conferirAtribuicao(n)
            is IncrementoDecremento -> {
                val t = tipo(n.alvo)
                if (t != null && !numerico(t))
                    erro(n, "incremento ou decremento exige variavel numerica")
            }
            is ComandoRetorna -> {
                val esperado = retorno
                if (esperado != null) {
                    if (n.valor == null && esperado.base != TipoDado.VAZIO && !emInicio)
                        erro(n, "funcao com resultado exige valor de retorno")
                    if (n.valor != null && esperado.base == TipoDado.VAZIO)
                        erro(n, "funcao 'vazio' nao deve retornar valor")
                    n.valor?.let { conferir(esperado,tipo(it),it,"retorno") }
                }
            }
            is ComandoImprimir -> n.argumentos.forEach { tipo(it) }
            is ComandoLer -> tipo(n.alvo)
            is ExpressaoComando -> tipo(n.expressao)
            is ComandoSe -> {
                exigirCondicao(n.condicao); bloco(n.entao)
                n.senaoSe.forEach { exigirCondicao(it.primeiro); bloco(it.segundo) }
                n.senao?.let { bloco(it) }
            }
            is ComandoEnquanto -> { exigirCondicao(n.condicao); bloco(n.corpo) }
            is ComandoFacaEnquanto -> { bloco(n.corpo); exigirCondicao(n.condicao) }
            is ComandoPara -> {
                entrar()
                try {
                    n.inicializacao?.let { comando(it) }
                    n.condicao?.let { exigirCondicao(it) }
                    n.incremento?.let { comando(it) }
                    bloco(n.corpo)
                } finally { sair() }
            }
            is ComandoParaCada -> {
                val t = tipo(n.array)
                if (t != null && !t.ehArray && !t.ehArray2D)
                    erro(n, "'cada' exige array")
                entrar()
                try { salvar(n.nomeElemento,n.tipoElemento); bloco(n.corpo) }
                finally { sair() }
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
        escopos.clear(); funcoes.clear(); estruturas.clear()
        for (p in modulos + programa) for (d in p.declaracoesGlobais) {
            if (d is DeclaracaoFuncao) funcoes[d.nome] = d
            if (d is DeclaracaoStruct) estruturas[d.nome] = d
        }
        for (d in programa.declaracoesGlobais) if (d is DeclaracaoFuncao && d.corpo != null) {
            entrar()
            try {
                d.parametros.forEach { salvar(it.nome,it.tipo) }
                retorno = d.tipoRetorno
                emInicio = false
                bloco(d.corpo,false)
                if (d.tipoRetorno.base != TipoDado.VAZIO && !retornaSempre(d.corpo))
                    erro(d, "funcao '" + d.nome + "' pode terminar sem retornar valor")
            } finally { sair() }
        }
        retorno = Tipo(TipoDado.INTEIRO)
        emInicio = true
        programa.inicio?.let { bloco(it.comandos) }
        emInicio = false
        retorno = null
    }
}
