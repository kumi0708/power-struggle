package com.kenkawamoto.powerstruggle

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

private val Ink = Color(0xFF494562)
private val Blue = Color(0xFF608DE8)
private val Pink = Color(0xFFE878A6)
private val Muted = Color(0xFF8D87A3)
private val Paper = Color(0xFFFFFAFE)
private val Plum = Color(0xFF9A56B8)
private enum class Flow { NONE, IN, OUT }
private enum class VisualMode { TUG, SUCCUBUS }

private data class PlayerSide(
    val flow: Flow,
    val rope: Float,
    val batteryLevel: Int?,
    val peerBatteryLevel: Int?,
    val currentMa: Int?,
    val label: String = "あなたのスマホ",
    val pinkPlayer: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            // The system charging icon remains visible as independent proof of actual power flow.
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        Battle.start(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Pink, surface = Paper)) {
                BattleScreen(
                    previewScene = if (BuildConfig.DEBUG) intent.getStringExtra("preview_scene") else null,
                    faceUp = BuildConfig.DEBUG && intent.getBooleanExtra("preview_face_up", false),
                    previewVisual = if (BuildConfig.DEBUG) intent.getStringExtra("preview_visual") else null,
                )
            }
        }
    }
}

@Composable
private fun BattleScreen(previewScene: String? = null, faceUp: Boolean = false, previewVisual: String? = null) {
    val live by Battle.state.collectAsState()
    val sprites = rememberTugSprites()
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE) }
    var visualMode by rememberSaveable {
        mutableStateOf(if ((previewVisual ?: preferences.getString("visual_mode", "tug")) == "succubus")
            VisualMode.SUCCUBUS else VisualMode.TUG)
    }
    val selectVisual: (VisualMode) -> Unit = {
        visualMode = it
        preferences.edit { putString("visual_mode", if (it == VisualMode.SUCCUBUS) "succubus" else "tug") }
    }
    var demo by remember { mutableStateOf(previewScene in setOf("charging", "draining", "split")) }
    var demoRope by remember { mutableFloatStateOf(if (previewScene == "draining") -0.65f else 0.45f) }
    var showDetails by remember { mutableStateOf(false) }
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The demo changes only local UI state. It never calls the USB tap or power-swap functions.
    LaunchedEffect(demo, visualMode) {
        while (demo && visualMode == VisualMode.TUG) {
            delay(50)
            demoRope = (demoRope * 0.995f - 0.002f).coerceIn(-1f, 1f)
        }
    }
    LaunchedEffect(live.mode) {
        if (live.mode != Mode.NONE) demo = false
    }
    BackHandler(demo) { demo = false }
    BackHandler(!demo && visualMode == VisualMode.SUCCUBUS) { selectVisual(VisualMode.TUG) }
    val s = if (demo) live.copy(
        mode = if (previewScene == "split") Mode.ONE_PHONE else Mode.TWO_PHONES,
        rope = demoRope,
        batteryLevel = 68,
        peerBatteryLevel = 52,
        currentMa = 0,
    ) else live
    val myFlow = when {
        demo && s.rope > Battle.SWAP_THRESHOLD -> Flow.IN
        demo && s.rope < -Battle.SWAP_THRESHOLD -> Flow.OUT
        demo || s.mode == Mode.NONE -> Flow.NONE
        s.charging -> Flow.IN
        else -> Flow.OUT
    }
    val tapMe: () -> Unit = {
        if (demo) demoRope = (demoRope + 0.09f).coerceAtMost(1f) else Battle.tap()
    }
    val tapOther: () -> Unit = {
        if (demo) demoRope = (demoRope - 0.09f).coerceAtLeast(-1f) else Battle.tapOther()
    }
    val startDemo: () -> Unit = { demoRope = if (visualMode == VisualMode.SUCCUBUS) 0.65f else 0f; demo = true }
    val rope = if (s.mode == Mode.NONE) 0f else s.rope
    Box(Modifier.fillMaxSize().background(Paper)) {
        if (s.mode == Mode.ONE_PHONE) {
            Column(Modifier.fillMaxSize()) {
                PlayerView(
                    PlayerSide(myFlow, rope, s.batteryLevel, s.peerBatteryLevel, s.currentMa),
                    sprites = sprites,
                    visualMode = visualMode, onVisualMode = selectVisual,
                    connected = true, demo = demo, compact = true, onTap = tapMe,
                    bottomInset = statusInset,
                    modifier = Modifier.weight(1f).rotate(180f),
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(Pink.copy(alpha = 0.3f)))
                val otherFlow = when (myFlow) { Flow.IN -> Flow.OUT; Flow.OUT -> Flow.IN; Flow.NONE -> Flow.NONE }
                PlayerView(
                    PlayerSide(otherFlow, -rope, s.peerBatteryLevel, s.batteryLevel, null,
                        label = s.partnerName ?: "相手のスマホ", pinkPlayer = true),
                    sprites = sprites,
                    visualMode = visualMode, onVisualMode = selectVisual,
                    connected = true, demo = demo, compact = true, onTap = tapOther,
                    bottomInset = 0.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // Bottom-to-bottom phones: preserve the upstream face-to-face orientation.
            PlayerView(
                PlayerSide(myFlow, rope, s.batteryLevel, s.peerBatteryLevel, s.currentMa,
                    pinkPlayer = !demo && s.mode == Mode.TWO_PHONES && s.role == Role.PLAYER),
                sprites = sprites,
                visualMode = visualMode, onVisualMode = selectVisual,
                connected = s.mode != Mode.NONE, demo = demo, compact = false, onTap = tapMe,
                bottomInset = statusInset,
                modifier = Modifier.fillMaxSize().rotate(if (faceUp) 0f else 180f),
                onDemo = startDemo, onExitDemo = { demo = false }, onDetails = { showDetails = true },
            )
        }
        if (showDetails) {
            AlertDialog(
                modifier = Modifier.rotate(if (faceUp) 0f else 180f),
                onDismissRequest = { showDetails = false },
                title = { Text("接続の詳細") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("USB-Cケーブルで2台を接続し、片方のAndroidでShizukuを起動してください。権限の許可後、アプリを開き直してください。", color = Ink)
                        Spacer(Modifier.height(16.dp))
                        Text("${live.role} · ${live.setup}\nlink ${live.rxPerSec}/s · gap ${live.maxGapMs}ms\ndrops ${live.linkDrops} · swaps ${live.swaps}",
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        live.log.takeLast(8).forEach { Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                        if (live.role == Role.REFEREE && live.mode != Mode.NONE) {
                            TextButton(onClick = Battle::manualSwap) { Text("電力の向きを切り替える") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showDetails = false }) { Text("閉じる") } },
            )
        }
    }
}

@Composable
private fun PlayerView(
    side: PlayerSide,
    sprites: TugSprites,
    visualMode: VisualMode,
    onVisualMode: (VisualMode) -> Unit,
    connected: Boolean,
    demo: Boolean,
    compact: Boolean,
    onTap: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    onDemo: (() -> Unit)? = null,
    onExitDemo: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null,
) {
    val succubus = visualMode == VisualMode.SUCCUBUS
    val playerColor = if (succubus) Plum else if (side.pinkPlayer) Pink else Blue
    val accent by animateColorAsState(
        if (side.flow == Flow.OUT) Pink else playerColor, label = "powerAccent",
    )
    var tapCount by remember { mutableIntStateOf(0) }
    val tap: () -> Unit = { onTap(); tapCount++ }
    val currentTap by rememberUpdatedState(tap)
    val heading = when {
        succubus && !connected -> "チューチューしてみる？"
        succubus && demo && side.flow == Flow.OUT -> "相手へ、チューチュー♡"
        succubus && demo -> "チューチュー、おいしい♡"
        succubus && side.flow == Flow.IN -> "あなたのスマホへ、チューチュー♡"
        succubus && side.flow == Flow.OUT -> "今は相手へおすそわけ♡"
        !connected -> "でんりょく綱引き、はじめよう！"
        demo && side.flow == Flow.IN -> "あなたが優勢！"
        demo && side.flow == Flow.OUT -> "負けないで、引っぱろう！"
        demo -> "勝負はこれから！"
        side.flow == Flow.IN -> "電力をうばっている！"
        else -> "電力をうばわれている…"
    }
    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(if (succubus)
            listOf(Color(0xFFEDE1F8), Paper, Color(0xFFFFE6F2))
        else listOf(Color(0xFFF0F5FF), Paper, Color(0xFFFFEFF6)))),
    ) {
        val short = compact || maxHeight < 580.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (short) 16.dp else 24.dp)
                .padding(top = if (short) 10.dp else 24.dp, bottom = bottomInset + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!short) {
                Text(if (succubus) "サキュバスの充電タイム" else "POWER STRUGGLE", color = Ink, fontWeight = FontWeight.Black,
                    fontSize = if (succubus) 23.sp else 25.sp, letterSpacing = if (succubus) 0.sp else 2.sp)
                Text(if (succubus) "USB-Cから、でんりょくいただき♡" else "ふたりで、でんりょく綱引き。", color = Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            }
            VisualModeSelector(visualMode, onVisualMode, short)
            Spacer(Modifier.height(if (short) 5.dp else 12.dp))
            if (demo) {
                Text("おためし · 電力移動なし · 電池残量はサンプル", color = Muted,
                    fontSize = if (short) 9.sp else 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BatteryBadge(side.label, side.batteryLevel, playerColor, Modifier.weight(1f))
                BatteryBadge("相手", side.peerBatteryLevel, if (side.pinkPlayer) Blue else Pink, Modifier.weight(1f))
            }
            Spacer(Modifier.height(if (short) 6.dp else 14.dp))
            Text(heading, color = accent, fontWeight = FontWeight.ExtraBold,
                fontSize = if (short) 16.sp else 21.sp, textAlign = TextAlign.Center)
            Text(
                when {
                    succubus && demo -> "USB-Cの先から、ずっとチューチュー♡"
                    succubus && !connected -> "USB-Cからすするアニメーションをおためし"
                    demo -> "青い子とピンクの子、どっちが勝つかな？"
                    !connected -> "USB-Cで2台のスマホをつないでね"
                    side.currentMa != null -> "実際のバッテリー電流  ${side.currentMa.signed()} mA"
                    else -> "こちら側をタップして電力を取り返そう"
                },
                color = Muted, fontSize = if (short) 10.sp else 12.sp,
                modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center,
            )
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(connected, succubus) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach {
                                    if (connected && !succubus && it.changedToDown()) currentTap()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ArenaSparkles(accent, Modifier.fillMaxSize())
                if (succubus) {
                    SuccubusArena(
                        energized = connected && (demo || side.flow != Flow.NONE),
                        energyToPlayer = side.flow != Flow.OUT,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else ChibiArena(
                    sprites = sprites,
                    rope = side.rope,
                    pinkPlayer = side.pinkPlayer,
                    connected = connected,
                    energized = connected && side.flow != Flow.NONE,
                    energyToPlayer = side.flow == Flow.IN,
                    tapCount = tapCount,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (succubus) {
                Text(if (connected) "自動でチューチュー中 ♡" else "小悪魔と、ひと息。", color = Plum,
                    fontWeight = FontWeight.Bold, fontSize = if (short) 12.sp else 16.sp,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.8f))
                        .fillMaxWidth().padding(if (short) 8.dp else 14.dp), textAlign = TextAlign.Center)
            } else TugMeter(side.rope, playerColor, short)
            Spacer(Modifier.height(if (short) 6.dp else 14.dp))
            if (!succubus || !connected) Button(
                onClick = { if (connected) tap() else onDemo?.invoke() },
                enabled = connected || onDemo != null,
                colors = ButtonDefaults.buttonColors(containerColor = playerColor, contentColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(if (short) 46.dp else 60.dp),
            ) {
                Text(if (succubus) "チューチューを眺める" else if (connected) "タップで引っぱる！" else "おためしで遊ぶ", fontWeight = FontWeight.ExtraBold,
                    fontSize = if (short) 17.sp else 20.sp)
            }
            if (!short) {
                Text(if (succubus && connected) "電力の向きはスマホの充電状態に合わせて表示"
                    else if (connected) "イラストをタップしても引っぱれるよ" else "本番は片方のスマホでShizukuを起動してね",
                    color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                if (demo && onExitDemo != null) {
                    TextButton(onClick = onExitDemo) { Text("接続画面に戻る", color = Muted, fontSize = 12.sp) }
                } else if (onDetails != null) {
                    TextButton(onClick = onDetails) { Text("接続の詳細", color = Muted, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun VisualModeSelector(mode: VisualMode, onSelect: (VisualMode) -> Unit, compact: Boolean) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.7f)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        VisualMode.entries.forEach { option ->
            val selected = option == mode
            val color = if (option == VisualMode.SUCCUBUS) Plum else Blue
            TextButton(onClick = { onSelect(option) },
                modifier = Modifier.weight(1f).height(if (compact) 34.dp else 42.dp)
                    .semantics { this.selected = selected },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) color else Color.Transparent,
                    contentColor = if (selected) Color.White else Muted),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(if (option == VisualMode.TUG) "綱引き" else "サキュバス ♡",
                    fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BatteryBadge(label: String, level: Int?, color: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, color.copy(alpha = 0.14f), RoundedCornerShape(18.dp)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(width = 14.dp, height = 21.dp)) {
            val terminal = 2.dp.toPx()
            drawRoundRect(color.copy(alpha = 0.18f), topLeft = Offset(0f, terminal),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - terminal),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawLine(color, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.65f, 0f),
                strokeWidth = terminal, cap = StrokeCap.Round)
            val fraction = (level ?: 0).coerceIn(0, 100) / 100f
            val inset = 2.dp.toPx()
            val height = (size.height - terminal - inset * 2) * fraction
            drawRoundRect(color, topLeft = Offset(inset, size.height - inset - height),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
            Text(level?.let { "$it%" } ?: "—", color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TugMeter(rope: Float, color: Color, compact: Boolean) {
    val position by animateFloatAsState(rope.coerceIn(-1f, 1f), label = "knotPosition")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.8f))
        .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("あなたへ", color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("綱の位置", color = Muted, fontSize = 10.sp)
            Text("相手へ", color = if (color == Pink) Blue else Pink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val inset = 9.dp.toPx()
            val start = inset
            val end = size.width - inset
            val middle = size.width / 2
            val y = size.height / 2
            val x = start + (1f - position) * 0.5f * (end - start)
            drawLine(Color(0xFFEDE7F3), Offset(start, y), Offset(end, y), 6.dp.toPx(), StrokeCap.Round)
            drawLine(color.copy(alpha = 0.45f), Offset(middle, y), Offset(x, y), 6.dp.toPx(), StrokeCap.Round)
            for (direction in listOf(-1f, 1f)) {
                val thresholdX = middle + direction * Battle.SWAP_THRESHOLD * (end - start) / 2
                drawLine(Muted.copy(alpha = 0.45f), Offset(thresholdX, y - 5.dp.toPx()),
                    Offset(thresholdX, y + 5.dp.toPx()), 1.dp.toPx())
            }
            drawCircle(Color(0xFFFFE6A0), 10.dp.toPx(), Offset(x, y))
            drawCircle(Color(0xFFFFB84D), 6.dp.toPx(), Offset(x, y))
            drawCircle(Color.White, 2.dp.toPx(), Offset(x - 1.dp.toPx(), y - 2.dp.toPx()))
        }
    }
}

@Composable
private fun ArenaSparkles(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stars = listOf(0.1f to 0.23f, 0.86f to 0.16f, 0.18f to 0.77f, 0.92f to 0.73f, 0.52f to 0.12f)
        stars.forEachIndexed { index, (x, y) ->
            val center = Offset(size.width * x, size.height * y)
            val radius = (if (index % 2 == 0) 5 else 3).dp.toPx()
            drawLine(color.copy(alpha = 0.3f), center - Offset(radius, 0f), center + Offset(radius, 0f),
                2.dp.toPx(), StrokeCap.Round)
            drawLine(color.copy(alpha = 0.3f), center - Offset(0f, radius), center + Offset(0f, radius),
                2.dp.toPx(), StrokeCap.Round)
        }
    }
}

private fun Int.signed() = if (this > 0) "+$this" else "$this"
