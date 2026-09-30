package co.adilson889.typec.parser

import co.adilson889.typec.ast.ArrayLiteral
import co.adilson889.typec.ast.No
import co.adilson889.typec.ast.Tipo
import co.adilson889.typec.lexer.Token
import co.adilson889.typec.lexer.TipoToken

/**
* Suporte a arrays de 2 dimensoes (matrizes), ex: "inteiro[][] matriz".
* Isolado do Parser principal — feature nova, ver CHANGELOG.
*
* Cobre so 2D por enquanto (caso real e documentado na secao 10 da doc
* oficial); C suporta mais dimensoes, mas 2D cobre matriz/tabela, que e
* o uso comum. Tamanho e sempre inferido do literal de inicializacao,
* igual ja acontece com array 1D — nunca exigido na declaracao.
*
* As funcoes de deteccao recebem o estado de navegacao do Parser
* (token atual/proximo, avancar) como parametros simples, sem herdar
* nem duplicar a logica de navegacao — o Parser principal continua
* dono dos tokens.
*/
object Array2D {

    /**
     * Chamar logo depois de um tipo já ter sido detectado como array 1D
     * (tipo.ehArray == true), verificando se vem um segundo "[]" em
     * seguida (ex: "inteiro[][]"). Se houver, consome os dois tokens via
     * 'avancar' e retorna o tipo com ehArray2D = true.
     */
    fun tentarConsumirSegundoColchete(
        tipo: Tipo,
        atual: () -> Token,
        proximo: () -> Token,
        avancar: () -> Token
    ): Tipo {
        if (!tipo.ehArray) return tipo
        if (atual().tipo == TipoToken.COLCHETE_ESQ && proximo().tipo == TipoToken.COLCHETE_DIR) {
            avancar(); avancar()
            return tipo.copy(ehArray2D = true)
        }
        return tipo
    }

    /**
     * Chamar depois de já ter lido "nome[n]" (tamanhoFixo preenchido) numa
     * declaração de variável. Detecta um segundo "[m]" numérico em seguida
     * (ex: "matriz[2][2]") e retorna o tipo com ehArray2D = true e
     * tamanhoFixo2 = m. Suportado para paridade com C, embora o uso comum
     * seja deixar o tamanho ser inferido do literal de inicialização.
     */
    fun tentarConsumirSegundoTamanho(
        tipo: Tipo,
        atual: () -> Token,
        proximo: () -> Token,
        avancar: () -> Token
    ): Tipo {
        if (tipo.tamanhoFixo == null) return tipo
        if (atual().tipo == TipoToken.COLCHETE_ESQ && proximo().tipo == TipoToken.NUMERO_INTEIRO) {
            avancar() // consome '['
            val tamanhoToken = avancar() // número
            avancar() // consome ']'
            return tipo.copy(ehArray2D = true, tamanhoFixo2 = tamanhoToken.texto.toInt())
        }
        return tipo
    }

    /**
     * Extrai (linhas, colunas) de um ArrayLiteral aninhado válido, ex:
     * {{1, 2}, {3, 4}} -> Dimensoes2D(2, 2). Usado pelo Transpilador para
     * inferir o tamanho quando a declaração não trouxe tamanho explícito.
     * Retorna null se a estrutura não for retangular (nem toda linha é
     * array, ou linhas de tamanhos diferentes entre si).
     */
    fun inferirDimensoes(valores: List<No>): Dimensoes2D? {
        if (valores.isEmpty()) return null
        val primeiraLinha = valores[0] as? ArrayLiteral ?: return null
        val colunas = primeiraLinha.valores.size
        for (linha in valores) {
            val linhaArray = linha as? ArrayLiteral ?: return null
            if (linhaArray.valores.size != colunas) return null
        }
        return Dimensoes2D(valores.size, colunas)
    }

    data class Dimensoes2D(val linhas: Int, val colunas: Int)

    /**
     * Gera a linha de declaração em C para uma variável array 2D, dado o
     * tipo em C já traduzido (ex: "int"), o nome da variável, o Tipo (para
     * ler tamanhoFixo/tamanhoFixo2 se houver) e a expressão já transpilada
     * de cada valor do literal (se houver inicializador). Espelha o
     * comportamento de array 1D: tamanho no C só aparece se foi declarado
     * explicitamente ou pôde ser inferido do literal; senão fica em branco
     * (`[]`) e o compilador C infere sozinho, igual já acontece hoje em 1D.
     *
     * 'transpilarLinha' transpila um ArrayLiteral de uma linha da matriz
     * em "v1, v2, v3" (delegado ao Transpilador principal, que já sabe
     * transpilar expressões — evita duplicar essa lógica aqui).
     */
    fun transpilarDeclaracao(
        tipoC: String,
        nomeVar: String,
        tipo: Tipo,
        valorInicial: No?,
        transpilarLinha: (ArrayLiteral) -> String
    ): String {
        val linhasLiteral = (valorInicial as? ArrayLiteral)?.valores
        val dimensoesInferidas = linhasLiteral?.let { inferirDimensoes(it) }

        val colcheteLinhas = tipo.tamanhoFixo?.toString()
            ?: dimensoesInferidas?.linhas?.toString()
            ?: ""
        val colcheteColunas = tipo.tamanhoFixo2?.toString()
            ?: dimensoesInferidas?.colunas?.toString()
            ?: ""

        val declaracao = "$tipoC $nomeVar[$colcheteLinhas][$colcheteColunas]"

        if (linhasLiteral == null) {
            return "$declaracao;\n"
        }

        val linhasC = linhasLiteral.joinToString(", ") { linha ->
            val linhaArray = linha as? ArrayLiteral
                ?: return@joinToString "/* TODO: linha de matriz mal formada */"
            "{${transpilarLinha(linhaArray)}}"
        }
        return "$declaracao = {$linhasC};\n"
    }
}
