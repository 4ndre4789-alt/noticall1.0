@echo off
set APK=Noticall-test.apk

echo.
echo === Noticall - installazione test ===
echo Assicurati che adb sia disponibile e il telefono abbia Debug USB attivo.
echo.
adb devices
if errorlevel 1 goto error

echo.
echo Installazione APK con permessi limitati consentiti...
adb install -r --allow-restricted-permissions "%APK%"
if errorlevel 1 goto error

echo.
echo Concessione permessi...
adb shell pm grant com.noticall.app android.permission.READ_PHONE_STATE
adb shell pm grant com.noticall.app android.permission.READ_CALL_LOG
adb shell pm grant com.noticall.app android.permission.SEND_SMS

echo.
echo Avvio Noticall...
adb shell monkey -p com.noticall.app 1

echo.
echo Fatto. Ora configura Noticall e prova prima l'SMS di test.
pause
exit /b 0

:error
echo.
echo ERRORE. Controlla adb, il collegamento USB e il nome del file APK.
pause
exit /b 1
