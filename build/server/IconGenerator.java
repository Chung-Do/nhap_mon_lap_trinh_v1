import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * Tao cac icon cho ung dung
 */
public class IconGenerator {

    /**
     * Tao icon gia mao Word document
     * Mau xanh duong voi chu W trang
     */
    public static BufferedImage createWordIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background gradient (xanh duong Word)
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(0x2B579A),
            0, size, new Color(0x1B4470)
        );
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, size, size, size/8, size/8);

        // White border
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(size/32f));
        g.drawRoundRect(size/32, size/32, size - size/16, size - size/16, size/8, size/8);

        // Draw "W" letter
        g.setColor(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, size * 60 / 100);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        String text = "W";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - size/20;

        g.drawString(text, x, y);

        // Small document line effect
        g.setColor(new Color(255, 255, 255, 60));
        int lineY = size * 7 / 10;
        g.fillRect(size/4, lineY, size/2, size/40);

        g.dispose();
        return img;
    }

    /**
     * Tao icon cho client (may tinh voi sóng remote)
     */
    public static BufferedImage createClientIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient (xanh la)
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(0x43A047),
            0, size, new Color(0x2E7D32)
        );
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, size, size, size/8, size/8);

        // White border
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(size/32f));
        g.drawRoundRect(size/32, size/32, size - size/16, size - size/16, size/8, size/8);

        // Draw computer monitor
        g.setColor(Color.WHITE);
        int monW = size * 5 / 10;
        int monH = size * 4 / 10;
        int monX = (size - monW) / 2;
        int monY = size * 3 / 10;

        // Screen
        g.fillRoundRect(monX, monY, monW, monH, size/20, size/20);

        // Screen border (darker)
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(size/50f));
        g.drawRoundRect(monX, monY, monW, monH, size/20, size/20);

        // Stand
        g.setColor(Color.WHITE);
        int standW = size / 10;
        int standH = size / 12;
        int standX = (size - standW) / 2;
        int standY = monY + monH;
        g.fillRect(standX, standY, standW, standH);

        // Base
        int baseW = size * 3 / 10;
        int baseH = size / 20;
        int baseX = (size - baseW) / 2;
        int baseY = standY + standH;
        g.fillRoundRect(baseX, baseY, baseW, baseH, size/20, size/20);

        // Draw signal waves
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(size/40f));

        int centerX = size / 2;
        int centerY = monY + monH / 2;

        // 3 curved waves
        for (int i = 1; i <= 3; i++) {
            int radius = size * i / 8;
            g.drawArc(centerX - radius/2, centerY - radius/2, radius, radius, 45, 90);
        }

        g.dispose();
        return img;
    }

    /**
     * Tao icon tray cho server (nho hon, don gian hon)
     */
    public static BufferedImage createServerTrayIcon(int size, boolean isRunning) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background transparent
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);

        // Circle with W
        Color bgColor = isRunning ? new Color(0x2B579A) : new Color(0x888888);
        g.setColor(bgColor);
        g.fillOval(1, 1, size - 2, size - 2);

        // White W
        g.setColor(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, size * 60 / 100);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        String text = "W";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - size/20;

        g.drawString(text, x, y);

        g.dispose();
        return img;
    }

    /**
     * Luu icon thanh file PNG
     */
    public static void saveIcon(BufferedImage img, String filePath) {
        try {
            ImageIO.write(img, "PNG", new File(filePath));
            System.out.println("✓ Da luu: " + filePath);
        } catch (Exception e) {
            System.err.println("✗ Loi luu icon: " + e.getMessage());
        }
    }

    /**
     * Tao tat ca cac icon
     */
    public static void generateAllIcons() {
        System.out.println("=== Tao icon cho RemotePC ===");

        // Tao thu muc icons
        new File("icons").mkdirs();

        // Server icon (gia mao Word) - nhieu kich thuoc
        saveIcon(createWordIcon(16), "icons/server_16.png");
        saveIcon(createWordIcon(32), "icons/server_32.png");
        saveIcon(createWordIcon(64), "icons/server_64.png");
        saveIcon(createWordIcon(128), "icons/server_128.png");
        saveIcon(createWordIcon(256), "icons/server_256.png");

        // Client icon - nhieu kich thuoc
        saveIcon(createClientIcon(16), "icons/client_16.png");
        saveIcon(createClientIcon(32), "icons/client_32.png");
        saveIcon(createClientIcon(64), "icons/client_64.png");
        saveIcon(createClientIcon(128), "icons/client_128.png");
        saveIcon(createClientIcon(256), "icons/client_256.png");

        // Tray icons
        saveIcon(createServerTrayIcon(16, true), "icons/tray_running.png");
        saveIcon(createServerTrayIcon(16, false), "icons/tray_stopped.png");

        System.out.println("=== Hoan thanh! ===");
    }

    public static void main(String[] args) {
        generateAllIcons();
    }
}
