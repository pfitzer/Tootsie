package com.pfitzer.tootsie.domain.model

/**
 * Text size options for accessibility customization.
 * Each option has a scale factor that will be applied to typography.
 */
enum class TextSize(val scaleFactor: Float, val displayName: String) {
    SMALL(0.85f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.15f, "Large"),
    EXTRA_LARGE(1.3f, "Extra Large");

    companion object {
        fun fromString(value: String): TextSize {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
