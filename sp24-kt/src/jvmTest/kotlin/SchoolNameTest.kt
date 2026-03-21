import kotlinx.coroutines.runBlocking
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.isSuccess
import kotlin.test.Test

class SchoolNameTest {

    private val client = clientForSp24Id("10000000")!!

    @Test
    fun `Get school name test`() = runBlocking {
        val schoolNameResponse = client.getSchoolName()
        if (!schoolNameResponse.isSuccess())
            throw IllegalStateException("Failed to get school name: $schoolNameResponse")

        val schoolName = schoolNameResponse.data
        println(schoolName)
        assertTrue(schoolName in setOf("Testschule", "Musterschule Indiware")) { "Name does not match" }
    }
}