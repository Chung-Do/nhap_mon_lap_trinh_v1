import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * SERVER GUI - Swing window thay the console.
 * Hien thi IP, port, danh sach client, log hoat dong.
 * Cho phep Start / Stop server tu giao dien.
 *
 * TINH NANG MOI:
 * - Chay nen voi System Tray icon
 * - Minimize to tray thay vi dong ung dung
 * - Right-click tray icon de show menu
 */
public class Server extends JFrame {

    private static final int DEFAULT_PORT = 9999;
    private static final int MAX_CLIENTS  = 10;

    private ServerSocket    serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;

    // ── UI components ──────────────────────────────────────────────────────
    private JTextField    portField;
    private JButton       startBtn, stopBtn;
    private JLabel        statusLabel, ipLabel;
    private DefaultListModel<String> clientModel;
    private JTextArea     logArea;

    // ── System Tray ──────────────────────────────────────────────────────
    private TrayIcon trayIcon;
    private SystemTray systemTray;

    // ─────────────────────────────────────────────────────────────────────────
    public Server() {
        super("Document");  // Nguỵ trang title giống Word document
        setSize(560, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Minimize to tray thay vi dong
        setLocationRelativeTo(null);

        // Set icon gia mao Word
        try {
            java.util.List<Image> icons = new java.util.ArrayList<>();
            icons.add(IconGenerator.createWordIcon(16));
            icons.add(IconGenerator.createWordIcon(32));
            icons.add(IconGenerator.createWordIcon(64));
            icons.add(IconGenerator.createWordIcon(128));
            setIconImages(icons);
        } catch (Exception e) {
            log("[WARN] Khong the load icon: " + e.getMessage());
        }

        // Xu ly khi dong cua so -> minimize to tray
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    hideToTray();
                } else {
                    // Neu khong co system tray thi confirm thoat
                    int choice = JOptionPane.showConfirmDialog(
                        Server.this,
                        "Ban co chac chan muon thoat?\n(Server se dung lai)",
                        "Xac nhan thoat",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        stopServer();
                        System.exit(0);
                    }
                }
            }
        });

        buildUI();
        setupSystemTray();

        String ip = getLocalIP();
        ipLabel.setText(ip);
        log("IP may nay: " + ip);
        log("Nhan 'Khoi dong' de bat dau lang nghe ket noi...");

        if (SystemTray.isSupported()) {
            log("Tip: Dong cua so de chay server o che do nen (system tray)");
        }
    }

    // ════════════════════════════════════════════
    //  BUILD UI
    // ════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildMidPanel(), BorderLayout.CENTER);
        root.add(buildLogPanel(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildTopPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createTitledBorder("Cau hinh Server"));

        // Row 1: port + buttons + status
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        row1.add(portField);

        startBtn = new JButton("▶  Khoi dong");
        startBtn.setBackground(new Color(0x388E3C));
        startBtn.setForeground(Color.WHITE);
        startBtn.setOpaque(true);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.setFont(startBtn.getFont().deriveFont(Font.BOLD, 13f));
        startBtn.addActionListener(e -> startServer());
        row1.add(startBtn);

        stopBtn = new JButton("■  Dung");
        stopBtn.setBackground(new Color(0xD32F2F));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setOpaque(true);
        stopBtn.setBorderPainted(false);
        stopBtn.setFocusPainted(false);
        stopBtn.setFont(stopBtn.getFont().deriveFont(Font.BOLD, 13f));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopServer());
        row1.add(stopBtn);

        statusLabel = new JLabel("  ●  Dang dung");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        row1.add(statusLabel);

        // Row 2: IP display + copy button
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.add(new JLabel("Dia chi IP cua may nay:"));
        ipLabel = new JLabel("...");
        ipLabel.setFont(ipLabel.getFont().deriveFont(Font.BOLD, 14f));
        ipLabel.setForeground(new Color(0x1565C0));
        row2.add(ipLabel);

        JButton copyBtn = new JButton("Copy IP");
        copyBtn.setBackground(new Color(0x1976D2));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setOpaque(true);
        copyBtn.setBorderPainted(false);
        copyBtn.setFocusPainted(false);
        copyBtn.setMargin(new Insets(1, 6, 1, 6));
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel =
                new java.awt.datatransfer.StringSelection(ipLabel.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            log("Da copy IP: " + ipLabel.getText());
        });
        row2.add(copyBtn);

        wrap.add(row1, BorderLayout.NORTH);
        wrap.add(row2, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildMidPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Client dang ket noi (0)"));
        clientModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientModel);
        clientList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        clientList.setBackground(new Color(0xFAFAFA));

        // Cap nhat tieu de khi model thay doi
        clientModel.addListDataListener(new javax.swing.event.ListDataListener() {
            void refresh() {
                ((TitledBorder)p.getBorder())
                    .setTitle("Client dang ket noi (" + clientModel.size() + ")");
                p.repaint();
            }
            public void intervalAdded(javax.swing.event.ListDataEvent e)   { refresh(); }
            public void intervalRemoved(javax.swing.event.ListDataEvent e) { refresh(); }
            public void contentsChanged(javax.swing.event.ListDataEvent e) { refresh(); }
        });

        p.add(new JScrollPane(clientList), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setBorder(BorderFactory.createTitledBorder("Log"));
        p.setPreferredSize(new Dimension(0, 250));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(0x1E1E1E));
        logArea.setForeground(new Color(0xD4D4D4));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton clearBtn = new JButton("Xoa log");
        clearBtn.setBackground(new Color(0x616161));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setOpaque(true);
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> logArea.setText(""));
        p.add(clearBtn, BorderLayout.SOUTH);
        return p;
    }

    // ════════════════════════════════════════════
    //  SYSTEM TRAY SUPPORT
    // ════════════════════════════════════════════
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            log("[WARN] System tray khong duoc ho tro tren he dieu hanh nay");
            return;
        }

        try {
            systemTray = SystemTray.getSystemTray();

            // Tao icon cho tray (dung icon mac dinh cua Java)
            Image image = createTrayImage();

            // Tao popup menu
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Hien thi");
            showItem.addActionListener(e -> showFromTray());
            popup.add(showItem);

            MenuItem hideItem = new MenuItem("An xuong tray");
            hideItem.addActionListener(e -> hideToTray());
            popup.add(hideItem);

            popup.addSeparator();

            MenuItem startItem = new MenuItem("Khoi dong Server");
            startItem.addActionListener(e -> {
                if (!running) {
                    SwingUtilities.invokeLater(() -> startServer());
                }
            });
            popup.add(startItem);

            MenuItem stopItem = new MenuItem("Dung Server");
            stopItem.addActionListener(e -> {
                if (running) {
                    SwingUtilities.invokeLater(() -> stopServer());
                }
            });
            popup.add(stopItem);

            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Thoat");
            exitItem.addActionListener(e -> exitApplication());
            popup.add(exitItem);

            // Tao tray icon (nguỵ trang tooltip)
            trayIcon = new TrayIcon(image, "Document", popup);
            trayIcon.setImageAutoSize(true);

            // Double-click de show window
            trayIcon.addActionListener(e -> showFromTray());

        } catch (Exception e) {
            log("[ERROR] Khong the tao system tray icon: " + e.getMessage());
        }
    }

    private Image createTrayImage() {
        // Su dung icon gia mao Word cho tray
        return IconGenerator.createServerTrayIcon(16, running);
    }

    private void hideToTray() {
        if (!SystemTray.isSupported()) return;

        try {
            if (trayIcon != null && systemTray != null) {
                systemTray.add(trayIcon);
                setVisible(false);
                log("[TRAY] Server dang chay o che do nen. Double-click tray icon de hien thi lai.");

                // Update tray icon tooltip
                updateTrayIcon();
            }
        } catch (AWTException e) {
            log("[ERROR] Khong the them icon vao system tray: " + e.getMessage());
        }
    }

    private void showFromTray() {
        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
        requestFocus();

        // Remove icon khoi tray khi show window
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    private void updateTrayIcon() {
        if (trayIcon != null) {
            // Cap nhat icon va tooltip (nguỵ trang)
            trayIcon.setImage(createTrayImage());
            String status = running ? "Document" : "Document";
            trayIcon.setToolTip(status);
        }
    }

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(
            null,
            "Ban co chac chan muon thoat?\n(Server se dung lai)",
            "Xac nhan thoat",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            if (running) {
                stopServer();
            }
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            System.exit(0);
        }
    }

    // ════════════════════════════════════════════
    //  SERVER CONTROL
    // ════════════════════════════════════════════
    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port khong hop le!", "Loi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                pool         = Executors.newFixedThreadPool(MAX_CLIENTS);
                serverSocket = new ServerSocket(port);
                running      = true;

                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    portField.setEnabled(false);
                    statusLabel.setText("  ●  Dang chay - port " + port);
                    statusLabel.setForeground(new Color(0x2E7D32));
                    log("[SERVER] Lang nghe tren port " + port);
                    log("[SERVER] Cho toi da " + MAX_CLIENTS + " client dong thoi");
                    updateTrayIcon();
                });

                while (running) {
                    Socket client = serverSocket.accept();
                    String clientIp = client.getInetAddress().getHostAddress();
                    log("[SERVER] ++ Client ket noi: " + clientIp);
                    SwingUtilities.invokeLater(() -> clientModel.addElement(clientIp));

                    Consumer<String> logger  = msg  -> log(msg);
                    Runnable onDisconnect    = ()   ->
                        SwingUtilities.invokeLater(() -> {
                            clientModel.removeElement(clientIp);
                            log("[SERVER] -- Client ngat: " + clientIp);
                        });

                    pool.execute(new ClientHandler(client, logger, onDisconnect));
                }
            } catch (IOException ex) {
                if (running) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(Server.this,
                            "Loi server: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE));
                    log("[SERVER] Loi: " + ex.getMessage());
                }
            }
        }).start();
    }

    private void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (pool != null) pool.shutdownNow();

        // Shutdown streaming thread pool
        ClientHandler.shutdownThreadPool();

        SwingUtilities.invokeLater(() -> {
            clientModel.clear();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            portField.setEnabled(true);
            statusLabel.setText("  ●  Dang dung");
            statusLabel.setForeground(Color.RED);
            log("[SERVER] Da dung.");
            updateTrayIcon();
        });
    }

    // ════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════
    public void log(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + ts + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String getLocalIP() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "N/A"; }
    }

    // ════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}
