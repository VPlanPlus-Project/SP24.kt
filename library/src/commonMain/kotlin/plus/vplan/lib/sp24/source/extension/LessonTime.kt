@file:OptIn(ExperimentalTime::class)

package plus.vplan.lib.sp24.source.extension

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import plus.vplan.lib.sp24.source.Authentication
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.Stundenplan24Client
import plus.vplan.lib.sp24.source.removeLeadingZeros
import plus.vplan.lib.sp24.util.minus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class LessonTimeExtension(
    private val stundenplan24Client: Stundenplan24Client
) {
    suspend fun getLessonTime(contextSchoolWeek: Int?, authentication: Authentication = stundenplan24Client.authentication): Response<List<LessonTime>> {
        val timeParser = LocalTime.Format {
            hour(Padding.NONE)
            char(':')
            minute(Padding.ZERO)
        }

        val mobileBaseData = stundenplan24Client.getMobileBaseDataStudent(authentication = authentication).let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }

        var lessonTimes = listOf<LessonTime>()
        mobileBaseData?.classes.orEmpty().map { baseDataClas ->
            baseDataClas.lessonTimes?.lessonTimes.orEmpty().map { lessonTime ->
                LessonTime(
                    className = removeLeadingZeros(baseDataClas.name),
                    lessonNumber = lessonTime.lessonNumber,
                    start = LocalTime.parse(lessonTime.startTime.trim().dropWhile { it == '0' }, timeParser),
                    end = LocalTime.parse(lessonTime.endTime.trim().dropWhile { it == '0' }, timeParser),
                    interpolated = false
                )
            }
        }
            .flatten()
            .let { lessonTimes = lessonTimes + it }

        if (contextSchoolWeek != null) {
            val sPlanStudentData = stundenplan24Client.getSPlanDataStudent(schoolWeekIndex = contextSchoolWeek, authentication = authentication).let {
                when (it) {
                    is Response.Success -> it.data
                    is Response.Error.OnlineError.NotFound -> null
                    else -> return it as Response.Error
                }
            }

            sPlanStudentData?.classes.orEmpty().map { sPlanStudentDataClass ->
                sPlanStudentDataClass.lessonTimes?.lessonTimes.orEmpty().mapNotNull { lessonTime ->
                    if (
                        lessonTime.start.isBlank() ||
                        lessonTime.end.isBlank() ||
                        lessonTime.lessonNumber == null
                        ) return@mapNotNull null
                    LessonTime(
                        className = removeLeadingZeros(sPlanStudentDataClass.name),
                        lessonNumber = lessonTime.lessonNumber,
                        start = LocalTime.parse(lessonTime.start.trim().dropWhile { it == '0' }, timeParser),
                        end = LocalTime.parse(lessonTime.end.trim().dropWhile { it == '0' }, timeParser),
                        interpolated = false
                    )
                }
            }
                .flatten()
                .let { lessonTimes = lessonTimes + it }
        }

        lessonTimes = lessonTimes.distinctBy { it.className + "_" + it.lessonNumber }

        lessonTimes
            .groupBy { it.className }
            .mapValues { it.value.sortedBy { lessonTime -> lessonTime.lessonNumber } }
            .mapNotNull { (className, lessonTimes) ->
                if (lessonTimes.isEmpty()) return@mapNotNull null
                val lessonTimes = lessonTimes.toMutableList()
                if (lessonTimes.none { it.lessonNumber == 0 }) {
                    val lessonDuration = 45.minutes
                    val breakTime = 10.minutes
                    lessonTimes.add(
                        LessonTime(
                            className = className,
                            lessonNumber = 0,
                            start = lessonTimes.first { it.lessonNumber == 1 }.start - breakTime - lessonDuration,
                            end = lessonTimes.first { it.lessonNumber == 1 }.start - breakTime,
                            interpolated = true
                        )
                    )
                    lessonTimes.sortBy { it.lessonNumber }
                }
                while (!lessonTimes.map { it.lessonNumber }.isContinuous() || lessonTimes.size < 10) {
                    val last = lessonTimes.lastContinuousBy { it.lessonNumber } ?: lessonTimes.last()
                    val lessonDuration = (last.start until last.end)
                    val next = LessonTime(
                        start = last.end,
                        end = last.end + lessonDuration,
                        lessonNumber = last.lessonNumber + 1,
                        interpolated = true,
                        className = className
                    )
                    lessonTimes.add(next)
                    lessonTimes.sortBy { it.lessonNumber }
                }
                lessonTimes.filter { it.interpolated }
            }
            .flatten()
            .let { lessonTimes = lessonTimes + it }

        lessonTimes = lessonTimes
            .distinctBy { it.className + "_" + it.lessonNumber }
            .sortedWith(compareBy({ it.className }, { it.lessonNumber }))

        return Response.Success(lessonTimes)
    }
}

/**
 * @param interpolated Indicates whether the lesson time was found in the data set or calculated to
 * fill gaps that may exist in the data. The 0th lesson is always added if missing, ending
 * 10 minutes before the first lesson of the day and starting 45 minutes before that.
 */
data class LessonTime(
    val className: String,
    val lessonNumber: Int,
    val start: LocalTime,
    val end: LocalTime,
    val interpolated: Boolean = false
)

private fun List<Int>.isContinuous(): Boolean {
    if (isEmpty()) return true
    var last = first()
    drop(1).forEach { current ->
        if (current != last + 1) return false
        last = current
    }
    return true
}

private fun <T> List<T>.lastContinuousBy(predicate: (T) -> Int): T? {
    if (size < 2) return null
    val array = sortedBy { predicate(it) }
    var last = array.first()
    array.drop(1).forEach { current ->
        if (predicate(current) != predicate(last) + 1) return last
        last = current
    }
    return null
}

private infix fun LocalTime.until(other: LocalTime): Duration {
    val start = this.atDate(LocalDate.fromEpochDays(0)).toInstant(TimeZone.UTC)
    val end = other.atDate(LocalDate.fromEpochDays(0)).toInstant(TimeZone.UTC)
    return start.until(end, DateTimeUnit.MILLISECOND).milliseconds
}

private operator fun LocalTime.plus(duration: Duration): LocalTime {
    val instant = atDate(LocalDate.fromEpochDays(0)).toInstant(TimeZone.UTC)
    return instant.plus(duration).toLocalDateTime(TimeZone.UTC).time
}