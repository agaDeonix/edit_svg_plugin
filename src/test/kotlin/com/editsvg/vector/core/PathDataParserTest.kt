package com.editsvg.vector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathDataParserTest {
    @Test
    fun `parses simple MLZ`() {
        val pd = PathDataParser.parse("M0,0 L10,0 L10,10 Z")
        assertEquals(4, pd.commands.size)
        assertTrue(pd.commands[0] is PathCommand.MoveTo)
        assertTrue(pd.commands[1] is PathCommand.LineTo)
    }

    @Test
    fun `implicit line after M`() {
        val pd = PathDataParser.parse("M0 0 10 0 10 10 Z")
        assertEquals(4, pd.commands.size)
    }

    @Test
    fun `cubic absolute`() {
        val pd = PathDataParser.parse("M0 0 C0 10 10 10 10 0")
        assertEquals(2, pd.commands.size)
        val c = pd.commands[1] as PathCommand.CubicTo
        assertEquals(10.0, c.x, 1e-9)
    }

    @Test
    fun `round trip serialize`() {
        val original = "M1,2 L3,4 Z"
        val pd = PathDataParser.parse(original)
        val again = PathDataParser.parse(PathData(pd.commands).serialize())
        assertEquals(pd.commands.size, again.commands.size)
    }

    @Test
    fun `parses smooth cubic and quad commands`() {
        val pd = PathDataParser.parse("M0 0 C2 0 4 2 6 2 S10 4 12 2 Q14 0 16 2 T20 2")

        assertTrue(pd.commands[2] is PathCommand.SmoothCubicTo)
        assertTrue(pd.commands[4] is PathCommand.SmoothQuadTo)
    }

    @Test
    fun `parses relative arc and repeated argument groups`() {
        val pd = PathDataParser.parse("M1 2 a3 4 45 0 1 6 7 3 4 0 1 0 2-5")

        val first = pd.commands[1] as PathCommand.ArcTo
        val second = pd.commands[2] as PathCommand.ArcTo
        assertEquals(7.0, first.x, 1e-9)
        assertEquals(9.0, first.y, 1e-9)
        assertEquals(9.0, second.x, 1e-9)
        assertEquals(4.0, second.y, 1e-9)
    }

    @Test
    fun `parses exponent notation and adjacent signed numbers`() {
        val pd = PathDataParser.parse("M1e1-2e0 L.5-.25")

        assertEquals(PathCommand.MoveTo(10.0, -2.0), pd.commands[0])
        assertEquals(PathCommand.LineTo(0.5, -0.25), pd.commands[1])
    }

    @Test
    fun `serializes new commands for a stable round trip`() {
        val original = "M0 0 S1 2 3 4 T5 6 A7 8 30 1 0 9 10"
        val parsed = PathDataParser.parse(original)

        assertEquals(parsed, PathDataParser.parse(parsed.serialize()))
    }
}
