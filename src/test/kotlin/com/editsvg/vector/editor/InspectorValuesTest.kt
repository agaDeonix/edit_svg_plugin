package com.editsvg.vector.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InspectorValuesTest {
    @Test
    fun `blank optional value becomes attribute removal`() {
        assertNull(InspectorValues.optionalAttribute("   "))
        assertEquals("@color/icon", InspectorValues.optionalAttribute(" @color/icon "))
    }

    @Test
    fun `viewport must be finite and positive`() {
        assertTrue(InspectorValues.isPositiveNumber("24"))
        assertFalse(InspectorValues.isPositiveNumber("0"))
        assertFalse(InspectorValues.isPositiveNumber("NaN"))
        assertFalse(InspectorValues.isPositiveNumber("abc"))
    }

    @Test
    fun `stroke width allows zero but not negative`() {
        assertTrue(InspectorValues.isNonNegativeNumber("0"))
        assertFalse(InspectorValues.isNonNegativeNumber("-1"))
        assertTrue(InspectorValues.isNonNegativeNumber(""))
    }

    @Test
    fun `unchanged draft does not request another write`() {
        val baseline = InspectorDraft("#112233", "#445566", "2", "24", "24")

        assertFalse(baseline.hasChangesFrom(baseline))
        assertTrue(baseline.copy(strokeWidth = "3").hasChangesFrom(baseline))
    }

    @Test
    fun `draft reports first invalid numeric field`() {
        val invalidStroke = InspectorDraft("", "", "-1", "24", "24")
        val invalidViewport = InspectorDraft("", "", "1", "0", "24")

        assertEquals("Stroke width must be zero or greater", invalidStroke.validationError())
        assertEquals("Viewport width must be greater than zero", invalidViewport.validationError())
    }

    @Test
    fun `android colors accept hex resources and theme attributes`() {
        assertNull(InspectorValues.attributeError("fillColor", "#ff00aa"))
        assertNull(InspectorValues.attributeError("fillColor", "@color/icon"))
        assertNull(InspectorValues.attributeError("fillColor", "?attr/colorPrimary"))
        assertEquals("Enter a hex color or Android resource", InspectorValues.attributeError("fillColor", "purple"))
    }

    @Test
    fun `alpha and path data are validated before write`() {
        assertNull(InspectorValues.attributeError("fillAlpha", "0.5"))
        assertEquals("Alpha must be between 0 and 1", InspectorValues.attributeError("fillAlpha", "2"))
        assertNull(InspectorValues.attributeError("pathData", "M0,0 L2,2"))
        assertEquals("Invalid pathData", InspectorValues.attributeError("pathData", "M0,0 A"))
    }
}
