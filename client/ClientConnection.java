import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class ClientConnection {

    private Socket         socket;
    private DataInputStream  in;
    private DataOutputStream out;
    private volatile boolean connected = false;

    /**
     * Ket noi TCP den server.
     * SAU KHI ket noi, SERVER gui ngay 1 welcome message.
     * Phai doc no o day, neu khong no nam dong trong buffer
     * va lam lech toan bo giao tiep tiep theo.
     */
    public synchronized boolean connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(5000); // timeout cho viec doc welcome

            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // Doc va bo qua welcome message tu server
            String welcome = in.readUTF();
            System.out.println("[CLIENT] Server: " + welcome);

            socket.setSoTimeout(0); // tat timeout sau khi da nhan welcome
            connected = true;
            return true;
        } catch (IOException e) {
            connected = false;
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            throw e;
        }
    }

    public synchronized void disconnect() {
        if (connected) {
            try { sendCommand(JsonUtil.buildCommand("DISCONNECT")); } catch (Exception ignored) {}
        }
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        connected = false;
    }

    public synchronized void sendCommand(String json) throws IOException {
        if (!connected) throw new IOException("Chua ket noi den server");
        out.writeUTF(json);
        out.flush();
    }

    public synchronized String readTextResponse() throws IOException {
        if (!connected) throw new IOException("Chua ket noi den server");
        return in.readUTF();
    }

    /**
     * Nhan binary data (anh, file) tu server.
     * Server da gui "BINARY" marker truoc, phuong thuc nay chi doc size + data.
     */
    public synchronized byte[] readBinaryData() throws IOException {
        if (!connected) throw new IOException("Chua ket noi den server");
        int size = in.readInt();
        byte[] data = new byte[size];
        int total = 0;
        while (total < size) {
            int n = in.read(data, total, size - total);
            if (n < 0) throw new IOException("Ket noi bi dong khi nhan du lieu");
            total += n;
        }
        return data;
    }

    public synchronized void sendFileData(byte[] data) throws IOException {
        if (!connected) throw new IOException("Chua ket noi den server");
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public synchronized boolean isConnected() { return connected; }
}
