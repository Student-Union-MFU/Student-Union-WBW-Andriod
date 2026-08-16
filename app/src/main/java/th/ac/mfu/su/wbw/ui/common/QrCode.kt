package th.ac.mfu.su.wbw.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import kotlin.math.floor

/**
 * A real, scannable QR code, drawn as vector rectangles.
 *
 * Drawn rather than rasterised. `QRCodeWriter` would hand back a bitmap scaled to a pixel
 * size, and a bitmap whose module grid does not divide evenly into its target box comes out
 * with modules a pixel wider than their neighbours — which a scanner reads as noise at the
 * exact moment it matters, in bad light at arm's length. Here the module size is floored to
 * a whole number of pixels and the result is centred in the leftovers, so every module is
 * identical and every edge is hard.
 *
 * [Encoder] rather than `QRCodeWriter` for the same reason: it returns the bare module
 * grid, so the quiet zone is this function's to place rather than something baked into a
 * bitmap at whatever scale the writer chose.
 */
@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
) {
    // Encoding is pure and depends only on the token, which does not change while the
    // screen is open — so it happens once, not on every recomposition of the pass.
    val matrix = remember(content) { encodeQr(content) } ?: return

    Canvas(modifier) {
        val modules = matrix.width
        // Four modules of quiet zone on each side. This is not padding-to-taste: the QR
        // spec requires it, and scanners genuinely fail without it. Because the whole
        // canvas is used for grid-plus-quiet-zone, the caller can put this straight into a
        // white box and get a compliant margin without knowing the module count.
        val cells = modules + QuietZone * 2
        val cell = floor(minOf(size.width, size.height) / cells)
        if (cell < 1f) return@Canvas

        val drawn = cell * cells
        val originX = (size.width - drawn) / 2f + cell * QuietZone
        val originY = (size.height - drawn) / 2f + cell * QuietZone
        val moduleSize = Size(cell, cell)

        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix[x, y].toInt() != 1) continue
                drawRect(
                    color = foreground,
                    topLeft = Offset(originX + x * cell, originY + y * cell),
                    size = moduleSize,
                )
            }
        }
    }
}

/** The QR spec's mandatory margin, in modules. */
private const val QuietZone = 4

/**
 * Encodes [content], or null if it cannot be encoded.
 *
 * Null rather than a throw: a pass that shows no code is a participant who checks in by
 * bib number, which is the fallback the server already supports. A crash on the profile
 * screen would take the whole pass — name, bib, blood type, emergency contact — with it,
 * to avoid losing one of the two ways in.
 */
private fun encodeQr(content: String): ByteMatrix? {
    if (content.isBlank()) return null
    return runCatching {
        Encoder.encode(
            content,
            // Q (25%) rather than the usual M. The payload is 24 hex characters, so the
            // stronger level costs a version bump and nothing that matters — while the code
            // is read off a glossy phone screen, outdoors, over a shoulder, with whatever
            // fingerprints are on it by mid-morning.
            ErrorCorrectionLevel.Q,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        ).matrix
    }.getOrNull()
}
