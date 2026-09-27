package com.editsvg.vector.editor

import com.editsvg.vector.core.*
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.*
import java.awt.geom.AffineTransform
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class VectorCanvasPanel : JPanel() {
    var onCommitPathData: ((PathData) -> Unit)? = null
    var onViewportChanged: ((CanvasViewport) -> Unit)? = null
    var onCursorChanged: ((Double?, Double?) -> Unit)? = null
    var onSelectionChanged: ((GraphHandle?) -> Unit)? = null

    private var model: VectorDrawableRoot? = null
    private var selectedElementKey: String? = null
    private var state = EditorInteractionState()
    private var hoverHandle: GraphHandle? = null
    private var viewport = CanvasViewport()
    private var fitted = false
    private var panAnchor: Point? = null
    private var spaceDown = false
    private var gridVisible = true

    init {
        background = JBColor(Color(0xF2, 0xF3, 0xF5), Color(0x2B, 0x2D, 0x30))
        isFocusable = true
        installMouse()
        installKeys()
    }

    fun refreshModel(value: VectorDrawableRoot?, elementKey: String?) {
        model = value
        selectedElementKey = elementKey
        state = state.cancelGesture().copy(selectedHandle = null)
        hoverHandle = null
        if (!fitted && width > 0 && height > 0) fit()
        repaint()
    }

    fun setGridVisible(visible: Boolean) { gridVisible = visible; repaint() }

    fun viewState(): CanvasViewState = CanvasViewState(viewport, gridVisible)

    fun restoreViewState(value: CanvasViewState) {
        viewport = value.viewport
        gridVisible = value.gridVisible
        fitted = true
        viewportChanged()
    }

    fun fit() {
        val root = model ?: return
        viewport = CanvasViewport.fit(root.viewportWidth.toDouble(), root.viewportHeight.toDouble(), width.toDouble(), height.toDouble(), JBUI.scale(28).toDouble())
        fitted = true
        viewportChanged()
    }

    fun actualSize() {
        val root = model ?: return
        viewport = CanvasViewport(1.0, (width - root.viewportWidth) / 2.0, (height - root.viewportHeight) / 2.0)
        fitted = true
        viewportChanged()
    }

    private fun viewportChanged() { onViewportChanged?.invoke(viewport); repaint() }

    private fun currentCommands(): List<PathCommand>? =
        state.workingCommands ?: selectedPath()?.pathData?.commands

    private fun handles(): List<GraphHandle> {
        if (selectedPath()?.editabilityReason != null) return emptyList()
        return currentCommands()?.let(PathHandles::collect).orEmpty()
    }

    private fun selectedPath(): VectorPathItem? = model?.paths?.firstOrNull { it.elementKey == selectedElementKey }

    private fun installMouse() {
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                requestFocusInWindow()
                if (e.button == MouseEvent.BUTTON2 || spaceDown) {
                    panAnchor = e.point
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    return
                }
                val commands = currentCommands() ?: return
                val hit = CanvasInteraction.hitHandle(handles(), viewport, e.x.toDouble(), e.y.toDouble(), JBUI.scale(9).toDouble())
                if (hit != null) {
                    state = state.begin(hit, commands.toList())
                    onSelectionChanged?.invoke(state.selectedHandle)
                } else {
                    state = state.cancelGesture().copy(selectedHandle = null)
                    onSelectionChanged?.invoke(null)
                }
                repaint()
            }

            override fun mouseDragged(e: MouseEvent) {
                panAnchor?.let {
                    viewport = viewport.pan((e.x - it.x).toDouble(), (e.y - it.y).toDouble())
                    panAnchor = e.point
                    viewportChanged()
                    return
                }
                val handle = state.activeHandle ?: return
                val commands = state.workingCommands?.toMutableList() ?: return
                PathHandles.apply(commands, handle, viewport.screenToWorldX(e.x.toDouble()), viewport.screenToWorldY(e.y.toDouble()))
                state = state.copy(workingCommands = commands)
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                if (panAnchor != null) {
                    panAnchor = null
                    cursor = Cursor.getDefaultCursor()
                    return
                }
                val edited = state.activeHandle != null && state.workingCommands != null
                val result = state.workingCommands
                state = state.cancelGesture()
                if (edited && result != null) onCommitPathData?.invoke(PathData(result))
                repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                hoverHandle = CanvasInteraction.hitHandle(handles(), viewport, e.x.toDouble(), e.y.toDouble(), JBUI.scale(9).toDouble())
                onCursorChanged?.invoke(viewport.screenToWorldX(e.x.toDouble()), viewport.screenToWorldY(e.y.toDouble()))
                repaint()
            }

            override fun mouseExited(e: MouseEvent) { hoverHandle = null; onCursorChanged?.invoke(null, null); repaint() }

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                viewport = viewport.zoomAt(1.12.pow(-e.preciseWheelRotation), e.x.toDouble(), e.y.toDouble())
                fitted = true
                viewportChanged()
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
        addMouseWheelListener(mouse)
    }

    private fun installKeys() {
        fun bind(key: String, action: String, block: () -> Unit) {
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), action)
            actionMap.put(action, object : AbstractAction() { override fun actionPerformed(e: ActionEvent?) = block() })
        }
        bind("pressed SPACE", "pan-start") { spaceDown = true; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) }
        bind("released SPACE", "pan-end") { spaceDown = false; cursor = Cursor.getDefaultCursor() }
        bind("ESCAPE", "cancel") { state = state.cancelGesture(); repaint() }
        listOf("LEFT" to Pair(-1.0, 0.0), "RIGHT" to Pair(1.0, 0.0), "UP" to Pair(0.0, -1.0), "DOWN" to Pair(0.0, 1.0)).forEach { (key, delta) ->
            bind(key, "nudge-$key") { nudge(delta.first, delta.second) }
            bind("shift $key", "nudge-big-$key") { nudge(delta.first * 10, delta.second * 10) }
        }
    }

    private fun nudge(dx: Double, dy: Double) {
        val selected = state.selectedHandle ?: return
        val commands = currentCommands()?.toMutableList() ?: return
        PathHandles.apply(commands, selected, selected.x + dx, selected.y + dy)
        state = state.copy(selectedHandle = selected.copy(x = selected.x + dx, y = selected.y + dy))
        onCommitPathData?.invoke(PathData(commands))
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val root = model ?: return drawMessage(g2, "Open an Android <vector> drawable to preview it")
            if (root.paths.isEmpty()) return drawMessage(g2, "This drawable has no supported <path> elements")
            if (gridVisible) drawGrid(g2)
            g2.color = JBColor(Color(0, 0, 0, 24), Color(0, 0, 0, 70))
            g2.fillRect(viewport.originX.toInt() + JBUI.scale(3), viewport.originY.toInt() + JBUI.scale(4), (root.viewportWidth * viewport.scale).toInt(), (root.viewportHeight * viewport.scale).toInt())
            g2.color = JBColor(Color(0xFF, 0xFF, 0xFF), Color(0xF7, 0xF7, 0xF7))
            g2.fillRect(viewport.originX.toInt(), viewport.originY.toInt(), (root.viewportWidth * viewport.scale).toInt(), (root.viewportHeight * viewport.scale).toInt())
            g2.color = JBColor.border()
            g2.drawRect(viewport.originX.toInt(), viewport.originY.toInt(), (root.viewportWidth * viewport.scale).toInt(), (root.viewportHeight * viewport.scale).toInt())
            val originalTransform = g2.transform
            g2.transform = CanvasRendering.worldTransform(originalTransform, viewport)
            val selectedNode = selectedElementKey?.let(root.hierarchy::find)
            val emphasized = when (selectedNode?.type) {
                VectorElementType.VECTOR, null -> root.paths.mapTo(mutableSetOf()) { it.elementKey }
                VectorElementType.GROUP -> root.paths.filter { selectedNode.contains(it.elementKey) }.mapTo(mutableSetOf()) { it.elementKey }
                VectorElementType.PATH -> mutableSetOf(selectedNode.key)
                VectorElementType.CLIP_PATH -> emptySet()
            }
            root.paths.forEach { item ->
                val data = if (item.elementKey == selectedElementKey && state.workingCommands != null) PathData(state.workingCommands!!) else item.pathData
                val shape = item.transform.createTransformedShape(data.toPath2D())
                val fill = parseColor(item.fillColor) ?: JBColor(Color(0x3D, 0x7E, 0xD8), Color(0x58, 0xA6, 0xFF))
                val isEmphasized = item.elementKey in emphasized
                g2.color = if (isEmphasized) fill else JBColor(Color(0x91, 0x93, 0x99, 130), Color(0x70, 0x72, 0x78, 150))
                g2.fill(shape)
                val stroke = parseColor(item.strokeColor)
                val strokeWidth = item.strokeWidth?.toFloatOrNull() ?: 0f
                if (stroke != null && strokeWidth > 0f) {
                    g2.color = if (isEmphasized) stroke else JBColor(Color(0x82, 0x84, 0x8A), Color(0x78, 0x7A, 0x80))
                    g2.stroke = BasicStroke(strokeWidth)
                    g2.draw(shape)
                }
            }
            g2.transform = originalTransform
            drawHandles(g2)
            selectedPath()?.editabilityReason?.let {
                g2.color = JBColor(Color(0x8A, 0x5A, 0x00), Color(0xFF, 0xC6, 0x6D))
                g2.drawString("View only: $it", JBUI.scale(14), height - JBUI.scale(14))
            }
        } finally { g2.dispose() }
    }

    private fun drawGrid(g2: Graphics2D) {
        val raw = 28.0 / viewport.scale
        val power = 10.0.pow(floor(log10(raw)))
        val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * power >= raw } * power
        g2.color = JBColor(Color(0xC4, 0xC7, 0xCC), Color(0x3B, 0x3D, 0x43))
        val spacing = step * viewport.scale
        if (spacing < 7.0) return
        var sx = viewport.originX % spacing
        while (sx < width) {
            var sy = viewport.originY % spacing
            while (sy < height) {
                g2.fillOval(sx.toInt(), sy.toInt(), JBUI.scale(1), JBUI.scale(1))
                sy += spacing
            }
            sx += spacing
        }
    }

    private fun drawHandles(g2: Graphics2D) {
        if (selectedPath() == null) return
        val handles = handles()
        val selected = state.selectedHandle
        handles.filter { it.kind.name.contains("Cp") }.forEach { control ->
            val endpoint = handles.lastOrNull { it.commandIndex == control.commandIndex && it.kind.name.endsWith("End") }
            if (endpoint != null) { g2.color = JBColor.GRAY; g2.drawLine(viewport.worldToScreenX(control.x).toInt(), viewport.worldToScreenY(control.y).toInt(), viewport.worldToScreenX(endpoint.x).toInt(), viewport.worldToScreenY(endpoint.y).toInt()) }
        }
        handles.forEach { handle ->
            val x = viewport.worldToScreenX(handle.x).toInt(); val y = viewport.worldToScreenY(handle.y).toInt()
            val active = handle.commandIndex == selected?.commandIndex && handle.kind == selected.kind
            val hovered = handle == hoverHandle
            val radius = JBUI.scale(if (active || hovered) 6 else 4)
            g2.color = if (active) JBColor(Color(0xFF, 0x8A, 0x00), Color(0xFF, 0xA6, 0x37)) else JBColor(Color(0x16, 0x73, 0xD1), Color(0x58, 0xA6, 0xFF))
            if (handle.kind.name.contains("Cp")) g2.drawRect(x - radius, y - radius, radius * 2, radius * 2) else g2.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        }
    }

    private fun drawMessage(g2: Graphics2D, message: String) { g2.color = JBColor.GRAY; g2.drawString(message, JBUI.scale(20), JBUI.scale(32)) }

    private fun parseColor(value: String?): Color? {
        val hex = value?.trim()?.takeIf { it.startsWith('#') }?.drop(1) ?: return null
        return try { when (hex.length) { 6 -> Color(0xFF000000.toInt() or hex.toInt(16), true); 8 -> Color(hex.toLong(16).toInt(), true); else -> null } } catch (_: NumberFormatException) { null }
    }
}

data class CanvasViewState(
    val viewport: CanvasViewport = CanvasViewport(),
    val gridVisible: Boolean = true,
)
