package th.ac.mfu.su.wbw.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.FeedbackRequest
import th.ac.mfu.su.wbw.data.repository.ProgressRepository
import th.ac.mfu.su.wbw.ui.appContainer
import java.time.OffsetDateTime
import java.util.UUID

/** The four things a base is asked about. Order is the order they are shown in. */
enum class FeedbackQuestion { Scenery, Activity, Staff, Overall }

data class FeedbackUiState(
    val ratings: Map<FeedbackQuestion, Int> = emptyMap(),
    val comment: String = "",
    val submitting: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
) {
    /**
     * Only the overall answer is required.
     *
     * The server requires exactly one rating and takes the other three as optional, and
     * this follows it rather than being stricter: somebody who has an opinion about the
     * view and none about the staff should be able to say so and move on. Forcing four
     * answers is how a form gets four guesses instead of one honest one.
     */
    val canSubmit: Boolean get() = ratings[FeedbackQuestion.Overall] != null && !submitting
}

/**
 * One base's feedback, held while it is being filled in.
 *
 * [clientId] is made once, when the screen opens, and reused for every attempt — so a
 * submission that times out and is retried lands on the same row server-side instead of
 * becoming a second opinion from the same person.
 */
class FeedbackViewModel(
    private val progress: ProgressRepository,
    private val checkpointId: Int,
) : ViewModel() {

    private val clientId: String = UUID.randomUUID().toString()

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun rate(question: FeedbackQuestion, value: Int) {
        _state.update { it.copy(ratings = it.ratings + (question to value), error = null) }
    }

    fun onComment(text: String) {
        // Capped where the typing happens rather than only where the sending does, so the
        // limit cannot be reached in the first place.
        if (text.length <= MaxCommentChars) _state.update { it.copy(comment = text) }
    }

    fun submit() {
        val s = _state.value
        val overall = s.ratings[FeedbackQuestion.Overall] ?: return
        if (s.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val body = FeedbackRequest(
                clientId = clientId,
                checkpointId = checkpointId,
                rating = overall,
                ratingScenery = s.ratings[FeedbackQuestion.Scenery],
                ratingActivity = s.ratings[FeedbackQuestion.Activity],
                ratingStaff = s.ratings[FeedbackQuestion.Staff],
                comment = s.comment.trim().takeIf { it.isNotBlank() },
                deviceTime = OffsetDateTime.now().toString(),
            )
            when (val r = progress.submitFeedback(body)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, done = true) }
                // The server's own words. It distinguishes "you already answered for this
                // base" from "you were never checked in here", and both are worth reading.
                is ApiResult.Error -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }

    companion object {
        const val MaxCommentChars = 500

        fun factoryFor(checkpointId: Int) = viewModelFactory {
            initializer { FeedbackViewModel(appContainer.progressRepository, checkpointId) }
        }
    }
}
