import java.io.*;

/** Function 2: List / Start / Kill Processes */
public class ProcessManager {

    private static final boolean IS_WIN =
        System.getProperty("os.name").toLowerCase().contains("win");

    public static void listProcesses(DataOutputStream out) throws IOException {
        String result;
        if (IS_WIN) {
            // TABLE /NH: khong dung CSV (CSV co nhieu dau ngoac kep lam loi JSON escaping)
            // /NH = no header line
            result = JsonUtil.executeCommand("tasklist /FO TABLE /NH");
        } else {
            result = JsonUtil.executeCommand("ps -eo pid,pcpu,pmem,comm --sort=-%cpu | head -80");
        }
        if (result == null || result.trim().isEmpty())
            result = "(Khong co ket qua)";

        // Gioi han kich thuoc: writeUTF co gioi han 65535 bytes UTF-8
        if (result.length() > 25000)
            result = result.substring(0, 25000) + "\n...[Bi cat bot do qua lon]";

        send(out, "OK", result);
    }

    public static void startProcess(DataOutputStream out, String command) throws IOException {
        try {
            new ProcessBuilder(command.split("\\s+"))
                .redirectErrorStream(true)
                .start();
            send(out, "OK", "Da khoi chay: " + command);
        } catch (Exception e) {
            send(out, "ERROR", "Loi: " + e.getMessage());
        }
    }

    public static void killProcess(DataOutputStream out, String pid) throws IOException {
        String result = IS_WIN
            ? JsonUtil.executeCommand("taskkill /PID " + pid + " /F")
            : JsonUtil.executeCommand("kill -9 " + pid);
        result = result.trim().isEmpty()
            ? "Da ket thuc tien trinh PID=" + pid
            : result;
        send(out, "OK", result);
    }

    private static void send(DataOutputStream out, String status, String data) throws IOException {
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }
}
