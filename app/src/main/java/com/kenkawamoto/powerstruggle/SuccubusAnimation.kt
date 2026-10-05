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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalResources
import androidx.core.graphics.withTranslation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val SIP_COLUMNS = 8
private const val SIP_ROWS = 12
private const val SIP_WIDTH = 760f
private const val SIP_HEIGHT = 820f
private val Magic = Color(0xFFC56AE6)

/** Adult character poses; original transparent artwork remains unchanged on disk. */
private class SuccubusSprites(resources: Resources) {
    class Frame(val bitmap: Bitmap, val feet: Offset, val battery: Offset, val mouth: Offset) {
        val vertices = FloatArray((SIP_COLUMNS + 1) * (SIP_ROWS + 1) * 2)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val frames: List<Frame>

    init {
        val atlas = checkNotNull(BitmapFactory.decodeResource(resources, R.drawable.succubus_sip_atlas))
        val sx = atlas.width / 1143f
        val sy = atlas.height / 1376f
        val anchors = listOf(
            floatArrayOf(333f, 683f, 380f, 275f, 329f, 138f),
            floatArrayOf(270f, 683f, 342f, 275f, 289f, 138f),
            floatArrayOf(335f, 685f, 375f, 273f, 329f, 135f),
            floatArrayOf(272f, 686f, 340f, 277f, 292f, 140f),
        )
        frames = List(4) { i ->
            // The raised wing in the lower-left pose extends past the nominal cell edge.
            // Give it the full gutter and keep its tip out of the lower-right pose.
            val left = if (i % 2 == 0) 0 else if (i == 3) 600 else 571
            val rightEdge = if (i % 2 != 0) 1143 else if (i == 2) 600 else 571
            val x = (left * sx).toInt()
            val right = (rightEdge * sx).toInt()
            // The lower horns start on row 687; put the boundary in the transparent gutter.
            val y = if (i < 2) 0 else (686 * sy).toInt()
            val bottom = if (i < 2) (686 * sy).toInt() else atlas.height
            val a = anchors[i]
            val shiftX = if (i == 3) 29f else 0f
            Frame(Bitmap.createBitmap(atlas, x, y, right - x, bottom - y),
                Offset((a[0] - shiftX) * sx, a[1] * sy),
                Offset((a[2] - shiftX) * sx, a[3] * sy), Offset((a[4] - shiftX) * sx, a[5] * sy))
        }
    }
}

/** A self-running visual loop: this renderer never sends taps or changes USB power roles. */
@Composable
internal fun SuccubusArena(energized: Boolean, energyToPlayer: Boolean, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val sprites = remember(resources) { SuccubusSprites(resources) }
    val started = remember { System.nanoTime() }
    var frameTime by remember { mutableLongStateOf(started) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frameTime = it } }
    Canvas(modifier.semantics {
        contentDescription = "成人のサキュバスがストローで電力をチューチュー吸う。まばたきし、髪と翼が揺れる"
    }) {
        val t = (frameTime - started).coerceAtLeast(0L) / 1_000_000_000f
        // Two small sips, an eyes-closed sip, a wink, then a relaxed pause. No input needed.
        val cycle = t % 4.8f
        val index = when {
            cycle < 0.8f -> 0
            cycle < 1.55f -> 1
            cycle < 2.25f -> 0
            cycle < 3.1f -> 2
            cycle < 3.8f -> 1
            cycle < 4.15f -> 3
            else -> 0
        }
        val frame = sprites.frames[index]
        val scale = min(size.width / SIP_WIDTH, size.height / SIP_HEIGHT)
        val pose = SipPose(t, frame)
        val characterScale = 735f / frame.feet.y
        val origin = Offset(SIP_WIDTH / 2 - frame.feet.x * characterScale, 785f - frame.feet.y * characterScale)
        withTransform({
            translate((size.width - SIP_WIDTH * scale) / 2, (size.height - SIP_HEIGHT * scale) / 2)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawOval(Magic.copy(alpha = 0.12f), Offset(245f, 775f), Size(270f, 22f))
            val battery = origin + pose.deform(frame.battery) * characterScale
            val mouth = origin + pose.deform(frame.mouth) * characterScale
            val source = Offset(70f, 480f)
            if (energized) drawSiphon(source, battery, t, energyToPlayer)
            drawSipCharacter(frame, pose, origin, characterScale, sprites.paint)
            if (energized) {
                // Particles travel along the prop's straw, without obscuring the face.
                val bend = Offset(battery.x + 9f, mouth.y + 58f)
                repeat(5) { i ->
                    val progress = (t * 0.72f + i / 5f) % 1f
                    val u = if (energyToPlayer) progress else 1f - progress
                    val p = battery * ((1f - u) * (1f - u)) + bend * (2f * (1f - u) * u) + mouth * (u * u)
                    drawCircle(Magic.copy(alpha = 0.23f), 8f, p)
                    drawCircle(Color(0xFFFFE4FF), 2.6f, p)
                }
                val pulse = (1f + sin(t * 6.5f)) / 2f
                drawCircle(Magic.copy(alpha = 0.18f), 18f + pulse * 9f, battery)
                repeat(4) { i ->
                    val angle = t * 0.8f + i * PI.toFloat() / 2
                    val p = battery + Offset(cos(angle) * 46f, sin(angle) * 36f)
                    drawMagicHeart(p, 5f + pulse * 2f, Magic.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private class SipPose(t: Float, private val frame: SuccubusSprites.Frame) {
    private val breath = sin(t * 2.4f)
    private val sip = sin(t * 6.5f)
    private val hairTime = t * 3f

    fun deform(point: Offset, wave: Float = sin(hairTime + point.y / frame.feet.y * 7f)): Offset {
        val y = (point.y / frame.feet.y).coerceIn(0f, 1f)
        val upper = 1f - y
        val planted = ((1f - y) / 0.08f).coerceIn(0f, 1f)
        val outside = (abs(point.x - frame.feet.x) / frame.bitmap.width * 3f).coerceIn(0f, 1f)
        // Gentle head/hand sipping, breathing, and larger wing/hair movement at the sides.
        return Offset(point.x + (sip * upper * 1.8f + wave * outside * upper * 6.5f) * planted,
            point.y + (breath * upper * -3.2f + sip * upper * 1.4f) * planted)
    }

    fun rowWave(y: Float) = sin(hairTime + y / frame.feet.y * 7f)
}

private fun DrawScope.drawSipCharacter(frame: SuccubusSprites.Frame, pose: SipPose, origin: Offset, scale: Float, paint: Paint) {
    var index = 0
    for (row in 0..SIP_ROWS) {
        val y = frame.bitmap.height * row.toFloat() / SIP_ROWS
        val wave = pose.rowWave(y)
        for (column in 0..SIP_COLUMNS) {
            val p = pose.deform(Offset(frame.bitmap.width * column.toFloat() / SIP_COLUMNS, y), wave)
            frame.vertices[index++] = p.x
            frame.vertices[index++] = p.y
        }
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.withTranslation(origin.x, origin.y) {
            scale(scale, scale)
            drawBitmapMesh(frame.bitmap, SIP_COLUMNS, SIP_ROWS, frame.vertices, 0, null, 0, paint)
        }
    }
}

private fun DrawScope.drawSiphon(source: Offset, battery: Offset, t: Float, inward: Boolean) {
    val bend = Offset(source.x + 85f, source.y - 90f)
    val path = Path().apply { moveTo(source.x, source.y); quadraticTo(bend.x, bend.y, battery.x, battery.y) }
    drawPath(path, Magic.copy(alpha = 0.13f), style = Stroke(23f, cap = StrokeCap.Round))
    drawPath(path, Magic.copy(alpha = 0.38f), style = Stroke(3.5f, cap = StrokeCap.Round))
    repeat(8) { i ->
        val progress = (t * 0.45f + i / 8f) % 1f
        val u = if (inward) progress else 1f - progress
        val p = source * ((1f - u) * (1f - u)) + bend * (2f * (1f - u) * u) + battery * (u * u)
        drawCircle(Magic.copy(alpha = 0.24f), 10f, p)
        drawCircle(Color.White, 3f, p)
    }
    drawCircle(Magic.copy(alpha = 0.1f), 36f + sin(t * 4f) * 4f, source)
    drawCircle(Magic.copy(alpha = 0.35f), 19f, source)
    val bolt = Path().apply {
        moveTo(source.x + 3f, source.y - 13f); lineTo(source.x - 9f, source.y + 2f)
        lineTo(source.x - 1f, source.y + 2f); lineTo(source.x - 3f, source.y + 13f)
        lineTo(source.x + 9f, source.y - 2f); lineTo(source.x + 1f, source.y - 2f); close()
    }
    drawPath(bolt, Color.White)
}

private fun DrawScope.drawMagicHeart(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius)
        cubicTo(center.x - radius * 2, center.y - radius * 0.1f,
            center.x - radius, center.y - radius * 1.7f, center.x, center.y - radius * 0.5f)
        cubicTo(center.x + radius, center.y - radius * 1.7f,
            center.x + radius * 2, center.y - radius * 0.1f, center.x, center.y + radius)
        close()
    }
    drawPath(path, color)
}
