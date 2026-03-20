package plus.vplan.lib.sp24.model.splan.student

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("splan")
data class SPlanStudentData(
    @SerialName("Klassen") @XmlChildrenName("Kl")
    val classes: List<SPlanClassStudent> = emptyList(),

    @Transient val raw: String = ""
) {
    @Serializable
    @XmlSerialName("Kl")
    data class SPlanClassStudent(
        @XmlElement @SerialName("Kurz") val name: String,
        @SerialName("Stunden") val lessonTimes: LessonTimes? = null,
        @SerialName("Pl")
        @XmlChildrenName("Std")
        val lessons: List<ClassLesson>
    ) {
        @Serializable
        @XmlSerialName("Stunden")
        data class LessonTimes(
            @XmlValue val lessonTimes: List<LessonTime>
        ) {
            @Serializable
            @SerialName("St")
            data class LessonTime(
                @XmlElement(false) @SerialName("StZeit") val start: String,
                @XmlElement(false) @SerialName("StZeitBis") val end: String,
                @XmlValue val lessonNumber: Int? = null,
            )
        }

        @Serializable
        @SerialName("Std")
        data class ClassLesson(
            @SerialName("PlTg") val dayOfWeek: DayOfWeek,
            @SerialName("PlSt") val lessonNumber: LessonNumber,
            @SerialName("PlFa") val subject: Subject,
            @SerialName("PlKl") val lessonClass: Class,
            @SerialName("PlLe") val teacher: Teacher,
            @SerialName("PlRa") val room: Room,
            @SerialName("PlWo") val weekType: WeekType? = null,
            @SerialName("PlSwochen")
            @XmlChildrenName("PlSw")
            val specificWeeks: List<Int>? = null
        ) {
            @Serializable
            @SerialName("PlWo")
            data class WeekType(
                @XmlValue val value: String
            )

            @Serializable
            @SerialName("PlTg")
            data class DayOfWeek(
                @XmlValue val value: Int
            )

            @Serializable
            @SerialName("PlSt")
            data class LessonNumber(
                @XmlValue val value: Int
            )

            @Serializable
            @SerialName("PlFa")
            data class Subject(
                @XmlValue val value: String
            )

            @Serializable
            @SerialName("PlKl")
            data class Class(
                @XmlValue val group: String
            )

            @Serializable
            @SerialName("PlLe")
            data class Teacher(
                @XmlValue val value: String
            )

            @Serializable
            @SerialName("PlRa")
            data class Room(
                @XmlValue val value: String
            )
        }
    }
}