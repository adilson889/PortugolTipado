# Modelo android-nativo

APK para programas com `inclua layoutnativos`. Sem SDL, sem Kotlin e sem AndroidX.

- A lógica (`.port`) vira C e é compilada em `programa.so` (ndk-build).
- As telas (`.ui`) vão para `assets/ui/`.
- O renderizador (Java puro) carrega o `.so`, recebe a árvore de UI e cria as Views.

## O que o workflow coloca aqui antes do Gradle

- `app/jni/src/programa.c`: o C gerado pelo PortugolTipado.
- `app/jni/src/*.c`: runtime C da layoutnativos (a criar, quando o core estiver no repo).
- `app/src/main/assets/ui/`: os `.ui` do projecto.
- `app/src/main/java/.../`: o renderizador Java (a criar).
- `app/src/main/res/mipmap-*`: ícones (`preparar_icone.py`).
- Permissões do `app.yml` no marcador `<!-- PERMISSOES -->` (`app_yml.py`).

Os valores do `app.yml` entram no Gradle por `ORG_GRADLE_PROJECT_*`, como nos outros modelos.

## Falta fazer

1. Formato da árvore entre o C e o Java.
2. `PrincipalActivity` e o renderizador (Java puro).
3. Ponte JNI (thread própria para o C, mudanças aplicadas na thread principal).
4. Conversão dos `.ui` para template (por agora vão como ficheiros).
5. No `build-port-android.yaml`, desviar para este workflow quando houver `inclua layoutnativos`
   (como já se faz com `inclua interface`).

## Protecção

`-fvisibility=hidden`, `-Os` e `--gc-sections` no C, e R8 (`minifyEnabled true`) no release.
O workflow usa `assembleDebug`, como os outros; para release é preciso assinar com a tua chave.
