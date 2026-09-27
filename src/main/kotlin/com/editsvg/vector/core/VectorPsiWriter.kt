package com.editsvg.vector.core

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.util.PsiTreeUtil

enum class VectorDropPosition { BEFORE, INTO, AFTER }

object VectorPsiWriter {
    fun updatePathData(project: Project, xmlFile: XmlFile, pathIndex: Int, newPathData: PathData) {
        val root = xmlFile.rootTag ?: return
        val tag = findPathTagByIndex(root, pathIndex) ?: return
        WriteCommandAction.runWriteCommandAction(
            project,
            "Update pathData",
            "Vector Drawable Editor",
            {
                setOrRemove(tag, "pathData", newPathData.serialize())
            },
            xmlFile,
        )
    }

    fun updatePathData(project: Project, xmlFile: XmlFile, elementKey: String, newPathData: PathData) {
        val tag = findTagByKey(xmlFile.rootTag ?: return, elementKey) ?: return
        WriteCommandAction.runWriteCommandAction(project, "Update pathData", "Vector Drawable Editor", {
            setOrRemove(tag, "pathData", newPathData.serialize())
        }, xmlFile)
    }

    fun updateElementAttributes(project: Project, xmlFile: XmlFile, elementKey: String, updates: Map<String, String?>) {
        WriteCommandAction.runWriteCommandAction(project, "Update vector properties", "Vector Drawable Editor", {
            val tag = findTagByKey(xmlFile.rootTag ?: return@runWriteCommandAction, elementKey) ?: return@runWriteCommandAction
            updates.forEach { (name, value) -> setOrRemove(tag, name, value) }
        }, xmlFile)
    }

    fun group(project: Project, xmlFile: XmlFile, elementKey: String): Boolean = structuralEdit(
        project, xmlFile, "Group vector element",
    ) { root ->
        val tag = findTagByKey(root, elementKey) ?: return@structuralEdit false
        val parent = tag.parentTag ?: return@structuralEdit false
        val group = XmlElementFactory.getInstance(project).createTagFromText("<group/>")
        val inserted = parent.addBefore(group, tag) as XmlTag
        inserted.add(tag.copy())
        tag.delete()
        true
    }

    fun ungroup(project: Project, xmlFile: XmlFile, elementKey: String): Boolean = structuralEdit(
        project, xmlFile, "Ungroup vector elements",
    ) { root ->
        val group = findTagByKey(root, elementKey)?.takeIf { it.localName.equals("group", true) } ?: return@structuralEdit false
        val parent = group.parentTag ?: return@structuralEdit false
        group.subTags.forEach { child -> parent.addBefore(child.copy(), group) }
        group.delete()
        true
    }

    fun moveOut(project: Project, xmlFile: XmlFile, elementKey: String): Boolean = structuralEdit(
        project, xmlFile, "Move vector element out of group",
    ) { root ->
        val tag = findTagByKey(root, elementKey) ?: return@structuralEdit false
        val group = tag.parentTag?.takeIf { it.localName.equals("group", true) } ?: return@structuralEdit false
        val grandParent = group.parentTag ?: return@structuralEdit false
        grandParent.addAfter(tag.copy(), group)
        tag.delete()
        true
    }

    fun delete(project: Project, xmlFile: XmlFile, elementKey: String): Boolean = structuralEdit(
        project, xmlFile, "Delete vector element",
    ) { root ->
        val tag = findTagByKey(root, elementKey) ?: return@structuralEdit false
        if (tag == root) return@structuralEdit false
        tag.delete()
        true
    }

    fun move(
        project: Project,
        xmlFile: XmlFile,
        sourceKey: String,
        targetKey: String,
        position: VectorDropPosition,
    ): Boolean = structuralEdit(project, xmlFile, "Move vector element") { root ->
        val source = findTagByKey(root, sourceKey) ?: return@structuralEdit false
        val target = findTagByKey(root, targetKey) ?: return@structuralEdit false
        if (source == root || source == target || PsiTreeUtil.isAncestor(source, target, false)) return@structuralEdit false
        when (position) {
            VectorDropPosition.INTO -> {
                if (!target.localName.equals("group", true) && target != root) return@structuralEdit false
                target.add(source.copy())
            }
            VectorDropPosition.BEFORE -> target.parentTag?.addBefore(source.copy(), target) ?: return@structuralEdit false
            VectorDropPosition.AFTER -> target.parentTag?.addAfter(source.copy(), target) ?: return@structuralEdit false
        }
        source.delete()
        true
    }

    fun updatePathAttributes(
        project: Project,
        xmlFile: XmlFile,
        pathIndex: Int,
        fillColor: String?,
        strokeColor: String?,
        strokeWidth: String?,
        viewportWidth: Float?,
        viewportHeight: Float?,
    ) {
        WriteCommandAction.runWriteCommandAction(
            project,
            "Update vector drawable",
            "Vector Drawable Editor",
            {
                val root = xmlFile.rootTag ?: return@runWriteCommandAction
                if (viewportWidth != null) {
                    setOrRemove(root, "viewportWidth", viewportWidth.toString())
                }
                if (viewportHeight != null) {
                    setOrRemove(root, "viewportHeight", viewportHeight.toString())
                }
                val tag = findPathTagByIndex(root, pathIndex) ?: return@runWriteCommandAction
                setOrRemove(tag, "fillColor", fillColor)
                setOrRemove(tag, "strokeColor", strokeColor)
                setOrRemove(tag, "strokeWidth", strokeWidth)
            },
            xmlFile,
        )
    }

    private fun setOrRemove(tag: XmlTag, name: String, value: String?) {
        val existing = tag.getAttribute(name, AndroidNamespaces.ANDROID_URI)
        if (value == null) {
            existing?.delete()
            return
        }
        if (existing != null) {
            existing.setValue(value)
            return
        }
        val root = (tag.containingFile as? XmlFile)?.rootTag
        val prefix = root?.getPrefixByNamespace(AndroidNamespaces.ANDROID_URI).orEmpty().ifEmpty { "android" }
        tag.setAttribute("$prefix:$name", value)
    }

    private inline fun structuralEdit(
        project: Project,
        xmlFile: XmlFile,
        commandName: String,
        crossinline edit: (XmlTag) -> Boolean,
    ): Boolean {
        var changed = false
        WriteCommandAction.runWriteCommandAction(project, commandName, "Vector Drawable Editor", {
            val root = xmlFile.rootTag ?: return@runWriteCommandAction
            changed = edit(root)
        }, xmlFile)
        return changed
    }

    private fun findTagByKey(root: XmlTag, key: String): XmlTag? {
        if (key == "vector") return root
        val structuralPath = key.substringAfterLast('@', missingDelimiterValue = "")
        if (!structuralPath.startsWith("vector/")) return null
        var current = root
        for (part in structuralPath.removePrefix("vector/").split('/')) {
            val index = part.toIntOrNull() ?: return null
            current = current.subTags.getOrNull(index) ?: return null
        }
        return current
    }

    private fun findPathTagByIndex(vectorRoot: XmlTag, index: Int): XmlTag? {
        var i = 0
        fun walk(tag: XmlTag): XmlTag? {
            for (sub in tag.subTags) {
                when (sub.localName.lowercase()) {
                    "path" -> {
                        if (i == index) return sub
                        i++
                    }

                    "group" -> {
                        val found = walk(sub)
                        if (found != null) return found
                    }
                }
            }
            return null
        }
        return walk(vectorRoot)
    }
}
