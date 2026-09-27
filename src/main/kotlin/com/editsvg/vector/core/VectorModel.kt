package com.editsvg.vector.core

import java.awt.geom.AffineTransform

enum class VectorElementType { VECTOR, GROUP, PATH, CLIP_PATH }

data class VectorElementNode(
    val key: String,
    val type: VectorElementType,
    val displayName: String,
    val attributes: Map<String, String>,
    val children: List<VectorElementNode> = emptyList(),
    val pathIndex: Int? = null,
    val warning: String? = null,
) {
    fun find(key: String): VectorElementNode? =
        if (this.key == key) this else children.firstNotNullOfOrNull { it.find(key) }

    fun contains(descendantKey: String): Boolean = find(descendantKey) != null
}

data class VectorPathItem(
    val elementKey: String,
    val id: String,
    val pathTagName: String,
    val pathData: PathData,
    val fillColor: String?,
    val strokeColor: String?,
    val strokeWidth: String?,
    val fillAlpha: String?,
    val strokeAlpha: String?,
    val fillType: String?,
    val transform: AffineTransform = AffineTransform(),
    val editabilityReason: String? = null,
)

data class VectorDrawableRoot(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val widthDp: String?,
    val heightDp: String?,
    val hierarchy: VectorElementNode,
    val paths: List<VectorPathItem>,
)
