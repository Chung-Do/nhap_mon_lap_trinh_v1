import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;

public class ServerLauncher {
    private static final String REPO_URL = "https://github.com/Chung-Do/nhap_mon_lap_trinh_v1";
    private static final String DOWNLOAD_URL = REPO_URL + "/releases/download/latest/RemotePC-Server.zip";
    private static final String ZIP_FILE = "RemotePC-Server.zip";
    private static final String EXTRACT_DIR = "RemotePC-Server";
    private static final String EXE_PATH = EXTRACT_DIR + "\\RemotePC-Server\\RemotePC-Server.exe";

    public static void main(String[] args) {
        System.out.println("===================================================");
        System.out.println(" Download and Run RemotePC Server");
        System.out.println("===================================================");

        try {
            // Step 1: Download
            System.out.println("\n[1/3] Downloading latest server build...");
            downloadFile(DOWNLOAD_URL, ZIP_FILE);
            System.out.println("     OK.");

            // Step 2: Extract
            System.out.println("\n[2/3] Extracting...");
            deleteDirectory(new File(EXTRACT_DIR));
            unzip(ZIP_FILE, ".");
            System.out.println("     OK.");

            // Step 3: Run
            System.out.println("\n[3/3] Starting server...");
            File exeFile = new File(EXE_PATH);
            if (!exeFile.exists()) {
                throw new FileNotFoundException("EXE not found: " + EXE_PATH);
            }

            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
            pb.directory(exeFile.getParentFile());
            pb.start();

            System.out.println("     Server started!");
            System.out.println("\n==> Server is running. You can close this window.");

            Thread.sleep(3000);

        } catch (Exception e) {
            System.err.println("\nERROR: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nPress Enter to exit...");
            try { System.in.read(); } catch (IOException ignored) {}
            System.exit(1);
        }
    }

    private static void downloadFile(String urlStr, String outputFile) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);

        // Follow redirects
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
            status == HttpURLConnection.HTTP_MOVED_PERM ||
            status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            long fileSize = conn.getContentLengthLong();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (fileSize > 0) {
                    int percent = (int) ((totalBytes * 100) / fileSize);
                    System.out.print("\r     Progress: " + percent + "%");
                }
            }
            System.out.println();
        }
    }

    private static void unzip(String zipFile, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void deleteDirectory(File dir) throws IOException {
        if (dir.exists()) {
            Files.walk(dir.toPath())
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
}
