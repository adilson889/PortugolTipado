# Modelo de APK (NDK + SDL2)

Projeto Android que o workflow `build-port-android.yaml` preenche e compila.

O que o workflow coloca aqui antes de correr o Gradle:

- `app/jni/SDL` e `app/jni/SDL2_ttf`: o código do SDL2 e do SDL2_ttf (release fixa).
- `app/src/main/java/org/libsdl/app/*`: a casca Java do SDL2 (copiada de `SDL/android-project`).
- `app/jni/src/programa.c`: o C gerado pelo PortugolTipado.
- `app/jni/src/graficos.c`, `graficos.h` e `dejavu_ttf.h`: a biblioteca gráfica de `modelos/desktop-exe`.
- Os ícones em `app/src/main/res/mipmap-*` (ou os padrão, se o `app.yml` não tiver `icone`).

Os valores do `app.yml` entram no Gradle por variáveis de ambiente `ORG_GRADLE_PROJECT_*`
(`appNome`, `appPacote`, `appVersao`, `appVersionCode`, `appOrientacao`).

O `main` do programa passa a chamar-se `SDL_main` com `-Dmain=SDL_main`, que é o ponto de entrada do SDL no Android.
O `graficos.c` usa `<SDL2/SDL.h>`; os atalhos em `app/jni/src/SDL2/` encaminham para os cabeçalhos do SDL.
