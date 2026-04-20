import java.io.*;
import java.util.*;

/** Function 1: List / Start / Stop Applications (Windows Only) */
public class AppManager {

    public static void listApps(DataOutputStream out) throws IOException {
        System.out.println("[APP MANAGER] Listing applications...");

        try {
            String result = listAppsWindows();

            System.out.println("[APP MANAGER] Result length: " + result.length());
            if (result.length() < 100) {
                System.out.println("[APP MANAGER] Result: " + result);
            }

            send(out, "OK", result.isEmpty() ? "(Khong co ung dung)" : result);
        } catch (Exception e) {
            System.err.println("[APP MANAGER] Exception: " + e.getMessage());
            e.printStackTrace();
            send(out, "ERROR", "Exception: " + e.getMessage());
        }
    }

    /**
     * List running applications on Windows
     */
    private static String listAppsWindows() {
        try {
            System.out.println("[APP MANAGER] Executing Windows tasklist...");
            // Use tasklist with TABLE format for better compatibility
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FO", "TABLE");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Fix: Use Windows console encoding (CP850/OEM)
            String encoding = System.getProperty("os.name").toLowerCase().contains("win") ? "CP850" : "UTF-8";
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), encoding));

            StringBuilder result = new StringBuilder();
            result.append("=== RUNNING APPLICATIONS ===\n\n");

            String line;
            int count = 0;
            int lineNum = 0;
            Set<String> seenNames = new LinkedHashSet<>(); // Track unique app names

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // Skip empty lines and headers
                if (line.isEmpty() || lineNum <= 3) continue;

                // Parse TABLE format (fixed width columns)
                // Format: ImageName    PID    SessionName    Session#    MemUsage

                // Split by whitespace (2+ spaces)
                String[] parts = line.split("\\s{2,}");

                // Debug first few lines
                if (lineNum <= 10) {
                    System.out.println("[APP MANAGER] Line " + lineNum + " parts: " + parts.length + " -> " + java.util.Arrays.toString(parts));
                }

                if (parts.length >= 5) {
                    String imageName = parts[0].trim();
                    String pid = parts[1].trim();
                    String sessionName = parts[2].trim();
                    String memUsage = parts[4].trim();

                    // CHI hien thi User Apps - chi lay processes chay trong Console session
                    // Giong nhu Task Manager phan biet Apps vs Background processes
                    if (!sessionName.equalsIgnoreCase("Console")) {
                        continue;
                    }

                    // Skip system UI processes
                    String lowerName = imageName.toLowerCase();
                    if (lowerName.contains("dwm.exe") ||
                        lowerName.contains("csrss.exe") ||
                        lowerName.contains("winlogon.exe") ||
                        lowerName.contains("fontdrvhost.exe") ||
                        lowerName.contains("logonui.exe") ||
                        lowerName.contains("sihost.exe") ||
                        lowerName.contains("taskhostw.exe") ||
                        lowerName.contains("ctfmon.exe") ||
                        lowerName.contains("runtimebroker.exe") ||
                        lowerName.contains("searchapp.exe") ||
                        lowerName.contains("startmenuexperiencehost.exe") ||
                        lowerName.contains("textinputhost.exe") ||
                        lowerName.contains("shellexperiencehost.exe") ||
                        lowerName.contains("securityhealthsystray.exe")) {
                        continue;
                    }

                    // Only show each app once (many processes have duplicates)
                    String baseName = imageName.toLowerCase();
                    if (seenNames.contains(baseName)) {
                        continue;
                    }
                    seenNames.add(baseName);

                    // Format output
                    String appInfo = String.format("%-35s PID: %-8s  Mem: %s",
                                                   imageName, pid, memUsage);

                    result.append(appInfo).append("\n");
                    count++;

                    if (count >= 50) break; // Limit
                }
            }

            int exitCode = p.waitFor();

            if (count == 0) {
                System.out.println("[APP MANAGER] No apps found, exit code: " + exitCode);
                result.append("Khong tim thay ung dung nao.\n");
                result.append("Luu y: Chi hien thi cac ung dung nguoi dung, khong hien thi system processes.\n");
            } else {
                result.append("\n").append("─────────────────────────────────────────\n");
                result.append("Tong so: ").append(count).append(" ung dung dang chay");
            }

            System.out.println("[APP MANAGER] Found " + count + " applications (exit code: " + exitCode + ")");
            return result.toString();

        } catch (Exception e) {
            System.err.println("[APP MANAGER] Error listing Windows apps: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage() + "\n\nKhong the lay danh sach ung dung. Hay thu:\n" +
                   "1. Chay server voi quyen Administrator\n" +
                   "2. Kiem tra Windows Defender khong block server";
        }
    }


    public static void startApp(DataOutputStream out, String appName) throws IOException {
        try {
            System.out.println("[APP MANAGER] Starting app: " + appName);

            ProcessBuilder pb;
            // Windows: Try multiple methods
            // Method 1: Direct execution
            if (appName.toLowerCase().endsWith(".exe")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", appName);
            }
            // Method 2: Search in PATH and common locations
            else if (!appName.contains("\\") && !appName.contains("/")) {
                // Try to find in PATH
                pb = new ProcessBuilder("cmd", "/c", "start", "", appName + ".exe");
            } else {
                // Full path provided
                pb = new ProcessBuilder("cmd", "/c", "start", "", appName);
            }

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Wait a bit to check if it started
            Thread.sleep(500);

            if (p.isAlive() || !p.waitFor(100, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                send(out, "OK", "Da khoi dong ung dung: " + appName + "\n\n" +
                     "Luu y: Ung dung se mo tren may server, khong phai may client.");
            } else {
                int exitCode = p.exitValue();
                if (exitCode == 0) {
                    send(out, "OK", "Da khoi dong ung dung: " + appName);
                } else {
                    send(out, "ERROR", "Khong the mo ung dung. Exit code: " + exitCode + "\n\n" +
                         "Hay thu:\n" +
                         "- Nhap dung ten file .exe (VD: notepad.exe)\n" +
                         "- Hoac nhap full path (VD: C:\\Program Files\\App\\app.exe)");
                }
            }

        } catch (Exception e) {
            System.err.println("[APP MANAGER] Error starting app: " + e.getMessage());
            send(out, "ERROR", "Khong the mo ung dung: " + e.getMessage() + "\n\n" +
                 "Vi du:\n" +
                 "- notepad.exe\n" +
                 "- calc.exe\n" +
                 "- chrome.exe\n" +
                 "- C:\\path\\to\\app.exe");
        }
    }

    public static void stopApp(DataOutputStream out, String appName) throws IOException {
        try {
            System.out.println("[APP MANAGER] Stopping app: " + appName);

            // Make sure .exe extension is present
            if (!appName.toLowerCase().endsWith(".exe")) {
                appName = appName + ".exe";
            }

            String result = JsonUtil.executeCommand("taskkill /IM " + appName + " /F");

            // Check if successful
            if (result.toLowerCase().contains("success") || result.toLowerCase().contains("terminated")) {
                send(out, "OK", "Da dong ung dung: " + appName + "\n\n" + result);
            } else if (result.toLowerCase().contains("not found") || result.toLowerCase().contains("khong tim thay")) {
                send(out, "ERROR", "Khong tim thay tien trinh: " + appName + "\n\n" +
                     "Hay kiem tra lai ten chinh xac tu danh sach 'Liet ke'.");
            } else {
                send(out, "OK", result);
            }

        } catch (Exception e) {
            System.err.println("[APP MANAGER] Error stopping app: " + e.getMessage());
            send(out, "ERROR", "Loi khi dong ung dung: " + e.getMessage());
        }
    }

    private static void send(DataOutputStream out, String status, String data) throws IOException {
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }
}
