import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import plus.vplan.lib.sp24.source.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Suppress("NewApi")
class WPlanTests {

    @Test
    fun `Get wplan base`() {
        runBlocking {
            val baseData = wPlanStundenplan24Client.getWPlanBaseDataStudent() as? Response.Success
            assertNotNull(baseData)
            assertNotEquals(baseData.data.classes.size, 0)
        }
    }

    @Test
    fun `Get wplan for today`() {
        runBlocking {
            val holidays = wPlanStundenplan24Client.holiday.getHolidays() as Response.Success
            assertNotNull(holidays)
            val date = run {
                var date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                while (date in holidays.data || date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                    date = date.minus(DatePeriod(days = 1))
                }
                date
            }
            val today = wPlanStundenplan24Client.getWPlanDataStudent(date = date).also {
                if (it is Response.Error.OnlineError.NotFound) return@runBlocking
            } as? Response.Success
            assertNotNull(today)
            assertEquals(today.data.classes.isEmpty(), false)
        }
    }
}