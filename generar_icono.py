#!/usr/bin/env python3
"""Vista previa del icono de la app (mismo diseño que el vector adaptativo)."""
from PIL import Image, ImageDraw

SS = 4
SIZE = 512
W = SIZE * SS
s = W / 108.0

def P(x, y):
    return (x * s, y * s)

def cubic(p0, p1, p2, p3, n=64):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1-t)**3*p0[0] + 3*(1-t)**2*t*p1[0] + 3*(1-t)*t*t*p2[0] + t**3*p3[0]
        y = (1-t)**3*p0[1] + 3*(1-t)**2*t*p1[1] + 3*(1-t)*t*t*p2[1] + t**3*p3[1]
        pts.append((x, y))
    return pts

img = Image.new("RGB", (W, W), "#4527A0")
d = ImageDraw.Draw(img)

# Fondo degradado índigo
top = (0x45, 0x27, 0xA0)
bot = (0x1A, 0x23, 0x7E)
for yy in range(W):
    t = yy / W
    c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
    d.line([(0, yy), (W, yy)], fill=c)

def lw(w):
    return max(1, int(w * s))

# Rayos de luz
ray = "#FFE082"
for a, b in [((54, 30), (54, 24)), ((43, 33), (39, 28)), ((65, 33), (69, 28)),
             ((36, 44), (30, 44)), ((72, 44), (78, 44))]:
    d.line([P(*a), P(*b)], fill=ray, width=lw(2.5))

# Cruz dorada
d.polygon([P(51, 35), P(57, 35), P(57, 59), P(51, 59)], fill="#FFD54F")
d.polygon([P(42, 41), P(66, 41), P(66, 47), P(42, 47)], fill="#FFD54F")

# Páginas de la Biblia abierta
left = cubic((54, 65), (49, 61), (41, 59), (32, 60))
left += cubic((32, 79), (41, 78), (49, 79.5), (54, 83))
right = cubic((54, 65), (59, 61), (67, 59), (76, 60))
right += cubic((76, 79), (67, 78), (59, 79.5), (54, 83))
d.polygon([P(x, y) for x, y in left], fill="#FFF3E0")
d.polygon([P(x, y) for x, y in right], fill="#FFF3E0")

# Lomo central
d.line([P(54, 65.5), P(54, 82.5)], fill="#D7A94B", width=lw(1.2))

# Líneas de texto
lines = "#C9A227"
for a, b, c2, e in [
    ((36, 65.5), (40, 64.3), (45, 64.6), (50, 66.4)),
    ((36, 70.5), (40, 69.3), (45, 69.6), (50, 71.4)),
    ((58, 66.4), (63, 64.6), (68, 64.3), (72, 65.5)),
    ((58, 71.4), (63, 69.6), (68, 69.3), (72, 70.5)),
]:
    pts = cubic(a, b, c2, e, n=24)
    d.line([P(x, y) for x, y in pts], fill=lines, width=lw(1.4), joint="curve")

# Máscara redondeada estilo launcher y reducción con antialias
mask = Image.new("L", (W, W), 0)
md = ImageDraw.Draw(mask)
md.rounded_rectangle([0, 0, W, W], radius=int(W * 0.22), fill=255)
out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
out.paste(img, (0, 0), mask)
out = out.resize((SIZE, SIZE), Image.LANCZOS)
out.save("icono-preview.png")
print("icono-preview.png generado")
