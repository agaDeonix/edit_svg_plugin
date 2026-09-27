package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VectorOptimizerTest {
    @Test
    fun `removes zero length line`() {
        val pd = PathDataParser.parse("M0 0 L0 0 L5 5")
        val opt = VectorOptimizer.optimizePathData(pd)
        assertEquals(2, opt.commands.size)
    }
}
