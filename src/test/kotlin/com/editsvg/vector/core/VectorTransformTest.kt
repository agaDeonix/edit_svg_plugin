package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

class VectorTransformTest {
    @Test
    fun `android group rotates and scales around pivot before translation`() {
        val transform = VectorTransforms.group(rotation = 90.0, pivotX = 1.0, pivotY = 1.0, scaleX = 2.0, scaleY = 1.0, translateX = 3.0, translateY = 4.0)
        val point = transform.transform(Point2D.Double(2.0, 1.0), null)

        assertEquals(4.0, point.x, 1e-9)
        assertEquals(7.0, point.y, 1e-9)
    }

    @Test
    fun `child transform is applied before parent transform`() {
        val parent = VectorTransforms.group(translateX = 10.0)
        val child = VectorTransforms.group(scaleX = 2.0, scaleY = 2.0)
        val combined = VectorTransforms.combine(parent, child)
        val point = combined.transform(Point2D.Double(3.0, 0.0), null)

        assertEquals(16.0, point.x, 1e-9)
    }
}
