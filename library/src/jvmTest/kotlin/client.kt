import plus.vplan.lib.sp24.source.Authentication
import java.io.File

fun getWPlanSchool(): Authentication {
    return getAuthFromFile("10063764")
}

fun getSPlanSchool(): Authentication {
    return getAuthFromFile("20299165")
}

private fun getAuthFromFile(schoolId: String): Authentication {
    val file = File("$schoolId.txt")
    if (!file.exists()) {
        throw IllegalStateException("Authentication file for school ID $schoolId does not exist.")
    }
    val (username, password) = try {
        file.readLines().first().split(" ")
    } catch (e: Exception) {
        throw IllegalStateException("Failed to read authentication details from file: ${file.path}\n${file.readText()}", e)
    }

    return Authentication(
        sp24SchoolId = schoolId,
        username = username,
        password = password
    )
}