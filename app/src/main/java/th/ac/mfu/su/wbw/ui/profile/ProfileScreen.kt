package th.ac.mfu.su.wbw.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.ui.common.ErrorState
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.common.UiState
import th.ac.mfu.su.wbw.ui.home.GrowthPhase
import th.ac.mfu.su.wbw.ui.theme.Kanit
import th.ac.mfu.su.wbw.ui.theme.wbwColors

// The pass is a physical ticket — fixed cream/forest palette, independent of app theme.
private val TicketInk = Color(0xFF173D2C)
private val TicketMuted = Color(0xFF8A8577)
private val TicketGold = Color(0xFFC9883F)
private val TicketGoldSoft = Color(0xFFE2B078)
private val TicketTileBg = Color(0xFFF6EFE1)
private val TicketTileBorder = Color(0xFFEBE1CD)
private val TicketCream = Color(0xFFFDFBF5)

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is UiState.Success -> ProfileContent(s.data, contentPadding, onOpenSettings)
        }
    }
}

@Composable
private fun ProfileContent(p: ParticipantDetail, contentPadding: PaddingValues, onOpenSettings: () -> Unit) {
    val colors = wbwColors
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 18.dp),
    ) {
        // header
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_pass_kicker), color = colors.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp)
                Text(stringResource(R.string.profile_pass_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(colors.glass)
                    .clickableTap(onOpenSettings),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title), tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
        }

        Ticket(p)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Ticket(p: ParticipantDetail) {
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(TicketCream, Color(0xFFF0E8D8)))),
    ) {
        // stub header
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(TicketInk, Color(0xFF2D6A4F)))).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.profile_pass_title).uppercase(), color = Color(0x99FAF7F0), fontSize = 8.5.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.profile_event_name), color = TicketGoldSoft, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Park, null, tint = TicketGoldSoft, modifier = Modifier.size(30.dp))
                    Text(stringResource(R.string.profile_official), color = Color(0xA6FAF7F0), fontSize = 7.sp, letterSpacing = 2.sp)
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TicketChip("22–23 Nov 2026", Color(0x2EE2B078), TicketGoldSoft, border = Color(0x57E2B078))
                p.groupNumber?.let { TicketChip("Group $it", Color(0x1AFAF7F0), Color(0xFFFAF7F0), border = Color(0x2EFAF7F0)) }
            }
        }

        // identity row
        Row(Modifier.padding(20.dp, 16.dp, 20.dp, 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xFF40916C), TicketInk))),
                contentAlignment = Alignment.Center,
            ) { Text(initialOf(p), fontFamily = Kanit, color = Color(0xFFFAF7F0), fontSize = 25.sp) }
            Column(Modifier.weight(1f)) {
                Text(p.fullName, color = TicketInk, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
                p.schoolName?.let { Text(it, color = TicketMuted, fontSize = 12.sp) }
                p.studentId?.let { Text("Student $it", color = TicketGold, fontWeight = FontWeight.Bold, fontSize = 11.5.sp) }
            }
        }

        // BIB band
        Row(
            Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(TicketInk, Color(0xFF2D6A4F)))).padding(16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_bib_number).uppercase(), color = Color(0x99FAF7F0), fontSize = 8.5.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text(p.bib?.toString() ?: "—", fontFamily = Kanit, color = Color(0xFFFAF7F0), fontSize = 42.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(TicketCream).padding(5.dp)) {
                Icon(Icons.Outlined.QrCode2, null, tint = Color(0xFF1B4332), modifier = Modifier.size(44.dp))
            }
        }

        // trail stamps (placeholder progress — backend has no per-base check-ins yet)
        val total = 8
        val done = if (p.checkedIn) 3 else 0
        val phase = GrowthPhase.forProgress(done, total)
        Column(Modifier.padding(20.dp, 14.dp, 20.dp, 0.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.profile_trail_stamps), color = TicketInk, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.5.sp)
                Text(stringResource(R.string.profile_stamps_progress, done, total, stringResource(phase.labelRes)), color = TicketGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Row(Modifier.padding(top = 9.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(total) { i ->
                    val filled = i < done
                    Box(
                        Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp))
                            .background(if (filled) Color(0xFF6FB894) else Color(0xFFF4EEE1)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Park, null, tint = if (filled) Color(0xFF123826) else Color(0xFFCABFA8), modifier = Modifier.size(16.dp))
                    }
                }
            }
            // progress bar
            Box(Modifier.padding(top = 11.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE6DDC9))) {
                Box(Modifier.fillMaxWidth(done.toFloat() / total).height(12.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF40916C)))
            }
        }

        // perforation
        Canvas(Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 16.dp)) {
            val y = size.height / 2
            var x = 0f
            val dash = 10f; val gap = 8f
            while (x < size.width) {
                drawLine(Color(0xFFD8CFBA), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x + dash, y), strokeWidth = 4f)
                x += dash + gap
            }
        }

        // info tiles
        Row(Modifier.padding(20.dp, 0.dp, 20.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(p.groupNumber?.toString() ?: "—", stringResource(R.string.profile_label_group), Modifier.weight(1f))
            InfoTile(p.bloodType ?: "—", stringResource(R.string.profile_label_blood), Modifier.weight(1f))
            InfoTile(if (p.checkedIn) stringResource(R.string.profile_checkin_status) else "—", stringResource(R.string.profile_label_rank), Modifier.weight(1f))
        }

        // detail rows
        Column(Modifier.padding(20.dp, 8.dp, 20.dp, 4.dp)) {
            DetailRow(stringResource(R.string.profile_row_height_weight), heightWeight(p))
            DetailRow(stringResource(R.string.profile_row_contact_phone), p.contactPhone ?: "—")
            DetailRow(
                stringResource(R.string.profile_section_emergency),
                listOfNotNull(p.emergencyContactName, p.emergencyContactPhone).joinToString(" · ").ifBlank { "—" },
                last = true,
            )
        }

        // barcode footer
        Row(Modifier.fillMaxWidth().background(Color(0xFFF3ECDD)).padding(20.dp, 13.dp, 20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).height(34.dp)) {
                var x = 0f
                val widths = listOf(2f, 1f, 3f, 1f, 2f, 4f, 1f, 2f, 3f, 1f, 2f, 1f, 4f, 2f, 1f, 3f, 2f, 1f, 2f, 4f)
                var i = 0
                while (x < size.width) {
                    val w = widths[i % widths.size]
                    drawRect(TicketInk, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
                    x += w + 2f; i++
                }
            }
            Column(Modifier.padding(start = 14.dp), horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.profile_label_student_id), color = TicketMuted, fontSize = 8.5.sp, letterSpacing = 1.5.sp)
                Text(p.studentId ?: "—", fontFamily = Kanit, color = TicketInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TicketChip(text: String, bg: Color, fg: Color, border: Color) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(TicketTileBg).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Kanit, color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
        Text(label.uppercase(), color = TicketMuted, fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TicketMuted, fontSize = 12.sp)
        Text(value, color = TicketInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEFE6D5)))
}

private fun initialOf(p: ParticipantDetail): String =
    (p.firstName ?: p.fullName).trim().take(1).uppercase().ifBlank { "•" }

@Composable
private fun heightWeight(p: ParticipantDetail): String {
    val h = p.heightCm?.let { fmt(it) }
    val w = p.weightKg?.let { fmt(it) }
    return if (h != null && w != null) stringResource(R.string.profile_hw_value, h, w) else "—"
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
