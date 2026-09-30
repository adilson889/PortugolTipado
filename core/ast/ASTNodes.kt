package co.adilson889.typec.ast

/**
* Nó base de toda a AST do PortugolTipado 
* Cada nó guarda a linha original do .port para rastrear erros de volta.
*/
sealed class No {
    abstract val linha: Int
}

// ---------------------------------------------------------------------
// Tipos
// ---------------------------------------------------------------------

enum class TipoDado {
    INTEIRO,
    REAL,
    DUPLO,
    TEXTO,
    CARACTERE,
    LOGICO,
    VAZIO,
    INTEIRO_LONGO,
    INTEIRO_CURTO,
    INTEIRO_POSITIVO,
    INTEIRO_GIGANTE,
    DUPLO_LONGO,
    TEMPO
}

/** Representa um tipo, incluindo se é array (ex: inteiro[]) */
data class Tipo(
    val base: TipoDado,
    val ehArray: Boolean = false,
    val nomeStruct: String? = null, // usado quando base é uma struct definida pelo usuário
    val tamanhoFixo: Int? = null, // usado em campo array de struct, ex: inteiro notas[10] -> tamanhoFixo = 10
    val ehArray2D: Boolean = false, // true para "inteiro[][]" — ver Array2D.kt
    val tamanhoFixo2: Int? = null // segunda dimensão, ex: "matriz[2][2]" -> tamanhoFixo=2, tamanhoFixo2=2 (ver Array2D.kt)
)

// ---------------------------------------------------------------------
// Programa
// ---------------------------------------------------------------------

data class Programa(
    val includes: List<Inclua>,
    val declaracoesGlobais: List<No>, // funções, structs, no futuro
    val inicio: BlocoInicio?, // null quando o arquivo é um módulo (só biblioteca, sem ponto de entrada)
    override val linha: Int = 1
) : No()

data class Inclua(
    val nomeLib: String, // ex: "stdlib", "ctype" (lib padrao) ou "base/core/base_core" (arquivo local, sem extensao)
    val ehArquivoLocal: Boolean, // true = veio entre aspas (inclua "caminho/arquivo"), false = lib padrao (inclua stdlib)
    override val linha: Int
) : No()

data class BlocoInicio(
    val comandos: List<No>,
    override val linha: Int
) : No()

// ---------------------------------------------------------------------
// Declarações
// ---------------------------------------------------------------------

data class DeclaracaoVariavel(
    val tipo: Tipo,
    val nome: String,
    val valorInicial: No?, // pode ser null (ex: "inteiro idade" sem valor, pra usar com ler())
    override val linha: Int,
    val ehConstante: Boolean = false // true = declarada com 'const' (gera "const" no C; exige valor inicial)
) : No()

data class DeclaracaoFuncao(
    val tipoRetorno: Tipo,
    val nome: String,
    val parametros: List<Parametro>,
    val corpo: List<No>?, // null = declaração externa, sem implementação (função já existe em lib incluída via 'inclua')
    val nomeReal: String? = null, // alias: 'funcao apelido(...): tipo = NomeReal' -> nome real chamado no C gerado
    override val linha: Int
) : No()

data class Parametro(
    val tipo: Tipo,
    val nome: String,
    val ehAlterar: Boolean, // true = passagem por referência (palavra "alterar")
    override val linha: Int,
    val ehConstante: Boolean = false // true = parâmetro 'const' (não pode ser combinado com "alterar")
) : No()

data class DeclaracaoStruct(
    val nome: String,
    val campos: List<CampoStruct>,
    override val linha: Int
) : No()

data class CampoStruct(
    val tipo: Tipo,
    val nome: String,
    override val linha: Int
) : No()

// ---------------------------------------------------------------------
// Expressões
// ---------------------------------------------------------------------

data class Numero(
    val valor: String, // guardado como texto, o transpilador decide o literal certo (10, 10.0, 10.0f)
    val tipo: TipoDado,
    override val linha: Int
) : No()

data class Texto(
    val valor: String,
    override val linha: Int
) : No()

data class Caractere(
    val valor: Char,
    override val linha: Int
) : No()

data class Logico(
    val valor: Boolean,
    override val linha: Int
) : No()

data class Identificador(
    val nome: String,
    override val linha: Int
) : No()

data class OperacaoBinaria(
    val esquerda: No,
    val operador: String, // "+", "-", "*", "/", "%", "==", "!=", ">", "<", ">=", "<=", "&&", "||"
    val direita: No,
    override val linha: Int
) : No()

data class OperacaoUnaria(
    val operador: String, // "!", "-"
    val operando: No,
    override val linha: Int
) : No()

data class Atribuicao(
    val alvo: No, // Identificador ou AcessoIndice ou AcessoCampo
    val operador: String, // "=", "+=", "-=", "*=", "/=", "%="
    val valor: No,
    override val linha: Int
) : No()

data class IncrementoDecremento(
    val alvo: No,
    val operador: String, // "++", "--"
    override val linha: Int
) : No()

data class ChamadaFuncao(
    val nome: String,
    val argumentos: List<No>,
    override val linha: Int
) : No()

data class AcessoIndice(
    val array: No,
    val indice: No,
    override val linha: Int
) : No()

data class AcessoCampo(
    val objeto: No,
    val campo: String,
    override val linha: Int
) : No()

data class ArrayLiteral(
    val valores: List<No>,
    override val linha: Int
) : No()

// ---------------------------------------------------------------------
// Comandos
// ---------------------------------------------------------------------

data class ComandoImprimir(
    val argumentos: List<No>,
    override val linha: Int
) : No()

data class ComandoLer(
    val alvo: No,
    override val linha: Int
) : No()

data class ComandoRetorna(
    val valor: No?,
    override val linha: Int
) : No()

data class ComandoSe(
    val condicao: No,
    val entao: List<No>,
    val senaoSe: List<Par<No, List<No>>>, // lista de (condição, bloco) para "senao se"
    val senao: List<No>?,
    override val linha: Int
) : No()

/** Par simples, evita depender de kotlin.Pair em construções complexas */
data class Par<A, B>(val primeiro: A, val segundo: B)

data class ComandoEnquanto(
    val condicao: No,
    val corpo: List<No>,
    override val linha: Int
) : No()

data class ComandoPara(
    val inicializacao: No?, // DeclaracaoVariavel ou Atribuicao
    val condicao: No?,
    val incremento: No?, // Atribuicao ou IncrementoDecremento
    val corpo: List<No>,
    override val linha: Int
) : No()

data class ComandoParaCada(
    val tipoElemento: Tipo,
    val nomeElemento: String,
    val array: No,
    val corpo: List<No>,
    override val linha: Int
) : No()

data class ComandoDispensar(override val linha: Int) : No() // break
data class ComandoIgnorar(override val linha: Int) : No()   // continue

data class ComandoEscolher(
    val valor: No,
    val casos: List<CasoEscolher>,
    val padrao: List<No>?,
    override val linha: Int
) : No()

data class CasoEscolher(
    val valor: No,
    val corpo: List<No>,
    override val linha: Int
) : No()

data class ExpressaoComando(
    val expressao: No, // ex: chamada de função usada como comando (dobrar(numero))
    override val linha: Int
) : No()
