# Criar uma aplicação para Android (APK)

Podes transformar o teu programa gráfico numa aplicação Android que instalas no telemóvel. Basta juntar um ficheiro chamado **app.yml** ao teu projeto.

## O ficheiro app.yml

Põe-no na raiz do projeto, ao lado do teu `principal.port`:

```
# Configuração da aplicação
nome: Bola Saltitante
pacote: co.adilson889.apps.bola
versao: 1.0.0
icone: icone.png
orientacao: horizontal
```

Cada linha tem a forma `campo: valor`. O que vem depois de `#` é um comentário e é ignorado.

## Campos

- **nome** (obrigatório): o nome que aparece no telemóvel. Até 30 caracteres, sem os símbolos `< > & " '`.
- **pacote** (opcional): o identificador único da aplicação, por exemplo `co.adilson889.apps.bola`. Usa só letras minúsculas, números e `_`, em blocos separados por pontos. Se não o indicares, é criado a partir do nome.
- **versao** (opcional): o número da versão, no formato `1.2.3`. Por defeito é `1.0.0`. Aumenta-o sempre que publicares uma versão nova.
- **icone** (opcional): um ficheiro PNG do teu projeto, com pelo menos 192 por 192 pixels. O ideal é 512 por 512, quadrado. Sem ele, a aplicação usa o ícone padrão do PortugolTipado.
- **orientacao** (opcional): `horizontal`, `vertical` ou `automatica`. Por defeito é `horizontal`.
- **programa** (opcional): o ficheiro por onde o programa começa. Por defeito é `principal.port`.

## Como correr

Quando enviares o projeto para o GitHub, o build do Android lê o `app.yml`, compila o programa e deixa o `.apk` pronto a descarregar, junto com as versões para Windows e Linux.

Se algum campo estiver errado, o build pára com uma mensagem a explicar o que corrigir, por exemplo:

```
O campo "pacote" é inválido: usa só minúsculas, números e "_",
em blocos separados por pontos.
```

## Instalar no telemóvel

1. Descarrega o ficheiro `.apk`.
2. Abre-o no telemóvel e, se o Android pedir, permite a instalação de aplicações de fontes desconhecidas.
3. Abre a aplicação.

## O que funciona

- Programas gráficos: janela, desenho, texto e cliques.
- O toque no ecrã conta como clique do rato.
- A janela ocupa o ecrã inteiro, e o programa é escalado para caber.

Ainda não há botões no ecrã para as setas e a barra de espaço. Programas que dependem do teclado por enquanto ficam melhor no computador.
