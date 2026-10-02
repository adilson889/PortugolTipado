package co.adilson889.typec.validador

import co.adilson889.typec.ast.*
import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.erros.extrairLinha
import co.adilson889.typec.graficos.LibGraficos

/**
* Validação semântica leve, feita em Kotlin puro (sem NDK, sem gcc).
* Não é tão completa quanto um compilador C real, mas cobre os erros
* mais comuns:
*  - variável usada sem ter sido declarada antes
*  - função chamada que não existe (nem é da biblioteca padrão)
*  - atribuição com tipo obviamente incompatível (ex: inteiro = texto)
*  - alteração de variável, array ou parâmetro declarado com 'const'
*    (atribuição, ++/--, leia, ou passagem para parâmetro 'alterar')
*
* Percorre a AST antes da transpilação. Erros de sintaxe (chaves, tipos
* mal formados etc) já são pegos pelo Parser antes de chegar aqui.
*/
/** 'palavra' e a palavra exata a marcar no editor (opcional). */
class ErroValidacao(mensagem: String, linha: Int, fonte: String, palavra: String? = null, sugestao: String? = null) :
ErroTypeC(mensagem, linha, extrairLinha(fonte, linha), palavra, sugestao)

class Validador(
    private val fonte: String,
    private val nomeArquivo: String = "main.port" // usado nas mensagens de erro (ver ErroTypeC) — importante em módulos, onde o arquivo com erro nem sempre é o principal
) {

    private val escopos = mutableListOf<MutableMap<String, Tipo>>()
    // Em paralelo a 'escopos': nomes declarados com 'const' em cada escopo
    private val escoposConst = mutableListOf<MutableSet<String>>()
    // Assinaturas das funções declaradas (para saber quais parâmetros são 'alterar')
    private val assinaturas = mutableMapOf<String, List<Parametro>>()
    private val funcoesDeclaradas = mutableSetOf<String>()
    private val structsDeclaradas = mutableSetOf<String>()

    // Funções da biblioteca padrão (mesmas 6 libs da doc) - nomes válidos
    // mesmo sem 'funcao' própria declarada pelo usuário.
    // Nomes no imperativo, sincronizados com Transpilador.funcoesTraduzidas.
    // Funções externas de libs de terceiros (ex: SDL) não entram aqui — são
    // declaradas pelo próprio dev via 'funcao apelido(...): tipo = NomeReal'
    // (sem corpo), e já ficam cobertas por funcoesDeclaradas.
    private val funcoesBiblioteca: Set<String> = setOf(
        // math
        "raiz", "raiz_cubica", "potencia", "absoluto", "piso", "teto",
        "arredondar", "truncar", "seno", "cosseno", "tg",
        "arcsen", "arcos", "arctg", "arctg2",
        "senh", "cosh", "tgh",
        "exponencial", "logaritmo", "logaritmo10", "logaritmo2",
        "resto", "hipotenusa", "copiar_sinal", "eh_nan", "eh_infinito",
        "eh_finito", "absoluto_int",
        // string
        "tamanho", "copie", "junte", "compare",
        // stdlib
        "converta_inteiro", "converta_decimal", "converta_longo",
        "aleatorio", "semente",
        "aloque", "aloque_zerado", "realoque", "libere",
        "sair", "aborte", "variavel_ambiente", "execute",
        "ordene", "busque",
        // stdio
        "leia_caractere", "escreva_caractere", "escreva_linha", "leia_linha",
        // ctype
        "eh_letra", "eh_numero", "eh_letra_ou_numero", "eh_espaco",
        "eh_maiuscula", "eh_minuscula", "eh_pontuacao", "eh_controle",
        "eh_imprimivel", "eh_grafico", "eh_hexadecimal", "eh_branco",
        "maiusculo", "minusculo",
        // time
        "agora", "relogio", "diferenca_tempo", "data_texto"
    )

    // true quando o programa tem 'inclua graficos': habilita os nomes de LibGraficos
    // como funcoes/constantes validas (sem isso, 'desenhe_linha' etc. seguem desconhecidos,
    // igual qualquer outro nome nao declarado)
    private var usaGraficos = false

    private val erros = mutableListOf<ErroTypeC>()

    /** Como antes: lanca o PRIMEIRO erro encontrado (ou nada, se o programa estiver certo). */
    fun validar(programa: Programa) {
        val todos = validarTodos(programa)
        if (todos.isNotEmpty()) throw todos.first()
    }

    /** Executa 'bloco'; se der erro, regista-o e repoe os escopos para a validacao continuar. */
    private fun registrandoErros(bloco: () -> Unit) {
        val profundidade = escopos.size
        try {
            bloco()
        } catch (e: ErroTypeC) {
            if (erros.none { it.linha == e.linha && it.mensagem == e.mensagem }) erros.add(e)
            while (escopos.size > profundidade) sairEscopo()
        }
    }

    /** Valida o programa inteiro e devolve TODOS os erros, por ordem de linha. */
    fun validarTodos(programa: Programa): List<ErroTypeC> {
        erros.clear()
        validarPrograma(programa)
        return erros.sortedBy { it.linha }
    }

    private fun validarPrograma(programa: Programa) {
        escopos.clear()
        escoposConst.clear()
        assinaturas.clear()
        funcoesDeclaradas.clear()
        structsDeclaradas.clear()
        usaGraficos = programa.includes.any { !it.ehArquivoLocal && it.nomeLib == LibGraficos.NOME_INCLUDE }

        // Pré-registra tudo que é declarado globalmente, em qualquer ordem
        for (decl in programa.declaracoesGlobais) {
            registrandoErros {
            when (decl) {
                is DeclaracaoFuncao -> {
                    if (usaGraficos && LibGraficos.existe(decl.nome)) {
                        throw ErroValidacao(
                            "'${decl.nome}' já é uma função da biblioteca gráfica (inclua graficos) e não pode ser redeclarada",
                            decl.linha,
                            fonte,
                            decl.nome
                        )
                    }
                    funcoesDeclaradas.add(decl.nome)
                    assinaturas[decl.nome] = decl.parametros
                }
                is DeclaracaoStruct -> structsDeclaradas.add(decl.nome)
                else -> {}
            }
            }
        }

        for (decl in programa.declaracoesGlobais) {
            if (decl is DeclaracaoFuncao) registrandoErros { validarFuncao(decl) }
        }

        // Um módulo (arquivo incluído via 'inclua "caminho"') normalmente não tem
        // 'inicio()' — é só biblioteca (structs, funções). Só valida o bloco se existir.
        val bloco = programa.inicio
        if (bloco != null) {
            entrarEscopo()
            for (comando in bloco.comandos) {
                validarComando(comando)
            }
            sairEscopo()
        }
    }

    // -------------------------------------------------------------
    // Escopos (pilha simples de variáveis visíveis)
    // -------------------------------------------------------------

    private fun entrarEscopo() {
        escopos.add(mutableMapOf())
        escoposConst.add(mutableSetOf())
    }

    private fun sairEscopo() {
        escopos.removeAt(escopos.size - 1)
        escoposConst.removeAt(escoposConst.size - 1)
    }

    private fun declararVariavel(nome: String, tipo: Tipo, ehConstante: Boolean = false) {
        escopos.last()[nome] = tipo
        if (ehConstante) escoposConst.last().add(nome) else escoposConst.last().remove(nome)
    }

    /** Procura o nome do escopo mais interno que o declara e diz se foi declarado com 'const'. */
    private fun variavelEhConstante(nome: String): Boolean {
        for (i in escopos.indices.reversed()) {
            if (escopos[i].containsKey(nome)) return nome in escoposConst[i]
        }
        return false
    }

    /** Nome da variável na raiz de um alvo: x, x[i] e x.campo apontam todos para 'x'. */
    private fun nomeRaiz(alvo: No): String? = when (alvo) {
        is Identificador -> alvo.nome
        is AcessoIndice -> nomeRaiz(alvo.array)
        is AcessoCampo -> nomeRaiz(alvo.objeto)
        else -> null
    }

    /** Em C, alterar algo declarado 'const' não compila. Aqui o erro sai antes, em português. */
    private fun validarAlvoNaoConstante(alvo: No, linha: Int) {
        val nome = nomeRaiz(alvo) ?: return
        if (variavelEhConstante(nome)) {
            throw ErroValidacao(
                "não é possível alterar '$nome': foi declarada como 'const'",
                linha,
                fonte,
                nome
            )
        }
    }

    private fun tipoDaVariavel(nome: String): Tipo? {
        for (i in escopos.indices.reversed()) {
            escopos[i][nome]?.let { return it }
        }
        return null
    }

    private fun variavelDeclarada(nome: String): Boolean = tipoDaVariavel(nome) != null

    // -------------------------------------------------------------
    // Funções
    // -------------------------------------------------------------

    private fun validarFuncao(funcao: DeclaracaoFuncao) {
        // Declaração externa (sem implementação em PortugolTipado, já existe em lib incluída) — nada a validar
        val corpo = funcao.corpo ?: return
        entrarEscopo()
        for (param in funcao.parametros) {
            declararVariavel(param.nome, param.tipo, param.ehConstante)
        }
        for (comando in corpo) {
            validarComando(comando)
        }
        sairEscopo()
    }

    // -------------------------------------------------------------
    // Comandos
    // -------------------------------------------------------------

    /** Cada comando e validado em separado: um erro nao impede de ver os seguintes. */
    private fun validarComando(comando: No) {
        registrandoErros { validarComandoSimples(comando) }
    }

    private fun validarComandoSimples(comando: No) {
        when (comando) {
            is DeclaracaoVariavel -> {
                // A variavel fica declarada mesmo que o valor inicial tenha erro,
                // para os usos seguintes nao gerarem 'nao foi declarada' em cadeia.
                try {
                    comando.valorInicial?.let { validarExpressaoUsada(it) }
                    if (comando.valorInicial != null) {
                        validarCompatibilidadeAtribuicao(comando.tipo, comando.valorInicial, comando.linha)
                    }
                } finally {
                    declararVariavel(comando.nome, comando.tipo, comando.ehConstante)
                }
            }
            is ComandoImprimir -> comando.argumentos.forEach { validarExpressaoUsada(it) }
            is ComandoLer -> {
                validarExpressaoUsada(comando.alvo)
                validarAlvoNaoConstante(comando.alvo, comando.linha)
            }
            is ComandoRetorna -> comando.valor?.let { validarExpressaoUsada(it) }
            is ComandoSe -> {
                validarExpressaoUsada(comando.condicao)
                entrarEscopo(); comando.entao.forEach { validarComando(it) }; sairEscopo()
                for (par in comando.senaoSe) {
                    validarExpressaoUsada(par.primeiro)
                    entrarEscopo(); par.segundo.forEach { validarComando(it) }; sairEscopo()
                }
                comando.senao?.let { bloco ->
                    entrarEscopo(); bloco.forEach { validarComando(it) }; sairEscopo()
                }
            }
            is ComandoEnquanto -> {
                validarExpressaoUsada(comando.condicao)
                entrarEscopo(); comando.corpo.forEach { validarComando(it) }; sairEscopo()
            }
            is ComandoFacaEnquanto -> {
                entrarEscopo(); comando.corpo.forEach { validarComando(it) }; sairEscopo()
                validarExpressaoUsada(comando.condicao)
            }
            is ComandoDeclaracoes -> comando.declaracoes.forEach { validarComando(it) }
            is ComandoPara -> {
                entrarEscopo()
                comando.inicializacao?.let { validarComando(it) }
                comando.condicao?.let { validarExpressaoUsada(it) }
                comando.incremento?.let { validarComando(it) }
                comando.corpo.forEach { validarComando(it) }
                sairEscopo()
            }
            is ComandoParaCada -> {
                validarExpressaoUsada(comando.array)
                entrarEscopo()
                declararVariavel(comando.nomeElemento, comando.tipoElemento)
                comando.corpo.forEach { validarComando(it) }
                sairEscopo()
            }
            is ComandoEscolher -> {
                validarExpressaoUsada(comando.valor)
                comando.casos.forEach { caso ->
                    entrarEscopo(); caso.corpo.forEach { validarComando(it) }; sairEscopo()
                }
                comando.padrao?.let { bloco ->
                    entrarEscopo(); bloco.forEach { validarComando(it) }; sairEscopo()
                }
            }
            is Atribuicao -> {
                validarExpressaoUsada(comando.alvo)
                validarExpressaoUsada(comando.valor)
                validarAlvoNaoConstante(comando.alvo, comando.linha)
            }
            is IncrementoDecremento -> {
                validarExpressaoUsada(comando.alvo)
                validarAlvoNaoConstante(comando.alvo, comando.linha)
            }
            is ExpressaoComando -> validarExpressaoUsada(comando.expressao)
            is ComandoDispensar, is ComandoIgnorar -> {}
            else -> {}
        }
    }

    // -------------------------------------------------------------
    // Expressões: verifica se identificadores/funções usados existem
    // -------------------------------------------------------------

    private fun validarExpressaoUsada(expressao: No) {
        when (expressao) {
            is Identificador -> {
                // PI e E são constantes globais reconhecidas, não precisam declaração
                val ehConstanteGrafica = usaGraficos && LibGraficos.ehConstante(expressao.nome)
                if (expressao.nome !in setOf("PI", "E") && !ehConstanteGrafica && !variavelDeclarada(expressao.nome)) {
                    throw ErroValidacao(
                        "variável '${expressao.nome}' usada mas não foi declarada",
                        expressao.linha,
                        fonte,
                        expressao.nome
                    )
                }
            }
            is ChamadaFuncao -> {
                val funcaoGrafica = if (usaGraficos) LibGraficos.funcoes[expressao.nome] else null
                if (funcaoGrafica == null &&
                    expressao.nome !in funcoesDeclaradas &&
                    expressao.nome !in funcoesBiblioteca
                ) {
                    throw ErroValidacao(
                        "função '${expressao.nome}' usada mas não foi declarada",
                        expressao.linha,
                        fonte,
                        expressao.nome
                    )
                }
                if (funcaoGrafica != null && expressao.argumentos.size != funcaoGrafica.parametros.size) {
                    throw ErroValidacao(
                        "'${expressao.nome}' espera ${funcaoGrafica.parametros.size} argumento(s), mas recebeu ${expressao.argumentos.size}",
                        expressao.linha,
                        fonte,
                        expressao.nome
                    )
                }
                expressao.argumentos.forEach { validarExpressaoUsada(it) }
                validarArgumentosConstParaAlterar(expressao)
            }
            is OperacaoBinaria -> {
                validarExpressaoUsada(expressao.esquerda)
                validarExpressaoUsada(expressao.direita)
            }
            is OperacaoUnaria -> validarExpressaoUsada(expressao.operando)
            is AcessoIndice -> {
                validarExpressaoUsada(expressao.array)
                validarExpressaoUsada(expressao.indice)
            }
            is AcessoCampo -> validarExpressaoUsada(expressao.objeto)
            is ArrayLiteral -> expressao.valores.forEach { validarExpressaoUsada(it) }
            is Atribuicao -> {
                validarExpressaoUsada(expressao.alvo)
                validarExpressaoUsada(expressao.valor)
                validarAlvoNaoConstante(expressao.alvo, expressao.linha)
            }
            is IncrementoDecremento -> {
                validarExpressaoUsada(expressao.alvo)
                validarAlvoNaoConstante(expressao.alvo, expressao.linha)
            }
            else -> {} // Numero, Texto, Caractere, Logico não precisam checagem
        }
    }

    /** Passar uma variável 'const' para um parâmetro 'alterar' permitiria alterá-la por fora. */
    private fun validarArgumentosConstParaAlterar(chamada: ChamadaFuncao) {
        val parametros = assinaturas[chamada.nome] ?: return
        for (i in parametros.indices) {
            val param = parametros[i]
            if (!param.ehAlterar) continue
            val arg = chamada.argumentos.getOrNull(i) ?: continue
            val nome = nomeRaiz(arg) ?: continue
            if (variavelEhConstante(nome)) {
                throw ErroValidacao(
                    "'$nome' foi declarada como 'const' e não pode ser passada para o parâmetro '${param.nome}' (alterar) da função '${chamada.nome}'",
                    chamada.linha,
                    fonte,
                    nome
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Checagem de tipo simples (só casos óbvios, para não gerar falsos positivos)
    // -------------------------------------------------------------

    private fun validarCompatibilidadeAtribuicao(tipoAlvo: Tipo, valor: No, linha: Int) {
        // Só compara casos diretos e óbvios: literal contra tipo primitivo simples.
        // Evita falsos positivos em expressões complexas, chamadas de função, etc.
        val tipoValor = when (valor) {
            is Texto -> TipoDado.TEXTO
            is Numero -> if (valor.tipo == TipoDado.INTEIRO) TipoDado.INTEIRO else TipoDado.DUPLO
            is Caractere -> TipoDado.CARACTERE
            is Logico -> TipoDado.LOGICO
            else -> return // não arriscar checagem em expressões complexas
        }

        if (tipoAlvo.ehArray || tipoAlvo.nomeStruct != null) return // já tratado em outro lugar

        val incompativel = when (tipoAlvo.base) {
            TipoDado.TEXTO -> tipoValor != TipoDado.TEXTO
            TipoDado.CARACTERE -> tipoValor != TipoDado.CARACTERE
            TipoDado.INTEIRO, TipoDado.INTEIRO_LONGO, TipoDado.INTEIRO_CURTO,
            TipoDado.INTEIRO_POSITIVO, TipoDado.INTEIRO_GIGANTE, TipoDado.LOGICO ->
            tipoValor == TipoDado.TEXTO || tipoValor == TipoDado.CARACTERE
            TipoDado.REAL, TipoDado.DUPLO, TipoDado.DUPLO_LONGO ->
            tipoValor == TipoDado.TEXTO || tipoValor == TipoDado.CARACTERE
            else -> false
        }

        if (incompativel) {
            throw ErroValidacao(
                "tipo incompatível: não é possível atribuir um valor do tipo '$tipoValor' a uma variável do tipo '${tipoAlvo.base}'",
                linha,
                fonte
            )
        }
    }
}
