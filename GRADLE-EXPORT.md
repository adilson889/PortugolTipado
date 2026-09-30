# Editor PortugolTipado

Exportado do CodeAssist como um projeto Gradle. Os ficheiros de build aqui foram gerados a partir do modelo do projeto, portanto são um ponto de partida fiel, e não um build que tenha sido executado anteriormente: abra-o no Android Studio (ou execute `gradle build`) e espere precisar de fazer alguns ajustes.

## Com o que foi gerado

- Android Gradle Plugin 8.13.0
- Kotlin 2.4.0
- Gradle 8.13 (as propriedades do wrapper estão aqui; os scripts `gradlew` não estão incluídos, portanto execute `gradle wrapper` uma vez ou deixe o Android Studio fazer isso)

O Kotlin está fixado na versão com a qual o CodeAssist compilou este projeto, portanto os códigos-fonte compilam da mesma forma nesse ambiente. Essa versão é mais recente do que o D8/R8 incluído neste AGP, o que gera um aviso sobre metadados do Kotlin que ele não consegue reescrever; aumentar a versão do AGP elimina esse aviso.

## Módulos

- `:app` (app): aplicação Android

## Notas

Tudo o que estava no modelo do projeto foi mantido.

A localização do Android SDK não é exportada: o Android Studio cria o ficheiro `local.properties` na primeira sincronização, ou pode definir `sdk.dir` nesse ficheiro manualmente.