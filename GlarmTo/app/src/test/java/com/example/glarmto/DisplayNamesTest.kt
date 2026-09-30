package com.example.glarmto

import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.Goals
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.labelRes
import com.example.glarmto.testsupport.TestTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stored names (muscle, equipment, goal) stay English; what the user reads follows the language. */
class DisplayNamesTest {

    private val en = TestTexts.english
    private val th = TestTexts.thai
    private val thaiChar = Regex("[฀-๿]")

    @Test
    fun `English labels are the familiar names`() {
        assertEquals(listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core"), MuscleGroup.values().map { en.get(it.labelRes()) })
        assertEquals(
            listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "None"),
            Equipment.values().map { en.get(it.labelRes()) }
        )
    }

    @Test
    fun `English labels match the stored enum names so nothing changes for English users`() {
        MuscleGroup.values().forEach { assertEquals(it.name, en.get(it.labelRes())) }
        Equipment.values().filter { it != Equipment.None }.forEach { assertEquals(it.name, en.get(it.labelRes())) }
    }

    @Test
    fun `every muscle group has Thai text`() {
        MuscleGroup.values().forEach {
            val text = th.get(it.labelRes())
            assertTrue("$it -> $text", thaiChar.containsMatchIn(text))
        }
    }

    @Test
    fun `every piece of equipment has Thai text`() {
        Equipment.values().forEach {
            val text = th.get(it.labelRes())
            assertTrue("$it -> $text", thaiChar.containsMatchIn(text))
        }
    }

    @Test
    fun `muscle labels are distinct from each other in both languages`() {
        assertEquals(MuscleGroup.values().size, MuscleGroup.values().map { en.get(it.labelRes()) }.toSet().size)
        assertEquals(MuscleGroup.values().size, MuscleGroup.values().map { th.get(it.labelRes()) }.toSet().size)
    }

    @Test
    fun `equipment labels are distinct from each other in both languages`() {
        assertEquals(Equipment.values().size, Equipment.values().map { en.get(it.labelRes()) }.toSet().size)
        assertEquals(Equipment.values().size, Equipment.values().map { th.get(it.labelRes()) }.toSet().size)
    }

    @Test
    fun `goals keep their stored values`() {
        assertEquals(listOf("Cut", "Maintain", "Bulk"), Goals.all)
        assertEquals("Cut", Goals.CUT)
        assertEquals("Maintain", Goals.MAINTAIN)
        assertEquals("Bulk", Goals.BULK)
    }

    @Test
    fun `every goal has a label in both languages`() {
        Goals.all.forEach { goal ->
            val id = Goals.labelRes(goal)!!
            assertEquals(goal, en.get(id))
            assertTrue(thaiChar.containsMatchIn(th.get(id)))
            assertNotEquals(en.get(id), th.get(id))
        }
    }

    @Test
    fun `an unknown goal has no label so the raw value is shown instead`() {
        assertNull(Goals.labelRes("Recomp"))
        assertNull(Goals.labelRes(""))
        assertNull("labels are case sensitive, like the stored value", Goals.labelRes("cut"))
    }
}
