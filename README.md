# نور — تطبيق الإقلاع عن الإدمان 🌟

## وصف التطبيق
تطبيق أندرويد متكامل مكتوب بلغة **Kotlin** يساعد المستخدمين على الإقلاع عن الإدمان بشكل تدريجي وصحي.

---

## المميزات
- 🎯 **اختيار نوع الإدمان** — تدخين، كحول، ألعاب، سوشيال ميديا، مخدرات
- ⏱️ **عداد دقيق** — أيام / ساعات / دقائق / ثواني مع حلقة تقدم دائرية
- 🏅 **٧ محطات إنجاز** تتفعّل تلقائياً (ساعة ← سنة كاملة)
- 🌊 **نافذة تجاوز الرغبة** — تمارين تنفس تأملي
- 💡 **١٠ نصائح عملية** للصمود
- 💬 **اقتباسات ملهمة** عربية
- 🔔 **إشعارات تشجيعية** يومية (الساعة 9 صباحاً)
- 💾 **حفظ تلقائي** بـ DataStore (يحتفظ بالتقدم بعد إغلاق التطبيق)

---

## البنية التقنية

```
Architecture : MVVM
Language     : Kotlin 1.9
Min SDK      : 26 (Android 8)
Target SDK   : 34 (Android 14)

المكتبات المستخدمة:
- AndroidX Core KTX
- Material Design 3
- ViewModel + LiveFlow
- DataStore Preferences
- Kotlin Coroutines
- Gson
```

---

## هيكل المشروع

```
app/
├── data/
│   ├── Models.kt          ← نماذج البيانات + الاقتباسات + المحطات
│   └── RecoveryRepository.kt ← حفظ الجلسة بـ DataStore
├── viewmodel/
│   └── MainViewModel.kt   ← منطق العداد + المحطات
├── ui/
│   ├── SplashActivity.kt
│   ├── SetupActivity.kt
│   ├── MainActivity.kt
│   ├── HomeFragment.kt
│   ├── MilestonesFragment.kt
│   ├── TipsFragment.kt
│   ├── CravingBottomSheet.kt
│   └── adapter/
│       ├── MilestoneAdapter.kt
│       └── TipAdapter.kt
└── notification/
    ├── Receivers.kt
    └── NotificationHelper.kt
```

---

## كيفية تشغيل المشروع

### المتطلبات
- Android Studio Hedgehog أو أحدث
- JDK 17+
- Android SDK 34

### الخطوات
1. افتح Android Studio
2. اختر **Open** وافتح مجلد `NoorApp`
3. انتظر حتى ينتهي Gradle Sync
4. اضغط **Run** ▶️

---

## لقطات الشاشة (Screens)

| شاشة البداية | الشاشة الرئيسية | الإنجازات |
|---|---|---|
| اختيار نوع الإدمان | عداد الأيام + حلقة | ٧ مراحل تفعيل |

---

## الترخيص
مشروع مفتوح المصدر — استخدمه وطوّره بحرية ✨
