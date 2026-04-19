import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Network Diagnostics - 3 chuc nang:
 *
 *  1. NET_INFO       - Liet ke tat ca network interface, IP, subnet mask, MAC, MTU, gateway
 *  2. PING_ECHO      - Echo lai timestamp ngay lap tuc -> client do RTT (Round-Trip Time)
 *  3. BANDWIDTH_TEST - Server gui N bytes ngau nhien -> client do throughput TCP
 *
 * Cac khai niem mang lien quan:
 *  - NetworkInterface / InetAddress    : OSI Layer 2-3 (Data Link + Network)
 *  - RTT / Latency                     : thoi gian mot packet di va ve
 *  - TCP Throughput                    : toc do truyen du lieu thuc te (bi anh huong boi
 *                                        window size, congestion control, buffer...)
 */
public class NetworkDiag {

    private static final boolean IS_WIN =
        System.getProperty("os.name").toLowerCase().contains("win");

    // ════════════════════════════════════════════
    //  1. NETWORK INFO
    // ════════════════════════════════════════════
    public static void getNetworkInfo(DataOutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("=== NETWORK INTERFACES ===\n\n");

        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                // Bo qua interface khong hoat dong va loopback (127.x.x.x)
                if (!ni.isUp()) continue;

                String displayName = ni.getDisplayName();
                sb.append("[").append(ni.getName()).append("] ").append(displayName).append("\n");

                // MAC Address (Layer 2 - Data Link)
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    sb.append("  MAC : ");
                    for (int i = 0; i < mac.length; i++)
                        sb.append(String.format("%02X%s", mac[i], i<mac.length-1 ? ":" : ""));
                    sb.append("\n");
                }

                // IP + Subnet Prefix (Layer 3 - Network)
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    int prefix = ia.getNetworkPrefixLength();
                    sb.append("  IP  : ").append(addr.getHostAddress())
                      .append(" / ").append(prefix);
                    // Tinh subnet mask tu prefix length (chi cho IPv4)
                    if (addr instanceof Inet4Address) {
                        int mask = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
                        sb.append(String.format("  [Mask: %d.%d.%d.%d]",
                            (mask >> 24) & 0xFF, (mask >> 16) & 0xFF,
                            (mask >>  8) & 0xFF,  mask        & 0xFF));
                    }
                    // Broadcast address
                    InetAddress bcast = ia.getBroadcast();
                    if (bcast != null)
                        sb.append("  Broadcast: ").append(bcast.getHostAddress());
                    sb.append("\n");
                }

                // MTU - Maximum Transmission Unit (so byte lon nhat trong 1 frame)
                sb.append("  MTU : ").append(ni.getMTU()).append(" bytes")
                  .append(ni.isLoopback() ? "  (loopback)" : "")
                  .append(ni.isVirtual()  ? "  (virtual)"  : "")
                  .append("\n\n");
            }
        } catch (SocketException e) {
            sb.append("Loi lay interface: ").append(e.getMessage()).append("\n\n");
        }

        // Default Gateway
        sb.append("=== DEFAULT GATEWAY ===\n");
        try {
            if (IS_WIN) {
                // route PRINT 0.0.0.0 chi hien route co destination 0.0.0.0 (default)
                String routeOut = JsonUtil.executeCommand("route PRINT 0.0.0.0");
                boolean found = false;
                for (String line : routeOut.split("\n")) {
                    String t = line.trim();
                    // Tim dong co dang: "0.0.0.0   0.0.0.0   <gateway>  <interface>  <metric>"
                    if (t.startsWith("0.0.0.0") && t.split("\\s+").length >= 4) {
                        String[] cols = t.split("\\s+");
                        if (cols.length >= 3) {
                            sb.append("  Gateway : ").append(cols[2]).append("\n");
                            if (cols.length >= 4)
                                sb.append("  Via iface: ").append(cols[3]).append("\n");
                            found = true;
                        }
                    }
                }
                if (!found) sb.append("  (Khong tim thay default gateway)\n");
            } else {
                String gw = JsonUtil.executeCommand("ip route show default");
                sb.append(gw.trim().isEmpty() ? "  (Khong co)" : gw);
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("  Loi doc gateway: ").append(e.getMessage()).append("\n");
        }

        // Hostname
        try {
            sb.append("\n=== HOSTNAME ===\n");
            sb.append("  ").append(InetAddress.getLocalHost().getHostName()).append("\n");
        } catch (Exception ignored) {}

        String result = sb.toString();
        if (result.length() > 28000)
            result = result.substring(0, 28000) + "\n...[cat bot]";

        out.writeUTF(JsonUtil.textResponse("OK", result));
        out.flush();
    }

    // ════════════════════════════════════════════
    //  2. PING ECHO  (do RTT)
    // ════════════════════════════════════════════
    /**
     * Client gui timestamp t1, server echo lai ngay, client do t2-t1 = RTT.
     *
     * Day la "application-level ping" qua TCP (khac ICMP ping cua OS).
     * RTT nay bao gom:
     *   - Thoi gian truyen TCP segment di (network latency)
     *   - Thoi gian xu ly tren server (rat nho, microseconds)
     *   - Thoi gian truyen TCP segment ve
     *   - TCP ACK overhead
     * Nen RTT o day >= ICMP ping (vi co them TCP/application overhead).
     */
    public static void pingEcho(DataOutputStream out, String clientTimestamp) throws IOException {
        // Echo lai chinh xac timestamp cua client -> client tinh duoc RTT
        out.writeUTF(JsonUtil.textResponse("OK", clientTimestamp));
        out.flush();
    }

    // ════════════════════════════════════════════
    //  3. BANDWIDTH TEST  (do throughput TCP)
    // ════════════════════════════════════════════
    /**
     * Server sinh ra 'sizeBytes' bytes va gui xuong client.
     * Client do thoi gian nhan het -> tinh Mbps.
     *
     * Throughput thuc te bi gioi han boi:
     *   - TCP Receive Window size (mac dinh ~65KB, auto-tuning len den ~16MB)
     *   - Congestion window (tang dan theo thuat toan Slow Start / AIMD)
     *   - Bandwidth cua duong truyen vat ly (LAN = 100Mbps/1Gbps, WiFi thap hon)
     *   - CPU / disk I/O cua ca hai dau
     *
     * Ket qua test nay gan voi 'iperf' nhung don gian hon (1 luong, 1 huong).
     */
    public static void bandwidthTest(DataOutputStream out, int sizeBytes) throws IOException {
        // Gioi han an toan: toi da 50MB
        if (sizeBytes <= 0 || sizeBytes > 50 * 1024 * 1024)
            sizeBytes = 1024 * 1024;

        try {
            // Dung pattern byte don gian (0x00..0xFF lap lai)
            // Khong can random vi muc tieu la test throughput mang, khong test entropy
            byte[] data = new byte[sizeBytes];
            for (int i = 0; i < sizeBytes; i++)
                data[i] = (byte) (i & 0xFF);

            // Dung Binary Protocol giong ScreenCapture
            out.writeUTF("BINARY");
            out.writeInt(sizeBytes);
            out.write(data);
            out.flush();
        } catch (OutOfMemoryError e) {
            out.writeUTF(JsonUtil.textResponse("ERROR", "Khong du RAM cho test nay: " + sizeBytes + " bytes"));
            out.flush();
        }
    }
}
