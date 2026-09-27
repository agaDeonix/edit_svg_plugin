package com.editsvg.vector.core

import java.awt.geom.AffineTransform

object CanvasRendering {
    fun worldTransform(componentTransform: AffineTransform, viewport: CanvasViewport): AffineTransform =
        AffineTransform(componentTransform).apply {
            translate(viewport.originX, viewport.originY)
            scale(viewport.scale, viewport.scale)
        }
}
