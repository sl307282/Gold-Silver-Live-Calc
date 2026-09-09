from PIL import Image, ImageDraw, ImageFont
import os

canvas_size = 864
img = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Logo size - Optimum Large Size
logo = Image.open("app/src/main/res/drawable/splash_logo.png").convert("RGBA")
# Crop out massive transparent padding so we only deal with the visual logo
logo = logo.crop(logo.getbbox())
logo_size = 250
logo = logo.resize((logo_size, int(logo_size * logo.height / logo.width)), Image.Resampling.LANCZOS)

total_h = logo.height
start_y = (canvas_size - total_h) / 2

# Draw a subtle premium gold radial glow behind the logo!
center_x = canvas_size // 2
center_y = int(start_y + logo.height / 2)
max_radius = 270

for r in range(max_radius, 0, -2):
    alpha = int((1 - (r / max_radius)) * 100)
    # Rich warm gold glow
    glow_color = (255, 200, 0, alpha)
    draw.ellipse((center_x - r, center_y - r, center_x + r, center_y + r), fill=glow_color)

# Paste logo
logo_x = (canvas_size - logo.width) // 2
img.paste(logo, (logo_x, int(start_y)), logo)

os.makedirs("app/src/main/res/drawable-xxxhdpi", exist_ok=True)
img.save("app/src/main/res/drawable-xxxhdpi/splash_logo_combined.png")
print(f"Generated successfully with optimum pure logo {logo_size}")

