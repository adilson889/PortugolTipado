package co.adilson889.typec.terminal

/**
* Emulador de terminal ANSI simples, em Kotlin puro (sem Android/View).
* Recebe texto cru contendo códigos de escape ANSI (\u001B[...) e
* mantém uma grade de células (linha x coluna), cada uma com caractere,
* cor de texto e cor de fundo — exatamente como um terminal real faz.
*
* Suporta os códigos ANSI mais comuns (referência: ECMA-48 / VT100):
*  - \u001B[H ou \u001B[L;CH -> posiciona cursor (linha L, coluna C)
*  - \u001B[2J -> limpa a tela inteira
*  - \u001B[K  -> limpa da posição do cursor até o fim da linha
*  - \u001B[nA / nB / nC / nD -> move cursor cima/baixo/direita/esquerda
*  - \u001B[0m / [1m / [30-37m / [40-47m / [90-97m / [100-107m -> cores e reset
*  - \n, \r, \t, \b -> comportamento normal de terminal
*/

data class Celula(
var caractere: Char = ' ',
var corTexto: Int = CorAnsi.PADRAO,
var corFundo: Int = CorAnsi.PADRAO,
var negrito: Boolean = false
)

object CorAnsi {
const val PADRAO = -1
const val PRETO = 0
const val VERMELHO = 1
const val VERDE = 2
const val AMARELO = 3
const val AZUL = 4
const val MAGENTA = 5
const val CIANO = 6
const val BRANCO = 7
}

class TerminalANSI(
private val linhasIniciais: Int = 24,
val colunas: Int = 80,
private val maxLinhas: Int = 2000 // scrollback: passou disso, descarta as linhas mais antigas
) {
private val grade: ArrayList<Array<Celula>> = ArrayList<Array<Celula>>().also { g ->
repeat(linhasIniciais) { g.add(novaLinha()) }
}

/** Total de linhas do buffer (cresce conforme a saída passa de linhasIniciais) */
val linhas: Int get() = grade.size

var cursorLinha = 0
private set
var cursorColuna = 0
private set

// Maior linha onde algo foi escrito: a view usa isso pra não desenhar um bloco vazio embaixo
private var maxLinhaEscrita = 0

/** Quantas linhas têm conteúdo (ou o cursor) — é a altura que a view precisa desenhar */
val linhasUsadas: Int get() = maxOf(maxLinhaEscrita, cursorLinha) + 1

private var corTextoAtual = CorAnsi.PADRAO
private var corFundoAtual = CorAnsi.PADRAO
private var negritoAtual = false

// Trecho de uma sequência de escape que ficou cortada no fim de uma chamada a processar();
// é completado na chamada seguinte (útil se a saída chegar em pedaços).
private var pendente: String = ""

private fun novaLinha(): Array<Celula> = Array(colunas) { Celula() }

fun celula(linha: Int, coluna: Int): Celula = grade[linha][coluna]

/** Processa um texto inteiro (com ou sem escapes ANSI misturados) */
fun processar(texto: String) {
val entrada = if (pendente.isEmpty()) texto else pendente + texto
pendente = ""

var i = 0
while (i < entrada.length) {
val c = entrada[i]
when {
c == '\u001B' -> {
if (i + 1 >= entrada.length) {
// ESC solto no fim: guarda para a próxima chamada
pendente = entrada.substring(i)
return
}
if (entrada[i + 1] == '[') {
val fimSeq = acharFimSequencia(entrada, i + 2)
if (fimSeq >= entrada.length) {
// Sequência CSI cortada: guarda o resto para completar depois
pendente = entrada.substring(i)
return
}
val sequencia = entrada.substring(i + 2, fimSeq)
processarSequenciaCSI(sequencia, entrada[fimSeq])
i = fimSeq + 1
} else {
// ESC seguido de outra coisa (não suportado): descarta só o ESC
i++
}
}
c == '\n' -> {
cursorColuna = 0
novalinha()
i++
}
c == '\r' -> {
cursorColuna = 0
i++
}
c == '\t' -> {
cursorColuna = ((cursorColuna / 8) + 1) * 8
ajustarLimites()
i++
}
c == '\b' -> {
if (cursorColuna > 0) cursorColuna--
i++
}
c.isHighSurrogate() || c.isLowSurrogate() -> {
// Caracteres fora do BMP (emoji etc.) não cabem numa Celula de Char.
// Troca o par por um único '?' para não partir em duas células.
if (c.isHighSurrogate() && i + 1 < entrada.length && entrada[i + 1].isLowSurrogate()) {
escreverCaractere('?')
i += 2
} else {
i++
}
}
else -> {
escreverCaractere(c)
i++
}
}
}
}

// Uma sequência CSI termina num byte final entre '@' e '~'; parâmetros (dígitos, ';', '?')
// ficam antes. Devolve texto.length se a sequência estiver cortada.
private fun acharFimSequencia(texto: String, inicio: Int): Int {
var i = inicio
while (i < texto.length && texto[i] !in '@'..'~') i++
return i
}

private fun escreverCaractere(c: Char) {
if (cursorLinha in 0 until linhas && cursorColuna in 0 until colunas) {
grade[cursorLinha][cursorColuna] = Celula(c, corTextoAtual, corFundoAtual, negritoAtual)
if (cursorLinha > maxLinhaEscrita) maxLinhaEscrita = cursorLinha
}
cursorColuna++
if (cursorColuna >= colunas) {
cursorColuna = 0
novalinha()
}
}

private fun novalinha() {
cursorLinha++
// Em vez de rolar e perder a primeira linha, o buffer cresce
while (cursorLinha >= grade.size) grade.add(novaLinha())
// Só descarta o mais antigo quando passa do limite de scrollback
if (grade.size > maxLinhas) {
grade.removeAt(0)
cursorLinha--
maxLinhaEscrita = (maxLinhaEscrita - 1).coerceAtLeast(0)
}
}

private fun reiniciarTela() {
pendente = ""
grade.clear()
repeat(linhasIniciais) { grade.add(novaLinha()) }
cursorLinha = 0
cursorColuna = 0
maxLinhaEscrita = 0
}

private fun ajustarLimites() {
cursorLinha = cursorLinha.coerceIn(0, linhas - 1)
cursorColuna = cursorColuna.coerceIn(0, colunas - 1)
}

// -------------------------------------------------------------
// Processamento de sequências CSI: \u001B[<params><letra final>
// -------------------------------------------------------------

private fun processarSequenciaCSI(parametros: String, letraFinal: Char) {
// Sequências privadas (ex: ESC[?25l esconde cursor) não são suportadas: ignora
if (parametros.isNotEmpty() && parametros[0] in "?<=>") return

val partes = parametros.split(";").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }

when (letraFinal) {
'H', 'f' -> {
// Posiciona cursor: \u001B[linha;colunaH (1-indexado no padrão ANSI)
val linha = (partes.getOrNull(0) ?: 1) - 1
val coluna = (partes.getOrNull(1) ?: 1) - 1
cursorLinha = linha
cursorColuna = coluna
ajustarLimites()
}
'A' -> { cursorLinha -= (partes.getOrNull(0) ?: 1).coerceAtLeast(1); ajustarLimites() }
'B' -> { cursorLinha += (partes.getOrNull(0) ?: 1).coerceAtLeast(1); ajustarLimites() }
'C' -> { cursorColuna += (partes.getOrNull(0) ?: 1).coerceAtLeast(1); ajustarLimites() }
'D' -> { cursorColuna -= (partes.getOrNull(0) ?: 1).coerceAtLeast(1); ajustarLimites() }
'J' -> {
// \u001B[2J -> limpa tela inteira e volta o buffer ao tamanho inicial (redesenho de "frames")
val modo = partes.getOrNull(0) ?: 0
when (modo) {
0 -> {
// Do cursor até o fim da tela
for (c in cursorColuna until colunas) grade[cursorLinha][c] = Celula()
for (l in cursorLinha + 1 until linhas) grade[l] = novaLinha()
}
1 -> {
// Do início da tela até o cursor
for (l in 0 until cursorLinha) grade[l] = novaLinha()
for (c in 0..cursorColuna.coerceAtMost(colunas - 1)) grade[cursorLinha][c] = Celula()
}
2, 3 -> reiniciarTela()
}
}
'K' -> {
val modo = partes.getOrNull(0) ?: 0
when (modo) {
0 -> for (c in cursorColuna until colunas) grade[cursorLinha][c] = Celula()
1 -> for (c in 0..cursorColuna.coerceAtMost(colunas - 1)) grade[cursorLinha][c] = Celula()
2 -> grade[cursorLinha] = novaLinha()
}
}
'm' -> aplicarGraficos(partes)
else -> {} // sequência não suportada, ignora silenciosamente
}
}

private fun aplicarGraficos(codigos: List<Int>) {
if (codigos.isEmpty()) { resetarGraficos(); return }
for (codigo in codigos) {
when {
codigo == 0 -> resetarGraficos()
codigo == 1 -> negritoAtual = true
codigo == 22 -> negritoAtual = false
codigo in 30..37 -> corTextoAtual = codigo - 30
codigo == 39 -> corTextoAtual = CorAnsi.PADRAO
codigo in 40..47 -> corFundoAtual = codigo - 40
codigo == 49 -> corFundoAtual = CorAnsi.PADRAO
codigo in 90..97 -> corTextoAtual = codigo - 90 // cores "brilhantes", tratadas igual às normais aqui
codigo in 100..107 -> corFundoAtual = codigo - 100
else -> {}
}
}
}

private fun resetarGraficos() {
corTextoAtual = CorAnsi.PADRAO
corFundoAtual = CorAnsi.PADRAO
negritoAtual = false
}
}