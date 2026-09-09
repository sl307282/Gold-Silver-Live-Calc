from PIL import Image, ImageDraw, ImageFont
import os

# Ensure directory exists
os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)

# Create a transparent image wide enough to hold the text
img = Image.new('RGBA', (800, 160), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Try to load a premium bold font from macOS
try:
    font = ImageFont.truetype("/System/Library/Fonts/Avenir Next.ttc", 72, index=7) # Heavy/Bold
except:
    try:
        font = ImageFont.truetype("/System/Library/Fonts/HelveticaNeue.ttc", 72, index=4) # Bold
    except:
        font = ImageFont.load_default()

text = "Gold & Silver Calculator"
# Get text bounding box
bbox = draw.textbbox((0, 0), text, font=font)
w = bbox[2] - bbox[0]
h = bbox[3] - bbox[1]

# Draw text centered with a premium gold color matching your theme
gold_color = (255, 215, 0) # Solid Gold
draw.text(((800 - w) / 2, (160 - h) / 2), text, font=font, fill=gold_color)

# Save it directly into the Android drawable folder
img.save("app/src/main/res/drawable-nodpi/splash_text.png")
print("Text image generated successfully.")
