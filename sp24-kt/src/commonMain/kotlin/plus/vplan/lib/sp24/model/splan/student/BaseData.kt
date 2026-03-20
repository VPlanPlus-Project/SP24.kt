package plus.vplan.lib.sp24.model.splan.student

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("splan")
data class SPlanBaseDataStudent(
    @SerialName("Kopf") val head: Head,
    @SerialName("FreieTage") @XmlChildrenName("ft")
    val holidays: List<String>,

    @SerialName("Kalenderwochen") @XmlChildrenName("Kw")
    val weeks: List<Week> = emptyList(),

    @SerialName("Klassen") @XmlChildrenName("Kl")
    val classes: List<SPlanClassStudent> = emptyList(),

    @Transient val raw: String = ""
) {

    @Serializable
    @XmlSerialName("Kopf")
    data class Head(
        @XmlElement @SerialName("schulname") val schoolName: String? = null,
    )

    @Serializable
    @XmlSerialName("Kl")
    data class SPlanClassStudent(
        @XmlElement @SerialName("Kurz") val name: String,
        @SerialName("Pl") val plan: Plan? = null
    ) {
        @Serializable
        @XmlSerialName("Pl")
        data class Plan(
            @XmlValue val lessons: List<Lesson>
        ) {
            @Serializable
            @SerialName("Std")
            data class Lesson(
                @XmlElement @SerialName("PlLe") val teacher: String,
                @XmlElement @SerialName("PlRa") val room: String
            )
        }
    }

    @Serializable
    @SerialName("Kw")
    data class Week(
        @XmlValue val weekIndex: Int,
        @XmlElement(value = false) @SerialName("KwDatumVon") val start: String,
        @XmlElement(value = false) @SerialName("KwDatumBis") val end: String,
        @XmlElement(value = false) @SerialName("KwNr") val calendarWeek: Int,
        @XmlElement(value = false) @SerialName("KwWoche") val weekType: String
    ) {
        companion object {
            private val weekFormat = LocalDate.Format {
                day(padding = Padding.ZERO)
                char('.')
                monthNumber(padding = Padding.ZERO)
                char('.')
                year()
            }
        }

        val startDate = LocalDate.parse(start, weekFormat)
        val endDate = LocalDate.parse(end, weekFormat)
    }
}