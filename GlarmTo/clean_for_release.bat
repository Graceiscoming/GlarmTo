@echo off
chcp 65001 >nul
color 0A
echo ========================================================
echo   GlarmTo Project Cleaner (For Final Submission)
echo ========================================================
echo.
echo สคริปต์นี้จะทำความสะอาดไฟล์ขยะ (Cache) ของโปรเจกต์ Android ทั้งหมด
echo เพื่อเตรียมพร้อมสำหรับการบีบอัด Zip ส่งอาจารย์ หรืออัปโหลดขึ้น GitHub
echo ซึ่งจะช่วยลดขนาดไฟล์จากระดับ กิกะไบต์ (GB) ให้เหลือแค่ไม่กี่เมกะไบต์ (MB)
echo.
echo หมายเหตุ: ครั้งต่อไปที่เปิด Android Studio โค้ดจะใช้เวลาโหลดโปรเจกต์ใหม่เล็กน้อย
echo.
pause

echo.
echo [1/4] กำลังลบโฟลเดอร์ Build ของแอปพลิเคชัน...
if exist "build" rmdir /s /q "build"
if exist "app\build" rmdir /s /q "app\build"
if exist ".cxx" rmdir /s /q ".cxx"
if exist "app\.cxx" rmdir /s /q "app\.cxx"

echo [2/4] กำลังลบแคชของ Gradle...
if exist ".gradle" rmdir /s /q ".gradle"

echo [3/4] กำลังลบไฟล์ตั้งค่าจำลองของ Android Studio...
if exist ".idea" rmdir /s /q ".idea"
del /s /q *.iml >nul 2>&1

echo [4/4] สั่งใช้งานคำสั่งย้ำ (Gradlew Clean)...
call gradlew clean

echo.
echo ========================================================
echo   ทำความสะอาดเสร็จสมบูรณ์! (Clean Complete!)
echo   โปรเจกต์ของคุณเบาหวิว พร้อมสำหรับการส่งงาน หรือ Build ขึ้นแบบคลีนๆ แล้วครับ!
echo ========================================================
pause
