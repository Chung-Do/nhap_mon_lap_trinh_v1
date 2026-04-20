Set oWS = WScript.CreateObject("WScript.Shell")
sLinkFile = oWS.ExpandEnvironmentStrings("%USERPROFILE%\Desktop\RemotePC Server.lnk")
Set oLink = oWS.CreateShortcut(sLinkFile)

' Đường dẫn tới file .bat
oLink.TargetPath = oWS.CurrentDirectory & "\download-and-run-server.bat"
oLink.WorkingDirectory = oWS.CurrentDirectory

' Icon - thay đổi đường dẫn này
' Ví dụ: C:\Windows\System32\imageres.dll,73 (icon cloud download)
oLink.IconLocation = "C:\Windows\System32\imageres.dll,73"

oLink.Description = "Download and run RemotePC Server"
oLink.Save

WScript.Echo "Shortcut created on Desktop!"
