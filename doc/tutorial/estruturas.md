# Estruturas em C — e como o PortugolTipado te leva até lá

Em C, uma `struct` permite agrupar várias variáveis de tipos diferentes debaixo de um único nome, criando um tipo de dados novo e personalizado. Isto resolve um problema real: sem `struct`, representar um produto exigiria três variáveis soltas (`nome`, `preco`, `estoque`) sem nenhuma relação explícita entre elas no código — e nada impediria de, por engano, passar o preço de um produto com o estoque de outro.

```c
struct Produto {
    char* nome;
    float preco;
    int estoque;
};
```

Esta declaração não cria nenhuma variável ainda — define apenas o **molde**: a partir de agora, `struct Produto` é um tipo válido, tal como `int` ou `float` já são.

## Criando e inicializando uma variável do tipo struct

Para criar uma variável deste tipo, e já atribuir valores a cada campo na ordem em que foram declarados:

```c
struct Produto p = {"Teclado", 150.50, 20};
```

Isto atribui `"Teclado"` a `nome`, `150.50` a `preco`, e `20` a `estoque`, pela ordem da declaração da `struct`.

## Acedendo aos campos: o operador `.`

Para ler ou escrever um campo específico de uma struct, C usa o operador ponto (`.`):

```c
printf("Produto: %s\n", p.nome);
printf("Preco: Kz %f\n", p.preco);
```

## Alterando um campo

Um campo de uma struct pode ser alterado como qualquer variável normal, desde que a struct em si não seja `const`:

```c
p.estoque = p.estoque - 1;
printf("Estoque atualizado: %d\n", p.estoque);
```

## A mesma lógica, em PortugolTipado

```portugol
estrutura Produto {
    texto nome
    real preco
    inteiro estoque
}

funcao inicio()
{
    estrutura Produto p = {"Teclado", 150.50, 20}

    escreva("Produto: ", p.nome, "\n")
    escreva("Preço: Kz ", p.preco, "\n")

    // Alterando um campo
    p.estoque = p.estoque - 1
    escreva("Estoque atualizado: ", p.estoque, "\n")
}
```

A correspondência é directa, campo a campo: `estrutura` declara o molde exactamente como `struct` faz em C; a inicialização entre chaves `{...}` segue a mesma ordem posicional; e o acesso com `.` é idêntico nas duas linguagens — porque, ao compilar, é literalmente o mesmo operador `.` de C que aparece no código gerado.

## Porque isto importa

Uma `struct` é o primeiro passo para modelar dados do mundo real dentro de um programa: um produto, um cliente, uma coordenada, um registo de aluno. Sem esta ferramenta, um programa fica reduzido a variáveis soltas e sem relação visível entre si; com ela, o código passa a espelhar a forma como se pensa sobre o problema. Dominar `struct` em PortugolTipado é dominar a mesma ferramenta que sustenta sistemas C inteiros — desde um simples cadastro até ao núcleo de um sistema operativo.