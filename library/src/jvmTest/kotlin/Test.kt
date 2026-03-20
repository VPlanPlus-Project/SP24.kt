import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.io.files.FileNotFoundException
import plus.vplan.lib.sp24.source.Authentication
import plus.vplan.lib.sp24.source.Response
import java.io.File
import kotlin.test.Test

class Test {

    @Test
    fun `Test raw data`() = runBlocking {
        val mobile = (stundenplan24Client.getMobileBaseDataStudent() as Response.Success).data.raw
        assert(mobile.startsWith("<?xml") && mobile.endsWith(">"))

        val wplan = (stundenplan24Client.getWPlanBaseDataStudent() as Response.Success).data.raw
        assert(wplan.startsWith("<?xml") && wplan.endsWith(">"))

        val vplan = (stundenplan24Client.getVPlanBaseDataStudent() as Response.Success).data.raw
        assert(vplan.startsWith("<?xml") && vplan.endsWith(">"))
    }

    @Test
    fun `Get school names`() {
        val file = File("./access.csv")
        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.absolutePath}")
        }

        val lines = file.readLines().drop(1)
        val accessList = lines.map {
            val (sp24SchoolId, username, password, _) = it.split(",")
            Authentication(sp24SchoolId, username.replace("\"", ""), password.replace("\"", ""))
        }
        runBlocking {
            accessList.forEach { access ->
                val schoolName = stundenplan24Client.getSchoolName(access)
                println("${access.sp24SchoolId}: $schoolName")
            }
        }
    }

    @Test
    fun `Get classes`() {
        val file = File("./access.csv")
        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.absolutePath}")
        }

        val lines = file.readLines().drop(1)
        val accessList = lines.map {
            val (sp24SchoolId, username, password, _) = it.split(",")
            Authentication(sp24SchoolId, username.replace("\"", ""), password.replace("\"", ""))
        }
        runBlocking {
            accessList.forEach { access ->
                val classes = stundenplan24Client.getAllClassesIntelligent(access) as? Response.Success
                println("School ID: ${access.sp24SchoolId}, Classes: ${classes?.data?.joinToString()}")
            }
        }
    }

    @Test
    fun `Get teachers`() {
        val file = File("./access.csv")
        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.absolutePath}")
        }

        val lines = file.readLines().drop(1)
        val accessList = lines.map {
            val (sp24SchoolId, username, password, _) = it.split(",")
            Authentication(sp24SchoolId, username.replace("\"", ""), password.replace("\"", ""))
        }
        runBlocking {
            accessList.forEach { access ->
                val teachers = stundenplan24Client.getAllTeachersIntelligent(access).let {
                    if (it !is Response.Success) println(it)
                    it as? Response.Success
                }
                println("School ID: ${access.sp24SchoolId}, Teachers: ${teachers?.data?.joinToString { "'$it'" }}")
            }
        }
    }

    @Test
    fun `Get rooms`() {
        val file = File("./access.csv")
        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.absolutePath}")
        }

        val lines = file.readLines().drop(1)
        val accessList = lines.map {
            val (sp24SchoolId, username, password, _) = it.split(",")
            Authentication(sp24SchoolId, username.replace("\"", ""), password.replace("\"", ""))
        }
        runBlocking {
            accessList.forEach { access ->
                val rooms = stundenplan24Client.getAllRoomsIntelligent(access).let {
                    if (it !is Response.Success) println(it)
                    it as? Response.Success
                }
                println("School ID: ${access.sp24SchoolId}, Rooms: ${rooms?.data?.joinToString()}")
            }
        }
    }

    @Test
    fun `Test data`() = runBlocking {
        val mobile = stundenplan24Client.getMobileDataStudent(date = LocalDate(2025, 6, 10))
        println(mobile)
    }
}