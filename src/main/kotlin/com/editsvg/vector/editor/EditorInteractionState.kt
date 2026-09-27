package com.editsvg.vector.editor

import com.editsvg.vector.core.GraphHandle
import com.editsvg.vector.core.PathCommand

enum class EditorTool { SELECT, EDIT_NODES }

data class EditorInteractionState(
    val tool: EditorTool = EditorTool.EDIT_NODES,
    val selectedHandle: GraphHandle? = null,
    val activeHandle: GraphHandle? = null,
    val workingCommands: List<PathCommand>? = null,
) {
    fun begin(handle: GraphHandle, commands: List<PathCommand>): EditorInteractionState =
        if (tool == EditorTool.EDIT_NODES) copy(selectedHandle = handle, activeHandle = handle, workingCommands = commands)
        else copy(selectedHandle = null)

    fun cancelGesture(): EditorInteractionState = copy(activeHandle = null, workingCommands = null)
}
