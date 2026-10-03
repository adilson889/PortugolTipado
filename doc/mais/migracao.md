# Do Portugol para o PortugolTipado

O PortugolTipado está para o Portugol como o TypeScript está para o JavaScript: a mesma ideia, com sintaxe própria e tipos. Se já programas em Portugol, a migração é feita com poucas trocas, e quase todas são mecânicas.

Os comandos usam o imperativo: em vez de `g.limpar()`, escreves `limpe()`.

## Equivalências

- `programa { ... }` passa a **ficheiro solto**, sem o bloco `programa`.
- `inclua biblioteca Graficos --> g` passa a `inclua graficos`.
- `g.desenhar_linha(...)` passa a `desenhe_linha(...)`.
- `g.definir_cor(0xFF0000)` passa a `defina_cor(255, 0, 0)`.
- `g.limpar()` passa a `limpe()`.
- `g.renderizar()` passa a `renderize()`.
- `funcao real f(real x)` passa a `funcao f(real x) : real`.

## Passo a passo

1. **Tira o bloco `programa`.** No PortugolTipado o ficheiro não é embrulhado em `programa { ... }`. As funções e estruturas ficam diretamente no ficheiro.
2. **Troca o `inclua` da biblioteca gráfica.** Em vez de `inclua biblioteca Graficos --> g`, usa apenas `inclua graficos`. Deixa de existir o apelido `g`.
3. **Remove o prefixo `g.`** e passa os comandos gráficos ao imperativo: `desenhe_linha`, `defina_cor`, `limpe`, `renderize`.
4. **Converte as cores.** O Portugol aceita um valor hexadecimal, como `0xFF0000`. No PortugolTipado indicas as três componentes, de 0 a 255, e o vermelho fica `defina_cor(255, 0, 0)`.
5. **Põe o tipo de retorno no fim da função.** `funcao real f(real x)` passa a `funcao f(real x) : real`.
6. **Muda a extensão do ficheiro** de `.por` para `.port`.

## Um exemplo

Antes, em Portugol:

```
funcao real dobro(real x) {
    retorne x * 2
}
```

Depois, em PortugolTipado:

```
funcao dobro(real x) : real {
    retorne x * 2
}
```

O corpo da função não muda. Só a assinatura passa a ter o tipo de retorno depois dos parâmetros.

## Tipos

Os tipos básicos são `inteiro`, `real`, `texto` e `logico`. O editor reconhece ainda `duplo`, `caractere`, `vazio` e `tempo`.

```
inteiro idade = 16
real media = 9.5
```

## Ficheiros e extensões

- Os ficheiros em PortugolTipado usam a extensão **`.port`**.
- Os ficheiros em Portugol usam **`.por`**.

Durante a migração, um projeto pode ter os dois tipos de ficheiro ao mesmo tempo. Na página de um projeto do Explorar, a barra de linguagens mostra a proporção de cada um.

## Dicas

- Migra **um ficheiro de cada vez** e testa a cada passo, em vez de mudar tudo de uma vez.
- Se um comando gráfico deixou de funcionar, confirma primeiro se ainda tem o prefixo `g.` ou o nome antigo.
- Precisas de ajuda? Usa **Enviar feedback**, na aba Mais, e conta-nos o que travou a tua migração.
