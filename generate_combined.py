from PIL import Image, ImageDraw, ImageFont
import os

# Load the existing logo
logo = Image.open("app/src/main/res/drawable/splash_logo.png").convert("RGBA")
logo = logo.resize((190, 190), Image.Resampling.LANCZOS)

# Create a tight 288x288 canvas (matches Android Splash Screen internal canvas ratio)
canvas_size = 288
img = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Premium bold font
try:
    font = ImageFont.truetype("/System/Library/Fonts/Avenir Next.ttc", 30, index=7)
except:
    try:
        font = ImageFont.truetype("/System/Library/Fonts/HelveticaNeue.ttc", 30, index=4)
    except:
        font = ImageFont.load_default()

text = "Gold & Silver Calculator"
bbox = draw.textbbox((0, 0), text, font=font)
text_w = bbox[2] - bbox[0]
text_h = bbox[3] - bbox[1]

gap = 10
total_h = 190 + gap + text_h
start_y = (canvas_size - total_h) / 2

# Paste logo
logo_x = (canvas_size - 190) // 2
img.paste(logo, (logo_x, int(start_y)), logo)

# Draw text
gold_color = (255, 215, 0)
text_x = (canvas_size - text_w) / 2
text_y = start_y + 190 + gap
draw.text((text_x, text_y), text, font=font, fill=gold_color)

# Save
img.save("app/src/main/res/drawable/splash_logo_combined.png")
print("Tightly packed logo and text generated successfully.")
