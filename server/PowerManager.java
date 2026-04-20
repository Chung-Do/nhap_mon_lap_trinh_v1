import java.io.*;

/** Function 6: Shutdown / Restart (Windows Only) */
public class PowerManager {

    public static void shutdown(DataOutputStream out) throws IOException {
        out.writeUTF(JsonUtil.textResponse("OK", "May tinh se tat sau 5 giay..."));
        out.flush();
        try {
            Runtime.getRuntime().exec("shutdown /s /t 5");
        } catch (Exception e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Loi shutdown: " + e.getMessage()));
            out.flush();
        }
    }

    public static void restart(DataOutputStream out) throws IOException {
        out.writeUTF(JsonUtil.textResponse("OK", "May tinh se restart sau 5 giay..."));
        out.flush();
        try {
            Runtime.getRuntime().exec("shutdown /r /t 5");
        } catch (Exception e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Loi restart: " + e.getMessage()));
            out.flush();
        }
    }
}
