import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Xu ly 1 client tren 1 thread rieng.
 * Nhan callbacks de log va bao hieu ngat ket noi len Server GUI.
 */
public class ClientHandler implements Runnable {

    private final Socket           socket;
    private final Consumer<String> logger;
    private final Runnable         onDisconnect;
    private DataInputStream        in;
    private DataOutputStream       out;

    public ClientHandler(Socket socket, Consumer<String> logger, Runnable onDisconnect) {
        this.socket       = socket;
        this.logger       = logger;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // Gui welcome ngay sau khi accept
            sendText("OK", "Ket noi thanh cong! Server san sang.");

            while (true) {
                String json = in.readUTF();
                log("[CMD] " + json);
                if (!processCommand(json)) break;
            }
        } catch (EOFException e) {
            log("[HANDLER] Client ngat ket noi: " + socket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            log("[HANDLER] Loi I/O: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private boolean processCommand(String json) throws IOException {
        String cmd = JsonUtil.extractValue(json, "command").toUpperCase();
        switch (cmd) {
            // ── Apps ──
            case "LIST_APPS":
                AppManager.listApps(out);
                break;
            case "START_APP":
                AppManager.startApp(out, JsonUtil.extractValue(json, "app"));
                break;
            case "STOP_APP":
                AppManager.stopApp(out, JsonUtil.extractValue(json, "app"));
                break;

            // ── Processes ──
            case "LIST_PROCESSES":
                ProcessManager.listProcesses(out);
                break;
            case "START_PROCESS":
                ProcessManager.startProcess(out, JsonUtil.extractValue(json, "process"));
                break;
            case "KILL_PROCESS":
                ProcessManager.killProcess(out, JsonUtil.extractValue(json, "pid"));
                break;

            // ── Screen ──
            case "SCREENSHOT":
                ScreenCapture.capture(out);
                break;

            // ── Keyboard Monitor ──
            case "START_KEYLOG":
                KeyboardMonitor.startKeylog(out);
                break;
            case "GET_KEYLOG":
                KeyboardMonitor.getKeylogData(out);
                break;
            case "STOP_KEYLOG":
                KeyboardMonitor.stopKeylog(out);
                break;

            // ── File Transfer ──
            case "LIST_FILES":
                FileTransfer.listFiles(out, JsonUtil.extractValue(json, "directory"));
                break;
            case "DOWNLOAD_FILE":
                FileTransfer.sendFile(out, JsonUtil.extractValue(json, "filepath"));
                break;
            case "UPLOAD_FILE":
                FileTransfer.receiveFile(in, out,
                    JsonUtil.extractValue(json, "directory"),
                    JsonUtil.extractValue(json, "filename"));
                break;

            // ── Power ──
            case "SHUTDOWN":
                PowerManager.shutdown(out);
                break;
            case "RESTART":
                PowerManager.restart(out);
                break;

            // ── Webcam ──
            case "LIST_CAMERAS":
                WebcamCapture.listCameras(out);
                break;
            case "WEBCAM_CAPTURE":
                String cameraCapture = JsonUtil.extractValue(json, "camera");
                String qualityCapture = JsonUtil.extractValue(json, "quality");
                if (qualityCapture.isEmpty()) qualityCapture = "medium"; // Default
                WebcamCapture.capture(out, cameraCapture, qualityCapture);
                break;
            case "WEBCAM_START_RECORD":
                String cameraRecord = JsonUtil.extractValue(json, "camera");
                WebcamCapture.startRecording(out, cameraRecord);
                break;
            case "WEBCAM_STOP_RECORD":
                WebcamCapture.stopRecording(out);
                break;

            // ── Remote Desktop ──
            case "REMOTE_SCREEN_SIZE":
                RemoteDesktop.sendScreenSize(out);
                break;
            case "REMOTE_MOUSE":
                RemoteDesktop.clickMouse(out,
                    safeInt(JsonUtil.extractValue(json, "x")),
                    safeInt(JsonUtil.extractValue(json, "y")),
                    safeInt(JsonUtil.extractValue(json, "btn")));
                break;
            case "REMOTE_MOVE":
                RemoteDesktop.moveMouse(out,
                    safeInt(JsonUtil.extractValue(json, "x")),
                    safeInt(JsonUtil.extractValue(json, "y")));
                break;
            case "REMOTE_SCROLL":
                RemoteDesktop.scrollMouse(out,
                    safeInt(JsonUtil.extractValue(json, "x")),
                    safeInt(JsonUtil.extractValue(json, "y")),
                    safeInt(JsonUtil.extractValue(json, "amount")));
                break;
            case "REMOTE_KEY":
                RemoteDesktop.pressKey(out, safeInt(JsonUtil.extractValue(json, "code")));
                break;
            case "REMOTE_TYPE":
                RemoteDesktop.typeText(out, JsonUtil.extractValue(json, "text"));
                break;

            // ── Network Diagnostics ──
            case "NET_INFO":
                NetworkDiag.getNetworkInfo(out);
                break;
            case "PING_ECHO":
                NetworkDiag.pingEcho(out, JsonUtil.extractValue(json, "ts"));
                break;
            case "BANDWIDTH_TEST":
                NetworkDiag.bandwidthTest(out, safeInt(JsonUtil.extractValue(json, "size")));
                break;

            // ── Session ──
            case "DISCONNECT":
                sendText("OK", "Da ngat ket noi.");
                return false;

            default:
                sendText("ERROR", "Lenh khong hop le: " + cmd);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void sendText(String status, String data) throws IOException {
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }

    private void log(String msg) {
        if (logger != null) logger.accept(msg);
    }

    /** Chuyen String sang int an toan (tra ve 0 neu loi) */
    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}
