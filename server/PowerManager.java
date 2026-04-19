import java.io.*;

/** Function 6: Shutdown / Restart */
public class PowerManager {

    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    public static void shutdown(DataOutputStream out) throws IOException {
        out.writeUTF(JsonUtil.textResponse("OK", "May tinh se tat sau 5 giay..."));
        out.flush();
        try {
            if (IS_WIN) Runtime.getRuntime().exec("shutdown /s /t 5");
            else        Runtime.getRuntime().exec(new String[]{"shutdown", "-h", "now"});
        } catch (Exception e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Loi shutdown: " + e.getMessage()));
            out.flush();
        }
    }

    public static void restart(DataOutputStream out) throws IOException {
        out.writeUTF(JsonUtil.textResponse("OK", "May tinh se restart sau 5 giay..."));
        out.flush();
        try {
            if (IS_WIN) Runtime.getRuntime().exec("shutdown /r /t 5");
            else        Runtime.getRuntime().exec(new String[]{"shutdown", "-r", "now"});
        } catch (Exception e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Loi restart: " + e.getMessage()));
            out.flush();
        }
    }
}
