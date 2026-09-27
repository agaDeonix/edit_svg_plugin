package com.editsvg.vector.core

enum class HandleKind {
    MoveEnd,
    LineEnd,
    HorizontalEnd,
    VerticalEnd,
    CubicCp1,
    CubicCp2,
    CubicEnd,
    QuadCp,
    QuadEnd,
}

data class GraphHandle(
    val commandIndex: Int,
    val kind: HandleKind,
    val x: Double,
    val y: Double,
)

object PathHandles {
    fun collect(cmds: List<PathCommand>): List<GraphHandle> {
        val out = ArrayList<GraphHandle>()
        var penX = 0.0
        var penY = 0.0

        cmds.forEachIndexed { i, cmd ->
            when (cmd) {
                is PathCommand.MoveTo -> {
                    out.add(GraphHandle(i, HandleKind.MoveEnd, cmd.x, cmd.y))
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.LineTo -> {
                    out.add(GraphHandle(i, HandleKind.LineEnd, cmd.x, cmd.y))
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.HorizontalTo -> {
                    out.add(GraphHandle(i, HandleKind.HorizontalEnd, cmd.x, penY))
                    penX = cmd.x
                }

                is PathCommand.VerticalTo -> {
                    out.add(GraphHandle(i, HandleKind.VerticalEnd, penX, cmd.y))
                    penY = cmd.y
                }

                is PathCommand.CubicTo -> {
                    out.add(GraphHandle(i, HandleKind.CubicCp1, cmd.x1, cmd.y1))
                    out.add(GraphHandle(i, HandleKind.CubicCp2, cmd.x2, cmd.y2))
                    out.add(GraphHandle(i, HandleKind.CubicEnd, cmd.x, cmd.y))
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.QuadTo -> {
                    out.add(GraphHandle(i, HandleKind.QuadCp, cmd.x1, cmd.y1))
                    out.add(GraphHandle(i, HandleKind.QuadEnd, cmd.x, cmd.y))
                    penX = cmd.x
                    penY = cmd.y
                }

                is PathCommand.SmoothCubicTo, is PathCommand.SmoothQuadTo, is PathCommand.ArcTo -> {
                    // These commands are rendered losslessly but remain view-only in the first release.
                }

                is PathCommand.ClosePath -> {
                    penX = findSubpathStart(cmds, i)
                    penY = findSubpathStartY(cmds, i)
                }
            }
        }
        return out
    }

    private fun findSubpathStart(cmds: List<PathCommand>, closeIndex: Int): Double {
        var j = closeIndex - 1
        while (j >= 0) {
            when (val c = cmds[j]) {
                is PathCommand.MoveTo -> return c.x
                else -> j--
            }
        }
        return 0.0
    }

    private fun findSubpathStartY(cmds: List<PathCommand>, closeIndex: Int): Double {
        var j = closeIndex - 1
        while (j >= 0) {
            when (val c = cmds[j]) {
                is PathCommand.MoveTo -> return c.y
                else -> j--
            }
        }
        return 0.0
    }

    fun apply(cmds: MutableList<PathCommand>, handle: GraphHandle, newX: Double, newY: Double) {
        when (val cmd = cmds[handle.commandIndex]) {
            is PathCommand.MoveTo -> {
                cmds[handle.commandIndex] = cmd.copy(x = newX, y = newY)
            }

            is PathCommand.LineTo -> {
                cmds[handle.commandIndex] = cmd.copy(x = newX, y = newY)
            }

            is PathCommand.HorizontalTo -> {
                cmds[handle.commandIndex] = cmd.copy(x = newX)
            }

            is PathCommand.VerticalTo -> {
                cmds[handle.commandIndex] = cmd.copy(y = newY)
            }

            is PathCommand.CubicTo -> {
                when (handle.kind) {
                    HandleKind.CubicCp1 -> cmds[handle.commandIndex] = cmd.copy(x1 = newX, y1 = newY)
                    HandleKind.CubicCp2 -> cmds[handle.commandIndex] = cmd.copy(x2 = newX, y2 = newY)
                    HandleKind.CubicEnd -> cmds[handle.commandIndex] = cmd.copy(x = newX, y = newY)
                    else -> {}
                }
            }

            is PathCommand.QuadTo -> {
                when (handle.kind) {
                    HandleKind.QuadCp -> cmds[handle.commandIndex] = cmd.copy(x1 = newX, y1 = newY)
                    HandleKind.QuadEnd -> cmds[handle.commandIndex] = cmd.copy(x = newX, y = newY)
                    else -> {}
                }
            }

            is PathCommand.SmoothCubicTo, is PathCommand.SmoothQuadTo, is PathCommand.ArcTo -> {}

            else -> {}
        }
    }
}
