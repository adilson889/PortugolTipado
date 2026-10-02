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

import co.adilson889.typec.lexer.tokenizarFonte
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
            "-h", "--ajuda", "--help" -> {
                println("Uso: port [-o saida.c] <ficheiro.port>")
                exitProcess(0)
            }
            else -> {
                if (entrada == null) entrada = args[i]
                else {
                    System.err.println("Demasiados argumentos. Uso: port [-o saida.c] <ficheiro.port>")
                    exitProcess(2)
                }
            }
        }
        i++
    }

    if (entrada == null) {
        System.err.println("Uso: port [-o saida.c] <ficheiro.port>")
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
        Validador(fonte).validar(programa)
        Transpilador().transpilar(programa)
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