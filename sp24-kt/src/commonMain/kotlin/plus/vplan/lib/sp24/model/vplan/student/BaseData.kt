package plus.vplan.lib.sp24.model.vplan.student

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("vp")
data class VPlanBaseDataStudent(
    @SerialName("kopf") val head: Head,
    @SerialName("freietage")
    @XmlChildrenName("ft") val holidays: List<String>,
    @SerialName("haupt")
    @XmlChildrenName("aktion") val actions: List<Action>,

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
    @SerialName("kopf")
    data class Head(
        @SerialName("schulname") val schoolName: SchoolName? = null
    ) {
        @Serializable
        @SerialName("schulname")
        data class SchoolName(
            @XmlValue val name: String
        )
    }

    @Serializable
    @SerialName("aktion")
    data class Action(
        @SerialName("klasse") val className: ClassName,
    ) {
        @Serializable
        @SerialName("klasse")
        data class ClassName(
            @XmlValue val name: String
        )
    }
}