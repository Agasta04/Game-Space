@echo off
title Space Shooter Game
echo ===================================================
echo     Memulai Space Shooter Game (JavaFX)
echo ===================================================
echo.

cd /d "%~dp0"

echo Menjalankan game dengan Maven Wrapper...
call mvnw.cmd javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [Gagal menjalankan game]
    echo Pastikan JDK 17 atau yang lebih baru sudah terpasang.
    pause
)

