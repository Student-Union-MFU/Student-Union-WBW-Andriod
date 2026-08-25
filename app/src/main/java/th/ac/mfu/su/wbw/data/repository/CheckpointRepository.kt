package th.ac.mfu.su.wbw.data.repository

import kotlinx.serialization.builtins.ListSerializer
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantCheckpoint

/**
 * Where the bases are.
 *
 * Cached hard, and this is the screen that needs it most: the map is opened on a hill with
 * one bar of signal, and a route drawn with no bases on it is the map failing at the one
 * question it is asked all day. The list changes about never — an admin adding a base on
 * the morning of the event is the only case — so last run's copy is almost always current.
 */
class CheckpointRepository(
    private val api: WbwApi,
    private val cache: ResponseCache,
) {
    private val listSerializer = ListSerializer(ParticipantCheckpoint.serializer())

    /** Synchronous, so the map's first frame already has its pins. */
    fun cached(): List<ParticipantCheckpoint>? =
        cache.read(ResponseCache.KeyCheckpoints, listSerializer)

    suspend fun checkpoints(): ApiResult<List<ParticipantCheckpoint>> =
        apiCall { api.checkpoints() }
            .onSuccess { cache.write(ResponseCache.KeyCheckpoints, listSerializer, it) }
}
