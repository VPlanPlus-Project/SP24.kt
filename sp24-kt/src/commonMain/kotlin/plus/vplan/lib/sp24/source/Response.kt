@file:OptIn(ExperimentalContracts::class)

package plus.vplan.lib.sp24.source

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Response<out T> {
    sealed class Error : Response<Nothing>() {
        open val throwable: Throwable? = null

        data class Other(val message: String = "Other error", override val throwable: Throwable?) : OnlineError()
        data class ParsingError(override val throwable: Throwable) : Error()

        sealed class OnlineError: Error() {
            data class ConnectionError(override val throwable: Throwable?) : OnlineError()
            data class Unauthorized(override val throwable: Throwable?) : OnlineError()
            data class NotFound(override val throwable: Throwable?) : OnlineError()
        }
    }
    data class Success<out T>(val data: T) : Response<T>()
}

@OptIn(ExperimentalContracts::class)
fun <T> Response<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Response.Success<T>)
        returns(false) implies (this@isSuccess is Response.Error)
    }
    return this is Response.Success
}