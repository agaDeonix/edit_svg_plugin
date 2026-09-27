package com.editsvg.vector.core

object CanvasInteraction {
    fun hitHandle(
        handles: List<GraphHandle>,
        viewport: CanvasViewport,
        screenX: Double,
        screenY: Double,
        radiusPixels: Double,
    ): GraphHandle? {
        val radiusSquared = radiusPixels * radiusPixels
        return handles.withIndex()
            .map { indexed ->
                val dx = viewport.worldToScreenX(indexed.value.x) - screenX
                val dy = viewport.worldToScreenY(indexed.value.y) - screenY
                Triple(indexed.value, dx * dx + dy * dy, indexed.index)
            }
            .filter { it.second <= radiusSquared }
            .minWithOrNull(compareBy<Triple<GraphHandle, Double, Int>> { it.second }.thenBy { it.third })
            ?.first
    }
}
