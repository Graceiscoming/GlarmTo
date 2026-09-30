# บันทึกความคืบหน้าการพัฒนา GlarmTo (กล้ามโต)
**วันที่อัปเดต:** 25 มีนาคม 2026

---

## 🚀 สรุปงานที่ทำในวันนี้ (Phase 1: The MVP สำเร็จ 100%)

วันนี้เราได้วางโครงสร้างแกนกลางของแอปพลิเคชันทั้งหมด ตั้งแต่ระบบการนำทาง (Navigation) ไปจนถึงการจดบันทึกลงฐานข้อมูลในเครื่อง (Room Database) พร้อมธีมที่สวยงามดุดันตามคอนเซปต์ "Simple Log for Big Gains" สีแดงตัดดำครับ

### 1. โครงสร้างและการแสดงผล (Architecture & Theme)
*   **สร้างแท็บเมนูล่าง (Bottom Navigation):** แบ่งเป็น 4 แท็บหลัก ได้แก่ `Dashboard`, `Workout`, `Nutrition`, `Calculators` 
*   **ไฟล์ที่เกี่ยวข้อง:**
    *   `MainActivity.kt`: แกนกลางของระบบนำทาง (NavHost และ Scaffold)
    *   `Color.kt` / `Theme.kt`: เซ็ตระบบธีม **"แดงเดือดสลับดำ (Blood Red & Black)"** บังคับให้เป็นโหมด Dark Mode เสมอ

### 2. ระบบฐานข้อมูลและตัวกลางเชื่อมโยง (Database & Repository)
*   **เลือกใช้เทคโนโลยี:** Room Database ทำงานร่วมกับ KSP (Kotlin Symbol Processing) แทนที่ KAPT
*   **ไฟล์ที่เกี่ยวข้อง:**
    *   `libs.versions.toml` และไฟล์ `build.gradle.kts`: เพิ่มปลั๊กอิน KSP โฉมใหม่ที่อุดช่องโหว่การแปลงคลาสผิดพลาดได้
    *   `WorkoutEntity.kt` / `NutritionEntity.kt`: โครงสร้างตารางจดบันทึกเซตออกกำลังกาย และค่าแคลอรี่รายวัน
    *   `GlarmToDao.kt`: หัวใจของการจัดการข้อมูล (สร้างโค้ดแบบไม่ต้องพึ่งพา `suspend` เพื่อป้องกันบั๊ก Generic Name Clash จาก Kotlin 2.0.21)
    *   `GlarmToRepository.kt`: จัดการการดึงข้อมูลรายวัน รวมถึงการโยนภาระประมวลผลเซฟและลบข้อมูลไปทำเบื้องหลังให้แอปไม่กระตุก (`Dispatchers.IO`)
    *   `GlarmToApplication.kt` / `AndroidManifest.xml`: ลงทะเบียนและสร้างตัวแปรเพื่อเรียกใช้ Database กลาง (พร้อมระบุแบบ Fully Qualified Name (FQN) เพื่อแก้บั๊กเข้าแอปแล้วเด้งออก)

### 3. หน้าจอและฟีเจอร์หลัก (Screens & Features)
*   **Workout Logger (บันทึกเซตการยก):** 
    *   ผู้ใช้สามารถกรอก "ชื่อท่า" -> "น้ำหนัก" -> "จำนวนครั้ง" ลงระบบได้
    *   มีปุ่ม **"คัดลอกเซตเดิม (Copy Last)"** ดึงชื่อท่าจากเซตล่าสุดมาใช้ใหม่ ทุ่นแรงพิมพ์
    *   **ไฟล์ที่เกี่ยวข้อง:** `WorkoutScreen.kt` และ `WorkoutViewModel.kt`
*   **Nutrition Counter (ตัวนับโควต้าแคลอรี่):** 
    *   กำหนดเป้าหมายรายวันได้ (Daily Goal) แถมบันทึกลง `SharedPreferences` ทำให้ปิดเปิดแอปใหม่ค่าเป้าหมายยังอยู่ครบ
    *   ตัวพิมพ์ชื่ออาหารและแคลอรี่แบบ Manual
    *   มีหลอดความคืบหน้า (Progress Bar) ที่อิงตามเปอร์เซ็นโควต้ายิ่งกินหลอดยิ่งใกล้เต็ม
    *   **ไฟล์ที่เกี่ยวข้อง:** `NutritionScreen.kt` และ `NutritionViewModel.kt`
*   **Calculators (เครื่องคิดเลขมวลอวัยวะ):**
    *   ระบบเครื่องคำนวณ **BMR/TDEE** (สำหรับ Bulking, Cutting) รูปร่าง และเครื่องคำนวณสถิติสูงสุด **1RM (One Rep Max)** 
    *   แก้ไขอาการแป้นพิมพ์ค้างด้วยการล้างตัวแปรและการจัดการ `FocusManager`
    *   **ไฟล์ที่เกี่ยวข้อง:** `CalculatorScreen.kt`
*   **Dashboard (สรุปรวมหน้าแรก):**
    *   หน้าจอสรุปทุกอย่างในวันเดียว "วันนี้เราเหนื่อยมาแค่ไหน"
    *   คำนวณรวมข้อมูลแบบเรียลไทม์ (Total Sets, Total Volume Kg, Calories Left) ข้อมูลไหลมาจากระบบ Flow
    *   **ไฟล์ที่เกี่ยวข้อง:** `DashboardScreen.kt`, `DashboardViewModel.kt`

---

## 🎯 พรุ่งนี้ทำอะไรต่อดี? (ก้าวสู่ Phase 2)

เราจะเริ่มต่อยอดในส่วนของ **Visualization & Usability (ทำให้ดูง่ายและน่าเล่น)** โดยอ้างอิงจากแผนเดิม:

1.  **History Log (ดูย้อนหลัง):** สร้างหน้าปฏิทินที่เรียกข้อมูลการเล่นของวันก่อนๆ กลับมาดู (ดึงข้อมูล Query หาจากตัวเลขช่วงเวลาเริ่มจนเริ่มวันใน Dao)
2.  **Exercise Preset (Dropdown ท่า):** ลดความน่าเบื่อในการพิมพ์เอง เลือกรวบรวมฐานข้อมูลท่าเบสิกเช่น Push up, Squat
3.  **Rest Timer:** นาฬิกาจับเวลาพักระหว่างลุยเซต
4.  **Gamification:** เริ่มสะสมแต้ม Level!


---

**วันที่อัปเดต:** 5 เมษายน 2026

## 🚀 สรุปงานที่ทำในวันนี้ (Phase: Multi-User & Personalized Evolution)

วันนี้เราได้ยกระดับแอปไปอีกขั้น จากเดิมที่เป็นแอปจดบันทึกรวมๆ ให้กลายเป็น **"แอปส่วนบุคคล (Personalized)"** ที่รองรับผู้ใช้หลายคนในเครื่องเดียว และมีระบบคำนวณเป้าหมายแคลอรี่อัจฉริยะตามร่างกายจริงของผู้ใช้ครับ

### 1. ระบบจัดการผู้ใช้ (Local Multi-User System)
*   **ระบบ Login/Logout:** สร้างหน้าจอ Login โทนสีแดงดำดุดัน รองรับการเข้าใช้งานด้วยชื่อตัวเอง (Username)
*   **Data Isolation (แยกประวัติชัดเจน):** ปรับปรุงระบบฐานข้อมูลใหม่ให้จดจำว่าข้อมูลการเล่นเวทและอาหารจานไหนเป็นของใคร ล็อกอินชื่อใครเห็นเฉพาะของคนนั้น ข้อมูลไม่ปนกันแน่นอน
*   **ไฟล์ที่เกี่ยวข้อง:**
    *   `LoginScreen.kt`: หน้าจอกรอกชื่อสไตล์เท่ๆ
    *   `SessionManager.kt`: ระบบจำสถานะการล็อกอินในเครื่อง (SharedPreferences)
    *   `GlarmToRepository.kt`: กรองข้อมูล Query ทั้งหมดตามชื่อผู้ใช้ปัจจุบัน

### 2. ระบบต้อนรับและโปรไฟล์ (Onboarding & Profile)
*   **Onboarding Flow:** เมื่อสมัครใหม่ ระบบจะบังคับให้กรอก อายุ, เพศ, น้ำหนัก, ส่วนสูง เพื่อรู้จักตัวตนผู้ใช้
*   **Automatic TDEE Calculation:** ใช้สูตรทางการแพทย์ (Mifflin-St Jeor) คำนวณเป้าหมายแคลอรี่ (Maintenance) อัตโนมัติทันทีที่กรอกโปรไฟล์เสร็จ
*   **Profile Tab (แทนที่หน้า Calcs เดิม):** ปรับโฉมเมนู Calcs ให้กลายเป็นหน้า Profile แสดงสถิติตัวเอง และสามารถกด Edit เพื่อปรับน้ำหนัก/ส่วนสูงใหม่ได้ตลอดเวลา (เลขแคลอรี่เป้าหมายจะอัปเดตให้ทั่วแอปทันที)
*   **ไฟล์ที่เกี่ยวข้อง:**
    *   `OnboardingScreen.kt`: หน้าจอต้อนรับและกรอกข้อมูลเริ่มต้น
    *   `CalculatorScreen.kt` (โฉมใหม่): หน้า Profile และ 1RM Calculator
    *   `HealthCalculator.kt`: หัวใจของการคำนวณสุขภาพ (Centralized Logic)

### 3. การอัปเกรดฐานข้อมูล (Database Migration V3)
*   **Version Up:** อัปเกรด Database จาก Version 1 -> 2 -> 3 เพื่อเพิ่มตาราง `user_log` และคอลัมน์เก็บข้อมูลโปรไฟล์
*   **Migration Scripts:** เขียนสคริปต์อพยพข้อมูลอย่างเป็นระบบ ป้องกันแอปเด้งและรักษาข้อมูลเก่าให้ปลอดภัย

### 4. ฟีเจอร์ยกระดับการใช้งาน (Evolution Features)
*   **Exercise Presets (เลือกท่ามาตรฐาน):** เพิ่มระบบ Dropdown ที่มีรายชื่อท่าออกกำลังกายแยกตามส่วนร่างกาย (Chest, Back, Legs, ฯลฯ) ช่วยให้ไม่ต้องพิมพ์เองทกครั้งที่เล่นท่าเดิม
*   **History Log (ปฏิทินย้อนหลัง):** เพิ่มหน้าจอประวัติที่สามารถเลือกวันที่ผ่านปฏิทิน (DatePicker) เพื่อย้อนกลับไปดูรายการอาหารและท่าที่ยกในวันนั้นๆ ได้แบบเรียลไทม์
*   **Progress Charts (กราฟพลังกล้าม):** แสดงกราฟแท่ง (Weekly Volume) 7 วันล่าสุดที่หน้า Dashboard เพื่อให้เห็นพัฒนาการความหนักในการฝึกซ้อมอย่างชัดเจน

### 5. ระบบตรวจสอบความแม่นยำ (Unit Testing)
*   **Consolidated Testing:** สร้างไฟล์ `LoginSetupTest.kt` เพื่อให้มั่นใจ 100% ว่าสูตรคำนวณแคลอรี่ที่ผู้ใช้ได้เห็นนั้นถูกต้องเป๊ะตามสัดส่วนร่างกาย
*   **Edge Case Handling:** ทดสอบกรณีผู้ใช้กรอกตัวเลขแปลกๆ (เช่น อายุ 0 หรือค่าน้ำหนักติดลบ) ระบบจะจัดการให้ปลอดภัยไม่พัง

---

**วันที่อัปเดต:** 5 เมษายน 2026 (รอบค่ำ)

## 🚀 สรุปงานที่ทำในวันนี้ (Phase 3: Workout Routines & Date Constraints)

เราได้เพิ่มความสามารถในการ "วางแผน" และ "ความแม่นยำของข้อมูล" เข้าไปอีกระดับ โดยเน้นไปที่การลดภาระการกรอกข้อมูลของผู้ใช้ และการควบคุมกฎเวลาในการบันทึกให้เหมือนแอปฟิตเนสระดับโลกครับ

### 1. ระบบจัดเซตใหญ่ (Custom Workout Routines) [NEW TAB]
*   **สร้างแท็บที่ 5 "Routines":** เพิ่มไอคอนรายการ (List) 📋 เพื่อให้ผู้ใช้สามารถสร้าง "เทมเพลต" การซ้อมส่วนตัวได้ (เช่น ตั้งชื่อว่า "ตารางอก" แล้วเลือกท่าเตรียมไว้ล่วงหน้า)
*   **ระบบ Load Routine & Queue:** ในหน้า Workout สามารถกด "Load Routine" เพื่อดึงท่าที่วางแผนไว้มาจดได้ทันที และมีปุ่ม "Next Exercise" เพื่อสลับท่าถัดไปในลิสต์ให้อัตโนมัติ ทุ่นเวลาพิมพ์ชื่อท่าไปได้ 300%
*   **ไฟล์ที่เกี่ยวข้อง:**
    *   `RoutinesScreen.kt` / `RoutineViewModel.kt`: หน้าจัดการสร้างและลบแผนการซ้อม
    *   `RoutineEntity.kt`: ตารางฐานข้อมูลใหม่สำหรับเก็บข้อมูลแผนการซ้อม

### 2. ระบบเลือกวันและจำกัดสิทธิ์แก้ไข (DatePicker & Date Constraints)
*   **History Lookup ในตัว:** เพิ่ม Header ปฏิทิน 🗓️ ในหน้า Workout และ Nutrition ให้สามารถจิ้มเลือกวันเพื่อถอยกลับไปดูประวัติ หรือไปข้างหน้าเพื่อวางแผนได้สะดวก (ไม่ต้องเข้าหน้า History แยกซ้อนกัน)
*   **กฎการแก้ไข (Editing Constraints):**
    *   **Workout:** ให้จด/แก้ข้อมูลได้เฉพาะ **"วันนี้และเมื่อวาน"** เท่านั้น (ป้องกันการแก้ประวัติย้อนหลังนานเกินไปจนเลอะเทอะ)
    *   **Nutrition:** ให้จด/แก้ได้เฉพาะ **"วันนี้จนถึงล่วงหน้า 7 วัน"** (เน้นการจดปัจจุบันและการวางแผนกินล่วงหน้า)
    *   หากเลือกวันนอกเหนือจากนี้ ระบบจะซ่อนปุ่มเพิ่ม/ลบข้อมูลอัตโนมัติ พร้อมแสดงแถบเตือนอย่างสวยงาม
*   **ไฟล์ที่เกี่ยวข้อง:** `WorkoutScreen.kt`, `NutritionScreen.kt`

### 3. การอัปเกรดฐานข้อมูล (Database Migration V4)
*   **Version Up:** อัปเกรดจาก Version 3 -> 4 เพื่อเพิ่มตาราง `routine_log`
*   **Migration Script:** เขียนโค้ดสคริปต์ SQL สำหรับสร้างตารางใหม่โดยไม่กระทบข้อมูลผู้ใช้เดิม

### 4. ระบบทดสอบและคู่มือความถูกต้อง (Testing & Walkthrough)
*   **Manual Test Cases:** อัปเดตไฟล์ `test_cases.md` เพิ่มขั้นตอนการทดสอบ (Checklist) สำหรับระบบ Routine และระบบล็อควันที่ เพื่อให้การ QA ทำได้รวดเร็ว
*   **Routine Logic Test:** สร้าง `RoutineLogicTest.kt` เพื่อทดสอบระบบการเชื่อมรายชื่อท่าเข้าด้วยกัน (Serialization) ให้มั่นใจว่าข้อมูลแผนการซ้อมจะไม่เพี้ยน
*   **Walkthrough Guide:** สร้างไฟล์สรุปวิธีใช้งานระบบ Routine แบบ Step-by-Step เพื่อให้ผู้ใช้ใหม่เข้าใจระบบการ "Queue" ท่าได้ทันที

---

**วันที่อัปเดต:** 6 เมษายน 2026

## 🚀 สรุปงานที่ทำในวันนี้ (Phase 4: Rest Timer & Gamification)

เราได้เปลี่ยนแอป GlarmTo ให้เป็นมากกว่าแค่เครื่องมือจดบันทึก แต่เป็น "เกมชีวิตจริง" ที่ช่วยให้การเข้ายิมสนุกขึ้น และช่วยจัดระเบียบการพักให้นิ่งขึ้นครับ

### 1. ระบบนาฬิกาพักอัตโนมัติ (Dynamic Rest Timer)
*   **Auto-Trigger:** ทันทีที่กดบันทึกเซต (`Add Set`) ระบบจะเริ่มนับถอยหลังการพักตามเวลาที่ตั้งค่าไว้ (ค่าเริ่มต้นคือ 60 วินาที)
*   **Flexible Control:** 
    *   เพิ่มปุ่ม **-30s** และ **+30s** สำหรับปรับเวลาพักระหว่างทาง
    *   ปุ่ม Skip กรณีที่ต้องการลุยต่อทันที
*   **Default Rest Setting:** พี่สามารถเข้าไปตั้งค่า "เวลาพักเริ่มต้น" ของตัวเองได้แล้วที่หน้า **Profile** เพื่อให้ระบบจำเวลาที่พี่ถนัดที่สุดครับ!
*   **ไฟล์ที่เกี่ยวข้อง:** `WorkoutScreen.kt`, `CalculatorScreen.kt`

### 2. ระบบเก็บเลเวลและค่าประสบการณ์ (XP Refined)
*   **XP Balance (Daily Cap):** เพิ่มโควตาการรับ XP สูงสุดที่ **300 XP ต่อวัน** เพื่อความเป็นธรรมและป้องกันการปั๊มเลเวล (เน้นคุณภาพมากกว่าปริมาณ)
*   **Level Progression (+10%):** ปรับจูนความยากในการเลื่อนเลเวลให้ท้าทายขึ้นแบบก้าวหน้า (Level Up ครั้งถัดไปจะใช้แต้มเพิ่มขึ้น 10% จากเลเวลก่อนหน้า)
*   **XP Reset Logic:** ระบบจะตรวจสอบวันเวลาอัตโนมัติ เมื่อขึ้นวันใหม่แต้มโควตา 300 XP จะถูกรีเซ็ตให้เริ่มใหม่ทันที
*   **Improved UI:** 
    *   หน้าแรก (Dashboard) และหน้า Profile จะแสดงความก้าวหน้าของเลเวลตามจริง
    *   เพิ่มแถบ **"Today's XP: X / 300"** ในหน้า Profile เพื่อให้พี่รู้ว่าวันนี้เก็บแต้มได้ตามเป้าหรือยัง
*   **ไฟล์ที่เกี่ยวข้อง:** `UserEntity.kt`, `GlarmToRepository.kt`, `DashboardScreen.kt`, `CalculatorScreen.kt`

### 3. ระบบป้องกันบัคหลงวัน (Midnight Date Drift Fix)
*   **Time Normalization:** ปรับปรุงระบบหลังบ้านให้รีเซ็ตเศษ "ชั่วโมง/นาที/วินาที" เป็น **00:00:00 (Start of Day)** เสมอเมื่อมีการบันทึกหรือสืบค้นข้อมูล เพื่อความแม่นยำสูงสุด
*   **Reactive Today:** แก้ไขปัญหาข้อมูล "ผีหลอก" ข้ามวัน โดยการทำให้ Dashboard และหน้าจดบันทึกรับรู้การเปลี่ยนผ่านของเที่ยงคืนได้แม่นยำขึ้น ข้อมูลจะไม่ไหลตามไปวันใหม่แน่นอน
*   **ไฟล์ที่เกี่ยวข้อง:** `WorkoutViewModel.kt`, `DashboardViewModel.kt`, `HistoryViewModel.kt`, `GlarmToRepository.kt`

### 4. ปุ่มทางลัดย้อนกลับ "วันนี้" (Today Shortcut)
*   **One-Tap Return:** เพิ่มปุ่ม **"Go to Today"** ในหน้า Workout กรณีที่พี่เลือกดูวันย้อนหลังอยู่ สามารถกดกลับมาบันทึกของวันนี้ได้ทันทีในคลิกเดียว ไม่ต้องเปิดปฏิทินหาเอง
*   **Selective Visibility:** ปุ่มนี้จะปรากฏเฉพาะเมื่อพี่ไม่ได้อยู่ที่วันปัจจุบันเท่านั้น เพื่อความสะอาดของหน้าจอครับ

### 5. ระบบตรวจสอบความถูกต้อง (Unit Testing - Midnight & XP)
*   **XP & Date Validation:** สร้างชุดทดสอบ `XPLimitAndMidnightTest.kt` เพื่อตรวจสอบ 3 เรื่องหลัก:
    *   การจำกัด XP 300 ต่อวันทำงานได้จริง
    *   ระบบต้องรีเซ็ตแต้มเป็น 0 ทันทีเมื่อขึ้นวันใหม่
    *   การสืบค้นข้อมูลตามช่วงเวลา (Start/End of Day) ต้องแม่นยำไม่เหลื่อมกัน
*   **Robust Architecture:** ปรับปรุง `SessionManager` ให้รองรับการทำ Unit Test ได้ดีขึ้น (Make it open for testing)


**วันที่อัปเดต:** 7 เมษายน 2026

### 🚀 อัปเกรดระบบ Workout Session & Performance Tracker (Phase 4.0)
เปลี่ยนหน้าจด Workout แบบเดิมให้กลายเป็นระบบ Tracking มืออาชีพที่ติดตามทุกวินาทีของการซ้อม

#### 1. ระบบจัดการ Workout Session (Start/Stop Workout)
*   **Active Tracking:** เพิ่มปุ่ม **Start Workout** เพื่อเริ่มนับเวลาในการซ้อมจริง (Session-based)
*   **Real-time Stopwatch:** นาฬิกาจับเวลา ⏱ แสดงที่ส่วนบนของหน้าจอเมื่ออยู่ในโหมดซ้อม เพื่อความแม่นยำในการพักและฝึกซ้อม
*   **Session-Specific Logging:** ทุกเซต (Sets) ที่บันทึกในโหมดนี้ จะถูกผูกติด (Link) เข้ากับไอดีของเซสชันนั้นๆ ทันที ทำให้สามารถแยกข้อมูลการซ้อมเช้า-เย็นได้ชัดเจน

#### 2. หน้าสรุปข้อมูลก่อนและหลังการซ้อม (Dashboard & Feedback)
*   **Pre-Workout Stats:** ส่วนบนของหน้า Workout จะโชว์สรุปแคลอรี่ของวันนี้ (เป้าหมาย vs ที่กินไปแล้ว) ก่อนเริ่มฝึกซ้อม
*   **Workout Summary Form (Finish Mode):** เมื่อกดปุ่ม Finish แอปจะเปิดหน้าต่างให้จดสรุปความรู้สึกหลังซ้อม:
    *   **Custom Session Name:** ตั้งชื่อรอบได้ (เช่น "Leg Day สุดโหด") หรือจะใช้ชื่อวันที่อัตโนมัติ
    *   **Notes:** บันทึกข้อความสั้นๆ เกี่ยวกับฟอร์มการเล่นหรือสิ่งที่อยากปรับปรุง
    *   **Performance Ratings:** บันทึกระดับความเหนื่อยและความพึงพอใจ (Rating 1-5 Stars ⭐️) โดยใช้ไอคอนดาวที่สวยงามและ Minimalist

#### 3. ปรับปรุง UI/UX แบบ Minimalist Clean (Expandable List)
*   **Grouped Sessions:** รายการ Workout ถูกเปลี่ยนเป็นแบบกล่อง Sessions ที่สวยงามและสะอาดตา ทั้งในหน้า **Workout** และหน้า **History** (ประวัติย้อนหลัง)
*   **Expand/Collapse:** ผู้ใช้สามารถกดที่กล่องเพื่อกางดูรายละเอียดท่าออกกำลังกาย, โน้ตที่จดไว้, และระดับคะแนนดาวที่บันทึกไว้ในอดีตได้ครบถ้วน
*   **Uncategorized Sets:** ระบบจัดการชุดข้อมูลที่ไม่มีไอดีเซสชัน (Legacy Data) โดยการรวมไว้ในกลุ่ม "Uncategorized" เพื่อป้องกันข้อมูลสูญหาย

#### 4. การจัดการฐานข้อมูลและสถาปัตยกรรม (Core & Database)
*   **Unified History View:** อัปเดต `HistoryViewModel` และ `HistoryScreen` ให้รองรับการดึงข้อมูล Session ย้อนหลัง ทำให้ประสบการณ์การใช้งานลื่นไหลและต่อเนื่องกันทั้งแอป
*   **Database Migration (Version 8 -> 10):**
    *   เพิ่มตาราง `workout_sessions` เพื่อเก็บ metadata ของแต่ละรอบการซ้อม
    *   ปรับปรุง `workout_log` ให้รองรับการเชื่อมโยง Session ID
    *   เพิ่ม Migration Logic เพื่อป้องกันข้อมูลสูญหายระหว่างการอัปเกรดแอป
*   **Security & Bug Fixes:**
    *   **Username Consistency:** แก้ไข Bug เรื่องชื่อผู้ใช้ (Username) ระหว่างการ Update ข้อมูลเซสชัน ทำให้ข้อมูลไม่หายเมื่อบันทึกผล
    *   **Robust State Management:** ปรับปรุง Flow การดึงข้อมูลให้แม่นยำขึ้น แก้ไขปัญหา Error "Unresolved reference" ของไอคอนต่างๆ

---

**วันที่อัปเดต:** 12 เมษายน 2026

## ปรับปรุงความถูกต้อง ประสิทธิภาพ และความปลอดภัยของระบบ (Hardening & optimization)

*   **`GlarmToRepository.login`:** เปลี่ยนเป็น `suspend` และรอ `withContext(Dispatchers.IO)` ให้สร้าง/โหลดผู้ใช้จาก Room เสร็จก่อน แล้วค่อย `onLoginSuccess()` จาก `LoginScreen` (แก้ race ที่เคยทำให้ `isProfileSetup()` อ่านค่า prefs ก่อน DB อัปเดต)
*   **`GlarmToDao.getWorkoutsForSession`:** เพิ่มเงื่อนไข `AND username = :username` และให้ repository ส่ง username ปัจจุบัน — แยกข้อมูลเซตตามเซสชันต่อผู้ใช้ชัดเจน
*   **`revokeXP` / ลบเซตและมื้ออาหาร:** เมื่อ `deleteWorkout` / `deleteNutrition` จะถอน XP สอดคล้องกับตอนเพิ่ม (10 / 5) และปรับ `dailyXPEarned` เมื่อยังเป็นวันเดียวกับ `lastXPDate`
*   **`SessionManager.logoutUser`:** ลบคีย์ `profile_setup_<username>` ของผู้ที่ออกระบบด้วย เพื่อลดข้อมูลค้างบนเครื่องแชร์
*   **`WorkoutViewModel`:** ยกเลิกลูป `while (true)` ตลอดเวลา — ใช้ `Job` จับเวลาเฉพาะตอน `Start Workout` และยกเลิกเมื่อจบ/ `onCleared` (ประหยัด CPU/แบต)
*   **`CalendarDayUtils`:** แปลงวันจาก Material3 DatePicker (UTC) เป็นต้นวันในโซนเครื่อง, ใช้ร่วมกับ Nutrition (ช่วงแก้ไข + `SelectableDates`), ค่าเริ่มต้นวันใน Nutrition/History/Workout
*   **`NutritionScreen`:** `SelectableDates` บังคับทั้งขอบล่างและบนด้วยช่วงเดียวกับ `isNutritionDateValid`
*   **`CalculatorScreen`:** ลบ FQN ที่ซ้ำซ้อน ใช้ `GlarmToApplication` แบบ import เดียว
*   **`XPLimitAndMidnightTest`:** ย้าย fake ไป `testsupport/TestDoubles.kt` (`RecordingFakeGlarmToDao`, `StaticFakeSessionManager`) และเพิ่มเทส `revokeXP`

## ชุด Unit / JVM tests (`app/src/test`)

รันทั้งโมดูลจากโฟลเดอร์ `GlarmTo`: `./gradlew test`  
(ต้องตั้ง `JAVA_HOME` และ Android SDK ให้ Gradle ใช้งานได้)

*   **`testsupport/TestDoubles.kt`:** `RecordingFakeGlarmToDao` + `StaticFakeSessionManager` / `MutableFakeSessionManager` สำหรับจำลอง Room และเซสชันโดยไม่ต้องเปิด Emulator
*   **`CalendarDayUtilsTest`:** ช่วง Nutrition, normalize วัน, แปลง UTC DatePicker → local
*   **`ExercisePresetsTest`:** รายการท่า sorted / ไม่ซ้ำ / มีหมวดหลัก
*   **`HealthCalculatorGoalsTest`:** เป้า Cut/Bulk กับ TDEE และ `workoutDays`
*   **`GlarmToRepositoryTest`:** login ผู้ใช้ใหม่/เก่า, insert/delete workout + XP, `getWorkoutsForSession` กรอง user, สูตร XP threshold
*   **`XPLimitAndMidnightTest`:** ขอบเขตวัน, cap XP 300, รีเซ็ตข้ามวัน, `revokeXP`
*   **`SessionManagerRobolectricTest`:** SharedPreferences จริงผ่าน Robolectric
*   **`WorkoutViewModelRobolectricTest` / `NutritionViewModelRobolectricTest` / `CalculatorViewModelRobolectricTest` / `DashboardViewModelRobolectricTest` / `HistoryViewModelRobolectricTest` / `RoutineViewModelRobolectricTest`:** ViewModel + `MainDispatcherRule` (ที่จำเป็น) + delay สั้นๆ ให้ coroutine จบ
*   **`RoutineLogicTest`:** ต่อจากเดิม + roundtrip `RoutineEntity` กับ pipe
*   **`LoginSetupTest`:** TDEE (เดิม)
*   **`ExampleUnitTest`:** smoke ว่าโปรเจกต์เปิดใช้ unit test

**Dependencies สำหรับเทส:** `kotlinx-coroutines-test`, `lifecycle-runtime-testing`, `androidx.test:core`, `org.robolectric:robolectric` และ `testOptions.unitTests.isIncludeAndroidResources = true` ใน `app/build.gradle.kts`

**Instrumented tests (`connectedAndroidTest`):** ยังใช้ `ExampleInstrumentedTest` ตรวจ package name — รันบนอุปกรณ์/Emulator: `./gradlew connectedDebugAndroidTest`

---

**วันที่อัปเดต:** 12 เมษายน 2026

## ฟีเจอร์ใหม่: ส่งออกข้อมูล, สถิติ, โภชนาการเชิงโครง, น้ำ, Plate calculator, RPE, คัดลอกจากเมื่อวาน (Database v11)

รอบนี้เพิ่มความสามารถใช้งานจริงโดย **ไม่ตัดฟังก์ชันเดิม** — มี migration **10 → 11** สำหรับผู้ใช้ที่อัปเกรดจากแอปเวอร์ชันก่อน

### 1. ฐานข้อมูลและโมเดล (Room v11)
*   **`user_log`:** เพิ่ม `macroProteinPct`, `macroCarbPct`, `macroFatPct` (ค่าเริ่ม 30 / 40 / 30), `dailyWaterGoalMl` (ค่าเริ่ม 2000 ml)
*   **`workout_log`:** เพิ่มคอลัมน์ `rpe` (nullable, เก็บ RPE 1–10 ต่อเซต)
*   **ตาราง `water_log`:** บันทึกน้ำเป็นรายการต่อวันต่อผู้ใช้ (`username`, `dateInMillis`, `amountMl`)

### 2. ส่งออก / สำรองข้อมูล (Export)
*   **`GlarmToExport`:** สร้างข้อความ **JSON** (รวม user ย่อ, workouts, nutrition, water) และ **CSV** สำหรับนำไปเก็บหรือแชร์
*   **`GlarmToRepository`:** `exportUserDataJson()`, `exportUserDataCsv()` — ดึงข้อมูลทั้งหมดของผู้ใช้ปัจจุบันจาก Room
*   **Dashboard:** ปุ่ม **Export JSON** / **Export CSV** เปิด Share sheet (ส่งเป็น plain text)

### 3. สถิติและ streak
*   **Streak:** นับจำนวนวันติดกันที่มีอย่างน้อยหนึ่งเซต (จาก `workout_log`) โดยเริ่มจากวันนี้หรือเมื่อวานถ้าวันนี้ยังไม่มีเซต
*   **ช่วงสถิติ:** เลือก **7 วัน** หรือ **30 วัน** — แสดง **ปริมาณ volume รวม (kg)** และ **จำนวนเซต** ในช่วงนั้น
*   แสดงบน Dashboard ใต้การ์ดสวัสดี / ปุ่ม export

### 4. น้ำดื่ม (Water)
*   **Repository / DAO:** `getWaterForDay`, `insertWater`, `deleteWater`
*   **Nutrition:** การ์ดน้ำตามวันที่เลือก — ปุ่ม +250 ml / +500 ml, ลบรายการ, แถบความคืบหน้าเทียบกับ `dailyWaterGoalMl`
*   **Dashboard:** สรุปน้ำวันนี้เทียบเป้า (ml)

### 5. เป้าหมายมาโคร (สัดส่วนจากแคลอรี่)
*   **`HealthCalculator.macroGramsFromCalories`:** คำนวณกรัมโปรตีน / คาร์บ / ไขมันจาก `dailyGoal` และเปอร์เซ็นต์ (4 kcal/g สำหรับ P และ C, 9 kcal/g สำหรับ F)
*   **Profile (แก้ไขโปรไฟล์):** ช่องปรับ % มาโครและเป้าน้ำ (ml/วัน) — บันทึกลง `UserEntity`
*   **Nutrition:** ข้อความสรุปมาโครโดยประมาณอ้างอิงเป้าแคลอรี่ปัจจุบัน

### 6. Plate calculator
*   **`PlateCalculator`:** คำนวณโหลดแผ่นต่อข้างแบบ symmetric (บาร์ + แผ่นสองข้าง) แบบ greedy จากขนาดแผ่นที่มี
*   **แท็บใหม่ใน Profile:** 「Plate load」— กรอกน้ำหนักบาร์, เป้าหมายรวม, รายการแผ่น (kg คั่นด้วยจุลภาค)

### 7. RPE และคัดลอกจากเมื่อวาน
*   **Workout:** ช่อง **RPE (1–10)** ตอนกด Add Set, แสดงในรายการเซต; ปุ่ม **Copy all sets from yesterday** ดึงเซตของวันก่อนหน้ามาใส่วันที่เลือก (และ session ปัจจุบันถ้ามี) — **ไม่ให้ XP** ตอนคัดลอก
*   **Nutrition:** ปุ่ม **Copy yesterday** — คัดลอกมื้อจากวันก่อนหน้ามาวันที่เลือก — **ไม่ให้ XP**

### 8. การทดสอบและ test support
*   **`testsupport/MainDispatcherRule.kt`:** ใช้แทน `androidx.lifecycle.testing.MainDispatcherRule` เพื่อให้ unit test compile ได้เสถียรบน classpath ชุดเดียวกับโปรเจกต์
*   **`RecordingFakeGlarmToDao`:** รองรับเมธอด DAO ใหม่ (น้ำ, export, ช่วงวันที่)
*   **`LoginSetupTest` / `DashboardViewModelRobolectricTest`:** ปรับ expected / พฤติกรรม collect ให้ตรงกับ TDEE + multiplier และ `dailyGoal` StateFlow
*   **`CalculatorViewModelRobolectricTest`:** อัปเดตพารามิเตอร์ `updateProfile` ให้รวมมาโครและเป้าน้ำ

### 9. รายการฟังก์ชัน / API หลักที่เพิ่ม (อ้างอิงโค้ด)

**`com.example.glarmto.data.util`**
*   **`HealthCalculator.macroGramsFromCalories(dailyCalories, proteinPct, carbPct, fatPct)`** → `Triple<Int,Int,Int>` กรัม P/C/F
*   **`GlarmToExport.toJson(...)`** / **`GlarmToExport.toCsv(...)`** — สตริงส่งออก
*   **`PlateCalculator.computeLoad(targetTotalKg, barKg, plateSizesKg)`** → `Result?` และ **`PlateCalculator.isGoodEnough(Result)`**

**`com.example.glarmto.data.repository.GlarmToRepository`**
*   **`getWaterForDay(dateMillis)`** → `Flow<List<WaterEntity>>`
*   **`insertWater(amountMl, dateInMillis)`** / **`deleteWater(id)`**
*   **`exportUserDataJson()`** / **`exportUserDataCsv()`** → `String`
*   **`getPeriodTrainingStats(daysBackInclusive)`** → **`PeriodTrainingStats`** (volume + จำนวนเซต)
*   **`getTrainingStreakDays()`** → `Int`
*   **`copyWorkoutsFromPreviousDay(targetDayMillis, sessionId)`** / **`copyNutritionFromPreviousDay(targetDayMillis)`**
*   **`insertWorkout(..., awardXp: Boolean = true)`** / **`insertNutrition(..., awardXp: Boolean = true)`** — พารามิเตอร์หลังใช้กันเองตอนคัดลอก (DAO โดยตรงไม่ผ่านสาขานี้)

**`com.example.glarmto.data.local.dao.GlarmToDao`**
*   **`getWaterForDate`**, **`insertWater`**, **`deleteWater`**
*   **`getAllWorkoutsForUser`**, **`getAllNutritionForUser`**, **`getAllWaterForUser`**, **`getWorkoutsBetween`**, **`getNutritionBetween`** — แบบ **`fun` คืน `List` (blocking)** เรียกเฉพาะบน `Dispatchers.IO` (หลีกเลี่ยงปัญหา generate ของ Room+KSP กับ `suspend` ชุดเดียวกัน)

**ViewModel / UI (สรุป)**
*   **`DashboardViewModel`:** `todayWaterMl`, `waterGoalMl`, `trainingStreakDays`, `periodTrainingStats`, `setStatsPeriodDays`, `shareExportJson`, `shareExportCsv` — constructor ใช้ **`private val repository`**
*   **`WorkoutViewModel`:** **`addWorkout(..., rpe)`**, **`copyWorkoutsFromYesterday()`**
*   **`NutritionViewModel`:** **`userFlow`**, **`waterEntries`**, **`addWater`**, **`deleteWater`**, **`copyMealsFromYesterday()`**
*   **`CalculatorViewModel`:** **`updateProfile(..., macroProteinPct, macroCarbPct, macroFatPct, dailyWaterGoalMl)`**

### 10. Unit tests ที่เพิ่มสำหรับฟีเจอร์ใหม่ (`app/src/test/...`)

| ไฟล์ | สิ่งที่ทดสอบ |
|------|----------------|
| **`MacroGramsTest.kt`** | `macroGramsFromCalories` — สัดส่วน 30/40/30 ที่ 2000 kcal, กรณีเปอร์เซ็นต์รวม ≠ 100, แคลอรี่ ≤ 0 |
| **`PlateCalculatorTest.kt`** | โหลดเป้า 100 kg / บาร์ 20 kg, กรณีเป้าเท่าบาร์, กรณีเป้าต่ำกว่าบาร์ |
| **`GlarmToExportTest.kt`** | `toJson` มี `exportVersion`, `username`, ฟิลด์ workout รวม `rpe`; `toCsv` มีหัวและแถว workout/nutrition/water |
| **`GlarmToRepositoryFeaturesTest.kt`** | `getPeriodTrainingStats`, `getTrainingStreakDays` (อย่างน้อย 1 วัน), export JSON/CSV, `insertWater` + `getWaterForDay`, `copyWorkoutsFromPreviousDay`, `copyNutritionFromPreviousDay` (ใช้ `RecordingFakeGlarmToDao` + `StaticFakeSessionManager`) |

รวมกับรายการเดิมในหมวด **「ชุด Unit / JVM tests」** ด้านบน — รันทั้งโมดูล: `./gradlew test` จากโฟลเดอร์ `GlarmTo`

---

**วันที่อัปเดต:** 13 เมษายน 2026


### 1. Haptic Feedback & Confetti Animation (ความสมบูรณ์แบบ / Polish)
*   **ไฟล์ที่เกี่ยวข้อง**: `build.gradle.kts`, `WorkoutScreen.kt`
*   **การอัปเดต**: 
    *   เพิ่ม Library `nl.dionsegijn:konfetti-compose`
    *   แทรก `LocalHapticFeedback` เมื่อมีการกดปุ่มสำคัญ เช่น ตั้งเวลาพัก หรือบันทึกเซต เพื่อลดปัญหาจืดชืด
    *   เพิ่มหน้าต่างพลุกระจาย (Confetti Particle System) หลังจากกดปุ่ม **"SAVE & FINISH"** (End Session) ให้ความรู้สึกฉลองความสำเร็จหลังซ้อมเสร็จ

### 2. Smart Coach AI-Progression (การแก้ปัญหาและการใช้งานจริง)
*   **ไฟล์ที่เกี่ยวข้อง**: `GlarmToDao.kt`, `GlarmToRepository.kt`, `WorkoutViewModel.kt`, `WorkoutScreen.kt`
*   **การอัปเดต**: 
    *   เพิ่ม Query `getLatestWorkoutByName` สำหรับหาประวัติการฝึกท่านั้นๆ ในอดีต
    *   ผูก Logic ใน ViewModel ให้อ่านค่า `RPE` (Rate of Perceived Exertion) ครั้งล่าสุด ถ้าน้อยแสดงว่ายกไหว ระบบจะคำนวณและแสดงข้อความ Suggestion แนะนำเพิ่มน้ำหนัก `+2.5 kg` แบบ Real-time ทันทีที่พิมพ์ชื่อท่าเสร็จ

### 3. Muscle Heatmap Activity (ความคิดสร้างสรรค์ & UI Design)
*   **ไฟล์ที่เกี่ยวข้อง**: `DashboardViewModel.kt`, `DashboardScreen.kt`
*   **การอัปเดต**:
    *   ออกแบบ Custom UI ดึงตารางกิจกรรมรายวัน 91 วันย้อนหลัง (13 สัปดาห์)
    *   วาด Box Grid เรียงแบบ Contribution Graph ของ GitHub โดยเช็คปริมาณ Sets รวมในแต่ละวัน (ซ้อมเบา=สีจาง, ซ้อมหนัก=สีแดดงเข้ม) ประดับตกแต่งหน้า Dashboard ให้ดูโปรเฟสชันแนลระดับบน

### 4. Jetpack Glance Home Widget (การเรียนรู้เทคโนโลยีใหม่ / Advanced Skill)
*   **ไฟล์ที่เกี่ยวข้อง**: `GlarmToWidget.kt`, `GlarmToWidgetReceiver.kt`, `glarmto_widget_initial.xml`, `glarmto_widget_info.xml`, `AndroidManifest.xml`
*   **การอัปเดต**:
    *   เพิ่ม Library `androidx.glance:glance-appwidget`
    *   สร้าง UI วิดเจ็ตขนาดเล็กที่สามารถวางบน Home Screen ของมือถือ Android ด้วยเทคโนโลยีค่ายใหม่ล่าสุด Glance (Declarative Widget UI ซึ่งใหม่มาก)
    *   ตั้งค่า Receiver และ AppWidgetProvider ใน AndroidManifest ให้ระบบดึง Widget ไปแสดงบนปลายนิ้วผู้ใช้ได้ตลอดเวลา

### 5. IG Story Social Export (สร้างสรรค์จนสมบูรณ์แบบ / Engagement)
*   **ไฟล์ที่เกี่ยวข้อง**: `DashboardViewModel.kt`, `DashboardScreen.kt`, `file_paths.xml`, `AndroidManifest.xml`
*   **การอัปเดต**:
    *   เขียนฟังก์ชัน `shareToInstagramStory` จัดเตรียมพื้นที่ Canvas และลงพู่กันวาดรูปภาพ Bitmap ขนาดแนวตั้ง (Story Size 1080x1920) แบบ On-The-Fly (วาดตอนกดปุ่ม)
    *   แนบข้อมูล `Level` และ `Streak (วัน)` เข้าไปในรูปภาพ แล้วเซฟใส่ Cache Folder อย่างรวดเร็ว
    *   กำหนด `FileProvider` เพื่อยิง Intent รูปนั้นออกนอกกรอบของแอป กระโดดสู่หน้า Share ข้ามไปยัง Story ใน IG หรือแอปโซเชียลอื่นๆ ได้ทันที


### 6. Picture-in-Picture (PiP) Rest Timer (สลับแอปก็ยังมีนาฬิกาลอยได้)
*   **ไฟล์ที่เกี่ยวข้อง**: `AndroidManifest.xml`, `MainActivity.kt`, `WorkoutScreen.kt`
*   **การอัปเดต**:
    *   ตั้งค่า `android:supportsPictureInPicture="true"` ในล่วงหน้า
    *   ดักจับอีเวนท์ระดับระบบ (OS) `onUserLeaveHint` (เมื่อผู้ใช้ย่อแอปหรือสลับไปแอปอื่น)
    *   เมื่ออยู่ในระหว่างนับเวลาพัก หน้าต่างแอปจะยุบลงมาเหลือแค่ตัวเลขจับเวลาพัก (สีดำล้วน + นาฬิกา) ซ้อนทับบนแอปอื่น เช่น IG หรือ YouTube เพื่อให้เห็นเวลาพักตลอดเวลา

###  7. 3D Radar Chart Analysis (กราฟประเมินกล้ามเนื้อ 5 แฉก)
*   **ไฟล์ที่เกี่ยวข้อง**: `DashboardViewModel.kt`, `DashboardScreen.kt`
*   **การอัปเดต**:
    *   นำประวัติการออกกำลังกาย 30 วันหลังสุด มาทำ **Regex / String Match** แยกคำศัพท์ชื่อท่า เพื่อลงตะกร้า 5 หมวด: Chest, Back, Legs, Arms, Shoulders อย่างอัตโนมัติ
    *   ใช้ `Android Compose Canvas` ร่างกราฟใยแมงมุม 5 แกน (Spider Web / Radar Chart) แบบ Custom Drawing เต็มระบบ พร้อมใส่เงาและความโปร่งแสงสุดเฉียบ
    *   ทำให้ผู้ใช้ประเมินสัดส่วนร่างตัวเองได้แบบ Real-time (ถ้าช่วงนี้เล่นแต่อก กราฟขากับหลังก็จะหดลงไป)

### 8. On-Device "Expert" Workout Generator (AI สร้างตารางฝึกออฟไลน์) 🎲🏋️
*   **ไฟล์ที่เกี่ยวข้อง**: `ExerciseLibrary.kt`, `WorkoutGenerator.kt`, `WorkoutScreen.kt`, `AiWorkoutGeneratorSheet.kt`
*   **การอัปเดต**:
    *   สร้างระบบ Rule-based AI Engine ภายในตัวเครื่อง (ไม่ต้องรอโหลดเน็ต) โดยการจับคู่ท่าทาง (Exercise Presets) นำมาผูกกับ Tag อุปกรณ์ และชิ้นส่วนกล้ามเนื้อ
    *   ปรับหน้าต่างเข้าสู่หน้าจอ Modal Bottom Sheet ให้ผู้ใช้เลื่อนกำหนดเวลาที่มี อุปกรณ์ที่มี และกล้ามเนื้อที่อยากโฟกัส
    *   Algorithm จะคำนวณหั่นเวลา แบ่งสัดส่วนพัก/ยก และสุ่มจัดท่าพร้อมจำนวนเซต/เรปให้โดยอัตโนมัติ 4-5 ท่า พร้อมเริ่มลุยได้ทันที!

### 9. Muscle Recovery & Fatigue Tracker (ระบบวิเคราะห์ความล้ากล้ามเนื้อ) 🩸🔋
*   **ไฟล์ที่เกี่ยวข้อง**: `RecoveryViewModel.kt`, `RecoveryScreen.kt`, `MainActivity.kt`
*   **การอัปเดต**:
    *   เพิ่มแท็บเมนูล่างอันใหม่ **Recovery** 
    *   สร้าง Fatigue Math Algorithm ที่แอบไปดึงข้อมูล Room Database 72 ชั่วโมงล่าสุดของตาราง `workout_log` มาคำนวณสะสมความล้า (Damage) และหักล้างกับค่าเวลาพักฟื้น (Time-Decay)
    *   โชว์ผลลัพธ์ผ่าน Custom Canvas Animation เป็นหลอดพลัง 0-100% จำแนกตามสัดส่วนกล้ามเนื้อ พร้อม AI Recommendation แนะนำว่าวันนี้ควรพักส่วนไหนและควรเล่นส่วนไหนที่ฟื้นตัวเต็มที่แล้ว

### 10. AI Camera & ML Kit "Wow Factors" (ฟีเจอร์พรีเมียมที่ทำเองฟรี 100%) 📷🧠
*   **ไฟล์ที่เกี่ยวข้อง**: `build.gradle.kts`, `AndroidManifest.xml`, `OpenFoodFactsApi.kt`, `NutritionOcrParser.kt`, `PoseAngleMath.kt`, `CameraScannerScreen.kt`, `AiPoseTrackerScreen.kt`, `WorkoutScreen.kt`, `NutritionScreen.kt`
*   **การอัปเดต**:
    *   **Smart Barcode Scanner:** ติดตั้ง `CameraX` คู่กับ `ML Kit Barcode Scanning` เมื่อแสกนบาร์โค้ดขนมปัง ระบบจะยิงหา `OpenFoodFacts API` และดึงข้อมูลแคลอรี่/โปรตีนมากรอกลง Database เราให้อัตโนมัติ!
    *   **Nutrition Label OCR:** ถ้าไม่มีบาร์โค้ด ให้เล็งกล้องไปที่ตารางโภชนาการหลังซอง `ML Kit Text Recognition` จะกวาดสายตาอ่านหนังสือทั้งหมด แล้วใช้สมการ Regex ดึงตัวเลข "แคลอรี่" ออกมากรอกใส่หน้าจดบันทึกให้เองแบบไม่ต้องพิมพ์
    *   **AI Form Pose Tracker (Squat Observer):** เปิดกล้องหน้าพร้อมเรียก `ML Kit Pose Detection` วาดเส้นก้างปลา (Skeleton Canvas) มาร์คจุดข้อต่อร่างกายแบบสดๆ เรียลไทม์! พร้อมคลาส `PoseAngleMath` ใช้คณิตศาสตร์เวกเตอร์จับมุมหัวเข่าและสะโพก เพื่อตะโกนบอกฟอร์มการจับ Squat เช่น "ย่อลงอีกนิด (Go Lower!)" หรือ "เยี่ยมมาก (Good Depth!)" เหมือนมี PT ส่วนตัวมานั่งมอง!

### 11. Full Authentication System Overhaul (ยกระดับระบบยืนยันตัวตนแบบเต็มรูปแบบ) 🔐
*   **ไฟล์ที่เกี่ยวข้อง**: `AppDatabase.kt`, `UserEntity.kt`, `GlarmToRepository.kt`, `WelcomeScreen.kt`, `RegisterScreen.kt`, `LoginScreen.kt`, `MainActivity.kt`
*   **การอัปเดต**:
    *   **Database Migration V12:** เพิ่มคอลัมน์ `password` ลงในตาราง `user_log` พร้อมเขียนสคริปต์ Migration ทำการล็อกผู้ใช้ให้มีรหัสผ่าน (แถมทำ Backward Compatibility ให้ลูกค้ารุ่นแรกที่ไม่มีรหัสผ่าน ยังเล่นได้ปกติ)
    *   **แยกหน้าจอ (Flow Separation):** ลบระบบ Auto-Login แบบทำส่งลวกๆ ทิ้งไป แล้วแยกเป็น 3 หน้าจอแบบสายยิมดุดัน (ดำ-แดงเลือดนก) ได้แก่ `WelcomeScreen` (หน้าต้อนรับ), `RegisterScreen` (หน้าสมัครใหม่), และ `LoginScreen`
    *   **Security Validation:** ในหน้า `RegisterScreen` มีระบบตัวกรองบังคับให้ผู้ใช้กรอกรหัสผ่าน (Password & Confirm) 2 ช่องให้ตรงกัน หากไม่ตรงจะขึ้นข้อความเตอืน และใช้ปุ่มลูกเล่นซ่อนรหัสผ่านเป็นจุดไข่ปลา
    *   **Persistent Session & Navigation:** ปรับแกนการนำทาง (Navigation Graph) ใหม่ เมื่อล็อกอินสำเร็จ ระบบจะฝังวิญญาณผู้ใช้ลง `SharedPreferences` ทำให้ปิดเปิดแอปใหม่ได้โดยไม่ต้องล็อกอินซ้ำ จนกว่าจะจงใจกดปุ่ม  `Log Out` ให้ดีดกลับมาที่หน้า `WelcomeScreen`
    *   **Rock-Solid Unit Testing:** ได้ทำการเพิ่ม Test Cases จำนวน 5 เควสลงฝั่ง Repository เพื่อทดสอบการเช็ค Password ถูก/ผิด ให้มั่นใจ 100% ว่าการจัดเก็บข้อมูลลูกค้าปลอดภัยสุดๆ

### 12. Hands-free Voice Logger (ระบบ AI จดข้อความด้วยเสียง) 🎙️⚡
*   **ไฟล์ที่เกี่ยวข้อง**: `WorkoutScreen.kt`
*   **การอัปเดต**:
    *   **Native Speech Recognition:** เชื่อมต่อระบบแปลงเสียงกับ `RecognizerIntent` ของ Android OS ที่ดึงเอนจิ้นแปลงเสียงอันทรงพลังของ Google มาใช้ ข้อดีคือแอปเราไม่ต้องฝังโค้ดขออนุญาต (Permission) เข้าถึงไมโครโฟนให้ผู้ใช้อึดอัดใจเลย!
    *   **Advanced Regex Parser:** สร้างชุดคำสั่งตัวกรองข้อความ `parseVoiceCommand` เพื่อแกะประโยคคำพูด (รองรับได้ทั้งภาษาไทยและอังกฤษ) ให้หั่นออกมาเป็น [ชื่อท่า], [น้ำหนัก], และ [จำนวนครั้ง] ให้ตรงล็อค
    *   **Auto-Save & Magic Workflow:** เอาใจคนเล่นยิมมือเลอะเหงื่อ แค่กดไมค์ 🎤 แล้วตะโกน "Squat หนึ่งร้อยกิโล แปดครั้ง" ระบบจะแกะความหมาย เอาไปใส่ในช่อง และกดเซฟเซตให้โดยอัตโนมัติ พร้อมสั่นเตือน Haptic ลงมือถือให้รู้ว่าจดเสร็จแล้ว! อย่างเท่!

---

**วันที่อัปเดต:** 14 เมษายน 2026

## 🚀 สรุปงานที่ทำในวันนี้ (Phase 5: Visual Excellence & UX Consolidation)

วันนี้เราได้ยกระดับความสวยงามและประสบการณ์การใช้งาน (UX) ของแอปขึ้นไปอีกขั้น โดยเน้นไปที่ความพรีเมียมของดีไซน์ และการรวบรวมฟีเจอร์ให้ใช้งานง่ายขึ้น ลดความซับซ้อนของเมนูครับ

### 1. ระบบดูประวัติรายเดือน (Monthly History View)
*   **Monthly Data Grouping:** พัฒนาระบบจัดกลุ่มข้อมูลรายเดือนในหน้า History โดยจะรวบรวม Workout Sessions และ Nutrition ของทั้งเดือนมาแสดงผลแยกตามวันที่อย่างสวยงาม
*   **Dual View Toggle:** เพิ่มเมนู Dropdown ที่หน้า Dashboard ให้ผู้ใช้เลือกได้ว่าจะดูประวัติแบบ "รายวัน" หรือ "รายเดือน"
*   **ไฟล์ที่เกี่ยวข้อง:** `HistoryScreen.kt`, `HistoryViewModel.kt`, `GlarmToRepository.kt`

### 2. ระบบตีมไดนามิก (Dynamic Themes System)
*   **Theme Manager:** สร้างระบบจำตีมที่ผู้ใช้เลือก (Persistence) ทำให้เบลนด์เข้ากับบุคลิกของผู้ใช้ได้ 5 รูปแบบ:
    *   **Classic GlarmTo:** แดด-ดำ ดุดัน
    *   **Ocean Flow:** น้ำเงิน-เทล สบายตา (Contrast Optimized)
    *   **Neon Energy:** เขียวนีออน Cyberpunk
    *   **Forest Ground:** เขียวมะกอกแนวธรรมชาติ
*   **ไฟล์ที่เกี่ยวข้อง:** `ThemeManager.kt`, `Theme.kt`, `Color.kt`, `DashboardScreen.kt`

### 3. Aura 120Hz & Full Glassmorphism (Wow Factor!) ✨
*   **High-Refresh Animation:** สร้างพื้นหลังแบบเคลื่อนไหว (Animated Canvas) สำหรับตีมพิเศษ "Aura 120Hz" ที่มีการไล่สีล่วงหน้าและขยับเป็นจังหวะชีพจร (Pulsing) อย่างไหลลื่น
*   **Transparent UI Layer:** ปรับทุกหน้าจอให้เป็นแบบโปร่งแสง (Glassmorphism) เมื่อใช้ตีม Aura ทำให้เห็นแอนิเมชั่นวิ่งอยู่ข้างหลังตลอดเวลาทุกเมนู
*   **ไฟล์ที่เกี่ยวข้อง:** `AnimatedAuraBackground.kt`, `MainActivity.kt`

### 4. การปรับโครงสร้างเมนู (UX Consolidation)
*   **Recovery Migration:** ย้ายฟีเจอร์ Muscle Recovery จากเมนูหลักด้านล่าง (Navbar) มาไว้ในหน้า Dashboard แทน โดยออกแบบให้เป็นส่วนที่พับเก็บได้ (Expandable Section) เพื่อลดความรกรุงรังของเมนู
*   **ไฟล์ที่เกี่ยวข้อง:** `DashboardScreen.kt`, `MainActivity.kt`

### 5. แก้วน้ำแอนิเมชั่น (Animated Water Glass) 🌊
*   **Interactive Visualization:** เปลี่ยนหลอด Progress Bar น้ำเดิมๆ ให้กลายเป็น "แก้วน้ำจำลอง" ที่วาดด้วย Canvas
*   **Smooth Fluid Motion:** เมื่อกดเติมน้ำ ระดับน้ำจะค่อยๆ เพิ่มขึ้น (fill animation) พร้อมมีระลอกคลื่น (Wave Effect) ขยับไปมาอย่างสมจริง เพิ่มความน่าดึงดูดในการดื่มน้ำ
*   **ไฟล์ที่เกี่ยวข้อง:** `AnimatedWaterGlass.kt`, `NutritionScreen.kt`

### 6. การปรับปรุงความถูกต้องของข้อมูล (Bug Fixes & Hardening)
*   **Color Contrast Optimization:** ปรับแก้สัดส่วนสีในทุกตีม โดยเฉพาะ Ocean Flow เพื่อให้ตัวอักษรบนการ์ดอ่านง่ายขึ้น ไม่กลืนกับพื้นหลัง
*   **Syntax Error Resolution:** เคลียร์ปัญหา Parenthesis/Bracket nesting ในหน้า History หลังจากการ refactor ครั้งใหญ่
