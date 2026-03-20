import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.source.NamedEntity
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.ValueSource

class SchoolEntityTest {

    val client = clientForSp24Id("10063764")!!

    @Test
    fun getClasses() = runBlocking {
        val response = client.getAllClassesIntelligent() as? Response.Success<Set<NamedEntity>>
        assertNotNull(response)
        val classes = response!!.data
        assertTrue(classes.isNotEmpty())
        println("Is Common | Is Indexed | Should show in UI | Name")
        classes.forEach {
            val shouldShowInUi = it.isCommon() || it.source == ValueSource.Indexed
            println("${boolToEmoji(it.isCommon())} ${boolToEmoji(it.source == ValueSource.Indexed)} ${boolToEmoji(shouldShowInUi)} ${it.name}")
            assertNotEquals("", it.name)
        }
    }

    @Test
    fun getTeachers() = runBlocking {
        val response = client.getAllTeachersIntelligent() as? Response.Success<Set<NamedEntity>>
        assertNotNull(response)
        val teachers = response!!.data
        assertTrue(teachers.isNotEmpty())
        teachers.forEach {
            println("${boolToEmoji(it.isCommon())} ${it.name}")
            assertNotEquals("", it.name)
        }
    }

    @Test
    fun getRooms() = runBlocking {
        val response = client.getAllRoomsIntelligent() as? Response.Success<Set<NamedEntity>>
        assertNotNull(response)
        val rooms = response!!.data
        assertTrue(rooms.isNotEmpty())
        rooms.forEach {
            println("${boolToEmoji(it.isCommon())} ${it.name}")
            assertNotEquals("", it.name)
        }
    }

    private fun boolToEmoji(value: Boolean): String {
        return if (value) "✅" else "❌"
    }
}