package plus.vplan.lib.sp24.source.extension

import kotlinx.datetime.LocalDate
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.SchoolEntityType
import plus.vplan.lib.sp24.source.Stundenplan24Client
import plus.vplan.lib.sp24.source.filterBadSp24Entities
import plus.vplan.lib.sp24.source.removeLeadingZeros
import plus.vplan.lib.sp24.source.sp24EntityListSplitter

class SubstitutionPlanExtension(
    private val client: Stundenplan24Client
) {
    suspend fun getSubstitutionPlan(
        date: LocalDate
    ): Response<SubstitutionPlan> {
        val teachers = (client.getAllTeachersIntelligent() as? Response.Success)?.data.orEmpty().map { it.name }.toSet()
        val rooms = (client.getAllRoomsIntelligent() as? Response.Success)?.data.orEmpty().map { it.name }.toSet()

        val wplanResponse = client.getWPlanDataStudent(date = date)
        if (wplanResponse is Response.Error.OnlineError.NotFound) {

            val mobdatenResponse = client.getMobileDataStudent(date = date)
            if (mobdatenResponse !is Response.Success) return mobdatenResponse as Response.Error
            val mobdatenData = mobdatenResponse.data

            return SubstitutionPlan(
                date = date,
                source = SubstitutionPlan.Source.Mobil,
                info = mobdatenData.info.map { it.text },
                lessons = mobdatenData.classes.flatMap { clazz ->
                    clazz.lessons.map { lesson ->
                        SubstitutionPlan.Lesson(
                            lessonNumber = lesson.lessonNumber.value,
                            subject = lesson.subject.subject,
                            subjectChanged = lesson.subject.subjectChanged != null,
                            classes = listOf(clazz.name.name).map { removeLeadingZeros(it) },
                            teachers = lesson.teacher.teacher?.let { sp24EntityListSplitter(
                                knownEntities = teachers,
                                input = it,
                                entityType = SchoolEntityType.Teacher,
                                throwIfIsComposed = false
                            ) }.orEmpty().filterBadSp24Entities(SchoolEntityType.Teacher).sorted(),
                            teachersChanged = lesson.teacher.teacherChanged != null,
                            rooms = lesson.room.room?.let { sp24EntityListSplitter(
                                knownEntities = rooms,
                                input = it,
                                entityType = SchoolEntityType.Room,
                                throwIfIsComposed = false
                            ) }.orEmpty().filterBadSp24Entities(SchoolEntityType.Room).sorted(),
                            roomsChanged = lesson.room.roomChanged != null,
                            info = lesson.info?.text,
                            subjectInstanceId = lesson.subjectInstance?.value
                        )
                    }
                }
            ).let { Response.Success(it) }

        }
        if (wplanResponse !is Response.Success) return wplanResponse as Response.Error
        val data = wplanResponse.data

        return SubstitutionPlan(
            date = date,
            source = SubstitutionPlan.Source.Wochenplan,
            info = data.info.map { it.text },
            lessons = data.classes.flatMap { clazz ->
                clazz.lessons.map { lesson ->
                    SubstitutionPlan.Lesson(
                        lessonNumber = lesson.lessonNumber.value,
                        subject = lesson.subject.subject,
                        subjectChanged = lesson.subject.subjectChanged != null,
                        classes = listOf(clazz.name.name).map { removeLeadingZeros(it) },
                        teachers = lesson.teacher.teacher?.let { sp24EntityListSplitter(
                            knownEntities = teachers,
                            input = it,
                            entityType = SchoolEntityType.Teacher,
                            throwIfIsComposed = false
                        ) }.orEmpty().sorted(),
                        teachersChanged = lesson.teacher.teacherChanged != null,
                        rooms = lesson.room.room?.let { sp24EntityListSplitter(
                            knownEntities = rooms,
                            input = it,
                            entityType = SchoolEntityType.Room,
                            throwIfIsComposed = false
                        ) }.orEmpty().sorted(),
                        roomsChanged = lesson.room.roomChanged != null,
                        info = lesson.info?.text,
                        subjectInstanceId = lesson.subjectInstance?.value
                    )
                }
            }
                .groupBy { lesson -> "${lesson.lessonNumber} ${lesson.teachers} ${lesson.rooms} ${lesson.subjectInstanceId}" }
                .flatMap { (_, lessons) ->
                    if (lessons.first().subject == null) lessons
                    else listOf(lessons.reduce { acc, lesson ->
                        SubstitutionPlan.Lesson(
                            lessonNumber = acc.lessonNumber,
                            subject = acc.subject,
                            subjectChanged = acc.subjectChanged,
                            teachers = acc.teachers,
                            teachersChanged = acc.teachersChanged,
                            rooms = acc.rooms,
                            roomsChanged = acc.roomsChanged,
                            classes = (acc.classes + lesson.classes).distinct().map { removeLeadingZeros(it) },
                            info = acc.info,
                            subjectInstanceId = acc.subjectInstanceId
                        )
                    })
                }

        ).let { Response.Success(it) }
    }
}

data class SubstitutionPlan(
    val date: LocalDate,
    val source: Source,
    val info: List<String>,
    val lessons: List<Lesson>
) {
    data class Lesson(
        val lessonNumber: Int,
        val subject: String?,
        val subjectChanged: Boolean,
        val teachers: List<String>,
        val teachersChanged: Boolean,
        val rooms: List<String>,
        val roomsChanged: Boolean,
        val classes: List<String>,
        val info: String?,
        val subjectInstanceId: Int?
    )

    enum class Source {
        Wochenplan,
        Mobil
    }
}