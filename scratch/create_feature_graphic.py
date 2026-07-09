import os
from PIL import Image, ImageDraw, ImageFont

def create_graphic():
    width, height = 1024, 500
    
    # Create dark base
    img = Image.new('RGB', (width, height), color='#0F1015')
    draw = ImageDraw.Draw(img, 'RGBA')
    
    # Draw subtle background gradient
    for i in range(height):
        # Very dark blue-gray fading to slightly lighter at the bottom
        r = int(15 + (i/height)*10)
        g = int(16 + (i/height)*15)
        b = int(21 + (i/height)*25)
        draw.line([(0, i), (width, i)], fill=(r, g, b))
        
    # Draw a stylized gold and silver chart line in the background
    silver_points = [(-50, 450), (200, 400), (400, 420), (600, 300), (800, 320), (1100, 200)]
    gold_points = [(-50, 400), (150, 350), (350, 380), (550, 220), (750, 250), (1100, 80)]
    
    draw.line(silver_points, fill='#E0E0E080', width=6, joint='curve') # Silver line (semi-transparent)
    draw.line(gold_points, fill='#D4AF37A0', width=8, joint='curve') # Gold line (semi-transparent)
    
    # Load fonts (using macOS built-in fonts)
    try:
        font_title = ImageFont.truetype('/System/Library/Fonts/Supplemental/Avenir.ttc', 80, index=2) # Avenir Heavy
        font_sub = ImageFont.truetype('/System/Library/Fonts/Supplemental/Avenir.ttc', 36, index=0) # Avenir Light
    except IOError:
        try:
            font_title = ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial Bold.ttf', 80)
            font_sub = ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial.ttf', 36)
        except IOError:
            font_title = ImageFont.load_default()
            font_sub = ImageFont.load_default()
            
    title = "Gold Silver Live Calc"
    subtitle = "Real-Time Rates & Smart Calculations"
    
    # Positioning
    x_pos = 80
    y_title = 160
    y_sub = 260
    
    # Draw Text Shadows
    draw.text((x_pos+3, y_title+3), title, font=font_title, fill='#000000A0')
    draw.text((x_pos+2, y_sub+2), subtitle, font=font_sub, fill='#00000080')
    
    # Draw Text (Gold color for title, Light gray for subtitle)
    draw.text((x_pos, y_title), title, font=font_title, fill='#D4AF37')
    draw.text((x_pos, y_sub), subtitle, font=font_sub, fill='#E0E0E0')

    # Save to Desktop
    out_path = os.path.expanduser('~/Desktop/FeatureGraphic.png')
    img.save(out_path)
    print(f"Success! Feature graphic saved to {out_path}")

if __name__ == '__main__':
    create_graphic()
