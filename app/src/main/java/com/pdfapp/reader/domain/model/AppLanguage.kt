package com.pdfapp.reader.domain.model

/** Supported app UI languages. */
enum class AppLanguage(
    val displayName: String,
    val nativeName: String,
    val emojiFlag: String,
    val localeCode: String
) {
    EN("English", "English", "🇺🇸", "en"),
    VI("Vietnamese", "Tiếng Việt", "🇻🇳", "vi"),
    ES("Spanish", "Español", "🇪🇸", "es"),
    PT("Portuguese", "Português", "🇧🇷", "pt"),
    KO("Korean", "한국어", "🇰🇷", "ko"),
    FR("French", "Français", "🇫🇷", "fr"),
    DE("German", "Deutsch", "🇩🇪", "de"),
    ID("Indonesian", "Bahasa Indonesia", "🇮🇩", "id");

    companion object {
        fun fromLocaleCode(code: String): AppLanguage =
            entries.firstOrNull { it.localeCode == code } ?: EN
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM;
    companion object {
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() } ?: SYSTEM
    }
}

enum class PageMode { CONTINUOUS, SINGLE;
    companion object {
        fun fromString(value: String): PageMode =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() } ?: CONTINUOUS
    }
}

enum class SortOption {
    NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST
}

enum class ViewMode { LIST, GRID }
