package th.ac.mfu.su.wbw.core.network

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.WbwApplication
import java.io.IOException

/** A network outcome: success payload, or a user-facing error message. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onError(block: (String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) block(message)
    return this
}

/**
 * Runs a suspending API call and maps failures to a friendly message. The
 * backend returns errors as `{"error": "..."}` (see middleware.WriteError), so
 * on an HttpException we try to surface that text.
 */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> {
    val res = WbwApplication.appContext.resources
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Error(parseError(e) ?: res.getString(R.string.error_http, e.code()), e.code())
    } catch (e: IOException) {
        ApiResult.Error(res.getString(R.string.error_network))
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: res.getString(R.string.error_unknown))
    }
}

private val errorJson = Json { ignoreUnknownKeys = true }

private fun parseError(e: HttpException): String? = try {
    val body = e.response()?.errorBody()?.string()
    if (body.isNullOrBlank()) null
    else errorJson.decodeFromString<ServerError>(body).error
} catch (_: Exception) {
    null
}

@kotlinx.serialization.Serializable
private data class ServerError(val error: String? = null)
