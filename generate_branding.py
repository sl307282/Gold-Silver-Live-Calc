from PIL import Image, ImageDraw, ImageFont
import os

img = Image.new('RGBA', (1000, 400), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

text = "Gold & Silver Calculator"

# Automatically find the absolute largest font size that fits the 1000px canvas
font_size = 10
font_path = "/System/Library/Fonts/Avenir Next.ttc"
fallback_path = "/System/Library/Fonts/HelveticaNeue.ttc"

while True:
    try:
        # Index 7 is Heavy/Bold for Avenir Next
        font = ImageFont.truetype(font_path, font_size, index=7)
    except:
        try:
            # Index 4 is Bold for Helvetica Neue
            font = ImageFont.truetype(fallback_path, font_size, index=4)
        except:
            font = ImageFont.load_default()
            break
            
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    if w >= 930: # Leave a tiny bit of room for the stroke width expansion
        break
    font_size += 2

print(f"Maximized font size: {font_size}")

bbox = draw.textbbox((0, 0), text, font=font)
w = bbox[2] - bbox[0]
h = bbox[3] - bbox[1]

gold_color = (255, 215, 0)
# Draw the text with a thick pseudo-bold stroke to make it ultra bold!
stroke_thickness = 4

draw.text(((1000 - w) / 2, 0), text, font=font, fill=gold_color, stroke_width=stroke_thickness, stroke_fill=gold_color)

img.save("app/src/main/res/drawable-nodpi/splash_branding.png")
print("Branding text updated with ultra-bold stroke.")
