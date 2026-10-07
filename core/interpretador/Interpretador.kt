package co.adilson889.typec.interpretador

import co.adilson889.typec.ast.*
import co.adilson889.typec.graficos.ErroGrafico
import co.adilson889.typec.graficos.Graficos
import co.adilson889.typec.graficos.LibGraficos
import kotlin.math.*

/**
* Interpretador da AST do PortugolTipado. Não gera C nem depende de gcc/NDK —
* executa a árvore diretamente em Kotlin, no mesmo espírito do
* SuperMontador/shmiJS usado no Chorty/Edu.
*
* Usado exclusivamente para alimentar o Preview (mostrar o que o
* programa produziria ao rodar). O C gerado pelo Transpilador continua
* sendo o produto final de verdade.
*/

// ---------------------------------------------------------------------
// Saída do interpretador: uma lista de linhas de texto (o que teria
// sido impresso), ou um erro de execução se algo falhar no meio.
// ---------------------------------------------------------------------

sealed class ResultadoExecucao {
    data class Sucesso(val saida: String) : ResultadoExecucao()
    data class Erro(val mensagem: String, val linha: Int) : ResultadoExecucao()
    // Nao e erro do programa: o Preview parou a execucao por seguranca (ex: limite de passos)
    data class Interrompido(val mensagem: String, val linha: Int, val saida: String) : ResultadoExecucao()
}

class ErroExecucao(mensagem: String, val linha: Int) : Exception(mensagem)

/** Aviso (nao erro): a execucao foi interrompida por seguranca, mas o programa pode estar certo. */
class AvisoExecucao(mensagem: String, val linha: Int) : Exception(mensagem)

/** Fonte de entrada para o comando leia(): implementação real pausa e
*  espera o usuário digitar na janela flutuante (suspend function). */
interface FonteEntrada {
    suspend fun pedirValor(promptLinha: Int): String
}

// ---------------------------------------------------------------------
// Valor em tempo de execução (o que uma variável guarda de fato
// enquanto o interpretador roda)
// ---------------------------------------------------------------------

sealed class Valor {
    data class Inteiro(val v: Long) : Valor()
    data class Decimal(val v: Double) : Valor()
    data class Texto(val v: String) : Valor()
    data class Caractere(val v: Char) : Valor()
    data class Logico(val v: Boolean) : Valor()
    data class Array(val itens: MutableList<Valor>) : Valor()
    data class Struct(val nomeTipo: String, val campos: MutableMap<String, Valor>) : Valor()
    object Vazio : Valor()
}

// Sinal interno usado para propagar 'retorne' através da recursão de execução de comandos
private class SinalRetorno(val valor: Valor) : RuntimeException()
private object SinalDispensar : RuntimeException()
private object SinalIgnorar : RuntimeException()

// ---------------------------------------------------------------------
// Ambiente: pilha de escopos (mapa nome -> Valor), mais referências
// para simular 'altere' (aponta para a caixa da variável original)
// ---------------------------------------------------------------------

private class Caixa(var valor: Valor)

private class Ambiente {
    private val escopos = mutableListOf<MutableMap<String, Caixa>>(mutableMapOf())

    fun entrarEscopo() = escopos.add(mutableMapOf())
    fun sairEscopo() = escopos.removeAt(escopos.size - 1)

    fun declarar(nome: String, valor: Valor) {
        escopos.last()[nome] = Caixa(valor)
    }

    fun declararComCaixa(nome: String, caixa: Caixa) {
        escopos.last()[nome] = caixa
    }

    fun caixaDe(nome: String): Caixa? {
        for (i in escopos.indices.reversed()) {
            escopos[i][nome]?.let { return it }
        }
        return null
    }

    fun obter(nome: String): Valor = caixaDe(nome)?.valor ?: Valor.Vazio

    fun atribuir(nome: String, valor: Valor) {
        caixaDe(nome)?.let { it.valor = valor }
    }
}

class Interpretador(
    private val fonteEntrada: FonteEntrada? = null,
    private val aoImprimir: ((String) -> Unit)? = null, // callback opcional, chamado a cada escreva() com a saída acumulada até agora
    private val graficos: Graficos? = null, // implementação de UI para 'inclua graficos' (ex: GraficosCanvas no Preview); null = sem suporte gráfico neste host
    private val limitePassos: Int = 2_000_000 // guarda contra loop infinito (o Preview usa o padrão; o APK passa Int.MAX_VALUE, sem limite)
) {

    private val saida = StringBuilder()
    private val funcoes = mutableMapOf<String, DeclaracaoFuncao>()
    private val structs = mutableMapOf<String, DeclaracaoStruct>()
    private val tiposDeclarados = mutableMapOf<String, Tipo>() // nome variável -> tipo (para leia() saber converter)

    private var passos = 0

    suspend fun executar(programa: Programa, modulos: List<Programa> = emptyList()): ResultadoExecucao {
        saida.clear()
        funcoes.clear()
        structs.clear()
        tiposDeclarados.clear()
        passos = 0

        // Módulos incluídos via 'inclua "arquivo"': registados primeiro, para que o
        // programa principal possa redefinir um nome sem conflito.
        for (modulo in modulos) {
            for (decl in modulo.declaracoesGlobais) {
                when (decl) {
                    is DeclaracaoFuncao -> funcoes[decl.nome] = decl
                    is DeclaracaoStruct -> structs[decl.nome] = decl
                    else -> {}
                }
            }
        }

        for (decl in programa.declaracoesGlobais) {
            when (decl) {
                is DeclaracaoFuncao -> funcoes[decl.nome] = decl
                is DeclaracaoStruct -> structs[decl.nome] = decl
                else -> {}
            }
        }

        val ambiente = Ambiente()
        val bloco = programa.inicio
        if (bloco == null) {
            // Módulo puro (sem 'inicio()') — nada para o Preview rodar.
            return ResultadoExecucao.Sucesso("")
        }
        return try {
            for (comando in bloco.comandos) {
                executarComando(comando, ambiente)
            }
            ResultadoExecucao.Sucesso(saida.toString())
        } catch (e: ErroExecucao) {
            ResultadoExecucao.Erro(e.message ?: "erro desconhecido", e.linha)
        } catch (e: AvisoExecucao) {
            ResultadoExecucao.Interrompido(e.message ?: "execução interrompida", e.linha, saida.toString())
        } catch (e: SinalRetorno) {
            // retorno solto no nível de inicio() não deveria ocorrer, mas não é fatal
            ResultadoExecucao.Sucesso(saida.toString())
        } catch (e: StackOverflowError) {
            ResultadoExecucao.Erro("recursão infinita ou profunda demais", 0)
        }
    }

    private fun contarPasso(linha: Int) {
        passos++
        if (passos > limitePassos) {
            throw AvisoExecucao(
                "o programa executou muitos passos seguidos sem desenhar nem fazer pausa e foi interrompido para não travar o app (possível loop infinito)",
                linha
            )
        }
    }

    // -------------------------------------------------------------
    // Comandos
    // -------------------------------------------------------------

    private suspend fun executarComando(comando: No, ambiente: Ambiente) {
        contarPasso(comando.linha)
        when (comando) {
            is DeclaracaoVariavel -> {
                val valor = if (comando.valorInicial != null) {
                    val v = avaliar(comando.valorInicial, ambiente)
                    // Se o tipo declarado é struct e o valor veio como {} (ArrayLiteral),
                    // converte para Valor.Struct usando a ordem dos campos declarados.
                    if (comando.tipo.nomeStruct != null && !comando.tipo.ehArray && v is Valor.Array) {
                        converterParaStruct(comando.tipo.nomeStruct, v)
                    } else {
                        v
                    }
                } else {
                    valorPadrao(comando.tipo)
                }
                tiposDeclarados[comando.nome] = comando.tipo
                ambiente.declarar(comando.nome, valor)
            }
            is ComandoImprimir -> {
                for (arg in comando.argumentos) {
                    val v = avaliar(arg, ambiente)
                    saida.append(formatarValor(v))
                }
                aoImprimir?.invoke(saida.toString())
            }
            is ComandoLer -> {
                val alvo = comando.alvo
                val tipoAlvo = if (alvo is Identificador) tiposDeclarados[alvo.nome] else null
                val textoDigitado = fonteEntrada?.pedirValor(comando.linha) ?: "0"
                val valorLido = converterTextoParaTipo(textoDigitado, tipoAlvo)
                if (alvo is Identificador) {
                    ambiente.atribuir(alvo.nome, valorLido)
                }
            }
            is ComandoRetorna -> {
                val v = comando.valor?.let { avaliar(it, ambiente) } ?: Valor.Vazio
                throw SinalRetorno(v)
            }
            is ComandoSe -> {
                if (verdadeiro(avaliar(comando.condicao, ambiente))) {
                    executarBloco(comando.entao, ambiente)
                    return
                }
                for (par in comando.senaoSe) {
                    if (verdadeiro(avaliar(par.primeiro, ambiente))) {
                        executarBloco(par.segundo, ambiente)
                        return
                    }
                }
                comando.senao?.let { executarBloco(it, ambiente) }
            }
            is ComandoEnquanto -> {
                while (verdadeiro(avaliar(comando.condicao, ambiente))) {
                    contarPasso(comando.linha)
                    try {
                        executarBloco(comando.corpo, ambiente)
                    } catch (s: SinalDispensar) {
                        break
                    } catch (s: SinalIgnorar) {
                        continue
                    }
                }
            }
            is ComandoFacaEnquanto -> {
                do {
                    contarPasso(comando.linha)
                    try {
                        executarBloco(comando.corpo, ambiente)
                    } catch (s: SinalDispensar) {
                        break
                    } catch (s: SinalIgnorar) {
                        // 'continue' no faca: segue para a avaliacao da condicao
                    }
                } while (verdadeiro(avaliar(comando.condicao, ambiente)))
            }
            is ComandoDeclaracoes -> comando.declaracoes.forEach { executarComando(it, ambiente) }
            is ComandoPara -> {
                ambiente.entrarEscopo()
                try {
                    comando.inicializacao?.let { executarComando(it, ambiente) }
                    while (comando.condicao == null || verdadeiro(avaliar(comando.condicao, ambiente))) {
                        contarPasso(comando.linha)
                        try {
                            executarBloco(comando.corpo, ambiente)
                        } catch (s: SinalDispensar) {
                            break
                        } catch (s: SinalIgnorar) {
                            // segue para o incremento mesmo assim
                        }
                        comando.incremento?.let { executarComando(it, ambiente) }
                    }
                } finally {
                    ambiente.sairEscopo()
                }
            }
            is ComandoParaCada -> {
                val arrayValor = avaliar(comando.array, ambiente)
                val itens = (arrayValor as? Valor.Array)?.itens ?: mutableListOf()
                for (item in itens) {
                    contarPasso(comando.linha)
                    ambiente.entrarEscopo()
                    ambiente.declarar(comando.nomeElemento, item)
                    try {
                        executarBloco(comando.corpo, ambiente)
                    } catch (s: SinalDispensar) {
                        ambiente.sairEscopo()
                        break
                    } catch (s: SinalIgnorar) {
                        // continua pro próximo item
                    }
                    ambiente.sairEscopo()
                }
            }
            is ComandoDispensar -> throw SinalDispensar
            is ComandoIgnorar -> throw SinalIgnorar
            is ComandoEscolher -> {
                val valor = avaliar(comando.valor, ambiente)
                var executou = false
                for (caso in comando.casos) {
                    val valorCaso = avaliar(caso.valor, ambiente)
                    if (valoresIguais(valor, valorCaso)) {
                        executou = true
                    }
                    if (executou) {
                        try {
                            executarBloco(caso.corpo, ambiente)
                        } catch (s: SinalDispensar) {
                            return
                        }
                    }
                }
                if (!executou && comando.padrao != null) {
                    try {
                        executarBloco(comando.padrao, ambiente)
                    } catch (s: SinalDispensar) {
                        return
                    }
                }
            }
            is Atribuicao -> executarAtribuicao(comando, ambiente)
            is IncrementoDecremento -> {
                val atual = avaliarAlvoNumerico(comando.alvo, ambiente)
                val novo = if (comando.operador == "++") atual + 1 else atual - 1
                atribuirValorEm(comando.alvo, numeroParaValor(atual, novo), ambiente)
            }
            is ExpressaoComando -> avaliar(comando.expressao, ambiente)
            else -> {}
        }
    }

    private suspend fun executarBloco(comandos: List<No>, ambiente: Ambiente) {
        ambiente.entrarEscopo()
        try {
            for (c in comandos) executarComando(c, ambiente)
        } finally {
            ambiente.sairEscopo()
        }
    }

    private fun valorPadrao(tipo: Tipo): Valor {
    if (tipo.ehArray) {
        val tamanho = tipo.tamanhoFixo ?: 0
        val tipoElemento = tipo.copy(ehArray = false, tamanhoFixo = null)
        return Valor.Array(MutableList(tamanho) { valorPadrao(tipoElemento) })
    }
    if (tipo.nomeStruct != null) return valorStructPadrao(tipo.nomeStruct)
    return when (tipo.base) {
        TipoDado.TEXTO -> Valor.Texto("")
        TipoDado.CARACTERE -> Valor.Caractere(' ')
        TipoDado.LOGICO -> Valor.Logico(false)
        TipoDado.REAL, TipoDado.DUPLO, TipoDado.DUPLO_LONGO -> Valor.Decimal(0.0)
        else -> Valor.Inteiro(0)
    }
}

    private fun valorStructPadrao(nomeStruct: String): Valor {
        val decl = structs[nomeStruct] ?: return Valor.Vazio
        val campos = mutableMapOf<String, Valor>()
        for (campo in decl.campos) campos[campo.nome] = valorPadrao(campo.tipo)
        return Valor.Struct(nomeStruct, campos)
    }

    /** Converte um Valor.Array (vindo do literal {}) para Valor.Struct,
    *  usando a ordem dos campos declarados na struct (mesma lógica do
    *  inicializador posicional do C: {"Ana", 25} -> nome="Ana", idade=25). */
    private fun converterParaStruct(nomeStruct: String, valores: Valor.Array): Valor {
        val decl = structs[nomeStruct] ?: return valores
        val campos = mutableMapOf<String, Valor>()
        for (i in decl.campos.indices) {
            val campoDecl = decl.campos[i]
            val valorBruto = valores.itens.getOrNull(i)
            campos[campoDecl.nome] = when {
                valorBruto == null -> valorPadrao(campoDecl.tipo)
                // Campo array de struct com sub-literal aninhado {8, 9, 7}
                campoDecl.tipo.ehArray && valorBruto is Valor.Array -> valorBruto
                // Campo que é outra struct aninhada
                campoDecl.tipo.nomeStruct != null && valorBruto is Valor.Array ->
                converterParaStruct(campoDecl.tipo.nomeStruct, valorBruto)
                else -> valorBruto
            }
        }
        return Valor.Struct(nomeStruct, campos)
    }

    // -------------------------------------------------------------
    // Atribuição (considera índice de array e campo de struct como alvo)
    // -------------------------------------------------------------

    private suspend fun executarAtribuicao(atr: Atribuicao, ambiente: Ambiente) {
        val valorNovo = avaliar(atr.valor, ambiente)
        val valorFinal = if (atr.operador == "=") {
            valorNovo
        } else {
            val atual = avaliar(atr.alvo, ambiente)
            aplicarOperadorComposto(atr.operador, atual, valorNovo)
        }
        atribuirValorEm(atr.alvo, valorFinal, ambiente)
    }

    private suspend fun atribuirValorEm(alvo: No, valor: Valor, ambiente: Ambiente) {
        when (alvo) {
            is Identificador -> ambiente.atribuir(alvo.nome, valor)
            is AcessoIndice -> {
                val arrayValor = avaliar(alvo.array, ambiente) as? Valor.Array ?: return
                val indice = (avaliar(alvo.indice, ambiente) as? Valor.Inteiro)?.v?.toInt() ?: return
                if (indice in arrayValor.itens.indices) arrayValor.itens[indice] = valor
            }
            is AcessoCampo -> {
                val objetoValor = avaliar(alvo.objeto, ambiente) as? Valor.Struct ?: return
                objetoValor.campos[alvo.campo] = valor
            }
            else -> {}
        }
    }

    private fun aplicarOperadorComposto(operador: String, atual: Valor, novo: Valor): Valor {
        val op = operador.dropLast(1) // "+=" -> "+"
        return aplicarOperadorBinario(op, atual, novo, 0)
    }

    // -------------------------------------------------------------
    // Avaliação de expressões
    // -------------------------------------------------------------

    private suspend fun avaliar(expressao: No, ambiente: Ambiente): Valor {
        contarPasso(expressao.linha)
        return when (expressao) {
            is Numero -> if (expressao.tipo == TipoDado.INTEIRO) {
                Valor.Inteiro(expressao.valor.toLong())
            } else {
                Valor.Decimal(expressao.valor.toDouble())
            }
            is Texto -> Valor.Texto(resolverEscapes(expressao.valor))
            is Caractere -> Valor.Caractere(expressao.valor)
            is Logico -> Valor.Logico(expressao.valor)
            is Identificador -> when {
                expressao.nome == "PI" -> Valor.Decimal(PI)
                expressao.nome == "E" -> Valor.Decimal(E)
                // Constantes de LibGraficos (TECLA_ESC etc.) so existem como Long no Kotlin
                // do lado da biblioteca -- nao sao variaveis do Ambiente, resolvidas aqui direto.
                LibGraficos.ehConstante(expressao.nome) ->
                    Valor.Inteiro(LibGraficos.constantes.getValue(expressao.nome).valor)
                else -> ambiente.obter(expressao.nome)
            }
            is OperacaoBinaria -> {
                val esq = avaliar(expressao.esquerda, ambiente)
                if (expressao.operador == "&&" && !verdadeiro(esq)) return Valor.Logico(false)
                if (expressao.operador == "||" && verdadeiro(esq)) return Valor.Logico(true)
                val dir = avaliar(expressao.direita, ambiente)
                aplicarOperadorBinario(expressao.operador, esq, dir, expressao.linha)
            }
            is OperacaoUnaria -> {
                val v = avaliar(expressao.operando, ambiente)
                when (expressao.operador) {
                    "!" -> Valor.Logico(!verdadeiro(v))
                    "-" -> when (v) {
                        is Valor.Inteiro -> Valor.Inteiro(-v.v)
                        is Valor.Decimal -> Valor.Decimal(-v.v)
                        else -> v
                    }
                    else -> v
                }
            }
            is ChamadaFuncao -> chamarFuncao(expressao, ambiente)
            is AcessoIndice -> {
                val arrayValor = avaliar(expressao.array, ambiente) as? Valor.Array
                ?: throw ErroExecucao("tentou acessar índice de algo que não é array", expressao.linha)
                val indice = (avaliar(expressao.indice, ambiente) as? Valor.Inteiro)?.v?.toInt()
                ?: throw ErroExecucao("índice inválido", expressao.linha)
                if (indice !in arrayValor.itens.indices) {
                    throw ErroExecucao("índice $indice fora dos limites do array (tamanho ${arrayValor.itens.size})", expressao.linha)
                }
                arrayValor.itens[indice]
            }
            is AcessoCampo -> {
                val objetoValor = avaliar(expressao.objeto, ambiente) as? Valor.Struct
                ?: throw ErroExecucao("tentou acessar campo de algo que não é struct", expressao.linha)
                objetoValor.campos[expressao.campo] ?: Valor.Vazio
            }
            is ArrayLiteral -> Valor.Array(expressao.valores.map { avaliar(it, ambiente) }.toMutableList())
            is Atribuicao -> { executarAtribuicao(expressao, ambiente); avaliar(expressao.alvo, ambiente) }
            is IncrementoDecremento -> {
                val atual = avaliarAlvoNumerico(expressao.alvo, ambiente)
                val novo = if (expressao.operador == "++") atual + 1 else atual - 1
                val valorNovo = numeroParaValor(atual, novo)
                atribuirValorEm(expressao.alvo, valorNovo, ambiente)
                valorNovo
            }
            else -> Valor.Vazio
        }
    }

    private suspend fun avaliarAlvoNumerico(alvo: No, ambiente: Ambiente): Double {
        return when (val v = avaliar(alvo, ambiente)) {
            is Valor.Inteiro -> v.v.toDouble()
            is Valor.Decimal -> v.v
            else -> 0.0
        }
    }

    private fun numeroParaValor(referencia: Double, novo: Double): Valor {
        return if (referencia == referencia.toLong().toDouble() && novo == novo.toLong().toDouble()) {
            Valor.Inteiro(novo.toLong())
        } else {
            Valor.Decimal(novo)
        }
    }

    // -------------------------------------------------------------
    // Operadores binários
    // -------------------------------------------------------------

    private fun aplicarOperadorBinario(op: String, esq: Valor, dir: Valor, linha: Int): Valor {
        // Concatenação de texto com '+'
        if (op == "+" && (esq is Valor.Texto || dir is Valor.Texto)) {
            return Valor.Texto(formatarValor(esq) + formatarValor(dir))
        }

        if (esq is Valor.Inteiro && dir is Valor.Inteiro) {
            return when (op) {
                "+" -> Valor.Inteiro(esq.v + dir.v)
                "-" -> Valor.Inteiro(esq.v - dir.v)
                "*" -> Valor.Inteiro(esq.v * dir.v)
                "/" -> {
                    if (dir.v == 0L) throw ErroExecucao("divisão por zero", linha)
                    Valor.Inteiro(esq.v / dir.v)
                }
                "%" -> {
                    if (dir.v == 0L) throw ErroExecucao("divisão por zero (resto)", linha)
                    Valor.Inteiro(esq.v % dir.v)
                }
                "==" -> Valor.Logico(esq.v == dir.v)
                "!=" -> Valor.Logico(esq.v != dir.v)
                ">" -> Valor.Logico(esq.v > dir.v)
                "<" -> Valor.Logico(esq.v < dir.v)
                ">=" -> Valor.Logico(esq.v >= dir.v)
                "<=" -> Valor.Logico(esq.v <= dir.v)
                else -> Valor.Vazio
            }
        }

        // Qualquer combinação envolvendo decimal cai para double
        val e = paraDouble(esq)
        val d = paraDouble(dir)
        return when (op) {
            "+" -> Valor.Decimal(e + d)
            "-" -> Valor.Decimal(e - d)
            "*" -> Valor.Decimal(e * d)
            "/" -> Valor.Decimal(e / d)
            "==" -> Valor.Logico(e == d)
            "!=" -> Valor.Logico(e != d)
            ">" -> Valor.Logico(e > d)
            "<" -> Valor.Logico(e < d)
            ">=" -> Valor.Logico(e >= d)
            "<=" -> Valor.Logico(e <= d)
            else -> Valor.Vazio
        }
    }

    private fun paraDouble(v: Valor): Double = when (v) {
        is Valor.Inteiro -> v.v.toDouble()
        is Valor.Decimal -> v.v
        else -> 0.0
    }

    private fun verdadeiro(v: Valor): Boolean = when (v) {
        is Valor.Logico -> v.v
        is Valor.Inteiro -> v.v != 0L
        is Valor.Decimal -> v.v != 0.0
        else -> false
    }

    private fun valoresIguais(a: Valor, b: Valor): Boolean = when {
        a is Valor.Inteiro && b is Valor.Inteiro -> a.v == b.v
        a is Valor.Texto && b is Valor.Texto -> a.v == b.v
        a is Valor.Caractere && b is Valor.Caractere -> a.v == b.v
        else -> paraDouble(a) == paraDouble(b)
    }

    // -------------------------------------------------------------
    // Chamada de função (própria do usuário ou biblioteca padrão)
    // -------------------------------------------------------------

    private suspend fun chamarFuncao(chamada: ChamadaFuncao, ambienteChamador: Ambiente): Valor {
        val bibliotecaResultado = tentarChamarBiblioteca(chamada, ambienteChamador)
        if (bibliotecaResultado != null) return bibliotecaResultado

        val funcao = funcoes[chamada.nome]
        ?: throw ErroExecucao("função '${chamada.nome}' não encontrada", chamada.linha)

        // Declaração externa (corpo == null): função de lib real em C, sem implementação
        // em PortugolTipado — o Preview (interpretador Kotlin puro) não tem como executá-la de verdade,
        // já que não tem acesso à lib nativa. Erro claro em vez de rodar corpo vazio.
        val corpoFuncao = funcao.corpo
        ?: throw ErroExecucao(
            "a função '${chamada.nome}' é uma declaração externa (de lib incluída via 'inclua') " +
            "e não pode ser simulada no Preview — funciona só no C gerado de verdade",
            chamada.linha
        )

        val ambienteFuncao = Ambiente()
        // Guarda pares (nome do parâmetro, expressão de origem) para os casos
        // 'altere' em que a origem é campo/índice (não uma caixa compartilhável
        // diretamente) — escreve de volta manualmente ao final da chamada.
        val referenciasParaEscreverDeVolta = mutableListOf<Pair<String, No>>()

        for (i in funcao.parametros.indices) {
            val param = funcao.parametros[i]
            val argExpr = chamada.argumentos.getOrNull(i) ?: continue

            if (param.ehAlterar) {
                when (argExpr) {
                    is Identificador -> {
                        // Passagem por referência real: compartilha a MESMA caixa da variável original
                        val caixaOriginal = ambienteChamador.caixaDe(argExpr.nome)
                        if (caixaOriginal != null) {
                            ambienteFuncao.declararComCaixa(param.nome, caixaOriginal)
                        } else {
                            ambienteFuncao.declarar(param.nome, avaliar(argExpr, ambienteChamador))
                        }
                    }
                    is AcessoCampo, is AcessoIndice -> {
                        // Campo/índice não tem "caixa" própria: copia o valor atual,
                        // deixa a função mexer na cópia, e escreve de volta ao final.
                        ambienteFuncao.declarar(param.nome, avaliar(argExpr, ambienteChamador))
                        referenciasParaEscreverDeVolta.add(param.nome to argExpr)
                    }
                    else -> {
                        ambienteFuncao.declarar(param.nome, avaliar(argExpr, ambienteChamador))
                    }
                }
            } else {
                ambienteFuncao.declarar(param.nome, avaliar(argExpr, ambienteChamador))
            }
        }

        val resultado = try {
            for (comando in corpoFuncao) {
                executarComando(comando, ambienteFuncao)
            }
            Valor.Vazio
        } catch (sinal: SinalRetorno) {
            sinal.valor
        }

        // Propaga de volta o valor final dos parâmetros 'altere' que vieram
        // de campo/índice (struct.campo, array[i]).
        for ((nomeParam, origem) in referenciasParaEscreverDeVolta) {
            val valorFinal = ambienteFuncao.obter(nomeParam)
            atribuirValorEm(origem, valorFinal, ambienteChamador)
        }

        return resultado
    }

    /** Funções das bibliotecas padrão (math, string, stdlib...) simuladas em Kotlin.
    *  Retorna null se o nome não é uma função de biblioteca conhecida.
    *  Nomes em imperativo, sincronizados com Transpilador.funcoesTraduzidas. */
    private suspend fun tentarChamarBiblioteca(chamada: ChamadaFuncao, ambiente: Ambiente): Valor? {
        suspend fun arg(i: Int): Valor = avaliar(chamada.argumentos[i], ambiente)
        suspend fun argD(i: Int): Double = paraDouble(arg(i))

        if (LibGraficos.ehFuncao(chamada.nome)) {
            return chamarFuncaoGrafica(chamada, ambiente)
        }

        return when (chamada.nome) {
            "raiz" -> Valor.Decimal(sqrt(argD(0)))
            "raiz_cubica" -> Valor.Decimal(argD(0).pow(1.0 / 3.0))
            "potencia" -> Valor.Decimal(argD(0).pow(argD(1)))
            "absoluto" -> Valor.Decimal(abs(argD(0)))
            "absoluto_int" -> Valor.Inteiro(abs((arg(0) as? Valor.Inteiro)?.v ?: 0))
            "piso" -> Valor.Decimal(floor(argD(0)))
            "teto" -> Valor.Decimal(ceil(argD(0)))
            "arredondar" -> Valor.Decimal(round(argD(0)))
            "truncar" -> Valor.Decimal(truncate(argD(0)))
            "seno" -> Valor.Decimal(sin(argD(0)))
            "cosseno" -> Valor.Decimal(cos(argD(0)))
            "tangente" -> Valor.Decimal(tan(argD(0)))
            "exponencial" -> Valor.Decimal(exp(argD(0)))
            "logaritmo" -> Valor.Decimal(ln(argD(0)))
            "logaritmo10" -> Valor.Decimal(log10(argD(0)))
            "hipotenusa" -> Valor.Decimal(hypot(argD(0), argD(1)))
            "resto" -> Valor.Decimal(argD(0).rem(argD(1)))

            "tamanho" -> Valor.Inteiro(((arg(0) as? Valor.Texto)?.v?.length ?: 0).toLong())
            "compare" -> {
                val a = (arg(0) as? Valor.Texto)?.v ?: ""
                val b = (arg(1) as? Valor.Texto)?.v ?: ""
                Valor.Inteiro(a.compareTo(b).toLong())
            }
            "maiusculo" -> Valor.Caractere(((arg(0) as? Valor.Caractere)?.v ?: ' ').uppercaseChar())
            "minusculo" -> Valor.Caractere(((arg(0) as? Valor.Caractere)?.v ?: ' ').lowercaseChar())
            "eh_letra" -> Valor.Logico(((arg(0) as? Valor.Caractere)?.v ?: ' ').isLetter())
            "eh_numero" -> Valor.Logico(((arg(0) as? Valor.Caractere)?.v ?: ' ').isDigit())

            "converta_inteiro" -> Valor.Inteiro(((arg(0) as? Valor.Texto)?.v?.trim()?.toLongOrNull()) ?: 0)
            "converta_decimal" -> Valor.Decimal(((arg(0) as? Valor.Texto)?.v?.trim()?.toDoubleOrNull()) ?: 0.0)
            "aleatorio" -> Valor.Inteiro((0..32767).random().toLong()) // simula RAND_MAX comum do C

            else -> null
        }
    }

    // -------------------------------------------------------------
    // Biblioteca gráfica (LibGraficos): delega para a implementação de Graficos
    // recebida no construtor. Se nenhum host implementou (graficos == null),
    // erro claro em vez de crash silencioso.
    // -------------------------------------------------------------

    private suspend fun chamarFuncaoGrafica(chamada: ChamadaFuncao, ambiente: Ambiente): Valor {
        val g = graficos
            ?: throw ErroExecucao(
                "o programa usa a biblioteca gráfica ('inclua graficos'), mas esta plataforma não tem suporte gráfico",
                chamada.linha
            )
        val args = chamada.argumentos.map { valorParaAny(avaliar(it, ambiente)) }
        // renderize e aguarde devolvem o controlo a interface (o programa nao trava
        // o app), entao um laco de jogo que os chama a cada volta nao e "infinito".
        if (chamada.nome == "renderize" || chamada.nome == "aguarde") {
            passos = 0
        }
        val resultado = try {
            LibGraficos.chamar(chamada.nome, g, args)
        } catch (e: ErroGrafico) {
            throw ErroExecucao(e.message ?: "erro na biblioteca gráfica", chamada.linha)
        }
        return anyParaValor(resultado)
    }

    /** Valor (interno do interpretador) -> tipo simples do Kotlin que LibGraficos espera. */
    private fun valorParaAny(v: Valor): Any = when (v) {
        is Valor.Inteiro -> v.v
        is Valor.Decimal -> v.v
        is Valor.Texto -> v.v
        is Valor.Caractere -> v.v.toString()
        is Valor.Logico -> v.v
        else -> throw ErroExecucao("tipo de valor não suportado como argumento de função gráfica", 0)
    }

    /** Resultado de LibGraficos.chamar (Any? simples) -> Valor interno do interpretador. */
    private fun anyParaValor(resultado: Any?): Valor = when (resultado) {
        null -> Valor.Vazio
        is Long -> Valor.Inteiro(resultado)
        is Int -> Valor.Inteiro(resultado.toLong())
        is Double -> Valor.Decimal(resultado)
        is String -> Valor.Texto(resultado)
        is Boolean -> Valor.Logico(resultado)
        else -> Valor.Vazio
    }

    // -------------------------------------------------------------
    // Formatação de valor para saída (equivalente ao printf com %d/%f/%s/%c)
    // -------------------------------------------------------------

    /** Converte o texto digitado pelo usuário (via leia()) para o Valor do tipo
    *  declarado da variável, imitando o comportamento de scanf com %d/%f/%s/%c. */
    private fun converterTextoParaTipo(texto: String, tipo: Tipo?): Valor {
        val limpo = texto.trim()
        return when (tipo?.base) {
            TipoDado.INTEIRO, TipoDado.INTEIRO_LONGO, TipoDado.INTEIRO_CURTO,
            TipoDado.INTEIRO_POSITIVO, TipoDado.INTEIRO_GIGANTE, TipoDado.LOGICO ->
            Valor.Inteiro(limpo.toLongOrNull() ?: 0)
            TipoDado.REAL, TipoDado.DUPLO, TipoDado.DUPLO_LONGO ->
            Valor.Decimal(limpo.toDoubleOrNull() ?: 0.0)
            TipoDado.CARACTERE -> Valor.Caractere(limpo.firstOrNull() ?: ' ')
            TipoDado.TEXTO -> Valor.Texto(limpo)
            else -> Valor.Inteiro(limpo.toLongOrNull() ?: 0) // fallback seguro
        }
    }

    private fun formatarValor(v: Valor): String = when (v) {
        is Valor.Inteiro -> v.v.toString()
        is Valor.Decimal -> String.format("%.6f", v.v)
        is Valor.Texto -> v.v
        is Valor.Caractere -> v.v.toString()
        is Valor.Logico -> if (v.v) "1" else "0"
        is Valor.Array -> "[array]"
        is Valor.Struct -> "[struct ${v.nomeTipo}]"
        is Valor.Vazio -> ""
    }

    /** Converte escapes literais (\n, \t, \\, \") do código-fonte em caracteres reais,
    *  igual o compilador C faria ao processar uma string literal. */
    private fun resolverEscapes(texto: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < texto.length) {
            val c = texto[i]
            if (c == '\\' && i + 1 < texto.length) {
                when (texto[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    '\'' -> { sb.append('\''); i += 2 }
                    '0' -> { i += 2 } // caractere nulo, ignorado na exibição
                    'u' -> {
                        // \uXXXX -> caractere Unicode de 4 dígitos hex (usado para \u001B = ESC)
                        val fim = i + 6
                        if (fim <= texto.length) {
                            val hex = texto.substring(i + 2, fim)
                            val codigo = hex.toIntOrNull(16)
                            if (codigo != null) {
                                sb.append(codigo.toChar())
                                i = fim
                            } else {
                                sb.append(c); i += 1
                            }
                        } else {
                            sb.append(c); i += 1
                        }
                    }
                    'x' -> {
                        // \xHH -> caractere hexadecimal (1-2 dígitos), equivalente C
                        var j = i + 2
                        val inicioHex = j
                        while (j < texto.length && j < inicioHex + 2 && texto[j].isDigit_hex()) j++
                        val hex = texto.substring(inicioHex, j)
                        val codigo = hex.toIntOrNull(16)
                        if (codigo != null && hex.isNotEmpty()) {
                            sb.append(codigo.toChar())
                            i = j
                        } else {
                            sb.append(c); i += 1
                        }
                    }
                    in '0'..'7' -> {
                        // \NNN -> caractere octal (até 3 dígitos), ex: \033 = ESC
                        var j = i + 1
                        val inicioOital = j
                        while (j < texto.length && j < inicioOital + 3 && texto[j] in '0'..'7') j++
                        val octal = texto.substring(inicioOital, j)
                        val codigo = octal.toIntOrNull(8)
                        if (codigo != null) {
                            sb.append(codigo.toChar())
                            i = j
                        } else {
                            sb.append(c); i += 1
                        }
                    }
                    else -> { sb.append(c); i += 1 }
                }
            } else {
                sb.append(c); i += 1
            }
        }
        return sb.toString()
    }

    private fun Char.isDigit_hex(): Boolean = this.isDigit() || this in 'a'..'f' || this in 'A'..'F'
}
