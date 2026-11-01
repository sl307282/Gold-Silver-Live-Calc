import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.*;

public class GenerateSplashAssets {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting splash asset generation...");

        // 1. Generate splash_tagline.png (1000x400)
        // Title: Gold Silver Live Calc
        // Tagline: Calculate with Confidence
        BufferedImage taglineImg = new BufferedImage(1000, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = taglineImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        String titleText = "Gold Silver Live Calc";
        String tagText = "Calculate with Confidence";

        // Try to load modern sans-serif bold font
        Font titleFont = new Font("Avenir Next", Font.BOLD, 82);
        if (!titleFont.getFamily().equalsIgnoreCase("Avenir Next")) {
            titleFont = new Font("Helvetica Neue", Font.BOLD, 82);
            if (!titleFont.getFamily().equalsIgnoreCase("Helvetica Neue")) {
                titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 82);
            }
        }

        Font tagFont = new Font(titleFont.getName(), Font.BOLD, 54);

        FontMetrics titleFm = g.getFontMetrics(titleFont);
        int titleW = titleFm.stringWidth(titleText);
        int titleAscent = titleFm.getAscent();

        FontMetrics tagFm = g.getFontMetrics(tagFont);
        int tagW = tagFm.stringWidth(tagText);
        int tagAscent = tagFm.getAscent();

        int titleX = (1000 - titleW) / 2;
        int titleY = titleAscent + 20;

        int tagX = (1000 - tagW) / 2;
        int tagY = titleY + tagAscent + 30;

        Color goldColor = new Color(255, 215, 0); // #FFD700
        g.setColor(goldColor);

        // Draw title
        g.setFont(titleFont);
        g.drawString(titleText, titleX, titleY);

        // Draw tagline
        g.setFont(tagFont);
        g.drawString(tagText, tagX, tagY);

        g.dispose();

        File taglineFile = new File("app/src/main/res/drawable-nodpi/splash_tagline.png");
        ImageIO.write(taglineImg, "png", taglineFile);
        System.out.println("Saved: " + taglineFile.getAbsolutePath());

        // 2. Generate splash_branding.png (1000x400)
        BufferedImage brandingImg = new BufferedImage(1000, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = brandingImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Font brandingFont = new Font(titleFont.getName(), Font.BOLD, 86);
        FontMetrics brandingFm = g2.getFontMetrics(brandingFont);
        int brandingW = brandingFm.stringWidth(titleText);
        int brandingAscent = brandingFm.getAscent();

        int brandingX = (1000 - brandingW) / 2;
        int brandingY = brandingAscent + 30;

        g2.setColor(goldColor);
        g2.setFont(brandingFont);
        g2.drawString(titleText, brandingX, brandingY);
        g2.dispose();

        File brandingFile = new File("app/src/main/res/drawable-nodpi/splash_branding.png");
        ImageIO.write(brandingImg, "png", brandingFile);
        System.out.println("Saved: " + brandingFile.getAbsolutePath());

        // 3. Update splash_logo_combined.png files:
        // In splash_logo_combined, the top contains the central logo badge (around top ~40% to 75%).
        // At the bottom, old text was "Gold & Silver Calculator".
        // Let's inspect existing drawable/splash_logo_combined.png
        File combinedFile = new File("app/src/main/res/drawable/splash_logo_combined.png");
        BufferedImage existing = ImageIO.read(combinedFile);
        int cw = existing.getWidth();
        int ch = existing.getHeight();
        System.out.println("Combined dimensions: " + cw + "x" + ch);

        // Clear the bottom text area (bottom 25% of height) and replace with "Gold Silver Live Calc"
        // Or keep only the logo badge centered
        BufferedImage updated = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g3 = updated.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Copy existing image but only the top part that contains the badge
        // Let's copy up to 75% height
        int badgeHeight = (int)(ch * 0.74);
        g3.drawImage(existing.getSubimage(0, 0, cw, badgeHeight), 0, 0, null);

        // Draw "Gold Silver Live Calc" at the bottom centered
        Font logoTextFont = new Font(titleFont.getName(), Font.BOLD, (int)(cw * 0.058));
        FontMetrics ltfm = g3.getFontMetrics(logoTextFont);
        int ltw = ltfm.stringWidth(titleText);
        int ltx = (cw - ltw) / 2;
        int lty = ch - (int)(ch * 0.12);

        g3.setFont(logoTextFont);
        g3.setColor(goldColor);
        g3.drawString(titleText, ltx, lty);
        g3.dispose();

        ImageIO.write(updated, "png", combinedFile);
        ImageIO.write(updated, "png", new File("app/src/main/res/drawable-nodpi/splash_logo_combined.png"));

        // For xxxhdpi (if exists, scale or create)
        File xxxFile = new File("app/src/main/res/drawable-xxxhdpi/splash_logo_combined.png");
        if (xxxFile.exists()) {
            BufferedImage xxxExisting = ImageIO.read(xxxFile);
            int xcw = xxxExisting.getWidth();
            int xch = xxxExisting.getHeight();
            BufferedImage xxxUpdated = new BufferedImage(xcw, xch, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g4 = xxxUpdated.createGraphics();
            g4.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g4.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int xbadgeHeight = (int)(xch * 0.74);
            g4.drawImage(xxxExisting.getSubimage(0, 0, xcw, xbadgeHeight), 0, 0, null);

            Font xlogoTextFont = new Font(titleFont.getName(), Font.BOLD, (int)(xcw * 0.058));
            FontMetrics xltfm = g4.getFontMetrics(xlogoTextFont);
            int xltw = xltfm.stringWidth(titleText);
            int xltx = (xcw - xltw) / 2;
            int xlty = xch - (int)(xch * 0.12);

            g4.setFont(xlogoTextFont);
            g4.setColor(goldColor);
            g4.drawString(titleText, xltx, xlty);
            g4.dispose();
            ImageIO.write(xxxUpdated, "png", xxxFile);
        }

        System.out.println("All splash assets updated successfully!");
    }
}
