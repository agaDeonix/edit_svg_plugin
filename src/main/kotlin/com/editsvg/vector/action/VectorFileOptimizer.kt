package com.editsvg.vector.action

import com.editsvg.vector.core.AndroidNamespaces
import com.editsvg.vector.core.PathDataParser
import com.editsvg.vector.core.VectorOptimizer
import com.editsvg.vector.core.serialize
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

object VectorFileOptimizer {
    fun optimizeAllPaths(project: Project, xmlFile: XmlFile) {
        val root = xmlFile.rootTag ?: return
        WriteCommandAction.runWriteCommandAction(
            project,
            "Optimize vector drawable",
            "Vector Drawable Editor",
            {
                walk(root)
            },
            xmlFile,
        )
    }

    private fun walk(tag: XmlTag) {
        for (sub in tag.subTags) {
            when (sub.localName.lowercase()) {
                "path" -> {
                    val pdStr = sub.getAttributeValue("pathData", AndroidNamespaces.ANDROID_URI) ?: continue
                    try {
                        val optimized = VectorOptimizer.optimizePathData(PathDataParser.parse(pdStr))
                        sub.setAttribute("pathData", optimized.serialize(), AndroidNamespaces.ANDROID_URI)
                    } catch (_: IllegalArgumentException) {
                        // skip
                    }
                }

                "group" -> walk(sub)
                else -> {}
            }
        }
    }
}
