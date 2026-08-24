package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import kotlinx.serialization.json.JsonNull
import th.ac.mfu.su.wbw.core.network.NetworkModule
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.OkResponse
import th.ac.mfu.su.wbw.data.remote.dto.SosCase
import th.ac.mfu.su.wbw.data.remote.dto.SosRequest

/**
 * The emergency endpoints.
 *
 * Thin like [ChatRepository], and for the same reason — the loop that holds the long-poll
 * open belongs to whoever knows the screen is still there. What is different is what is
 * *missing*: there is no cache here.
 *
 * That is deliberate. Every other screen in this app opens on the last thing it knew,
 * because a stale pass or a stale weather card is better than a spinner. An emergency is
 * the one case where that bargain inverts: showing a cached "help is coming" for a case
 * that was resolved yesterday, to somebody who is hurt today, is worse than showing
 * nothing. The state of an emergency is only ever what the server says right now.
 */
class SosRepository(private val api: WbwApi) {

    /**
     * Raise, or update the case already open.
     *
     * The caller passes the same [SosRequest.clientId] for both, which is what makes the
     * position arriving late an update rather than a second emergency.
     */
    suspend fun raise(request: SosRequest): ApiResult<SosCase> =
        apiCall { api.raiseSos(request) }

    /**
     * One long-poll round trip for the participant's own open case.
     *
     * A `null` inside a [ApiResult.Success] means "no emergency", which is a real answer.
     * Only [ApiResult.Error] means the question could not be asked, and keeping those two
     * apart is the entire reason this decodes by hand: the server says "no emergency" with
     * a JSON `null` body, and letting Retrofit's converter meet that with a non-nullable
     * serializer would raise a parse error instead — an emergency screen reporting a fault
     * when the honest answer is "you are fine". See [WbwApi.activeSos].
     */
    suspend fun active(waitSeconds: Int): ApiResult<SosCase?> = apiCall {
        val body = api.activeSos(waitSeconds)
        if (body is JsonNull) null
        else NetworkModule.json.decodeFromJsonElement(SosCase.serializer(), body)
    }

    suspend fun cancel(id: Long): ApiResult<OkResponse> =
        apiCall { api.cancelSos(id) }
}
