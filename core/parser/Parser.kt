package co.adilson889.typec.parser

import co.adilson889.typec.ast.*
import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.erros.extrairLinha
import co.adilson889.typec.lexer.Token
import co.adilson889.typec.lexer.TipoToken

class ErroSintatico(mensagem: String, linha: Int, fonte: String, nomeArquivo: String = "main.port") :
ErroTypeC(mensagem, linha, extrairLinha(fonte, linha))

/** Resultado de 'parsearTolerante': o programa (pode estar incompleto) e todos os erros de sintaxe encontrados. */
class ResultadoParse(val programa: Programa, val erros: List<ErroTypeC>)

class Parser(
    private val tokens: List<Token>,
    private val fonte: String,
    private val nomeArquivo: String = "main.port" // usado nas mensagens de erro (ver ErroTypeC) — importante em módulos, onde o arquivo com erro nem sempre é o principal
) {

    private var posicao = 0

    private fun atual(): Token = tokens[posicao]
    private fun proximo(): Token = tokens.getOrElse(posicao + 1) { tokens.last() }
    private fun fimDosTokens(): Boolean = atual().tipo == TipoToken.FIM_ARQUIVO
    private fun verifica(tipo: TipoToken): Boolean = atual().tipo == tipo

    private fun avancar(): Token {
        val t = atual()
        if (!fimDosTokens()) posicao++
        return t
    }

    private fun consumir(tipo: TipoToken, mensagemErro: String): Token {
        if (verifica(tipo)) return avancar()
        throw ErroSintatico(mensagemErro, atual().linha, fonte, nomeArquivo)
    }

    private fun consumirSeExistir(tipo: TipoToken): Boolean {
        if (verifica(tipo)) { avancar(); return true }
        return false
    }

    private val erros = mutableListOf<ErroTypeC>()

    /** Como antes: devolve o programa ou lança o PRIMEIRO erro de sintaxe. */
    fun parsear(): Programa {
        val resultado = parsearTolerante()
        if (resultado.erros.isNotEmpty()) throw resultado.erros.first()
        return resultado.programa
    }

    /**
     * Analisa o codigo sem parar no primeiro erro: cada declaracao global e cada
     * comando com erro e registado e saltado, e a analise continua no comando
     * seguinte. Se houver erros, o programa devolvido esta incompleto e nao deve
     * ser validado nem executado.
     */
    fun parsearTolerante(): ResultadoParse {
        erros.clear()
        val programa = parsearPrograma()
        return ResultadoParse(programa, erros.toList())
    }

    private fun registrarErro(e: ErroTypeC) {
        if (erros.none { it.linha == e.linha && it.mensagem == e.mensagem }) erros.add(e)
    }

    private fun comecaDeclaracaoGlobal(tipo: TipoToken): Boolean =
        tipo == TipoToken.INCLUA || tipo == TipoToken.ESTRUTURA || tipo == TipoToken.FUNCAO || tipo == TipoToken.INICIO

    /**
     * Salta o resto da construcao com erro, incluindo o corpo entre chaves, para
     * nao gerar erros em cadeia. Para no primeiro simbolo de uma linha seguinte
     * (ou, em bloco, no '}' que fecha o bloco atual).
     */
    private fun sincronizar(linhaDoErro: Int, emBloco: Boolean) {
        var erroLinha = linhaDoErro
        var profundidade = 0
        while (!fimDosTokens()) {
            val t = atual()
            when {
                t.tipo == TipoToken.CHAVE_ESQ -> {
                    profundidade++
                    erroLinha = maxOf(erroLinha, t.linha)
                    avancar()
                }
                t.tipo == TipoToken.CHAVE_DIR -> {
                    if (profundidade == 0 && emBloco) return
                    if (profundidade > 0) profundidade--
                    erroLinha = maxOf(erroLinha, t.linha)
                    avancar()
                }
                profundidade == 0 && t.linha > erroLinha && (emBloco || comecaDeclaracaoGlobal(t.tipo)) -> return
                else -> avancar()
            }
        }
    }

    /**
     * Corre 'analise'. Se der erro de sintaxe, regista-o, salta a construcao e
     * devolve null. Garante sempre que a analise avanca (sem ciclos infinitos).
     */
    private fun <T> tentar(emBloco: Boolean, analise: () -> T): T? {
        val inicio = posicao
        return try {
            analise()
        } catch (e: ErroTypeC) {
            registrarErro(e)
            sincronizar(maxOf(e.linha, atual().linha), emBloco)
            if (posicao == inicio && !fimDosTokens() && !(emBloco && verifica(TipoToken.CHAVE_DIR))) avancar()
            null
        }
    }

    private fun parsearPrograma(): Programa {
        val linhaInicio = atual().linha
        val includes = mutableListOf<Inclua>()
        val declaracoesGlobais = mutableListOf<No>()

        while (!fimDosTokens() && !verifica(TipoToken.INICIO) && !ehFuncaoInicio()) {
            tentar(false) {
                when {
                    verifica(TipoToken.INCLUA) -> includes.add(parsearInclua())
                    verifica(TipoToken.ESTRUTURA) -> declaracoesGlobais.add(parsearStruct())
                    ehInicioDeFuncao() -> declaracoesGlobais.add(parsearFuncao())
                    else -> throw ErroSintatico(
                        "esperado 'inclua', 'estrutura', declaração de função ou 'inicio'",
                        atual().linha,
                        fonte,
                        nomeArquivo
                    )
                }
            }
        }

        val bloco = if (verifica(TipoToken.INICIO) || ehFuncaoInicio()) tentar(false) { parsearBlocoInicio() } else null

        return Programa(includes, declaracoesGlobais, bloco, linhaInicio)
    }

    /** Detecta se a partir daqui começa uma declaração de função: 'funcao' <nome> ( */
    private fun ehInicioDeFuncao(): Boolean {
        return atual().tipo == TipoToken.FUNCAO
    }

    /** Detecta a forma 'funcao inicio() { ... }', aceita como alternativa a 'inicio() { ... }' */
    private fun ehFuncaoInicio(): Boolean {
        return atual().tipo == TipoToken.FUNCAO && proximo().tipo == TipoToken.INICIO
    }

    private fun parsearInclua(): Inclua {
        val linha = atual().linha
        consumir(TipoToken.INCLUA, "esperado 'inclua'")

        if (verifica(TipoToken.TEXTO)) {
            val caminho = avancar().texto
            return Inclua(caminho, ehArquivoLocal = true, linha = linha)
        }

        val nome = StringBuilder(consumir(TipoToken.IDENTIFICADOR, "esperado nome da biblioteca ou caminho entre aspas após 'inclua'").texto)
        while (verifica(TipoToken.DIVIDIR)) {
            avancar()
            val parte = consumir(TipoToken.IDENTIFICADOR, "esperado nome após '/' em 'inclua'")
            nome.append("/").append(parte.texto)
        }

        return Inclua(nome.toString(), ehArquivoLocal = false, linha = linha)
    }

    private fun parsearBlocoInicio(): BlocoInicio {
        val linha = atual().linha
        consumirSeExistir(TipoToken.FUNCAO) // forma opcional: 'funcao inicio()'
        consumir(TipoToken.INICIO, "esperado 'inicio'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'inicio'")
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' após 'inicio('")
        consumir(TipoToken.CHAVE_ESQ, "esperado '{' após 'inicio()'")

        val comandos = mutableListOf<No>()
        while (!verifica(TipoToken.CHAVE_DIR) && !fimDosTokens()) {
            tentar(true) { comandos.add(parsearComando()) }
        }

        consumir(TipoToken.CHAVE_DIR, "esperado '}' para fechar 'inicio()'")
        return BlocoInicio(comandos, linha)
    }

    private fun ehTokenDeTipo(tipo: TipoToken): Boolean = tipo in setOf(
        TipoToken.INTEIRO, TipoToken.REAL, TipoToken.DUPLO, TipoToken.TEXTO_TIPO,
        TipoToken.CARACTERE_TIPO, TipoToken.LOGICO, TipoToken.VAZIO, TipoToken.TEMPO,
        TipoToken.ESTRUTURA
    )

    private fun parsearTipo(): Tipo {
        val tipoToken = atual()

        if (tipoToken.tipo == TipoToken.ESTRUTURA) {
            avancar()
            val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome da struct após 'estrutura'")
            var ehArray = false
            if (verifica(TipoToken.COLCHETE_ESQ) && proximo().tipo == TipoToken.COLCHETE_DIR) {
                avancar(); avancar()
                ehArray = true
            }
            return Tipo(TipoDado.VAZIO, ehArray, nomeStruct = nome.texto)
        }

        val base = when (tipoToken.tipo) {
            TipoToken.INTEIRO -> TipoDado.INTEIRO
            TipoToken.REAL -> TipoDado.REAL
            TipoToken.DUPLO -> TipoDado.DUPLO
            TipoToken.TEXTO_TIPO -> TipoDado.TEXTO
            TipoToken.CARACTERE_TIPO -> TipoDado.CARACTERE
            TipoToken.LOGICO -> TipoDado.LOGICO
            TipoToken.VAZIO -> TipoDado.VAZIO
            TipoToken.TEMPO -> TipoDado.TEMPO
            else -> throw ErroSintatico("esperado um tipo válido (inteiro, real, texto...)", tipoToken.linha, fonte, nomeArquivo)
        }
        avancar()

        var tipoFinal = base
        when (base) {
            TipoDado.INTEIRO -> when {
                verifica(TipoToken.LONGO) -> {
                    avancar()
                    tipoFinal = if (consumirSeExistir(TipoToken.LONGO)) TipoDado.INTEIRO_GIGANTE else TipoDado.INTEIRO_LONGO
                }
                verifica(TipoToken.CURTO) -> { avancar(); tipoFinal = TipoDado.INTEIRO_CURTO }
                verifica(TipoToken.POSITIVO) -> { avancar(); tipoFinal = TipoDado.INTEIRO_POSITIVO }
            }
            TipoDado.DUPLO -> if (verifica(TipoToken.LONGO)) { avancar(); tipoFinal = TipoDado.DUPLO_LONGO }
            else -> {}
        }

        var ehArray = false
        if (verifica(TipoToken.COLCHETE_ESQ) && proximo().tipo == TipoToken.COLCHETE_DIR) {
            avancar(); avancar()
            ehArray = true
        }

        var tipoResultado = Tipo(tipoFinal, ehArray)
        // Segundo "[]" seguido do primeiro -> array 2D (ex: "inteiro[][]"). Ver Array2D.kt
        tipoResultado = Array2D.tentarConsumirSegundoColchete(tipoResultado, ::atual, ::proximo, ::avancar)

        return tipoResultado
    }

    private fun parsearComando(): No {
        return when (atual().tipo) {
            TipoToken.IMPRIMIR -> parsearImprimir()
            TipoToken.LER -> parsearLer()
            TipoToken.SE -> parsearSe()
            TipoToken.ENQUANTO -> parsearEnquanto()
            TipoToken.FACA -> parsearFacaEnquanto()
            TipoToken.PARA -> parsearParaOuParaCada()
            TipoToken.ESCOLHER -> parsearEscolher()
            TipoToken.DISPENSAR -> { val l = atual().linha; avancar(); ComandoDispensar(l) }
            TipoToken.IGNORAR -> { val l = atual().linha; avancar(); ComandoIgnorar(l) }
            TipoToken.RETORNA -> parsearRetorna()
            TipoToken.CONST -> parsearDeclaracoes()
            else -> {
                if (ehTokenDeTipo(atual().tipo)) {
                    parsearDeclaracoes()
                } else {
                    parsearComandoExpressao()
                }
            }
        }
    }

    private fun parsearBloco(): List<No> {
        consumir(TipoToken.CHAVE_ESQ, "esperado '{'")
        val comandos = mutableListOf<No>()
        while (!verifica(TipoToken.CHAVE_DIR) && !fimDosTokens()) {
            tentar(true) { comandos.add(parsearComando()) }
        }
        consumir(TipoToken.CHAVE_DIR, "esperado '}'")
        return comandos
    }

    private fun parsearDeclaracaoVariavel(): DeclaracaoVariavel {
        val linha = atual().linha
        val ehConstante = consumirSeExistir(TipoToken.CONST)
        val tipoBase = parsearTipo()
        return parsearDeclarador(tipoBase, ehConstante, linha)
    }

    /** Uma ou mais variaveis do mesmo tipo: inteiro a = 0, b = 1 */
    private fun parsearDeclaracoes(): No {
        val linha = atual().linha
        val ehConstante = consumirSeExistir(TipoToken.CONST)
        val tipoBase = parsearTipo()
        if (verifica(TipoToken.CHAVE_ESQ)) return parsearGrupoDeclaracoes(tipoBase, ehConstante, linha)
        val primeira = parsearDeclarador(tipoBase, ehConstante, linha)
        if (!verifica(TipoToken.VIRGULA)) return primeira
        val lista = mutableListOf(primeira)
        while (consumirSeExistir(TipoToken.VIRGULA)) {
            lista.add(parsearDeclarador(tipoBase, ehConstante, linha))
        }
        return ComandoDeclaracoes(lista, linha)
    }

    /**
     * Grupo de declaracoes: o tipo escrito uma vez, varias variaveis entre chaves.
     *     const inteiro { A = 2  B = 3  C = 6 }
     *     inteiro { a, b, c }
     * As chaves NAO abrem um novo escopo: as variaveis pertencem ao bloco que contem o grupo.
     * A virgula entre os itens e opcional.
     */
    private fun parsearGrupoDeclaracoes(tipoBase: Tipo, ehConstante: Boolean, linha: Int): ComandoDeclaracoes {
        consumir(TipoToken.CHAVE_ESQ, "esperado '{' para abrir o grupo de declarações")
        val lista = mutableListOf<DeclaracaoVariavel>()
        while (!verifica(TipoToken.CHAVE_DIR) && !fimDosTokens()) {
            lista.add(parsearDeclarador(tipoBase, ehConstante, atual().linha))
            consumirSeExistir(TipoToken.VIRGULA)
        }
        if (lista.isEmpty()) {
            throw ErroSintatico("grupo de declarações vazio: escreva ao menos uma variável entre as chaves", linha, fonte, nomeArquivo)
        }
        consumir(TipoToken.CHAVE_DIR, "esperado '}' para fechar o grupo de declarações")
        return ComandoDeclaracoes(lista, linha)
    }

    private fun parsearDeclarador(tipoBase: Tipo, ehConstante: Boolean, linha: Int): DeclaracaoVariavel {
        var tipo = tipoBase
        val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome da variável")

        if (!tipo.ehArray && verifica(TipoToken.COLCHETE_ESQ)) {
            if (proximo().tipo == TipoToken.COLCHETE_DIR) {
                avancar(); avancar()
                tipo = tipo.copy(ehArray = true)
            } else if (proximo().tipo == TipoToken.NUMERO_INTEIRO) {
                avancar()
                val tamanhoToken = consumir(TipoToken.NUMERO_INTEIRO, "esperado um número para o tamanho do array (ex: turma[5])")
                consumir(TipoToken.COLCHETE_DIR, "esperado ']' para fechar tamanho do array")
                tipo = tipo.copy(ehArray = true, tamanhoFixo = tamanhoToken.texto.toInt())
                // Segundo "[m]" seguido do primeiro -> array 2D com tamanho explícito (ex: "matriz[2][2]"). Ver Array2D.kt
                tipo = Array2D.tentarConsumirSegundoTamanho(tipo, ::atual, ::proximo, ::avancar)
            }
        }

        var valorInicial: No? = null
        if (consumirSeExistir(TipoToken.IGUAL)) {
            valorInicial = if (verifica(TipoToken.CHAVE_ESQ)) {
                parsearArrayLiteral()
            } else {
                parsearExpressao()
            }
        }

        if (ehConstante && valorInicial == null) {
            throw ErroSintatico("'const' precisa de um valor inicial (ex: const inteiro ${nome.texto} = 0)", linha, fonte, nomeArquivo)
        }

        return DeclaracaoVariavel(tipo, nome.texto, valorInicial, linha, ehConstante)
    }

    private fun parsearArrayLiteral(): ArrayLiteral {
        val linha = atual().linha
        consumir(TipoToken.CHAVE_ESQ, "esperado '{' para iniciar valores do array")
        val valores = mutableListOf<No>()
        if (!verifica(TipoToken.CHAVE_DIR)) {
            valores.add(parsearValorDeInicializador())
            while (consumirSeExistir(TipoToken.VIRGULA)) {
                valores.add(parsearValorDeInicializador())
            }
        }
        consumir(TipoToken.CHAVE_DIR, "esperado '}' para fechar valores do array")
        return ArrayLiteral(valores, linha)
    }

    private fun parsearValorDeInicializador(): No {
        return if (verifica(TipoToken.CHAVE_ESQ)) {
            parsearArrayLiteral()
        } else {
            parsearExpressao()
        }
    }

    private fun parsearImprimir(): ComandoImprimir {
        val linha = atual().linha
        consumir(TipoToken.IMPRIMIR, "esperado 'escreva'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'escreva'")
        val argumentos = mutableListOf<No>()
        if (!verifica(TipoToken.PARENTESE_DIR)) {
            argumentos.add(parsearExpressao())
            while (consumirSeExistir(TipoToken.VIRGULA)) {
                argumentos.add(parsearExpressao())
            }
        }
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar 'escreva'")
        return ComandoImprimir(argumentos, linha)
    }

    private fun parsearLer(): ComandoLer {
        val linha = atual().linha
        consumir(TipoToken.LER, "esperado 'leia'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'leia'")
        val alvo = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar 'leia'")
        return ComandoLer(alvo, linha)
    }

    private fun parsearRetorna(): ComandoRetorna {
        val linha = atual().linha
        consumir(TipoToken.RETORNA, "esperado 'retorne'")
        val podeTerValor = !verifica(TipoToken.CHAVE_DIR)
        val valor = if (podeTerValor) parsearExpressao() else null
        return ComandoRetorna(valor, linha)
    }

    private fun parsearSe(): ComandoSe {
        val linha = atual().linha
        consumir(TipoToken.SE, "esperado 'se'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'se'")
        val condicao = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar condição do 'se'")
        val entao = parsearBloco()

        val senaoSeLista = mutableListOf<Par<No, List<No>>>()
        var senaoBloco: List<No>? = null

        while (verifica(TipoToken.SENAO)) {
            avancar()
            if (verifica(TipoToken.SE)) {
                avancar()
                consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'senao se'")
                val condicaoSenaoSe = parsearExpressao()
                consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar condição do 'senao se'")
                val blocoSenaoSe = parsearBloco()
                senaoSeLista.add(Par(condicaoSenaoSe, blocoSenaoSe))
            } else {
                senaoBloco = parsearBloco()
                break
            }
        }

        return ComandoSe(condicao, entao, senaoSeLista, senaoBloco, linha)
    }

    private fun parsearEnquanto(): ComandoEnquanto {
        val linha = atual().linha
        consumir(TipoToken.ENQUANTO, "esperado 'enquanto'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'enquanto'")
        val condicao = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar condição do 'enquanto'")
        val corpo = parsearBloco()
        return ComandoEnquanto(condicao, corpo, linha)
    }

    private fun parsearFacaEnquanto(): ComandoFacaEnquanto {
        val linha = atual().linha
        consumir(TipoToken.FACA, "esperado 'faca'")
        val corpo = parsearBloco()
        consumir(TipoToken.ENQUANTO, "esperado 'enquanto' depois do bloco do 'faca'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'enquanto'")
        val condicao = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar condição do 'faca ... enquanto'")
        return ComandoFacaEnquanto(corpo, condicao, linha)
    }

    private fun parsearParaOuParaCada(): No {
        val linha = atual().linha
        consumir(TipoToken.PARA, "esperado 'para'")

        if (verifica(TipoToken.CADA)) {
            return parsearParaCadaResto(linha)
        }

        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'para'")

        val inicializacao: No? = if (!verifica(TipoToken.PONTO_E_VIRGULA)) {
            if (verifica(TipoToken.CONST)) {
                throw ErroSintatico("'const' não pode ser usado na inicialização do 'para' (a variável de controle muda a cada volta)", atual().linha, fonte, nomeArquivo)
            }
            if (ehTokenDeTipo(atual().tipo)) parsearDeclaracaoVariavel()
            else parsearAtribuicaoOuExpressaoComando()
        } else null
        consumir(TipoToken.PONTO_E_VIRGULA, "esperado ';' após inicialização do 'para'")

        val condicao: No? = if (!verifica(TipoToken.PONTO_E_VIRGULA)) parsearExpressao() else null
        consumir(TipoToken.PONTO_E_VIRGULA, "esperado ';' após condição do 'para'")

        val incremento: No? = if (!verifica(TipoToken.PARENTESE_DIR)) parsearAtribuicaoOuExpressaoComando() else null

        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar cabeçalho do 'para'")
        val corpo = parsearBloco()

        return ComandoPara(inicializacao, condicao, incremento, corpo, linha)
    }

    private fun parsearParaCadaResto(linha: Int): ComandoParaCada {
        consumir(TipoToken.CADA, "esperado 'cada' após 'para'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'para cada'")
        val tipo = parsearTipo()
        val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome da variável em 'para cada'")
        consumir(TipoToken.EM, "esperado 'em' em 'para cada (tipo nome em array)'")
        val array = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar 'para cada'")
        val corpo = parsearBloco()
        return ComandoParaCada(tipo, nome.texto, array, corpo, linha)
    }

    private fun parsearEscolher(): ComandoEscolher {
        val linha = atual().linha
        consumir(TipoToken.ESCOLHER, "esperado 'escolha'")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após 'escolha'")
        val valor = parsearExpressao()
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar 'escolha'")
        consumir(TipoToken.CHAVE_ESQ, "esperado '{' para iniciar corpo do 'escolha'")

        val casos = mutableListOf<CasoEscolher>()
        var padrao: List<No>? = null

        while (!verifica(TipoToken.CHAVE_DIR) && !fimDosTokens()) {
            when {
                verifica(TipoToken.CASO) -> {
                    val linhaCaso = atual().linha
                    avancar()
                    val valorCaso = parsearExpressao()
                    consumir(TipoToken.DOIS_PONTOS, "esperado ':' após valor do 'caso'")
                    val corpo = mutableListOf<No>()
                    while (!verifica(TipoToken.CASO) && !verifica(TipoToken.PADRAO) && !verifica(TipoToken.CHAVE_DIR)) {
                        corpo.add(parsearComando())
                    }
                    casos.add(CasoEscolher(valorCaso, corpo, linhaCaso))
                }
                verifica(TipoToken.PADRAO) -> {
                    avancar()
                    consumir(TipoToken.DOIS_PONTOS, "esperado ':' após 'casocontrario'")
                    val corpo = mutableListOf<No>()
                    while (!verifica(TipoToken.CASO) && !verifica(TipoToken.PADRAO) && !verifica(TipoToken.CHAVE_DIR)) {
                        corpo.add(parsearComando())
                    }
                    padrao = corpo
                }
                else -> throw ErroSintatico("esperado 'caso' ou 'casocontrario' dentro de 'escolha'", atual().linha, fonte, nomeArquivo)
            }
        }

        consumir(TipoToken.CHAVE_DIR, "esperado '}' para fechar 'escolha'")
        return ComandoEscolher(valor, casos, padrao, linha)
    }

    // -------------------------------------------------------------
    // Funções — NOVO FORMATO: funcao nome(parametros): tipoRetorno { corpo }
    // (antes era: tipoRetorno funcao nome(parametros) { corpo })
    // Motivo: leitura mais rápida — nome da função logo após a palavra-chave,
    // sem precisar "atravessar" o tipo primeiro. 'estrutura' e variáveis
    // continuam tipo-primeiro; a mudança é só para a declaração de função.
    // -------------------------------------------------------------

    private fun parsearFuncao(): DeclaracaoFuncao {
        val linha = atual().linha
        consumir(TipoToken.FUNCAO, "esperado 'funcao'")
        val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome da função")
        consumir(TipoToken.PARENTESE_ESQ, "esperado '(' após nome da função")

        val parametros = mutableListOf<Parametro>()
        if (!verifica(TipoToken.PARENTESE_DIR)) {
            parametros.add(parsearParametro())
            while (consumirSeExistir(TipoToken.VIRGULA)) {
                parametros.add(parsearParametro())
            }
        }
        consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar parâmetros da função")

        consumir(TipoToken.DOIS_PONTOS, "esperado ':' seguido do tipo de retorno após os parâmetros da função (ex: funcao soma(inteiro a, inteiro b): inteiro)")
        val tipoRetorno = parsearTipo()

        // Sem '{' após o tipo de retorno: declaração externa (função de lib incluída via 'inclua',
        // sem implementação em TypeC — só registra assinatura/tipo para inferência).
        // Opcionalmente pode ter '= NomeReal' (alias): o nome declarado é o apelido usado
        // no código TypeC, e NomeReal é o nome exato chamado no C gerado.
        if (verifica(TipoToken.CHAVE_ESQ)) {
            val corpo = parsearBloco()
            return DeclaracaoFuncao(tipoRetorno, nome.texto, parametros, corpo, null, linha)
        }

        val nomeReal = if (consumirSeExistir(TipoToken.IGUAL)) {
            consumir(TipoToken.IDENTIFICADOR, "esperado nome real da função após '=' (ex: funcao apelido(...): tipo = NomeRealDaLib)").texto
        } else null

        return DeclaracaoFuncao(tipoRetorno, nome.texto, parametros, null, nomeReal, linha)
    }

    private fun parsearParametro(): Parametro {
        val linha = atual().linha
        val ehConstante = consumirSeExistir(TipoToken.CONST)
        if (ehConstante && verifica(TipoToken.ALTERAR)) {
            throw ErroSintatico("'const' e 'alterar' não podem ser usados juntos no mesmo parâmetro", linha, fonte, nomeArquivo)
        }
        val ehAlterar = consumirSeExistir(TipoToken.ALTERAR)
        if (ehAlterar && verifica(TipoToken.CONST)) {
            throw ErroSintatico("'const' e 'alterar' não podem ser usados juntos no mesmo parâmetro", linha, fonte, nomeArquivo)
        }
        val tipo = parsearTipo()
        val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome do parâmetro")
        return Parametro(tipo, nome.texto, ehAlterar, linha, ehConstante)
    }

    private fun parsearStruct(): DeclaracaoStruct {
        val linha = atual().linha
        consumir(TipoToken.ESTRUTURA, "esperado 'estrutura'")
        val nome = consumir(TipoToken.IDENTIFICADOR, "esperado nome da struct")
        consumir(TipoToken.CHAVE_ESQ, "esperado '{' após nome da struct")

        val campos = mutableListOf<CampoStruct>()
        while (!verifica(TipoToken.CHAVE_DIR) && !fimDosTokens()) {
            val linhaCampo = atual().linha
            var tipoCampo = parsearTipo()
            val nomeCampo = consumir(TipoToken.IDENTIFICADOR, "esperado nome do campo da struct")

            if (!tipoCampo.ehArray && verifica(TipoToken.COLCHETE_ESQ)) {
                avancar()
                val tamanhoToken = consumir(TipoToken.NUMERO_INTEIRO, "esperado um número para o tamanho do array dentro da struct (ex: notas[10])")
                consumir(TipoToken.COLCHETE_DIR, "esperado ']' para fechar tamanho do array")
                tipoCampo = tipoCampo.copy(ehArray = true, tamanhoFixo = tamanhoToken.texto.toInt())
            }

            campos.add(CampoStruct(tipoCampo, nomeCampo.texto, linhaCampo))
        }

        consumir(TipoToken.CHAVE_DIR, "esperado '}' para fechar struct")
        return DeclaracaoStruct(nome.texto, campos, linha)
    }

    private fun parsearComandoExpressao(): No {
        val comando = parsearAtribuicaoOuExpressaoComando()
        return comando
    }

    private fun parsearAtribuicaoOuExpressaoComando(): No {
        val linha = atual().linha
        val alvo = parsearExpressao()

        val operadoresAtribuicao = setOf(
            TipoToken.IGUAL, TipoToken.MAIS_IGUAL, TipoToken.MENOS_IGUAL,
            TipoToken.VEZES_IGUAL, TipoToken.DIVIDIR_IGUAL, TipoToken.RESTO_IGUAL
        )

        return when {
            atual().tipo in operadoresAtribuicao -> {
                val operador = avancar().texto
                val valor = parsearExpressao()
                Atribuicao(alvo, operador, valor, linha)
            }
            verifica(TipoToken.INCREMENTO) || verifica(TipoToken.DECREMENTO) -> {
                val operador = avancar().texto
                IncrementoDecremento(alvo, operador, linha)
            }
            else -> ExpressaoComando(alvo, linha)
        }
    }

    private fun parsearExpressao(): No = parsearOu()

    private fun parsearOu(): No {
        var esquerda = parsearE()
        while (verifica(TipoToken.OU_LOGICO)) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearE()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearE(): No {
        var esquerda = parsearIgualdade()
        while (verifica(TipoToken.E_LOGICO)) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearIgualdade()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearIgualdade(): No {
        var esquerda = parsearComparacao()
        while (verifica(TipoToken.IGUAL_IGUAL) || verifica(TipoToken.DIFERENTE)) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearComparacao()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearComparacao(): No {
        var esquerda = parsearAdicaoSubtracao()
        val operadoresComparacao = setOf(
            TipoToken.MAIOR, TipoToken.MENOR, TipoToken.MAIOR_IGUAL, TipoToken.MENOR_IGUAL
        )
        while (atual().tipo in operadoresComparacao) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearAdicaoSubtracao()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearAdicaoSubtracao(): No {
        var esquerda = parsearMultiplicacaoDivisao()
        while (verifica(TipoToken.MAIS) || verifica(TipoToken.MENOS)) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearMultiplicacaoDivisao()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearMultiplicacaoDivisao(): No {
        var esquerda = parsearUnario()
        val operadores = setOf(TipoToken.VEZES, TipoToken.DIVIDIR, TipoToken.RESTO)
        while (atual().tipo in operadores) {
            val linha = atual().linha
            val op = avancar().texto
            val direita = parsearUnario()
            esquerda = OperacaoBinaria(esquerda, op, direita, linha)
        }
        return esquerda
    }

    private fun parsearUnario(): No {
        if (verifica(TipoToken.NEGACAO) || verifica(TipoToken.MENOS)) {
            val linha = atual().linha
            val op = avancar().texto
            val operando = parsearUnario()
            return OperacaoUnaria(op, operando, linha)
        }
        return parsearPosfixo()
    }

    private fun parsearPosfixo(): No {
        var expressao = parsearPrimario()

        while (true) {
            expressao = when {
                verifica(TipoToken.COLCHETE_ESQ) -> {
                    val linha = atual().linha
                    avancar()
                    val indice = parsearExpressao()
                    consumir(TipoToken.COLCHETE_DIR, "esperado ']' para fechar acesso a índice")
                    AcessoIndice(expressao, indice, linha)
                }
                verifica(TipoToken.PONTO) -> {
                    val linha = atual().linha
                    avancar()
                    val campo = consumir(TipoToken.IDENTIFICADOR, "esperado nome do campo após '.'")
                    AcessoCampo(expressao, campo.texto, linha)
                }
                else -> return expressao
            }
        }
    }

    private fun parsearPrimario(): No {
        val token = atual()
        return when (token.tipo) {
            TipoToken.NUMERO_INTEIRO -> { avancar(); Numero(token.texto, TipoDado.INTEIRO, token.linha) }
            TipoToken.NUMERO_DECIMAL -> { avancar(); Numero(token.texto, TipoDado.DUPLO, token.linha) }
            TipoToken.TEXTO -> { avancar(); Texto(token.texto, token.linha) }
            TipoToken.CARACTERE -> { avancar(); Caractere(token.texto[0], token.linha) }
            TipoToken.VERDADEIRO -> { avancar(); Logico(true, token.linha) }
            TipoToken.FALSO -> { avancar(); Logico(false, token.linha) }
            TipoToken.IDENTIFICADOR -> parsearIdentificadorOuChamada()
            TipoToken.PARENTESE_ESQ -> {
                avancar()
                val expr = parsearExpressao()
                consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar expressão")
                expr
            }
            else -> throw ErroSintatico("expressão inválida, token inesperado: '${token.texto}'", token.linha, fonte, nomeArquivo)
        }
    }

    private fun parsearIdentificadorOuChamada(): No {
        val token = consumir(TipoToken.IDENTIFICADOR, "esperado identificador")
        if (verifica(TipoToken.PARENTESE_ESQ)) {
            avancar()
            val argumentos = mutableListOf<No>()
            if (!verifica(TipoToken.PARENTESE_DIR)) {
                argumentos.add(parsearExpressao())
                while (consumirSeExistir(TipoToken.VIRGULA)) {
                    argumentos.add(parsearExpressao())
                }
            }
            consumir(TipoToken.PARENTESE_DIR, "esperado ')' para fechar chamada de '${token.texto}'")
            return ChamadaFuncao(token.texto, argumentos, token.linha)
        }
        return Identificador(token.texto, token.linha)
    }
}
