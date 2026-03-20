package plus.vplan.lib.sp24.model.wplan.student

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlValue
import plus.vplan.lib.sp24.source.isBadSp24Entity

@Serializable
@SerialName("WplanVp")
data class WPlanStudentData(
    @SerialName("Klassen") @XmlChildrenName("Kl") val classes: List<Class>,
    @SerialName("ZusatzInfo") @XmlChildrenName("ZiZeile") val info: List<Info> = emptyList(),

    @Transient val raw: String = "",
) {

    @SerialName("Kl")
    @Serializable
    data class Class(
        @SerialName("Kurz") val name: Name,
        @SerialName("Unterricht") @XmlChildrenName("Ue") val subjectInstanceWrapper: List<SubjectInstanceWrapper>,
        @SerialName("Pl") @XmlChildrenName("Std") val lessons: List<Lesson>,
    ) {
        @Serializable
        @SerialName("Kurz")
        data class Name(
            @XmlValue val name: String
        )

        @SerialName("Ue")
        @Serializable
        data class SubjectInstanceWrapper(
            @SerialName("UeNr") val subjectInstance: SubjectInstance
        ) {
            @Serializable
            @SerialName("UeNr")
            data class SubjectInstance(
                @XmlValue val subjectInstanceNumber: Int,
                @SerialName("UeLe") val teacherName: String? = null,
                @SerialName("UeFa") val subjectName: String,
                @SerialName("UeGr") val courseName: String? = null
            )
        }

        @SerialName("Std")
        @Serializable
        data class Lesson(
            @SerialName("Le") val teacher: Teacher,
            @SerialName("Ra") val room: Room,
            @SerialName("St") val lessonNumber: LessonNumber,
            @SerialName("Fa") val subject: Subject,
            @SerialName("If") val info: Info? = null,
            @SerialName("Nr") val subjectInstance: SubjectInstance? = null
        ) {
            @Serializable
            @SerialName("Le")
            data class Teacher(
                @XmlValue val name: String,
                @SerialName("LeAe") val teacherChanged: String? = null
            ) {
                val teacher = if (name.isBadSp24Entity()) null else name
            }

            @Serializable
            @SerialName("Ra")
            data class Room(
                @XmlValue val name: String,
                @SerialName("RaAe") val roomChanged: String? = null
            ) {
                val room = if (name.isBadSp24Entity()) null else name
            }

            @Serializable
            @SerialName("St")
            data class LessonNumber(
                @XmlValue val value: Int
            )

            @Serializable
            @SerialName("Fa")
            data class Subject(
                @XmlValue val name: String,
                @SerialName("FaAe") val subjectChanged: String? = null
            ) {
                val subject = name
                    .replace(Regex("-+"), "")
                    .replace("&nbsp;", "")
                    .trim()
                    .ifBlank { null }
            }

            @Serializable
            @SerialName("If")
            data class Info(
                @XmlValue val text: String? = null
            )

            @Serializable
            @SerialName("Nr")
            data class SubjectInstance(
                @XmlValue val value: Int
            )
        }
    }

    @Serializable
    @SerialName("ZiZeile")
    data class Info(
        @XmlValue val text: String
    )
}