package com.editsvg.vector.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

class VectorEditorFileState(
    var selectedElementKey: String = "vector",
    var expandedElementKeys: Set<String> = emptySet(),
    var mainSplitterProportion: Float = 0.76f,
    var rightSplitterProportion: Float = 0.4f,
    var scale: Double = 1.0,
    var originX: Double = 0.0,
    var originY: Double = 0.0,
    var gridVisible: Boolean = true,
) : FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean = false
}

class VectorDrawableFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(),
    FileEditor,
    DocumentListener,
    Disposable {

    private val propertyChangeSupport = PropertyChangeSupport(this)

    private val document =
        FileDocumentManager.getInstance().getDocument(file)
            ?: error("No document for $file")

    private val panel = VectorEditorPanel(project, file, document)

    init {
        document.addDocumentListener(this)
    }

    override fun dispose() {
        document.removeDocumentListener(this)
    }

    override fun documentChanged(event: DocumentEvent) {
        panel.reloadFromPsi()
    }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel

    override fun getName(): String = "Visual"

    override fun isValid(): Boolean = file.isValid

    override fun getFile(): VirtualFile = file

    override fun isModified(): Boolean = false

    override fun getState(level: FileEditorStateLevel): FileEditorState = panel.editorState()

    override fun setState(state: FileEditorState) {
        if (state is VectorEditorFileState) panel.restoreEditorState(state)
    }

    override fun selectNotify() {
        panel.reloadFromPsi()
    }

    override fun deselectNotify() {
        // no-op
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }
}
