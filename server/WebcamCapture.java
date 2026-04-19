import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Function 7: Webcam Capture & Video Recording
 *
 * Su dung ffmpeg de chup anh va record video tu webcam.
 * Can cai ffmpeg: https://ffmpeg.org/download.html
 *
 * Flow:
 *  1. Bam "Record" → Bat dau record video tu camera (background)
 *  2. Bam "Stop & Lay video" → Dung record, gui file video ve client
 *
 * LUU Y BAO MAT:
 *   - Viec truy cap webcam can co su dong y cua nguoi dung.
 *   - Trong ung dung thuc te, can co co che xac thuc va thong bao truoc khi kich hoat webcam.
 */
public class WebcamCapture {

    private static Process recordingProcess = null;
    private static String currentVideoFile = null;
    private static boolean isRecording = false;
    private static String ffmpegPath = null;
    private static String detectedCamera = null;  // Cache detected camera

    /**
     * Get ffmpeg path (auto-setup neu can)
     */
    private static String getFFmpeg() throws Exception {
        if (ffmpegPath == null) {
            ffmpegPath = FFmpegManager.getFFmpegPath();
        }
        return ffmpegPath;
    }

    /**
     * Auto-detect camera dau tien co san tren he thong
     * @return Ten camera dau tien, hoac null neu khong tim thay
     */
    private static String detectFirstCamera() {
        // Neu da detect roi thi dung cache
        if (detectedCamera != null) {
            System.out.println("[CAMERA DETECT] Using cached camera: " + detectedCamera);
            return detectedCamera;
        }

        System.out.println("[CAMERA DETECT] ========== AUTO-DETECTING CAMERA ==========");
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

        try {
            String ffmpeg = getFFmpeg();
            ProcessBuilder pb;

            if (isWin) {
                // Windows: list devices
                System.out.println("[CAMERA DETECT] Scanning Windows cameras...");
                pb = new ProcessBuilder(ffmpeg, "-list_devices", "true", "-f", "dshow", "-i", "dummy");
            } else if (isMac) {
                // macOS: list devices
                System.out.println("[CAMERA DETECT] Scanning macOS cameras...");
                pb = new ProcessBuilder(ffmpeg, "-f", "avfoundation", "-list_devices", "true", "-i", "");
            } else {
                // Linux: check /dev/video0
                System.out.println("[CAMERA DETECT] Checking Linux camera at /dev/video0...");
                File videoDevice = new File("/dev/video0");
                if (videoDevice.exists()) {
                    detectedCamera = "/dev/video0";
                    System.out.println("[CAMERA DETECT] ✓ Found: " + detectedCamera);
                    return detectedCamera;
                }
                System.out.println("[CAMERA DETECT] ✗ No camera found at /dev/video0");
                return null;
            }

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Doc output va tim camera dau tien
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            boolean foundVideoSection = false;

            while ((line = reader.readLine()) != null) {
                System.out.println("[CAMERA DETECT] " + line);

                // Windows: Tim dong chua ten camera trong dau ""
                if (isWin) {
                    if (line.contains("DirectShow video devices") || line.contains("dshow")) {
                        foundVideoSection = true;
                    }
                    if (foundVideoSection && line.contains("\"")) {
                        // Extract ten camera tu dong nhu: [dshow @ ...] "Camera Name"
                        int start = line.indexOf("\"");
                        int end = line.indexOf("\"", start + 1);
                        if (start != -1 && end != -1) {
                            detectedCamera = line.substring(start + 1, end);
                            System.out.println("[CAMERA DETECT] ✓ Found Windows camera: " + detectedCamera);
                            p.destroy();
                            return detectedCamera;
                        }
                    }
                }

                // macOS: Tim camera index (thuong la 0)
                if (isMac) {
                    if (line.contains("AVFoundation video devices")) {
                        foundVideoSection = true;
                    }
                    if (foundVideoSection && line.matches(".*\\[\\d+\\].*")) {
                        // Extract index tu dong nhu: [AVFoundation indev @ ...] [0] FaceTime HD Camera
                        if (line.contains("[0]")) {
                            detectedCamera = "0";  // macOS dung index
                            System.out.println("[CAMERA DETECT] ✓ Found macOS camera at index: 0");
                            p.destroy();
                            return detectedCamera;
                        }
                    }
                }
            }

            p.waitFor();
            System.out.println("[CAMERA DETECT] ✗ No camera detected");
            return null;

        } catch (Exception e) {
            System.err.println("[CAMERA DETECT] ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Chup anh snapshot tu webcam
     * @param out Output stream to client
     * @param cameraName Ten camera (VD: "Samsung Slimfit Cam", "Integrated Camera")
     */
    public static void capture(DataOutputStream out, String cameraName) throws IOException {
        capture(out, cameraName, "medium"); // Default quality
    }

    /**
     * Chup anh snapshot tu webcam voi quality parameter
     * @param out Output stream to client
     * @param cameraName Ten camera
     * @param quality Quality setting: "low" (fast, 320x240), "medium" (balanced, 640x480), "high" (slow, 1280x720)
     */
    public static void capture(DataOutputStream out, String cameraName, String quality) throws IOException {
        String tmpFile = System.getProperty("java.io.tmpdir") + File.separator + "webcam_snap.jpg";
        boolean isWin  = System.getProperty("os.name").toLowerCase().contains("win");
        boolean isMac  = System.getProperty("os.name").toLowerCase().contains("mac");

        System.out.println("[WEBCAM DEBUG] ========== WEBCAM CAPTURE START ==========");
        System.out.println("[WEBCAM DEBUG] OS: " + System.getProperty("os.name"));
        System.out.println("[WEBCAM DEBUG] Camera (from client): " + (cameraName == null || cameraName.trim().isEmpty() ? "(empty)" : cameraName));
        System.out.println("[WEBCAM DEBUG] Temp file: " + tmpFile);

        try {
            String ffmpeg = getFFmpeg();  // Auto-setup if needed
            System.out.println("[WEBCAM DEBUG] FFmpeg path: " + ffmpeg);

            // Auto-detect camera if empty
            if (cameraName == null || cameraName.trim().isEmpty()) {
                System.out.println("[WEBCAM DEBUG] Camera name empty, auto-detecting...");
                cameraName = detectFirstCamera();

                if (cameraName == null) {
                    // Neu khong detect duoc thi fallback
                    cameraName = isWin ? "Integrated Camera" : (isMac ? "0" : "/dev/video0");
                    System.out.println("[WEBCAM DEBUG] ⚠ Auto-detect failed, using fallback: " + cameraName);
                } else {
                    System.out.println("[WEBCAM DEBUG] ✓ Auto-detected camera: " + cameraName);
                }
            }

            System.out.println("[WEBCAM DEBUG] 🎥 USING CAMERA: " + cameraName);
            System.out.println("[WEBCAM DEBUG] 📊 Quality: " + quality);

            // Determine resolution and compression based on quality
            String resolution, jpegQuality;
            switch (quality.toLowerCase()) {
                case "low":
                    resolution = "320x240";
                    jpegQuality = "5"; // Low quality, fast
                    System.out.println("[WEBCAM DEBUG] ⚡ FAST MODE: 320x240, Q=5");
                    break;
                case "high":
                    resolution = "1280x720";
                    jpegQuality = "2"; // High quality, slow
                    System.out.println("[WEBCAM DEBUG] 🎨 HIGH QUALITY: 1280x720, Q=2");
                    break;
                case "medium":
                default:
                    resolution = "640x480";
                    jpegQuality = "3"; // Balanced
                    System.out.println("[WEBCAM DEBUG] ⚖️ BALANCED: 640x480, Q=3");
                    break;
            }

            ProcessBuilder pb;
            String[] command;
            if (isWin) {
                // Windows: dung dshow with optimization
                command = new String[]{
                    ffmpeg,
                    "-f", "dshow",
                    "-video_size", resolution,
                    "-rtbufsize", "100M",  // Buffer size
                    "-i", "video=" + cameraName,
                    "-frames:v", "1",
                    "-q:v", jpegQuality,  // JPEG quality (1-31, lower = better)
                    "-pix_fmt", "yuvj420p",  // Fast pixel format
                    "-y", tmpFile
                };
                pb = new ProcessBuilder(command);
            } else if (isMac) {
                // macOS: dung avfoundation with optimization
                command = new String[]{
                    ffmpeg,
                    "-f", "avfoundation",
                    "-video_size", resolution,
                    "-framerate", "30",
                    "-i", cameraName,
                    "-frames:v", "1",
                    "-q:v", jpegQuality,
                    "-pix_fmt", "yuvj420p",
                    "-y", tmpFile
                };
                pb = new ProcessBuilder(command);
            } else {
                // Linux: dung v4l2 with optimization
                command = new String[]{
                    ffmpeg,
                    "-f", "v4l2",
                    "-video_size", resolution,
                    "-i", cameraName,
                    "-frames:v", "1",
                    "-q:v", jpegQuality,
                    "-pix_fmt", "yuvj420p",
                    "-y", tmpFile
                };
                pb = new ProcessBuilder(command);
            }

            System.out.println("[WEBCAM DEBUG] Command: " + String.join(" ", command));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Doc output de tranh bi block VA log ra
            System.out.println("[WEBCAM DEBUG] FFmpeg output:");
            consumeStreamWithLog(p.getInputStream());

            int exitCode = p.waitFor();
            System.out.println("[WEBCAM DEBUG] FFmpeg exit code: " + exitCode);

            if (exitCode != 0) {
                String errMsg = "Loi khi chup anh. Ma loi: " + exitCode;
                System.err.println("[WEBCAM DEBUG] ERROR: " + errMsg);
                sendError(out, errMsg);
                return;
            }

            File f = new File(tmpFile);
            System.out.println("[WEBCAM DEBUG] Checking output file...");
            System.out.println("[WEBCAM DEBUG] File exists: " + f.exists());
            if (f.exists()) {
                System.out.println("[WEBCAM DEBUG] File size: " + f.length() + " bytes");
            }

            if (f.exists() && f.length() > 0) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                System.out.println("[WEBCAM DEBUG] Sending " + bytes.length + " bytes to client");
                out.writeUTF("BINARY");
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();
                f.delete();
                System.out.println("[WEBCAM DEBUG] ========== WEBCAM CAPTURE SUCCESS ==========");
            } else {
                String errMsg = "Khong the chup webcam.\nCan cai ffmpeg: https://ffmpeg.org/download.html";
                System.err.println("[WEBCAM DEBUG] ERROR: " + errMsg);
                sendError(out, errMsg);
            }
        } catch (Exception e) {
            System.err.println("[WEBCAM DEBUG] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            sendError(out, "Loi webcam: " + e.getMessage());
        }
    }

    /**
     * BAT DAU RECORD VIDEO tu webcam
     * @param out Output stream to client
     * @param cameraName Ten camera (VD: "Samsung Slimfit Cam", "Integrated Camera")
     */
    public static void startRecording(DataOutputStream out, String cameraName) throws IOException {
        System.out.println("[WEBCAM DEBUG] ========== START RECORDING ==========");

        if (isRecording) {
            System.err.println("[WEBCAM DEBUG] Already recording!");
            sendError(out, "Dang record roi! Hay bam Stop truoc khi record lai.");
            return;
        }

        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        System.out.println("[WEBCAM DEBUG] OS: " + System.getProperty("os.name"));
        System.out.println("[WEBCAM DEBUG] Camera (from client): " + (cameraName == null || cameraName.trim().isEmpty() ? "(empty)" : cameraName));

        try {
            String ffmpeg = getFFmpeg();  // Auto-setup if needed
            System.out.println("[WEBCAM DEBUG] FFmpeg path: " + ffmpeg);

            // Auto-detect camera if empty
            if (cameraName == null || cameraName.trim().isEmpty()) {
                System.out.println("[WEBCAM DEBUG] Camera name empty, auto-detecting...");
                cameraName = detectFirstCamera();

                if (cameraName == null) {
                    // Neu khong detect duoc thi fallback
                    cameraName = isWin ? "Integrated Camera" : (isMac ? "0" : "/dev/video0");
                    System.out.println("[WEBCAM DEBUG] ⚠ Auto-detect failed, using fallback: " + cameraName);
                } else {
                    System.out.println("[WEBCAM DEBUG] ✓ Auto-detected camera: " + cameraName);
                }
            }

            System.out.println("[WEBCAM DEBUG] 🎥 USING CAMERA: " + cameraName);

            // Tao ten file video voi timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            currentVideoFile = System.getProperty("java.io.tmpdir") + File.separator
                             + "webcam_video_" + timestamp + ".mp4";
            System.out.println("[WEBCAM DEBUG] Output video: " + currentVideoFile);

            ProcessBuilder pb;
            String[] command;
            if (isWin) {
                // Windows: record from auto-detected or specified camera
                // List cameras: ffmpeg -list_devices true -f dshow -i dummy
                command = new String[] {
                    ffmpeg,
                    "-f", "dshow",
                    "-i", "video=" + cameraName,
                    "-framerate", "30",
                    "-video_size", "1280x720",
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-y",
                    currentVideoFile
                };
                pb = new ProcessBuilder(command);
            } else if (isMac) {
                // macOS: record from auto-detected or specified camera
                // List devices: ffmpeg -f avfoundation -list_devices true -i ""
                command = new String[] {
                    ffmpeg,
                    "-f", "avfoundation",
                    "-framerate", "30",
                    "-video_size", "1280x720",
                    "-i", cameraName,  // Use detected camera index
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-y",
                    currentVideoFile
                };
                pb = new ProcessBuilder(command);
            } else {
                // Linux: record from auto-detected or specified camera
                command = new String[] {
                    ffmpeg,
                    "-f", "v4l2",
                    "-framerate", "30",
                    "-video_size", "1280x720",
                    "-i", cameraName,  // Use detected device path
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-y",
                    currentVideoFile
                };
                pb = new ProcessBuilder(command);
            }

            System.out.println("[WEBCAM DEBUG] Command: " + String.join(" ", command));

            pb.redirectErrorStream(true);
            System.out.println("[WEBCAM DEBUG] Starting FFmpeg process...");
            recordingProcess = pb.start();
            isRecording = true;

            // Doc output trong thread rieng de tranh block
            new Thread(() -> {
                try {
                    System.out.println("[WEBCAM DEBUG] FFmpeg output (recording):");
                    consumeStreamWithLog(recordingProcess.getInputStream());
                } catch (Exception e) {
                    System.err.println("[WEBCAM DEBUG] Error reading FFmpeg output: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

            // Cho 1 chut de ffmpeg khoi dong
            Thread.sleep(500);

            // Check neu process die ngay
            if (!recordingProcess.isAlive()) {
                isRecording = false;
                int exitCode = recordingProcess.exitValue();
                System.err.println("[WEBCAM DEBUG] Process died immediately! Exit code: " + exitCode);
                sendError(out, "Khong the bat dau record (exit code: " + exitCode + ").\n" +
                    "Kiem tra:\n" +
                    "1. ffmpeg da duoc cai chua?\n" +
                    "2. Webcam co hoat dong khong?\n" +
                    "3. Co app nao khac dang dung camera khong?");
                return;
            }

            System.out.println("[WEBCAM DEBUG] Process started, PID: " + recordingProcess.pid());
            out.writeUTF(JsonUtil.textResponse("OK", "Dang record video... Bam Stop de ket thuc."));
            out.flush();
            System.out.println("[WEBCAM DEBUG] ========== RECORDING STARTED ==========");

        } catch (Exception e) {
            isRecording = false;
            recordingProcess = null;
            System.err.println("[WEBCAM DEBUG] EXCEPTION in startRecording: " + e.getMessage());
            e.printStackTrace();
            sendError(out, "Loi khi bat dau record: " + e.getMessage());
        }
    }

    /**
     * DUNG RECORD va GUI VIDEO ve client
     */
    public static void stopRecording(DataOutputStream out) throws IOException {
        System.out.println("[WEBCAM DEBUG] ========== STOP RECORDING ==========");

        if (!isRecording || recordingProcess == null) {
            System.err.println("[WEBCAM DEBUG] No recording in progress!");
            sendError(out, "Khong co video nao dang record!");
            return;
        }

        System.out.println("[WEBCAM DEBUG] Stopping FFmpeg process...");
        try {
            // Gui signal 'q' de ffmpeg ket thuc gracefully
            // (ffmpeg nhan 'q' qua stdin de stop)
            if (recordingProcess.isAlive()) {
                System.out.println("[WEBCAM DEBUG] Sending 'q' signal to FFmpeg...");
                OutputStream stdin = recordingProcess.getOutputStream();
                stdin.write('q');
                stdin.flush();
                stdin.close();

                // Cho ffmpeg finalize file (toi da 10s)
                System.out.println("[WEBCAM DEBUG] Waiting for FFmpeg to finish (max 10s)...");
                boolean finished = recordingProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    // Neu chua done thi force kill
                    System.err.println("[WEBCAM DEBUG] FFmpeg didn't finish in time, force killing...");
                    recordingProcess.destroyForcibly();
                } else {
                    int exitCode = recordingProcess.exitValue();
                    System.out.println("[WEBCAM DEBUG] FFmpeg exit code: " + exitCode);
                }
            }

            isRecording = false;
            recordingProcess = null;

            // Cho them 500ms de file duoc flush
            System.out.println("[WEBCAM DEBUG] Waiting for file flush...");
            Thread.sleep(500);

            // Kiem tra file video
            File videoFile = new File(currentVideoFile);
            System.out.println("[WEBCAM DEBUG] Checking video file: " + currentVideoFile);
            System.out.println("[WEBCAM DEBUG] File exists: " + videoFile.exists());
            if (videoFile.exists()) {
                System.out.println("[WEBCAM DEBUG] File size: " + (videoFile.length() / 1024 / 1024) + " MB");
            }

            if (!videoFile.exists() || videoFile.length() == 0) {
                System.err.println("[WEBCAM DEBUG] ERROR: Video file empty or missing!");
                sendError(out, "Loi: File video khong ton tai hoac rong.");
                return;
            }

            // Doc va gui file video
            System.out.println("[WEBCAM DEBUG] Reading video file...");
            byte[] videoBytes = Files.readAllBytes(videoFile.toPath());
            System.out.println("[WEBCAM DEBUG] Sending " + (videoBytes.length / 1024 / 1024) + " MB to client...");
            out.writeUTF("BINARY");
            out.writeInt(videoBytes.length);
            out.write(videoBytes);
            out.flush();

            // Xoa file tam
            videoFile.delete();
            System.out.println("[WEBCAM DEBUG] ========== VIDEO SENT SUCCESSFULLY ==========");

        } catch (Exception e) {
            isRecording = false;
            recordingProcess = null;
            System.err.println("[WEBCAM DEBUG] EXCEPTION in stopRecording: " + e.getMessage());
            e.printStackTrace();
            sendError(out, "Loi khi dung record: " + e.getMessage());
        }
    }

    /**
     * Helper: Doc output stream de tranh process bi block
     */
    private static void consumeStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            // Log neu muon debug
            // System.out.println("[ffmpeg] " + line);
        }
    }

    /**
     * Helper: Doc output stream VA log ra (for debugging)
     */
    private static void consumeStreamWithLog(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[ffmpeg] " + line);
        }
    }

    /**
     * Helper: Gui error message
     */
    private static void sendError(DataOutputStream out, String msg) throws IOException {
        out.writeUTF(JsonUtil.textResponse("ERROR", msg));
        out.flush();
    }

    /**
     * Cleanup khi dong server
     */
    public static void cleanup() {
        if (recordingProcess != null && recordingProcess.isAlive()) {
            recordingProcess.destroyForcibly();
        }
    }
}
