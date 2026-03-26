package plus.vplan.lib.sp24.source

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig.Companion.IGNORING_UNKNOWN_CHILD_HANDLER
import plus.vplan.lib.sp24.model.mobile.student.MobileStudentBaseData
import plus.vplan.lib.sp24.model.mobile.student.MobileStudentData
import plus.vplan.lib.sp24.model.splan.student.SPlanBaseDataStudent
import plus.vplan.lib.sp24.model.splan.student.SPlanStudentData
import plus.vplan.lib.sp24.model.vplan.student.VPlanBaseDataStudent
import plus.vplan.lib.sp24.model.wplan.student.WPlanStudentBaseData
import plus.vplan.lib.sp24.model.wplan.student.WPlanStudentData
import plus.vplan.lib.sp24.source.extension.HolidayExtension
import plus.vplan.lib.sp24.source.extension.LessonTimeExtension
import plus.vplan.lib.sp24.source.extension.SubjectInstanceExtension
import plus.vplan.lib.sp24.source.extension.SubstitutionPlanExtension
import plus.vplan.lib.sp24.source.extension.TimetableExtension
import plus.vplan.lib.sp24.source.extension.WeekExtension
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("unused")
class Stundenplan24Client(
    val authentication: Authentication,
    val client: HttpClient = HttpClient(),
    val enableInternalCache: Boolean = false
) {
    constructor(client: HttpClient) : this(
        authentication = Authentication(
            sp24SchoolId = "000000",
            username = "username",
            password = "password"
        ),
        client = client
    )

    private val cache = mutableMapOf<String, HttpResponse>()
    fun flushCache() {
        cache.clear()
    }

    private suspend fun getFromCacheOrPutRespectingSettings(
        key: String,
        request: suspend () -> HttpResponse
    ): HttpResponse {
        if (enableInternalCache) {
            return cache.getOrPut(key) {
                request()
            }
        }
        return request()
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private val xml: XML = XML {
        xmlVersion = XmlVersion.XML10
        xmlDeclMode = XmlDeclMode.Auto
        repairNamespaces = true
        defaultPolicy {
            unknownChildHandler = IGNORING_UNKNOWN_CHILD_HANDLER
        }
    }

    val lessonTime = LessonTimeExtension(stundenplan24Client = this)
    val holiday = HolidayExtension(stundenplan24Client = this)
    val week = WeekExtension(stundenplan24Client = this)
    val subjectInstances = SubjectInstanceExtension(stundenplan24Client = this)
    val timetable = TimetableExtension(client = this)
    val substitutionPlan = SubstitutionPlanExtension(client = this)

    suspend fun testConnection(
        authentication: Authentication = this.authentication,
    ): TestConnectionResult {
        safeRequest(onError = { return TestConnectionResult.Error(it) }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(
                    authentication.sp24SchoolId,
                    "mobil",
                    "mobdaten",
                    "Klassen.xml"
                )
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get {
                    url(
                        scheme = "https",
                        host = "stundenplan24.de",
                        path = "/${authentication.sp24SchoolId}/mobil/mobdaten/Klassen.xml"
                    )
                    authentication.useInRequest(this)
                }
            }

            when (response.status) {
                HttpStatusCode.NotFound -> return TestConnectionResult.NotFound
                HttpStatusCode.Unauthorized -> return TestConnectionResult.Unauthorized
                HttpStatusCode.OK -> return TestConnectionResult.Success
                else -> {
                    val error = response.handleUnsuccessfulStates()
                    return if (error != null) TestConnectionResult.Error(error)
                    else {
                        TestConnectionResult.Error(
                            Response.Error.Other(
                                message = "Unexpected status code: ${response.status.value} (${response.status.description}) - body: ${response.bodyAsText()}",
                                throwable = null
                            )
                        )
                    }
                }
            }
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getMobileBaseDataStudent(
        authentication: Authentication = this.authentication,
    ): Response<MobileStudentBaseData> {
        safeRequest(onError = { return it }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(
                    authentication.sp24SchoolId,
                    "mobil",
                    "mobdaten",
                    "Klassen.xml"
                )
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }
            response.handleUnsuccessfulStates()?.let { return it }

            val mobileBaseDataStudent = try {
                xml.decodeFromString(
                    deserializer = MobileStudentBaseData.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            val result = Response.Success(data = mobileBaseDataStudent)
            return result
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getMobileDataStudent(
        authentication: Authentication = this.authentication,
        date: LocalDate
    ): Response<MobileStudentData> {
        safeRequest(onError = { return it }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(
                    authentication.sp24SchoolId,
                    "mobil",
                    "mobdaten",
                    "PlanKl${
                        date.let {
                            val format = LocalDate.Format {
                                year(Padding.ZERO)
                                monthNumber(Padding.ZERO)
                                day(padding = Padding.ZERO)
                            }
                            date.format(format)
                        }
                    }.xml"
                )
            ).build()

            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }

            val mobileDataStudent = try {
                xml.decodeFromString(
                    deserializer = MobileStudentData.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = mobileDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getWPlanBaseDataStudent(
        authentication: Authentication = this.authentication
    ): Response<WPlanStudentBaseData> {
        safeRequest(onError = { return it }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(
                    authentication.sp24SchoolId,
                    "wplan",
                    "wdatenk",
                    "SPlanKl_Basis.xml"
                )
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }
            val wPlanBaseDataStudent = try {
                xml.decodeFromString(
                    deserializer = WPlanStudentBaseData.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = wPlanBaseDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getWPlanDataStudent(
        authentication: Authentication = this.authentication,
        date: LocalDate
    ): Response<WPlanStudentData> {
        safeRequest(onError = { return it }) {
            val fileName = "WPlanKl_${
                date.let {
                    val format = LocalDate.Format {
                        year(Padding.ZERO)
                        monthNumber(Padding.ZERO)
                        day(padding = Padding.ZERO)
                    }
                    date.format(format)
                }
            }.xml"
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(authentication.sp24SchoolId, "wplan", "wdatenk", fileName)
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }
            val wPlanBaseDataStudent = try {
                xml.decodeFromString(
                    deserializer = WPlanStudentData.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = wPlanBaseDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getVPlanBaseDataStudent(
        authentication: Authentication = this.authentication
    ): Response<VPlanBaseDataStudent> {
        safeRequest(onError = { return it }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(authentication.sp24SchoolId, "vplan", "vdaten", "VplanKl.xml")
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }
            val vPlanBaseDataStudent = try {
                xml.decodeFromString(
                    deserializer = VPlanBaseDataStudent.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = vPlanBaseDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getSPlanBaseDataStudent(
        authentication: Authentication = this.authentication
    ): Response<SPlanBaseDataStudent> {
        safeRequest(onError = { return it }) {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(authentication.sp24SchoolId, "splan", "sdaten", "splank.xml")
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }
            val sPlanBaseDataStudent = try {
                xml.decodeFromString(
                    deserializer = SPlanBaseDataStudent.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = sPlanBaseDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    suspend fun getSPlanDataStudent(
        authentication: Authentication = this.authentication,
        schoolWeekIndex: Int
    ): Response<SPlanStudentData> {
        safeRequest(onError = { return it }) {
            val fileName = "SPlanKl_Sw$schoolWeekIndex.xml"
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "stundenplan24.de",
                pathSegments = listOf(authentication.sp24SchoolId, "wplan", "wdatenk", fileName)
            ).build()
            val response = getFromCacheOrPutRespectingSettings(url.toString() + authentication) {
                client.get(url) {
                    authentication.useInRequest(this)
                }
            }

            response.handleUnsuccessfulStates()?.let { return it }
            val sPlanDataStudent = try {
                xml.decodeFromString(
                    deserializer = SPlanStudentData.serializer(),
                    string = response.bodyAsText().sanitizeRawPayload()
                ).copy(raw = response.bodyAsText().sanitizeRawPayload())
            } catch (e: Exception) {
                throw PayloadParsingException(
                    url = response.request.url.toString(),
                    cause = e
                )
            }

            return Response.Success(data = sPlanDataStudent)
        }

        throw IllegalStateException("This should never happen, if it does, please report a bug.")
    }

    /**
     * Fetches the school name, trying all of the available data sources if necessary.
     * @return If successful, returns the school name as a [Response.Success] with the name as data. If the name is null, it was not found in any of the data sources.
     */
    suspend fun getSchoolName(
        authentication: Authentication = this.authentication
    ): Response<String?> {
        val wPlanStudentBaseData = getWPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        wPlanStudentBaseData?.head?.schoolName?.name?.let { return Response.Success(it) }

        val vPlanBaseDataStudent = getVPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        vPlanBaseDataStudent?.head?.schoolName?.name?.ifBlank { null }
            ?.let { return Response.Success(it) }

        val sPlanBaseDataStudent = getSPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        sPlanBaseDataStudent?.head?.schoolName?.ifBlank { null }
            ?.let { return Response.Success(it) }

        return Response.Success(null)
    }

    /**
     * Fetches all classes from the available data sources, intelligently combining, deduplicating, and trimming them.
     * @return A [Response.Success] with a set of class names, or an error if any of the data sources fail critically, which means, that a 404 will be tolerated.
     */
    suspend fun getAllClassesIntelligent(
        authentication: Authentication = this.authentication
    ): Response<Set<NamedEntity>> {
        val classes = mutableSetOf<NamedEntity>()
        val mobileBaseData = getMobileBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        classes.addAll(
            mobileBaseData?.classes.orEmpty()
                .map { NamedEntity(it.name, ValueSource.Indexed, SchoolEntityType.Class) })

        val vPlanBaseDataStudent = getVPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        classes.addAll(
            vPlanBaseDataStudent
                ?.actions.orEmpty()
                .map { it.className.name }
                .filterBadSp24Entities(SchoolEntityType.Class)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Class) }
        )

        val wPlanStudentBaseData = getWPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        classes.addAll(
            wPlanStudentBaseData
                ?.classes.orEmpty()
                .map { it.name.name }
                .filterBadSp24Entities(SchoolEntityType.Class)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Class) }
        )

        val sPlanBaseDataStudent = getSPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        classes.addAll(
            sPlanBaseDataStudent
                ?.classes.orEmpty()
                .map { it.name }
                .filterBadSp24Entities(SchoolEntityType.Class)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Class) }
        )

        val optimizedClasses = classes
            .map { it.name }
            .map { removeLeadingZeros(it) }
            .toSet()
            .let { filtered ->
                filtered.flatMap {
                    try {
                        sp24EntityListSplitter(
                            knownEntities = filtered,
                            input = it,
                            entityType = SchoolEntityType.Class,
                            throwIfIsComposed = true
                        )
                    } catch (e: BadSp24EntityException) {
                        emptyList()
                    }
                }
            }
        return Response.Success(
            (optimizedClasses.mapNotNull { name ->
                if (name.isBadSp24Entity()) return@mapNotNull null
                NamedEntity(
                    name,
                    classes.firstOrNull { it.name == name }?.source ?: ValueSource.Calculated,
                    SchoolEntityType.Class
                )
            })
                .sortedBy { it.name }
                .distinctBy { it.name }
                .toSet()
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getAllTeachersIntelligent(
        authentication: Authentication = this.authentication
    ): Response<Set<NamedEntity>> {
        val teachers = mutableSetOf<NamedEntity>()

        val wPlanData =
            getSPlanDataStudent(authentication = authentication, schoolWeekIndex = 1).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }
        teachers.addAll(
            wPlanData
                ?.classes
                .orEmpty()
                .flatMap { it.lessons.mapNotNull { si -> si.teacher.value.ifBlank { null } } }
                .filterBadSp24Entities(SchoolEntityType.Teacher)
                .map { NamedEntity(it, ValueSource.Found, SchoolEntityType.Teacher) }
        )

        val mobileStudentBaseData = getMobileBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        teachers.addAll(
            mobileStudentBaseData
                ?.classes
                .orEmpty()
                .flatMap { classItem -> classItem.subjectInstances.subjectInstances.map { it.subjectInstance.teacherName } }
                .filterBadSp24Entities(SchoolEntityType.Teacher)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Teacher) }
        )
        teachers.addAll(
            mobileStudentBaseData
                ?.classes
                .orEmpty()
                .flatMap { classItem -> classItem.courses.map { it.course.courseTeacherName } }
                .filterBadSp24Entities(SchoolEntityType.Teacher)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Teacher) }
        )

        val weekStart =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                it.minus(DatePeriod(days = it.dayOfWeek.isoDayNumber.minus(1)))
            }
        if (wPlanData == null) repeat(5) { i ->
            val date = weekStart.plus(DatePeriod(days = i))
            val data = getMobileDataStudent(authentication = authentication, date = date).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }
            teachers.addAll(
                data
                    ?.classes
                    .orEmpty()
                    .flatMap { it.lessons.mapNotNull { l -> l.teacher.teacher } }
                    .filterBadSp24Entities(SchoolEntityType.Teacher)
                    .map { NamedEntity(it, ValueSource.Found, SchoolEntityType.Teacher) }
            )

            val sPlanBaseDataStudent = getSPlanBaseDataStudent(authentication).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }
            teachers.addAll(
                sPlanBaseDataStudent
                    ?.classes
                    .orEmpty()
                    .flatMap {
                        it.plan?.lessons.orEmpty().mapNotNull { l -> l.teacher.ifBlank { null } }
                    }.flatMap { it.split(",") }
                    .filterBadSp24Entities(SchoolEntityType.Teacher)
                    .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Teacher) }
            )
        }

        val optimizedTeachers = teachers.map { it.name }
            .toSet()
            .let { filtered ->
                filtered.flatMap {
                    try {
                        sp24EntityListSplitter(
                            knownEntities = filtered,
                            input = it,
                            entityType = SchoolEntityType.Teacher,
                            throwIfIsComposed = true
                        )
                    } catch (e: BadSp24EntityException) {
                        emptyList()
                    }
                }
            }
        return Response.Success(
            (optimizedTeachers.mapNotNull { name ->
                if (name.isBadSp24Entity()) return@mapNotNull null
                NamedEntity(
                    name,
                    teachers.firstOrNull { it.name == name }?.source ?: ValueSource.Calculated,
                    SchoolEntityType.Teacher
                )
            })
                .sortedBy { it.name }
                .distinctBy { it.name }
                .toSet()
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getAllRoomsIntelligent(
        authentication: Authentication = this.authentication
    ): Response<Set<NamedEntity>> {
        val rooms = mutableSetOf<NamedEntity>()

        val weekStart =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                it.minus(DatePeriod(days = it.dayOfWeek.isoDayNumber.minus(1)))
            }

        val wPlanData = (getSPlanDataStudent(authentication, 1) as? Response.Success)?.data
        repeat(5) { i ->
            val date = weekStart.plus(DatePeriod(days = i))
            val data = getMobileDataStudent(authentication = authentication, date = date).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }
            rooms.addAll(
                data
                    ?.classes
                    .orEmpty()
                    .flatMap { it.lessons.mapNotNull { l -> l.room.room } }
                    .filterBadSp24Entities(SchoolEntityType.Room)
                    .map { NamedEntity(it, ValueSource.Found, SchoolEntityType.Room) }
            )

            val wPlanData = getWPlanDataStudent(authentication = authentication, date = date).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }
            rooms.addAll(
                wPlanData
                    ?.classes
                    .orEmpty()
                    .flatMap { it.lessons.mapNotNull { l -> l.room.room } }
                    .filterBadSp24Entities(SchoolEntityType.Room)
                    .map { NamedEntity(it, ValueSource.Found, SchoolEntityType.Room) }
            )
        }
        if (wPlanData != null) {
            rooms.addAll(
                wPlanData
                    .classes
                    .flatMap { clazz -> clazz.lessons.map { it.room.value } }
                    .filterBadSp24Entities(SchoolEntityType.Room)
                    .map { NamedEntity(it, ValueSource.Found, SchoolEntityType.Room) }
            )
        }

        val sPlanBaseDataStudent = getSPlanBaseDataStudent(authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }
        rooms.addAll(
            sPlanBaseDataStudent
                ?.classes
                .orEmpty()
                .flatMap { it.plan?.lessons.orEmpty().mapNotNull { l -> l.room.ifBlank { null } } }
                .flatMap { it.split(",") }
                .filterBadSp24Entities(SchoolEntityType.Room)
                .map { NamedEntity(it, ValueSource.Indexed, SchoolEntityType.Room) }
        )

        val optimizedRooms = rooms.map { it.name }
            .toSet()
            .let { filtered ->
                filtered.flatMap {
                    try {
                        sp24EntityListSplitter(
                            knownEntities = filtered,
                            input = it,
                            entityType = SchoolEntityType.Room,
                            throwIfIsComposed = true
                        )
                    } catch (e: BadSp24EntityException) {
                        emptyList()
                    }
                }
            }
        return Response.Success(
            (optimizedRooms.mapNotNull { name ->
                if (name.isBadSp24Entity()) return@mapNotNull null
                NamedEntity(
                    name,
                    rooms.firstOrNull { it.name == name }?.source ?: ValueSource.Calculated,
                    SchoolEntityType.Room
                )
            })
                .sortedBy { it.name }
                .distinctBy { it.name }
                .toSet()
        )
    }
}

internal inline fun safeRequest(
    onError: (error: Response.Error) -> Unit,
    request: () -> Unit
) {
    try {
        request()
    } catch (e: Exception) {
        onError(
            when (e) {
                is ClientRequestException, is HttpRequestTimeoutException -> Response.Error.OnlineError.ConnectionError(e)
                is ServerResponseException -> Response.Error.Other(e.message, e)
                is PayloadParsingException -> {
                    e.printStackTrace()
                    Response.Error.ParsingError(e)
                }

                else -> Response.Error.Other(e.stackTraceToString(), e)
            }
        )
    }
}

fun extractComposedEntities(possibleResults: Set<String>, input: String): List<String> {
    val splitItems = input
        .split(",")
        .map { it.trim() }

    if (splitItems.size > 1) return splitItems.flatMap {
        extractComposedEntities(
            possibleResults,
            it
        )
    }

    fun decompose(remaining: String, results: MutableList<String>): Boolean {
        if (remaining.isBlank()) return true
        for (candidate in possibleResults) {
            if (remaining.startsWith(candidate)) {
                results.add(candidate)
                val next = remaining.removePrefix(candidate).trimStart()
                if (decompose(next, results)) return true
                results.removeAt(results.size - 1)
            }
        }
        return false
    }

    val rangeRegex = Regex("""(\d+)([A-Za-z])-.*-(\d+)([A-Za-z])""")
    rangeRegex.matchEntire(input)?.let { match ->
        val startNum = match.groupValues[1]
        val startChar = match.groupValues[2].first()
        val endNum = match.groupValues[3]
        val endChar = match.groupValues[4].first()
        if (startNum == endNum) {
            return (startChar..endChar).map { "$startNum$it" }
                .filter { possibleResults.contains(it) }
        }
    }

    val splitters = listOf("/", ",", ", ")
    for (splitter in splitters) {
        if (input.contains(splitter)) {
            return input.split(splitter).map { it.trim() }.filter { possibleResults.contains(it) }
        }
    }

    val result = mutableListOf<String>()
    if (decompose(input, result)) return result
    return listOf(input)
}

internal suspend inline fun HttpResponse.handleUnsuccessfulStates(): Response.Error? {
    if (this.status == HttpStatusCode.Unauthorized) return Response.Error.OnlineError.Unauthorized(null)
    if (this.status == HttpStatusCode.NotFound) return Response.Error.OnlineError.NotFound(null)
    if (this.status != HttpStatusCode.OK) {
        return Response.Error.Other(
            message = "Unexpected status code: ${this.status.value} (${this.status.description}) - body: ${this.bodyAsText()}",
            throwable = null
        )
    }
    return null
}

class PayloadParsingException(
    url: String,
    cause: Throwable? = null
) : Exception() {
    override val message: String =
        "Failed to parse payload from $url. This is unexpected.\nPlease file a bug report at the official repository at $PROJECT_URL:\n${cause?.stackTraceToString()}"
}

internal const val PROJECT_URL =
    "https://gitlab.jvbabi.es/vplanplus/lib/SP24-kt or https://github.com/VPlanPlus-Project/SP24-kt"

internal fun String.sanitizeRawPayload() =
    this
        .dropWhile { it != '<' }
        .dropLastWhile { it != '>' }
        .lines()
        .joinToString("\n") { it.trim() }

sealed class TestConnectionResult {
    data object NotFound : TestConnectionResult()
    data object Unauthorized : TestConnectionResult()
    data object Success : TestConnectionResult()
    data class Error(val error: Response.Error) : TestConnectionResult()
}

/**
 * A school entity found in the data of stundenplan24.de.
 */
data class NamedEntity(
    val name: String,
    val source: ValueSource,
    val type: SchoolEntityType
) {

    /**
     * Depending on the [type], this method checks if the [name] is something that is very likely
     * to be a real entity. It uses a set of heuristics, check the implementation for details.
     * Some schools have very interesting composed names, so this is not a 100% guarantee but an
     * orientation, for example, to show common entities by default and all the weird ones only
     * if the user explicitly asks for them.
     */
    fun isCommon(): Boolean = type.isCommon(name)
}

sealed class SchoolEntityType {
    data object Class : SchoolEntityType() {
        override fun isCommon(name: String): Boolean {
            val numberClass = Regex("""^\d+$""") // 1, 2 ...
            val classWithSuffix = Regex("""^\d+[a-zA-Z]+$""") // 1a, 2b, 3c ...
            val oberstufe = Regex("""^(JG)\d{1,2}$""")
            val regularName = Regex("""^[a-zA-ZäöüßÄÖÜ]+$""") // e.g. Verw, DAZ
            val evwtScheme = Regex("""^[0-9]+[a-zA-ZäöüÄÖÜß]+[0-9]+$""") // e.g. 9S2
            val classNumberedScheme = Regex("""^\d+/\d+$""") // e.g. 7/8, 9/10

            return listOf(
                numberClass,
                classWithSuffix,
                oberstufe,
                regularName,
                evwtScheme,
                classNumberedScheme
            ).any {
                it.matches(name)
            }
        }
    }

    data object Teacher : SchoolEntityType() {
        override fun isCommon(name: String): Boolean {
            val teacherName = Regex("""^[a-zA-ZäöüßÄÖÜ]+$""") // e.g. Müller, Schmidt
            val teacherWithTitle =
                Regex("""^(Dr\.|Prof\.)\s?(Herr|Frau)?\s?[a-zA-ZäöüßÄÖÜ]+$""") // e.G. Dr. Müller, Prof. Schmidt, Dr. Herr Müller, Prof. Frau Schmidt

            return listOf(teacherName, teacherWithTitle).any {
                it.matches(name)
            }
        }
    }

    data object Room : SchoolEntityType() {
        override fun isCommon(name: String): Boolean {
            return listOf(
                Regex("""^\d+[a-zA-Z]?$"""), // e.g. 101, 102a, 103b
                Regex("""^(Raum( )?)?-?([a-zA-Z]( )?)?\d+(( )?[a-zA-Z]?)?$"""), // e.g. Raum-101, Raum 101, R102, S103a
                Regex("""^(Raum )?[a-zA-ZäöüßÄÖÜ]+(( )?[0-9]+)?$"""), // e.g. Aula, Raum Paris, Hörsaal 2
                Regex("""^(((R(aum))|[a-zA-Z])?([ .])?)?(\d+\.)*(\d+)[a-zA-Z]*$"""), // e.g. R1.2, Raum 3.4.5, 1.2a
                Regex("""^(TH|GYM)\s?(\d+|([a-zA-Z]))$"""), // e.g. TH 1, TH A, GYM 2
            ).any {
                it.matches(name)
            }
        }
    }

    /**
     * @see [NamedEntity.isCommon]
     */
    abstract fun isCommon(name: String): Boolean
}

/**
 * Represents the way a value was obtained.
 */
enum class ValueSource {
    /**
     * The value was obtained from an index page a real user would see at stundenplan24.de. Values
     * from this source are usually the most reliable however, they are not always available and
     * have been discovered to be missing some values some times.
     */
    Indexed,

    /**
     * The value was found in the XML data of a plan, which contains values missing in the index page.
     * Be aware that this source is not as reliable as the indexed source, as it may contain plan-specific
     * values. These values are fetched from the XML data of a plan, which is generated by stundenplan24.de
     * to serve their app and website with no extra fetching required, meaning that it is usually laid out
     * in a way that it is easy to read but not necessarily easy to parse. You may encounter some weird stuff.
     */
    Found,

    /**
     * The value was calculated based on other values. These values are always derived from a [Found]
     * value. They are not guaranteed to be correct, as they are based on assumptions made by the library
     * reflecting what was discovered in the XML data. An example would be a class name generated from
     * `7a-7c` being calculated as the values `7a`, `7b`, and `7c` with one of them being missing in the
     * indexed data. This is done to provide a more complete view of the data, but it may not always
     * be accurate.
     */
    Calculated
}

open class Sp24Exception : Exception()