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
     * List running applications on Windows using tasklist /v
     * This enumerates all windows like Task Manager does
     */
    private static String listAppsWindows() {
        try {
            System.out.println("[APP MANAGER] Using tasklist /v to enumerate windows...");

            // tasklist /v shows window titles (column 9 in CSV)
            // /fo csv = CSV format, /nh = no header
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/v", "/fo", "csv", "/nh");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Use CP850 (OEM) encoding for Windows console
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "CP850"));

            StringBuilder result = new StringBuilder();
            result.append("=== RUNNING APPLICATIONS (Apps) ===\n\n");

            String line;
            int count = 0;

            // Track seen processes to avoid duplicates (gom multiple windows cung 1 process)
            Set<String> seenProcesses = new HashSet<>();

            // Filter out Windows system processes
            Set<String> systemProcesses = new HashSet<>(Arrays.asList(
                "system idle process", "system", "registry", "smss.exe", "csrss.exe",
                "wininit.exe", "winlogon.exe", "services.exe", "lsass.exe", "svchost.exe",
                "fontdrvhost.exe", "dwm.exe", "logonui.exe", "sihost.exe", "taskhostw.exe",
                "ctfmon.exe", "runtimebroker.exe", "searchapp.exe", "startmenuexperiencehost.exe",
                "textinputhost.exe", "shellexperiencehost.exe", "securityhealthsystray.exe",
                "securityhealthservice.exe", "msmpeng.exe", "nissrv.exe", "sgrmbroker.exe",
                "dllhost.exe", "conhost.exe", "smartscreen.exe", "wmiprvse.exe"
            ));

            while ((line = reader.readLine()) != null) {
                // Parse CSV: "ImageName","PID","SessionName","Session#","MemUsage","Status","UserName","CPUTime","WindowTitle"
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (parts.length >= 9) {
                    String imageName = parts[0].replace("\"", "").trim();
                    String pid = parts[1].replace("\"", "").trim();
                    String sessionName = parts[2].replace("\"", "").trim();
                    String memUsage = parts[4].replace("\"", "").trim();
                    String windowTitle = parts[8].replace("\"", "").trim();

                    // Filter: only user session (not Services)
                    if (sessionName.equalsIgnoreCase("Services")) {
                        continue;
                    }

                    // Filter: only processes with window titles (Apps section)
                    if (windowTitle.isEmpty() ||
                        windowTitle.equalsIgnoreCase("N/A") ||
                        windowTitle.equalsIgnoreCase("Running")) {
                        continue;
                    }

                    // Filter out system processes
                    if (systemProcesses.contains(imageName.toLowerCase())) {
                        continue;
                    }

                    // Skip if we already added this process (gom multiple windows)
                    String processKey = imageName.toLowerCase();
                    if (seenProcesses.contains(processKey)) {
                        continue;
                    }
                    seenProcesses.add(processKey);

                    // Get friendly name (FileDescription) from exe
                    String displayName = getFriendlyName(imageName, windowTitle);

                    // Format output
                    String appInfo = String.format("%-40s (%-20s)  PID: %-8s  Mem: %s",
                                                   displayName, imageName, pid, memUsage);

                    result.append(appInfo).append("\n");
                    count++;
                }
            }

            int exitCode = p.waitFor();

            if (count == 0) {
                System.out.println("[APP MANAGER] No apps found, exit code: " + exitCode);
                result.append("Khong tim thay ung dung nao.\n");
            } else {
                result.append("\n").append("─────────────────────────────────────────\n");
                result.append("Tong so: ").append(count).append(" ung dung");
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

    /**
     * Get friendly name (FileDescription) using WMIC
     * Fallback to manual mapping or window title
     */
    private static String getFriendlyName(String imageName, String windowTitle) {
        // Manual mapping for common apps (fast, no need WMIC)
        String lowerName = imageName.toLowerCase();
        switch (lowerName) {
            case "explorer.exe": return "Windows Explorer";
            case "taskmgr.exe": return "Task Manager";
            case "mspaint.exe": return "Paint";
            case "notepad.exe": return "Notepad";
            case "calc.exe": case "calculator.exe": return "Calculator";
            case "msedge.exe": return "Microsoft Edge";
            case "chrome.exe": return "Google Chrome";
            case "firefox.exe": return "Mozilla Firefox";
            case "wmplayer.exe": return "Windows Media Player";
            case "cmd.exe": return "Command Prompt";
            case "powershell.exe": return "Windows PowerShell";
            case "applicationframehost.exe": return windowTitle; // UWP apps
        }

        // Try WMIC for other apps (slower but accurate)
        try {
            String wmic = "wmic process where \"name='" + imageName + "'\" get Description /value";
            String result = JsonUtil.executeCommand(wmic);
            if (result.contains("Description=")) {
                String desc = result.substring(result.indexOf("Description=") + 12).trim();
                if (!desc.isEmpty() && !desc.equalsIgnoreCase(imageName)) {
                    return desc;
                }
            }
        } catch (Exception e) {
            // Ignore WMIC errors
        }

        // Fallback: use window title or process name
        if (windowTitle != null && !windowTitle.isEmpty() && !windowTitle.equalsIgnoreCase("N/A")) {
            return windowTitle;
        }
        return imageName.replace(".exe", "");
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
