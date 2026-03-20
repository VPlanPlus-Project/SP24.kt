@file:OptIn(ExperimentalTime::class)

package plus.vplan.lib.sp24.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

operator fun LocalTime.minus(duration: Duration): LocalTime {
    val instant = atDate(LocalDate.fromEpochDays(0)).toInstant(TimeZone.UTC)
    return instant.minus(duration).toLocalDateTime(TimeZone.UTC).time
}