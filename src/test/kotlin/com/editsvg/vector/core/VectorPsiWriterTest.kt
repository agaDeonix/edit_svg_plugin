package com.editsvg.vector.core

import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VectorPsiWriterTest : BasePlatformTestCase() {
    fun `test changing stroke width preserves android namespace and other attributes`() {
        val xml = myFixture.configureByText(
            "icon.xml",
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp" android:height="24dp"
                android:viewportWidth="24" android:viewportHeight="24">
                <path android:name="body" android:fillColor="#112233"
                    android:strokeColor="#445566" android:strokeWidth="1"
                    android:pathData="M0,0 L10,10" />
            </vector>
            """.trimIndent(),
        ) as XmlFile

        VectorPsiWriter.updatePathAttributes(project, xml, 0, "#112233", "#445566", "2.5", 24f, 24f)

        val text = xml.text
        assertTrue(text, text.contains("android:strokeWidth=\"2.5\""))
        assertEquals(text, 1, Regex("xmlns:android=").findAll(text).count())
        assertFalse(text, text.contains("xmlns=\"${AndroidNamespaces.ANDROID_URI}\""))
        assertTrue(text, text.contains("android:pathData=\"M0,0 L10,10\""))
    }

    fun `test blank optional property removes only its attribute`() {
        val xml = myFixture.configureByText(
            "icon.xml",
            """<vector xmlns:android="${AndroidNamespaces.ANDROID_URI}" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#000000" android:strokeWidth="1" android:pathData="M0,0" /></vector>""",
        ) as XmlFile

        VectorPsiWriter.updatePathAttributes(project, xml, 0, "#000000", null, null, 24f, 24f)

        assertFalse(xml.text, xml.text.contains("strokeWidth"))
        assertTrue(xml.text, xml.text.contains("android:pathData=\"M0,0\""))
    }

    fun `test adding missing stroke width uses existing android prefix`() {
        val xml = myFixture.configureByText(
            "icon.xml",
            """<vector xmlns:android="${AndroidNamespaces.ANDROID_URI}" android:viewportWidth="24" android:viewportHeight="24"><path android:strokeColor="#ffffff" android:pathData="M0,0" /></vector>""",
        ) as XmlFile

        VectorPsiWriter.updatePathAttributes(project, xml, 0, null, "#ffffff", "3", 24f, 24f)

        val text = xml.text
        assertTrue(text, text.contains("android:strokeWidth=\"3\""))
        assertEquals(text, 1, Regex("xmlns:android=").findAll(text).count())
        assertFalse(text, text.contains("xmlns=\"${AndroidNamespaces.ANDROID_URI}\""))
    }

    fun testGroupAndUngroupPreserveChildOrder() {
        val xml = hierarchyFixture()
        val initial = VectorPsiParser.parse(xml)!!
        val secondPath = initial.hierarchy.children[1]

        assertTrue(VectorPsiWriter.group(project, xml, secondPath.key))
        val grouped = VectorPsiParser.parse(xml)!!
        val group = grouped.hierarchy.children[1]
        assertEquals(VectorElementType.GROUP, group.type)
        assertEquals("second", group.children.single().displayName)

        assertTrue(VectorPsiWriter.ungroup(project, xml, group.key))
        val ungrouped = VectorPsiParser.parse(xml)!!
        assertEquals(listOf("first", "second", "third"), ungrouped.hierarchy.children.map { it.displayName })
    }

    fun testMoveIntoGroupAndMoveOut() {
        val xml = hierarchyFixture(withGroup = true)
        var model = VectorPsiParser.parse(xml)!!
        val path = model.hierarchy.children.first()
        val group = model.hierarchy.children[1]

        assertTrue(VectorPsiWriter.move(project, xml, path.key, group.key, VectorDropPosition.INTO))
        model = VectorPsiParser.parse(xml)!!
        val movedPath = model.hierarchy.children[0].children.first { it.displayName == "first" }
        assertTrue(VectorPsiWriter.moveOut(project, xml, movedPath.key))

        model = VectorPsiParser.parse(xml)!!
        assertEquals(listOf(VectorElementType.GROUP, VectorElementType.PATH, VectorElementType.PATH), model.hierarchy.children.map { it.type })
        assertEquals("first", model.hierarchy.children[1].displayName)
    }

    fun testMoveRejectsDroppingGroupIntoDescendant() {
        val xml = hierarchyFixture(withGroup = true)
        val model = VectorPsiParser.parse(xml)!!
        val group = model.hierarchy.children[1]
        val child = group.children.single()

        assertFalse(VectorPsiWriter.move(project, xml, group.key, child.key, VectorDropPosition.INTO))
    }

    fun testDeleteRemovesOnlySelectedElement() {
        val xml = hierarchyFixture()
        val second = VectorPsiParser.parse(xml)!!.hierarchy.children[1]

        assertTrue(VectorPsiWriter.delete(project, xml, second.key))

        assertEquals(listOf("first", "third"), VectorPsiParser.parse(xml)!!.hierarchy.children.map { it.displayName })
    }

    fun testDeleteRejectsVectorRoot() {
        val xml = hierarchyFixture()
        val before = xml.text

        assertFalse(VectorPsiWriter.delete(project, xml, "vector"))
        assertEquals(before, xml.text)
    }

    fun testContextPropertiesPreserveResourceReferences() {
        val xml = hierarchyFixture()
        val path = VectorPsiParser.parse(xml)!!.hierarchy.children.first()

        VectorPsiWriter.updateElementAttributes(
            project,
            xml,
            path.key,
            mapOf("fillColor" to "@color/vector_fill", "strokeColor" to "?attr/colorAccent", "strokeWidth" to "2"),
        )

        assertTrue(xml.text, xml.text.contains("android:fillColor=\"@color/vector_fill\""))
        assertTrue(xml.text, xml.text.contains("android:strokeColor=\"?attr/colorAccent\""))
        assertTrue(xml.text, xml.text.contains("android:strokeWidth=\"2\""))
        assertEquals(xml.text, 1, Regex("xmlns:android=").findAll(xml.text).count())
    }

    private fun hierarchyFixture(withGroup: Boolean = false): XmlFile {
        val middle = if (withGroup) {
            "<group android:name=\"container\"><path android:name=\"inside\" android:pathData=\"M1,1\"/></group>"
        } else {
            "<path android:name=\"second\" android:pathData=\"M1,1\"/>"
        }
        return myFixture.configureByText(
            "tree.xml",
            """<vector xmlns:android="${AndroidNamespaces.ANDROID_URI}" android:viewportWidth="24" android:viewportHeight="24"><path android:name="first" android:pathData="M0,0"/>$middle<path android:name="third" android:pathData="M2,2"/></vector>""",
        ) as XmlFile
    }
}
