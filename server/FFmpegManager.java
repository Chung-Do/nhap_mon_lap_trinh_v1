import java.io.*;
import java.net.URL;
import java.nio.file.*;

/**
 * FFmpeg Manager - Tu dong download va quan ly ffmpeg
 *
 * App se tu dong download ffmpeg lan dau tien chay, khong can user cai gi!
 *
 * Windows: Download ffmpeg.exe (essentials build ~70MB)
 * macOS: Huong dan cai qua Homebrew (1 dong lenh)
 * Linux: Huong dan cai qua apt/dnf (1 dong lenh)
 */
public class FFmpegManager {

    private static final String FFMPEG_DIR = "ffmpeg_bundle";
    private static String ffmpegPath = null;

    /**
     * Lay duong dan den ffmpeg executable.
     * Tu dong download neu chua co.
     */
    public static String getFFmpegPath() throws Exception {
        if (ffmpegPath != null) {
            return ffmpegPath;
        }

        // 1. Kiem tra ffmpeg da co trong system PATH chua
        if (isFFmpegInPath()) {
            ffmpegPath = "ffmpeg";
            System.out.println("[FFmpeg] Found in system PATH");
            return ffmpegPath;
        }

        // 2. Kiem tra ffmpeg da co trong bundle folder chua
        File bundleDir = new File(FFMPEG_DIR);
        File ffmpegFile = new File(bundleDir, isWindows() ? "ffmpeg.exe" : "ffmpeg");

        if (ffmpegFile.exists() && ffmpegFile.canExecute()) {
            ffmpegPath = ffmpegFile.getAbsolutePath();
            System.out.println("[FFmpeg] Found in bundle: " + ffmpegPath);
            return ffmpegPath;
        }

        // 3. Chua co → Tu dong setup
        System.out.println("[FFmpeg] Not found. Setting up...");
        setupFFmpeg(bundleDir, ffmpegFile);

        if (ffmpegFile.exists()) {
            ffmpegPath = ffmpegFile.getAbsolutePath();
            return ffmpegPath;
        }

        throw new Exception("Khong the setup ffmpeg. Vui long xem huong dan trong console.");
    }

    /**
     * Kiem tra ffmpeg co trong system PATH khong
     */
    private static boolean isFFmpegInPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Doc output
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();

            int exitCode = p.waitFor();
            return exitCode == 0 && line != null && line.contains("ffmpeg version");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Setup ffmpeg tu dong
     */
    private static void setupFFmpeg(File bundleDir, File ffmpegFile) throws Exception {
        bundleDir.mkdirs();

        if (isWindows()) {
            setupFFmpegWindows(ffmpegFile);
        } else if (isMac()) {
            setupFFmpegMac();
        } else {
            setupFFmpegLinux();
        }
    }

    /**
     * Windows: Tu dong download ffmpeg.exe
     */
    private static void setupFFmpegWindows(File ffmpegFile) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  WINDOWS: Tu dong download ffmpeg...                 ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // Danh sach URL fallback (thu lan luot neu url nao loi)
        String[] ffmpegUrls = {
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip",
            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
        };

        File zipFile = new File(FFMPEG_DIR, "ffmpeg.zip");
        boolean success = false;

        for (int i = 0; i < ffmpegUrls.length && !success; i++) {
            String ffmpegUrl = ffmpegUrls[i];
            System.out.println("[Attempt " + (i+1) + "/" + ffmpegUrls.length + "]");
            System.out.println("[1/4] Downloading ffmpeg...");
            System.out.println("      URL: " + ffmpegUrl);
            System.out.println("      Vui long cho, co the mat 1-2 phut...");

            try {
                // Download
                downloadFileWithProgress(ffmpegUrl, zipFile);
                System.out.println("\n      ✓ Download thanh cong! Size: " + (zipFile.length() / 1024 / 1024) + " MB");

                // Verify ZIP file
                System.out.println("[2/4] Verifying ZIP file...");
                if (!isValidZipFile(zipFile)) {
                    System.err.println("      ✗ File ZIP khong hop le, thu lai...");
                    zipFile.delete();
                    continue;
                }
                System.out.println("      ✓ ZIP file hop le");

                // Extract
                System.out.println("[3/4] Extracting ffmpeg.exe...");
                extractFFmpegFromZip(zipFile, ffmpegFile);
                System.out.println("      ✓ Extract thanh cong!");

                // Verify extracted file
                if (ffmpegFile.exists() && ffmpegFile.length() > 1000000) {
                    System.out.println("      ✓ ffmpeg.exe size: " + (ffmpegFile.length() / 1024 / 1024) + " MB");
                    success = true;
                } else {
                    System.err.println("      ✗ File extract bi loi, thu lai...");
                    ffmpegFile.delete();
                    continue;
                }

                // Cleanup
                System.out.println("[4/4] Cleaning up...");
                zipFile.delete();
                System.out.println("      ✓ Done!");

                System.out.println("");
                System.out.println("╔══════════════════════════════════════════════════════╗");
                System.out.println("║  ✓ FFmpeg da duoc cai dat thanh cong!               ║");
                System.out.println("║    Chuc nang webcam/video da san sang su dung.      ║");
                System.out.println("╚══════════════════════════════════════════════════════╝");
                System.out.println("");

            } catch (Exception e) {
                System.err.println("      ✗ Loi: " + e.getMessage());
                if (zipFile.exists()) zipFile.delete();
                if (i == ffmpegUrls.length - 1) {
                    // Lan thu cuoi cung van loi
                    System.err.println("");
                    System.err.println("✗ Tat ca cac nguon download deu that bai!");
                    System.err.println("");
                    printWindowsManualInstructions();
                    throw e;
                }
            }
        }

        if (!success) {
            throw new Exception("Khong the download ffmpeg tu bat ky nguon nao");
        }
    }

    /**
     * macOS: Huong dan cai qua Homebrew
     */
    private static void setupFFmpegMac() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  macOS: Can cai ffmpeg                               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("");
        System.out.println("Chay lenh sau trong Terminal:");
        System.out.println("");
        System.out.println("    brew install ffmpeg");
        System.out.println("");
        System.out.println("Neu chua co Homebrew, cai bang:");
        System.out.println("");
        System.out.println("    /bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"");
        System.out.println("");
        System.out.println("Sau khi cai xong, khoi dong lai server.");
        System.out.println("");
    }

    /**
     * Linux: Huong dan cai qua package manager
     */
    private static void setupFFmpegLinux() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Linux: Can cai ffmpeg                               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("");
        System.out.println("Chay lenh sau tuy theo distro:");
        System.out.println("");
        System.out.println("Ubuntu/Debian:");
        System.out.println("    sudo apt update && sudo apt install ffmpeg");
        System.out.println("");
        System.out.println("Fedora/CentOS:");
        System.out.println("    sudo dnf install ffmpeg");
        System.out.println("");
        System.out.println("Arch Linux:");
        System.out.println("    sudo pacman -S ffmpeg");
        System.out.println("");
        System.out.println("Sau khi cai xong, khoi dong lai server.");
        System.out.println("");
    }

    /**
     * Download file tu URL voi progress bar
     */
    private static void downloadFileWithProgress(String urlStr, File outputFile) throws Exception {
        System.out.println("      [DOWNLOAD DEBUG] Opening connection...");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.connect();

        int responseCode = conn.getResponseCode();
        System.out.println("      [DOWNLOAD DEBUG] Response code: " + responseCode);

        if (responseCode != 200) {
            throw new Exception("HTTP error: " + responseCode);
        }

        long fileSize = conn.getContentLengthLong();
        System.out.println("      [DOWNLOAD DEBUG] File size: " + (fileSize / 1024 / 1024) + " MB");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            long lastPrintedMB = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // Progress indicator (every MB)
                long currentMB = totalBytes / (1024 * 1024);
                if (currentMB > lastPrintedMB) {
                    lastPrintedMB = currentMB;
                    if (fileSize > 0) {
                        int percent = (int)((totalBytes * 100) / fileSize);
                        System.out.print("\r      Progress: " + currentMB + " MB / " + (fileSize/1024/1024) + " MB (" + percent + "%)");
                    } else {
                        System.out.print("\r      Downloaded: " + currentMB + " MB");
                    }
                }
            }
        }
    }

    /**
     * Kiem tra xem file ZIP co hop le khong
     */
    private static boolean isValidZipFile(File zipFile) {
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
            return zf.entries().hasMoreElements();
        } catch (Exception e) {
            System.err.println("      [ZIP DEBUG] Invalid ZIP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract ffmpeg.exe tu ZIP file
     */
    private static void extractFFmpegFromZip(File zipFile, File outputFile) throws Exception {
        System.out.println("      [EXTRACT DEBUG] Opening ZIP file...");
        System.out.println("      [EXTRACT DEBUG] ZIP size: " + (zipFile.length() / 1024 / 1024) + " MB");

        int entryCount = 0; // Declare outside try block

        // Dung java.util.zip de extract
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new FileInputStream(zipFile))) {

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                String name = entry.getName();

                // Log every 100 entries
                if (entryCount % 100 == 0) {
                    System.out.println("      [EXTRACT DEBUG] Processing entry " + entryCount + "...");
                }

                // Tim file ffmpeg.exe trong ZIP (thuong o bin/ffmpeg.exe)
                if (name.endsWith("bin/ffmpeg.exe") ||
                    name.endsWith("bin\\ffmpeg.exe") ||
                    name.equals("ffmpeg.exe") ||
                    name.contains("ffmpeg.exe")) {

                    System.out.println("      [EXTRACT DEBUG] Found ffmpeg.exe at: " + name);
                    System.out.println("      [EXTRACT DEBUG] Entry size: " + entry.getSize() + " bytes");

                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytes = 0;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
                        System.out.println("      [EXTRACT DEBUG] Extracted " + (totalBytes / 1024 / 1024) + " MB");
                    }

                    // Set executable permission
                    outputFile.setExecutable(true);
                    System.out.println("      [EXTRACT DEBUG] Set executable permission");
                    return;
                }
            }

            System.err.println("      [EXTRACT DEBUG] Scanned " + entryCount + " entries");
        }

        throw new Exception("Khong tim thay ffmpeg.exe trong file ZIP (scanned " + entryCount + " entries)");
    }

    /**
     * Huong dan thu cong cho Windows
     */
    private static void printWindowsManualInstructions() {
        System.err.println("╔══════════════════════════════════════════════════════╗");
        System.err.println("║  HUONG DAN THU CONG (Windows)                        ║");
        System.err.println("╚══════════════════════════════════════════════════════╝");
        System.err.println("");
        System.err.println("Option 1: Dung Chocolatey (de nhat)");
        System.err.println("  1. Mo PowerShell (admin)");
        System.err.println("  2. Chay: choco install ffmpeg");
        System.err.println("");
        System.err.println("Option 2: Download thu cong");
        System.err.println("  1. Truy cap: https://www.gyan.dev/ffmpeg/builds/");
        System.err.println("  2. Download: ffmpeg-release-essentials.zip");
        System.err.println("  3. Giai nen vao thu muc: " + new File(FFMPEG_DIR).getAbsolutePath());
        System.err.println("  4. Copy ffmpeg.exe vao thu muc tren");
        System.err.println("");
        System.err.println("Sau khi cai xong, khoi dong lai server.");
        System.err.println("");
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    /**
     * Test ffmpeg
     */
    public static void main(String[] args) {
        try {
            String path = getFFmpegPath();
            System.out.println("✓ FFmpeg path: " + path);

            // Test run
            ProcessBuilder pb = new ProcessBuilder(path, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            System.out.println("\nFFmpeg version info:");
            while ((line = reader.readLine()) != null && line.contains("version")) {
                System.out.println(line);
                break;
            }

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
