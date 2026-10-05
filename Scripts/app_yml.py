#!/usr/bin/env python3
"""Le e valida o app.yml de um projeto PortugolTipado.

Uso:  python3 scripts/app_yml.py app.yml [--saida FICHEIRO] [--sem-ficheiros]

Escreve as variaveis APP_* em FICHEIRO (por exemplo $GITHUB_ENV) ou no ecra.
Em caso de erro escreve mensagens em portugues e termina com codigo 1.
"""
import os
import re
import sys
import unicodedata

CAMPOS = ['nome', 'pacote', 'versao', 'icone', 'orientacao', 'programa']
ORIENTACOES = {'horizontal': 'sensorLandscape', 'vertical': 'sensorPortrait', 'automatica': 'fullSensor'}
PALAVRAS_JAVA = set(
    'abstract assert boolean break byte case catch char class const continue default do double else enum '
    'extends final finally float for goto if implements import instanceof int interface long native new '
    'package private protected public return short static strictfp super switch synchronized this throw '
    'throws transient try void volatile while true false null'.split())


def sem_acentos(txt):
    return unicodedata.normalize('NFKD', txt).encode('ascii', 'ignore').decode('ascii')


def ler_yaml_simples(texto, erros):
    """YAML plano, 'chave: valor', com comentarios '#'."""
    dados = {}
    for n, linha in enumerate(texto.splitlines(), 1):
        bruta = linha.strip()
        if not bruta or bruta.startswith('#'):
            continue
        valor_sem_coment = ''
        aspas = None
        for ch in bruta:
            if aspas:
                if ch == aspas:
                    aspas = None
            elif ch in ('"', "'"):
                aspas = ch
            elif ch == '#' and valor_sem_coment.endswith(' '):
                break
            valor_sem_coment += ch
        bruta = valor_sem_coment.strip()
        if ':' not in bruta:
            erros.append('Linha %d do app.yml inválida: "%s". Usa o formato  chave: valor' % (n, bruta))
            continue
        chave, valor = bruta.split(':', 1)
        chave = sem_acentos(chave.strip().lower())
        valor = valor.strip()
        if len(valor) >= 2 and valor[0] == valor[-1] and valor[0] in ('"', "'"):
            valor = valor[1:-1]
        if chave not in CAMPOS:
            erros.append('Campo desconhecido no app.yml: "%s". Campos válidos: %s.' % (chave, ', '.join(CAMPOS)))
            continue
        if chave in dados:
            erros.append('O campo "%s" aparece duas vezes no app.yml.' % chave)
            continue
        dados[chave] = valor
    return dados


def slug(txt):
    s = re.sub(r'[^a-z0-9]', '', sem_acentos(txt).lower())
    if not s or not s[0].isalpha():
        s = 'app' + s
    return s[:40]


def validar(dados, pasta, verificar_ficheiros, erros):
    r = {}
    nome = dados.get('nome', '').strip()
    if not nome:
        erros.append('Falta o campo "nome" no app.yml. Exemplo:  nome: Bola Saltitante')
    elif len(nome) > 30:
        erros.append('O campo "nome" tem %d caracteres; o máximo é 30.' % len(nome))
    elif re.search(r'[<>&"\'\\\x00-\x1f]', nome):
        erros.append('O campo "nome" tem caracteres não permitidos (< > & " \' \\).')
    r['APP_NOME'] = nome

    pacote = dados.get('pacote', '').strip()
    if not pacote:
        pacote = 'co.adilson889.apps.' + slug(nome or 'app')
    ok = re.fullmatch(r'[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+', pacote) is not None
    if not ok or len(pacote) > 100:
        erros.append('O campo "pacote" é inválido: usa só minúsculas, números e "_", em blocos separados por pontos '
                     '(por exemplo co.adilson889.apps.bola).')
    elif any(seg in PALAVRAS_JAVA for seg in pacote.split('.')):
        erros.append('O campo "pacote" tem um bloco que é palavra reservada (por exemplo "new" ou "class"). Muda esse bloco.')
    r['APP_PACOTE'] = pacote

    versao = dados.get('versao', '1.0.0').strip() or '1.0.0'
    m = re.fullmatch(r'(\d{1,2})\.(\d{1,2})\.(\d{1,2})', versao)
    if not m:
        erros.append('O campo "versao" é inválido: usa o formato 1.2.3 (cada número de 0 a 99).')
        codigo = 1
    else:
        codigo = max(1, int(m.group(1)) * 10000 + int(m.group(2)) * 100 + int(m.group(3)))
    r['APP_VERSAO'] = versao
    r['APP_VERSION_CODE'] = str(codigo)

    ori = sem_acentos(dados.get('orientacao', 'horizontal').strip().lower()) or 'horizontal'
    if ori not in ORIENTACOES:
        erros.append('O campo "orientacao" é inválido: usa horizontal, vertical ou automatica.')
        ori = 'horizontal'
    r['APP_ORIENTACAO'] = ORIENTACOES[ori]

    icone = dados.get('icone', '').strip()
    if icone:
        if icone.startswith('/') or '..' in icone.split('/'):
            erros.append('O campo "icone" deve ser um caminho dentro do projeto (sem "/" no início nem "..").')
        elif not icone.lower().endswith('.png'):
            erros.append('O campo "icone" deve ser um ficheiro .png.')
        elif verificar_ficheiros and not os.path.isfile(os.path.join(pasta, icone)):
            erros.append('O ficheiro do ícone não existe: %s' % icone)
    r['APP_ICONE'] = icone

    prog = dados.get('programa', 'principal.port').strip() or 'principal.port'
    if prog.startswith('/') or '..' in prog.split('/') or not prog.lower().endswith('.port'):
        erros.append('O campo "programa" deve ser um ficheiro .port dentro do projeto (por exemplo principal.port).')
    elif verificar_ficheiros and not os.path.isfile(os.path.join(pasta, prog)):
        erros.append('O programa indicado não existe: %s' % prog)
    r['APP_PROGRAMA'] = prog
    return r


def main(argv):
    if len(argv) < 2:
        print('Uso: app_yml.py app.yml [--saida FICHEIRO] [--sem-ficheiros]')
        return 2
    caminho = argv[1]
    saida = argv[argv.index('--saida') + 1] if '--saida' in argv else None
    verificar = '--sem-ficheiros' not in argv
    erros = []
    if not os.path.isfile(caminho):
        print('::error title=app.yml::Falta o ficheiro app.yml na raiz do projeto.')
        return 1
    with open(caminho, encoding='utf-8-sig') as f:
        dados = ler_yaml_simples(f.read(), erros)
    resultado = validar(dados, os.path.dirname(os.path.abspath(caminho)), verificar, erros)
    if erros:
        for e in erros:
            print('::error title=app.yml::' + e)
        return 1
    linhas = ['%s=%s' % (k, v) for k, v in resultado.items()]
    if saida:
        with open(saida, 'a', encoding='utf-8') as f:
            f.write('\n'.join(linhas) + '\n')
    else:
        print('\n'.join(linhas))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
