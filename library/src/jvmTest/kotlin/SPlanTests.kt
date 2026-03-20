import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.model.splan.student.SPlanBaseDataStudent
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.extension.Timetable

class SPlanTests {

    val client = clientForSp24Id("10063764")!!

//    @Test
//    fun sPlanTestsBaseData() = runBlocking {
//        val baseData = client.getSPlanBaseDataStudent(getSPlanSchool()).also {
//            if (it is Response.Error.OnlineError.NotFound) return@runBlocking
//        } as? Response.Success<SPlanBaseDataStudent>
//        assertNotNull(baseData)
//        val data = baseData!!.data
//        assertTrue(data.holidays.isNotEmpty())
//        assertTrue(data.classes.any { it.plan?.lessons.orEmpty().isNotEmpty() })
//    }

    @Test
    fun sPlanTestsExtension() = runBlocking {
        val data = client.timetable.getTimetable(1).also {
            if (it is Response.Error.OnlineError.NotFound) return@runBlocking
        } as? Response.Success<Timetable>

        val classes = (client.getAllClassesIntelligent() as Response.Success).data.map { it.name }

        assertNotNull(data)

        data!!.data.lessons.forEach { lesson ->
            lesson.classes.forEach { clazz ->
                println(clazz)
                assertTrue(clazz in classes)
            }
        }
    }
}