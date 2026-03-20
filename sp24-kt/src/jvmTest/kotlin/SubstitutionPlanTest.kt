import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import plus.vplan.lib.sp24.source.Response
import kotlin.test.Test

class SubstitutionPlanTest {

    @Test
    fun `Get substitution plan`() = runBlocking {
        val client = clientForSp24Id("10063764")!!
        val substitutionPlan = client.substitutionPlan.getSubstitutionPlan(LocalDate(2026, 3, 20)) as Response.Success
        println(substitutionPlan.data)
    }
}