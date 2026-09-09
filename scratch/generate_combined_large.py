from PIL import Image, ImageDraw, ImageFont
import os
import math

# We use a 1000x1000 high-res canvas. This will be scaled natively by Android to 288dp.
canvas_size = 1000
img = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# The Android 12 adaptive mask is a circle of diameter 192dp (666px in this 1000px canvas).
# Radius is 333px from the center.

logo = Image.open("app/src/main/res/drawable/splash_logo.png").convert("RGBA")

# Increase logo size significantly (300px corresponds to approx 86dp, twice as large as the tiny one!)
logo_size = 300
logo = logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)

# 24dp gap corresponds to ~83px in our canvas
gap = 80

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
    h = bbox[3] - bbox[1]
    
    total_h = logo_size + gap + h
    bottom_y_from_center = total_h / 2
    
    # Calculate max allowed width at this Y to stay inside the circular mask (Radius = 333)
    if bottom_y_from_center >= 333:
        break
        
    max_x = math.sqrt(333**2 - bottom_y_from_center**2)
    max_w = max_x * 2
    
    if w >= (max_w - 20): # Leave a 20px padding from the curved edge
        break
    font_size += 2

bbox = draw.textbbox((0, 0), text, font=font)
w = bbox[2] - bbox[0]
h = bbox[3] - bbox[1]

total_h = logo_size + gap + h
start_y = (canvas_size - total_h) / 2

# Paste logo
logo_x = (canvas_size - logo_size) // 2
img.paste(logo, (logo_x, int(start_y)), logo)

gold_color = (255, 215, 0)
text_x = (canvas_size - w) / 2
text_y = start_y + logo_size + gap

# Add pseudo-bold stroke to keep the text ultra-bold
draw.text((text_x, text_y), text, font=font, fill=gold_color, stroke_width=2, stroke_fill=gold_color)

img.save("app/src/main/res/drawable-nodpi/splash_logo_combined.png")
print(f"Generated successfully with max logo {logo_size} and text width {w}")
