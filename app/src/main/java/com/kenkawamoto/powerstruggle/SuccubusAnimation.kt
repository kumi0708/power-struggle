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
import androidx.compose.ui.geometry.CornerRadius
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
    class Frame(val bitmap: Bitmap, val feet: Offset, val cableTail: Offset, val usbTip: Offset, val upperRow: Boolean) {
        val vertices = FloatArray((SIP_COLUMNS + 1) * (SIP_ROWS + 1) * 2)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 19f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val frames: List<Frame>

    init {
        val atlas = checkNotNull(BitmapFactory.decodeResource(resources, R.drawable.succubus_usb_sip_atlas))
        val sx = atlas.width / 1143f
        val sy = atlas.height / 1376f
        val anchors = listOf(
            floatArrayOf(339f, 687f, 128f, 568f, 310f, 140f),
            floatArrayOf(282f, 687f, 71f, 568f, 253f, 140f),
            floatArrayOf(339f, 683f, 128f, 563f, 310f, 135f),
            floatArrayOf(282f, 683f, 71f, 563f, 253f, 135f),
        )
        frames = List(4) { i ->
            val left = if (i % 2 == 0) 0 else 571
            val rightEdge = if (i % 2 != 0) 1143 else 571
            val x = (left * sx).toInt()
            val right = (rightEdge * sx).toInt()
            // The generated row boundary has two pixels of adjacent horn/shoe tips.
            // Register clean source regions rather than rendering those stray fragments.
            val y = if (i < 2) 0 else (692 * sy).toInt()
            val bottom = if (i < 2) (687 * sy).toInt() else atlas.height
            val a = anchors[i]
            Frame(Bitmap.createBitmap(atlas, x, y, right - x, bottom - y),
                Offset(a[0] * sx, a[1] * sy), Offset(a[2] * sx, a[3] * sy), Offset(a[4] * sx, a[5] * sy), i < 2)
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
        contentDescription = "成人のサキュバスがスマホにつながったUSB-Cケーブルの先から電力をすすっている。髪と翼が揺れる"
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
            val tail = origin + pose.deform(frame.cableTail) * characterScale
            val tip = origin + pose.deform(frame.usbTip) * characterScale
            // The phone's port follows the free USB-C plug already drawn in each pose.
            drawUsbPhone(tail, sprites.labelPaint)
            drawSipCharacter(frame, pose, origin, characterScale, sprites.paint)
            if (energized) {
                val c1 = origin + pose.deform(frame.cableTail + Offset(-35f, -115f)) * characterScale
                val c2 = origin + pose.deform(frame.usbTip + Offset(-110f, 175f)) * characterScale
                val housing = origin + pose.deform(frame.usbTip + Offset(-37f, 13f)) * characterScale
                drawUsbEnergy(tail, c1, c2, housing, tip, t, energyToPlayer)
                val pulse = (1f + sin(t * 6.5f)) / 2f
                drawCircle(Magic.copy(alpha = 0.18f), 13f + pulse * 8f, tip)
                repeat(4) { i ->
                    val angle = t * 0.8f + i * PI.toFloat() / 2
                    val p = tip + Offset(cos(angle) * 31f, sin(angle) * 21f)
                    drawMagicHeart(p, 3.5f + pulse * 1.5f, Magic.copy(alpha = 0.55f))
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
            if (frame.upperRow) {
                // A faint tip from the next row lies between the soles, outside this pose.
                // Clip only that empty gap, retaining the boots' complete lower outlines.
                clipOutRect(frame.feet.x - 12f, frame.feet.y - 7f,
                    frame.feet.x + 12f, frame.bitmap.height + 8f)
            }
            drawBitmapMesh(frame.bitmap, SIP_COLUMNS, SIP_ROWS, frame.vertices, 0, null, 0, paint)
        }
    }
}

private fun DrawScope.drawUsbPhone(port: Offset, labelPaint: Paint) {
    withTransform({ rotate(-18f, pivot = port) }) {
        val left = port.x - 58f
        val top = port.y - 2f
        drawRoundRect(Color(0xFF42334F), Offset(left, top), Size(116f, 146f), CornerRadius(17f))
        drawRoundRect(Color(0xFF7B5396), Offset(left + 8f, top + 10f), Size(100f, 126f), CornerRadius(10f))
        drawRoundRect(Color(0xFFEAD7FF), Offset(port.x - 22f, top + 31f), Size(44f, 49f), CornerRadius(6f))
        drawRoundRect(Magic, Offset(port.x - 17f, top + 43f), Size(34f, 32f), CornerRadius(3f))
        drawLine(Color(0xFFEAD7FF), Offset(port.x - 7f, top + 27f), Offset(port.x + 7f, top + 27f), 3f, StrokeCap.Round)
        drawIntoCanvas { it.nativeCanvas.drawText("USB-C", port.x, top + 112f, labelPaint) }
        // The generated connector's silver tip meets this port; there is no added cable join.
        drawRoundRect(Color(0xFF20172B), Offset(port.x - 10f, top - 1f), Size(20f, 5f), CornerRadius(2f))
    }
}

private fun DrawScope.drawUsbEnergy(source: Offset, c1: Offset, c2: Offset, housing: Offset, tip: Offset, t: Float, inward: Boolean) {
    val path = Path().apply {
        moveTo(source.x, source.y); cubicTo(c1.x, c1.y, c2.x, c2.y, housing.x, housing.y)
        lineTo(tip.x, tip.y)
    }
    drawPath(path, Magic.copy(alpha = 0.12f), style = Stroke(17f, cap = StrokeCap.Round))
    repeat(10) { i ->
        val progress = (t * 0.42f + i / 10f) % 1f
        val u = if (inward) progress else 1f - progress
        val p = if (u > 0.9f) housing + (tip - housing) * ((u - 0.9f) / 0.1f) else {
            val along = u / 0.9f
            val v = 1f - along
            source * (v * v * v) + c1 * (3f * v * v * along) + c2 * (3f * v * along * along) + housing * (along * along * along)
        }
        drawCircle(Magic.copy(alpha = 0.24f), 7f, p)
        drawCircle(Color.White, 2.6f, p)
    }
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
