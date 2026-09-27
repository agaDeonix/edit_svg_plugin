package com.editsvg.vector.core

data class CanvasViewport(
    val scale: Double = 1.0,
    val originX: Double = 0.0,
    val originY: Double = 0.0,
) {
    fun worldToScreenX(value: Double): Double = value * scale + originX
    fun worldToScreenY(value: Double): Double = value * scale + originY
    fun screenToWorldX(value: Double): Double = (value - originX) / scale
    fun screenToWorldY(value: Double): Double = (value - originY) / scale

    fun zoomAt(factor: Double, screenX: Double, screenY: Double): CanvasViewport {
        val worldX = screenToWorldX(screenX)
        val worldY = screenToWorldY(screenY)
        val nextScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        return CanvasViewport(nextScale, screenX - worldX * nextScale, screenY - worldY * nextScale)
    }

    fun pan(dx: Double, dy: Double): CanvasViewport = copy(originX = originX + dx, originY = originY + dy)

    companion object {
        const val MIN_SCALE = 0.05
        const val MAX_SCALE = 256.0

        fun fit(worldWidth: Double, worldHeight: Double, canvasWidth: Double, canvasHeight: Double, margin: Double): CanvasViewport {
            val availableWidth = (canvasWidth - 2 * margin).coerceAtLeast(1.0)
            val availableHeight = (canvasHeight - 2 * margin).coerceAtLeast(1.0)
            val scale = minOf(availableWidth / worldWidth.coerceAtLeast(1.0), availableHeight / worldHeight.coerceAtLeast(1.0))
                .coerceIn(MIN_SCALE, MAX_SCALE)
            return CanvasViewport(
                scale,
                (canvasWidth - worldWidth * scale) / 2.0,
                (canvasHeight - worldHeight * scale) / 2.0,
            )
        }
    }
}
