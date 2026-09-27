package com.editsvg.vector.core

import kotlin.math.abs

object VectorOptimizer {
    private const val EPS = 1e-6

    fun optimizePathData(pd: PathData): PathData {
        val out = ArrayList<PathCommand>()
        var penX = 0.0
        var penY = 0.0
        var subStartX = 0.0
        var subStartY = 0.0

        fun advancePen(cmd: PathCommand) {
            when (cmd) {
                is PathCommand.MoveTo -> {
                    penX = cmd.x
                    penY = cmd.y
                    subStartX = penX
                    subStartY = penY
                }

                is PathCommand.LineTo -> {
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.HorizontalTo -> penX = cmd.x
                is PathCommand.VerticalTo -> penY = cmd.y
                is PathCommand.CubicTo -> {
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.QuadTo -> {
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.SmoothCubicTo -> { penX = cmd.x; penY = cmd.y }
                is PathCommand.SmoothQuadTo -> { penX = cmd.x; penY = cmd.y }
                is PathCommand.ArcTo -> { penX = cmd.x; penY = cmd.y }

                is PathCommand.ClosePath -> {
                    penX = subStartX
                    penY = subStartY
                }
            }
        }

        for (cmd in pd.commands) {
            when (cmd) {
                is PathCommand.LineTo -> {
                    if (abs(cmd.x - penX) < EPS && abs(cmd.y - penY) < EPS) continue
                }

                is PathCommand.HorizontalTo -> {
                    if (abs(cmd.x - penX) < EPS) continue
                }

                is PathCommand.VerticalTo -> {
                    if (abs(cmd.y - penY) < EPS) continue
                }

                else -> {}
            }
            out.add(cmd)
            advancePen(cmd)
        }

        val serialized = PathData(out).serialize()
        return try {
            PathDataParser.parse(serialized)
        } catch (_: IllegalArgumentException) {
            PathData(out)
        }
    }
}
