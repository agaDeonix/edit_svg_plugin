package com.editsvg.vector.editor

import com.editsvg.vector.core.VectorDrawableRoot
import com.editsvg.vector.core.VectorElementNode
import com.editsvg.vector.core.VectorElementType
import com.editsvg.vector.core.VectorPsiWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.KeyStroke

class VectorInspectorPanel : JPanel(BorderLayout()) {
    private val title = JBLabel("Properties")
    private val subtitle = JBLabel("Select an element")
    private val content = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor(Color(0xF7, 0xF7, 0xFA), Color(0x24, 0x25, 0x29))
    }
    private var generation = 0
    private var advancedExpanded = false

    init {
        background = content.background
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(9, 12, 7, 12)
            title.font = title.font.deriveFont(java.awt.Font.BOLD)
            title.alignmentX = LEFT_ALIGNMENT
            subtitle.foreground = JBColor.GRAY
            subtitle.alignmentX = LEFT_ALIGNMENT
            add(title)
            add(Box.createVerticalStrut(JBUI.scale(2)))
            add(subtitle)
        }
        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(content, true), BorderLayout.CENTER)
    }

    fun bind(
        project: Project,
        file: VirtualFile,
        model: VectorDrawableRoot?,
        selectedKey: String,
        onApplied: () -> Unit,
    ) {
        generation++
        val currentGeneration = generation
        content.removeAll()
        val node = model?.hierarchy?.find(selectedKey)
        if (node == null) {
            subtitle.text = "Select an element"
            content.revalidate()
            content.repaint()
            return
        }
        subtitle.text = "${typeLabel(node.type)} · ${node.displayName}"
        val baseline = node.attributes.toMutableMap()
        val basic = basicSections(node)
        val advanced = advancedSections(node)
        basic.forEach { (sectionTitle, specs) -> content.add(section(project, file, node, sectionTitle, specs, baseline, currentGeneration, onApplied)) }
        if (advanced.isNotEmpty()) {
            val advancedContent = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentX = LEFT_ALIGNMENT
                isVisible = advancedExpanded
                advanced.forEach { (sectionTitle, specs) -> add(section(project, file, node, sectionTitle, specs, baseline, currentGeneration, onApplied)) }
            }
            content.add(JToggleButton(if (advancedExpanded) "Advanced ▴" else "Advanced ▾", advancedExpanded).apply {
                alignmentX = LEFT_ALIGNMENT
                isFocusable = false
                margin = Insets(0, JBUI.scale(7), 0, JBUI.scale(7))
                addActionListener {
                    advancedExpanded = isSelected
                    text = if (isSelected) "Advanced ▴" else "Advanced ▾"
                    advancedContent.isVisible = isSelected
                    content.revalidate()
                }
            })
            content.add(advancedContent)
        }
        content.add(Box.createVerticalGlue())
        content.revalidate()
        content.repaint()
    }

    private fun section(
        project: Project,
        file: VirtualFile,
        node: VectorElementNode,
        sectionTitle: String,
        specs: List<FieldSpec>,
        baseline: MutableMap<String, String>,
        currentGeneration: Int,
        onApplied: () -> Unit,
    ): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        alignmentX = LEFT_ALIGNMENT
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
            JBUI.Borders.empty(8, 11, 9, 11),
        )
        add(JBLabel(sectionTitle).apply {
            font = font.deriveFont(java.awt.Font.BOLD)
            alignmentX = LEFT_ALIGNMENT
        })
        add(Box.createVerticalStrut(JBUI.scale(6)))
        specs.forEach { spec -> add(fieldRow(project, file, node, spec, baseline, currentGeneration, onApplied)) }
    }

    private fun fieldRow(
        project: Project,
        file: VirtualFile,
        node: VectorElementNode,
        spec: FieldSpec,
        baseline: MutableMap<String, String>,
        currentGeneration: Int,
        onApplied: () -> Unit,
    ): JPanel = JPanel(GridBagLayout()).apply {
        isOpaque = false
        alignmentX = LEFT_ALIGNMENT
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(48))
        val field = JBTextField(initialValue(node, spec)).apply {
            preferredSize = Dimension(JBUI.scale(110), JBUI.scale(28))
            minimumSize = Dimension(JBUI.scale(65), JBUI.scale(28))
            putClientProperty("JTextField.variant", "compact")
        }
        baseline.putIfAbsent(spec.attribute, field.text)
        val errorLabel = JBLabel("").apply {
            foreground = JBColor(Color(0xB3, 0x26, 0x1E), Color(0xFF, 0xB4, 0xAB))
        }
        val editor = if (spec.color) JPanel(BorderLayout(JBUI.scale(4), 0)).apply {
            isOpaque = false
            add(JButton(ColorIcon { field.text }).apply {
                preferredSize = Dimension(JBUI.scale(28), JBUI.scale(28))
                isFocusable = false
                margin = JBUI.emptyInsets()
                toolTipText = "Choose color"
                addActionListener {
                    val chosen = JColorChooser.showDialog(this@VectorInspectorPanel, "Choose ${spec.label}", parseColor(field.text) ?: Color.WHITE)
                    if (chosen != null) {
                        field.text = "#%02X%02X%02X".format(chosen.red, chosen.green, chosen.blue)
                        field.postActionEvent()
                    }
                }
            }, BorderLayout.WEST)
            add(field, BorderLayout.CENTER)
        } else field

        fun commit() {
            if (generation != currentGeneration) return
            val value = field.text.trim()
            val error = InspectorValues.attributeError(spec.attribute, value)
            errorLabel.text = error.orEmpty()
            field.toolTipText = error
            if (error != null || value == baseline[spec.attribute].orEmpty()) return
            val xml = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: return
            val nullableValue = if (value.isEmpty() && !spec.required) null else value
            VectorPsiWriter.updateElementAttributes(project, xml, node.key, mapOf(spec.attribute to nullableValue))
            baseline[spec.attribute] = value
            onApplied()
        }

        field.addActionListener { commit() }
        field.addFocusListener(object : FocusAdapter() { override fun focusLost(e: FocusEvent) = commit() })
        field.registerKeyboardAction({
            field.text = baseline[spec.attribute].orEmpty()
            errorLabel.text = ""
        }, KeyStroke.getKeyStroke("ESCAPE"), WHEN_FOCUSED)

        add(JBLabel(spec.label).apply { foreground = JBColor.GRAY }, GridBagConstraints().apply {
            gridx = 0; gridy = 0; anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 0, JBUI.scale(8))
        })
        add(editor, GridBagConstraints().apply {
            gridx = 1; gridy = 0; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL
        })
        add(errorLabel, GridBagConstraints().apply {
            gridx = 1; gridy = 1; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL
            insets = Insets(JBUI.scale(2), 0, 0, 0)
        })
    }

    private fun basicSections(node: VectorElementNode): List<Pair<String, List<FieldSpec>>> = when (node.type) {
        VectorElementType.VECTOR -> listOf(
            "Size" to listOf(field("Width", "width"), field("Height", "height"), field("Viewport W", "viewportWidth", required = true), field("Viewport H", "viewportHeight", required = true)),
        )
        VectorElementType.GROUP -> listOf(
            "Element" to listOf(field("Name", "name")),
            "Transform" to listOf(field("Rotation", "rotation"), field("Scale X", "scaleX"), field("Scale Y", "scaleY"), field("Translate X", "translateX"), field("Translate Y", "translateY")),
        )
        VectorElementType.PATH -> listOf(
            "Element" to listOf(field("Name", "name")),
            "Fill" to listOf(field("Color", "fillColor", color = true), field("Alpha", "fillAlpha")),
            "Stroke" to listOf(field("Color", "strokeColor", color = true), field("Width", "strokeWidth"), field("Alpha", "strokeAlpha")),
        )
        VectorElementType.CLIP_PATH -> listOf(
            "Element" to listOf(field("Name", "name")),
            "Clip" to listOf(field("Path data", "pathData", required = true)),
        )
    }

    private fun advancedSections(node: VectorElementNode): List<Pair<String, List<FieldSpec>>> = when (node.type) {
        VectorElementType.VECTOR -> listOf(
            "Appearance" to listOf(field("Alpha", "alpha"), field("Tint", "tint", color = true), field("Tint mode", "tintMode"), field("Auto mirrored", "autoMirrored")),
        )
        VectorElementType.GROUP -> listOf("Pivot" to listOf(field("Pivot X", "pivotX"), field("Pivot Y", "pivotY")))
        VectorElementType.PATH -> listOf(
            "Rendering" to listOf(field("Fill type", "fillType"), field("Line cap", "strokeLineCap"), field("Line join", "strokeLineJoin"), field("Miter limit", "strokeMiterLimit")),
            "Trim" to listOf(field("Start", "trimPathStart"), field("End", "trimPathEnd"), field("Offset", "trimPathOffset")),
        )
        VectorElementType.CLIP_PATH -> listOf("Rendering" to listOf(field("Fill type", "fillType")))
    }

    private fun initialValue(node: VectorElementNode, spec: FieldSpec): String = node.attributes[spec.attribute] ?: when (spec.attribute) {
        "rotation", "pivotX", "pivotY", "translateX", "translateY" -> "0"
        "scaleX", "scaleY" -> "1"
        else -> ""
    }

    private fun typeLabel(type: VectorElementType): String = when (type) {
        VectorElementType.VECTOR -> "Vector"
        VectorElementType.GROUP -> "Group"
        VectorElementType.PATH -> "Path"
        VectorElementType.CLIP_PATH -> "Clip path"
    }

    private data class FieldSpec(val label: String, val attribute: String, val color: Boolean = false, val required: Boolean = false)
    private fun field(label: String, attribute: String, color: Boolean = false, required: Boolean = false) = FieldSpec(label, attribute, color, required)

    private class ColorIcon(private val value: () -> String) : Icon {
        override fun getIconWidth() = JBUI.scale(16)
        override fun getIconHeight() = JBUI.scale(16)
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            g.color = parseColor(value()) ?: JBColor.GRAY
            g.fillRoundRect(x, y, iconWidth, iconHeight, JBUI.scale(4), JBUI.scale(4))
            g.color = JBColor.border()
            g.drawRoundRect(x, y, iconWidth - 1, iconHeight - 1, JBUI.scale(4), JBUI.scale(4))
        }
    }

    private companion object {
        fun parseColor(value: String): Color? {
            val hex = value.trim().takeIf { it.startsWith('#') }?.drop(1) ?: return null
            return runCatching { when (hex.length) {
                3 -> Color("${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}".toInt(16))
                6 -> Color(hex.toInt(16))
                8 -> Color(hex.toLong(16).toInt(), true)
                else -> null
            } }.getOrNull()
        }
    }
}
