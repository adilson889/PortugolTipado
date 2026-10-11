package co.adilson889.typec.transpilador

import co.adilson889.typec.ast.Tipo
import co.adilson889.typec.ast.TipoDado

/**
 * Gerenciador de memoria e entrada do Transpilador.
 *
 * O C gerado e para ser COPIADO e compilado noutro ambiente (gcc etc.), por isso:
 *  - nao ha biblioteca externa: as funcoes auxiliares de entrada sao emitidas UMA vez no topo
 *    do proprio ficheiro C, e so as que o programa realmente usa;
 *  - cada 'leia' vira uma unica linha de chamada, facil de ler;
 *  - texto local vira array fixo na pilha (char nome[256]): sem malloc, sem vazamento,
 *    e sizeof() da o limite real de cada leitura;
 *  - numeros sao lidos por linha inteira e validados (formato, lixo depois do numero e
 *    intervalo do tipo). Entrada invalida: avisa e repete. Fim da entrada: termina com erro.
 *
 * Motivo: 'scanf("%s", nome)' em 'char* nome;' gravava num endereco indefinido, e
 * scanf misturado com fgets deixa lixo no buffer de entrada.
 */
class MemoryManager(
    /** Capacidade padrao, em bytes, de uma variavel de texto local (inclui o terminador nulo). */
    val capacidadeTexto: Int = CAPACIDADE_TEXTO_PADRAO
) {

    init {
        require(capacidadeTexto in 2..MAXIMO_SEGURO_PILHA) {
            "capacidadeTexto deve estar entre 2 e $MAXIMO_SEGURO_PILHA, recebido $capacidadeTexto"
        }
    }

    /** Funcoes auxiliares que o C gerado pode precisar, na ordem em que sao emitidas. */
    private enum class Auxiliar { TEXTO, EXIGIR, INTEIRO, REAL, CARACTERE }

    private val auxiliaresUsados = mutableSetOf<Auxiliar>()
    private var usaCopiaSegura = false
    private var usaJuncaoSegura = false

    // Cabecalhos <...> que o codigo gerado realmente precisa
    private val cabecalhos = linkedSetOf<String>()

    /** Limpa o estado entre transpilacoes. */
    fun reiniciar() {
        cabecalhos.clear()
        auxiliaresUsados.clear()
        usaCopiaSegura = false
        usaJuncaoSegura = false
    }

    /** Cabecalhos necessarios pelo que foi emitido ate agora (chamadas e auxiliares). */
    fun cabecalhosNecessarios(): Set<String> = cabecalhos.toSet()

    private fun usar(vararg nomes: String) {
        cabecalhos.addAll(nomes)
    }

    private fun pedir(aux: Auxiliar) {
        auxiliaresUsados.add(aux)
        auxiliaresUsados.add(Auxiliar.TEXTO)
        if (aux != Auxiliar.TEXTO) auxiliaresUsados.add(Auxiliar.EXIGIR)
        usar("stdio.h", "string.h")
        if (aux != Auxiliar.TEXTO) usar("stdlib.h")
        if (aux == Auxiliar.INTEIRO || aux == Auxiliar.REAL) usar("errno.h")
    }

    // -------------------------------------------------------------
    // Decisao de armazenamento
    // -------------------------------------------------------------

    /**
     * Uma variavel de texto local, sem valor inicial, precisa de buffer proprio.
     * Com valor inicial (literal ou expressao) o ponteiro ja aponta para memoria valida.
     * Arrays, structs e outros tipos nao sao afetados.
     */
    fun precisaBufferProprio(tipo: Tipo, temValorInicial: Boolean): Boolean =
        tipo.base == TipoDado.TEXTO &&
            !tipo.ehArray &&
            !tipo.ehArray2D &&
            tipo.nomeStruct == null &&
            !temValorInicial

    /** Declaracao C de um buffer de texto zerado: 'char nome[256] = {0};' */
    fun declararBufferTexto(nome: String): String =
        "char $nome[$capacidadeTexto] = {0};"

    // -------------------------------------------------------------
    // Leitura: cada funcao devolve UMA linha de C (indentada e terminada em nova linha)
    // -------------------------------------------------------------

    /**
     * Le uma linha inteira (aceita espacos) para o alvo, sem passar do limite.
     * Se o alvo e um buffer conhecido, 'tamanhoConhecido' e "sizeof(nome)".
     * Caso contrario (parametro, campo de struct, ponteiro) usa a capacidade padrao,
     * porque nao ha como saber o tamanho real.
     */
    fun lerTexto(alvo: String, tamanhoConhecido: String?, ind: String): String {
        require(tamanhoConhecido != null) { "leia(texto): capacidade do buffer precisa ser conhecida" }
        pedir(Auxiliar.EXIGIR)
        return "${ind}while (tc_exigir_linha($alvo, $tamanhoConhecido) != 1) {\n" +
            "${ind}    fputs(\"Texto muito longo, tente novamente: \", stderr);\n" +
            "${ind}}\n"
    }

    /**
     * Leitura de tipo primitivo, validada e com repeticao. Devolve null se o tipo nao tem
     * leitor (o chamador cai no scanf).
     */
    fun lerPrimitivo(tipo: TipoDado, alvo: String, ind: String): String? {
        val chamada = when (tipo) {
            TipoDado.INTEIRO, TipoDado.LOGICO -> inteiro("(int)", "INT_MIN", "INT_MAX")
            TipoDado.INTEIRO_CURTO -> inteiro("(short)", "SHRT_MIN", "SHRT_MAX")
            TipoDado.INTEIRO_POSITIVO -> inteiro("(unsigned int)", "0", "UINT_MAX")
            TipoDado.INTEIRO_LONGO -> inteiro("(long)", "LONG_MIN", "LONG_MAX")
            TipoDado.INTEIRO_GIGANTE -> inteiro("", "LLONG_MIN", "LLONG_MAX")
            TipoDado.TEMPO -> inteiro("(time_t)", "LLONG_MIN", "LLONG_MAX")
            TipoDado.REAL -> real("(float)", "FLT_MAX")
            TipoDado.DUPLO -> real("(double)", "DBL_MAX")
            TipoDado.DUPLO_LONGO -> real("", "LDBL_MAX")
            TipoDado.CARACTERE -> {
                pedir(Auxiliar.CARACTERE)
                "tc_ler_caractere()"
            }
            else -> return null
        }
        return "$ind$alvo = $chamada;\n"
    }

    private fun inteiro(conversao: String, minimo: String, maximo: String): String {
        pedir(Auxiliar.INTEIRO)
        usar("limits.h")
        return "${conversao}tc_ler_inteiro($minimo, $maximo)"
    }

    private fun real(conversao: String, limite: String): String {
        pedir(Auxiliar.REAL)
        usar("float.h")
        return "${conversao}tc_ler_real(-$limite, $limite)"
    }

    // -------------------------------------------------------------
    // Copia e concatenacao limitadas ao buffer local
    // -------------------------------------------------------------

    /** Copia limitada por sizeof. Devolve a chamada C (sem ';'). Trunca em vez de estourar. */
    fun copiar(destino: String, origem: String): String {
        usaCopiaSegura = true
        usar("string.h")
        return "tc_copiar_texto($destino, sizeof($destino), $origem)"
    }

    /** Concatenacao limitada por sizeof. Devolve a chamada C (sem ';'). Trunca em vez de estourar. */
    fun concatenar(destino: String, origem: String): String {
        usaJuncaoSegura = true
        usar("string.h")
        return "tc_juntar_texto($destino, sizeof($destino), $origem)"
    }

    // -------------------------------------------------------------
    // Funcoes auxiliares (emitidas uma vez, no topo do .c, depois dos includes)
    // -------------------------------------------------------------

    /** Texto C das funcoes auxiliares realmente usadas. Vazio se o programa nao le nada. */
    fun gerarAuxiliares(): String {
        if (auxiliaresUsados.isEmpty() && !usaCopiaSegura && !usaJuncaoSegura) return ""
        val sb = StringBuilder("/* --- funcoes auxiliares PortugolTipado (seguranca) --- */\n\n")
        for (aux in Auxiliar.values()) {
            if (aux !in auxiliaresUsados) continue
            sb.append(
                when (aux) {
                    Auxiliar.TEXTO -> AUX_TEXTO
                    Auxiliar.EXIGIR -> AUX_EXIGIR
                    Auxiliar.INTEIRO -> AUX_INTEIRO
                    Auxiliar.REAL -> AUX_REAL
                    Auxiliar.CARACTERE -> AUX_CARACTERE
                }.trimIndent()
            ).append("\n\n")
        }
        if (usaCopiaSegura) sb.append(AUX_COPIAR_SEGURO.trimIndent()).append("\n\n")
        if (usaJuncaoSegura) sb.append(AUX_JUNTAR_SEGURO.trimIndent()).append("\n\n")
        return sb.toString()
    }

    companion object {
        private const val AUX_COPIAR_SEGURO = """
            /* Copia limitada; memmove aceita sobreposicao. */
            static char *tc_copiar_texto(char *destino, size_t capacidade, const char *origem) {
                if (capacidade == 0) return destino;
                size_t n = strlen(origem);
                if (n >= capacidade) n = capacidade - 1;
                memmove(destino, origem, n);
                destino[n] = '\0';
                return destino;
            }"""

        private const val AUX_JUNTAR_SEGURO = """
            /* Concatena no destino sem exceder a capacidade conhecida. */
            static char *tc_juntar_texto(char *destino, size_t capacidade, const char *origem) {
                if (capacidade == 0) return destino;
                size_t usados = strlen(destino);
                if (usados >= capacidade) {
                    destino[capacidade - 1] = '\0';
                    return destino;
                }
                size_t n = strlen(origem);
                size_t livre = capacidade - usados - 1;
                if (n > livre) n = livre;
                memmove(destino + usados, origem, n);
                destino[usados + n] = '\0';
                return destino;
            }"""

        const val CAPACIDADE_TEXTO_PADRAO = 256

        /** Acima disto um array local na pilha deixa de ser prudente (pilhas moveis costumam ter ~1 MB). */
        const val MAXIMO_SEGURO_PILHA = 64 * 1024

        private const val AUX_TEXTO = """
            /* Le uma linha de stdin para buf, sem o \n final. Aceita espacos.
               Devolve: 1 = linha completa, 0 = a entrada acabou,
               -1 = a linha nao coube em buf (o excedente foi descartado). */
            static int tc_ler_texto(char *buf, size_t tam) {
                int estado = 1;
                if (fgets(buf, (int)tam, stdin) == NULL) {
                    buf[0] = '\0';
                    return 0;
                }
                size_t n = strcspn(buf, "\n");
                if (buf[n] == '\n') {
                    buf[n] = '\0';
                } else if (!feof(stdin)) {
                    int c;
                    while ((c = getchar()) != '\n' && c != EOF) { }
                    estado = -1;
                }
                n = strlen(buf);
                if (n > 0 && buf[n - 1] == '\r') buf[n - 1] = '\0';
                return estado;
            }"""

        private const val AUX_EXIGIR = """
            /* Como tc_ler_texto, mas termina o programa se a entrada acabar. */
            static int tc_exigir_linha(char *buf, size_t tam) {
                int estado = tc_ler_texto(buf, tam);
                if (estado == 0) {
                    fputs("Fim da entrada.\n", stderr);
                    exit(1);
                }
                return estado;
            }"""

        private const val AUX_INTEIRO = """
            /* Le um inteiro entre minimo e maximo. Repete ate a entrada ser valida. */
            static long long tc_ler_inteiro(long long minimo, long long maximo) {
                char linha[64], *fim;
                for (;;) {
                    if (tc_exigir_linha(linha, sizeof(linha)) > 0) {
                        errno = 0;
                        long long v = strtoll(linha, &fim, 10);
                        if (fim != linha && errno == 0 && fim[strspn(fim, " \t")] == '\0' && v >= minimo && v <= maximo) return v;
                    }
                    printf("Entrada invalida, tente novamente: ");
                }
            }"""

        private const val AUX_REAL = """
            /* Le um numero real entre minimo e maximo (recusa nan e inf). Repete ate ser valido. */
            static long double tc_ler_real(long double minimo, long double maximo) {
                char linha[64], *fim;
                for (;;) {
                    if (tc_exigir_linha(linha, sizeof(linha)) > 0) {
                        errno = 0;
                        long double v = strtold(linha, &fim);
                        if (fim != linha && errno == 0 && fim[strspn(fim, " \t")] == '\0' && v >= minimo && v <= maximo) return v;
                    }
                    printf("Entrada invalida, tente novamente: ");
                }
            }"""

        private const val AUX_CARACTERE = """
            /* Le um caractere (o primeiro da linha). Repete se a linha vier vazia. */
            static char tc_ler_caractere(void) {
                char linha[64];
                for (;;) {
                    tc_exigir_linha(linha, sizeof(linha));
                    if (linha[0] != '\0') return linha[0];
                    printf("Entrada invalida, tente novamente: ");
                }
            }"""
    }
}
