package plus.vplan.lib.sp24.model.mobile.student

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import plus.vplan.lib.sp24.source.isBadSp24Entity

@Suppress("unused")
@Serializable
@SerialName("VpMobil")
data class MobileStudentData(
    @SerialName("Kopf") val header: Header,
    @SerialName("FreieTage") @XmlChildrenName("ft") val holidays: List<Holiday>,
    @SerialName("Klassen") @XmlChildrenName("Kl") val classes: List<Class>,
    @SerialName("ZusatzInfo") @XmlSerialName("ZiZeile") val info: List<Info> = emptyList(),

    @Transient val raw: String = ""
) {
    @Serializable
    @SerialName("Kopf")
    data class Header(
        @SerialName("planart") val planType: PlanType,
        @SerialName("zeitstempel") val timestamp: Timestamp,
        @SerialName("DatumPlan") val date: Date,
        @SerialName("woche") val week: Week? = null,
    ) {
        @Serializable
        @SerialName("planart")
        data class PlanType(
            @XmlValue val type: String
        )

        @Serializable
        @SerialName("zeitstempel")
        data class Timestamp(
            @XmlValue val value: String
        ) {
            val createdAt = run {
                val format = LocalDateTime.Format {
                    day(padding = Padding.ZERO)
                    char('.')
                    monthNumber(Padding.ZERO)
                    char('.')
                    year(Padding.ZERO)
                    chars(", ")
                    hour(Padding.ZERO)
                    char(':')
                    minute(Padding.ZERO)
                }
                LocalDateTime.parse(value, format)
            }
        }

        @Serializable
        @SerialName("DatumPlan")
        data class Date(
            @XmlValue val value: String
        ) {
            val date = run {
                val format = LocalDate.Format {
                    dayOfWeek(
                        DayOfWeekNames(
                            "Montag",
                            "Dienstag",
                            "Mittwoch",
                            "Donnerstag",
                            "Freitag",
                            "Samstag",
                            "Sonntag"
                        )
                    )
                    chars(", ")
                    day(padding = Padding.ZERO)
                    chars(". ")
                    monthName(
                        MonthNames(
                            "Januar",
                            "Februar",
                            "März",
                            "April",
                            "Mai",
                            "Juni",
                            "Juli",
                            "August",
                            "September",
                            "Oktober",
                            "November",
                            "Dezember"
                        )
                    )
                    char(' ')
                    year(Padding.ZERO)
                }
                LocalDate.parse(value, format)
            }
        }

        @Serializable
        @SerialName("woche")
        data class Week(
            @XmlValue val value: String
        )
    }

    @Serializable
    @SerialName("ft")
    data class Holiday(
        @XmlValue val value: String
    ) {
        companion object {
            private val holidayFormat = LocalDate.Format {
                yearTwoDigits(2000)
                monthNumber()
                day(padding = Padding.ZERO)
            }
        }

        val date = LocalDate.parse(value, holidayFormat)
    }

    @Serializable
    @SerialName("Kl")
    data class Class(
        @SerialName("Kurz") val name: ClassName,
        @SerialName("KlStunden") @XmlChildrenName("KlSt") val lessonTimes: List<ClassLessonTime> = emptyList(),
        @SerialName("Kurse") @XmlChildrenName("Ku") val courses: List<ClassCourseWrapper>,
        @SerialName("Pl") @XmlChildrenName("Std") val lessons: List<ClassLessonStudent>
    ) {
        @Serializable
        @SerialName("Kurz")
        data class ClassName(
            @XmlValue val name: String
        )

        @Serializable
        @SerialName("KlSt")
        data class ClassLessonTime(
            @XmlValue val lessonNumberValue: String? = null,
            @XmlSerialName("ZeitVon") val startTimeValue: String,
            @XmlSerialName("ZeitBis") val endTimeValue: String,
        ) {
            val lessonNumber: Int? = lessonNumberValue?.toIntOrNull()

            val startTime = try {
                LocalDateTime.parse(startTimeValue, LocalDateTime.Format {
                    hour(Padding.ZERO)
                    char(':')
                    minute(Padding.ZERO)
                })
            } catch (_: IllegalArgumentException) {
                null
            }

            val endTime = try {
                LocalDateTime.parse(endTimeValue, LocalDateTime.Format {
                    hour(Padding.ZERO)
                    char(':')
                    minute(Padding.ZERO)
                })
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        @Serializable
        @SerialName("Ku")
        data class ClassCourseWrapper(
            @SerialName("Ku") val course: ClassCourse
        )

        @Serializable
        @SerialName("KKz")
        data class ClassCourse(
            @XmlValue val name: String,
            @XmlSerialName("KLe") val teacher: String? = null,
        )

        @Serializable
        @SerialName("Std")
        data class ClassLessonStudent(
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
                val subject = if (name.isBadSp24Entity()) null else name
            }

            @Serializable
            @SerialName("If")
            data class Info(
                @XmlValue val text: String? = null
            )

            @Serializable
            @SerialName("Nr")
            data class SubjectInstance(
                @XmlValue val rawValue: String?
            ) {
                val value: Int? = rawValue
                    ?.dropWhile { !it.isDigit() }
                    ?.dropLastWhile { !it.isDigit() }
                    ?.toIntOrNull()
            }
        }
    }

    @Serializable
    @SerialName("ZiZeile")
    data class Info(
        @XmlValue val text: String
    )
}