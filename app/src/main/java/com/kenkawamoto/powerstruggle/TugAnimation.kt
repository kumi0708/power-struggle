package com.kenkawamoto.powerstruggle

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Original generated atlases are kept intact; source rectangles are registered in pixel space. */
internal class TugSprites(resources: Resources) {
    internal class Frame(
        val bitmap: Bitmap,
        val feet: Offset,
        val grip: Offset,
        val scale: Float,
    ) {
        val vertices = FloatArray((MESH_COLUMNS + 1) * (MESH_ROWS + 1) * 2)
    }

    val blue: List<Frame>
    val pinkEmotes: Map<Int, Frame>
    val pinkPull: List<Frame>
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    init {
        val emotes = checkNotNull(BitmapFactory.decodeResource(resources, R.drawable.tug_emotes_atlas))
        val pink = checkNotNull(BitmapFactory.decodeResource(resources, R.drawable.tug_pink_pull_atlas))
        fun frame(
            atlas: Bitmap, left: Int, top: Int, right: Int, bottom: Int,
            footX: Float, footY: Float, gripX: Float, gripY: Float, scale: Float,
        ): Frame {
            // Coordinates refer to the approved 1254 px atlas. Density-independent resources
            // retain that size, but keep registration valid if the entire atlas is resampled.
            val sx = atlas.width / 1254f
            val sy = atlas.height / 1254f
            val x = (left * sx).toInt()
            val y = (top * sy).toInt()
            val w = ((right - left) * sx).toInt()
            val h = ((bottom - top) * sy).toInt()
            return Frame(Bitmap.createBitmap(atlas, x, y, w, h),
                Offset(footX * sx, footY * sy), Offset(gripX * sx, gripY * sy), scale / sx)
        }
        val columns = intArrayOf(0, 320, 640, 950, 1254)
        val blueFeetX = floatArrayOf(180f, 180f, 174f, 194f, 180f, 181f, 174f, 187f)
        val blueGripX = floatArrayOf(232f, 230f, 232f, 245f, 243f, 229f, 227f, 226f)
        val blueGripY = floatArrayOf(231f, 231f, 224f, 222f, 211f, 211f, 212f, 214f)
        blue = List(8) { i ->
            val column = i % 4
            val top = if (i < 4) 0 else 332
            val bottom = if (i < 4) 332 else 637
            frame(emotes, columns[column], top, columns[column + 1], bottom,
                blueFeetX[i], if (i < 4) 324f else 295f, blueGripX[i], blueGripY[i], 500f / 290f)
        }
        pinkEmotes = listOf(0, 1, 6, 7).associateWith { i ->
            val column = i % 4
            val top = if (i < 4) 637 else 935
            val bottom = if (i < 4) 935 else 1254
            frame(emotes, columns[column], top, columns[column + 1], bottom,
                150f, if (i < 4) 290f else 298f,
                if (i < 4) 94f else 106f, if (i < 4) 201f else 207f, 500f / 290f)
        }
        // The strongest pink pose has wider swinging hair: register its complete silhouette,
        // rather than assuming the generated drawings have exact equal-cell boundaries.
        pinkPull = listOf(
            frame(pink, 0, 0, 628, 628, 323f, 621f, 151f, 387f, 500f / 625f),
            frame(pink, 628, 0, 1254, 628, 249f, 621f, 159f, 393f, 500f / 625f),
            frame(pink, 0, 628, 710, 1254, 371f, 604f, 387f, 411f, 500f / 625f),
            frame(pink, 710, 628, 1254, 1254, 239f, 604f, 132f, 380f, 500f / 625f),
        )
    }
}

private const val MESH_COLUMNS = 8
private const val MESH_ROWS = 12
private const val SCENE_WIDTH = 1000f
private const val SCENE_HEIGHT = 620f
private const val GROUND_Y = 580f
private val BlueShadow = Color(0xFF7397D4)
private val PinkShadow = Color(0xFFE687AB)

@Composable
internal fun rememberTugSprites(): TugSprites {
    val resources = LocalContext.current.resources
    return remember(resources) { TugSprites(resources) }
}

/** Clock and response belong to the arena; battery polling does not restart the animation. */
private class MotionClock {
    var bluePhase = 0f
    var pinkPhase = 0.45f
    var bluePullAt = -10f
    var pinkPullAt = -10f
    private var lastTime = 0f
    private var lastRope = 0f
    private var lastTaps = 0

    fun update(t: Float, rope: Float, taps: Int, pinkPlayer: Boolean, connected: Boolean) {
        val dt = (t - lastTime).coerceIn(0f, 0.25f)
        lastTime = t
        val delta = rope - lastRope
        lastRope = rope
        if (connected && taps != lastTaps) {
            if (pinkPlayer) { pinkPullAt = t; pinkPhase += 0.22f }
            else { bluePullAt = t; bluePhase += 0.22f }
        }
        lastTaps = taps
        // The original wire protocol carries rope position, not animation events. A tap-sized
        // remote pull triggers the opponent; small passive spring-back changes do not.
        if (connected && delta > 0.035f && t - bluePullAt > 0.12f) {
            bluePullAt = t; bluePhase += 0.22f
        }
        if (connected && delta < -0.035f && t - pinkPullAt > 0.12f) {
            pinkPullAt = t; pinkPhase += 0.22f
        }
        bluePhase += dt * (if (connected) 1.65f + impulse(t - bluePullAt) * 0.7f else 0.26f)
        pinkPhase += dt * (if (connected) 1.65f + impulse(t - pinkPullAt) * 0.7f else 0.26f)
    }
}

private fun impulse(age: Float) = if (age >= 0f) exp(-age * 7f) else 0f

@Composable
internal fun ChibiArena(
    sprites: TugSprites,
    rope: Float,
    pinkPlayer: Boolean,
    connected: Boolean,
    energized: Boolean,
    energyToPlayer: Boolean,
    tapCount: Int,
    modifier: Modifier = Modifier,
) {
    val started = remember { System.nanoTime() }
    var frameTime by remember { mutableLongStateOf(started) }
    val clock = remember { MotionClock() }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameTime = it }
    }
    Canvas(modifier.semantics {
        contentDescription = "青いリボンとピンクのリボンの女の子が、腕と膝を動かして電力の綱を引き合っている"
    }) {
        // Read the clock in the draw phase, so the surrounding screen does not recompose at 60 Hz.
        val t = (frameTime - started).coerceAtLeast(0L) / 1_000_000_000f
        val canonicalRope = if (pinkPlayer) -rope else rope
        clock.update(t, canonicalRope, tapCount, pinkPlayer, connected)
        val sceneScale = min(size.width / SCENE_WIDTH, size.height / SCENE_HEIGHT)
        withTransform({
            translate((size.width - SCENE_WIDTH * sceneScale) / 2, (size.height - SCENE_HEIGHT * sceneScale) / 2)
            scale(sceneScale, sceneScale, pivot = Offset.Zero)
            if (pinkPlayer) scale(-1f, 1f, pivot = Offset(SCENE_WIDTH / 2, SCENE_HEIGHT / 2))
        }) {
            val blue = chooseFrame(sprites, false, clock.bluePhase, canonicalRope, connected)
            val pink = chooseFrame(sprites, true, clock.pinkPhase, -canonicalRope, connected)
            val bluePose = Pose(t, clock.bluePhase, impulse(t - clock.bluePullAt), connected, false)
            val pinkPose = Pose(t, clock.pinkPhase, impulse(t - clock.pinkPullAt), connected, true)
            val blueOrigin = Offset(270f - blue.feet.x * blue.scale, GROUND_Y - blue.feet.y * blue.scale)
            val pinkOrigin = Offset(710f - pink.feet.x * pink.scale, GROUND_Y - pink.feet.y * pink.scale)
            val blueHand = blueOrigin + deform(blue.grip, blue, bluePose) * blue.scale
            val pinkHand = pinkOrigin + deform(pink.grip, pink, pinkPose) * pink.scale
            drawOval(BlueShadow.copy(alpha = 0.12f), Offset(185f, GROUND_Y - 3f), Size(170f, 18f))
            drawOval(PinkShadow.copy(alpha = 0.12f), Offset(625f, GROUND_Y - 3f), Size(170f, 18f))
            val towardBlue = if (pinkPlayer) !energyToPlayer else energyToPlayer
            drawEnergyRope(blueHand, pinkHand, canonicalRope, t, connected, energized, towardBlue)
            drawCharacter(blue, blueOrigin, bluePose, sprites.paint)
            drawCharacter(pink, pinkOrigin, pinkPose, sprites.paint)
            if (connected) {
                drawPullFlash(blueHand, t - clock.bluePullAt, BlueShadow)
                drawPullFlash(pinkHand, t - clock.pinkPullAt, PinkShadow)
            }
        }
    }
}

private fun chooseFrame(sprites: TugSprites, pink: Boolean, phase: Float, advantage: Float, active: Boolean): TugSprites.Frame {
    fun emote(index: Int) = if (pink) sprites.pinkEmotes.getValue(index) else sprites.blue[index]
    if (!active) {
        // A blink lasts roughly 150 ms every four seconds; both girls blink at different times.
        return emote(if (phase % 1f > 0.96f) 1 else 0)
    }
    val step = (phase * 4).toInt() % 4
    if (step == 0 && phase.toInt() % 3 == 2 && abs(advantage) > Battle.SWAP_THRESHOLD) {
        return emote(if (advantage > 0f) 6 else 7)
    }
    return if (pink) sprites.pinkPull[step] else sprites.blue[step + 2]
}

private class Pose(val time: Float, phase: Float, val pull: Float, val active: Boolean, val pink: Boolean) {
    val cycle = sin(phase * 2f * PI.toFloat())
    val breathing = sin(time * 2.6f + if (pink) 0.8f else 0f)
    val hairTime = time * 5.5f
}

/** Pin the soles, bend the torso/knees, and let the trailing hair/ribbon lag behind the pull. */
private fun deform(
    point: Offset, frame: TugSprites.Frame, pose: Pose,
    hairPhase: Float = sin(pose.hairTime + (point.y / frame.feet.y).coerceIn(0f, 1f) * 8f),
): Offset {
    val h = frame.feet.y
    val y = (point.y / h).coerceIn(0f, 1f)
    val upper = (1f - y).coerceAtLeast(0f)
    val planted = ((h - point.y) / (h * 0.12f)).coerceIn(0f, 1f)
    val cycle = pose.cycle
    val facing = if (pose.pink) -1f else 1f
    val lean = if (pose.active) (cycle * 0.012f - pose.pull * 0.035f) * facing else 0f
    val trailing = if (pose.pink) (point.x / frame.bitmap.width - 0.45f) * 2f
        else (0.55f - point.x / frame.bitmap.width) * 2f
    val hair = trailing.coerceIn(0f, 1f) * upper
    val hairWave = hairPhase * hair * h * (if (pose.active) 0.017f else 0.006f)
    val breath = pose.breathing * upper * h * 0.006f
    val crouch = if (pose.active) (0.5f - cycle * 0.5f + pose.pull) * sin(y * PI.toFloat()) * h * 0.018f else 0f
    val knee = if (y > 0.7f && pose.active) cycle * (point.x - frame.feet.x) * 0.03f else 0f
    return Offset(point.x + (lean * h * upper + hairWave + knee) * planted,
        point.y + (crouch - breath) * planted)
}

private fun DrawScope.drawCharacter(frame: TugSprites.Frame, origin: Offset, pose: Pose, paint: Paint) {
    val vertices = frame.vertices
    var index = 0
    for (row in 0..MESH_ROWS) {
        val rowY = frame.bitmap.height * row.toFloat() / MESH_ROWS
        val hairPhase = sin(pose.hairTime + (rowY / frame.feet.y).coerceIn(0f, 1f) * 8f)
        for (column in 0..MESH_COLUMNS) {
            val point = Offset(frame.bitmap.width * column.toFloat() / MESH_COLUMNS,
                rowY)
            val deformed = deform(point, frame, pose, hairPhase)
            vertices[index++] = deformed.x
            vertices[index++] = deformed.y
        }
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val saved = native.save()
        native.translate(origin.x, origin.y)
        native.scale(frame.scale, frame.scale)
        native.drawBitmapMesh(frame.bitmap, MESH_COLUMNS, MESH_ROWS, vertices, 0, null, 0, paint)
        native.restoreToCount(saved)
    }
}

private fun DrawScope.drawEnergyRope(
    left: Offset, right: Offset, rope: Float, t: Float, active: Boolean, energized: Boolean, towardBlue: Boolean,
) {
    val sag = if (active) 5f + (1f - abs(rope)) * 9f else 22f
    fun point(progress: Float): Offset = left + (right - left) * progress +
        Offset(0f, sin(progress * PI.toFloat()) * sag)
    fun strand(phase: Float, amplitude: Float): Path = Path().apply {
        for (i in 0..60) {
            val progress = i / 60f
            val p = point(progress)
            val y = p.y + sin(progress * 22f * PI.toFloat() + phase) * amplitude
            if (i == 0) moveTo(p.x, y) else lineTo(p.x, y)
        }
    }
    val cord = strand(0f, 0f)
    drawPath(cord, Color(0xFFFFC85B).copy(alpha = if (active) 0.22f else 0.1f), style = Stroke(30f, cap = StrokeCap.Round))
    drawPath(cord, Color(0xFFC8B8C9), style = Stroke(14f, cap = StrokeCap.Round))
    drawPath(cord, Color.White, style = Stroke(11f, cap = StrokeCap.Round))
    drawPath(strand(t * 2f, 3.2f), Color(0xFFB9C8E6), style = Stroke(3f))
    drawPath(strand(t * 2f + PI.toFloat(), 3.2f), Color(0xFFFFD778), style = Stroke(3f))
    val knot = point((1f - rope.coerceIn(-1f, 1f)) / 2)
    drawLightning(knot, 32f * (1f + 0.07f * sin(t * 7f)))
    if (energized) {
        repeat(7) { i ->
            val progress = (t * 0.55f + i / 7f) % 1f
            val p = point(if (towardBlue) 1f - progress else progress)
            drawCircle(Color(0xFFFFC34D).copy(alpha = 0.28f), 11f, p)
            drawCircle(Color.White, 3f, p)
        }
    }
    if (active) repeat(4) { i ->
        val a = t * 1.5f + i * PI.toFloat() / 2
        val p = knot + Offset(kotlin.math.cos(a) * 43f, sin(a) * 24f)
        drawLine(Color(0xFFFFC34D).copy(alpha = 0.6f), p - Offset(4f, 0f), p + Offset(4f, 0f), 2f, StrokeCap.Round)
        drawLine(Color(0xFFFFC34D).copy(alpha = 0.6f), p - Offset(0f, 4f), p + Offset(0f, 4f), 2f, StrokeCap.Round)
    }
}

private fun DrawScope.drawLightning(center: Offset, height: Float) {
    val path = Path().apply {
        moveTo(center.x + height * 0.2f, center.y - height)
        lineTo(center.x - height * 0.55f, center.y + height * 0.1f)
        lineTo(center.x - height * 0.08f, center.y + height * 0.1f)
        lineTo(center.x - height * 0.2f, center.y + height)
        lineTo(center.x + height * 0.55f, center.y - height * 0.1f)
        lineTo(center.x + height * 0.08f, center.y - height * 0.1f)
        close()
    }
    drawCircle(Color(0xFFFFD26C).copy(alpha = 0.16f), height * 1.2f, center)
    drawPath(path, Color.White, style = Stroke(6f))
    drawPath(path, Color(0xFFFFCD5B))
}

private fun DrawScope.drawPullFlash(hand: Offset, age: Float, color: Color) {
    if (age in 0f..0.4f) {
        val progress = age / 0.4f
        drawCircle(color.copy(alpha = (1f - progress) * 0.45f), 12f + progress * 35f,
            hand, style = Stroke(3f * (1f - progress)))
    }
}
