import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import plus.vplan.lib.sp24.source.Response
import kotlin.test.assertNotNull

class CoursesTests {

    val client = clientForSp24Id("10063764")!!

    @Test
    fun getCourses() = runBlocking {
        val courses = (client.subjectInstances.getSubjectInstances() as? Response.Success)!!.data.courses
        assertNotNull(courses)

        val classes = (client.getAllClassesIntelligent() as? Response.Success)
            ?.data
            ?.map { it.name }
        assertNotNull(classes)
        assertTrue(classes.isNotEmpty())
        println("Classes: ${classes.joinToString(", ")}")

        val teachers = (client.getAllTeachersIntelligent() as? Response.Success)
            ?.data
            ?.map { it.name }
        assertNotNull(teachers)
        assertTrue(teachers.isNotEmpty())
        println("Teachers: ${teachers.joinToString(", ")}")

        courses.forEach { course ->
            println(course)
            assertTrue(course.name.isNotEmpty())
            assertTrue(course.classes.isNotEmpty())
            assertTrue(course.classes.all { it in classes })
            if (course.teacher != null) assertTrue(course.teacher in teachers)
        }
    }
}