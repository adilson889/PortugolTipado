# PortugolTipado V2.2 — análise de tipos (experimental)

**Projeto original:** Adilson C. Rafael. **Proposta, direção e contribuição experimental V2.2:** Artur Mendes Vieira Alves ([@tuhmds](https://github.com/tuhmds)), com assistência de IA na implementação. Licença do projeto original: GPLv3 ou posterior.

## Como usar

O modo padrão continua compatível com o compilador anterior:

~~~sh
java -jar core/build/libs/port.jar -o programa.c programa.port
~~~

Para habilitar as regras experimentais de tipagem:

~~~sh
java -jar core/build/libs/port.jar --tipagem=segura -o programa.c programa.port
~~~

A CLI também aceita --tipagem=rigorosa e --tipagem=legada.

## Regras implementadas na V2.2

- Rejeita conversões numéricas implícitas com risco de perda de informação; literais inteiros são verificados conforme o intervalo admitido.
- Valida operações numéricas, incluindo promoção de inteiros longos e operandos de resto.
- Valida chamadas a funções declaradas, quantidade e tipos de argumentos, e parâmetros altere por referência.
- Rejeita funções vazio que retornam valor e funções com resultado que tenham retornos incompatíveis ou caminhos claramente sem retorno.
- Exige condições lógicas em se, enquanto, faca/enquanto e para no modo seguro.
- Verifica existência e tipo de campos de estruturas, índices de arrays e tamanho/tipos de literais agregados.
- Impede atribuição integral de arrays, cuja forma não seria válida no C de saída.
- Mantém mensagens de erro em português.
- Preserva as correções de leitura, cópia e concatenação de textos realizadas na V2.1.

## Testes de regressão

O workflow .github/workflows/v2-2-tipagem.yml compila o motor em Kotlin e executa tests/v2-2-tipos.sh.

A bateria inicial contém cinco programas válidos compilados com GCC e executados e dezessete programas inválidos rejeitados pela análise semântica. A V2.1 tem seu próprio workflow de segurança de textos.

## Limites conhecidos

**Esta ainda não é uma implementação completa de um sistema de tipos.**

- O verificador seguro continua opcional; o validador legado ainda roda antes dele.
- Assinaturas de certas funções externas/bibliotecas não são conhecidas pelo analisador e podem depender da verificação do compilador C.
- O transpilador C possui sua própria lógica para inferir tipos de expressões, ainda não unificada com este analisador.
- A análise de todos os caminhos de retorno é conservadora, mas não cobre todos os fluxos complexos.
- Nem todas as combinações de arrays multidimensionais, estruturas aninhadas e aliases de memória foram verificadas.
- Tamanhos de tipos C como long variam conforme a plataforma; a política de conversão adota limites conservadores.
- **Não há ainda implementação do algoritmo experimental de reutilização de memória**, nem ganho de RAM comprovado.
- Inferência automática com a palavra seja e outras mudanças de sintaxe ainda não fazem parte desta branch.

Antes de abrir um Pull Request no projeto original, ampliar a matriz de testes (Windows, Linux e Android quando aplicável), reduzir duplicações entre verificadores e estabelecer as regras oficiais da linguagem.
