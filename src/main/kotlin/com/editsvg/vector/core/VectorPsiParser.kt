package com.editsvg.vector.core

import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.awt.geom.AffineTransform

object VectorPsiParser {
    fun parse(xmlFile: XmlFile): VectorDrawableRoot? {
        val root = xmlFile.rootTag ?: return null
        if (!"vector".equals(root.localName, ignoreCase = true)) return null
        val viewportWidth = android(root, "viewportWidth")?.toFloatOrNull() ?: return null
        val viewportHeight = android(root, "viewportHeight")?.toFloatOrNull() ?: return null
        val paths = ArrayList<VectorPathItem>()
        val counters = Counters()
        val children = root.subTags.mapIndexedNotNull { index, tag ->
            parseElement(tag, "vector/$index", AffineTransform(), paths, counters)
        }
        val hierarchy = VectorElementNode(
            key = "vector",
            type = VectorElementType.VECTOR,
            displayName = android(root, "name") ?: "vector",
            attributes = attributes(root, VECTOR_ATTRIBUTES),
            children = children,
        )
        return VectorDrawableRoot(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            widthDp = android(root, "width"),
            heightDp = android(root, "height"),
            hierarchy = hierarchy,
            paths = paths,
        )
    }

    private fun parseElement(
        tag: XmlTag,
        structuralKey: String,
        parentTransform: AffineTransform,
        paths: MutableList<VectorPathItem>,
        counters: Counters,
    ): VectorElementNode? = when (tag.localName.lowercase()) {
        "group" -> {
            val ordinal = ++counters.groups
            val name = android(tag, "name")
            val key = semanticKey("group", name, structuralKey)
            fun number(attribute: String, default: Double) = android(tag, attribute)?.toDoubleOrNull() ?: default
            val local = VectorTransforms.group(
                rotation = number("rotation", 0.0),
                pivotX = number("pivotX", 0.0),
                pivotY = number("pivotY", 0.0),
                scaleX = number("scaleX", 1.0),
                scaleY = number("scaleY", 1.0),
                translateX = number("translateX", 0.0),
                translateY = number("translateY", 0.0),
            )
            val transform = VectorTransforms.combine(parentTransform, local)
            VectorElementNode(
                key = key,
                type = VectorElementType.GROUP,
                displayName = name ?: "group $ordinal",
                attributes = attributes(tag, GROUP_ATTRIBUTES),
                children = tag.subTags.mapIndexedNotNull { index, child ->
                    parseElement(child, "$structuralKey/$index", transform, paths, counters)
                },
            )
        }

        "path" -> {
            val ordinal = ++counters.paths
            val name = android(tag, "name")
            val key = semanticKey("path", name, structuralKey)
            val pathDataText = android(tag, "pathData")
            var warning: String? = null
            val parsed = try {
                pathDataText?.let(PathDataParser::parse)
            } catch (error: IllegalArgumentException) {
                warning = error.message ?: "Invalid pathData"
                null
            }
            val pathIndex = if (parsed != null) {
                val editabilityReason = when {
                    parsed.commands.any { it is PathCommand.SmoothCubicTo || it is PathCommand.SmoothQuadTo || it is PathCommand.ArcTo } -> "Smooth curves and arcs are view-only"
                    !parentTransform.isIdentity -> "Paths inside transformed groups are view-only"
                    else -> null
                }
                paths.add(
                    VectorPathItem(
                        elementKey = key,
                        id = name ?: "path $ordinal",
                        pathTagName = tag.name ?: "path",
                        pathData = parsed,
                        fillColor = android(tag, "fillColor"),
                        strokeColor = android(tag, "strokeColor"),
                        strokeWidth = android(tag, "strokeWidth"),
                        fillAlpha = android(tag, "fillAlpha"),
                        strokeAlpha = android(tag, "strokeAlpha"),
                        fillType = android(tag, "fillType"),
                        transform = AffineTransform(parentTransform),
                        editabilityReason = editabilityReason,
                    ),
                )
                paths.lastIndex
            } else null
            VectorElementNode(
                key = key,
                type = VectorElementType.PATH,
                displayName = name ?: "path $ordinal",
                attributes = attributes(tag, PATH_ATTRIBUTES),
                pathIndex = pathIndex,
                warning = warning ?: paths.getOrNull(pathIndex ?: -1)?.editabilityReason,
            )
        }

        "clip-path" -> {
            val ordinal = ++counters.clips
            val name = android(tag, "name")
            VectorElementNode(
                key = semanticKey("clip", name, structuralKey),
                type = VectorElementType.CLIP_PATH,
                displayName = name ?: "clip-path $ordinal",
                attributes = attributes(tag, CLIP_ATTRIBUTES),
                warning = "Clip paths are preview-only",
            )
        }

        else -> null
    }

    private fun android(tag: XmlTag, name: String): String? =
        tag.getAttributeValue(name, AndroidNamespaces.ANDROID_URI)

    private fun attributes(tag: XmlTag, names: Set<String>): Map<String, String> =
        names.mapNotNull { name -> android(tag, name)?.let { name to it } }.toMap()

    private fun semanticKey(type: String, name: String?, structuralKey: String): String =
        if (name.isNullOrBlank()) "$type@$structuralKey" else "$type:$name@$structuralKey"

    private class Counters(var groups: Int = 0, var paths: Int = 0, var clips: Int = 0)

    private val VECTOR_ATTRIBUTES = setOf("name", "width", "height", "viewportWidth", "viewportHeight", "alpha", "tint", "tintMode", "autoMirrored")
    private val GROUP_ATTRIBUTES = setOf("name", "rotation", "pivotX", "pivotY", "scaleX", "scaleY", "translateX", "translateY")
    private val PATH_ATTRIBUTES = setOf("name", "pathData", "fillColor", "fillAlpha", "strokeColor", "strokeAlpha", "strokeWidth", "strokeLineCap", "strokeLineJoin", "strokeMiterLimit", "trimPathStart", "trimPathEnd", "trimPathOffset", "fillType")
    private val CLIP_ATTRIBUTES = setOf("name", "pathData", "fillType")
}
