package th.ac.mfu.su.wbw.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * What did you think of this base?
 *
 * Opened after a participant has been checked in somewhere and has not yet said anything
 * about it. Four questions rather than one: a base can have a fine view and a dull
 * activity, or good staff and nowhere to sit, and a single "how was it" collapses all of
 * that into a number the organisers cannot act on.
 *
 * Only the last one is required. The server takes one rating and treats the rest as
 * optional, and this follows it rather than being stricter — somebody with an opinion
 * about the view and none about the staff should be able to say exactly that. A form that
 * demands four answers collects four guesses instead of one honest one.
 */
@Composable
fun FeedbackScreen(
    checkpointId: Int,
    checkpointName: String,
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    viewModel: FeedbackViewModel = viewModel(factory = FeedbackViewModel.factoryFor(checkpointId)),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaves as soon as the server has it. There is no confirmation screen because there
    // is nothing further to do — the base disappears from the "not answered" list, which
    // is the acknowledgement.
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.action_back),
                    tint = colors.onBackdrop,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feedback_title),
                    color = colors.onBackdropMuted,
                    fontSize = 12.sp,
                )
                // The base's own name, large. It is the subject of every question below,
                // and a form asking "rate this location" without naming it is a form
                // somebody answers about the wrong place.
                Text(
                    checkpointName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackdrop,
                )
            }
        }

        QuestionBlock(
            label = stringResource(R.string.feedback_q_scenery),
            hint = stringResource(R.string.feedback_q_scenery_hint),
            value = state.ratings[FeedbackQuestion.Scenery],
            onRate = { viewModel.rate(FeedbackQuestion.Scenery, it) },
        )
        QuestionBlock(
            label = stringResource(R.string.feedback_q_activity),
            hint = stringResource(R.string.feedback_q_activity_hint),
            value = state.ratings[FeedbackQuestion.Activity],
            onRate = { viewModel.rate(FeedbackQuestion.Activity, it) },
        )
        QuestionBlock(
            label = stringResource(R.string.feedback_q_staff),
            hint = stringResource(R.string.feedback_q_staff_hint),
            value = state.ratings[FeedbackQuestion.Staff],
            onRate = { viewModel.rate(FeedbackQuestion.Staff, it) },
        )
        QuestionBlock(
            label = stringResource(R.string.feedback_q_overall),
            hint = stringResource(R.string.feedback_q_overall_hint),
            value = state.ratings[FeedbackQuestion.Overall],
            required = true,
            onRate = { viewModel.rate(FeedbackQuestion.Overall, it) },
        )

        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.feedback_comment_label),
            color = colors.onBackdrop,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(16.dp), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (state.comment.isEmpty()) {
                Text(
                    stringResource(R.string.feedback_comment_hint),
                    color = colors.onBackdrop.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                )
            }
            BasicTextField(
                value = state.comment,
                onValueChange = viewModel::onComment,
                textStyle = LocalTextStyle.current.copy(color = colors.onBackdrop, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.onBackdrop),
                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
            )
        }

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = colors.danger, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        SubmitButton(enabled = state.canSubmit, busy = state.submitting) { viewModel.submit() }

        // The one question that has to be answered says so here rather than on a button
        // that simply does not respond — a control that ignores a press teaches nothing.
        if (state.ratings[FeedbackQuestion.Overall] == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.feedback_needs_overall),
                color = colors.onBackdropMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 20.dp))
    }
}

/** One question: what it asks, and five numbers to answer it with. */
@Composable
private fun QuestionBlock(
    label: String,
    hint: String,
    value: Int?,
    required: Boolean = false,
    onRate: (Int) -> Unit,
) {
    val colors = wbwColors
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = colors.onBackdrop,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (required) {
                Spacer(Modifier.width(6.dp))
                Text("*", color = colors.danger, fontSize = 14.sp)
            }
        }
        Text(hint, color = colors.onBackdropMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Numbers, not stars. Five stars is a shape people fill in without reading;
            // a number is a value somebody picks. They are also square and large, because
            // this is answered outdoors with a walked-on thumb.
            for (n in 1..5) {
                RatingButton(number = n, selected = value == n, modifier = Modifier.weight(1f)) { onRate(n) }
            }
        }
        // What the ends mean, said once per question rather than under every number.
        Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Text(stringResource(R.string.feedback_scale_low), color = colors.onBackdropMuted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.feedback_scale_high), color = colors.onBackdropMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RatingButton(
    number: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .then(
                if (selected) {
                    Modifier.background(colors.onBackdrop, shape)
                } else {
                    Modifier.glass(shape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                },
            )
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$number",
            color = if (selected) colors.forestVoid else colors.onBackdrop,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val colors = wbwColors
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (enabled) colors.onBackdrop else colors.onBackdrop.copy(alpha = 0.18f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = colors.forestVoid,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                stringResource(R.string.feedback_submit),
                color = if (enabled) colors.forestVoid else colors.onBackdropMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
