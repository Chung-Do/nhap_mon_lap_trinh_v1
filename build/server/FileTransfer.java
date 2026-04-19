import java.io.*;
import java.nio.file.*;

/** Function 5: Copy Files (Upload / Download) */
public class FileTransfer {

    private static final int BUFFER_SIZE = 8192;

    public static void listFiles(DataOutputStream out, String dirPath) throws IOException {
        try {
            File dir = new File(dirPath == null || dirPath.isEmpty() ? "." : dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                send(out, "ERROR", "Thu muc khong ton tai: " + dirPath);
                return;
            }
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    sb.append(String.format("%-6s %12d  %s\n",
                        f.isDirectory() ? "[DIR]" : "[FILE]", f.length(), f.getName()));
                }
            }
            send(out, "OK", sb.toString());
        } catch (Exception e) {
            send(out, "ERROR", e.getMessage());
        }
    }

    public static void sendFile(DataOutputStream out, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            send(out, "ERROR", "File khong ton tai: " + filePath);
            return;
        }
        byte[] data = Files.readAllBytes(file.toPath());
        out.writeUTF("BINARY");
        out.writeInt(data.length);
        int offset = 0;
        while (offset < data.length) {
            int chunk = Math.min(BUFFER_SIZE, data.length - offset);
            out.write(data, offset, chunk);
            offset += chunk;
        }
        out.flush();
    }

    public static void receiveFile(DataInputStream in, DataOutputStream out,
                                   String directory, String filename) throws IOException {
        try {
            int size = in.readInt();
            byte[] data = new byte[size];
            int total = 0;
            while (total < size) {
                int n = in.read(data, total, size - total);
                if (n < 0) throw new IOException("Ket noi bi dong khi nhan file");
                total += n;
            }
            File dir = new File(directory);
            if (!dir.exists()) dir.mkdirs();
            Files.write(Paths.get(directory, filename), data);
            send(out, "OK", "Da luu file: " + directory + File.separator + filename + " (" + size + " bytes)");
        } catch (Exception e) {
            send(out, "ERROR", "Loi nhan file: " + e.getMessage());
        }
    }

    private static void send(DataOutputStream out, String status, String data) throws IOException {
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }
}
