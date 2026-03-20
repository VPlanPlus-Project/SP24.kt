package plus.vplan.lib.sp24.source.extension

import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.Stundenplan24Client
import plus.vplan.lib.sp24.source.removeLeadingZeros

class SubjectInstanceExtension(
    private val stundenplan24Client: Stundenplan24Client
) {
    suspend fun getSubjectInstances(): Response<SubjectInstanceResponse> {
        val mobileStudentBaseData = stundenplan24Client.getMobileBaseDataStudent().let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                is Response.Error -> return it
            }
        }

        val subjectInstances = mobileStudentBaseData?.classes.orEmpty()
            .flatMap { clazz ->
                clazz.subjectInstances.subjectInstances.map { subjectInstance ->
                    SubjectInstanceResponse.SubjectInstance(
                        subject = subjectInstance.subjectInstance.subjectName,
                        teacher = subjectInstance.subjectInstance.teacherName.ifBlank { null },
                        course = subjectInstance.subjectInstance.courseName,
                        classes = listOf(clazz.name).map { removeLeadingZeros(it) },
                        id = subjectInstance.subjectInstance.subjectInstanceNumber
                    )
                }
            }
            .groupBy { it.id }
            .map { (_, instances) ->
                SubjectInstanceResponse.SubjectInstance(
                    subject = instances.first().subject,
                    teacher = instances.first().teacher,
                    course = instances.first().course,
                    classes = instances.flatMap { it.classes }.distinct().map { removeLeadingZeros(it) },
                    id = instances.first().id
                )
            }

        val courses = mobileStudentBaseData?.classes.orEmpty()
            .flatMap { clazz ->
                clazz.courses.map { course ->
                    Pair(
                        SubjectInstanceResponse.Course(
                            name = course.course.courseName,
                            teacher = course.course.courseTeacherName.ifBlank { null },
                            classes = listOf(clazz.name).map { removeLeadingZeros(it) }
                        ),
                        clazz.subjectInstances.subjectInstances.filter { it.subjectInstance.courseName == course.course.courseName }.map { it.subjectInstance.subjectInstanceNumber }
                    )
                }
            }
            .groupBy { (course, subjectInstanceIds) -> course.name + course.teacher + subjectInstanceIds }
            .map { (_, coursesPair) ->
                val courses = coursesPair.map { it.first }
                SubjectInstanceResponse.Course(
                    name = courses.first().name,
                    teacher = courses.first().teacher,
                    classes = courses.flatMap { it.classes }.distinct().map { removeLeadingZeros(it) }
                )
            }

        return Response.Success(
            SubjectInstanceResponse(
                subjectInstances = subjectInstances,
                courses = courses
            )
        )
    }
}

data class SubjectInstanceResponse(
    val subjectInstances: List<SubjectInstance>,
    val courses: List<Course>
) {
    data class Course(
        val name: String,
        val teacher: String?,
        val classes: List<String>
    )

    data class SubjectInstance(
        val subject: String,
        val teacher: String?,
        val course: String?,
        val classes: List<String>,
        val id: Int
    )
}