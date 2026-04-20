import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

/**
 * CLIENT GUI - Dieu khien PC tu xa
 * 8 tabs: Ung dung | Tien trinh | Man hinh | Ban phim | File | Nguon dien | Webcam | Dieu khien xa
 *
 * Tat ca socket I/O chay qua socketExec (single-thread executor) dam bao
 * khong bao gio co 2 lenh gui/nhan chay dong thoi (tranh protocol interleave).
 */
public class ClientGUI extends JFrame {

    // ── Ket noi ──────────────────────────────────────────────────────────────
    private final ClientConnection conn = new ClientConnection();
    private JTextField  ipField, portField;
    private JButton     connectBtn;
    private JLabel      statusLabel;

    /** Single-thread executor: serialize moi socket I/O, tranh race condition */
    private ExecutorService socketExec = Executors.newSingleThreadExecutor();

    // ── Text areas ────────────────────────────────────────────────────────────
    private JTextArea appsArea, procArea, keylogArea, fileArea, powerArea, logArea;

    // ── Screenshot ────────────────────────────────────────────────────────────
    private JLabel  screenLabel;
    private byte[]  lastScreenshot;
    private JButton saveScreenBtn;

    // ── Webcam ────────────────────────────────────────────────────────────────
    private JLabel  webcamLabel;
    private byte[]  lastWebcam;
    private JButton saveWebcamBtn;
    private JButton startRecordBtn;
    private JButton stopRecordBtn;
    private byte[]  lastVideo;
    private JTextField cameraNameField;  // Input cho ten camera
    private JComboBox<String> qualityComboBox;  // Quality selector
    private java.util.List<String> availableCameras = new java.util.ArrayList<>();  // Danh sach camera da quet

    // ── Webcam Stream ─────────────────────────────────────────────────────────
    private JPanel        webcamStreamPanel;
    private BufferedImage webcamStreamImage;
    private Timer         webcamStreamTimer;
    private AtomicBoolean webcamStreamBusy = new AtomicBoolean(false);
    private JButton       webcamStreamStartBtn, webcamStreamStopBtn;
    private JLabel        webcamStreamFpsLabel;

    // ── Apps / Processes / Files fields ──────────────────────────────────────
    private JTextField appStartField, appStopField;
    private JTextField procCmdField,  procPidField;
    private JTextField dirListField,  remoteFileField;

    // ── Remote Desktop ────────────────────────────────────────────────────────
    private JPanel        remotePanel;
    private BufferedImage remoteImage;
    private Timer         remoteTimer;
    private int           remoteScreenW = 1920, remoteScreenH = 1080;
    private AtomicBoolean remoteBusy    = new AtomicBoolean(false);
    private JButton       remoteStartBtn, remoteStopBtn;
    private JLabel        remoteFpsLabel;

    // ─────────────────────────────────────────────────────────────────────────
    public ClientGUI() {
        super("Remote PC Control");
        setSize(980, 740);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set icon cho client
        try {
            java.util.List<Image> icons = new java.util.ArrayList<>();
            icons.add(IconGenerator.createClientIcon(16));
            icons.add(IconGenerator.createClientIcon(32));
            icons.add(IconGenerator.createClientIcon(64));
            icons.add(IconGenerator.createClientIcon(128));
            setIconImages(icons);
        } catch (Exception e) {
            System.err.println("Khong the load icon: " + e.getMessage());
        }

        buildUI();
    }

    // ════════════════════════════════════════════
    //  BUILD UI
    // ════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(buildConnectionPanel(), BorderLayout.NORTH);
        root.add(buildTabs(),            BorderLayout.CENTER);
        root.add(buildLogPanel(),        BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBorder(BorderFactory.createTitledBorder("Ket noi"));
        p.add(new JLabel("IP:")); ipField   = new JTextField("127.0.0.1", 13); p.add(ipField);
        p.add(new JLabel("Port:")); portField = new JTextField("9999", 6);      p.add(portField);
        connectBtn = new JButton("Ket noi");
        connectBtn.setBackground(new Color(0x1976D2));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setOpaque(true);
        connectBtn.setBorderPainted(false);
        connectBtn.setFocusPainted(false);
        connectBtn.addActionListener(e -> toggleConnection());
        p.add(connectBtn);
        statusLabel = new JLabel("  Chua ket noi");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        p.add(statusLabel);
        return p;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Ung dung",      buildAppsTab());
        tabs.addTab("Tien trinh",    buildProcTab());
        tabs.addTab("Man hinh",      buildScreenTab());
        tabs.addTab("Ban phim",      buildKeylogTab());
        tabs.addTab("File",          buildFileTab());
        tabs.addTab("Nguon dien",    buildPowerTab());
        tabs.addTab("Webcam",        buildWebcamTab());
        tabs.addTab("Dieu khien xa", buildRemoteTab());
        tabs.addTab("Mang/Chan doan", buildNetDiagTab());
        tabs.addTab("🔒 Freeze",     buildFreezeTab());
        return tabs;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Log"));
        p.setPreferredSize(new Dimension(0, 100));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 1: APPS
    // ════════════════════════════════════════════
    private JPanel buildAppsTab() {
        JPanel p = panel(); JPanel ctrl = flow();
        btn(ctrl, "Liet ke", () -> {
            conn.sendCommand(JsonUtil.buildCommand("LIST_APPS"));
            String response = conn.readTextResponse();
            System.out.println("[DEBUG] LIST_APPS Response: " + response);
            System.out.println("[DEBUG] Response length: " + response.length());
            String extracted = extract(response);
            System.out.println("[DEBUG] Extracted data: " + extracted);
            System.out.println("[DEBUG] Extracted length: " + extracted.length());
            setOutput(appsArea, extracted);
            log("LIST_APPS OK");
        });
        ctrl.add(new JLabel("Ten app:")); appStartField = field(ctrl, 14);
        btn(ctrl, "Mo", () -> {
            final String n = appStartField.getText().trim();
            if (n.isEmpty()) { showErr("Nhap ten ung dung"); return; }
            conn.sendCommand(JsonUtil.buildCommand("START_APP","app",n));
            setOutput(appsArea, extract(conn.readTextResponse())); log("START_APP: "+n);
        });
        ctrl.add(new JLabel("Dong:")); appStopField = field(ctrl, 14);
        btn(ctrl, "Dong", () -> {
            final String n = appStopField.getText().trim();
            if (n.isEmpty()) { showErr("Nhap ten tien trinh"); return; }
            conn.sendCommand(JsonUtil.buildCommand("STOP_APP","app",n));
            setOutput(appsArea, extract(conn.readTextResponse())); log("STOP_APP: "+n);
        });
        appsArea = makeTextArea();
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(appsArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 2: PROCESSES
    // ════════════════════════════════════════════
    private JPanel buildProcTab() {
        JPanel p = panel(); JPanel ctrl = flow();
        btn(ctrl, "Liet ke", () -> {
            conn.sendCommand(JsonUtil.buildCommand("LIST_PROCESSES"));
            setOutput(procArea, extract(conn.readTextResponse())); log("LIST_PROCESSES OK");
        });
        ctrl.add(new JLabel("Lenh:")); procCmdField = field(ctrl, 14);
        btn(ctrl, "Chay", () -> {
            final String c = procCmdField.getText().trim();
            if (c.isEmpty()) { showErr("Nhap lenh"); return; }
            conn.sendCommand(JsonUtil.buildCommand("START_PROCESS","process",c));
            setOutput(procArea, extract(conn.readTextResponse())); log("START_PROCESS: "+c);
        });
        ctrl.add(new JLabel("PID:")); procPidField = field(ctrl, 7);
        btn(ctrl, "Kill", () -> {
            final String pid = procPidField.getText().trim();
            if (pid.isEmpty()) { showErr("Nhap PID"); return; }
            conn.sendCommand(JsonUtil.buildCommand("KILL_PROCESS","pid",pid));
            setOutput(procArea, extract(conn.readTextResponse())); log("KILL_PROCESS PID="+pid);
        });
        procArea = makeTextArea();
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(procArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 3: SCREENSHOT
    // ════════════════════════════════════════════
    private JPanel buildScreenTab() {
        JPanel p = panel(); JPanel ctrl = flow();
        btn(ctrl, "Chup man hinh", () -> {
            conn.sendCommand(JsonUtil.buildCommand("SCREENSHOT"));
            String marker = conn.readTextResponse();
            if ("BINARY".equals(marker)) {
                byte[] data = conn.readBinaryData();
                lastScreenshot = data;
                SwingUtilities.invokeLater(() -> { showImage(screenLabel, data); saveScreenBtn.setEnabled(true); });
                log("SCREENSHOT: " + data.length + " bytes");
            } else log("SCREENSHOT ERROR: " + extract(marker));
        });
        saveScreenBtn = new JButton("Luu anh");
        saveScreenBtn.setBackground(new Color(0x455A64));
        saveScreenBtn.setForeground(Color.WHITE);
        saveScreenBtn.setOpaque(true);
        saveScreenBtn.setBorderPainted(false);
        saveScreenBtn.setFocusPainted(false);
        saveScreenBtn.setEnabled(false);
        saveScreenBtn.addActionListener(e -> saveBytes(lastScreenshot, "screenshot_", ".png"));
        ctrl.add(saveScreenBtn);
        screenLabel = darkLabel("(Chua co anh)");
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(screenLabel), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 4: KEYLOGGER
    // ════════════════════════════════════════════
    private JPanel buildKeylogTab() {
        JPanel p = panel(); JPanel ctrl = flow();

        JButton startBtn = new JButton("Bat dau ghi");
        startBtn.setBackground(new Color(0x388E3C));
        startBtn.setForeground(Color.WHITE);
        startBtn.setOpaque(true);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("START_KEYLOG"));
            setOutput(keylogArea, extract(conn.readTextResponse())); log("START_KEYLOG");
        }));
        ctrl.add(startBtn);

        // Nut "Lay du lieu" - moi khi click se load phim tu server
        JButton getBtn = new JButton("Lay du lieu");
        getBtn.setBackground(new Color(0x1565C0));
        getBtn.setForeground(Color.WHITE);
        getBtn.setOpaque(true);
        getBtn.setBorderPainted(false);
        getBtn.setFocusPainted(false);
        getBtn.addActionListener(e -> runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("GET_KEYLOG"));
            setOutput(keylogArea, extract(conn.readTextResponse())); log("GET_KEYLOG OK");
        }));
        ctrl.add(getBtn);

        JButton stopBtn = new JButton("Dung ghi");
        stopBtn.setBackground(new Color(0xD32F2F));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setOpaque(true);
        stopBtn.setBorderPainted(false);
        stopBtn.setFocusPainted(false);
        stopBtn.addActionListener(e -> runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("STOP_KEYLOG"));
            setOutput(keylogArea, extract(conn.readTextResponse())); log("STOP_KEYLOG");
        }));
        ctrl.add(stopBtn);

        JButton clearBtn = new JButton("Xoa hien thi");
        clearBtn.setBackground(new Color(0x616161));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setOpaque(true);
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> keylogArea.setText(""));
        ctrl.add(clearBtn);

        keylogArea = makeTextArea();
        keylogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(keylogArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 5: FILE TRANSFER
    // ════════════════════════════════════════════
    private JPanel buildFileTab() {
        JPanel p = panel(); JPanel ctrl = flow();
        ctrl.add(new JLabel("Thu muc:")); dirListField = field(ctrl, 13);
        btn(ctrl, "Liet ke", () -> {
            final String dir = dirListField.getText().trim();
            conn.sendCommand(JsonUtil.buildCommand("LIST_FILES","directory",dir));
            setOutput(fileArea, extract(conn.readTextResponse())); log("LIST_FILES: "+dir);
        });
        ctrl.add(new JLabel("File remote:")); remoteFileField = field(ctrl, 13);
        // Download
        JButton dlBtn = new JButton("Tai ve");
        dlBtn.setBackground(new Color(0x00897B));
        dlBtn.setForeground(Color.WHITE);
        dlBtn.setOpaque(true);
        dlBtn.setBorderPainted(false);
        dlBtn.setFocusPainted(false);
        dlBtn.addActionListener(e -> {
            final String rp = remoteFileField.getText().trim();
            if (rp.isEmpty()) { showErr("Nhap duong dan file tren server"); return; }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(Paths.get(rp).getFileName().toString()));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            final String sp = fc.getSelectedFile().getAbsolutePath();
            runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("DOWNLOAD_FILE","filepath",rp));
                String marker = conn.readTextResponse();
                if ("BINARY".equals(marker)) {
                    byte[] data = conn.readBinaryData();
                    Files.write(Paths.get(sp), data);
                    setOutput(fileArea, "Da tai: "+sp+" ("+data.length+" bytes)"); log("DOWNLOAD OK");
                } else { setOutput(fileArea, extract(marker)); log("DOWNLOAD ERROR"); }
            });
        });
        ctrl.add(dlBtn);
        // Upload
        JButton ulBtn = new JButton("Gui len");
        ulBtn.setBackground(new Color(0x5E35B1));
        ulBtn.setForeground(Color.WHITE);
        ulBtn.setOpaque(true);
        ulBtn.setBorderPainted(false);
        ulBtn.setFocusPainted(false);
        ulBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            final File sel = fc.getSelectedFile();
            String di = JOptionPane.showInputDialog(this, "Thu muc luu tren server:", "C:\\");
            if (di == null) return;
            final String uploadDir = di.trim().isEmpty() ? "." : di.trim();
            final String uploadName = sel.getName();
            runTask(() -> {
                byte[] data = Files.readAllBytes(sel.toPath());
                conn.sendCommand(JsonUtil.buildCommand("UPLOAD_FILE","directory",uploadDir,"filename",uploadName));
                conn.sendFileData(data);
                setOutput(fileArea, extract(conn.readTextResponse())); log("UPLOAD: "+uploadName);
            });
        });
        ctrl.add(ulBtn);
        fileArea = makeTextArea();
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(fileArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 6: POWER
    // ════════════════════════════════════════════
    private JPanel buildPowerTab() {
        JPanel p = panel(); JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton shutBtn = new JButton("Tat may");
        shutBtn.setBackground(new Color(200,60,60));
        shutBtn.setForeground(Color.WHITE);
        shutBtn.setOpaque(true);
        shutBtn.setBorderPainted(false);
        shutBtn.setFocusPainted(false);
        shutBtn.addActionListener(e -> {
            if (confirm("Xac nhan TAT MAY server?")) runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("SHUTDOWN"));
                setOutput(powerArea, extract(conn.readTextResponse())); log("SHUTDOWN sent");
            });
        });
        ctrl.add(shutBtn);
        JButton restBtn = new JButton("Khoi dong lai");
        restBtn.setBackground(new Color(220,140,30));
        restBtn.setForeground(Color.WHITE);
        restBtn.setOpaque(true);
        restBtn.setBorderPainted(false);
        restBtn.setFocusPainted(false);
        restBtn.addActionListener(e -> {
            if (confirm("Xac nhan RESTART server?")) runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("RESTART"));
                setOutput(powerArea, extract(conn.readTextResponse())); log("RESTART sent");
            });
        });
        ctrl.add(restBtn);
        powerArea = makeTextArea();
        p.add(ctrl, BorderLayout.NORTH); p.add(new JScrollPane(powerArea), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 7: WEBCAM
    // ════════════════════════════════════════════
    private JPanel buildWebcamTab() {
        JPanel p = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        p.setBorder(new EmptyBorder(6, 6, 6, 6));

        // ═══════════════════════════════════════════════════════════════
        // PHAN 1: LIVE STREAM (tren cung)
        // ═══════════════════════════════════════════════════════════════
        JPanel streamSection = new JPanel(new java.awt.BorderLayout(4, 4));
        streamSection.setBorder(javax.swing.BorderFactory.createTitledBorder("📹 Stream Webcam Truc Tiep"));

        // Controls
        JPanel streamCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        // Camera selector - Quet va chon camera
        streamCtrl.add(new JLabel("Camera:"));

        JButton scanCameraBtn = new JButton("🔍 Quet");
        scanCameraBtn.setToolTipText("Quet danh sach camera tren server");
        scanCameraBtn.setBackground(new Color(0x1976D2));
        scanCameraBtn.setForeground(Color.WHITE);
        scanCameraBtn.setOpaque(true);
        scanCameraBtn.setBorderPainted(false);
        scanCameraBtn.setFocusPainted(false);
        scanCameraBtn.addActionListener(e -> scanCameras());
        streamCtrl.add(scanCameraBtn);

        cameraNameField = new JTextField("", 18);
        cameraNameField.setToolTipText("Chon tu dropdown sau khi quet, hoac de trong de auto-detect");
        streamCtrl.add(cameraNameField);

        JButton selectCameraBtn = new JButton("▼");
        selectCameraBtn.setToolTipText("Chon camera tu danh sach da quet");
        selectCameraBtn.addActionListener(e -> showCameraSelection());
        streamCtrl.add(selectCameraBtn);

        // Quality selector
        streamCtrl.add(new JLabel("  Quality:"));
        qualityComboBox = new JComboBox<>(new String[]{"Low (Fast ⚡)", "Medium (Balanced)", "High (Quality 🎨)"});
        qualityComboBox.setSelectedIndex(0); // Default to Low (Fast) for streaming
        qualityComboBox.setToolTipText("Low=320x240 (5-10 FPS), Medium=640x480 (2-5 FPS), High=1280x720 (0.5-2 FPS)");
        streamCtrl.add(qualityComboBox);

        webcamStreamStartBtn = new JButton("▶  BAT DAU STREAM");
        webcamStreamStartBtn.setBackground(new Color(0x388E3C));
        webcamStreamStartBtn.setForeground(Color.WHITE);
        webcamStreamStartBtn.setOpaque(true);
        webcamStreamStartBtn.setBorderPainted(false);
        webcamStreamStartBtn.setFocusPainted(false);
        webcamStreamStartBtn.setFont(webcamStreamStartBtn.getFont().deriveFont(14f).deriveFont(Font.BOLD));
        webcamStreamStartBtn.addActionListener(e -> startWebcamStream());
        streamCtrl.add(webcamStreamStartBtn);

        webcamStreamStopBtn = new JButton("■  DUNG STREAM");
        webcamStreamStopBtn.setBackground(new Color(0xD32F2F));
        webcamStreamStopBtn.setForeground(Color.WHITE);
        webcamStreamStopBtn.setOpaque(true);
        webcamStreamStopBtn.setBorderPainted(false);
        webcamStreamStopBtn.setFocusPainted(false);
        webcamStreamStopBtn.setFont(webcamStreamStopBtn.getFont().deriveFont(14f).deriveFont(Font.BOLD));
        webcamStreamStopBtn.setEnabled(false);
        webcamStreamStopBtn.addActionListener(e -> stopWebcamStream());
        streamCtrl.add(webcamStreamStopBtn);

        webcamStreamFpsLabel = new JLabel("  FPS: -");
        webcamStreamFpsLabel.setForeground(Color.GRAY);
        streamCtrl.add(webcamStreamFpsLabel);

        // Stream display panel
        webcamStreamPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (webcamStreamImage != null) {
                    g.drawImage(webcamStreamImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(new Color(0x2B2B2B));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.GRAY);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    String hint = "Nhan 'BAT DAU STREAM' de xem webcam truc tiep";
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2, getHeight() / 2);
                }
            }
        };
        webcamStreamPanel.setBackground(new Color(0x2B2B2B));
        webcamStreamPanel.setPreferredSize(new Dimension(640, 360));

        streamSection.add(streamCtrl, java.awt.BorderLayout.NORTH);
        streamSection.add(webcamStreamPanel, java.awt.BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════════
        // PHAN 2: CHUP ANH & RECORD VIDEO (duoi)
        // ═══════════════════════════════════════════════════════════════
        JPanel bottomSection = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));

        // ═══ BEN TRAI: CHUP ANH ═══
        JPanel leftPanel = new JPanel(new java.awt.BorderLayout(4, 4));
        leftPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("📷 Chup anh"));

        JPanel leftCtrl = flow();
        btn(leftCtrl, "Chup webcam", () -> {
            String cameraName = cameraNameField.getText().trim();
            String quality = getQualityFromComboBox();
            // Camera name co the empty (server se auto-detect)
            conn.sendCommand(JsonUtil.buildCommand("WEBCAM_CAPTURE", "camera", cameraName, "quality", quality));
            String marker = conn.readTextResponse();
            if ("BINARY".equals(marker)) {
                byte[] data = conn.readBinaryData(); lastWebcam = data;
                SwingUtilities.invokeLater(() -> { showImage(webcamLabel, data); saveWebcamBtn.setEnabled(true); });
                log("WEBCAM: " + data.length + " bytes");
            } else { String msg = extract(marker); log("WEBCAM ERROR: "+msg); showErr(msg); }
        });

        saveWebcamBtn = new JButton("Luu anh");
        saveWebcamBtn.setBackground(new Color(0x455A64));
        saveWebcamBtn.setForeground(Color.WHITE);
        saveWebcamBtn.setOpaque(true);
        saveWebcamBtn.setBorderPainted(false);
        saveWebcamBtn.setFocusPainted(false);
        saveWebcamBtn.setEnabled(false);
        saveWebcamBtn.addActionListener(e -> saveBytes(lastWebcam, "webcam_", ".jpg"));
        leftCtrl.add(saveWebcamBtn);

        webcamLabel = darkLabel("(Chua co anh)");
        leftPanel.add(leftCtrl, java.awt.BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(webcamLabel), java.awt.BorderLayout.CENTER);

        // ═══ BEN PHAI: RECORD VIDEO ═══
        JPanel rightPanel = new JPanel(new java.awt.BorderLayout(4, 4));
        rightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("🎥 Record Video"));

        JPanel rightCtrl = new JPanel(new java.awt.GridLayout(3, 1, 4, 8));

        // Button: Bat dau record
        startRecordBtn = new JButton("▶  BAT DAU RECORD");
        startRecordBtn.setBackground(new java.awt.Color(0x388E3C));
        startRecordBtn.setForeground(java.awt.Color.WHITE);
        startRecordBtn.setOpaque(true);
        startRecordBtn.setBorderPainted(false);
        startRecordBtn.setFocusPainted(false);
        startRecordBtn.setFont(startRecordBtn.getFont().deriveFont(14f).deriveFont(java.awt.Font.BOLD));
        startRecordBtn.addActionListener(e -> {
            // Dung webcam stream neu dang chay
            if (webcamStreamTimer != null && webcamStreamTimer.isRunning()) {
                stopWebcamStream();
                try { Thread.sleep(500); } catch (Exception ignored) {}
            }
            runTask(() -> {
                String cameraName = cameraNameField.getText().trim();
                // Camera name co the empty (server se auto-detect)
                conn.sendCommand(JsonUtil.buildCommand("WEBCAM_START_RECORD", "camera", cameraName));
                String resp = conn.readTextResponse();
                String msg = extract(resp);
                log("START RECORD: " + msg);
                if (resp.contains("\"OK\"")) {
                    SwingUtilities.invokeLater(() -> {
                        startRecordBtn.setEnabled(false);
                        stopRecordBtn.setEnabled(true);
                        webcamStreamStartBtn.setEnabled(false); // Disable stream khi dang record
                    });
                    showInfo("✓ Dang record video...\nBam Stop de ket thuc.");
                } else {
                    showErr(msg);
                }
            });
        });
        rightCtrl.add(startRecordBtn);

        // Button: Dung va lay video
        stopRecordBtn = new JButton("⏹  DUNG & LAY VIDEO");
        stopRecordBtn.setBackground(new java.awt.Color(0xD32F2F));
        stopRecordBtn.setForeground(java.awt.Color.WHITE);
        stopRecordBtn.setOpaque(true);
        stopRecordBtn.setBorderPainted(false);
        stopRecordBtn.setFocusPainted(false);
        stopRecordBtn.setFont(stopRecordBtn.getFont().deriveFont(14f).deriveFont(java.awt.Font.BOLD));
        stopRecordBtn.setEnabled(false);
        stopRecordBtn.addActionListener(e -> runTask(() -> {
            SwingUtilities.invokeLater(() -> stopRecordBtn.setEnabled(false)); // Disable ngay de tranh double-click
            log("Dang dung record va lay video... vui long cho...");
            conn.sendCommand(JsonUtil.buildCommand("WEBCAM_STOP_RECORD"));

            String marker = conn.readTextResponse();
            if ("BINARY".equals(marker)) {
                byte[] data = conn.readBinaryData();
                lastVideo = data;
                log("✓ Nhan duoc video: " + data.length + " bytes (" + (data.length/1024/1024) + " MB)");
                saveBytes(data, "webcam_video_", ".mp4");
                showInfo("✓ Video da duoc luu thanh cong!");
                SwingUtilities.invokeLater(() -> {
                    startRecordBtn.setEnabled(true);
                    webcamStreamStartBtn.setEnabled(true); // Enable lai stream sau khi record xong
                });
            } else {
                String msg = extract(marker);
                log("STOP RECORD ERROR: " + msg);
                showErr(msg);
                SwingUtilities.invokeLater(() -> {
                    startRecordBtn.setEnabled(true);
                    stopRecordBtn.setEnabled(true);
                    webcamStreamStartBtn.setEnabled(true); // Enable lai stream
                });
            }
        }));
        rightCtrl.add(stopRecordBtn);

        // Label huong dan
        JLabel infoLabel = new JLabel("<html><center>" +
            "<b>Cach su dung:</b><br>" +
            "1. Bam BAT DAU RECORD<br>" +
            "2. Camera se bat dau quay<br>" +
            "3. Bam DUNG & LAY VIDEO<br>" +
            "4. File video tu dong luu" +
            "</center></html>");
        infoLabel.setForeground(java.awt.Color.GRAY);
        infoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rightCtrl.add(infoLabel);

        rightPanel.add(rightCtrl, java.awt.BorderLayout.NORTH);

        // Them vao bottom section
        bottomSection.add(leftPanel);
        bottomSection.add(rightPanel);

        // Them ca 2 phan vao panel chinh
        p.add(streamSection);
        p.add(bottomSection);

        return p;
    }

    // ════════════════════════════════════════════
    //  TAB 8: REMOTE DESKTOP
    // ════════════════════════════════════════════
    private JPanel buildRemoteTab() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(new EmptyBorder(6,6,6,6));

        // ── Controls ──────────────────────────────────────────────────────────
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        remoteStartBtn = new JButton("▶  Bat dau (2fps)");
        remoteStartBtn.setBackground(new Color(0x388E3C));
        remoteStartBtn.setForeground(Color.WHITE);
        remoteStartBtn.setOpaque(true);
        remoteStartBtn.setBorderPainted(false);
        remoteStartBtn.setFocusPainted(false);
        remoteStartBtn.addActionListener(e -> startRemoteView());
        ctrl.add(remoteStartBtn);

        remoteStopBtn = new JButton("■  Dung");
        remoteStopBtn.setBackground(new Color(0xD32F2F));
        remoteStopBtn.setForeground(Color.WHITE);
        remoteStopBtn.setOpaque(true);
        remoteStopBtn.setBorderPainted(false);
        remoteStopBtn.setFocusPainted(false);
        remoteStopBtn.setEnabled(false);
        remoteStopBtn.addActionListener(e -> stopRemoteView());
        ctrl.add(remoteStopBtn);

        remoteFpsLabel = new JLabel("  FPS: -");
        remoteFpsLabel.setForeground(Color.GRAY);
        ctrl.add(remoteFpsLabel);

        ctrl.add(new JLabel("  |  Nhap text:"));
        JTextField typeField = new JTextField(22);
        JButton typeBtn = new JButton("Gui");
        typeBtn.setBackground(new Color(0x1976D2));
        typeBtn.setForeground(Color.WHITE);
        typeBtn.setOpaque(true);
        typeBtn.setBorderPainted(false);
        typeBtn.setFocusPainted(false);
        typeBtn.addActionListener(e -> sendRemoteText(typeField));
        typeField.addActionListener(e -> sendRemoteText(typeField));
        ctrl.add(typeField); ctrl.add(typeBtn);

        // ── Remote screen panel ───────────────────────────────────────────────
        remotePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (remoteImage != null) {
                    g.drawImage(remoteImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(new Color(0x2B2B2B)); g.fillRect(0,0,getWidth(),getHeight());
                    g.setColor(Color.GRAY);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    String hint = "Nhan 'Bat dau' de hien thi man hinh may chu";
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(hint, (getWidth()-fm.stringWidth(hint))/2, getHeight()/2);
                }
            }
        };
        remotePanel.setBackground(new Color(0x2B2B2B));
        remotePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        remotePanel.setFocusable(true);

        // Click chuot → gui toa do da scale ve kich thuoc man hinh thuc
        remotePanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!conn.isConnected()) return;
                int rx = toRemoteX(e.getX()), ry = toRemoteY(e.getY());
                int btn = (e.getButton() == MouseEvent.BUTTON3) ? 3 : 1;
                runTask(() -> {
                    conn.sendCommand(JsonUtil.buildCommand("REMOTE_MOUSE",
                        "x",String.valueOf(rx),"y",String.valueOf(ry),"btn",String.valueOf(btn)));
                    conn.readTextResponse();
                    log("CLICK ("+rx+","+ry+") btn="+btn);
                });
            }
        });

        // Scroll chuot
        remotePanel.addMouseWheelListener(e -> {
            if (!conn.isConnected()) return;
            int rx = toRemoteX(e.getX()), ry = toRemoteY(e.getY());
            final int amt = e.getWheelRotation();
            runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("REMOTE_SCROLL",
                    "x",String.valueOf(rx),"y",String.valueOf(ry),"amount",String.valueOf(amt)));
                conn.readTextResponse();
            });
        });

        // Phim dac biet (F1-F12, Delete, Arrows…)
        remotePanel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (!conn.isConnected() || !isSpecialKey(e.getKeyCode())) return;
                final int kc = e.getKeyCode();
                runTask(() -> {
                    conn.sendCommand(JsonUtil.buildCommand("REMOTE_KEY","code",String.valueOf(kc)));
                    conn.readTextResponse();
                });
            }
        });

        p.add(ctrl, BorderLayout.NORTH);
        p.add(remotePanel, BorderLayout.CENTER);

        JLabel hint = new JLabel(
            "Click chuot tren anh → click tren may chu  |  Cuon → scroll  |  Nhap text trong o phia tren",
            SwingConstants.CENTER);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(Color.GRAY);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private void sendRemoteText(JTextField tf) {
        final String txt = tf.getText();
        if (txt.isEmpty()) return;
        tf.setText("");
        runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("REMOTE_TYPE","text",txt));
            conn.readTextResponse(); log("REMOTE_TYPE: "+txt);
        });
    }

    private int toRemoteX(int px) { int w=remotePanel.getWidth(); return w>0 ? px*remoteScreenW/w : px; }
    private int toRemoteY(int py) { int h=remotePanel.getHeight(); return h>0 ? py*remoteScreenH/h : py; }

    private boolean isSpecialKey(int c) {
        return (c>=KeyEvent.VK_F1 && c<=KeyEvent.VK_F12)
            || c==KeyEvent.VK_DELETE   || c==KeyEvent.VK_BACK_SPACE
            || c==KeyEvent.VK_ESCAPE   || c==KeyEvent.VK_TAB
            || c==KeyEvent.VK_ENTER    || c==KeyEvent.VK_UP
            || c==KeyEvent.VK_DOWN     || c==KeyEvent.VK_LEFT
            || c==KeyEvent.VK_RIGHT    || c==KeyEvent.VK_HOME
            || c==KeyEvent.VK_END      || c==KeyEvent.VK_PAGE_UP
            || c==KeyEvent.VK_PAGE_DOWN;
    }

    // ── Remote Desktop lifecycle ──────────────────────────────────────────────
    private void startRemoteView() {
        if (!conn.isConnected()) { showErr("Chua ket noi den server"); return; }
        remoteStartBtn.setEnabled(false);
        remoteStopBtn.setEnabled(true);

        socketExec.submit(() -> {
            try {
                // 1. Lay kich thuoc man hinh server
                conn.sendCommand(JsonUtil.buildCommand("REMOTE_SCREEN_SIZE"));
                String resp = conn.readTextResponse();
                String size = JsonUtil.extractValue(resp, "data");
                String[] parts = size.split("x");
                if (parts.length == 2) {
                    remoteScreenW = Integer.parseInt(parts[0].trim());
                    remoteScreenH = Integer.parseInt(parts[1].trim());
                }
                log("Man hinh server: " + remoteScreenW + "x" + remoteScreenH);
            } catch (Exception ex) {
                log("Khong lay duoc kich thuoc man hinh: " + ex.getMessage());
            }

            // 2. Khoi dong timer tren EDT (500ms = 2fps)
            SwingUtilities.invokeLater(() -> {
                final long[] lastT = {System.currentTimeMillis()};
                remoteTimer = new Timer(500, ev -> {
                    if (remoteBusy.get()) return;
                    remoteBusy.set(true);
                    socketExec.submit(() -> {
                        try {
                            conn.sendCommand(JsonUtil.buildCommand("SCREENSHOT"));
                            String marker = conn.readTextResponse();
                            if ("BINARY".equals(marker)) {
                                byte[] data = conn.readBinaryData();
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                                if (img != null) {
                                    remoteImage = img;
                                    long now = System.currentTimeMillis();
                                    String fps = String.format("%.1f", 1000.0/(now-lastT[0]));
                                    lastT[0] = now;
                                    SwingUtilities.invokeLater(() -> {
                                        remotePanel.repaint();
                                        remoteFpsLabel.setText("  FPS: "+fps);
                                    });
                                }
                            }
                        } catch (Exception ex) {
                            log("LOI remote: " + ex.getMessage());
                            SwingUtilities.invokeLater(() -> stopRemoteView());
                        } finally {
                            remoteBusy.set(false);
                        }
                    });
                });
                remoteTimer.start();
                log("Remote view bat dau. Click tren anh de dieu khien.");
            });
        });
    }

    private void stopRemoteView() {
        if (remoteTimer != null) { remoteTimer.stop(); remoteTimer = null; }
        remoteBusy.set(false);
        SwingUtilities.invokeLater(() -> {
            remoteStartBtn.setEnabled(true);
            remoteStopBtn.setEnabled(false);
            remoteFpsLabel.setText("  FPS: -");
            remoteImage = null;
            remotePanel.repaint();
        });
        log("Remote view da dung.");
    }

    // ── Webcam Stream lifecycle ───────────────────────────────────────────────
    private void startWebcamStream() {
        if (!conn.isConnected()) {
            showErr("Chua ket noi den server");
            return;
        }

        // Camera name co the empty (server se auto-detect)
        final String cameraName = cameraNameField.getText().trim();
        final String quality = getQualityFromComboBox();

        webcamStreamStartBtn.setEnabled(false);
        webcamStreamStopBtn.setEnabled(true);
        startRecordBtn.setEnabled(false); // Disable record khi dang stream
        log("Bat dau stream webcam... (Quality: " + quality + ")");

        // Adjust timer based on quality (lower quality = faster FPS possible)
        int timerDelay = quality.equals("low") ? 100 : (quality.equals("medium") ? 200 : 300);

        // Khoi dong timer
        SwingUtilities.invokeLater(() -> {
            final long[] lastT = {System.currentTimeMillis()};
            webcamStreamTimer = new Timer(timerDelay, ev -> {
                if (webcamStreamBusy.get()) return;
                webcamStreamBusy.set(true);
                socketExec.submit(() -> {
                    try {
                        conn.sendCommand(JsonUtil.buildCommand("WEBCAM_CAPTURE", "camera", cameraName, "quality", quality));
                        String marker = conn.readTextResponse();
                        if ("BINARY".equals(marker)) {
                            byte[] data = conn.readBinaryData();
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                            if (img != null) {
                                webcamStreamImage = img;
                                long now = System.currentTimeMillis();
                                String fps = String.format("%.1f", 1000.0 / (now - lastT[0]));
                                lastT[0] = now;
                                SwingUtilities.invokeLater(() -> {
                                    webcamStreamPanel.repaint();
                                    webcamStreamFpsLabel.setText("  FPS: " + fps);
                                });
                            }
                        }
                    } catch (Exception ex) {
                        log("LOI webcam stream: " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> stopWebcamStream());
                    } finally {
                        webcamStreamBusy.set(false);
                    }
                });
            });
            webcamStreamTimer.start();
            log("Webcam stream da bat dau");
        });
    }

    private void stopWebcamStream() {
        if (webcamStreamTimer != null) {
            webcamStreamTimer.stop();
            webcamStreamTimer = null;
        }
        webcamStreamBusy.set(false);
        SwingUtilities.invokeLater(() -> {
            webcamStreamStartBtn.setEnabled(true);
            webcamStreamStopBtn.setEnabled(false);
            startRecordBtn.setEnabled(true); // Enable lai record sau khi stream dung
            webcamStreamFpsLabel.setText("  FPS: -");
            webcamStreamImage = null;
            webcamStreamPanel.repaint();
        });
        log("Webcam stream da dung.");
    }

    /**
     * Get quality setting from combo box
     * @return "low", "medium", or "high"
     */
    private String getQualityFromComboBox() {
        if (qualityComboBox == null) return "medium";
        int index = qualityComboBox.getSelectedIndex();
        switch (index) {
            case 0: return "low";    // Low (Fast)
            case 1: return "medium"; // Medium (Balanced)
            case 2: return "high";   // High (Quality)
            default: return "medium";
        }
    }

    /**
     * Quet danh sach camera tren server
     */
    private void scanCameras() {
        runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("LIST_CAMERAS"));
            String response = conn.readTextResponse();
            String data = extract(response);

            if (response.contains("\"status\":\"OK\"")) {
                // Parse danh sach camera tu response
                availableCameras.clear();
                String[] lines = data.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    // Tim dong dang: "1. Camera Name"
                    if (line.matches("^\\d+\\.\\s+.+$")) {
                        String cameraName = line.substring(line.indexOf('.') + 1).trim();
                        availableCameras.add(cameraName);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, data, "Danh sach Camera", JOptionPane.INFORMATION_MESSAGE);
                    if (!availableCameras.isEmpty()) {
                        // Tu dong chon camera dau tien
                        cameraNameField.setText(availableCameras.get(0));
                    }
                });
                log("SCAN CAMERA: Found " + availableCameras.size() + " camera(s)");
            } else {
                SwingUtilities.invokeLater(() -> {
                    showErr("Loi quet camera:\n" + data);
                });
                log("SCAN CAMERA ERROR: " + data);
            }
        });
    }

    /**
     * Hien thi dialog chon camera tu danh sach da quet
     */
    private void showCameraSelection() {
        if (availableCameras.isEmpty()) {
            showErr("Chua co danh sach camera!\nVui long bam nut 'Quet' truoc.");
            return;
        }

        String[] options = availableCameras.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Chon camera:",
            "Chon Camera",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (selected != null) {
            cameraNameField.setText(selected);
            log("Selected camera: " + selected);
        }
    }

    // ════════════════════════════════════════════
    //  TAB 9: MANG / CHAN DOAN (Network Diagnostics)
    // ════════════════════════════════════════════
    private JPanel buildNetDiagTab() {
        JPanel root = new JPanel(new GridLayout(3, 1, 0, 6));
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // ── Section 1: Network Info ───────────────────────────────────────────
        // Hoc ve: NetworkInterface, InetAddress, MAC, Subnet Mask, MTU, Gateway
        JPanel s1 = new JPanel(new BorderLayout(4, 4));
        s1.setBorder(BorderFactory.createTitledBorder(
            "1. Thong tin mang may chu (Interface / IP / MAC / Subnet / Gateway)"));

        JTextArea netArea = new JTextArea();
        netArea.setEditable(false);
        netArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JButton netBtn = new JButton("Lay thong tin mang");
        netBtn.setBackground(new Color(0x1976D2));
        netBtn.setForeground(Color.WHITE);
        netBtn.setOpaque(true);
        netBtn.setBorderPainted(false);
        netBtn.setFocusPainted(false);
        netBtn.addActionListener(e -> runTask(() -> {
            conn.sendCommand(JsonUtil.buildCommand("NET_INFO"));
            setOutput(netArea, extract(conn.readTextResponse()));
            log("NET_INFO OK");
        }));
        s1.add(netBtn, BorderLayout.NORTH);
        s1.add(new JScrollPane(netArea), BorderLayout.CENTER);

        // ── Section 2: RTT (Round-Trip Time) ─────────────────────────────────
        // Hoc ve: latency, jitter, TCP overhead so voi ICMP ping
        JPanel s2 = new JPanel(new BorderLayout(4, 4));
        s2.setBorder(BorderFactory.createTitledBorder(
            "2. Do tre mang (RTT - Round Trip Time)   [application-level ping qua TCP]"));

        JTextArea rttLog = new JTextArea(3, 1);
        rttLog.setEditable(false);
        rttLog.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JLabel rttResult = new JLabel("  Nhan 'Do RTT' de bat dau...", SwingConstants.LEFT);
        rttResult.setFont(new Font("Monospaced", Font.BOLD, 13));
        rttResult.setForeground(new Color(0x1565C0));

        JButton rttBtn = new JButton("Do RTT  (5 lan)");
        rttBtn.setBackground(new Color(0x1976D2));
        rttBtn.setForeground(Color.WHITE);
        rttBtn.setOpaque(true);
        rttBtn.setBorderPainted(false);
        rttBtn.setFocusPainted(false);
        rttBtn.addActionListener(e -> runTask(() -> {
            long[] rtts = new long[5];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                long t1 = System.currentTimeMillis();
                conn.sendCommand(JsonUtil.buildCommand("PING_ECHO", "ts", String.valueOf(t1)));
                conn.readTextResponse();           // server echo lai ngay lap tuc
                rtts[i] = System.currentTimeMillis() - t1;
                final int idx = i; final long rtt = rtts[i];
                sb.append(String.format("  Lan %d: %3d ms\n", idx + 1, rtt));
                if (i < 4) Thread.sleep(100);      // gap nho giua cac ping
            }
            long min = Long.MAX_VALUE, max = 0, sum = 0;
            for (long r : rtts) { min = Math.min(min, r); max = Math.max(max, r); sum += r; }
            final String detail = sb.toString();
            final String summary = String.format(
                "  Min: %d ms    Avg: %d ms    Max: %d ms    Jitter: %d ms",
                min, sum / 5, max, max - min);
            SwingUtilities.invokeLater(() -> {
                rttLog.setText(detail);
                rttResult.setText(summary);
            });
            log("PING RTT -> avg=" + (sum/5) + "ms  jitter=" + (max-min) + "ms");
        }));

        JPanel rttTop = new JPanel(new BorderLayout(6, 0));
        rttTop.add(rttBtn,   BorderLayout.WEST);
        rttTop.add(rttResult, BorderLayout.CENTER);
        s2.add(rttTop,                   BorderLayout.NORTH);
        s2.add(new JScrollPane(rttLog),  BorderLayout.CENTER);

        // ── Section 3: Bandwidth (TCP Throughput) ────────────────────────────
        // Hoc ve: TCP window size, congestion control, throughput vs latency
        JPanel s3 = new JPanel(new BorderLayout(4, 4));
        s3.setBorder(BorderFactory.createTitledBorder(
            "3. Do bang thong TCP (Throughput)   [server -> client, 1 luong]"));

        JLabel bwResult = new JLabel("  Nhan 'Bat dau do' de bat dau...", SwingConstants.LEFT);
        bwResult.setFont(new Font("Monospaced", Font.BOLD, 13));
        bwResult.setForeground(new Color(0x2E7D32));

        String[] sizeLabels = {"1 MB", "5 MB", "10 MB", "20 MB", "50 MB"};
        int[]    sizeBytes  = {
            1  * 1024 * 1024,
            5  * 1024 * 1024,
            10 * 1024 * 1024,
            20 * 1024 * 1024,
            50 * 1024 * 1024
        };
        JComboBox<String> sizeBox = new JComboBox<>(sizeLabels);

        JButton bwBtn = new JButton("Bat dau do bang thong");
        bwBtn.setBackground(new Color(0x00897B));
        bwBtn.setForeground(Color.WHITE);
        bwBtn.setOpaque(true);
        bwBtn.setBorderPainted(false);
        bwBtn.setFocusPainted(false);
        bwBtn.addActionListener(e -> {
            final int sz   = sizeBytes[sizeBox.getSelectedIndex()];
            final String sl = sizeLabels[sizeBox.getSelectedIndex()];
            SwingUtilities.invokeLater(() -> bwResult.setText("  Dang nhan du lieu..."));
            runTask(() -> {
                long t1 = System.currentTimeMillis();
                conn.sendCommand(JsonUtil.buildCommand("BANDWIDTH_TEST", "size", String.valueOf(sz)));
                String marker = conn.readTextResponse();
                if ("BINARY".equals(marker)) {
                    byte[] data   = conn.readBinaryData();
                    long elapsed  = Math.max(1, System.currentTimeMillis() - t1);
                    // Mbps = (bytes * 8 bits) / (elapsed_ms * 1000) / 1_000_000
                    double mbps   = (data.length * 8.0) / (elapsed * 1000.0);
                    double mbs    = data.length / 1024.0 / 1024.0 / (elapsed / 1000.0);
                    SwingUtilities.invokeLater(() -> bwResult.setText(String.format(
                        "  %s  trong %d ms   →   %.2f Mbps   (%.2f MB/s)",
                        sl, elapsed, mbps, mbs)));
                    log(String.format("BANDWIDTH %s: %.2f Mbps (%.2f MB/s)", sl, mbps, mbs));
                } else {
                    SwingUtilities.invokeLater(() -> bwResult.setText("  LOI: " + extract(marker)));
                }
            });
        });

        JPanel bwTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bwTop.add(new JLabel("Payload:"));
        bwTop.add(sizeBox);
        bwTop.add(bwBtn);
        s3.add(bwTop,    BorderLayout.NORTH);
        s3.add(bwResult, BorderLayout.CENTER);

        JLabel note = new JLabel(
            "  Throughput bi gioi han boi: TCP window, congestion control, bang thong LAN/WiFi",
            SwingConstants.LEFT);
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 10f));
        note.setForeground(Color.GRAY);
        s3.add(note, BorderLayout.SOUTH);

        root.add(s1);
        root.add(s2);
        root.add(s3);
        return root;
    }

    // ════════════════════════════════════════════
    //  TAB 10: FREEZE SCREEN
    // ════════════════════════════════════════════
    private JPanel buildFreezeTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Main Panel ──
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("🔒 Freeze Screen - Dong bang man hinh server"));

        // Warning message
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel warningIcon = new JLabel("⚠️");
        warningIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        warningPanel.add(warningIcon);

        JTextArea warningText = new JTextArea(
            "CANH BAO: Tinh nang nay se:\n" +
            "• Dong bang toan bo man hinh server\n" +
            "• Block chuot va ban phim\n" +
            "• Hien thi thong bao fullscreen\n" +
            "• Chi co the mo khoa tu client nay\n\n" +
            "CHI su dung cho muc dich:\n" +
            "✓ Giao duc / Demo bao mat\n" +
            "✓ Testing voi su dong y\n" +
            "✗ KHONG su dung cho muc dich xau"
        );
        warningText.setEditable(false);
        warningText.setBackground(new Color(0xFFF3CD));
        warningText.setForeground(new Color(0x856404));
        warningText.setFont(new Font("Arial", Font.PLAIN, 13));
        warningText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        warningPanel.add(warningText);
        mainPanel.add(warningPanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // Message editor
        JPanel msgPanel = new JPanel(new BorderLayout(4, 4));
        msgPanel.setBorder(BorderFactory.createTitledBorder("Thong bao hien thi tren server"));

        JTextArea msgArea = new JTextArea(4, 40);
        msgArea.setText("HACKED BY DINO\nVCB 0123456\n500K de mo khoa");
        msgArea.setFont(new Font("Arial", Font.BOLD, 14));
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        JScrollPane msgScroll = new JScrollPane(msgArea);
        msgPanel.add(msgScroll, BorderLayout.CENTER);

        JLabel msgHint = new JLabel("💡 Moi dong se hien thi rieng biet. Su dung \\n de xuong dong.");
        msgHint.setFont(new Font("Arial", Font.PLAIN, 11));
        msgHint.setForeground(Color.GRAY);
        msgPanel.add(msgHint, BorderLayout.SOUTH);

        mainPanel.add(msgPanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // Control buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Freeze button
        JButton freezeBtn = new JButton("🔒 FREEZE SCREEN");
        freezeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        freezeBtn.setBackground(new Color(0xDC3545)); // Red
        freezeBtn.setForeground(Color.WHITE);
        freezeBtn.setOpaque(true);
        freezeBtn.setBorderPainted(false);
        freezeBtn.setFocusPainted(false);
        freezeBtn.setPreferredSize(new Dimension(200, 50));
        freezeBtn.addActionListener(e -> {
            String msg = msgArea.getText().trim();
            if (msg.isEmpty()) {
                showErr("Vui long nhap thong bao!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                "Ban chac chan muon FREEZE man hinh server?\n" +
                "Server se bi dong bang toan bo!\n" +
                "Chi co the mo khoa tu client nay.",
                "Xac nhan Freeze",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                runTask(() -> {
                    conn.sendCommand(JsonUtil.buildCommand("FREEZE_SCREEN", "message", msg));
                    String response = extract(conn.readTextResponse());
                    log("FREEZE: " + response);
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                            "✓ Server da bi FREEZE!\n\n" + response,
                            "Freeze Thanh Cong",
                            JOptionPane.INFORMATION_MESSAGE)
                    );
                });
            }
        });
        btnPanel.add(freezeBtn);

        // Unfreeze button
        JButton unfreezeBtn = new JButton("🔓 UNFREEZE (Mo khoa)");
        unfreezeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        unfreezeBtn.setBackground(new Color(0x28A745)); // Green
        unfreezeBtn.setForeground(Color.WHITE);
        unfreezeBtn.setOpaque(true);
        unfreezeBtn.setBorderPainted(false);
        unfreezeBtn.setFocusPainted(false);
        unfreezeBtn.setPreferredSize(new Dimension(200, 50));
        unfreezeBtn.addActionListener(e -> {
            runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("UNFREEZE_SCREEN"));
                String response = extract(conn.readTextResponse());
                log("UNFREEZE: " + response);
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "✓ Server da duoc mo khoa!\n\n" + response,
                        "Unfreeze Thanh Cong",
                        JOptionPane.INFORMATION_MESSAGE)
                );
            });
        });
        btnPanel.add(unfreezeBtn);

        // Check status button
        JButton checkBtn = new JButton("🔍 Kiem tra trang thai");
        checkBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        checkBtn.setBackground(new Color(0x007BFF)); // Blue
        checkBtn.setForeground(Color.WHITE);
        checkBtn.setOpaque(true);
        checkBtn.setBorderPainted(false);
        checkBtn.setFocusPainted(false);
        checkBtn.addActionListener(e -> {
            runTask(() -> {
                conn.sendCommand(JsonUtil.buildCommand("CHECK_FREEZE_STATUS"));
                String response = extract(conn.readTextResponse());
                log("CHECK FREEZE: " + response);
                String status = response.equals("FROZEN") ? "🔒 FROZEN (Dang dong bang)" : "🔓 NORMAL (Binh thuong)";
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Trang thai hien tai:\n" + status,
                        "Trang Thai Server",
                        JOptionPane.INFORMATION_MESSAGE)
                );
            });
        });
        btnPanel.add(checkBtn);

        mainPanel.add(btnPanel);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("ℹ️ Thong tin"));

        JTextArea infoText = new JTextArea(
            "CO CHE HOAT DONG:\n" +
            "1. Client gui lenh FREEZE_SCREEN den server\n" +
            "2. Server tao fullscreen window voi thong bao\n" +
            "3. Server block tat ca mouse/keyboard input\n" +
            "4. Chi client nay moi co the gui lenh UNFREEZE\n\n" +
            "CACH SU DUNG:\n" +
            "• Nhap thong bao (co the nhieu dong)\n" +
            "• Bam 'FREEZE SCREEN'\n" +
            "• De mo khoa: Bam 'UNFREEZE'\n\n" +
            "LUU Y BAO MAT:\n" +
            "⚠️  Tinh nang nay CHI de demo/giao duc\n" +
            "⚠️  KHONG lam dung cho muc dich toi pham\n" +
            "⚠️  Luon co su dong y cua nguoi su dung server"
        );
        infoText.setEditable(false);
        infoText.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoText.setBackground(new Color(0xF8F9FA));
        infoPanel.add(new JScrollPane(infoText), BorderLayout.CENTER);

        root.add(mainPanel, BorderLayout.NORTH);
        root.add(infoPanel, BorderLayout.CENTER);

        return root;
    }

    // ════════════════════════════════════════════
    //  CONNECTION TOGGLE
    // ════════════════════════════════════════════
    /**
     * Dung new Thread() truc tiep (KHONG qua runTask/socketExec) vi
     * khi chua ket noi, socketExec van co the nhan task nhung
     * conn.sendCommand se nem IOException "Chua ket noi".
     */
    private void toggleConnection() {
        if (conn.isConnected()) {
            stopRemoteView();
            stopWebcamStream();
            conn.disconnect();
            socketExec.shutdownNow();
            socketExec = Executors.newSingleThreadExecutor();
            SwingUtilities.invokeLater(() -> {
                connectBtn.setText("Ket noi"); connectBtn.setEnabled(true);
                statusLabel.setText("  Chua ket noi"); statusLabel.setForeground(Color.RED);
                ipField.setEnabled(true); portField.setEnabled(true);
            });
            log("Da ngat ket noi");
            return;
        }

        final String host = ipField.getText().trim();
        final String portStr = portField.getText().trim();
        if (host.isEmpty() || portStr.isEmpty()) { showErr("Nhap IP va Port"); return; }
        final int port;
        try { port = Integer.parseInt(portStr); }
        catch (NumberFormatException ex) { showErr("Port phai la so nguyen"); return; }

        connectBtn.setEnabled(false);
        connectBtn.setText("Dang ket noi...");

        new Thread(() -> {
            try {
                conn.connect(host, port);
                SwingUtilities.invokeLater(() -> {
                    connectBtn.setText("Ngat ket noi"); connectBtn.setEnabled(true);
                    statusLabel.setText("  Da ket noi: " + host);
                    statusLabel.setForeground(new Color(0, 160, 0));
                    ipField.setEnabled(false); portField.setEnabled(false);
                });
                log("Ket noi thanh cong: " + host + ":" + port);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    connectBtn.setText("Ket noi"); connectBtn.setEnabled(true);
                    showErr("Khong the ket noi toi " + host + ":" + port
                        + "\nLoi: " + ex.getMessage()
                        + "\n\nKiem tra:\n- Server da chay chua?\n- IP/Port dung khong?\n- Firewall chan port " + port + "?");
                });
                log("Ket noi that bai: " + ex.getMessage());
            }
        }).start();
    }

    // ════════════════════════════════════════════
    //  TASK RUNNER
    // ════════════════════════════════════════════
    /**
     * Moi socket I/O duoc serialize qua 1 thread duy nhat.
     * Dam bao khong bao gio co 2 lenh gui/nhan chay dong thoi.
     */
    private void runTask(IOTask task) {
        if (!conn.isConnected()) { showErr("Chua ket noi den server"); return; }
        socketExec.submit(() -> {
            try { task.run(); }
            catch (Exception ex) {
                log("LOI: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Loi: "+ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    @FunctionalInterface interface IOTask { void run() throws Exception; }

    // ════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════
    private String extract(String resp) { return JsonUtil.extractValue(resp, "data"); }

    private void setOutput(JTextArea area, String text) {
        if (area == null) return;
        SwingUtilities.invokeLater(() -> { area.setText(text==null?"":text); area.setCaretPosition(0); });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("["+new SimpleDateFormat("HH:mm:ss").format(new Date())+"] "+msg+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showErr(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, msg, "Loi", JOptionPane.ERROR_MESSAGE));
    }

    private void showInfo(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, msg, "Thong bao", JOptionPane.INFORMATION_MESSAGE));
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Xac nhan",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void showImage(JLabel label, byte[] data) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img==null) { label.setText("(Khong doc duoc anh)"); return; }
            int w = Math.max(1, label.getParent().getWidth()-20);
            int h = Math.max(1, label.getParent().getHeight()-20);
            label.setIcon(new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
            label.setText("");
        } catch (Exception e) { label.setText("Loi hien thi: "+e.getMessage()); }
    }

    private void saveBytes(byte[] data, String prefix, String suffix) {
        if (data==null) { showErr("Chua co du lieu"); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(prefix+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+suffix));
        if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try { Files.write(fc.getSelectedFile().toPath(), data); log("Da luu: "+fc.getSelectedFile()); }
        catch (IOException ex) { showErr("Loi luu: "+ex.getMessage()); }
    }

    // ── UI factory helpers ────────────────────────────────────────────────────
    private static JPanel panel() {
        JPanel p = new JPanel(new BorderLayout(4,4));
        p.setBorder(new EmptyBorder(6,6,6,6)); return p;
    }
    private static JPanel flow() { return new JPanel(new FlowLayout(FlowLayout.LEFT,6,4)); }
    private static JTextField field(JPanel p, int cols) {
        JTextField f = new JTextField(cols); p.add(f); return f;
    }
    private void btn(JPanel ctrl, String label, IOTask task) {
        JButton b = new JButton(label);
        b.setBackground(new Color(0x1976D2));
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.addActionListener(e -> runTask(task));
        ctrl.add(b);
    }
    private static JLabel darkLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setBackground(Color.DARK_GRAY); l.setOpaque(true); l.setForeground(Color.LIGHT_GRAY);
        return l;
    }
    private static JTextArea makeTextArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false); a.setFont(new Font("Monospaced",Font.PLAIN,12)); return a;
    }

    // ════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
