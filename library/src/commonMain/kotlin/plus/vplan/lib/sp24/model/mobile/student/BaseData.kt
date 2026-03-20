package plus.vplan.lib.sp24.model.mobile.student

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("VpMobil")
data class MobileStudentBaseData(
    @SerialName("FreieTage") @XmlChildrenName("ft")
    val holidays: List<String>,

    @SerialName("Klassen") @XmlChildrenName("Kl")
    val classes: List<Class>,

    @SerialName("Kopf")
    val header: Header,

    @Transient
    val raw: String = ""
) {

    companion object {
        private val holidayFormat = LocalDate.Format {
            yearTwoDigits(2000)
            monthNumber()
            day(padding = Padding.ZERO)
        }
    }

    val prettifiedHolidays: Set<LocalDate> = this.holidays.map { LocalDate.parse(it, holidayFormat) }.toSet()

    @Serializable
    @XmlSerialName("Kopf")
    data class Header(
        @XmlElement @SerialName("tageprowoche")
        val daysPerWeek: Int? = null,
    )

    @Serializable
    @XmlSerialName("Kl")
    data class Class(
        @XmlElement @SerialName("Kurz")
        val name: String,

        @SerialName("KlStunden")
        val lessonTimes: LessonTimes? = null,

        @SerialName("Unterricht")
        val subjectInstances: SubjectInstances,

        @SerialName("Kurse") @XmlChildrenName("Ku")
        val courses: List<ClassCourseWrapper>
    ) {

        @Serializable
        @XmlSerialName("KlStunden")
        data class LessonTimes(
            @XmlValue val lessonTimes: List<ClassLessonTime>
        ) {
            @XmlSerialName("KlSt")
            @Serializable
            data class ClassLessonTime(
                @SerialName("ZeitVon") val startTime: String,
                @SerialName("ZeitBis") val endTime: String,
                @XmlValue val lessonNumber: Int,
            )
        }

        @Serializable
        @XmlSerialName("Unterricht")
        data class SubjectInstances(
            @XmlValue val subjectInstances: List<ClassSubjectInstanceWrapper>
        ) {

            @Serializable
            @XmlSerialName("Ue")
            data class ClassSubjectInstanceWrapper(
                @SerialName("UeNr") val subjectInstance: ClassSubjectInstance
            ) {
                @Serializable
                @XmlSerialName("UeNr")
                data class ClassSubjectInstance(
                    @XmlValue val subjectInstanceNumber: Int,
                    @SerialName("UeLe") val teacherName: String,
                    @SerialName("UeFa") val subjectName: String,
                    @SerialName("UeGr") val courseName: String? = null
                )
            }
        }

        @Serializable
        @SerialName("Ku")
        data class ClassCourseWrapper(
            @SerialName("KKz") val course: ClassCourse
        ) {
            @Serializable
            @SerialName("KKz")
            data class ClassCourse(
                @SerialName("KLe") val courseTeacherName: String,
                @XmlValue val courseName: String
            )
        }
    }
}