# Modelo android-interpretador

APK sem C, sem NDK e sem SDL. O motor (pasta `core/` do repositório) é compilado
dentro do APK e corre o programa `.port` no Interpretador. Cada `escreva`/`leia`
aparece num diálogo nativo do Android, no estilo USSD.

- Os `.port` do projeto vão para `app/src/main/assets/projeto/` e o ficheiro
  principal para `assets/programa.txt` (o workflow trata disto).
- As permissões do `app.yml` entram no lugar do marcador `<!-- PERMISSOES -->`
  do `AndroidManifest.xml` (feito pelo `scripts/app_yml.py`).
- Programas com `inclua graficos` continuam no modelo `android-apk` (SDL).
