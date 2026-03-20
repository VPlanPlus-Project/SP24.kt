import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.utils.io.InternalAPI
import plus.vplan.lib.sp24.source.Authentication
import plus.vplan.lib.sp24.source.Stundenplan24Client

@OptIn(InternalAPI::class)
@Suppress("NewApi")
internal val client = HttpClient(CIO) {

//    install(Logging) {
//        logger = object: Logger {
//            override fun log(message: String) {
//                println(message)
//            }
//        }
//        level = LogLevel.HEADERS
//    }

    install(UserAgent) {
        agent = "Sp24.kt automated tests"
    }
}

internal val stundenplan24Client = Stundenplan24Client(
    authentication = Authentication(
        sp24SchoolId = "10000000",
        username = "schueler",
        password = "123123"
    ),
    enableInternalCache = true,
    client = client
)

internal val wPlanStundenplan24Client = Stundenplan24Client(
    authentication = getWPlanSchool(),
    enableInternalCache = true,
    client = client
)