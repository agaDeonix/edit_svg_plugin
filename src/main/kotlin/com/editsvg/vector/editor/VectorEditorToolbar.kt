package com.editsvg.vector.editor

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton

class VectorEditorToolbar : JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(5))) {
    var onFit: (() -> Unit)? = null
    var onActualSize: (() -> Unit)? = null
    var onGridChanged: ((Boolean) -> Unit)? = null

    private val grid = JToggleButton("Grid", true)
    private val zoomLabel = JBLabel("100%")
    private val coordinatesLabel = JBLabel("x —  y —")

    init {
        isOpaque = false
        add(compactButton("Fit") { onFit?.invoke() }.apply { toolTipText = "Fit drawable" })
        add(compactButton("1:1") { onActualSize?.invoke() }.apply { toolTipText = "Actual viewport size" })
        grid.margin = Insets(0, JBUI.scale(7), 0, JBUI.scale(7))
        grid.preferredSize = Dimension(grid.preferredSize.width, JBUI.scale(28))
        grid.isFocusable = false
        grid.toolTipText = "Show or hide grid"
        grid.addActionListener { onGridChanged?.invoke(grid.isSelected) }
        add(grid)
        add(zoomLabel.apply { border = JBUI.Borders.emptyLeft(5) })
        add(coordinatesLabel.apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.emptyLeft(8)
        })
    }

    fun showZoom(scale: Double) { zoomLabel.text = "${(scale * 100).toInt()}%" }

    fun showGrid(visible: Boolean) { grid.isSelected = visible }

    fun showCoordinates(x: Double?, y: Double?) {
        coordinatesLabel.text = if (x == null || y == null) "x —  y —" else "x %.2f  y %.2f".format(x, y)
    }

    private fun compactButton(text: String, action: () -> Unit) = JButton(text).apply {
        margin = Insets(0, JBUI.scale(7), 0, JBUI.scale(7))
        preferredSize = Dimension(preferredSize.width, JBUI.scale(28))
        isFocusable = false
        addActionListener { action() }
    }
}
