package plus.vplan.lib.sp24.source.extension

import kotlinx.datetime.LocalDate
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.Sp24Exception
import plus.vplan.lib.sp24.source.Stundenplan24Client

class WeekExtension(
    private val stundenplan24Client: Stundenplan24Client
) {
    suspend fun getWeeks(): Response<List<Week>> {
        val weeks = stundenplan24Client.getWPlanBaseDataStudent().let {
            when (it) {
                is Response.Success -> it.data.weeks.map { week ->
                    Week(
                        calendarWeek = week.calendarWeek,
                        start = week.startDate,
                        end = week.endDate,
                        weekType = week.weekType?.let { weekType -> removeWeekTypeSuffix(weekType) },
                        weekIndex = week.weekIndex
                    )
                }

                is Response.Error.OnlineError.NotFound -> null
                is Response.Error -> return it
            }
        }
            .orEmpty() + stundenplan24Client.getSPlanBaseDataStudent().let {
                when (it) {
                    is Response.Success -> it.data.weeks.map { week ->
                        Week(
                            calendarWeek = week.calendarWeek,
                            start = week.startDate,
                            end = week.endDate,
                            weekType = removeWeekTypeSuffix(week.weekType),
                            weekIndex = week.weekIndex
                        )
                    }

                    is Response.Error.OnlineError.NotFound -> null
                    is Response.Error -> return it
                }
            }
            .orEmpty()
            .distinctBy { it.start }

        if (weeks.isEmpty()) throw SchoolDoesNotSupportWeeks()

        return Response.Success(weeks)
    }
}

private fun removeWeekTypeSuffix(weekType: String): String {
    val regex = Regex("(?i)\\s*-?\\s*Woche\\s*")
    return weekType.replace(regex, "")
}

/**
 * @param weekIndex The number of the week in the current school year, starting with 1. This value is not calculated/validated in any way and comes as-is from Stundenplan24.de
 * @param calendarWeek The calendar week number as in Kalenderwoche. This value is not calculated/validated in any way and comes as-is from Stundenplan24.de
 * @param weekType The type of the week, e.g. "A" or "B". If there is no type specified by the school, this will be null. A suffix like "-Woche" is removed, check implementation for details.
 */
data class Week(
    val calendarWeek: Int,
    val start: LocalDate,
    val end: LocalDate,
    val weekType: String?,
    val weekIndex: Int,
)

class SchoolDoesNotSupportWeeks : Sp24Exception()