package com.editsvg.vector.editor

import com.editsvg.vector.core.GraphHandle
import com.editsvg.vector.core.HandleKind
import com.editsvg.vector.core.PathCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EditorInteractionStateTest {
    @Test
    fun `cancel clears working gesture but keeps selected handle`() {
        val handle = GraphHandle(0, HandleKind.MoveEnd, 1.0, 2.0)
        val state = EditorInteractionState(selectedHandle = handle)
            .begin(handle, listOf(PathCommand.MoveTo(1.0, 2.0)))
            .cancelGesture()

        assertEquals(handle, state.selectedHandle)
        assertNull(state.activeHandle)
        assertNull(state.workingCommands)
    }

    @Test
    fun `select mode cannot begin node gesture`() {
        val handle = GraphHandle(0, HandleKind.MoveEnd, 1.0, 2.0)
        val state = EditorInteractionState(tool = EditorTool.SELECT).begin(handle, listOf(PathCommand.MoveTo(1.0, 2.0)))

        assertNull(state.activeHandle)
    }
}
