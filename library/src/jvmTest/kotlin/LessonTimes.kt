import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.source.Response

class LessonTimesTest {

    private val client = clientForSp24Id("20331941")!!
    @Test
    fun testLessonTimesNotEmpty() = runBlocking {
        val lessonTimes = (client.lessonTime.getLessonTime(null) as Response.Success).data
            .groupBy { it.className }
        val classesWithoutLessonTimes = (client.getAllClassesIntelligent() as Response.Success).data
            .map { it.name }
            .filter { it !in lessonTimes.keys }
        lessonTimes.forEach { (key, value) ->
            println(key)
            value.forEach { println("  - $it") }
        }
        println("Classes without lesson times: ${classesWithoutLessonTimes.joinToString()}")
        assertTrue(lessonTimes.isNotEmpty())
    }
}