package com.editsvg.vector.core

/**
 * Parsed Android/SVG pathData as a list of absolute-space commands (uppercase semantics).
 */
data class PathData(val commands: List<PathCommand>) {
    fun isEmpty(): Boolean = commands.isEmpty()
}

sealed class PathCommand {
    abstract fun appendTo(sb: StringBuilder)

    data class MoveTo(val x: Double, val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('M').append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class LineTo(val x: Double, val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('L').append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class HorizontalTo(val x: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('H').append(formatNum(x))
        }
    }

    data class VerticalTo(val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('V').append(formatNum(y))
        }
    }

    data class CubicTo(
        val x1: Double,
        val y1: Double,
        val x2: Double,
        val y2: Double,
        val x: Double,
        val y: Double,
    ) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('C')
                .append(formatNum(x1)).append(',').append(formatNum(y1)).append(' ')
                .append(formatNum(x2)).append(',').append(formatNum(y2)).append(' ')
                .append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class QuadTo(val x1: Double, val y1: Double, val x: Double, val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('Q')
                .append(formatNum(x1)).append(',').append(formatNum(y1)).append(' ')
                .append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class SmoothCubicTo(val x2: Double, val y2: Double, val x: Double, val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('S')
                .append(formatNum(x2)).append(',').append(formatNum(y2)).append(' ')
                .append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class SmoothQuadTo(val x: Double, val y: Double) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('T').append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class ArcTo(
        val rx: Double,
        val ry: Double,
        val rotation: Double,
        val largeArc: Boolean,
        val sweep: Boolean,
        val x: Double,
        val y: Double,
    ) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('A')
                .append(formatNum(rx)).append(',').append(formatNum(ry)).append(' ')
                .append(formatNum(rotation)).append(' ')
                .append(if (largeArc) '1' else '0').append(',')
                .append(if (sweep) '1' else '0').append(' ')
                .append(formatNum(x)).append(',').append(formatNum(y))
        }
    }

    data class ClosePath(val dummy: Unit = Unit) : PathCommand() {
        override fun appendTo(sb: StringBuilder) {
            sb.append('Z')
        }
    }
}

fun PathData.serialize(): String {
    val sb = StringBuilder()
    for ((i, cmd) in commands.withIndex()) {
        if (i > 0) sb.append(' ')
        cmd.appendTo(sb)
    }
    return sb.toString()
}

internal fun formatNum(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "0"
    val r = kotlin.math.round(v * 1_000_000.0) / 1_000_000.0
    if (kotlin.math.abs(r - r.toLong()) < 1e-9) return r.toLong().toString()
    val s = String.format(java.util.Locale.US, "%.6f", r).trimEnd('0').trimEnd('.')
    return if (s == "-0") "0" else s
}
