import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Webcam Stream - High-FPS continuous streaming
 *
 * Approach: Su dung 1 FFmpeg process lien tuc chay va ghi frames ra file,
 * client doc file lien tuc de hien thi (polling approach).
 *
 * Performance: 10-15 FPS (thay vi 0.6 FPS)
 */
public class WebcamStream {

    private static Process streamProcess = null;
    private static AtomicBoolean isStreaming = new AtomicBoolean(false);
    private static String streamOutputFile = null;
    private static Thread streamThread = null;
    private static String ffmpegPath = null;
    private static String activeCamera = null;

    /**
     * Get ffmpeg path
     */
    private static String getFFmpeg() throws Exception {
        if (ffmpegPath == null) {
            ffmpegPath = FFmpegManager.getFFmpegPath();
        }
        return ffmpegPath;
    }

    /**
     * Start streaming webcam
     * @param out Output stream
     * @param cameraName Camera name
     * @param quality Quality: "low", "medium", "high"
     */
    public static void startStream(DataOutputStream out, String cameraName, String quality) throws IOException {
        System.out.println("[STREAM] ========== START STREAM ==========");

        if (isStreaming.get()) {
            sendError(out, "Stream da chay roi!");
            return;
        }

        try {
            // Auto-detect camera neu empty
            if (cameraName == null || cameraName.trim().isEmpty()) {
                cameraName = WebcamCapture.detectFirstCamera();
                if (cameraName == null) {
                    cameraName = "Integrated Camera";
                }
            }
            activeCamera = cameraName;

            // Determine resolution and framerate based on quality
            String resolution, framerate, jpegQuality;
            switch (quality.toLowerCase()) {
                case "low":
                    resolution = "320x240";
                    framerate = "15";
                    jpegQuality = "10";
                    break;
                case "high":
                    resolution = "640x480";
                    framerate = "7";
                    jpegQuality = "5";
                    break;
                case "medium":
                default:
                    resolution = "480x360";
                    framerate = "10";
                    jpegQuality = "7";
                    break;
            }

            // Create temporary stream file
            streamOutputFile = System.getProperty("java.io.tmpdir") + File.separator + "webcam_stream.jpg";

            String ffmpeg = getFFmpeg();

            // FFmpeg command: Continuous capture to file, overwrite each frame
            // -f dshow: DirectShow input
            // -framerate: Capture at specified FPS
            // -video_size: Resolution
            // -i: Input device
            // -f image2: Output as image
            // -update 1: Continuously update same file
            // -q:v: JPEG quality (1-31, lower is better)
            String[] command = new String[] {
                ffmpeg,
                "-f", "dshow",
                "-framerate", framerate,
                "-video_size", resolution,
                "-rtbufsize", "5M",
                "-fflags", "nobuffer+flush_packets",
                "-i", "video=" + activeCamera,
                "-f", "image2",
                "-update", "1",
                "-q:v", jpegQuality,
                "-pix_fmt", "yuvj420p",
                "-y",
                streamOutputFile
            };

            System.out.println("[STREAM] Command: " + String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            streamProcess = pb.start();
            isStreaming.set(true);

            // Consume FFmpeg output in background thread
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(streamProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[STREAM FFmpeg] " + line);
                    }
                } catch (Exception e) {
                    System.err.println("[STREAM] Error reading FFmpeg output: " + e.getMessage());
                }
            }).start();

            // Wait a bit for FFmpeg to initialize and generate first frame
            Thread.sleep(1000);

            // Check if process is alive
            if (!streamProcess.isAlive()) {
                isStreaming.set(false);
                sendError(out, "Khong the bat dau stream (FFmpeg died)");
                return;
            }

            out.writeUTF(JsonUtil.textResponse("OK", "Stream started: " + streamOutputFile + "|" + resolution + "|" + framerate));
            out.flush();
            System.out.println("[STREAM] ========== STREAM STARTED ==========");

        } catch (Exception e) {
            isStreaming.set(false);
            System.err.println("[STREAM] ERROR: " + e.getMessage());
            e.printStackTrace();
            sendError(out, "Loi bat dau stream: " + e.getMessage());
        }
    }

    /**
     * Stop streaming
     */
    public static void stopStream(DataOutputStream out) throws IOException {
        System.out.println("[STREAM] ========== STOP STREAM ==========");

        if (!isStreaming.get()) {
            sendError(out, "Khong co stream nao dang chay!");
            return;
        }

        try {
            // Kill FFmpeg process
            if (streamProcess != null && streamProcess.isAlive()) {
                streamProcess.destroy();
                streamProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                if (streamProcess.isAlive()) {
                    streamProcess.destroyForcibly();
                }
            }

            isStreaming.set(false);
            streamProcess = null;

            // Delete stream file
            if (streamOutputFile != null) {
                new File(streamOutputFile).delete();
            }

            out.writeUTF(JsonUtil.textResponse("OK", "Stream stopped"));
            out.flush();
            System.out.println("[STREAM] ========== STREAM STOPPED ==========");

        } catch (Exception e) {
            isStreaming.set(false);
            System.err.println("[STREAM] ERROR: " + e.getMessage());
            sendError(out, "Loi dung stream: " + e.getMessage());
        }
    }

    /**
     * Get current frame from stream
     * @param out Output stream to send frame to client
     */
    public static void getStreamFrame(DataOutputStream out) throws IOException {
        if (!isStreaming.get()) {
            sendError(out, "Stream chua bat dau!");
            return;
        }

        try {
            File frameFile = new File(streamOutputFile);

            if (!frameFile.exists() || frameFile.length() == 0) {
                sendError(out, "Frame chua san sang");
                return;
            }

            // Read and send frame
            byte[] frameData = Files.readAllBytes(frameFile.toPath());

            out.writeUTF("BINARY");
            out.writeInt(frameData.length);
            out.write(frameData);
            out.flush();

        } catch (Exception e) {
            System.err.println("[STREAM] Error getting frame: " + e.getMessage());
            sendError(out, "Loi lay frame: " + e.getMessage());
        }
    }

    /**
     * Check if streaming is active
     */
    public static boolean isStreaming() {
        return isStreaming.get();
    }

    /**
     * Helper: Send error message
     */
    private static void sendError(DataOutputStream out, String msg) throws IOException {
        out.writeUTF(JsonUtil.textResponse("ERROR", msg));
        out.flush();
    }

    /**
     * Cleanup when server stops
     */
    public static void cleanup() {
        if (streamProcess != null && streamProcess.isAlive()) {
            streamProcess.destroyForcibly();
        }
        if (streamOutputFile != null) {
            new File(streamOutputFile).delete();
        }
    }
}
