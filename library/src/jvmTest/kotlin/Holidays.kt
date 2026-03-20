import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.source.Response

class HolidaysTest {
    @Test
    fun testGetHolidays() = runBlocking {
        val holidays = (stundenplan24Client.holiday.getHolidays() as Response.Success).data
        holidays.forEach { date ->
            println(date)
        }
        assertTrue(holidays.isNotEmpty())
    }
}