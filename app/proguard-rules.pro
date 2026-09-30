# Adicione aqui regras específicas do projeto para o ProGuard.
# É possível controlar os ficheiros de configuração aplicados através
# da configuração proguardFiles no build.gradle.
#
# Para mais detalhes, consulte:
# https://developer.android.com/guide/developing/tools/proguard

# Se o projeto utilizar WebView com JavaScript, remova os comentários
# abaixo e especifique o nome completo da classe que serve como
# interface JavaScript:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Remova os comentários abaixo para preservar informações de número
# de linha usadas na depuração de stack traces.
#-keepattributes SourceFile,LineNumberTable

# Se preservar as informações de número de linha, remova os comentários
# abaixo para ocultar o nome original do ficheiro de origem.
#-renamesourcefileattribute SourceFile