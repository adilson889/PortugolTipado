// Treino de Lógica - modo texto (sem gráficos)
// Cada exercício é uma função. O menu no 'inicio' chama a que for escolhida.

// 1. Ciclo 'para': repete um número fixo de vezes
funcao tabuada(): vazio {
    inteiro n
    escreva("Tabuada de que número? ")
    leia(n)
    para (inteiro i = 1; i <= 10; i++) {
        inteiro resultado = n * i
        escreva(n, " x ", i, " = ", resultado, "\n")
    }
}

// 2. Condição 'se': o resto da divisão por 2 diz se é par
funcao par_ou_impar(): vazio {
    inteiro n
    escreva("Escreve um número inteiro: ")
    leia(n)
    se (n % 2 == 0) {
        escreva(n, " é par\n")
    } senao {
        escreva(n, " é ímpar\n")
    }
}

// 3. Variáveis reais e 'senao se'
funcao media_notas(): vazio {
    real n1
    real n2
    real n3
    escreva("Nota 1 (0 a 20): ")
    leia(n1)
    escreva("Nota 2 (0 a 20): ")
    leia(n2)
    escreva("Nota 3 (0 a 20): ")
    leia(n3)

    real media = (n1 + n2 + n3) / 3
    escreva("Média: ", media, "\n")

    se (media >= 14.0) {
        escreva("Resultado: Muito bom\n")
    } senao se (media >= 10.0) {
        escreva("Resultado: Aprovado\n")
    } senao {
        escreva("Resultado: Reprovado\n")
    }
}

// 4. Função que devolve 'logico': um número primo só se divide por 1 e por ele
funcao eh_primo(inteiro n): logico {
    se (n < 2) {
        retorne falso
    }
    para (inteiro d = 2; d * d <= n; d++) {
        se (n % d == 0) {
            retorne falso
        }
    }
    retorne verdadeiro
}

funcao listar_primos(): vazio {
    inteiro limite
    escreva("Primos até que número? ")
    leia(limite)
    para (inteiro n = 2; n <= limite; n++) {
        se (eh_primo(n)) {
            escreva(n, " ")
        }
    }
    escreva("\n")
}

// 5. Acumulador: multiplica 1 x 2 x 3 x ... x n
funcao fatorial(inteiro n): inteiro {
    inteiro resultado = 1
    para (inteiro i = 2; i <= n; i++) {
        resultado = resultado * i
    }
    retorne resultado
}

funcao calcular_fatorial(): vazio {
    inteiro n
    escreva("Fatorial de (0 a 12): ")
    leia(n)
    se (n < 0 || n > 12) {
        escreva("Número fora do limite.\n")
    } senao {
        inteiro f = fatorial(n)
        escreva(n, "! = ", f, "\n")
    }
}

// 6. Vários 'senao se' no mesmo ciclo
funcao fizzbuzz(): vazio {
    para (inteiro i = 1; i <= 30; i++) {
        se (i % 15 == 0) {
            escreva("FizzBuzz\n")
        } senao se (i % 3 == 0) {
            escreva("Fizz\n")
        } senao se (i % 5 == 0) {
            escreva("Buzz\n")
        } senao {
            escreva(i, "\n")
        }
    }
}

// 7. Arrays: guardar valores e percorrê-los para achar o maior
funcao maior_de_cinco(): vazio {
    inteiro numeros[5]
    para (inteiro i = 0; i < 5; i++) {
        inteiro posicao = i + 1
        inteiro valor
        escreva("Número ", posicao, ": ")
        leia(valor)
        numeros[i] = valor
    }

    inteiro maior = numeros[0]
    para (inteiro i = 1; i < 5; i++) {
        se (numeros[i] > maior) {
            maior = numeros[i]
        }
    }
    escreva("O maior é ", maior, "\n")
}

// 8. 'faca ... enquanto' e busca binária: o computador adivinha o teu número
funcao computador_adivinha(): vazio {
    inteiro baixo = 1
    inteiro alto = 100
    inteiro palpite = 0
    inteiro tentativas = 0
    caractere resposta = 'x'

    escreva("Pensa num número entre 1 e 100. Eu tento adivinhar.\n")
    faca {
        palpite = (baixo + alto) / 2
        tentativas++
        escreva("É o ", palpite, "? (m = o meu é maior, n = menor, c = certo): ")
        leia(resposta)
        se (resposta == 'm') {
            baixo = palpite + 1
        } senao se (resposta == 'n') {
            alto = palpite - 1
        }
    } enquanto (resposta != 'c' && baixo <= alto)

    se (resposta == 'c') {
        escreva("Acertei em ", tentativas, " tentativas!\n")
    } senao {
        escreva("As tuas respostas não batem certo.\n")
    }
}

inicio() {
    inteiro opcao = 0

    faca {
        escreva("\n=== Treino de Lógica ===\n")
        escreva("1 - Tabuada\n")
        escreva("2 - Par ou ímpar\n")
        escreva("3 - Média de notas\n")
        escreva("4 - Números primos\n")
        escreva("5 - Fatorial\n")
        escreva("6 - FizzBuzz\n")
        escreva("7 - Maior de cinco números\n")
        escreva("8 - O computador adivinha\n")
        escreva("0 - Sair\n")
        escreva("Escolha: ")
        leia(opcao)

        escolha (opcao) {
            caso 1:
                tabuada()
                pare
            caso 2:
                par_ou_impar()
                pare
            caso 3:
                media_notas()
                pare
            caso 4:
                listar_primos()
                pare
            caso 5:
                calcular_fatorial()
                pare
            caso 6:
                fizzbuzz()
                pare
            caso 7:
                maior_de_cinco()
                pare
            caso 8:
                computador_adivinha()
                pare
            caso 0:
                escreva("Até breve!\n")
                pare
            casocontrario:
                escreva("Opção inválida.\n")
        }
    } enquanto (opcao != 0)
}