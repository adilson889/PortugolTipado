O controle de fluxo permite ao programa tomar decisões com base em condições. O PortugolTipado suporta `se`, `senao se`, `senao` e a estrutura de múltiplas opções `escolha` / `caso`.

```portugol
funcao inicio()
{
    inteiro nota = 14

    se (nota >= 10) {
        escreva("Aluno aprovado!\n")
    } senao se (nota >= 7) {
        escreva("Aluno em recurso.\n")
    } senao {
        escreva("Aluno reprovado.\n")
    }

    inteiro opcao = 2
    escolha (opcao) {
        caso 1:
            escreva("Opção 1 selecionada\n")
            pare
        caso 2:
            escreva("Opção 2 selecionada\n")
            pare
        casocontrario:
            escreva("Opção inválida\n")
    }
}
```
