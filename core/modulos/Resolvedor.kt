package co.adilson889.typec.modulos

import co.adilson889.typec.ast.*
import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.erros.extrairLinha
import co.adilson889.typec.graficos.LibGraficos
import co.adilson889.typec.lexer.tokenizarFonte
import co.adilson889.typec.parser.Parser
import co.adilson889.typec.transpilador.BibliotecaPadrao
import co.adilson889.typec.validador.Validador
import java.io.File

class ResultadoResolucao(
    val programa: Programa,
    val modulos: List<Programa>,
    val requisitos: List<String>
)

class Pacote(
    val nome: String,
    val versao: String,
    val pasta: File,
    val cabecalho: String?,
    val requisitos: List<String>,
    val declaracoes: List<String>
)

/**
 * Sub-motor de modulos: fica ANTES do validador e do transpilador e so lhes
 * entrega a lista de 'modulos' que eles ja sabem receber.
 *
 * 'inclua nome' (sem aspas): 1) lib padrao fica como esta; 2) pacote instalado
 * em <pastaPacotes>/<nome>/ carrega as declaracoes; 3) outro nome: C cru.
 * 'inclua "caminho"': ficheiro .port relativo a quem inclui;
 * 'inclua "nome.declare"' carrega nome.declare.port.
 */
class Resolvedor(private val pastaPacotes: File?) {

    private val carregados = LinkedHashMap<String, Programa>()
    private val pilha = ArrayList<String>()
    private val requisitos = LinkedHashSet<String>()

    fun resolver(programa: Programa, fonte: String, ficheiro: File): ResultadoResolucao {
        carregados.clear()
        pilha.clear()
        requisitos.clear()
        val principal = resolverIncludes(programa, fonte, ficheiro.absoluteFile.parentFile)
        return ResultadoResolucao(principal, carregados.values.toList(), requisitos.toList())
    }

    private fun resolverIncludes(programa: Programa, fonte: String, pasta: File?): Programa {
        val novos = ArrayList<Inclua>()
        for (inc in programa.includes) {
            if (inc.ehArquivoLocal) {
                carregarLocal(inc, fonte, pasta)
                novos.add(inc)
            } else if (ehBibliotecaPadrao(inc.nomeLib)) {
                novos.add(inc)
            } else {
                val pacote = acharPacote(inc.nomeLib)
                if (pacote == null) {
                    novos.add(inc)
                } else {
                    carregarPacote(pacote, inc, fonte)
                    if (pacote.cabecalho != null) novos.add(Inclua(pacote.cabecalho, false, inc.linha))
                }
            }
        }
        return Programa(novos, programa.declaracoesGlobais, programa.inicio, programa.linha)
    }

    private fun carregarLocal(inc: Inclua, fonteQuemInclui: String, pasta: File?) {
        val nome = inc.nomeLib
        val ficheiro = File(pasta, "$nome.port")
        if (!ficheiro.isFile) {
            throw ErroTypeC(
                "módulo '$nome' não encontrado (procurei '${ficheiro.path}')",
                inc.linha, extrairLinha(fonteQuemInclui, inc.linha), nome
            )
        }
        carregarFicheiro(ficheiro, ficheiro.name, inc, fonteQuemInclui)
    }

    private fun carregarFicheiro(ficheiro: File, nomeParaErros: String, inc: Inclua, fonteQuemInclui: String) {
        val chave = ficheiro.canonicalPath
        if (chave in carregados) return
        if (chave in pilha) {
            throw ErroTypeC(
                "inclusão em ciclo: ${(pilha.map { File(it).name } + ficheiro.name).joinToString(" -> ")}",
                inc.linha, extrairLinha(fonteQuemInclui, inc.linha), inc.nomeLib
            )
        }
        pilha.add(chave)
        try {
            val texto = ficheiro.readText()
            val modulo = try {
                val bruto = Parser(tokenizarFonte(texto), texto, nomeParaErros).parsear()
                val resolvido = resolverIncludes(bruto, texto, ficheiro.absoluteFile.parentFile)
                val submodulos = ArrayList<Programa>()
                for (sub in resolvido.includes.filter { it.ehArquivoLocal }) {
                    carregados[File(ficheiro.absoluteFile.parentFile, sub.nomeLib + ".port").canonicalPath]?.let { submodulos.add(it) }
                }
                Validador(texto, nomeParaErros).validar(resolvido, submodulos)
                resolvido
            } catch (e: ErroTypeC) {
                if (e.mensagem.startsWith("[")) throw e
                throw ErroTypeC("[em $nomeParaErros] ${e.mensagem}", e.linha, e.contexto, e.palavra, e.sugestao)
            }
            carregados[chave] = modulo
        } finally {
            pilha.removeAt(pilha.size - 1)
        }
    }

    private fun ehBibliotecaPadrao(nome: String): Boolean {
        val n = nome.lowercase()
        return n == LibGraficos.NOME_INCLUDE || BibliotecaPadrao.values().any { it.nomePort == n }
    }

    private fun acharPacote(nome: String): Pacote? {
        val raiz = pastaPacotes ?: return null
        if (!nome.matches(Regex("[A-Za-z0-9_]+"))) return null
        val base = File(raiz, nome)
        if (!base.isDirectory) return null
        val pasta = if (File(base, "pacote.json").isFile) base else {
            base.listFiles { f -> f.isDirectory && File(f, "pacote.json").isFile }
                ?.maxWithOrNull { a, b -> compararVersoes(a.name, b.name) }
        } ?: return null
        return lerPacote(nome, pasta)
    }

    private fun lerPacote(nome: String, pasta: File): Pacote {
        val json = try {
            MiniJson(File(pasta, "pacote.json").readText()).ler() as? Map<*, *>
        } catch (e: Exception) { null }
            ?: throw ErroTypeC("o 'pacote.json' de '$nome' é inválido", 1, "pacote.json", nome)
        return Pacote(
            nome = nome,
            versao = json["versao"]?.toString() ?: "0",
            pasta = pasta,
            cabecalho = (json["cabecalho"] as? String)?.takeIf { it.isNotBlank() },
            requisitos = (json["requisitos"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            declaracoes = (json["declara"] as? List<*>)?.map { it.toString() } ?: listOf("$nome.declare.port")
        )
    }

    private fun carregarPacote(pacote: Pacote, inc: Inclua, fonteQuemInclui: String) {
        for (nomeDecl in pacote.declaracoes) {
            val ficheiro = File(pacote.pasta, nomeDecl)
            if (!ficheiro.isFile || !ficheiro.canonicalPath.startsWith(pacote.pasta.canonicalPath + File.separator)) {
                throw ErroTypeC(
                    "o pacote '${pacote.nome}' declara '$nomeDecl', mas esse ficheiro não existe no pacote",
                    inc.linha, extrairLinha(fonteQuemInclui, inc.linha), pacote.nome
                )
            }
            carregarFicheiro(ficheiro, "${pacote.nome}/$nomeDecl", inc, fonteQuemInclui)
        }
        requisitos.addAll(pacote.requisitos)
    }

    private fun compararVersoes(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}

/** JSON minimo (o motor nao depende de nenhuma biblioteca externa). */
internal class MiniJson(private val s: String) {
    private var i = 0

    fun ler(): Any? {
        val v = valor()
        espaco()
        if (i != s.length) throw IllegalArgumentException("lixo depois do JSON")
        return v
    }

    private fun espaco() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun valor(): Any? {
        espaco()
        if (i >= s.length) throw IllegalArgumentException("JSON incompleto")
        return when (s[i]) {
            '{' -> objeto()
            '[' -> lista()
            '"' -> texto()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> numero()
        }
    }

    private fun literal(txt: String, v: Any?): Any? {
        if (!s.startsWith(txt, i)) throw IllegalArgumentException("valor invalido")
        i += txt.length
        return v
    }

    private fun numero(): Any {
        val ini = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        if (ini == i) throw IllegalArgumentException("valor invalido")
        return s.substring(ini, i).toDouble()
    }

    private fun texto(): String {
        i++
        val sb = StringBuilder()
        while (i < s.length && s[i] != '"') {
            if (s[i] == '\\' && i + 1 < s.length) {
                i++
                when (s[i]) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                    'u' -> { sb.append(s.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }
                    else -> sb.append(s[i])
                }
            } else sb.append(s[i])
            i++
        }
        if (i >= s.length) throw IllegalArgumentException("texto sem fim")
        i++
        return sb.toString()
    }

    private fun lista(): List<Any?> {
        i++
        val out = ArrayList<Any?>()
        espaco()
        if (i < s.length && s[i] == ']') { i++; return out }
        while (true) {
            out.add(valor())
            espaco()
            if (i < s.length && s[i] == ',') { i++; continue }
            if (i < s.length && s[i] == ']') { i++; return out }
            throw IllegalArgumentException("lista mal formada")
        }
    }

    private fun objeto(): Map<String, Any?> {
        i++
        val out = LinkedHashMap<String, Any?>()
        espaco()
        if (i < s.length && s[i] == '}') { i++; return out }
        while (true) {
            espaco()
            if (i >= s.length || s[i] != '"') throw IllegalArgumentException("chave esperada")
            val k = texto()
            espaco()
            if (i >= s.length || s[i] != ':') throw IllegalArgumentException("':' esperado")
            i++
            out[k] = valor()
            espaco()
            if (i < s.length && s[i] == ',') { i++; continue }
            if (i < s.length && s[i] == '}') { i++; return out }
            throw IllegalArgumentException("objeto mal formado")
        }
    }
}