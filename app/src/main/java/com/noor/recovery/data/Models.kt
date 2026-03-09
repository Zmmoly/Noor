package com.noor.recovery.data

data class AddictionType(
    val id: String,
    val nameAr: String,
    val emoji: String
)

data class Milestone(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val emoji: String,
    val requiredHours: Long
)

data class RecoverySession(
    val addictionTypeId: String,
    val startTimeMs: Long
)

object AddictionTypes {
    val all = listOf(
        AddictionType("smoking",    "التدخين",         "🚬"),
        AddictionType("alcohol",    "الكحول",          "🍷"),
        AddictionType("gaming",     "إدمان الألعاب",   "🎮"),
        AddictionType("social",     "السوشيال ميديا",  "📱"),
        AddictionType("drugs",      "المخدرات",        "💊"),
        AddictionType("other",      "أخرى",            "🔗")
    )
}

object Milestones {
    val all = listOf(
        Milestone("1h",  "الساعة الأولى",   "ساعة واحدة",             "⚡", 1),
        Milestone("1d",  "اليوم الأول",     "٢٤ ساعة كاملة",         "🌅", 24),
        Milestone("3d",  "ثلاثة أيام",      "جسمك يبدأ التعافي",     "🌟", 72),
        Milestone("1w",  "أسبوع كامل",      "٧ أيام من الإرادة",     "🌙", 168),
        Milestone("1m",  "شهر واحد",        "٣٠ يوماً مباركاً",      "💎", 720),
        Milestone("3m",  "ثلاثة أشهر",      "عادة جديدة تتشكل",     "🦅", 2160),
        Milestone("1y",  "سنة كاملة",       "أنت أسطورة التعافي",    "👑", 8760)
    )
}

object Quotes {
    val all = listOf(
        Pair("كل لحظة صبر هي خطوة نحو نفسٍ أكثر حرية وكرامة.", "رحلة نور"),
        Pair("الإرادة ليست غياب الرغبة، بل القدرة على تجاوزها.", "ابن سينا"),
        Pair("أصعب المعارك هي تلك التي نخوضها مع أنفسنا، وأعظمها انتصاراً.", "حكمة عربية"),
        Pair("كن لطيفاً مع نفسك؛ أنت تخوض معركة يجهل أمرها كثيرون.", "رحلة نور"),
        Pair("التعافي لا يعني النسيان، بل تعلّم العيش بحرية.", "رحلة نور"),
        Pair("كل يوم جديد هو فرصة لتكون أقوى من أمس.", "رحلة نور"),
        Pair("الشجاعة الحقيقية هي الاعتراف بالمشكلة والعمل على حلها.", "حكمة عربية")
    )
    fun random() = all.random()
}
