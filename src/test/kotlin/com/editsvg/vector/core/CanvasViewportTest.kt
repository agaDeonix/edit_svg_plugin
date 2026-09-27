package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CanvasViewportTest {
    @Test
    fun `fit centers world viewport in canvas`() {
        val viewport = CanvasViewport.fit(200.0, 100.0, 500.0, 400.0, 20.0)

        assertEquals(2.3, viewport.scale, 1e-9)
        assertEquals(20.0, viewport.worldToScreenX(0.0), 1e-9)
        assertEquals(85.0, viewport.worldToScreenY(0.0), 1e-9)
    }

    @Test
    fun `zoom keeps world point beneath pointer`() {
        val before = CanvasViewport(2.0, 30.0, 40.0)
        val wx = before.screenToWorldX(130.0)
        val wy = before.screenToWorldY(140.0)
        val after = before.zoomAt(2.0, 130.0, 140.0)

        assertEquals(wx, after.screenToWorldX(130.0), 1e-9)
        assertEquals(wy, after.screenToWorldY(140.0), 1e-9)
    }

    @Test
    fun `zoom is bounded`() {
        assertEquals(CanvasViewport.MAX_SCALE, CanvasViewport().zoomAt(1e9, 0.0, 0.0).scale)
        assertEquals(CanvasViewport.MIN_SCALE, CanvasViewport().zoomAt(1e-9, 0.0, 0.0).scale)
    }
}
