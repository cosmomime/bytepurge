# BytePurge Android Emulator Screenshot Guide

This document outlines the exact procedure for generating clean, privacy-safe screenshots for BytePurge without connecting or capturing a physical phone.

For the comprehensive mobile projects guide, refer to [EMULATOR_SCREENSHOT_GUIDE.md](file:///D:/Mobile%20App%20Projects/EMULATOR_SCREENSHOT_GUIDE.md).

---

## ⚡ Quick Capture Flow

### 1. Launch Virtual Device
```powershell
$env:ANDROID_AVD_HOME = "D:\Utilities\.android\avd"
& "D:\Android\Sdk\emulator\emulator.exe" -avd BytePurge_Pixel_AVD -gpu host -accel on
```

### 2. Configure SystemUI Demo Mode & Night Theme
```powershell
$adb = "D:\Android\Sdk\platform-tools\adb.exe"

& $adb -s emulator-5554 shell "input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard"
& $adb -s emulator-5554 shell settings put global sysui_demo_allowed 1
& $adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command enter
& $adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
& $adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
& $adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
& $adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
& $adb -s emulator-5554 shell cmd uimode night yes
```

### 3. Deploy & Grant Storage Permissions
```powershell
& $adb -s emulator-5554 install -r -g "D:\Mobile App Projects\storagemaid\app\build\outputs\apk\debug\app-debug.apk"
& $adb -s emulator-5554 shell appops set com.cosmomime.bytepurge.debug MANAGE_EXTERNAL_STORAGE allow
```

### 4. Launch & Capture Screenshot
```powershell
& $adb -s emulator-5554 shell am start -S -n com.cosmomime.bytepurge.debug/com.storagemaid.app.MainActivity
Start-Sleep -Seconds 2

& $adb -s emulator-5554 shell screencap -p /data/local/tmp/android_dashboard.png
& $adb -s emulator-5554 pull /data/local/tmp/android_dashboard.png "D:\Mobile App Projects\storagemaid\docs\screenshots\android_dashboard.png"
```

### 5. Shut Down & Save RAM
```powershell
& $adb -s emulator-5554 emu kill
```
