package com.editsvg.vector.core

import java.awt.geom.Path2D
import kotlin.math.*

fun PathData.toPath2D(): Path2D.Double {
    val p = Path2D.Double()
    var penX = 0.0
    var penY = 0.0
    var subStartX = 0.0
    var subStartY = 0.0
    var lastCubicX2: Double? = null
    var lastCubicY2: Double? = null
    var lastQuadX1: Double? = null
    var lastQuadY1: Double? = null

    for (cmd in commands) {
        val previousX = penX
        val previousY = penY
        when (cmd) {
            is PathCommand.MoveTo -> {
                p.moveTo(cmd.x, cmd.y)
                penX = cmd.x
                penY = cmd.y
                subStartX = penX
                subStartY = penY
            }

            is PathCommand.LineTo -> {
                p.lineTo(cmd.x, cmd.y)
                penX = cmd.x
                penY = cmd.y
            }

            is PathCommand.HorizontalTo -> {
                p.lineTo(cmd.x, penY)
                penX = cmd.x
            }

            is PathCommand.VerticalTo -> {
                p.lineTo(penX, cmd.y)
                penY = cmd.y
            }

            is PathCommand.CubicTo -> {
                p.curveTo(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)
                penX = cmd.x
                penY = cmd.y
                lastCubicX2 = cmd.x2
                lastCubicY2 = cmd.y2
            }

            is PathCommand.QuadTo -> {
                p.quadTo(cmd.x1, cmd.y1, cmd.x, cmd.y)
                penX = cmd.x
                penY = cmd.y
                lastQuadX1 = cmd.x1
                lastQuadY1 = cmd.y1
            }

            is PathCommand.SmoothCubicTo -> {
                val x1 = lastCubicX2?.let { 2 * penX - it } ?: penX
                val y1 = lastCubicY2?.let { 2 * penY - it } ?: penY
                p.curveTo(x1, y1, cmd.x2, cmd.y2, cmd.x, cmd.y)
                penX = cmd.x; penY = cmd.y
                lastCubicX2 = cmd.x2; lastCubicY2 = cmd.y2
            }

            is PathCommand.SmoothQuadTo -> {
                val x1 = lastQuadX1?.let { 2 * penX - it } ?: penX
                val y1 = lastQuadY1?.let { 2 * penY - it } ?: penY
                p.quadTo(x1, y1, cmd.x, cmd.y)
                penX = cmd.x; penY = cmd.y
                lastQuadX1 = x1; lastQuadY1 = y1
            }

            is PathCommand.ArcTo -> {
                appendArc(p, penX, penY, cmd)
                penX = cmd.x; penY = cmd.y
            }

            is PathCommand.ClosePath -> {
                p.closePath()
                penX = subStartX
                penY = subStartY
            }
        }
        if (cmd !is PathCommand.CubicTo && cmd !is PathCommand.SmoothCubicTo) {
            lastCubicX2 = null; lastCubicY2 = null
        }
        if (cmd !is PathCommand.QuadTo && cmd !is PathCommand.SmoothQuadTo) {
            lastQuadX1 = null; lastQuadY1 = null
        }
    }
    return p
}

private fun appendArc(path: Path2D.Double, x0: Double, y0: Double, arc: PathCommand.ArcTo) {
    var rx = abs(arc.rx)
    var ry = abs(arc.ry)
    if (rx == 0.0 || ry == 0.0 || (x0 == arc.x && y0 == arc.y)) {
        path.lineTo(arc.x, arc.y)
        return
    }
    val phi = Math.toRadians(arc.rotation % 360.0)
    val cosPhi = cos(phi)
    val sinPhi = sin(phi)
    val dx = (x0 - arc.x) / 2.0
    val dy = (y0 - arc.y) / 2.0
    val x1p = cosPhi * dx + sinPhi * dy
    val y1p = -sinPhi * dx + cosPhi * dy
    val lambda = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry)
    if (lambda > 1.0) {
        val scale = sqrt(lambda)
        rx *= scale; ry *= scale
    }
    val sign = if (arc.largeArc == arc.sweep) -1.0 else 1.0
    val numerator = max(0.0, rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p)
    val denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p
    val coefficient = if (denominator == 0.0) 0.0 else sign * sqrt(numerator / denominator)
    val cxp = coefficient * (rx * y1p / ry)
    val cyp = coefficient * (-ry * x1p / rx)
    val cx = cosPhi * cxp - sinPhi * cyp + (x0 + arc.x) / 2.0
    val cy = sinPhi * cxp + cosPhi * cyp + (y0 + arc.y) / 2.0
    fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double =
        atan2(ux * vy - uy * vx, ux * vx + uy * vy)
    val ux = (x1p - cxp) / rx
    val uy = (y1p - cyp) / ry
    val vx = (-x1p - cxp) / rx
    val vy = (-y1p - cyp) / ry
    var theta = atan2(uy, ux)
    var delta = angle(ux, uy, vx, vy)
    if (!arc.sweep && delta > 0) delta -= 2 * PI
    if (arc.sweep && delta < 0) delta += 2 * PI
    val segments = ceil(abs(delta) / (PI / 2.0)).toInt().coerceAtLeast(1)
    val step = delta / segments
    repeat(segments) {
        val next = theta + step
        val alpha = 4.0 / 3.0 * tan((next - theta) / 4.0)
        fun point(t: Double): Pair<Double, Double> =
            Pair(cx + rx * cosPhi * cos(t) - ry * sinPhi * sin(t), cy + rx * sinPhi * cos(t) + ry * cosPhi * sin(t))
        val start = point(theta)
        val end = point(next)
        val dxStart = -rx * cosPhi * sin(theta) - ry * sinPhi * cos(theta)
        val dyStart = -rx * sinPhi * sin(theta) + ry * cosPhi * cos(theta)
        val dxEnd = -rx * cosPhi * sin(next) - ry * sinPhi * cos(next)
        val dyEnd = -rx * sinPhi * sin(next) + ry * cosPhi * cos(next)
        path.curveTo(start.first + alpha * dxStart, start.second + alpha * dyStart,
            end.first - alpha * dxEnd, end.second - alpha * dyEnd, end.first, end.second)
        theta = next
    }
}
