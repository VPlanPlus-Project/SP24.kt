import kotlinx.coroutines.runBlocking
import plus.vplan.lib.sp24.source.Response
import kotlin.test.Test

class TimetableTest {
    val client = clientForSp24Id("10063764")!!

    @Test
    fun `Get timetable`() = runBlocking {
        val timetable = (client.timetable.getTimetable(1) as Response.Success).data
        assert(timetable.lessons.isNotEmpty()) { "Timetable should not be empty" }
        println(timetable)
    }
}