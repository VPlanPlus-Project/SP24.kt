import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.source.Response

class SubjectInstanceTest {

    val client = clientForSp24Id("10063764")!!

    @Test
    fun getSubjectInstances() = runBlocking {
        val subjectInstances = (client.subjectInstances.getSubjectInstances() as? Response.Success)!!.data
        assertNotNull(subjectInstances)

        val classes = (client.getAllClassesIntelligent() as? Response.Success)
            ?.data
            ?.map { it.name }
        kotlin.test.assertNotNull(classes)
        assertTrue(classes.isNotEmpty())

        val teachers = (client.getAllTeachersIntelligent() as? Response.Success)
            ?.data
            ?.map { it.name }
        kotlin.test.assertNotNull(teachers)
        assertTrue(teachers.isNotEmpty())

        subjectInstances.subjectInstances.forEach { subjectInstance ->
            println(subjectInstance)
            if (subjectInstance.course != null) assertNotNull(subjectInstances.courses.find { course -> course.name == subjectInstance.course && subjectInstance.classes.any { it in classes } })
            assertTrue(subjectInstance.subject.isNotEmpty())
            if (subjectInstance.teacher != null) assertTrue(subjectInstance.teacher in teachers) { "Teacher ${subjectInstance.teacher} not found" }
            subjectInstance.classes.forEach { clazz ->
                assertTrue(clazz in classes) { "Class $clazz not found in ${classes.joinToString()}" }
            }
        }
    }
}