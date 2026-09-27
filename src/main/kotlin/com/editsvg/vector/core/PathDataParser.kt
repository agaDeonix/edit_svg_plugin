package com.editsvg.vector.core

/**
 * Tokenizes and parses pathData into absolute [PathCommand] list.
 * Supported: M/L/H/V/C/S/Q/T/A/Z, their relative forms, and repeated parameter groups.
 */
object PathDataParser {
    fun parse(pathData: String): PathData {
        val tokens = tokenize(pathData)
        val out = ArrayList<PathCommand>()
        var cx = 0.0
        var cy = 0.0
        var subStartX = 0.0
        var subStartY = 0.0

        var i = 0
        fun nextNumber(): Double {
            if (i >= tokens.size) throw IllegalArgumentException("Unexpected end of pathData")
            val t = tokens[i++]
            if (t is Token.Number) return t.value
            throw IllegalArgumentException("Expected number, got $t")
        }

        fun hasMore() = i < tokens.size

        while (hasMore()) {
            when (val t = tokens[i++]) {
                is Token.Command -> {
                    when (t.code) {
                        'M', 'm' -> {
                            val rel = t.code == 'm'
                            var nx = nextNumber()
                            var ny = nextNumber()
                            if (rel) {
                                nx += cx
                                ny += cy
                            }
                            cx = nx
                            cy = ny
                            subStartX = cx
                            subStartY = cy
                            out.add(PathCommand.MoveTo(cx, cy))
                            // Subsequent pairs are implicit L/l until next command
                            while (hasMore() && tokens[i] is Token.Number) {
                                var lx = nextNumber()
                                var ly = nextNumber()
                                if (rel) {
                                    lx += cx
                                    ly += cy
                                }
                                cx = lx
                                cy = ly
                                out.add(PathCommand.LineTo(cx, cy))
                            }
                        }

                        'L', 'l' -> {
                            val rel = t.code == 'l'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var lx = nextNumber()
                                var ly = nextNumber()
                                if (rel) {
                                    lx += cx
                                    ly += cy
                                }
                                cx = lx
                                cy = ly
                                out.add(PathCommand.LineTo(cx, cy))
                            }
                        }

                        'H', 'h' -> {
                            val rel = t.code == 'h'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var x = nextNumber()
                                if (rel) x += cx
                                cx = x
                                out.add(PathCommand.HorizontalTo(cx))
                            }
                        }

                        'V', 'v' -> {
                            val rel = t.code == 'v'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var y = nextNumber()
                                if (rel) y += cy
                                cy = y
                                out.add(PathCommand.VerticalTo(cy))
                            }
                        }

                        'C', 'c' -> {
                            val rel = t.code == 'c'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var x1 = nextNumber()
                                var y1 = nextNumber()
                                var x2 = nextNumber()
                                var y2 = nextNumber()
                                var x = nextNumber()
                                var y = nextNumber()
                                if (rel) {
                                    x1 += cx
                                    y1 += cy
                                    x2 += cx
                                    y2 += cy
                                    x += cx
                                    y += cy
                                }
                                cx = x
                                cy = y
                                out.add(PathCommand.CubicTo(x1, y1, x2, y2, cx, cy))
                            }
                        }

                        'Q', 'q' -> {
                            val rel = t.code == 'q'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var x1 = nextNumber()
                                var y1 = nextNumber()
                                var x = nextNumber()
                                var y = nextNumber()
                                if (rel) {
                                    x1 += cx
                                    y1 += cy
                                    x += cx
                                    y += cy
                                }
                                cx = x
                                cy = y
                                out.add(PathCommand.QuadTo(x1, y1, cx, cy))
                            }
                        }

                        'S', 's' -> {
                            val rel = t.code == 's'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var x2 = nextNumber()
                                var y2 = nextNumber()
                                var x = nextNumber()
                                var y = nextNumber()
                                if (rel) {
                                    x2 += cx; y2 += cy; x += cx; y += cy
                                }
                                cx = x; cy = y
                                out.add(PathCommand.SmoothCubicTo(x2, y2, x, y))
                            }
                        }

                        'T', 't' -> {
                            val rel = t.code == 't'
                            while (hasMore() && tokens[i] is Token.Number) {
                                var x = nextNumber()
                                var y = nextNumber()
                                if (rel) { x += cx; y += cy }
                                cx = x; cy = y
                                out.add(PathCommand.SmoothQuadTo(x, y))
                            }
                        }

                        'A', 'a' -> {
                            val rel = t.code == 'a'
                            while (hasMore() && tokens[i] is Token.Number) {
                                val rx = nextNumber()
                                val ry = nextNumber()
                                val rotation = nextNumber()
                                val largeArc = flag(nextNumber())
                                val sweep = flag(nextNumber())
                                var x = nextNumber()
                                var y = nextNumber()
                                if (rel) { x += cx; y += cy }
                                cx = x; cy = y
                                out.add(PathCommand.ArcTo(rx, ry, rotation, largeArc, sweep, x, y))
                            }
                        }

                        'Z', 'z' -> {
                            cx = subStartX
                            cy = subStartY
                            out.add(PathCommand.ClosePath())
                        }

                        else -> throw IllegalArgumentException("Unsupported path command '${t.code}' in pathData")
                    }
                }

                is Token.Number -> throw IllegalArgumentException("Unexpected number at position (missing command?)")
            }
        }

        return PathData(out)
    }

    private fun flag(value: Double): Boolean = when (value) {
        0.0 -> false
        1.0 -> true
        else -> throw IllegalArgumentException("Arc flag must be 0 or 1, got $value")
    }

    private sealed class Token {
        data class Command(val code: Char) : Token()
        data class Number(val value: Double) : Token()
    }

    private fun tokenize(s: String): List<Token> {
        val out = ArrayList<Token>()
        var idx = 0
        val n = s.length
        fun skipWs() {
            while (idx < n && s[idx].isWhitespace()) idx++
        }
        while (idx < n) {
            skipWs()
            if (idx >= n) break
            val ch = s[idx]
            if (ch.isLetter()) {
                idx++
                out.add(Token.Command(ch))
                continue
            }
            // number
            val start = idx
            if (ch == '-' || ch == '+') idx++
            var digits = 0
            while (idx < n && s[idx].isDigit()) { idx++; digits++ }
            if (idx < n && s[idx] == '.') {
                idx++
                while (idx < n && s[idx].isDigit()) { idx++; digits++ }
            }
            if (digits == 0) throw IllegalArgumentException("Invalid number near index $start")
            if (idx < n && (s[idx] == 'e' || s[idx] == 'E')) {
                val exponentStart = idx++
                if (idx < n && (s[idx] == '-' || s[idx] == '+')) idx++
                val exponentDigits = idx
                while (idx < n && s[idx].isDigit()) idx++
                if (idx == exponentDigits) throw IllegalArgumentException("Invalid exponent near index $exponentStart")
            }
            if (idx == start) throw IllegalArgumentException("Invalid pathData near index $start")
            val txt = s.substring(start, idx)
            val v = txt.toDouble()
            out.add(Token.Number(v))
            // optional comma between numbers
            skipWs()
            if (idx < n && s[idx] == ',') idx++
        }
        return out
    }
}
