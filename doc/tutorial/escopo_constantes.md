# Escopo e Constantes em C.
 Como o PortugolTipado te leva até lá

Em C, o escopo determina onde uma variável existe e pode ser acedida durante a execução do programa. Uma variável declarada fora de qualquer função tem escopo **global**: existe durante toda a vida do programa e é visível a partir de qualquer função. Uma variável declarada dentro de uma função tem escopo **local**: só existe enquanto essa função está a ser executada, e desaparece assim que a função termina.

```c
int limite = 100;  // variavel global

void funcao_exemplo() {
    int valor = 50;  // variavel local, so existe aqui dentro
}
```

Se duas variáveis com o mesmo nome existirem em escopos diferentes (uma global, outra local dentro de uma função), a variável local **tem prioridade** dentro dessa função — o compilador usa sempre a mais próxima.

## Constantes: `const`

C permite marcar uma variável como `const`, o que diz ao compilador que o seu valor nunca deve mudar depois de inicializada. Tentar alterar uma `const` é um erro detectado em tempo de compilação, não em tempo de execução — ou seja, o programa nem chega a compilar, o que evita que o erro passe despercebido:

```c
const int LIMITE = 100;
const float PI = 3.14159;

// LIMITE = 200;  // erro de compilacao: assignment of read-only variable
```

Usar `const` sempre que um valor não deve mudar é uma boa prática em C: comunica intenção ao próprio compilador, que passa a garantir essa regra por ti.

## A mesma lógica, em PortugolTipado

```portugol
// Variável global constante
const inteiro LIMITE = 100
const real PI = 3.14159

funcao inicio()
{
    // Variável local
    inteiro valor = 50

    se (valor < LIMITE) {
        escreva("Dentro do limite permitido.\n")
    }

    escreva("Valor de PI: ", PI, "\n")

    // LIMITE = 200 // Erro: Não se pode alterar uma constante
}
```

`LIMITE` e `PI` são globais porque estão declaradas fora de qualquer `funcao`, exactamente como em C. `valor` é local a `inicio()`, e desaparece quando a função termina. A linha comentada no fim, se descomentada, geraria o mesmo erro de compilação que geraria em C puro — o compilador do PortugolTipado aplica a mesma regra de `const`, só que detecta o erro antes mesmo de chegar a gerar o código C.

## Porque isto importa

Escopo e `const` não são detalhes de sintaxe — são decisões de desenho que evitam classes inteiras de bugs: variáveis que mudam de valor sem se perceber onde, ou constantes que deixam de ser constantes a meio do programa. Aprender a declarar correctamente o escopo e a usar `const` em PortugolTipado é aprender a disciplina que qualquer programador C experiente aplica por hábito.