from PIL import Image, ImageDraw, ImageFont
import os

# Create a transparent canvas that matches the 2.5 aspect ratio of the 200dp x 80dp branding slot
# 1000x400 is high resolution enough for crisp rendering
img = Image.new('RGBA', (1000, 400), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# We have an 1000x400 canvas. Let's draw two strings centered.
title_text = "Gold & Silver Calculator"
tag_text = "Calculate with Confidence"

title_font_path = "/System/Library/Fonts/Avenir Next.ttc"
fallback_path = "/System/Library/Fonts/HelveticaNeue.ttc"

# Try to load fonts
try:
    title_font = ImageFont.truetype(title_font_path, 80, index=7) # Bold
    tag_font = ImageFont.truetype(title_font_path, 60, index=7) # Bold
except:
    try:
        title_font = ImageFont.truetype(fallback_path, 80, index=4)
        tag_font = ImageFont.truetype(fallback_path, 60, index=4)
    except:
        title_font = ImageFont.load_default()
        tag_font = ImageFont.load_default()

# Measure title
title_bbox = draw.textbbox((0, 0), title_text, font=title_font)
title_w = title_bbox[2] - title_bbox[0]
title_h = title_bbox[3] - title_bbox[1]

# Measure tagline
tag_bbox = draw.textbbox((0, 0), tag_text, font=tag_font)
tag_w = tag_bbox[2] - tag_bbox[0]
tag_h = tag_bbox[3] - tag_bbox[1]

title_x = (1000 - title_w) / 2
# Pin the app name to the top edge of the canvas to keep it high up
title_y = 0

tag_x = (1000 - tag_w) / 2
# Position the tagline below the app name with a much larger gap for spacing
tag_y = title_h + 60

gold_color = (255, 215, 0)
fade_gold_color = (255, 215, 0, 200)

# Draw with thicker stroke to make it ultra-bold
draw.text((title_x, title_y - title_bbox[1]), title_text, font=title_font, fill=gold_color, stroke_width=3, stroke_fill=gold_color)
# Highlight the tagline with full opacity and massive bolding to match the app name
draw.text((tag_x, tag_y - tag_bbox[1]), tag_text, font=tag_font, fill=gold_color, stroke_width=3, stroke_fill=gold_color)

os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)
img.save("app/src/main/res/drawable-nodpi/splash_tagline.png")
print("Tagline image generated successfully!")
