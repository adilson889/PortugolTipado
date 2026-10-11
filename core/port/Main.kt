/*
 * PortugolTipado - CLI
 * Copyright (C) 2026  Adilson C. Rafael
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package co.adilson889.typec.port

import co.adilson889.typec.erros.ErroTypeC
import co.adilson889.typec.lexer.tokenizarFonte
import co.adilson889.typec.modulos.Resolvedor
import co.adilson889.typec.parser.Parser
import co.adilson889.typec.transpilador.Transpilador
import co.adilson889.typec.validador.Validador
import java.io.File
import kotlin.system.exitProcess

/**
 * CLI do motor PortugolTipado.
 *
 * Uso:
 *   port <ficheiro.port>            -> escreve o C no stdout
 *   port -o saida.c <ficheiro.port> -> escreve o C no ficheiro
 *   port -p <pasta> <ficheiro.port> -> pasta dos pacotes instalados (por omissao: $PORT_PACOTES,
 *                                      ou a pasta 'pacotes' ao lado do ficheiro)
 *   port -h                         -> ajuda
 *
 * Codigos de saida:
 *   0 -> sucesso
 *   1 -> erro (leitura, parsing, validacao, transpilacao)
 *   2 -> uso incorreto
 */
fun main(args: Array<String>) {
    var saida: String? = null
    var entrada: String? = null
    var pastaPacotes: String? = null
    var tipagemSeguraV2 = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-o", "--saida" -> {
                i++
                if (i < args.size) saida = args[i]
                else {
                    System.err.println("Falta o caminho depois de -o")
                    exitProcess(2)
                }
            }
            "-p", "--pacotes" -> {
                i++
                if (i < args.size) pastaPacotes = args[i]
                else {
                    System.err.println("Falta a pasta depois de -p")
                    exitProcess(2)
                }
            }
            "--tipagem=segura", "--tipagem=rigorosa" -> tipagemSeguraV2 = true
            "--tipagem=legada" -> tipagemSeguraV2 = false
            "-h", "--ajuda", "--help" -> {
                println("Uso: port [-o saida.c] [-p pasta_pacotes] <ficheiro.port>")
                exitProcess(0)
            }
            else -> {
                if (entrada == null) entrada = args[i]
                else {
                    System.err.println("Demasiados argumentos. Uso: port [-o saida.c] [-p pasta_pacotes] <ficheiro.port>")
                    exitProcess(2)
                }
            }
        }
        i++
    }

    if (entrada == null) {
        System.err.println("Uso: port [-o saida.c] [-p pasta_pacotes] <ficheiro.port>")
        exitProcess(2)
    }

    val fonte = try {
        File(entrada).readText()
    } catch (e: Exception) {
        System.err.println("Erro a ler '$entrada': ${e.message}")
        exitProcess(1)
    }

    val c = try {
        val tokens = tokenizarFonte(fonte)
        val programa = Parser(tokens, fonte).parsear()
        val pacotes = (pastaPacotes ?: System.getenv("PORT_PACOTES"))?.let { File(it) }
            ?: File(File(entrada).absoluteFile.parentFile, "pacotes")
        val resolucao = Resolvedor(pacotes).resolver(programa, fonte, File(entrada))
        Validador(fonte).validar(resolucao.programa, resolucao.modulos)
        if (tipagemSeguraV2) {
            co.adilson889.typec.validador.ChecagemSeguraV2(fonte)
                .validar(resolucao.programa, resolucao.modulos)
        }
        val codigo = Transpilador().transpilarUnico(resolucao.programa, resolucao.modulos)
        // O que o C gerado pede para ligar (ex: -lsqlite3), num comentario no topo
        if (resolucao.requisitos.isEmpty()) codigo
        else "/* requisitos: " + resolucao.requisitos.joinToString(" ") + " */\n" + codigo
    } catch (e: ErroTypeC) {
        System.err.println(e.formatar())
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println(e.message ?: "erro desconhecido")
        exitProcess(1)
    }

    if (saida != null) {
        try {
            File(saida).writeText(c)
        } catch (e: Exception) {
            System.err.println("Erro a escrever '$saida': ${e.message}")
            exitProcess(1)
        }
    } else {
        print(c)
    }
}