import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Function 7: Webcam Capture & Video Recording (Windows Only)
 *
 * Su dung ffmpeg de chup anh va record video tu webcam.
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
     * LIST tat ca camera co san tren he thong (Windows)
     * PRIMARY METHOD: PowerShell (nhanh va chinh xac)
     * BACKUP METHOD: FFmpeg DirectShow (neu PowerShell fail)
     * @param out Output stream to client
     */
    public static void listCameras(DataOutputStream out) throws IOException {
        System.out.println("[CAMERA LIST] ========== LISTING CAMERAS ==========");

        try {
            // PRIMARY: Thu PowerShell truoc (nhanh va chinh xac hon)
            System.out.println("[CAMERA LIST] Using PowerShell as primary method...");
            String psResult = listCamerasViaPowerShell();

            if (psResult != null && !psResult.isEmpty()) {
                System.out.println("[CAMERA LIST] ✓ PowerShell found cameras!");
                out.writeUTF(JsonUtil.textResponse("OK",
                    "=== DANH SACH CAMERA ===\n\n" + psResult +
                    "\n\n💡 Chon camera va bam nut '▼' de dien vao"));
                out.flush();
                return; // SUCCESS
            }

            // BACKUP: Neu PowerShell fail, thu FFmpeg DirectShow
            System.out.println("[CAMERA LIST] PowerShell failed, trying FFmpeg as backup...");
            String ffmpeg = getFFmpeg();
            System.out.println("[CAMERA LIST] FFmpeg path: " + ffmpeg);

            ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-list_devices", "true", "-f", "dshow", "-i", "dummy");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            boolean foundVideoSection = false;
            StringBuilder cameraList = new StringBuilder();
            StringBuilder fullOutput = new StringBuilder();
            int count = 0;

            System.out.println("[CAMERA LIST] FFmpeg output:");
            while ((line = reader.readLine()) != null) {
                System.out.println("[CAMERA LIST] " + line);
                fullOutput.append(line).append("\n");

                if (line.contains("DirectShow video devices") || line.contains("video devices")) {
                    foundVideoSection = true;
                    System.out.println("[CAMERA LIST] >>> Found video devices section!");
                }
                if (foundVideoSection && line.contains("\"")) {
                    int start = line.indexOf("\"");
                    int end = line.indexOf("\"", start + 1);
                    if (start != -1 && end != -1) {
                        String cameraName = line.substring(start + 1, end);
                        if (!line.contains("Alternative name") && !cameraName.startsWith("@")) {
                            count++;
                            cameraList.append(count).append(". ").append(cameraName).append("\n");
                            System.out.println("[CAMERA LIST] >>> FOUND CAMERA #" + count + ": " + cameraName);
                        }
                    }
                }
                if (foundVideoSection && (line.contains("DirectShow audio devices") || line.contains("audio devices"))) {
                    System.out.println("[CAMERA LIST] >>> Reached audio devices section, stopping");
                    break;
                }
            }

            p.waitFor();
            System.out.println("[CAMERA LIST] FFmpeg exit code: " + p.exitValue());

            if (count == 0) {
                System.out.println("[CAMERA LIST] ✗ FFmpeg also failed");
                System.out.println("[CAMERA LIST] Full FFmpeg output:");
                System.out.println(fullOutput.toString());

                sendError(out,
                    "⚠️  KHONG TIM THAY CAMERA\n\n" +
                    "PowerShell va FFmpeg deu khong detect duoc camera.\n\n" +
                    "Nguyen nhan co the:\n" +
                    "1. Camera chua duoc cam vao\n" +
                    "2. Driver camera chua cai dat dung\n" +
                    "3. Camera dang duoc app khac su dung (Zoom/Teams)\n" +
                    "4. Can chay server voi quyen Administrator\n\n" +
                    "Thu lam:\n" +
                    "• Mo 'Camera' app trong Windows de test camera\n" +
                    "• Cap nhat driver trong Device Manager\n" +
                    "• Dong Zoom/Teams/Skype\n" +
                    "• Chay server voi 'Run as Administrator'\n\n" +
                    "WORKAROUND: De TRONG o camera va bam 'Chup webcam'\n" +
                    "(Server se thu auto-detect)");
            } else {
                System.out.println("[CAMERA LIST] ✓ FFmpeg found " + count + " camera(s)");
                out.writeUTF(JsonUtil.textResponse("OK",
                    "=== DANH SACH CAMERA (FFmpeg) ===\n\n" + cameraList.toString() +
                    "\n━━━━━━━━━━━━━━━━━━━━━━━━\nTong so: " + count + " camera\n\n" +
                    "💡 Chon camera va bam nut '▼' de dien vao"));
                out.flush();
            }

        } catch (Exception e) {
            System.err.println("[CAMERA LIST] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            sendError(out, "Loi khi quet camera: " + e.getMessage() +
                "\n\nWORKAROUND: De TRONG o camera va thu chup truc tiep.");
        }
    }

    /**
     * BACKUP METHOD: List camera bang PowerShell (khi FFmpeg khong hoat dong)
     * @return Danh sach camera, hoac null neu khong tim thay
     */
    private static String listCamerasViaPowerShell() {
        try {
            System.out.println("[CAMERA PS] Trying PowerShell method...");

            // Thu nhieu cach khac nhau de list camera
            String[] psCommands = {
                // Method 1: WMI - PnpDevice (Windows 10+)
                "Get-PnpDevice -Class Camera,Image | Where-Object {$_.Status -eq 'OK'} | Select-Object -ExpandProperty FriendlyName",

                // Method 2: WMI - Win32_PnPEntity
                "Get-WmiObject Win32_PnPEntity | Where-Object {$_.PNPClass -eq 'Camera' -or $_.PNPClass -eq 'Image'} | Select-Object -ExpandProperty Name",

                // Method 3: Simple PnpDevice
                "Get-PnpDevice -Class Camera | Select-Object -ExpandProperty FriendlyName"
            };

            for (int i = 0; i < psCommands.length; i++) {
                System.out.println("[CAMERA PS] Trying method " + (i+1) + "/" + psCommands.length + "...");
                System.out.println("[CAMERA PS] Command: " + psCommands[i]);

                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommands[i]);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                int count = 0;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    System.out.println("[CAMERA PS] Output: " + line);
                    if (!line.isEmpty() &&
                        !line.equals("FriendlyName") &&
                        !line.equals("Name") &&
                        !line.startsWith("Get-") &&
                        !line.contains("----")) {
                        count++;
                        result.append(count).append(". ").append(line).append("\n");
                    }
                }

                p.waitFor();
                System.out.println("[CAMERA PS] Method " + (i+1) + " found " + count + " camera(s)");

                if (count > 0) {
                    return result.toString() + "\n━━━━━━━━━━━━━━━━━━━━━━━━\nTong so: " + count + " camera\n" +
                           "(Detect bang PowerShell Method " + (i+1) + ")";
                }
            }

            System.out.println("[CAMERA PS] All methods failed");
            return null;

        } catch (Exception e) {
            System.err.println("[CAMERA PS] ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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

        try {
            String ffmpeg = getFFmpeg();
            // Windows: list devices
            System.out.println("[CAMERA DETECT] Scanning Windows cameras...");
            ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-list_devices", "true", "-f", "dshow", "-i", "dummy");

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Doc output va tim camera dau tien
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            boolean foundVideoSection = false;

            while ((line = reader.readLine()) != null) {
                System.out.println("[CAMERA DETECT] " + line);

                // Windows: Tim dong chua ten camera trong dau ""
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
        long t0 = System.currentTimeMillis();
        long t1, t2, t3, t4, t5;

        try {
            String ffmpeg = getFFmpeg();
            t1 = System.currentTimeMillis();

            // FAST: Use cached camera name if available
            if (cameraName == null || cameraName.trim().isEmpty()) {
                if (detectedCamera != null) {
                    cameraName = detectedCamera; // Use cached!
                } else {
                    long detectStart = System.currentTimeMillis();
                    cameraName = detectFirstCamera();
                    long detectEnd = System.currentTimeMillis();
                    System.out.println("[WEBCAM TIMING] Camera detection: " + (detectEnd - detectStart) + "ms");
                    if (cameraName == null) {
                        cameraName = "Integrated Camera";
                    }
                }
            }
            t2 = System.currentTimeMillis();

            // SIMPLE & FAST: No scale filter, direct capture
            String resolution, jpegQuality;
            switch (quality.toLowerCase()) {
                case "low":
                    resolution = "320x240";
                    jpegQuality = "10";
                    break;
                case "high":
                    resolution = "640x480";
                    jpegQuality = "5";
                    break;
                case "medium":
                default:
                    resolution = "480x360";
                    jpegQuality = "7";
                    break;
            }

            // SIMPLE FFmpeg command - NO scale filter (causes delay!)
            ProcessBuilder pb = new ProcessBuilder(
                ffmpeg,
                "-f", "dshow",
                "-video_size", resolution,
                "-rtbufsize", "1M",
                "-probesize", "32",
                "-analyzeduration", "0",
                "-fflags", "nobuffer",
                "-i", "video=" + cameraName,
                "-frames:v", "1",
                "-q:v", jpegQuality,
                "-pix_fmt", "yuvj420p",
                "-y", tmpFile
            );
            pb.redirectErrorStream(true);

            System.out.println("[WEBCAM TIMING] Starting FFmpeg...");
            Process p = pb.start();
            t3 = System.currentTimeMillis();

            // Consume output (must do this!)
            new Thread(() -> {
                try { consumeStream(p.getInputStream()); }
                catch (Exception ignored) {}
            }).start();

            p.waitFor();
            t4 = System.currentTimeMillis();
            System.out.println("[WEBCAM TIMING] FFmpeg execution: " + (t4 - t3) + "ms");

            File f = new File(tmpFile);
            if (f.exists() && f.length() > 0) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                t5 = System.currentTimeMillis();

                // SIMPLE: Write all at once, single flush
                out.writeUTF("BINARY");
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();

                f.delete();

                long t6 = System.currentTimeMillis();

                System.out.println("[WEBCAM TIMING] Breakdown:");
                System.out.println("  Setup: " + (t1 - t0) + "ms");
                System.out.println("  Camera: " + (t2 - t1) + "ms");
                System.out.println("  FFmpeg start: " + (t3 - t2) + "ms");
                System.out.println("  FFmpeg wait: " + (t4 - t3) + "ms <<<< CRITICAL");
                System.out.println("  Read file: " + (t5 - t4) + "ms");
                System.out.println("  Network send: " + (t6 - t5) + "ms");
                System.out.println("  TOTAL: " + (t6 - t0) + "ms");
            } else {
                sendError(out, "Khong the chup webcam");
            }
        } catch (Exception e) {
            System.err.println("[WEBCAM] ERROR: " + e.getMessage());
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

        System.out.println("[WEBCAM DEBUG] OS: Windows");
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
                    cameraName = "Integrated Camera";
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

            // Windows: record from auto-detected or specified camera
            // OPTIMIZED for Windows Media Player compatibility
            String[] command = new String[] {
                ffmpeg,
                "-f", "dshow",
                "-i", "video=" + cameraName,
                "-framerate", "30",
                "-video_size", "1280x720",

                // Video codec settings (Windows Media Player compatible)
                "-c:v", "libx264",              // H.264 codec
                "-preset", "ultrafast",          // Fast encoding
                "-profile:v", "baseline",        // Baseline profile (most compatible)
                "-level", "3.0",                 // H.264 level 3.0
                "-pix_fmt", "yuv420p",          // Pixel format (required for compatibility)

                // Container settings
                "-movflags", "+faststart",       // Enable streaming/quick playback
                "-f", "mp4",                     // Force MP4 format

                "-y",
                currentVideoFile
            };
            ProcessBuilder pb = new ProcessBuilder(command);

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
