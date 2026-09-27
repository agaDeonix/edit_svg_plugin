package com.editsvg.vector.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

class OptimizeVectorDrawableAction : AnAction(), DumbAware {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val ok =
            project != null &&
                vf != null &&
                "xml".equals(vf.extension, ignoreCase = true) &&
                isVectorDrawable(project, vf)
        e.presentation.isEnabledAndVisible = ok
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val xml = PsiManager.getInstance(project).findFile(vf) as? XmlFile ?: return
        VectorFileOptimizer.optimizeAllPaths(project, xml)
    }

    private fun isVectorDrawable(project: com.intellij.openapi.project.Project, vf: com.intellij.openapi.vfs.VirtualFile): Boolean {
        val psi = PsiManager.getInstance(project).findFile(vf) as? XmlFile ?: return false
        return "vector".equals(psi.rootTag?.localName, ignoreCase = true)
    }
}
