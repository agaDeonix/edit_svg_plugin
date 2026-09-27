package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.geom.PathIterator

class PathGeometryTest {
    @Test
    fun `smooth cubic reflects previous control point`() {
        val path = PathDataParser.parse("M0 0 C1 0 2 1 3 1 S5 2 6 1").toPath2D()
        val coords = DoubleArray(6)
        val iterator = path.getPathIterator(null)
        iterator.next()
        iterator.next()

        assertEquals(PathIterator.SEG_CUBICTO, iterator.currentSegment(coords))
        assertEquals(4.0, coords[0], 1e-9)
        assertEquals(1.0, coords[1], 1e-9)
    }

    @Test
    fun `arc reaches its endpoint and has curvature`() {
        val bounds = PathDataParser.parse("M0 0 A10 10 0 0 1 20 0").toPath2D().bounds2D

        assertEquals(20.0, bounds.maxX, 1e-6)
        assertTrue(bounds.height > 9.9)
    }
}
