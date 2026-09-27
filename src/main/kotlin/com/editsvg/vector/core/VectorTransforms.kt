package com.editsvg.vector.core

import java.awt.geom.AffineTransform

object VectorTransforms {
    fun group(
        rotation: Double = 0.0,
        pivotX: Double = 0.0,
        pivotY: Double = 0.0,
        scaleX: Double = 1.0,
        scaleY: Double = 1.0,
        translateX: Double = 0.0,
        translateY: Double = 0.0,
    ): AffineTransform = AffineTransform().apply {
        translate(translateX + pivotX, translateY + pivotY)
        rotate(Math.toRadians(rotation))
        scale(scaleX, scaleY)
        translate(-pivotX, -pivotY)
    }

    fun combine(parent: AffineTransform, child: AffineTransform): AffineTransform =
        AffineTransform(parent).apply { concatenate(child) }
}
