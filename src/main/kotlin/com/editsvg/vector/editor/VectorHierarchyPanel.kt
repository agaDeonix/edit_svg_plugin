package com.editsvg.vector.editor

import com.editsvg.vector.core.VectorDropPosition
import com.editsvg.vector.core.VectorElementNode
import com.editsvg.vector.core.VectorElementType
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class VectorHierarchyPanel : JPanel(BorderLayout()) {
    var onSelectionChanged: ((String) -> Unit)? = null
    var onGroup: ((String) -> Unit)? = null
    var onUngroup: ((String) -> Unit)? = null
    var onMoveOut: ((String) -> Unit)? = null
    var onDelete: ((String) -> Unit)? = null
    var onMove: ((String, String, VectorDropPosition) -> Unit)? = null

    private var hierarchy: VectorElementNode? = null
    private var updatingSelection = false
    private val tree = Tree(DefaultMutableTreeNode("vector"))

    init {
        background = JBColor.PanelBackground
        border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(6, 9, 4, 5)
            add(JBLabel("Hierarchy").apply { font = font.deriveFont(java.awt.Font.BOLD) }, BorderLayout.WEST)
            add(JButton("⋮").apply {
                isFocusable = false
                margin = JBUI.emptyInsets()
                toolTipText = "Hierarchy actions"
                addActionListener { showActions(this, 0, height) }
            }, BorderLayout.EAST)
        }
        add(header, BorderLayout.NORTH)
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = ElementRenderer()
        tree.rowHeight = JBUI.scale(24)
        tree.dragEnabled = true
        tree.dropMode = javax.swing.DropMode.ON_OR_INSERT
        tree.transferHandler = TreeTransferHandler()
        tree.addTreeSelectionListener(::selectionChanged)
        tree.componentPopupMenu = JPopupMenu().apply {
            addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) { rebuildMenu(this@apply) }
                override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) = Unit
                override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) = Unit
            })
        }
        add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
    }

    fun bind(root: VectorElementNode?, selectedKey: String?) {
        val expanded = expandedKeys()
        hierarchy = root
        val swingRoot = root?.let(::toSwingNode) ?: DefaultMutableTreeNode("vector")
        tree.model = DefaultTreeModel(swingRoot)
        updatingSelection = true
        try {
            restoreExpanded(expanded.ifEmpty { collectGroupKeys(root) })
            selectKey(selectedKey ?: root?.key)
        } finally {
            updatingSelection = false
        }
    }

    fun selectedKey(): String? = selectedNode()?.key

    private fun selectionChanged(event: TreeSelectionEvent) {
        if (!updatingSelection) selectedNode()?.key?.let { onSelectionChanged?.invoke(it) }
    }

    private fun selectedNode(): VectorElementNode? =
        (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? VectorElementNode

    private fun toSwingNode(node: VectorElementNode): DefaultMutableTreeNode =
        DefaultMutableTreeNode(node).apply { node.children.forEach { add(toSwingNode(it)) } }

    private fun selectKey(key: String?) {
        if (key == null) return
        findTreePath(key)?.let { tree.selectionPath = it; tree.scrollPathToVisible(it) }
    }

    private fun findTreePath(key: String): TreePath? {
        val root = tree.model.root as? DefaultMutableTreeNode ?: return null
        val enumeration = root.preorderEnumeration()
        while (enumeration.hasMoreElements()) {
            val candidate = enumeration.nextElement() as DefaultMutableTreeNode
            if ((candidate.userObject as? VectorElementNode)?.key == key) return TreePath(candidate.path)
        }
        return null
    }

    fun expandedElementKeys(): Set<String> = expandedKeys()

    fun restoreExpandedElementKeys(keys: Set<String>) {
        for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row)
        restoreExpanded(keys)
    }

    private fun expandedKeys(): Set<String> = buildSet {
        for (row in 0 until tree.rowCount) {
            val path = tree.getPathForRow(row)
            if (tree.isExpanded(path)) ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? VectorElementNode)?.key?.let(::add)
        }
    }

    private fun restoreExpanded(keys: Set<String>) { keys.forEach { findTreePath(it)?.let(tree::expandPath) } }

    private fun collectGroupKeys(root: VectorElementNode?): Set<String> = buildSet {
        fun visit(node: VectorElementNode) {
            if (node.type == VectorElementType.VECTOR || node.type == VectorElementType.GROUP) add(node.key)
            node.children.forEach(::visit)
        }
        root?.let(::visit)
    }

    private fun showActions(component: JComponent, x: Int, y: Int) {
        JPopupMenu().also { rebuildMenu(it) }.show(component, x, y)
    }

    private fun rebuildMenu(menu: JPopupMenu) {
        menu.removeAll()
        val selected = selectedNode()
        fun item(label: String, enabled: Boolean = true, action: () -> Unit) {
            menu.add(JMenuItem(label).apply { isEnabled = enabled; addActionListener { action() } })
        }
        item("Group", selected != null && selected.type != VectorElementType.VECTOR) { selected?.key?.let { onGroup?.invoke(it) } }
        item("Ungroup", selected?.type == VectorElementType.GROUP) { selected?.key?.let { onUngroup?.invoke(it) } }
        val parentType = (tree.selectionPath?.parentPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject.let { it as? VectorElementNode }?.type
        item("Move out of group", parentType == VectorElementType.GROUP) { selected?.key?.let { onMoveOut?.invoke(it) } }
        menu.addSeparator()
        item("Delete", selected != null && selected.type != VectorElementType.VECTOR) {
            selected ?: return@item
            if (selected.type == VectorElementType.GROUP && selected.children.isNotEmpty()) {
                val answer = Messages.showYesNoDialog(this, "Delete group and all its children?", "Delete Group", Messages.getQuestionIcon())
                if (answer != Messages.YES) return@item
            }
            onDelete?.invoke(selected.key)
        }
    }

    private inner class TreeTransferHandler : TransferHandler() {
        override fun getSourceActions(c: JComponent): Int = MOVE
        override fun createTransferable(c: JComponent) = selectedKey()?.let(::StringSelection)

        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false
            val sourceKey = runCatching { support.transferable.getTransferData(DataFlavor.stringFlavor) as String }.getOrNull() ?: return false
            if (sourceKey == "vector") return false
            val (target, _) = dropTarget(support) ?: return false
            val source = hierarchy?.find(sourceKey) ?: return false
            return source.key != target.key && !source.contains(target.key)
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) return false
            val sourceKey = support.transferable.getTransferData(DataFlavor.stringFlavor) as String
            val (target, position) = dropTarget(support) ?: return false
            onMove?.invoke(sourceKey, target.key, position)
            return true
        }

        private fun dropTarget(support: TransferSupport): Pair<VectorElementNode, VectorDropPosition>? {
            val location = support.dropLocation as? JTree.DropLocation ?: return null
            val path = location.path ?: return null
            val swingNode = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
            if (location.childIndex >= 0) {
                val childCount = swingNode.childCount
                if (childCount == 0) return (swingNode.userObject as? VectorElementNode)?.let { it to VectorDropPosition.INTO }
                val targetChild = swingNode.getChildAt(location.childIndex.coerceAtMost(childCount - 1)) as DefaultMutableTreeNode
                val target = targetChild.userObject as? VectorElementNode ?: return null
                return target to if (location.childIndex >= childCount) VectorDropPosition.AFTER else VectorDropPosition.BEFORE
            }
            val target = swingNode.userObject as? VectorElementNode ?: return null
            return target to if (target.type == VectorElementType.GROUP || target.type == VectorElementType.VECTOR) VectorDropPosition.INTO else VectorDropPosition.AFTER
        }
    }

    private class ElementRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
            val component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
            val node = (value as? DefaultMutableTreeNode)?.userObject as? VectorElementNode ?: return component
            text = node.displayName + if (node.warning != null) "  ⚠" else ""
            icon = ElementIcon(node)
            toolTipText = node.warning
            return component
        }
    }

    private class ElementIcon(private val node: VectorElementNode) : Icon {
        override fun getIconWidth(): Int = JBUI.scale(16)
        override fun getIconHeight(): Int = JBUI.scale(16)
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as java.awt.Graphics2D
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                val size = JBUI.scale(11)
                val ox = x + (iconWidth - size) / 2
                val oy = y + (iconHeight - size) / 2
                g2.color = when (node.type) {
                    VectorElementType.VECTOR -> JBColor(Color(0x45, 0x65, 0x83), Color(0x8A, 0xB4, 0xF8))
                    VectorElementType.GROUP -> JBColor(Color(0x7A, 0x5C, 0x00), Color(0xE5, 0xC0, 0x7B))
                    VectorElementType.PATH -> parseColor(node.attributes["fillColor"]) ?: JBColor.GRAY
                    VectorElementType.CLIP_PATH -> JBColor(Color(0x6C, 0x56, 0x80), Color(0xD0, 0xBC, 0xFF))
                }
                when (node.type) {
                    VectorElementType.GROUP -> { g2.drawRect(ox, oy + 2, size, size - 3); g2.drawLine(ox + 1, oy + 2, ox + size / 2, oy + 2) }
                    VectorElementType.PATH -> g2.fillOval(ox, oy, size, size)
                    VectorElementType.CLIP_PATH -> g2.drawOval(ox, oy, size, size)
                    VectorElementType.VECTOR -> { g2.drawRect(ox, oy, size, size); g2.drawLine(ox, oy + size, ox + size, oy) }
                }
            } finally { g2.dispose() }
        }

        private fun parseColor(value: String?): Color? {
            val hex = value?.takeIf { it.startsWith('#') }?.drop(1) ?: return null
            return runCatching { when (hex.length) { 6 -> Color(hex.toInt(16)); 8 -> Color(hex.toLong(16).toInt(), true); else -> null } }.getOrNull()
        }
    }
}
