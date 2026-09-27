package com.editsvg.vector.editor

import com.editsvg.vector.core.PathDataParser

object InspectorValues {
    fun optionalAttribute(value: String): String? = value.trim().ifEmpty { null }
    fun isPositiveNumber(value: String): Boolean = value.toDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true
    fun isNonNegativeNumber(value: String): Boolean = value.isBlank() || value.toDoubleOrNull()?.let { it.isFinite() && it >= 0.0 } == true

    fun attributeError(name: String, rawValue: String): String? {
        val value = rawValue.trim()
        return when (name) {
            "viewportWidth", "viewportHeight" -> if (isPositiveNumber(value)) null else "Value must be greater than zero"
            "strokeWidth", "strokeMiterLimit" -> if (isNonNegativeNumber(value)) null else "Value must be zero or greater"
            "fillAlpha", "strokeAlpha", "alpha" -> {
                if (value.isEmpty() || value.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..1.0 } == true) null else "Alpha must be between 0 and 1"
            }
            "rotation", "pivotX", "pivotY", "scaleX", "scaleY", "translateX", "translateY", "trimPathStart", "trimPathEnd", "trimPathOffset" -> {
                if (value.isEmpty() || value.toDoubleOrNull()?.isFinite() == true) null else "Enter a valid number"
            }
            "fillColor", "strokeColor", "tint" -> if (validColor(value)) null else "Enter a hex color or Android resource"
            "pathData" -> try {
                if (value.isEmpty() || Regex("[MmLlHhVvCcSsQqTtAa]\\s*$").containsMatchIn(value)) "Invalid pathData"
                else { PathDataParser.parse(value); null }
            } catch (_: IllegalArgumentException) { "Invalid pathData" }
            "fillType" -> if (value.isEmpty() || value == "nonZero" || value == "evenOdd") null else "Use nonZero or evenOdd"
            "strokeLineCap" -> if (value.isEmpty() || value in setOf("butt", "round", "square")) null else "Use butt, round or square"
            "strokeLineJoin" -> if (value.isEmpty() || value in setOf("miter", "round", "bevel")) null else "Use miter, round or bevel"
            "autoMirrored" -> if (value.isEmpty() || value == "true" || value == "false") null else "Use true or false"
            else -> null
        }
    }

    private fun validColor(value: String): Boolean {
        if (value.isEmpty() || value.startsWith("@") || value.startsWith("?")) return true
        if (!value.startsWith("#")) return false
        val hex = value.drop(1)
        return hex.length in setOf(3, 4, 6, 8) && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}

data class InspectorDraft(
    val fillColor: String,
    val strokeColor: String,
    val strokeWidth: String,
    val viewportWidth: String,
    val viewportHeight: String,
) {
    fun hasChangesFrom(baseline: InspectorDraft): Boolean = this != baseline

    fun validationError(): String? = when {
        !InspectorValues.isNonNegativeNumber(strokeWidth) -> "Stroke width must be zero or greater"
        !InspectorValues.isPositiveNumber(viewportWidth) -> "Viewport width must be greater than zero"
        !InspectorValues.isPositiveNumber(viewportHeight) -> "Viewport height must be greater than zero"
        else -> null
    }
}
