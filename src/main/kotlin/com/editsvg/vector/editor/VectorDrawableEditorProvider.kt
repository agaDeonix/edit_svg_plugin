package com.editsvg.vector.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

class VectorDrawableEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!file.isValid || file.isDirectory) return false
        if (!"xml".equals(file.extension, ignoreCase = true)) return false
        val psi = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: return false
        return "vector".equals(psi.rootTag?.localName, ignoreCase = true)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return VectorDrawableFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "vector-drawable-visual-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
