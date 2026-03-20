package plus.vplan.lib.sp24.source.extension

import kotlinx.datetime.DayOfWeek
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.SchoolEntityType
import plus.vplan.lib.sp24.source.Stundenplan24Client
import plus.vplan.lib.sp24.source.filterBadSp24Entities
import plus.vplan.lib.sp24.source.removeLeadingZeros
import plus.vplan.lib.sp24.source.sp24EntityListSplitter


class TimetableExtension(
    private val client: Stundenplan24Client
) {
    suspend fun getTimetable(
        weekIndex: Int
    ): Response<Timetable> {
        val timetableResponse = client.getSPlanDataStudent(schoolWeekIndex = weekIndex)
        val classes = client.getAllClassesIntelligent() as? Response.Success
        val teachers = client.getAllTeachersIntelligent() as? Response.Success
        val rooms = client.getAllRoomsIntelligent() as? Response.Success
        if (timetableResponse !is Response.Success) return timetableResponse as Response.Error
        val data = timetableResponse.data

        return Timetable(
            lessons = data.classes.flatMap { clazz ->
                clazz.lessons.map { lesson ->
                    Timetable.Lesson(
                        dayOfWeek = DayOfWeek(lesson.dayOfWeek.value),
                        lessonNumber = lesson.lessonNumber.value,
                        subject = lesson.subject.value,
                        classes = sp24EntityListSplitter(
                            knownEntities = classes?.data.orEmpty().map { it.name }.toSet(),
                            input = lesson.lessonClass.group,
                            entityType = SchoolEntityType.Class,
                            throwIfIsComposed = false
                        ).filterBadSp24Entities(SchoolEntityType.Class).map { removeLeadingZeros(it) }.toSet(),
                        teachers = sp24EntityListSplitter(
                            knownEntities = teachers?.data.orEmpty().map { it.name }.toSet(),
                            input = lesson.teacher.value,
                            entityType = SchoolEntityType.Teacher,
                            throwIfIsComposed = false
                        ).filterBadSp24Entities(SchoolEntityType.Teacher),
                        rooms = sp24EntityListSplitter(
                            knownEntities = rooms?.data.orEmpty().map { it.name }.toSet(),
                            input = lesson.room.value,
                            entityType = SchoolEntityType.Room,
                            throwIfIsComposed = false
                        ).filterBadSp24Entities(SchoolEntityType.Room),
                        weekType = lesson.weekType?.value,
                        limitToWeekNumber = lesson.specificWeeks?.toSet()
                    )
                }
            }.distinctBy { it.hashCode() }.toSet()
        ).let { Response.Success(it) }
    }
}

data class Timetable(
    val lessons: Set<Lesson>
) {

    /**
     * @param limitToWeekNumber If not null, this lesson only applies to a certain set of school week numbers. The user is responsible for mapping this value to the correct week in the correct school year.
     */
    data class Lesson(
        val dayOfWeek: DayOfWeek,
        val lessonNumber: Int,
        val subject: String,
        val classes: Set<String>,
        val teachers: Set<String>,
        val rooms: Set<String>,
        val weekType: String?,
        val limitToWeekNumber: Set<Int>?
    )
}