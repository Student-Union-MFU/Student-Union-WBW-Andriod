package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail

/** The logged-in participant's profile (GET /me) and event groups. */
class ProfileRepository(private val api: WbwApi) {

    suspend fun me(): ApiResult<ParticipantDetail> = apiCall { api.me() }

    suspend fun groups(): ApiResult<List<Group>> = apiCall { api.groups() }
}
