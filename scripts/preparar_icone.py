#!/usr/bin/env python3
"""Gera os icones do Android (mipmap-*/ic_launcher.png) a partir de um PNG.

Uso:  python3 scripts/preparar_icone.py ICONE.png PASTA_RES
Sem icone (ou com "-"), gera o icone padrao do PortugolTipado.
"""
import os
import sys
from PIL import Image, ImageDraw

TAMANHOS = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}


def icone_padrao(lado=512):
    img = Image.new('RGBA', (lado, lado), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, lado - 1, lado - 1], radius=lado // 5, fill=(124, 58, 237, 255))
    w = max(6, lado // 20)
    # < / >
    d.line([(lado * .36, lado * .30), (lado * .20, lado * .50), (lado * .36, lado * .70)], fill='white', width=w, joint='curve')
    d.line([(lado * .64, lado * .30), (lado * .80, lado * .50), (lado * .64, lado * .70)], fill='white', width=w, joint='curve')
    d.line([(lado * .56, lado * .26), (lado * .44, lado * .74)], fill='white', width=w)
    return img


def main(argv):
    if len(argv) < 3:
        print('Uso: preparar_icone.py ICONE.png PASTA_RES')
        return 2
    origem, res = argv[1], argv[2]
    if origem in ('', '-'):
        img = icone_padrao()
    else:
        try:
            img = Image.open(origem).convert('RGBA')
        except Exception:
            print('::error title=icone::Não foi possível ler o ícone "%s". Usa um ficheiro PNG válido.' % origem)
            return 1
        lado = min(img.size)
        if lado < 192:
            print('::error title=icone::O ícone é pequeno (%dx%d). Usa um PNG com pelo menos 192x192 (ideal 512x512).' % img.size)
            return 1
        if img.size[0] != img.size[1]:
            print('::warning title=icone::O ícone não é quadrado e foi cortado ao centro.')
            x = (img.size[0] - lado) // 2
            y = (img.size[1] - lado) // 2
            img = img.crop((x, y, x + lado, y + lado))
    for dpi, px in TAMANHOS.items():
        pasta = os.path.join(res, 'mipmap-' + dpi)
        os.makedirs(pasta, exist_ok=True)
        img.resize((px, px), Image.LANCZOS).save(os.path.join(pasta, 'ic_launcher.png'), optimize=True)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
