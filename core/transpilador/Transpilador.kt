package co.adilson889.typec.transpilador

import co.adilson889.typec.ast.*
import co.adilson889.typec.parser.Array2D
import co.adilson889.typec.graficos.LibGraficos

/**
* Resultado da transpilação de um único arquivo .port: o código .c
* (implementação) e o .h correspondente (protótipos + structs, com
* guardas de inclusão), para suportar modulação estilo C.
*
* Para um programa sem 'inclua "caminho/arquivo"' (só libs padrão),
* cabecalho ainda é gerado mas normalmente não é necessário — o
* consumidor decide se usa ou não.
*/
data class ResultadoTranspilacao(
    val codigoC: String,
    val codigoH: String,
    val nomeGuardaH: String,
    val requisitos: List<String> = emptyList() // ex: ["sdl2"] quando o programa usa 'inclua graficos'
)

/**
* Converte a AST do TypeC em código C legível.
* Responsabilidade única: tradução estrutural. Não faz verificação
* semântica de tipos (isso fica por conta do gcc -fsyntax-only,
* rodado depois pelo Validador).
*
* Suporte a módulos (ver ChangelogTypeC-Modulos.md):
*   inclua stdlib                -> lib padrão do C, sem aspas: #include <stdlib.h>
*   inclua "base/core/base_core" -> arquivo TypeC local, com aspas, sem extensão:
*                                    gera #include "base/core/base_core.h"
* O motor NÃO resolve/transpila recursivamente o arquivo incluído aqui —
* isso é responsabilidade de uma camada acima (resolvedor de módulos),
* que chama o Transpilador uma vez por arquivo .port do projeto.
*
* Suporte a declaração externa de função (ver conversa sobre libs externas):
*   funcao nome(tipo param, ...): tipo_retorno
*   (sem '{' — sem corpo) declara que a função já existe numa lib trazida
*   por 'inclua'. Não gera implementação nem protótipo no .c/.h (o header
*   real da lib já traz isso) — só registra assinatura/tipo de retorno
*   para inferência correta em escreva()/leia() quando a chamada é usada
*   direto numa expressão.
*/
/** Erro de transpilacao com mensagem em portugues, para o usuario corrigir o .port
 *  antes de tentar compilar o C gerado (em vez de o gcc falhar sem contexto depois). */
class ErroTranspilacao(mensagem: String) : Exception(mensagem)

class Transpilador(
    /** Nome do arquivo .port sendo transpilado (sem extensão), usado para nomear a guarda do .h.
    *   Ex: "base_core" -> guarda BASE_CORE_H. Default seguro para uso avulso (sem módulos). */
    private val nomeArquivo: String = "programa",
    /** Decide como o texto é guardado em C (arrays fixos) e gera os blocos de leitura inline. */
    private val memoria: MemoryManager = MemoryManager()
) {

    // Nome da variável -> Tipo (escopo simples, achatado; suficiente para
    // inferir especificador de printf/scanf e detectar parâmetros 'altere')
    private val tiposVariaveis = mutableMapOf<String, Tipo>()

    // Nome da função -> lista de parâmetros (para saber quais são 'altere' na chamada)
    private val assinaturasFuncoes = mutableMapOf<String, List<Parametro>>()

    // Nome da função -> tipo de retorno (para inferir %d/%f/etc quando a chamada é usada em imprimir/ler)
    private val retornosFuncoes = mutableMapOf<String, Tipo>()

    // Apelido (nome usado no TypeC) -> nome real da função em C, para declarações externas com alias
    // ('funcao apelido(...): tipo = NomeReal'). Checado antes de funcoesTraduzidas e do nome cru.
    private val aliasesExternos = mutableMapOf<String, String>()

    // Tipo de retorno das funções mais comuns das bibliotecas padrão (para inferência de printf)
    private val retornosFuncoesBiblioteca: Map<String, Tipo> = mapOf(
        "compare" to Tipo(TipoDado.INTEIRO), // strcmp -> int
        "converta_inteiro" to Tipo(TipoDado.INTEIRO),
        "converta_decimal" to Tipo(TipoDado.DUPLO),
        "converta_longo" to Tipo(TipoDado.INTEIRO_LONGO),
        "aleatorio" to Tipo(TipoDado.INTEIRO),
        "raiz" to Tipo(TipoDado.DUPLO), "raiz_cubica" to Tipo(TipoDado.DUPLO),
        "potencia" to Tipo(TipoDado.DUPLO), "absoluto" to Tipo(TipoDado.DUPLO),
        "piso" to Tipo(TipoDado.DUPLO), "teto" to Tipo(TipoDado.DUPLO),
        "arredondar" to Tipo(TipoDado.DUPLO), "truncar" to Tipo(TipoDado.DUPLO),
        "seno" to Tipo(TipoDado.DUPLO), "cosseno" to Tipo(TipoDado.DUPLO), "tg" to Tipo(TipoDado.DUPLO),
        "absoluto_int" to Tipo(TipoDado.INTEIRO),
        "eh_letra" to Tipo(TipoDado.LOGICO), "eh_numero" to Tipo(TipoDado.LOGICO),
        "eh_maiuscula" to Tipo(TipoDado.LOGICO), "eh_minuscula" to Tipo(TipoDado.LOGICO),
        "maiusculo" to Tipo(TipoDado.CARACTERE), "minusculo" to Tipo(TipoDado.CARACTERE),
        "leia_caractere" to Tipo(TipoDado.CARACTERE)
    )

    // Nome de campo de struct -> tipo, por nome de struct (para futuro suporte completo)
    private val structs = mutableMapOf<String, DeclaracaoStruct>()

    // 'escolha' sobre texto vira if/else if com strcmp (switch do C não aceita string):
    // marca que o .c precisa de <string.h> e numera a variável temporária de cada escolha
    private var usouStrcmp = false
    private var contadorEscolhaTexto = 0

    // Variáveis de texto que viraram buffer próprio (char nome[N]): nelas sizeof(nome) é o tamanho real
    private val nomesBufferTexto = mutableSetOf<String>()

    // Verdadeiro enquanto transpila o corpo do 'inicio' (o main de C devolve int)
    private var dentroDoInicio = false

    // Tabela de tradução de nomes de função das bibliotecas padrão (seções 5, 12, 13, 14, 16, 17 da doc)
    // Todos os nomes em imperativo, seguindo o mesmo padrão de escreva/leia/altere.
    private val funcoesTraduzidas: Map<String, String> = mapOf(
        // math
        "raiz" to "sqrt", "raiz_cubica" to "cbrt", "potencia" to "pow",
        "absoluto" to "fabs", "piso" to "floor", "teto" to "ceil",
        "arredondar" to "round", "truncar" to "trunc",
        "seno" to "sin", "cosseno" to "cos", "tg" to "tan",
        "arcsen" to "asin", "arcos" to "acos", "arctg" to "atan",
        "arctg2" to "atan2",
        "senh" to "sinh", "cosh" to "cosh", "tgh" to "tanh",
        "exponencial" to "exp", "logaritmo" to "log", "logaritmo10" to "log10",
        "logaritmo2" to "log2", "resto" to "fmod", "hipotenusa" to "hypot",
        "copiar_sinal" to "copysign", "eh_nan" to "isnan", "eh_infinito" to "isinf",
        "eh_finito" to "isfinite", "absoluto_int" to "abs",
        // string — verbos no imperativo
        "tamanho" to "strlen", "copie" to "strcpy", "junte" to "strcat", "compare" to "strcmp",
        // stdlib - conversão (verbo "converta" em vez de "para_")
        "converta_inteiro" to "atoi", "converta_decimal" to "atof", "converta_longo" to "atol",
        // stdlib - aleatorio
        "aleatorio" to "rand", "semente" to "srand",
        // stdlib - memoria (verbos no imperativo)
        "aloque" to "malloc", "aloque_zerado" to "calloc", "realoque" to "realloc", "libere" to "free",
        // stdlib - processo
        "sair" to "exit", "aborte" to "abort", "variavel_ambiente" to "getenv", "execute" to "system",
        // stdlib - busca/ordenacao (verbos no imperativo)
        "ordene" to "qsort", "busque" to "bsearch",
        // stdio — pares escreva_/leia_ para I/O de caractere e linha
        "leia_caractere" to "getchar", "escreva_caractere" to "putchar",
        "escreva_linha" to "puts", "leia_linha" to "fgets",
        // ctype
        "eh_letra" to "isalpha", "eh_numero" to "isdigit", "eh_letra_ou_numero" to "isalnum",
        "eh_espaco" to "isspace", "eh_maiuscula" to "isupper", "eh_minuscula" to "islower",
        "eh_pontuacao" to "ispunct", "eh_controle" to "iscntrl", "eh_imprimivel" to "isprint",
        "eh_grafico" to "isgraph", "eh_hexadecimal" to "isxdigit", "eh_branco" to "isblank",
        "maiusculo" to "toupper", "minusculo" to "tolower",
        // time
        "relogio" to "clock", "diferenca_tempo" to "difftime", "data_texto" to "ctime"
    )

    // Constantes traduzidas (seção 5)
    private val constantesTraduzidas: Map<String, String> = mapOf(
        "PI" to "M_PI",
        "E" to "M_E"
    )

    /** Ponto de entrada legado: retorna só o .c (com includes inline), sem separar .h.
    *   Mantido para quem ainda consome a API antiga de arquivo único. */
    fun transpilar(programa: Programa): String {
        return transpilarModulo(programa).codigoC
    }

    fun transpilarModulo(programa: Programa): ResultadoTranspilacao {
        tiposVariaveis.clear()
        assinaturasFuncoes.clear()
        structs.clear()
        aliasesExternos.clear()
        usouStrcmp = false
        contadorEscolhaTexto = 0
        memoria.reiniciar()
        nomesBufferTexto.clear()

        for (decl in programa.declaracoesGlobais) {
            when (decl) {
                is DeclaracaoFuncao -> {
                    assinaturasFuncoes[decl.nome] = decl.parametros
                    retornosFuncoes[decl.nome] = decl.tipoRetorno
                    if (decl.nomeReal != null) aliasesExternos[decl.nome] = decl.nomeReal
                }
                is DeclaracaoStruct -> structs[decl.nome] = decl
                else -> {}
            }
        }

        val nomeGuarda = nomeArquivo.uppercase().replace(Regex("[^A-Z0-9_]"), "_") + "_H"

        val sbH = StringBuilder()
        sbH.append("#ifndef $nomeGuarda\n")
        sbH.append("#define $nomeGuarda\n\n")

        val includesH = linkedSetOf<String>()
        if (usaImprimirOuLer(programa)) includesH.add("#include <stdio.h>")
        for (inc in programa.includes) {
            includesH.add(diretivaInclude(inc))
        }
        for (linha in includesH) sbH.append(linha).append("\n")
        if (includesH.isNotEmpty()) sbH.append("\n")

        for (decl in programa.declaracoesGlobais) {
            if (decl is DeclaracaoStruct) {
                sbH.append(transpilarStruct(decl))
                sbH.append("\n")
            }
        }

        // Protótipos: só para funções com implementação em TypeC (corpo != null).
        // Declarações externas (corpo == null) não geram protótipo aqui — a função
        // já existe na lib trazida por 'inclua', cujo próprio header já a declara.
        for (decl in programa.declaracoesGlobais) {
            if (decl is DeclaracaoFuncao && decl.corpo != null) {
                sbH.append(prototipo(decl)).append("\n")
            }
        }

        sbH.append("\n#endif // $nomeGuarda\n")

        // Corpo do .c gerado ANTES de montar o cabeçalho do .c, porque durante a
        // geração descobrimos se algum 'escolha' de texto precisou de strcmp (string.h).
        val corpoC = StringBuilder()

        // Implementação: só para funções com corpo. Declarações externas são puramente
        // metadado do lado TypeC (assinatura/tipo já registrados acima) e não geram nada aqui.
        for (decl in programa.declaracoesGlobais) {
            if (decl is DeclaracaoFuncao && decl.corpo != null) {
                corpoC.append(transpilarFuncao(decl))
                corpoC.append("\n")
            }
        }

        if (programa.inicio != null) {
            nomesBufferTexto.clear()
            dentroDoInicio = true
            corpoC.append("int main() {\n")
            for (comando in programa.inicio.comandos) {
                corpoC.append(transpilarComando(comando, nivel = 1))
            }
            // Fallback: se o inicio nao termina com 'retorne', o programa devolve 0 (sucesso)
            if (programa.inicio.comandos.lastOrNull() !is ComandoRetorna) {
                corpoC.append("    return 0;\n")
            }
            dentroDoInicio = false
            corpoC.append("}\n")
        }

        val temIncludeLocal = programa.includes.any { it.ehArquivoLocal }
        // Módulo (sem inicio) ou programa com includes locais: o .c inclui o próprio .h.
        // Programa de arquivo único (o caso comum): tudo inline no .c, para ele compilar sozinho
        // (o .h dele não é incluído por ninguém, então não pode faltar nada no .c).
        val usaHeaderProprio = temIncludeLocal || programa.inicio == null

        val extras = mutableListOf<String>()
        if (usouStrcmp && "#include <string.h>" !in includesH) extras.add("#include <string.h>")
        for (cab in memoria.cabecalhosNecessarios()) {
            val linhaInclude = "#include <$cab>"
            if (linhaInclude !in includesH && linhaInclude !in extras) extras.add(linhaInclude)
        }
        // Funcoes auxiliares de leia() (so as usadas); vazio se o programa nao le nada
        val auxiliares = memoria.gerarAuxiliares()

        val sbC = StringBuilder()
        if (usaHeaderProprio) {
            sbC.append("#include \"$nomeArquivo.h\"\n")
            for (linha in extras) sbC.append(linha).append("\n")
            sbC.append("\n")
            sbC.append(auxiliares)
        } else {
            for (linha in includesH) sbC.append(linha).append("\n")
            for (linha in extras) sbC.append(linha).append("\n")
            if (includesH.isNotEmpty() || extras.isNotEmpty()) sbC.append("\n")
            sbC.append(auxiliares)

            for (decl in programa.declaracoesGlobais) {
                if (decl is DeclaracaoStruct) {
                    sbC.append(transpilarStruct(decl))
                    sbC.append("\n")
                }
            }

            // Protótipos: permitem uma função chamar outra declarada mais abaixo no arquivo
            var teveFuncao = false
            for (decl in programa.declaracoesGlobais) {
                if (decl is DeclaracaoFuncao && decl.corpo != null) {
                    sbC.append(prototipo(decl)).append("\n")
                    teveFuncao = true
                }
            }
            if (teveFuncao) sbC.append("\n")
        }
        sbC.append(corpoC)

        val requisitos = if (programa.includes.any { !it.ehArquivoLocal && it.nomeLib == LibGraficos.NOME_INCLUDE }) {
            listOf("sdl2")
        } else {
            emptyList()
        }
        return ResultadoTranspilacao(sbC.toString(), sbH.toString(), nomeGuarda, requisitos)
    }

    /** Protótipo C de uma função (sem nomes de parâmetro), usado no .h e no .c inline. */
    private fun prototipo(decl: DeclaracaoFuncao): String {
        val params = decl.parametros.joinToString(", ") { p ->
            val tipoC = (if (p.ehConstante) "const " else "") + tipoParaC(p.tipo)
            if (p.ehAlterar) "$tipoC*" else tipoC
        }
        return "${tipoParaC(decl.tipoRetorno)} ${decl.nome}($params);"
    }

    private fun diretivaInclude(inc: Inclua): String {
        // graficos.h acompanha o runtime do PortugolTipado (nao e' lib do sistema como
        // stdlib/math): usa aspas, como um arquivo local, mesmo vindo de 'inclua graficos' sem aspas.
        if (!inc.ehArquivoLocal && inc.nomeLib == LibGraficos.NOME_INCLUDE) {
            return "#include \"graficos.h\""
        }
        return if (inc.ehArquivoLocal) {
            "#include \"${inc.nomeLib}.h\""
        } else {
            "#include <${inc.nomeLib}.h>"
        }
    }

    private fun usaImprimirOuLer(programa: Programa): Boolean {
        fun contemEm(comandos: List<No>): Boolean = comandos.any {
            it is ComandoImprimir || it is ComandoLer
        }
        if (programa.inicio != null && contemEm(programa.inicio.comandos)) return true
        for (decl in programa.declaracoesGlobais) {
            if (decl is DeclaracaoFuncao && decl.corpo != null && contemEm(decl.corpo)) return true
        }
        return true
    }

    // -------------------------------------------------------------
    // Tipos
    // -------------------------------------------------------------

    private fun tipoParaC(tipo: Tipo): String {
        if (tipo.nomeStruct != null) {
            return "struct ${tipo.nomeStruct}"
        }
        val base = when (tipo.base) {
            TipoDado.INTEIRO -> "int"
            TipoDado.REAL -> "float"
            TipoDado.DUPLO -> "double"
            TipoDado.TEXTO -> "char*"
            TipoDado.CARACTERE -> "char"
            TipoDado.LOGICO -> "int"
            TipoDado.VAZIO -> "void"
            TipoDado.INTEIRO_LONGO -> "long"
            TipoDado.INTEIRO_CURTO -> "short"
            TipoDado.INTEIRO_POSITIVO -> "unsigned int"
            TipoDado.INTEIRO_GIGANTE -> "long long"
            TipoDado.DUPLO_LONGO -> "long double"
            TipoDado.TEMPO -> "time_t"
        }
        return base
    }

    private fun especificadorFormato(tipo: Tipo): String = when (tipo.base) {
        TipoDado.INTEIRO, TipoDado.LOGICO -> "%d"
        TipoDado.REAL -> "%f"
        TipoDado.DUPLO -> "%lf"
        TipoDado.TEXTO -> "%s"
        TipoDado.CARACTERE -> "%c"
        TipoDado.INTEIRO_LONGO -> "%ld"
        TipoDado.INTEIRO_CURTO -> "%hd"
        TipoDado.INTEIRO_POSITIVO -> "%u"
        TipoDado.INTEIRO_GIGANTE -> "%lld"
        TipoDado.DUPLO_LONGO -> "%Lf"
        TipoDado.TEMPO -> "%ld"
        TipoDado.VAZIO -> ""
    }

    private fun inferirTipo(expressao: No): Tipo = when (expressao) {
        is Numero -> when (expressao.tipo) {
            TipoDado.INTEIRO -> Tipo(TipoDado.INTEIRO)
            else -> Tipo(TipoDado.DUPLO)
        }
        is Texto -> Tipo(TipoDado.TEXTO)
        is Caractere -> Tipo(TipoDado.CARACTERE)
        is Logico -> Tipo(TipoDado.LOGICO)
        is Identificador -> tiposVariaveis[expressao.nome] ?: Tipo(TipoDado.INTEIRO)
        is AcessoIndice -> {
            // copy() preserva nomeStruct (ex: turma[i] continua sendo 'Aluno', então
            // turma[i].nome acha o campo certo e ganha %s, não %d)
            val tipoArray = inferirTipo(expressao.array)
            if (tipoArray.ehArray2D) {
                // matriz[i] ainda é um array (1D), com o tamanho da segunda dimensão
                tipoArray.copy(ehArray2D = false, tamanhoFixo = tipoArray.tamanhoFixo2, tamanhoFixo2 = null)
            } else {
                tipoArray.copy(ehArray = false, tamanhoFixo = null)
            }
        }
        is AcessoCampo -> {
            val tipoObjeto = inferirTipo(expressao.objeto)
            val nomeStruct = tipoObjeto.nomeStruct
            val structDecl = nomeStruct?.let { structs[it] }
            val campo = structDecl?.campos?.find { it.nome == expressao.campo }
            campo?.tipo ?: Tipo(TipoDado.INTEIRO)
        }
        is ChamadaFuncao -> {
            retornosFuncoes[expressao.nome]
            ?: retornosFuncoesBiblioteca[expressao.nome]
            ?: LibGraficos.funcoes[expressao.nome]?.let { Tipo(it.retorno) }
            ?: Tipo(TipoDado.INTEIRO)
        }
        is OperacaoBinaria -> {
            val comparadores = setOf("==", "!=", ">", "<", ">=", "<=", "&&", "||")
            if (expressao.operador in comparadores) {
                Tipo(TipoDado.LOGICO)
            } else {
                val tipoEsquerda = inferirTipo(expressao.esquerda)
                val tipoDireita = inferirTipo(expressao.direita)
                val ordemPromocao = listOf(
                    TipoDado.DUPLO_LONGO, TipoDado.DUPLO, TipoDado.REAL,
                    TipoDado.INTEIRO_GIGANTE, TipoDado.INTEIRO_LONGO,
                    TipoDado.INTEIRO, TipoDado.INTEIRO_POSITIVO, TipoDado.INTEIRO_CURTO
                )
                val posEsquerda = ordemPromocao.indexOf(tipoEsquerda.base).let { if (it == -1) ordemPromocao.size else it }
                val posDireita = ordemPromocao.indexOf(tipoDireita.base).let { if (it == -1) ordemPromocao.size else it }
                if (posEsquerda <= posDireita) tipoEsquerda else tipoDireita
            }
        }
        else -> Tipo(TipoDado.INTEIRO)
    }

    // -------------------------------------------------------------
    // Structs
    // -------------------------------------------------------------

    private fun transpilarStruct(struct: DeclaracaoStruct): String {
        val sb = StringBuilder()
        sb.append("struct ${struct.nome} {\n")
        for (campo in struct.campos) {
            if (campo.tipo.ehArray && campo.tipo.tamanhoFixo != null) {
                sb.append("    ${tipoParaC(campo.tipo)} ${campo.nome}[${campo.tipo.tamanhoFixo}];\n")
            } else {
                sb.append("    ${tipoParaC(campo.tipo)} ${campo.nome};\n")
            }
        }
        sb.append("};\n")
        return sb.toString()
    }

    // -------------------------------------------------------------
    // Funções
    // -------------------------------------------------------------

    /** Só é chamada para funções com corpo != null (ver filtro em transpilarModulo). */
    private fun transpilarFuncao(funcao: DeclaracaoFuncao): String {
        nomesBufferTexto.clear()
        val sb = StringBuilder()
        val params = funcao.parametros.joinToString(", ") { p ->
            tiposVariaveis[p.nome] = p.tipo
            val tipoC = (if (p.ehConstante) "const " else "") + tipoParaC(p.tipo)
            if (p.ehAlterar) "$tipoC *${p.nome}" else "$tipoC ${p.nome}"
        }
        sb.append("${tipoParaC(funcao.tipoRetorno)} ${funcao.nome}($params) {\n")

        for (comando in funcao.corpo.orEmpty()) {
            sb.append(transpilarComando(comando, nivel = 1, parametrosAlterar = funcao.parametros.filter { it.ehAlterar }.map { it.nome }.toSet()))
        }
        sb.append("}\n")
        return sb.toString()
    }

    // -------------------------------------------------------------
    // Comandos
    // -------------------------------------------------------------

    private fun indentacao(nivel: Int): String = "    ".repeat(nivel)

    private fun transpilarComando(comando: No, nivel: Int, parametrosAlterar: Set<String> = emptySet()): String {
        val ind = indentacao(nivel)
        return when (comando) {
            is DeclaracaoVariavel -> transpilarDeclaracaoVariavel(comando, nivel)
            is ComandoImprimir -> transpilarImprimir(comando, nivel)
            is ComandoLer -> transpilarLer(comando, nivel)
            is ComandoRetorna -> {
                if (dentroDoInicio) transpilarRetornoDoInicio(comando, ind)
                else if (comando.valor != null) "$ind" + "return ${transpilarExpressao(comando.valor, parametrosAlterar)};\n"
                else "$ind" + "return;\n"
            }
            is ComandoSe -> transpilarSe(comando, nivel, parametrosAlterar)
            is ComandoEnquanto -> transpilarEnquanto(comando, nivel, parametrosAlterar)
            is ComandoFacaEnquanto -> transpilarFacaEnquanto(comando, nivel, parametrosAlterar)
            is ComandoDeclaracoes -> comando.declaracoes.joinToString("") { transpilarComando(it, nivel, parametrosAlterar) }
            is ComandoPara -> transpilarPara(comando, nivel, parametrosAlterar)
            is ComandoParaCada -> transpilarParaCada(comando, nivel, parametrosAlterar)
            is ComandoDispensar -> "$ind" + "break;\n"
            is ComandoIgnorar -> "$ind" + "continue;\n"
            is ComandoEscolher -> transpilarEscolher(comando, nivel, parametrosAlterar)
            is Atribuicao -> "$ind" + transpilarAtribuicao(comando, parametrosAlterar) + ";\n"
            is IncrementoDecremento -> "$ind" + transpilarIncrementoDecremento(comando, parametrosAlterar) + ";\n"
            is ExpressaoComando -> "$ind" + "${transpilarExpressao(comando.expressao, parametrosAlterar)};\n"
            else -> "$ind// TODO: comando não suportado ainda ($comando)\n"
        }
    }

    private fun transpilarDeclaracaoVariavel(decl: DeclaracaoVariavel, nivel: Int): String {
        tiposVariaveis[decl.nome] = decl.tipo
        val ind = indentacao(nivel)
        // Em C, 'const' exige valor no mesmo lugar da declaracao (senao nem compila).
        // O PortugolTipado segue a regra do C, nao a do Portugol: aqui e erro de transpilacao,
        // reportado em portugues, em vez de deixar o gcc falhar sem contexto depois.
        if (decl.ehConstante && decl.valorInicial == null) {
            throw ErroTranspilacao("'const' precisa de um valor inicial: const ${tipoParaC(decl.tipo)} ${decl.nome} = ...")
        }
        val prefixoConst = if (decl.ehConstante) "const " else ""
        val tipoC = prefixoConst + tipoParaC(decl.tipo)

        // Array 2D (matriz), ex: "inteiro[][] matriz = {{1,2},{3,4}}" ou "matriz[2][2]". Ver Array2D.kt
        if (decl.tipo.ehArray2D) {
            val corpo = Array2D.transpilarDeclaracao(
                tipoC = tipoC,
                nomeVar = decl.nome,
                tipo = decl.tipo,
                valorInicial = decl.valorInicial
            ) { linhaArray -> linhaArray.valores.joinToString(", ") { transpilarExpressao(it, emptySet()) } }
            return "$ind$corpo"
        }

        if (decl.tipo.ehArray) {
            if (decl.valorInicial == null && decl.tipo.tamanhoFixo != null) {
                return "$ind$tipoC ${decl.nome}[${decl.tipo.tamanhoFixo}];\n"
            }
            val valores = (decl.valorInicial as? ArrayLiteral)?.valores?.joinToString(", ") {
                transpilarExpressao(it, emptySet())
            } ?: ""
            val colchete = decl.tipo.tamanhoFixo?.toString() ?: ""
            return "$ind$tipoC ${decl.nome}[$colchete] = {$valores};\n"
        }

        // 'const texto' sempre cai aqui com valorInicial != null (ja validado acima),
        // entao nunca precisa de buffer proprio -- so texto local SEM valor entra neste caso.
        if (memoria.precisaBufferProprio(decl.tipo, decl.valorInicial != null)) {
            nomesBufferTexto.add(decl.nome)
            return "$ind${memoria.declararBufferTexto(decl.nome)}\n"
        }

        val valorInicialStruct = decl.valorInicial
        if (decl.tipo.nomeStruct != null && valorInicialStruct is ArrayLiteral) {
            val valores = valorInicialStruct.valores.joinToString(", ") {
                transpilarExpressao(it, emptySet())
            }
            return "$ind$tipoC ${decl.nome} = {$valores};\n"
        }

        return if (decl.valorInicial != null) {
            "$ind$tipoC ${decl.nome} = ${transpilarExpressao(decl.valorInicial, emptySet())};\n"
        } else {
            "$ind$tipoC ${decl.nome};\n"
        }
    }

    private fun transpilarImprimir(cmd: ComandoImprimir, nivel: Int): String {
        val ind = indentacao(nivel)
        val formatoSb = StringBuilder()
        val valoresParaImprimir = mutableListOf<String>()

        for (arg in cmd.argumentos) {
            if (arg is Texto) {
                formatoSb.append(arg.valor.replace("%", "%%"))
            } else if (arg is ChamadaFuncao && arg.nome == "tamanho") {
                formatoSb.append("%zu")
                valoresParaImprimir.add(transpilarExpressao(arg, emptySet()))
            } else {
                val tipo = inferirTipo(arg)
                formatoSb.append(especificadorFormato(tipo))
                valoresParaImprimir.add(transpilarExpressao(arg, emptySet()))
            }
        }

        val argsFinal = if (valoresParaImprimir.isEmpty()) ""
        else ", " + valoresParaImprimir.joinToString(", ")

        return "$ind" + "printf(\"$formatoSb\"$argsFinal);\n"
    }

    /**
     * 'retorne' dentro do 'inicio': o main de C devolve int. Sem valor devolve 0 (sucesso);
     * valor real/duplo e convertido para int (evita aviso e devolucao sem sentido).
     */
    private fun transpilarRetornoDoInicio(comando: ComandoRetorna, ind: String): String {
        val valor = comando.valor ?: return "$ind" + "return 0;\n"
        val expr = transpilarExpressao(valor, emptySet())
        val base = inferirTipo(valor).base
        val ehFlutuante = base == TipoDado.REAL || base == TipoDado.DUPLO || base == TipoDado.DUPLO_LONGO
        return if (ehFlutuante) "$ind" + "return (int)($expr);\n" else "$ind" + "return $expr;\n"
    }

    private fun transpilarLer(cmd: ComandoLer, nivel: Int): String {
        val ind = indentacao(nivel)
        val tipo = inferirTipo(cmd.alvo)
        val formato = especificadorFormato(tipo)
        val nomeVar = transpilarExpressao(cmd.alvo, emptySet())

        if (tipo.base == TipoDado.TEXTO && !tipo.ehArray) {
            // sizeof so e valido quando o alvo e uma variavel local que virou buffer proprio
            val ehBufferLocal = cmd.alvo is Identificador && nomesBufferTexto.contains((cmd.alvo as Identificador).nome)
            val limite = if (ehBufferLocal) "sizeof($nomeVar)" else null
            return memoria.lerTexto(nomeVar, limite, ind)
        }

        if (!tipo.ehArray) {
            val bloco = memoria.lerPrimitivo(tipo.base, nomeVar, ind)
            if (bloco != null) return bloco
        }
        return "$ind" + "scanf(\"$formato\", &$nomeVar);\n"
    }

    private fun transpilarSe(cmd: ComandoSe, nivel: Int, parametrosAlterar: Set<String>): String {
        val ind = indentacao(nivel)
        val sb = StringBuilder()
        sb.append("$ind" + "if (${transpilarExpressao(cmd.condicao, parametrosAlterar)}) {\n")
        for (c in cmd.entao) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
        sb.append("$ind}")

        for (par in cmd.senaoSe) {
            sb.append(" else if (${transpilarExpressao(par.primeiro, parametrosAlterar)}) {\n")
            for (c in par.segundo) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
            sb.append("$ind}")
        }

        if (cmd.senao != null) {
            sb.append(" else {\n")
            for (c in cmd.senao) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
            sb.append("$ind}")
        }

        sb.append("\n")
        return sb.toString()
    }

    private fun transpilarEnquanto(cmd: ComandoEnquanto, nivel: Int, parametrosAlterar: Set<String>): String {
        val ind = indentacao(nivel)
        val sb = StringBuilder()
        sb.append("$ind" + "while (${transpilarExpressao(cmd.condicao, parametrosAlterar)}) {\n")
        for (c in cmd.corpo) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
        sb.append("$ind}\n")
        return sb.toString()
    }

    private fun transpilarFacaEnquanto(cmd: ComandoFacaEnquanto, nivel: Int, parametrosAlterar: Set<String>): String {
        val ind = indentacao(nivel)
        val sb = StringBuilder()
        sb.append("$ind" + "do {\n")
        for (c in cmd.corpo) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
        sb.append("$ind} while (${transpilarExpressao(cmd.condicao, parametrosAlterar)});\n")
        return sb.toString()
    }

    private fun transpilarPara(cmd: ComandoPara, nivel: Int, parametrosAlterar: Set<String>): String {
        val ind = indentacao(nivel)
        val sb = StringBuilder()

        val inicializacaoStr = when (val i = cmd.inicializacao) {
            is DeclaracaoVariavel -> {
                tiposVariaveis[i.nome] = i.tipo
                "${tipoParaC(i.tipo)} ${i.nome} = ${transpilarExpressao(i.valorInicial!!, parametrosAlterar)}"
            }
            is Atribuicao -> transpilarAtribuicao(i, parametrosAlterar)
            null -> ""
            else -> ""
        }

        val condicaoStr = cmd.condicao?.let { transpilarExpressao(it, parametrosAlterar) } ?: ""

        val incrementoStr = when (val inc = cmd.incremento) {
            is IncrementoDecremento -> transpilarIncrementoDecremento(inc, parametrosAlterar)
            is Atribuicao -> transpilarAtribuicao(inc, parametrosAlterar)
            null -> ""
            else -> ""
        }

        sb.append("$ind" + "for ($inicializacaoStr; $condicaoStr; $incrementoStr) {\n")
        for (c in cmd.corpo) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
        sb.append("$ind}\n")
        return sb.toString()
    }

    private fun transpilarParaCada(cmd: ComandoParaCada, nivel: Int, parametrosAlterar: Set<String>): String {
        val ind = indentacao(nivel)
        val sb = StringBuilder()
        val indiceInterno = "_i"
        val nomeArray = transpilarExpressao(cmd.array, parametrosAlterar)
        val tamanho = inferirTamanhoArray(cmd.array)

        tiposVariaveis[cmd.nomeElemento] = cmd.tipoElemento

        sb.append("$ind" + "for (int $indiceInterno = 0; $indiceInterno < $tamanho; $indiceInterno++) {\n")
        sb.append("${indentacao(nivel + 1)}${tipoParaC(cmd.tipoElemento)} ${cmd.nomeElemento} = $nomeArray[$indiceInterno];\n")
        for (c in cmd.corpo) sb.append(transpilarComando(c, nivel + 1, parametrosAlterar))
        sb.append("$ind}\n")
        return sb.toString()
    }

    private fun inferirTamanhoArray(expressaoArray: No): String {
        // Tamanho declarado (variável 'inteiro v[5]' ou campo de struct 'real notas[4]')
        val tamanhoDeclarado: Int? = when (expressaoArray) {
            is Identificador -> tiposVariaveis[expressaoArray.nome]?.tamanhoFixo
            is AcessoCampo -> {
                val nomeStruct = inferirTipo(expressaoArray.objeto).nomeStruct
                val decl = nomeStruct?.let { structs[it] }
                decl?.campos?.find { it.nome == expressaoArray.campo }?.tipo?.tamanhoFixo
            }
            else -> null
        }
        if (tamanhoDeclarado != null) return tamanhoDeclarado.toString()

        // Array declarado com literal (tamanho inferido pelo compilador C): sizeof funciona
        // no escopo onde ele foi declarado
        if (expressaoArray is Identificador) {
            return "sizeof(${expressaoArray.nome}) / sizeof(${expressaoArray.nome}[0])"
        }
        return "0 /* TODO: não foi possível inferir tamanho do array */"
    }

    /** switch do C só aceita inteiro/char; com texto (ou casos texto) o escolha vira strcmp. */
    private fun ehEscolhaDeTexto(cmd: ComandoEscolher): Boolean {
        val tipo = inferirTipo(cmd.valor)
        return (tipo.base == TipoDado.TEXTO && !tipo.ehArray) || cmd.casos.any { it.valor is Texto }
    }

    /** true se sobrar um 'pare' que pertence a este escolha (não a um laço/escolha aninhado) */
    private fun contemDispensarSolto(comandos: List<No>): Boolean = comandos.any { c ->
        c is ComandoDispensar || (c is ComandoSe && (
            contemDispensarSolto(c.entao) ||
            c.senaoSe.any { contemDispensarSolto(it.segundo) } ||
            (c.senao?.let { contemDispensarSolto(it) } ?: false)
        ))
    }

    /**
    * escolha (texto) { caso "A": ... pare  caso "B": ... casocontrario: ... }
    * vira:
    *   { char* _escolhaN = valor;  if (strcmp(_escolhaN, "A") == 0) {...} else if ... else {...} }
    * O valor é avaliado uma vez só (pode ser uma chamada de função). O 'pare' no fim de cada
    * caso some (a cadeia if já não cai no caso seguinte). Casos vazios em sequência
    * (caso "A": caso "B": corpo) viram uma condição com ||. Se sobrar 'pare' no meio do corpo,
    * a cadeia é envolvida em do { } while (0) para o break sair só dela, não do laço de fora.
    * Limitação: queda (fallthrough) de um caso COM corpo para o próximo não é reproduzida.
    */
    private fun transpilarEscolherTexto(cmd: ComandoEscolher, nivel: Int, parametrosAlterar: Set<String>): String {
        usouStrcmp = true
        val tmp = "_escolha${contadorEscolhaTexto++}"
        val ind0 = indentacao(nivel)

        fun semDispensarFinal(corpo: List<No>): List<No> =
            if (corpo.lastOrNull() is ComandoDispensar) corpo.dropLast(1) else corpo

        val ramos = mutableListOf<Pair<List<String>, List<No>>>()
        var pendentes = mutableListOf<String>()
        for (caso in cmd.casos) {
            pendentes.add("strcmp($tmp, ${transpilarExpressao(caso.valor, parametrosAlterar)}) == 0")
            if (caso.corpo.isEmpty()) continue // queda direta: junta com o próximo caso
            ramos.add(pendentes to semDispensarFinal(caso.corpo))
            pendentes = mutableListOf()
        }
        if (pendentes.isNotEmpty()) ramos.add(pendentes to emptyList())
        val padrao = cmd.padrao?.let { semDispensarFinal(it) }

        val precisaEnvolver = ramos.any { contemDispensarSolto(it.second) } ||
            (padrao != null && contemDispensarSolto(padrao))
        val nCadeia = if (precisaEnvolver) nivel + 2 else nivel + 1
        val indCadeia = indentacao(nCadeia)

        val sb = StringBuilder()
        sb.append("$ind0{\n")
        sb.append("${indentacao(nivel + 1)}char* $tmp = ${transpilarExpressao(cmd.valor, parametrosAlterar)};\n")
        if (precisaEnvolver) sb.append("${indentacao(nivel + 1)}do {\n")

        var primeiro = true
        for ((condicoes, corpo) in ramos) {
            val cond = condicoes.joinToString(" || ")
            sb.append(if (primeiro) "${indCadeia}if ($cond) {\n" else " else if ($cond) {\n")
            for (c in corpo) sb.append(transpilarComando(c, nCadeia + 1, parametrosAlterar))
            sb.append("$indCadeia}")
            primeiro = false
        }
        if (padrao != null) {
            sb.append(if (primeiro) "$indCadeia{\n" else " else {\n")
            for (c in padrao) sb.append(transpilarComando(c, nCadeia + 1, parametrosAlterar))
            sb.append("$indCadeia}")
            primeiro = false
        }
        if (!primeiro) sb.append("\n")

        if (precisaEnvolver) sb.append("${indentacao(nivel + 1)}} while (0);\n")
        sb.append("$ind0}\n")
        return sb.toString()
    }

    private fun transpilarEscolher(cmd: ComandoEscolher, nivel: Int, parametrosAlterar: Set<String>): String {
        if (ehEscolhaDeTexto(cmd)) return transpilarEscolherTexto(cmd, nivel, parametrosAlterar)
        val ind = indentacao(nivel)
        val sb = StringBuilder()
        sb.append("$ind" + "switch (${transpilarExpressao(cmd.valor, parametrosAlterar)}) {\n")
        for (caso in cmd.casos) {
            sb.append("${indentacao(nivel + 1)}case ${transpilarExpressao(caso.valor, parametrosAlterar)}:\n")
            for (c in caso.corpo) sb.append(transpilarComando(c, nivel + 2, parametrosAlterar))
        }
        if (cmd.padrao != null) {
            sb.append("${indentacao(nivel + 1)}default:\n")
            for (c in cmd.padrao) sb.append(transpilarComando(c, nivel + 2, parametrosAlterar))
        }
        sb.append("$ind}\n")
        return sb.toString()
    }

    // -------------------------------------------------------------
    // Atribuição / incremento (considera 'altere' -> desreferência *nome)
    // -------------------------------------------------------------

    private fun transpilarAtribuicao(atr: Atribuicao, parametrosAlterar: Set<String>): String {
        val alvoStr = transpilarExpressao(atr.alvo, parametrosAlterar)
        val valorStr = transpilarExpressao(atr.valor, parametrosAlterar)
        return "$alvoStr ${atr.operador} $valorStr"
    }

    private fun transpilarIncrementoDecremento(cmd: IncrementoDecremento, parametrosAlterar: Set<String>): String {
        val alvoStr = transpilarExpressao(cmd.alvo, parametrosAlterar)
        return "$alvoStr${cmd.operador}"
    }

    // -------------------------------------------------------------
    // Expressões
    // -------------------------------------------------------------

    private fun transpilarExpressao(expressao: No, parametrosAlterar: Set<String>): String = when (expressao) {
        is Numero -> when (expressao.tipo) {
            TipoDado.INTEIRO -> expressao.valor
            else -> if ('.' in expressao.valor) expressao.valor else "${expressao.valor}.0"
        }
        is Texto -> "\"${expressao.valor}\""
        is Caractere -> "'${expressao.valor}'"
        is Logico -> if (expressao.valor) "1" else "0"
        is Identificador -> {
            when {
                expressao.nome in parametrosAlterar -> "*${expressao.nome}"
                expressao.nome in constantesTraduzidas -> constantesTraduzidas.getValue(expressao.nome)
                LibGraficos.ehConstante(expressao.nome) -> LibGraficos.constantes.getValue(expressao.nome).nomeC
                else -> expressao.nome
            }
        }
        is OperacaoBinaria -> {
            val esq = transpilarExpressao(expressao.esquerda, parametrosAlterar)
            val dir = transpilarExpressao(expressao.direita, parametrosAlterar)
            val comparaTexto = (expressao.operador == "==" || expressao.operador == "!=") &&
                (inferirTipo(expressao.esquerda).base == TipoDado.TEXTO || inferirTipo(expressao.direita).base == TipoDado.TEXTO)
            if (comparaTexto) {
                // em C, '==' entre textos compara enderecos; o conteudo se compara com strcmp
                usouStrcmp = true
                "strcmp($esq, $dir) ${expressao.operador} 0"
            } else {
                "$esq ${expressao.operador} $dir"
            }
        }
        is OperacaoUnaria -> "${expressao.operador}${transpilarExpressao(expressao.operando, parametrosAlterar)}"
        is ChamadaFuncao -> transpilarChamadaFuncao(expressao, parametrosAlterar)
        is AcessoIndice -> {
            val arrExpr = expressao.array
            // parâmetro 'altere' é ponteiro: (*v)[i], senão '*v[i]' leria como *(v[i])
            val arr = if (arrExpr is Identificador && arrExpr.nome in parametrosAlterar) {
                "(*${arrExpr.nome})"
            } else {
                transpilarExpressao(arrExpr, parametrosAlterar)
            }
            val idx = transpilarExpressao(expressao.indice, parametrosAlterar)
            "$arr[$idx]"
        }
        is AcessoCampo -> {
            val objExpr = expressao.objeto
            if (objExpr is Identificador && objExpr.nome in parametrosAlterar) {
                // parâmetro 'altere' é ponteiro para a struct: a->campo, não '*a.campo'
                "${objExpr.nome}->${expressao.campo}"
            } else {
                val obj = transpilarExpressao(expressao.objeto, parametrosAlterar)
                "$obj.${expressao.campo}"
            }
        }
        is Atribuicao -> transpilarAtribuicao(expressao, parametrosAlterar)
        is IncrementoDecremento -> transpilarIncrementoDecremento(expressao, parametrosAlterar)
        is ArrayLiteral -> {
            val valores = expressao.valores.joinToString(", ") { transpilarExpressao(it, parametrosAlterar) }
            "{$valores}"
        }
        else -> "/* TODO: expressão não suportada */"
    }

    private fun transpilarChamadaFuncao(chamada: ChamadaFuncao, parametrosAlterarDoEscopoAtual: Set<String>): String {
        val nomeReal = aliasesExternos[chamada.nome]
            ?: funcoesTraduzidas[chamada.nome]
            ?: LibGraficos.funcoes[chamada.nome]?.nomeC
            ?: chamada.nome
        val assinatura = assinaturasFuncoes[chamada.nome]

        val ehEnderecavel = { no: No -> no is Identificador || no is AcessoCampo || no is AcessoIndice }

        val argumentos = chamada.argumentos.mapIndexed { i, arg ->
            val paramEhAlterar = assinatura?.getOrNull(i)?.ehAlterar == true
            if (paramEhAlterar && ehEnderecavel(arg)) {
                "&${transpilarExpressao(arg, parametrosAlterarDoEscopoAtual)}"
            } else {
                transpilarExpressao(arg, parametrosAlterarDoEscopoAtual)
            }
        }.joinToString(", ")

        // copie/junte: se o destino e um buffer local (char nome[N]), usa a versao limitada por sizeof
        if ((chamada.nome == "copie" || chamada.nome == "junte") && chamada.argumentos.size == 2) {
            val destino = chamada.argumentos[0]
            if (destino is Identificador && nomesBufferTexto.contains(destino.nome)) {
                val d = transpilarExpressao(destino, parametrosAlterarDoEscopoAtual)
                val o = transpilarExpressao(chamada.argumentos[1], parametrosAlterarDoEscopoAtual)
                return if (chamada.nome == "copie") memoria.copiar(d, o) else memoria.concatenar(d, o)
            }
        }

        return "$nomeReal($argumentos)"
    }
}
