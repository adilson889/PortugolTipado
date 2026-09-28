# PortugolTipado

> Superconjunto de Portugol, fortemente tipado, que compila pra C nativo e legível.

**PortugolTipado** leva a ideia do **Portugol** para além do ambiente educacional, adicionando **tipagem forte** e **compilação pra C nativo** — código real, legível e compilável, não interpretado.

Mantém a sintaxe familiar do Portugol, mas permite construir software de verdade usando todo o **ecossistema C**.

## Objetivos

- **Superconjunto de Portugol:** sintaxe familiar, sem curva de aprendizado
- **Tipagem forte:** todo valor tem tipo explícito, sem inferência mágica
- **Compila pra C nativo — gera código legível e compilável por `gcc`
- Compatível com o ecossistema C:** usa bibliotecas C reais (SDL2, SQLite, math, etc.)
- **Gera binário nativo:** roda em qualquer plataforma com `gcc`

## Estado

🚧 **Em desenvolvimento — fase inicial.**

- Lexer, Parser, Transpilador, Validador e Interpretador funcionais
- Editor Android (Código + Gerado + Preview)
- Módulos locais (`.tpc`) em progresso
- Alias de função externa implementado
- Arrays 2D implementados

## Licença

MIT