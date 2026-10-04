# Passagem de Parâmetros em C, e a Ponte do PortugolTipado

Em C, por padrão, toda variável passada a uma função é passada **por valor**: a função recebe uma cópia, e qualquer alteração feita dentro dela desaparece assim que a função termina, sem afectar a variável original.

```c
void dobrar(int valor) {
    valor = valor * 2;  // altera apenas a copia local
}

int main() {
    int numero = 10;
    dobrar(numero);
    printf("%d\n", numero);  // continua a imprimir 10
    return 0;
}
```

## Passagem por referência: ponteiros

Para que uma função possa modificar a variável original, C exige o uso explícito de um **ponteiro** — o endereço de memória da variável, em vez do seu valor. Isto obriga a alterar tanto a assinatura da função como a forma como ela é chamada:

```c
void dobrar(int *valor) {
    *valor = *valor * 2;  // acede e altera o valor no endereco original
}

int main() {
    int numero = 10;
    dobrar(&numero);  // passa o endereco de numero, nao o seu valor
    printf("%d\n", numero);  // agora imprime 20
    return 0;
}
```

O operador `&` obtém o endereço de uma variável; o operador `*`, na assinatura da função, declara um ponteiro; e o mesmo `*`, dentro do corpo, acede ao valor guardado nesse endereço. É uma das áreas de C que mais confunde quem está a aprender — confundir `*` com `&`, ou esquecer um dos dois, é uma fonte comum de erros difíceis de depurar.

## A mesma lógica, em PortugolTipado

```portugol
funcao dobrar(altere inteiro valor): vazio
{
    // Modifica diretamente a variável original
    valor = valor * 2
}

funcao inicio()
{
    inteiro numero = 10
    escreva("Antes: ", numero, "\n")

    dobrar(numero) // Não é necessário usar símbolos especiais na chamada


    escreva("Depois: ", numero, "\n")
}
```

A palavra `altere`, colocada antes do tipo do parâmetro, diz ao compilador para gerar um ponteiro em C — exactamente como `int *valor` no exemplo acima. A diferença está na chamada: enquanto em C é preciso lembrar o `&` em `dobrar(&numero)`, em PortugolTipado a chamada mantém-se simples, `dobrar(numero)`, e é o compilador que trata da conversão para ponteiro ao gerar o código C.

## Porque isto importa

Entender a diferença entre passagem por valor e por referência é um dos passos mais importantes para programar em C com segurança. É o mecanismo por trás de funções que alteram múltiplos valores, estruturas de dados complexas, e grande parte do código de sistemas escrito em C. O `altere` do PortugolTipado não esconde este conceito — torna-o explícito e legível, preparando para o dia em que `*` e `&` aparecerem num código C puro.