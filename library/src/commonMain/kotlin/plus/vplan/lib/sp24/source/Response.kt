package plus.vplan.lib.sp24.source

sealed class Response<out T> {
    sealed class Error : Response<Nothing>() {
        open val throwable: Throwable? = null

        data class Other(val message: String = "Other error", override val throwable: Throwable?) : OnlineError()
        data class ParsingError(override val throwable: Throwable) : Error()
        data object Cancelled : Error()

        sealed class OnlineError: Error() {
            data class ConnectionError(override val throwable: Throwable?) : OnlineError()
            data class Unauthorized(override val throwable: Throwable?) : OnlineError()
            data class NotFound(override val throwable: Throwable?) : OnlineError()
        }
    }
    data class Success<out T>(val data: T) : Response<T>()
}