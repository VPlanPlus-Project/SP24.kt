package plus.vplan.lib.sp24.model.wplan.student

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("splan")
data class WPlanStudentBaseData(
    @SerialName("Kopf") val head: Head,
    @SerialName("FreieTage")
    @XmlChildrenName("ft")
    val holidays: List<String>,

    @SerialName("Klassen")
    @XmlChildrenName("Kl")
    val classes: List<Class>,

    @SerialName("Schulwochen")
    @XmlChildrenName("Sw")
    val weeks: List<Week> = emptyList(),

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
    @SerialName("Kopf")
    data class Head(
        @SerialName("schulname") val schoolName: SchoolName? = null,
    ) {
        @Serializable
        @SerialName("schulname")
        data class SchoolName(
            @XmlValue val name: String
        )
    }

    @Serializable
    @SerialName("Kl")
    data class Class(
        @SerialName("Kurz") val name: ClassName,
    ) {
        @Serializable
        @SerialName("Kurz")
        data class ClassName(
            @XmlValue val name: String
        )
    }

    @Serializable
    @SerialName("Sw")
    data class Week(
        @XmlElement(false) @SerialName("SwDatumVon") val start: String,
        @XmlElement(false) @SerialName("SwDatumBis") val end: String,
        @XmlElement(false) @SerialName("SwKw") val calendarWeek: Int,
        @XmlElement(false) @SerialName("SwWo") val weekType: String? = null,
        @XmlValue val weekIndex: Int,
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