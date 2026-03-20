import kotlinx.coroutines.runBlocking
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.extension.SchoolDoesNotSupportWeeks
import kotlin.test.Test


class WeekTest {

    val client = clientForSp24Id("20331941")!!

    @Test
    fun getAllWeeks() = runBlocking {
        val weeks = try {
            (client.week.getWeeks() as Response.Success).data
        } catch (_: SchoolDoesNotSupportWeeks) {
            println("School ${client.authentication.sp24SchoolId} does not support weeks.")
            return@runBlocking
        }
        weeks.forEach {
            println(it)
        }
    }
}