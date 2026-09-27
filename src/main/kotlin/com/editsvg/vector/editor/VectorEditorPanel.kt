package com.editsvg.vector.editor

import com.editsvg.vector.core.VectorDrawableRoot
import com.editsvg.vector.core.VectorPsiParser
import com.editsvg.vector.core.VectorPsiWriter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class VectorEditorPanel(
    private val project: Project,
    private val file: VirtualFile,
    @Suppress("unused") private val document: Document,
) : JPanel(BorderLayout()) {
    private var model: VectorDrawableRoot? = null
    private var selectedElementKey: String = "vector"
    private val toolbar = VectorEditorToolbar()
    private val canvas = VectorCanvasPanel()
    private val hierarchy = VectorHierarchyPanel()
    private val inspector = VectorInspectorPanel()
    private val rightSplitter = JBSplitter(true, 0.4f).apply {
        dividerWidth = JBUI.scale(1)
        firstComponent = hierarchy
        secondComponent = inspector
    }
    private val mainSplitter = JBSplitter(false, 0.76f).apply {
        dividerWidth = JBUI.scale(1)
        firstComponent = canvas
        secondComponent = rightSplitter
    }

    init {
        border = JBUI.Borders.empty()
        add(JPanel(BorderLayout()).apply {
            background = JBColor.PanelBackground
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(0, 7),
            )
            add(toolbar, BorderLayout.WEST)
        }, BorderLayout.NORTH)
        add(mainSplitter, BorderLayout.CENTER)

        toolbar.onFit = canvas::fit
        toolbar.onActualSize = canvas::actualSize
        toolbar.onGridChanged = canvas::setGridVisible
        canvas.onViewportChanged = { toolbar.showZoom(it.scale) }
        canvas.onCursorChanged = toolbar::showCoordinates
        canvas.onCommitPathData = { data ->
            VectorPsiWriter.updatePathData(project, psiFile(), selectedElementKey, data)
        }

        hierarchy.onSelectionChanged = { key ->
            selectedElementKey = key
            bindSelection()
        }
        hierarchy.onGroup = { key -> mutateStructure(key) { VectorPsiWriter.group(project, psiFile(), key) } }
        hierarchy.onUngroup = { key -> mutateStructure(key) { VectorPsiWriter.ungroup(project, psiFile(), key) } }
        hierarchy.onMoveOut = { key -> mutateStructure(key) { VectorPsiWriter.moveOut(project, psiFile(), key) } }
        hierarchy.onDelete = { key ->
            if (VectorPsiWriter.delete(project, psiFile(), key)) {
                selectedElementKey = "vector"
                reloadFromPsi()
            }
        }
        hierarchy.onMove = { source, target, position ->
            mutateStructure(source) { VectorPsiWriter.move(project, psiFile(), source, target, position) }
        }
        reloadFromPsi()
    }

    fun reloadFromPsi() {
        val previousIdentity = selectedElementKey.substringBeforeLast('@')
        model = (PsiManager.getInstance(project).findFile(file) as? XmlFile)?.let(VectorPsiParser::parse)
        val root = model?.hierarchy
        selectedElementKey = when {
            root == null -> "vector"
            root.find(selectedElementKey) != null -> selectedElementKey
            else -> findByIdentity(root, previousIdentity)?.key ?: "vector"
        }
        bindSelection()
    }

    fun editorState(): VectorEditorFileState {
        val view = canvas.viewState()
        return VectorEditorFileState(
            selectedElementKey = selectedElementKey,
            expandedElementKeys = hierarchy.expandedElementKeys(),
            mainSplitterProportion = mainSplitter.proportion,
            rightSplitterProportion = rightSplitter.proportion,
            scale = view.viewport.scale,
            originX = view.viewport.originX,
            originY = view.viewport.originY,
            gridVisible = view.gridVisible,
        )
    }

    fun restoreEditorState(state: VectorEditorFileState) {
        selectedElementKey = state.selectedElementKey
        mainSplitter.proportion = state.mainSplitterProportion.coerceIn(0.2f, 0.9f)
        rightSplitter.proportion = state.rightSplitterProportion.coerceIn(0.15f, 0.85f)
        canvas.restoreViewState(
            CanvasViewState(
                com.editsvg.vector.core.CanvasViewport(state.scale, state.originX, state.originY),
                state.gridVisible,
            ),
        )
        toolbar.showGrid(state.gridVisible)
        reloadFromPsi()
        hierarchy.restoreExpandedElementKeys(state.expandedElementKeys)
    }

    private fun bindSelection() {
        val current = model
        hierarchy.bind(current?.hierarchy, selectedElementKey)
        canvas.refreshModel(current, selectedElementKey)
        inspector.bind(project, file, current, selectedElementKey) { reloadFromPsi() }
    }

    private fun mutateStructure(selectedKey: String, action: () -> Boolean) {
        selectedElementKey = selectedKey
        if (action()) reloadFromPsi()
    }

    private fun findByIdentity(node: com.editsvg.vector.core.VectorElementNode, identity: String): com.editsvg.vector.core.VectorElementNode? {
        if (node.key.substringBeforeLast('@') == identity) return node
        return node.children.firstNotNullOfOrNull { findByIdentity(it, identity) }
    }

    private fun psiFile(): XmlFile = PsiManager.getInstance(project).findFile(file) as XmlFile
}
