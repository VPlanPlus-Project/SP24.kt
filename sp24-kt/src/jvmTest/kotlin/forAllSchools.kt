import plus.vplan.lib.sp24.source.Authentication
import plus.vplan.lib.sp24.source.Stundenplan24Client
import java.io.File
import java.io.FileNotFoundException

private fun readAccessList(): List<Authentication> {
    val file = File("./access.csv")
    if (!file.exists()) {
        throw FileNotFoundException("File not found: ${file.absolutePath}")
    }
    return file.readLines().drop(1).map {
        val (sp24SchoolId, username, password, _) = it.split(",")
        Authentication(sp24SchoolId, username.replace("\"", ""), password.replace("\"", ""))
    }
}

suspend fun forAllSchools(body: suspend (Stundenplan24Client) -> Unit) {
    for (access in readAccessList()) {
        body(clients.getOrPut(access) {
            println()
            println(access.username + "@" + access.sp24SchoolId)
            Stundenplan24Client(
                authentication = access,
                enableInternalCache = true,
                client = client
            )
        })
    }
}

fun authenticationForSp24Id(sp24SchoolId: String): Authentication? {
    return readAccessList().find { it.sp24SchoolId == sp24SchoolId }
}

fun clientForSp24Id(sp24SchoolId: String): Stundenplan24Client? {
    val access = readAccessList().find { it.sp24SchoolId == sp24SchoolId } ?: return null
    return clients.getOrPut(access) {
        Stundenplan24Client(
            authentication = access,
            enableInternalCache = true,
            client = client
        )
    }
}

val clients = mutableMapOf<Authentication, Stundenplan24Client>()