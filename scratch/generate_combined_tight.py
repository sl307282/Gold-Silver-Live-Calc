from PIL import Image, ImageDraw, ImageFont
import os

# The canvas size is 288x288 to match the exact mathematical ratio of Android's adaptive icon.
canvas_size = 288
img = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Load the existing logo
logo = Image.open("app/src/main/res/drawable/splash_logo.png").convert("RGBA")

# We must size the logo to 120px to mathematically guarantee it doesn't push the text outside the circular mask
logo_size = 120
logo = logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)

# 24dp gap * 1.5 density factor = 36px gap
gap = 36

# Premium bold font
text = "Gold & Silver Calculator"
font_size = 10
font_path = "/System/Library/Fonts/Avenir Next.ttc"
fallback_path = "/System/Library/Fonts/HelveticaNeue.ttc"

while True:
    try:
        font = ImageFont.truetype(font_path, font_size, index=7)
    except:
        try:
            font = ImageFont.truetype(fallback_path, font_size, index=4)
        except:
            font = ImageFont.load_default()
            break
            
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    if w >= 210: # Max width before hitting the curvature of the circular mask
        break
    font_size += 1

bbox = draw.textbbox((0, 0), text, font=font)
w = bbox[2] - bbox[0]
h = bbox[3] - bbox[1]

# Total height of the grouped elements
total_h = logo_size + gap + h

# Perfectly center the entire group vertically in the 288 canvas
start_y = (canvas_size - total_h) / 2

# Paste logo
logo_x = (canvas_size - logo_size) // 2
img.paste(logo, (logo_x, int(start_y)), logo)

# Draw text exactly 24dp (36px) below the logo
gold_color = (255, 215, 0)
text_x = (canvas_size - w) / 2
text_y = start_y + logo_size + gap

# Add pseudo-bold stroke to make the tiny text legible and prominent
draw.text((text_x, text_y), text, font=font, fill=gold_color, stroke_width=1, stroke_fill=gold_color)

# Save
img.save("app/src/main/res/drawable-nodpi/splash_logo_combined.png")
print("Perfectly fitted combination generated!")
