# Modelo android-interface

APK sem C, sem NDK e sem SDL, para programas com `inclua graficos` e `inclua interface`.
O motor (pasta `core/` do repositório) é compilado dentro do APK e corre o programa `.port`
no Interpretador. A janela do programa é a `GraficosActivity` (o desenho), com os componentes
HTML (WebViews) por cima, geridos pela `InterfaceAndroid`.

O que o workflow `build-port-interface-android.yaml` coloca aqui antes de correr o Gradle:

- Os `.port` do projeto em `app/src/main/assets/projeto/` e o ficheiro principal em `assets/programa.txt`.
- `GraficosCanvas.kt`, `GraficosView.kt`, `GraficosActivity.kt` e `InterfaceAndroid.kt`, copiados do
  repositório do motor para `app/src/main/java/motor_android/` (se já fizerem parte do `core/`, não são copiados).
- Os ícones em `app/src/main/res/mipmap-*` (ou os padrão, se o `app.yml` não tiver `icone`).
- As permissões do `app.yml` no lugar do marcador `<!-- PERMISSOES -->` do `AndroidManifest.xml`
  (feito pelo `scripts/app_yml.py`).

Os valores do `app.yml` entram no Gradle por variáveis `ORG_GRADLE_PROJECT_*`.

`FabricaInterface.kt` é o único ficheiro do modelo que conhece o construtor da `InterfaceAndroid`.

Programas só de texto continuam no modelo `android-interpretador`, e programas com `inclua graficos`
sem `interface` continuam no modelo `android-apk` (SDL).
