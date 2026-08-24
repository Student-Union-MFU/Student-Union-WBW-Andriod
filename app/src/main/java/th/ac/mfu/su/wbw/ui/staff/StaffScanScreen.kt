package th.ac.mfu.su.wbw.ui.staff

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.CheckinResult
import th.ac.mfu.su.wbw.data.remote.dto.StaffCheckpoint
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import java.util.concurrent.Executors

/**
 * The staff check-in scanner.
 *
 * This is the screen that sits where the participant's pass sits — the round button beside
 * the tab bar. The symmetry is the point: on a participant's phone that button shows a QR
 * code, and on a staff phone it reads one. A staff account has no pass to show, and the
 * thing it needs instead at exactly that moment is the camera.
 *
 * What it scans is the participant pass's `qr_token`, the same value
 * [th.ac.mfu.su.wbw.ui.profile.ProfileScreen] encodes. Not the bib, not a user id: the
 * token is the identifier meant to be photographed off somebody's screen, and it is the
 * only one of the four the server will accept here.
 *
 * The manual bib entry underneath is not a convenience. A participant with a dead phone, a
 * cracked screen, or an app they never installed is a person who still walked to this
 * checkpoint, and a scanner with no fallback is a scanner that turns them away.
 */
@Composable
fun StaffScanScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: StaffScanViewModel = viewModel(factory = StaffScanViewModel.Factory),
) {
    val colors = wbwColors
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var torch by remember { mutableStateOf(false) }
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    // Asked for here rather than at launch: a participant account never opens this screen,
    // and a camera prompt makes sense only once it is obvious what it is for.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        viewModel.loadCheckpoints()
        if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, R.string.action_back, onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.scan_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackdrop,
                    modifier = Modifier.weight(1f),
                )
                // The walk finishes near dusk and half the checkpoints are under trees.
                if (hasCamera) {
                    GlassCircleButton(
                        if (torch) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                        R.string.scan_torch,
                    ) { torch = !torch }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (hasCamera) {
                    CameraFeed(
                        enabled = state.reading,
                        torch = torch,
                        onCode = viewModel::onScanned,
                    )
                    Reticle(active = state.reading, accent = colors.accent)
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(colors.forestVoid),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.scan_no_camera_permission),
                            color = colors.onBackdropMuted,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(28.dp)
                                .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        )
                    }
                }

                // The answer, over the lens. It covers the thing it is about, which is
                // correct here: while a result is up the camera is deliberately not
                // reading, so a live preview underneath would be inviting the next scan
                // before this one has been read.
                ResultLayer(
                    visible = state.result != null || state.error != null || state.submitting,
                    result = state.result,
                    error = state.error,
                    submitting = state.submitting,
                    onDismiss = viewModel::clearResult,
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(
                    if (state.reading) R.string.scan_hint else R.string.scan_paused,
                ),
                color = colors.onBackdropMuted,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            CheckpointPicker(
                checkpoints = state.checkpoints,
                selected = state.selected,
                loading = state.loadingCheckpoints,
                error = state.checkpointError,
                onSelect = viewModel::select,
            )

            Spacer(Modifier.height(14.dp))

            BibFallback(
                enabled = state.selected != null && !state.submitting,
                onSubmit = viewModel::onBibEntered,
            )

            // The tab bar's own space. Without it the bib field sits under the floating bar,
            // which is exactly where the keyboard pushes it on a short screen.
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }
}

/**
 * Check somebody in by the number printed on their bib.
 *
 * The path for a participant whose pass cannot be scanned — a flat battery, a broken
 * screen, an app they never installed. The server takes `bib` in place of `qr_token` and
 * lands on the same row, so a person checked in this way is not a lesser record.
 */
@Composable
private fun BibFallback(enabled: Boolean, onSubmit: (Int) -> Unit) {
    val colors = wbwColors
    var text by remember { mutableStateOf("") }

    fun submit() {
        text.trim().toIntOrNull()?.let {
            onSubmit(it)
            text = ""
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(16.dp), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    stringResource(R.string.scan_bib_placeholder),
                    color = colors.onBackdrop.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                )
            }
            BasicTextField(
                value = text,
                // Digits only, filtered here rather than validated on submit: a bib is a
                // number, and a field that accepts letters only to reject them later is
                // teaching the wrong thing at a table with a queue at it.
                onValueChange = { next -> text = next.filter(Char::isDigit).take(6) },
                enabled = enabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = colors.onBackdrop, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.scan_bib_submit),
            color = if (enabled && text.isNotBlank()) colors.accent else colors.onBackdropMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = enabled && text.isNotBlank()) { submit() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * The result, faded over the lens.
 *
 * Its own composable rather than an [AnimatedVisibility] inline in the screen: called from
 * inside the preview's [Box] the enclosing [androidx.compose.foundation.layout.ColumnScope]
 * is still an implicit receiver there, and Kotlin picks the ColumnScope overload over the
 * plain one. Pulling it out of that scope is what makes the ordinary overload resolve.
 */
@Composable
private fun ResultLayer(
    visible: Boolean,
    result: CheckinResult?,
    error: String?,
    submitting: Boolean,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.94f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f),
    ) {
        ResultOverlay(
            result = result,
            error = error,
            submitting = submitting,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The camera, bound to this screen's lifecycle.
 *
 * [enabled] gates the *decoding*, not the preview. Stopping and rebinding the camera between
 * scans would give a black rectangle and a half-second of relayout every time somebody is
 * checked in; leaving the preview running and dropping decoded frames costs nothing visible
 * and keeps the picture steady while the result card is up.
 */
@Composable
private fun CameraFeed(enabled: Boolean, torch: Boolean, onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // One background thread for decoding. ML Kit is called off the main thread and the
    // analyzer keeps only the newest frame, so a slow decode drops frames rather than
    // queueing them into a lag that grows for as long as the screen is open.
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    // Held in a ref the analyzer reads, so flipping `enabled` does not rebind the camera.
    val enabledRef = remember { mutableStateOf(enabled) }
    enabledRef.value = enabled

    // Kept so the torch can be toggled without rebinding. Null until the provider is ready.
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(torch, cameraRef.value) {
        runCatching { cameraRef.value?.cameraControl?.enableTorch(torch) }
    }

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            provider = providerFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || !enabledRef.value) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        codes.firstOrNull()?.rawValue
                            ?.takeIf { it.isNotBlank() }
                            ?.let(onCode)
                    }
                    // Always closed, on both paths. A proxy left open stalls the stream
                    // after a couple of frames and the preview freezes with no error.
                    .addOnCompleteListener { proxy.close() }
            }

            runCatching {
                provider?.unbindAll()
                cameraRef.value = provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { provider?.unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/** The frame. Corners only — a full rectangle reads as a border, corners read as a target. */
@Composable
private fun Reticle(active: Boolean, accent: Color) {
    val tint = if (active) accent else accent.copy(alpha = 0.35f)
    Box(Modifier.fillMaxSize().padding(34.dp)) {
        val corner = 30.dp
        val thickness = 3.dp
        listOf(
            Alignment.TopStart to (true to true),
            Alignment.TopEnd to (true to false),
            Alignment.BottomStart to (false to true),
            Alignment.BottomEnd to (false to false),
        ).forEach { (alignment, dirs) ->
            val (top, start) = dirs
            Box(Modifier.align(alignment)) {
                Box(
                    Modifier
                        .size(width = corner, height = thickness)
                        .background(tint, RoundedCornerShape(2.dp))
                        .align(if (top) Alignment.TopStart else Alignment.BottomStart),
                )
                Box(
                    Modifier
                        .size(width = thickness, height = corner)
                        .background(tint, RoundedCornerShape(2.dp))
                        .align(
                            when {
                                top && start -> Alignment.TopStart
                                top -> Alignment.TopEnd
                                start -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            },
                        ),
                )
            }
        }
    }
}

/**
 * Which checkpoint every stamp is being recorded against.
 *
 * Under the lens rather than over it, and a list rather than a strip of chips, because
 * there are eight of them and any staff member may be standing at any one.
 * `GET /wbw/staff/checkpoints` returns every checkpoint where `requires_checkin` is true —
 * the same set for staff and admin alike — so the restroom and welfare points never appear
 * as somewhere to stamp anybody in. It is deliberately not scoped to whatever
 * `checkpoint_staff` says: people swap bases all afternoon, and that table's job is
 * deciding whose phone an SOS reaches, not who may check somebody in.
 *
 * The whole list stays on screen rather than collapsing to the chosen one. Every scan for
 * the rest of the afternoon is recorded against whatever is selected here, and a value
 * folded away behind a tap is one a staff member cannot notice has drifted.
 */
@Composable
private fun CheckpointPicker(
    checkpoints: List<StaffCheckpoint>,
    selected: StaffCheckpoint?,
    loading: Boolean,
    error: String?,
    onSelect: (StaffCheckpoint) -> Unit,
) {
    val colors = wbwColors

    Text(
        stringResource(R.string.scan_checkpoint_heading),
        color = colors.onBackdropMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(8.dp))

    when {
        loading -> Text(
            stringResource(R.string.scan_loading_checkpoints),
            color = colors.onBackdropMuted,
            fontSize = 13.sp,
        )

        error != null -> Text(
            stringResource(R.string.scan_checkpoints_failed),
            color = colors.danger,
            fontSize = 13.sp,
        )

        // Not a failure, and not empty-state decoration either. Now that the list is the
        // same for everybody this can only mean the event has no check-in checkpoints
        // configured at all — rare, and completely blocking when it happens. Saying so
        // beats a scanner that reads codes and silently declines to do anything with them.
        checkpoints.isEmpty() -> Text(
            stringResource(R.string.scan_no_checkpoints),
            color = colors.danger,
            fontSize = 13.sp,
        )

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            checkpoints.forEach { cp ->
                CheckpointRow(
                    checkpoint = cp,
                    selected = cp.id == selected?.id,
                    onSelect = { onSelect(cp) },
                )
            }
        }
    }
}

/** One checkpoint. Its number when it has one — the trail is walked in order. */
@Composable
private fun CheckpointRow(
    checkpoint: StaffCheckpoint,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = wbwColors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.background(colors.accent, shape)
                } else {
                    Modifier.glass(shape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                },
            )
            .clip(shape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The sequence, where the checkpoint has one. Service points do not, and the
        // server filters those out of this list anyway, so a blank here is rare rather
        // than routine — a dot keeps the names aligned when it happens.
        Box(Modifier.width(26.dp)) {
            Text(
                checkpoint.sequence?.toString() ?: "·",
                color = if (selected) colors.forestVoid.copy(alpha = 0.7f) else colors.onBackdropMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            checkpoint.name,
            color = if (selected) colors.forestVoid else colors.onBackdrop,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.forestVoid,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The answer to one scan: who it was, and anything the checkpoint should know. */
@Composable
private fun ResultOverlay(
    result: CheckinResult?,
    error: String?,
    submitting: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = wbwColors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.forestVoid.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                submitting -> CircularProgressIndicator(
                    color = colors.onBackdropMuted,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(34.dp),
                )

                error != null -> {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(46.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.scan_failed),
                        color = colors.onBackdrop,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = colors.onBackdropMuted, fontSize = 13.sp)
                }

                result != null -> {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        // Amber rather than green for a repeat: it worked, and it is also
                        // not news. A staff member glancing at this needs to tell "counted"
                        // from "counted already" without reading the words.
                        tint = if (result.alreadyCheckedIn) colors.accentSoft else colors.green,
                        modifier = Modifier.size(46.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        result.displayName,
                        color = colors.onBackdrop,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    result.bib?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.staff_case_bib, it),
                            color = colors.onBackdropMuted,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(
                            if (result.alreadyCheckedIn) R.string.scan_already else R.string.scan_counted,
                        ),
                        color = if (result.alreadyCheckedIn) colors.accentSoft else colors.green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    if (result.hasMedicalFlag) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier
                                .glass(RoundedCornerShape(14.dp), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.MedicalServices,
                                contentDescription = null,
                                tint = colors.danger,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            // The flag, never the notes. What was declared is released by
                            // the server only into an open emergency; that this person
                            // declared something is all a checkpoint table needs.
                            Text(
                                stringResource(R.string.scan_medical_flag),
                                color = colors.onBackdrop,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: Int,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    Box(
        Modifier
            .size(42.dp)
            .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = stringResource(contentDescription),
            tint = colors.onBackdrop,
            modifier = Modifier.size(19.dp),
        )
    }
}
