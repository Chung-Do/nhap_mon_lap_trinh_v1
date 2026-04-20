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
            System.out.println("[APP MANAGER] Using PowerShell to get Apps with GUI (MainWindowTitle)...");

            // PowerShell: Get user apps (both with and without visible windows)
            // Include processes with MainWindowTitle OR processes in user session with known app paths
            // Get FileDescription (friendly name) like Task Manager shows
            // Use PrivateMemorySize64 (Private Bytes) for accurate memory like Task Manager
            String psCommand = "Get-Process | Where-Object {" +
                             "($_.MainWindowTitle -ne '') -or " +
                             "($_.Path -like '*\\Program Files\\*' -or $_.Path -like '*\\Program Files (x86)\\*' -or $_.Path -like '*\\Users\\*')" +
                             "} | Select-Object ProcessName, Id, MainWindowTitle, " +
                             "@{N='AppName';E={try{$_.MainModule.FileVersionInfo.FileDescription}catch{$_.ProcessName}}}, " +
                             "@{N='MemMB';E={[math]::Round($_.PrivateMemorySize64/1MB, 1)}} | " +
                             "ConvertTo-Csv -NoTypeInformation";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));

            StringBuilder result = new StringBuilder();
            result.append("=== RUNNING APPLICATIONS (Apps) ===\n\n");

            String line;
            int count = 0;
            boolean headerSkipped = false;

            // Filter out Windows system UI apps (but keep ApplicationFrameHost for UWP apps like Solitaire)
            Set<String> systemApps = new HashSet<>(Arrays.asList(
                "textinputhost", "searchapp", "startmenuexperiencehost",
                "shellexperiencehost", "runtimebroker",
                "windowsinternal.composableshell.experiences.textinput.inputapp"
            ));

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip CSV header
                if (!headerSkipped) {
                    if (line.startsWith("\"ProcessName\"") || line.startsWith("ProcessName")) {
                        headerSkipped = true;
                        continue;
                    }
                }

                // Parse CSV line: "ProcessName","Id","MainWindowTitle","AppName","MemMB"
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split CSV respecting quotes
                if (parts.length >= 5) {
                    String processName = parts[0].replace("\"", "").trim();
                    String pid = parts[1].replace("\"", "").trim();
                    String windowTitle = parts[2].replace("\"", "").trim();
                    String appName = parts[3].replace("\"", "").trim();
                    String memMB = parts[4].replace("\"", "").trim();

                    // Filter out system UI apps
                    if (systemApps.contains(processName.toLowerCase())) {
                        System.out.println("[APP MANAGER] Filtered out system app: " + processName);
                        continue;
                    }

                    // Use friendly name if available, fallback to process name
                    String displayName = (appName != null && !appName.isEmpty()) ? appName : processName;

                    // Format output
                    String appInfo = String.format("%-30s PID: %-8s  Mem: %4s MB",
                                                   displayName, pid, memMB);

                    result.append(appInfo).append("\n");
                    count++;
                }
            }

            int exitCode = p.waitFor();

            if (count == 0) {
                System.out.println("[APP MANAGER] No GUI apps found, exit code: " + exitCode);
                result.append("Khong tim thay ung dung nao.\n");
            } else {
                result.append("\n").append("─────────────────────────────────────────\n");
                result.append("Tong so: ").append(count).append(" ung dung");
            }

            System.out.println("[APP MANAGER] Found " + count + " GUI applications (exit code: " + exitCode + ")");
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
