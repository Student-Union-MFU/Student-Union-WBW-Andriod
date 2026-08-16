package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail

/** The logged-in participant's profile (GET /me) and event groups. */
class ProfileRepository(
    private val api: WbwApi,
    private val cache: ResponseCache,
) {

    /**
     * The profile as of the last successful fetch, or null on a first run.
     *
     * Synchronous and non-suspending on purpose — see [ResponseCache]. A view model reads
     * this in `init` so its very first emission is already the participant's data rather
     * than a spinner.
     */
    fun cachedMe(): ParticipantDetail? = cache.read(ResponseCache.KeyMe, ParticipantDetail.serializer())

    suspend fun me(): ApiResult<ParticipantDetail> =
        apiCall { api.me() }.onSuccess { cache.write(ResponseCache.KeyMe, ParticipantDetail.serializer(), it) }

    /** Not cached: nothing reads groups on a screen's opening frame. */
    suspend fun groups(): ApiResult<List<Group>> = apiCall { api.groups() }
}
