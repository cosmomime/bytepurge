@echo off
title BytePurge Desktop
cd /d "%~dp0"
echo Starting BytePurge Desktop...
if exist "D:\Utilities\Android Studio\jbr\bin\javaw.exe" (
    start "" "D:\Utilities\Android Studio\jbr\bin\javaw.exe" --enable-native-access=ALL-UNNAMED -jar "desktop\build\compose\jars\BytePurge-windows-x64-1.0.0.jar"
) else (
    start "" javaw --enable-native-access=ALL-UNNAMED -jar "desktop\build\compose\jars\BytePurge-windows-x64-1.0.0.jar"
)
exit
