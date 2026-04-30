# RemotePC - Ứng Dụng Điều Khiển Máy Tính Từ Xa

## 📋 Giới Thiệu

**RemotePC** là một ứng dụng Java cho phép điều khiển máy tính từ xa thông qua mạng LAN hoặc Internet. Ứng dụng hỗ trợ các tính năng như chụp màn hình, điều khiển chuột/bàn phím, quản lý ứng dụng, quản lý file, ghi hình webcam, và nhiều hơn thế nữa.

### 🏗️ Kiến Trúc

- **Server**: Chạy trên máy muốn điều khiển (bị điều khiển)
- **Client**: Chạy trên máy điều khiển (người dùng)
- **Protocol**: TCP Socket trên cổng 9999 (mặc định)

---

## 🚀 Yêu Cầu Hệ Thống

### Bắt Buộc
- **Java Development Kit (JDK)**: Phiên bản 14 hoặc cao hơn
  - Tải từ: https://www.oracle.com/java/technologies/javase-downloads.html
  - Hoặc OpenJDK: https://openjdk.java.net/

- **Windows**: Ứng dụng được tối ưu cho Windows (một số tính năng chỉ hỗ trợ Windows)

### Tùy Chọn
- **FFmpeg**: Cần thiết để ghi hình webcam
  - Tải từ: https://ffmpeg.org/download.html
  - Hoặc để ứng dụng tự động tải xuống lần đầu tiên

---

## 📦 Cài Đặt và Biên Dịch

### 1. Chuẩn Bị FFmpeg (Tùy Chọn)

Nếu muốn sử dụng tính năng ghi hình webcam:

1. Tải FFmpeg từ: https://ffmpeg.org/download.html (phiên bản full builds)
2. Giải nén file `ffmpeg.exe`
3. Đặt `ffmpeg.exe` vào thư mục: `resources/ffmpeg/`
4. Nếu bỏ qua bước này, ứng dụng sẽ tự động tải FFmpeg từ internet lần đầu tiên chạy

### 2. Biên Dịch Server

Mở Command Prompt/PowerShell trong thư mục gốc dự án và chạy:

```bash
build_server.bat
```

**Kết quả**:
- Tệp JAR: `build/server/server.jar`
- Ứng dụng đóng gói: `dist/server/RemotePC-Server/`

### 3. Biên Dịch Client

```bash
build_client.bat
```

**Kết quả**:
- Tệp JAR: `build/client/client.jar`
- Ứng dụng đóng gói: `dist/client/RemotePC-Client/`

### Khắc Phục Sự Cố Biên Dịch

**Lỗi: "javac is not recognized"**
- Java chưa được cài đặt hoặc chưa thêm vào PATH
- Giải pháp: Cài Java JDK và thêm đường dẫn bin vào biến môi trường PATH

**Lỗi: "jpackage is not recognized"**
- JDK version < 14
- Giải pháp: Cập nhật lên JDK 14 hoặc cao hơn

---

## ▶️ Chạy Ứng Dụng

### Phương Pháp 1: Chạy từ JAR (Đơn Giản)

**Server**:
```bash
java -jar build/server/server.jar
```

**Client**:
```bash
java -jar build/client/client.jar
```

### Phương Pháp 2: Chạy từ Ứng Dụng Đóng Gói (Khuyến Nghị)

**Server**:
- Mở thư mục: `dist/server/RemotePC-Server/bin/`
- Chạy file: `RemotePC-Server.bat` hoặc `RemotePC-Server.exe`

**Client**:
- Mở thư mục: `dist/client/RemotePC-Client/bin/`
- Chạy file: `RemotePC-Client.bat` hoặc `RemotePC-Client.exe`

### Phương Pháp 3: Chạy trực tiếp từ Java

**Server**:
```bash
java -cp build/server Server
```

**Client**:
```bash
java -cp build/client ClientGUI
```

---

## 🎯 Hướng Dẫn Sử Dụng

### 1️⃣ Khởi Động Server

1. Chạy Server trên máy tính muốn điều khiển
2. Giao diện Server sẽ hiển thị:
   - **IP Address**: Địa chỉ IP của máy
   - **Port**: Cổng lắng nghe (mặc định 9999)
   - **Status**: Trạng thái server

3. Click nút **"Khởi động"** để bắt đầu lắng nghe kết nối

4. Tùy chọn:
   - Đóng cửa sổ để Server chạy ở chế độ ẩn (System Tray)
   - Nhấp chuột phải vào icon tray để hiển thị menu

### 2️⃣ Kết Nối từ Client

1. Chạy Client trên máy muốn điều khiển máy khác
2. Tab **"Kết Nối"**:
   - Nhập **IP Address** của Server
   - Nhập **Port** (mặc định: 9999)
   - Click **"Kết Nối"**

3. Khi kết nối thành công:
   - Status sẽ hiển thị "✅ Đã kết nối"
   - Tất cả các tab chức năng sẽ được bật

### 3️⃣ Các Tính Năng Chi Tiết

#### 📱 Tab "Ứng Dụng" - Quản Lý Ứng Dụng
- **Liệt kê ứng dụng**: Xem danh sách các ứng dụng đang chạy trên máy server
- **Chạy ứng dụng**: Nhập tên/đường dẫn ứng dụng rồi click "Khởi Chạy"
  - Ví dụ: `notepad`, `calc.exe`, `C:\Program Files\Google\Chrome\chrome.exe`
- **Dừng ứng dụng**: Nhập tên ứng dụng rồi click "Dừng"

#### ⚙️ Tab "Tiến Trình" - Quản Lý Tiến Trình
- **Liệt kê tiến trình**: Xem danh sách các tiến trình đang chạy
- **Tìm tiến trình**: Nhập tên tiến trình (không cần .exe)
- **Kết Thúc tiến trình**: Nhập PID (Process ID) rồi click "Kết Thúc"

#### 🖥️ Tab "Màn Hình" - Chụp & Xem Màn Hình
- **Chụp ảnh màn hình**: Click "Chụp Ảnh" để lấy ảnh hiện tại của server
- **Xem ảnh**: Ảnh sẽ hiển thị trong tab
- **Lưu ảnh**: Click "Lưu Ảnh" để lưu về máy client

#### 🎹 Tab "Bàn Phím" - Theo Dõi Bàn Phím
- **Khởi động ghi bàn phím**: Click "Khởi Động" để bắt đầu ghi lại các phím được nhấn trên server
- **Dừng ghi**: Click "Dừng"
- **Xem nhật ký**: Nhật ký sẽ hiển thị các phím được nhấn

**Lưu ý bảo mật**: Chỉ sử dụng trên các máy tính đáng tin cậy!

#### 📂 Tab "File" - Quản Lý File
- **Duyệt thư mục**: Nhập đường dẫn thư mục (ví dụ: `C:\Users`)
- **Liệt kê file**: Click "Liệt Kê" để xem file trong thư mục
- **Tải file từ server**: Nhập tên file rồi click "Tải Về"
- **Gửi file đến server**: Chọn file từ máy local và gửi

#### 🔌 Tab "Nguồn Điện" - Quản Lý Điện
- **Tắt máy server**: Click "Tắt Máy" (sau 30 giây)
- **Khởi động lại**: Click "Khởi Động Lại"
- **Chế độ Sleep**: Click "Ngủ"
- **Hủy lệnh**: Click "Hủy Lệnh" nếu đã vô tình gửi lệnh

#### 📷 Tab "Webcam" - Ghi Hình Webcam
- **Chụp ảnh webcam**: Click "Chụp Ảnh" để lấy ảnh hiện tại
- **Bắt đầu ghi video**: 
  1. Chọn chất lượng video (480p, 720p, 1080p)
  2. Nhập tên camera (hoặc để mặc định)
  3. Click "Bắt Đầu Ghi"
- **Dừng ghi & tải video**: Click "Dừng & Tải Video"
- **Xem alivestream webcam**: Click "Bắt Đầu Stream" để xem video từ webcam real-time

**Yêu cầu**: FFmpeg phải được cài đặt hoặc sẽ được tải xuống tự động

#### 🎮 Tab "Điều Khiển Xa" - Remote Desktop
- **Bắt đầu điều khiển**: Click "Bắt Đầu" để xem và điều khiển màn hình server
- **Di chuyển chuột**: Di chuyển chuột trên panel để điều khiển chuột máy server
- **Nhấp chuột**: 
  - Nút trái: Click bình thường
  - Nút phải: Chuột phải
  - Nút giữa: Scroll
- **Nhấn phím**: Các phím sẽ được gửi tới server
- **Dừng điều khiển**: Click "Dừng"

---

## 🔧 Cấu Hình Nâng Cao

### Thay Đổi Cổng Mặc Định

Mở file `Server.java`, tìm dòng:
```java
private static final int DEFAULT_PORT = 9999;
```
Thay đổi giá trị và biên dịch lại.

### Tăng Số Lượng Client Tối Đa

Mở file `Server.java`, tìm dòng:
```java
private static final int MAX_CLIENTS = 10;
```
Thay đổi giá trị tùy ý.

### Kích Thước Màn Hình Remote Desktop

Trong `ClientGUI.java`, tìm:
```java
private int remoteScreenW = 1920, remoteScreenH = 1080;
```
Điều chỉnh để phù hợp với độ phân giải của server.

---

## ⚠️ Lưu Ý Bảo Mật

1. **Chỉ sử dụng trên mạng tin cậy**: Không có mã hóa trong phiên bản hiện tại
2. **Xác thực**: Hiện tại không có cơ chế xác thực, bất kỳ ai có IP và port đều có thể kết nối
3. **Ghi bàn phím**: Có thể ghi lại các phím được nhấn, bao gồm mật khẩu - chỉ sử dụng trên máy đáng tin cậy
4. **Webcam**: Yêu cầu sự đồng ý từ người dùng trước khi truy cập

**Khuyến nghị cho sử dụng sản xuất**:
- Thêm xác thực người dùng (username/password)
- Mã hóa dữ liệu truyền tải (SSL/TLS)
- Tường lửa để chỉ cho phép kết nối từ IP cụ thể
- Ghi log chi tiết các hoạt động

---

## 🆘 Khắc Phục Sự Cố

### "Kết nối bị từ chối"
- Kiểm tra Server có đang chạy không
- Kiểm tra IP và Port có đúng không
- Kiểm tra tường lửa có chặn cổng 9999 không
- Chạy lệnh: `netstat -an | findstr 9999` để kiểm tra cổng đang mở

### "Không thể chụp màn hình"
- Kiểm tra quyền admin trên Server
- Chạy Server ở chế độ Administrator

### "Ghi webcam không hoạt động"
- Kiểm tra FFmpeg đã được cài đặt
- Nếu FFmpeg không có sẵn, hãy cho Server kết nối internet để tải tự động
- Kiểm tra camera có được kết nối không

### "Remote Desktop bị lag"
- Giảm chất lượng (resolution) của remote desktop
- Tăng khoảng thời gian làm mới (FPS)
- Kiểm tra bandwidth mạng

---

## 📁 Cấu Trúc Thư Mục

```
nhap_mon_lap_trinh_v1/
├── client/                 # Mã nguồn Client
│   ├── ClientGUI.java
│   ├── ClientConnection.java
│   ├── IconGenerator.java
│   └── JsonUtil.java
├── server/                 # Mã nguồn Server
│   ├── Server.java
│   ├── ClientHandler.java
│   ├── AppManager.java
│   ├── ProcessManager.java
│   ├── ScreenCapture.java
│   ├── WebcamCapture.java
│   ├── RemoteDesktop.java
│   ├── FileTransfer.java
│   ├── PowerManager.java
│   ├── KeyboardMonitor.java
│   ├── NetworkDiag.java
│   ├── FFmpegManager.java
│   ├── FreezeScreen.java
│   └── JsonUtil.java
├── resources/              # Tài nguyên
│   └── ffmpeg/            # FFmpeg executable (tùy chọn)
├── build_client.bat        # Script biên dịch Client
├── build_server.bat        # Script biên dịch Server
└── README.md              # File này
```

---

## 🛠️ Phát Triển & Mở Rộng

### Thêm Tính Năng Mới

1. Tạo class Java mới trong thư mục `server/` hoặc `client/`
2. Thực hiện logic tính năng
3. Tích hợp vào `ClientHandler.java` (server-side) hoặc `ClientGUI.java` (client-side)
4. Định nghĩa protocol giao tiếp (JSON)
5. Biên dịch và kiểm tra

### Cải Thiện Giao Diện

- Sử dụng `swing-tips.java` để cải thiện UI
- Thêm icon bằng `IconGenerator.java`
- Tối ưu hóa bố cục với `GridBagLayout` hoặc `BorderLayout`

---

## 📝 Các Tính Năng Chính

| Tính Năng | Server | Client | Mô Tả |
|-----------|--------|--------|-------|
| Quản lý ứng dụng | ✅ | ✅ | Liệt kê, khởi chạy, dừng ứng dụng |
| Quản lý tiến trình | ✅ | ✅ | Xem danh sách, kết thúc tiến trình |
| Chụp màn hình | ✅ | ✅ | Capture và xem màn hình |
| Ghi bàn phím | ✅ | ✅ | Log các phím nhấn |
| Quản lý file | ✅ | ✅ | Upload/Download file |
| Quản lý điện | ✅ | ✅ | Tắt, khởi động lại, ngủ |
| Ghi webcam | ✅ | ✅ | Chụp & ghi video từ webcam |
| Remote Desktop | ✅ | ✅ | Điều khiển chuột/bàn phím |
| System Tray | ✅ | ❌ | Chạy ẩn trong system tray |
| Network Diagnostics | ✅ | ❌ | Kiểm tra kết nối mạng |

---

## 📞 Liên Hệ & Hỗ Trợ

Nếu gặp bất kỳ vấn đề nào, vui lòng:
1. Kiểm tra phần "Khắc Phục Sự Cố" phía trên
2. Kiểm tra log trong ứng dụng
3. Đảm bảo Java và FFmpeg được cài đặt đúng

---

## 📄 Giấy Phép

Ứng dụng này được tạo cho mục đích học tập. Vui lòng sử dụng một cách có trách nhiệm.

---

**Phiên Bản**: 1.0  
**Cập Nhật Lần Cuối**: Tháng 4, 2026

Enjoy! 🎉
