# RemotePC - Complete Package v1.1

<div align="center">

**Client-Server Remote Control Application**

📅 Package Date: April 7, 2026  
📦 Package Size: ~76 KB  
🐛 Bug Fixes: App Manager LIST_APPS fixed!

</div>

---

## 📋 What's Included

| Component | Description |
|-----------|-------------|
| 📂 `client/` | Client GUI source code (4 files) |
| 📂 `server/` | Server source code (14 files) |
| 📂 `icons/` | Application icons (12 files) |
| 📂 `resources/ffmpeg/` | FFmpeg bundle folder (optional) |
| 🔨 `build_client.bat` | Build client (Windows only) |
| 🔨 `build_server.bat` | Build server (Windows only) |

---

## ✨ What's New in v1.1

<div align="right">
<i>Released: April 7, 2026</i>
</div>

### 🐛 Bug Fixes
- ✅ Fixed App Manager LIST_APPS not working on Windows
  - Fixed encoding issue (UTF-8 → CP850)
  - Changed from CSV to TABLE format parsing
  - Removed duplicate entries
  - Added better error messages

### 📈 Improvements
- Better debug logging in server console
- Auto .exe extension for START_APP
- Verified process termination in STOP_APP
- Cleaner output formatting

---

## 🚀 Quick Start

```bash
# 1. Extract package
unzip RemotePC-Complete-Package.zip

# 2. Build client
build_client.bat

# 3. Build server
build_server.bat

# 4. Run
dist\server\RemotePC-Server\RemotePC-Server.exe
dist\client\RemotePC-Client\RemotePC-Client.exe
```

<div align="right">

**Output:**
- Client: `dist/client/RemotePC-Client/RemotePC-Client.exe`
- Server: `dist/server/RemotePC-Server/RemotePC-Server.exe`

</div>

---

## 📋 Requirements

<table>
<tr>
<td width="50%">

**For Building:**
- ✓ Java JDK 14+
- ✓ [Download JDK](https://adoptium.net/)

</td>
<td width="50%" align="right">

**For Running:**
- ✓ Windows 7/10/11
- ✓ No Java needed
- ✓ FFmpeg bundled

</td>
</tr>
</table>

---

## 🎯 Features

<div align="center">

| Feature | Status |
|---------|:------:|
| Remote Desktop | ✅ |
| Webcam Streaming | ✅ |
| File Transfer | ✅ |
| App Management | ✅ FIXED! |
| Process Management | ✅ |
| Keyboard Monitoring | ✅ |
| Network Diagnostics | ✅ |
| Power Control | ✅ |

</div>

---

## 🧪 Testing the Fix

<div align="right">

**Expected Result:**

```
=== RUNNING APPLICATIONS ===

notepad.exe          PID: 1234    Mem: 2,048 K
chrome.exe           PID: 5678    Mem: 512,000 K
explorer.exe         PID: 9012    Mem: 45,000 K

─────────────────────────────────
Total: XX applications running
```

✅ **If you see this → Fix works!**

</div>

---

## 📂 Project Structure

```
RemotePC/
├── client/          (source code)
├── server/          (source code - AppManager.java FIXED! ✅)
├── icons/           (app icons)
├── build/           (compiled files)
└── dist/
    ├── client/      (~ 120MB with JRE)
    └── server/      (~ 120MB with JRE)
```

<div align="right">
<i>Note: dist/ folders contain EVERYTHING needed to run!</i>
</div>

---

## ⚠️ Important Notes

<table>
<tr>
<td>

**Security:**
- ⚠️ For educational/authorized use only
- ⚠️ Get permission before deployment
- ⚠️ May trigger antivirus alerts

</td>
<td align="right">

**Deployment:**
- Copy entire folder
- Don't copy just .exe
- Needs app/ and runtime/

</td>
</tr>
</table>

---

## 🎓 For School Project

### Presentation Tips

<div align="right">

1. Build both client and server first
2. Test all features before demo
3. Prepare 2 computers (or VM)
4. Explain the App Manager fix
5. Demo impressive features

</div>

### Technical Highlights

| Aspect | Details |
|--------|---------|
| **Architecture** | Client-Server |
| **Protocol** | JSON over TCP |
| **Threading** | Multi-threaded |
| **Language** | Java 14+ |
| **Packaging** | Embedded JRE |

<div align="right">

**Bug Fix Story:**
- Problem: List apps not working
- Cause: Wrong encoding + CSV parsing
- Solution: CP850 encoding + TABLE format
- Result: Feature fully restored ✅

</div>

---

## 📊 Technical Specs

<div align="right">

| Metric | Value |
|--------|------:|
| Total Java files | 18 |
| Total lines of code | ~10,000 |
| Client package size | ~120 MB |
| Server package size | ~120 MB |
| Source package size | ~76 KB |

</div>

---

## 🆘 Common Issues

> **Issue:** Apps list empty  
> **Solution:** Normal! Only shows user apps. Try starting notepad.exe first.

<div align="right">

> **Issue:** Can't connect from client  
> **Solution:** Check firewall, verify server IP, ensure server is running.

</div>

> **Issue:** Webcam not working  
> **Solution:** Wait 30-60s on first use. FFmpeg downloads automatically.

---

## 📞 Support & Resources

<div align="right">

**Java JDK Download:** [adoptium.net](https://adoptium.net/)

**Build Issues:** Check Java version with `java -version`  
**Testing:** Use 2 computers or 1 computer + 1 VM

</div>

---

## 📝 Version History

<div align="right">

**v1.1** (April 7, 2026) - *CURRENT*
- Fixed App Manager bug
- Improved encoding handling
- Better parsing and logging

**v1.0** (April 1, 2026) - *Initial Release*
- All major features implemented
- Client and server GUI
- 8 main features

</div>

---

## 🌟 Project Summary

<div align="center">

RemotePC is a **full-featured remote control application** built with Java,  
featuring client-server architecture, multiple remote control capabilities,  
and embedded Java runtime for easy deployment.

### Key Achievements
✓ Complete remote control solution  
✓ 8 major features implemented  
✓ Professional GUI  
✓ Bug fixes applied  
✓ Production-ready  

</div>

---

## 💡 Tips for Success

<div align="right">

1. Always build server and client separately
2. Test on clean Windows VM first
3. Keep entire dist/ folders when deploying
4. Run server before starting client
5. Use "localhost" for testing on same machine
6. Firewall may block - add exceptions
7. First webcam use takes time
8. Admin rights needed for some features
9. Read console output for debug info
10. Test all features before presentation

</div>

---

## 🎊 Ready to Start!

<div align="center">

**Next Steps:**

```bash
build_server.bat && build_client.bat
```

Then test all features!

**Good luck with your project! 🚀**

</div>

---

<div align="right">

*RemotePC Project v1.1*  
*Built for Windows - April 2026*  
*With App Manager Bug Fix Applied ✅*

</div>
