package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.launch

private const val MODEL_PATH = "models/plant.glb"

// Native model bounds (CommonTree_2): ~6.97 tall (Y -0.24..6.73), centre at Y≈3.24.
private const val TREE_HEIGHT = 6.97f
private const val TREE_CENTER_Y = 3.24f
// At full growth the tree is FIT_UNITS tall in world space.
private const val FIT_UNITS = 2.25f

/**
 * The 3D plant hero: a single low-poly tree ("CommonTree_2", CC0, via Poly Pizza) over the forest
 * background — no capsule. ONE model that **grows by scaling**: [growth] (0..1) drives the tree from a
 * small sapling up to full size, kept vertically centred as it grows, so every stage is the exact same
 * tree — perfectly consistent. Drag to rotate (subtle spring-back).
 */
@Composable
fun PlantHero(growth: Float, modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(0f, 0.1f, 3.7f)
        rotation = Float3(-2f, 0f, 0f)
    }
    val mainLight = rememberMainLightNode(engine) {
        // Night scene — soft key so the foliage keeps its colour and doesn't blow out.
        intensity = 30_000f
        rotation = Float3(-50f, -28f, 0f)
    }

    // Loader frees the asset on dispose, so navigating away doesn't crash the engine.
    val model = remember(engine) {
        ModelNode(modelInstance = modelLoader.createModelInstance(MODEL_PATH), autoAnimate = false)
    }

    val grown by animateFloatAsState(
        targetValue = growth.coerceIn(0f, 1f),
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "grow",
    )
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val yaw = remember { Animatable(0f) }
    val pitch = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    SideEffect {
        // Growth 0..1 → the tree spans 40%..100% of its full size.
        val fraction = 0.4f + 0.6f * grown
        val s = (FIT_UNITS / TREE_HEIGHT) * fraction
        model.scale = Float3(s, s, s)
        model.position = Float3(0f, -TREE_CENTER_Y * s, 0f) // keep it vertically centred as it scales
        model.rotation = Float3(pitch.value + sway * 1.0f, yaw.value + sway * 2.2f, 0f)
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            mainLightNode = mainLight,
            childNodes = listOf(model),
            isOpaque = false,
        )
        // Transparent overlay to capture drags for a subtle rotate (spring back to front on release).
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch { yaw.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) }
                            scope.launch { pitch.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) }
                        },
                    ) { change: PointerInputChange, drag: Offset ->
                        change.consume()
                        scope.launch { yaw.snapTo((yaw.value + drag.x * 0.35f).coerceIn(-60f, 60f)) }
                        scope.launch { pitch.snapTo((pitch.value - drag.y * 0.15f).coerceIn(-20f, 20f)) }
                    }
                }
        )
    }
}
