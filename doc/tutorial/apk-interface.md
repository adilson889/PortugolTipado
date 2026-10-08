# Criar uma aplicação Android com ecrãs HTML (interface)

Programas com `inclua graficos` e `inclua interface` (componentes HTML/CSS sobre a janela) têm o seu próprio build de Android, que não usa C nem SDL: o programa corre dentro do APK, no interpretador.

## O que é preciso

1. O programa com os dois `inclua`:

```
inclua graficos
inclua interface
```

2. Um `app.yml` na raiz do projeto, igual ao dos outros APK (`nome`, `pacote`, `versao`, `icone`, `orientacao`, `programa`).

Sem o campo `orientacao`, a aplicação fica na **vertical**.

## Como correr

No GitHub, corre o workflow **Build port interface android**, ou chama-o a partir do teu workflow com `workflow_call` (campos `programa`, `nome` e `pasta`). O APK fica pronto a descarregar.

## Limites

- Só corre no Android. Programas com `interface` não geram C.
- Programas só de texto usam o build normal (`build-port-android`), e os que usam `inclua graficos` sem `interface` usam o modelo SDL.
