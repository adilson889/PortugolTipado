#!/usr/bin/env python3
"""Gera os icones do Android (mipmap-*/ic_launcher.png) a partir de um PNG.

Uso:  python3 scripts/preparar_icone.py ICONE.png PASTA_RES
Sem icone (ou com "-"), gera o icone padrao do PortugolTipado.
"""
import math
import os
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageOps

TAMANHOS = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}

# Cores do icone padrao
COR_FUNDO_1 = (124, 58, 237)    # violeta
COR_FUNDO_2 = (30, 64, 175)     # azul escuro
COR_LETRA = (255, 255, 255, 255)
COR_CURSOR = (251, 191, 36, 255)  # ambar


def _gradiente(lado, c1, c2):
    """Gradiente diagonal de c1 (canto superior esquerdo) para c2."""
    g = Image.linear_gradient('L').resize((lado * 2, lado * 2)).rotate(45, resample=Image.BICUBIC)
    m = lado // 2
    g = ImageOps.autocontrast(g.crop((m, m, m + lado, m + lado)))
    return Image.composite(Image.new('RGB', (lado, lado), c2), Image.new('RGB', (lado, lado), c1), g)


def _glifos(d, S, cor_letra, cor_cursor, dy=0):
    """Desenha um P grande de traco arredondado, com um cursor ao lado."""
    w = int(S * 0.10)

    def traco(pts, cor, largura):
        pts = [(x, y + dy) for x, y in pts]
        d.line(pts, fill=cor, width=largura, joint='curve')
        r = largura / 2
        for x, y in pts:
            d.ellipse([x - r, y - r, x + r, y + r], fill=cor)

    # P: haste, topo, arco da barriga e fecho
    cx, cy, raio = .50, .38, .16
    arco = [(S * (cx + raio * math.cos(math.radians(a))), S * (cy + raio * math.sin(math.radians(a))))
            for a in range(-90, 91, 6)]
    p = [(S * .34, S * .76), (S * .34, S * .22), (S * cx, S * .22)] + arco + [(S * .34, S * .54)]
    traco(p, cor_letra, w)
    # cursor
    traco([(S * .54, S * .76), (S * .70, S * .76)], cor_cursor, int(w * .8))


def icone_padrao(lado=512):
    S = lado * 2  # desenha em dobro e reduz no fim (bordas suaves)
    img = _gradiente(S, COR_FUNDO_1, COR_FUNDO_2).convert('RGBA')

    # brilho suave no canto superior esquerdo
    brilho = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(brilho).ellipse([-S * .25, -S * .35, S * .75, S * .45], fill=(255, 255, 255, 55))
    img = Image.alpha_composite(img, brilho.filter(ImageFilter.GaussianBlur(S * .06)))

    # sombra dos simbolos
    cor_sombra = (15, 10, 50, 140)
    sombra = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    _glifos(ImageDraw.Draw(sombra), S, cor_sombra, cor_sombra, dy=S * .025)
    img = Image.alpha_composite(img, sombra.filter(ImageFilter.GaussianBlur(S * .02)))

    # simbolos
    glifos = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    _glifos(ImageDraw.Draw(glifos), S, COR_LETRA, COR_CURSOR)
    img = Image.alpha_composite(img, glifos)

    # cantos arredondados
    mascara = Image.new('L', (S, S), 0)
    ImageDraw.Draw(mascara).rounded_rectangle([0, 0, S - 1, S - 1], radius=S // 5, fill=255)
    img.putalpha(mascara)
    return img.resize((lado, lado), Image.LANCZOS)


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
