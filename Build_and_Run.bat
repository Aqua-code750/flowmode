@echo off
:start
echo ==========================================
echo    FlowMode APK Builder & Deployer
echo ==========================================
echo.
echo 1. Build Debug APK
echo 2. Install on Connected Phone (ADB)
echo 3. Build & Install
echo 4. Exit
echo.
set /p choice="Select an option (1-4): "

if "%choice%"=="1" goto build
if "%choice%"=="2" goto install
if "%choice%"=="3" goto build install
if "%choice%"=="4" exit

:build
echo Building APK...
call gradlew assembleDebug
echo.
echo APK Build Complete!
echo Location: app\build\outputs\apk\debug\app-debug.apk
pause
goto start

:install
echo Installing on device...
call gradlew installDebug
echo.
echo Installation Complete!
pause
goto start

:build install
echo Building and Installing...
call gradlew installDebug
echo.
echo Process Complete!
pause
goto start

:start
cls
goto start
