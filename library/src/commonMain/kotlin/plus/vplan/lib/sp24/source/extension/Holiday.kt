package plus.vplan.lib.sp24.source.extension

import kotlinx.datetime.LocalDate
import plus.vplan.lib.sp24.source.Response
import plus.vplan.lib.sp24.source.Stundenplan24Client

class HolidayExtension(
    private val stundenplan24Client: Stundenplan24Client
) {
    suspend fun getHolidays(): Response<Set<LocalDate>> {
        val vpMobilStudentBaseData = stundenplan24Client.getMobileBaseDataStudent().let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }

        val vPlanStudentBaseData = stundenplan24Client.getVPlanBaseDataStudent().let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }

        val wPlanStudentBaseData = stundenplan24Client.getWPlanBaseDataStudent().let {
            when (it) {
                is Response.Success -> it.data
                is Response.Error.OnlineError.NotFound -> null
                else -> return it as Response.Error
            }
        }

        return (vpMobilStudentBaseData?.prettifiedHolidays.orEmpty() +
                vPlanStudentBaseData?.prettifiedHolidays.orEmpty() +
            wPlanStudentBaseData?.prettifiedHolidays.orEmpty())
            .let { Response.Success(it.toSet()) }
    }
}