package co.adilson889.typec.transpilador

/**
 * Mapa das bibliotecas padrão do C para os nomes em português do PortugolTipado.
 *
 * Só dados: não valida nem transpila nada sozinho. O Transpilador consulta este
 * mapa para traduzir nomes de função e o nome da biblioteca em 'inclua'.
 *
 * Cada biblioteca guarda:
 *  - nomePort: nome usado em 'inclua' (ex: 'inclua matematica'). null = automática,
 *    nunca se escreve (o cabeçalho C entra sozinho quando necessário).
 *  - cabecalhoC: cabeçalho C correspondente, sem '.h' (ex: "math" -> <math.h>).
 *  - funcoes: nome em português -> função em C.
 */
enum class BibliotecaPadrao(
    val nomePort: String?,
    val cabecalhoC: String,
    val funcoes: Map<String, String>
) {
    MATEMATICA(
        "matematica", "math",
        mapOf(
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
            "eh_finito" to "isfinite"
        )
    ),

    TEXTO(
        "texto", "string",
        mapOf(
            "tamanho" to "strlen", "copie" to "strcpy", "junte" to "strcat", "compare" to "strcmp"
        )
    ),

    CARACTERES(
        "caracteres", "ctype",
        mapOf(
            "eh_letra" to "isalpha", "eh_numero" to "isdigit", "eh_letra_ou_numero" to "isalnum",
            "eh_espaco" to "isspace", "eh_maiuscula" to "isupper", "eh_minuscula" to "islower",
            "eh_pontuacao" to "ispunct", "eh_controle" to "iscntrl", "eh_imprimivel" to "isprint",
            "eh_grafico" to "isgraph", "eh_hexadecimal" to "isxdigit", "eh_branco" to "isblank",
            "maiusculo" to "toupper", "minusculo" to "tolower"
        )
    ),

    TEMPO(
        "tempo", "time",
        mapOf(
            "relogio" to "clock", "diferenca_tempo" to "difftime", "data_texto" to "ctime"
        )
    ),

    // Automática: entra sozinha, nunca se escreve 'inclua'.
    PADRAO(
        null, "stdlib",
        mapOf(
            "absoluto_int" to "abs",
            "converta_inteiro" to "atoi", "converta_decimal" to "atof", "converta_longo" to "atol",
            "aleatorio" to "rand", "semente" to "srand",
            "aloque" to "malloc", "aloque_zerado" to "calloc", "realoque" to "realloc", "libere" to "free",
            "sair" to "exit", "aborte" to "abort", "variavel_ambiente" to "getenv", "execute" to "system",
            "ordene" to "qsort", "busque" to "bsearch"
        )
    ),

    // Automática: entra sozinha, nunca se escreve 'inclua'.
    ENTRADA_SAIDA(
        null, "stdio",
        mapOf(
            "leia_caractere" to "getchar", "escreva_caractere" to "putchar",
            "escreva_linha" to "puts", "leia_linha" to "fgets"
        )
    )
}

object MapaBibliotecas {

    /** Todas as funções das bibliotecas: nome em português -> função em C. */
    val funcoes: Map<String, String> =
        BibliotecaPadrao.values().flatMap { it.funcoes.entries }.associate { it.key to it.value }

    private val cabecalhoPorNomePort: Map<String, String> =
        BibliotecaPadrao.values()
            .filter { it.nomePort != null }
            .associate { it.nomePort!! to it.cabecalhoC }

    private val bibliotecaPorFuncao: Map<String, BibliotecaPadrao> =
        BibliotecaPadrao.values().flatMap { b -> b.funcoes.keys.map { it to b } }.toMap()

    /** Biblioteca a que a função (nome em português) pertence, ou null se não é de nenhuma. */
    fun bibliotecaDaFuncao(nome: String): BibliotecaPadrao? = bibliotecaPorFuncao[nome]

    /**
     * Cabeçalho C (sem '.h') para o nome usado em 'inclua'.
     * 'matematica' -> "math". Nome fora do mapa (ex: biblioteca de terceiros,
     * ou o nome C direto) segue como veio.
     */
    fun cabecalhoDe(nome: String): String = cabecalhoPorNomePort[nome.lowercase()] ?: nome
}
