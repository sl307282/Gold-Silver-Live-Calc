from PIL import Image, ImageDraw, ImageFont
import os
import math

# Use 864x864 canvas (maps exactly to 288dp on xxxhdpi screens)
canvas_size = 864
img = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Safe zone radius is 192dp / 2 = 96dp. On xxxhdpi, 96dp = 288 pixels.
safe_radius = 288

logo = Image.open("app/src/main/res/drawable/splash_logo.png").convert("RGBA")
# Logo size 250px is approx 83dp. This is a very comfortable large size.
logo_size = 250
logo = logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)

# Gap of 60px is 20dp
gap = 60

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
            
    # Calculate bounding box
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    w = right - left
    h = bottom - top
    
    total_h = logo_size + gap + h
    bottom_y_from_center = total_h / 2
    
    if bottom_y_from_center >= safe_radius:
        break
        
    # Calculate max allowed width at this Y to stay inside the circular mask
    max_x = math.sqrt(safe_radius**2 - bottom_y_from_center**2)
    max_w = max_x * 2
    
    # Keep 30px safety padding from the absolute curve
    if w >= (max_w - 30):
        break
    font_size += 2

# Final bounding box calculation
left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
w = right - left
h = bottom - top

total_h = logo_size + gap + h
start_y = (canvas_size - total_h) / 2

# Paste logo
logo_x = (canvas_size - logo_size) // 2
img.paste(logo, (logo_x, int(start_y)), logo)

gold_color = (255, 215, 0)
text_x = (canvas_size - w) / 2
text_y = start_y + logo_size + gap

# Draw text. Note: text_y must account for the font's internal ascender/top padding!
# By subtracting 'top', we guarantee the visual top of the text sits EXACTLY at text_y.
draw.text((text_x, text_y - top), text, font=font, fill=gold_color, stroke_width=2, stroke_fill=gold_color)

# Save to the xxxhdpi folder so Android perfectly maps 864px to 288dp!
os.makedirs("app/src/main/res/drawable-xxxhdpi", exist_ok=True)
img.save("app/src/main/res/drawable-xxxhdpi/splash_logo_combined.png")
print(f"Generated successfully with max logo {logo_size} and text width {w}")
