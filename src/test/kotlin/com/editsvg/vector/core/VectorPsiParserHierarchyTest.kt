package com.editsvg.vector.core

import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VectorPsiParserHierarchyTest : BasePlatformTestCase() {
    fun testParsesCompleteNestedHierarchy() {
        val xml = myFixture.configureByText(
            "icon.xml",
            """
            <vector xmlns:android="${AndroidNamespaces.ANDROID_URI}" android:name="root" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
              <group android:name="face" android:rotation="10">
                <path android:name="circle" android:fillColor="#ff0000" android:pathData="M0,0 L1,1"/>
                <clip-path android:name="mask" android:pathData="M0,0 L2,2"/>
                <group>
                  <path android:pathData="M2,2 L3,3"/>
                </group>
              </group>
            </vector>
            """.trimIndent(),
        ) as XmlFile

        val model = VectorPsiParser.parse(xml)!!
        val face = model.hierarchy.children.single()

        assertEquals(VectorElementType.VECTOR, model.hierarchy.type)
        assertEquals(VectorElementType.GROUP, face.type)
        assertEquals("face", face.displayName)
        assertEquals(listOf(VectorElementType.PATH, VectorElementType.CLIP_PATH, VectorElementType.GROUP), face.children.map { it.type })
        assertEquals("group 2", face.children.last().displayName)
        assertEquals("path 2", face.children.last().children.single().displayName)
        assertEquals(2, model.paths.size)
        assertEquals(face.children.first().key, model.paths.first().elementKey)
    }
}
