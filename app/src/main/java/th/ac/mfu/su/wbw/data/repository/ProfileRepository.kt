package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.remote.dto.JoinGroupResponse
import th.ac.mfu.su.wbw.data.remote.dto.OkResponse
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

    /**
     * Join a group, then refresh the cached profile.
     *
     * The refresh is part of the operation rather than the caller's problem: `group_id`
     * lives on `/me`, every screen that asks "which group am I in" reads it from the cache,
     * and a join that left that cache saying `null` would put the participant back in front
     * of the group picker they just used.
     */
    suspend fun joinGroup(groupId: Int): ApiResult<JoinGroupResponse> =
        apiCall { api.joinGroup(groupId) }.onSuccess { me() }

    /** Leave the current group. Costs the participant's single `leave_quota`. */
    suspend fun leaveGroup(): ApiResult<OkResponse> =
        apiCall { api.leaveGroup() }.onSuccess { me() }
}
