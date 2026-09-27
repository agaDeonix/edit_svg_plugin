package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D

class CanvasRenderingTest {
    @Test
    fun `world transform preserves component graphics offset`() {
        val componentTransform = AffineTransform.getTranslateInstance(200.0, 100.0)
        val viewport = CanvasViewport(scale = 2.0, originX = 30.0, originY = 40.0)
        val result = CanvasRendering.worldTransform(componentTransform, viewport)
            .transform(Point2D.Double(5.0, 6.0), null)

        assertEquals(240.0, result.x, 1e-9)
        assertEquals(152.0, result.y, 1e-9)
    }
}
