package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CanvasInteractionTest {
    private val first = GraphHandle(0, HandleKind.MoveEnd, 10.0, 10.0)
    private val second = GraphHandle(1, HandleKind.LineEnd, 10.2, 10.0)

    @Test
    fun `hit radius stays in screen pixels`() {
        val viewport = CanvasViewport(10.0, 0.0, 0.0)

        assertEquals(first, CanvasInteraction.hitHandle(listOf(first), viewport, 106.0, 100.0, 7.0))
        assertNull(CanvasInteraction.hitHandle(listOf(first), viewport, 108.0, 100.0, 7.0))
    }

    @Test
    fun `ties select earlier handle deterministically`() {
        val viewport = CanvasViewport(10.0, 0.0, 0.0)

        assertEquals(first, CanvasInteraction.hitHandle(listOf(first, second), viewport, 101.0, 100.0, 7.0))
    }
}
