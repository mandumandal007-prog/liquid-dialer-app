package com.mandu.liquiddialer.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mandu.liquiddialer.LdApp
import kotlinx.coroutines.launch

/** .sheet bottom sheet with scrim */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalLd.current
    BackHandler { onDismiss() }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val y by animateFloatAsState(if (shown) 0f else 1f, tween(320, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)), label = "sheet")
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    var sheetH by remember { mutableIntStateOf(1) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f * (1 - y))).clickable(remember { MutableInteractionSource() }, null) { onDismiss() })
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { sheetH = it.height }
                .graphicsLayer { translationY = size.height * y + dragY.value }
                .liquid(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(p.bg2.copy(alpha = if (p.isLight) .55f else .72f))
                .clickable(remember { MutableInteractionSource() }, null) {}
                .navigationBarsPadding().imePadding()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * .88f).dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().height(26.dp).testTag("sheet_handle")
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (dragY.value > sheetH * .22f) { dragY.animateTo(sheetH.toFloat(), tween(140)); onDismiss() }
                                else dragY.animateTo(0f, spring(dampingRatio = .55f, stiffness = 600f))
                            }
                        },
                        onDragCancel = { scope.launch { dragY.animateTo(0f, spring(dampingRatio = .55f, stiffness = 600f)) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { dragY.snapTo((dragY.value + drag).coerceAtLeast(0f)) }
                    }
                }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 5.dp).clip(RoundedCornerShape(99.dp)).background(p.muted.copy(alpha = .4f)))
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SheetTitle(t: String) { LdText(t, 17f, FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, start = 2.dp), maxLines = 2) }

/** .text-input */
@Composable
fun LdField(value: String, onChange: (String) -> Unit, placeholder: String, minLines: Int = 1, phone: Boolean = false, modifier: Modifier = Modifier) {
    val p = LocalLd.current
    var focused by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = if (p.isLight) .5f else .06f))
        .border(1.dp, if (focused) p.accent else p.stroke, RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) LdText(placeholder, 14f, color = p.muted)
        BasicTextField(value, onChange, Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = p.text, fontSize = 14.sp), minLines = minLines, cursorBrush = SolidColor(p.accent),
            keyboardOptions = if (phone) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default)
    }
}


@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, danger: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalLd.current
    val c = if (danger) p.danger else p.accent
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, .97f)
    Box(modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s }.alpha(if (enabled) 1f else .5f)
        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = c.pct(.4f), spotColor = c.pct(.4f)).clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(mix(c, Color.White, .86f), c)))
        .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(16.dp))
        .clickable(src, null, enabled = enabled, onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        LdText(text, 14.5f, FontWeight.Bold, Color.White)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalLd.current
    Box(modifier.fillMaxWidth().glass(p, RoundedCornerShape(16.dp), 4.dp).clickable(onClick = onClick).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
        LdText(text, 14f, FontWeight.SemiBold, p.accent)
    }
}

val KEY_DEFS = listOf("1" to "", "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "", "0" to "+", "#" to "")

private var toneGen: ToneGenerator? = null
fun playTone(k: String) {
    if (!LdApp.settings.value.keyTones) return
    val tone = when (k) { "*" -> ToneGenerator.TONE_DTMF_S; "#" -> ToneGenerator.TONE_DTMF_P; else -> ToneGenerator.TONE_DTMF_0 + (k.toIntOrNull() ?: 0) }
    try {
        if (toneGen == null) toneGen = ToneGenerator(AudioManager.STREAM_DTMF, 70)
        toneGen?.startTone(tone, 120)
    } catch (_: Exception) { }
}

/** .keypad grid of .key glass circles */
@Composable
fun Keypad(onKey: (String) -> Unit, onLong: ((String) -> Unit)? = null, small: Boolean = false, maxKey: Dp? = null, keyDp: Dp? = null, letters: Boolean = true, rowGap: Dp? = null, tag: String? = null) {
    val p = LocalLd.current
    val w = LocalConfiguration.current.screenWidthDp
    val keySize = (keyDp?.value ?: (w * (if (small) .15f else .19f)).coerceIn(if (small) 44f else 54f, if (small) 62f else 92f)).let { k -> maxKey?.let { minOf(k, it.value) } ?: k }.dp
    val gap = (keySize.value * .16f).coerceIn(7f, 14f).dp
    val haptic = LocalHapticFeedback.current
    val tapHaptic = com.mandu.liquiddialer.LdApp.settings.collectAsState().value.hapticKeys
    Column(Modifier.widthIn(max = 400.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier), verticalArrangement = Arrangement.spacedBy(rowGap ?: gap)) {
        KEY_DEFS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap * 2.1f)) {
                row.forEach { (d, sub) ->
                    var pressed by remember { mutableStateOf(false) }
                    val sc by animateFloatAsState(if (pressed) .93f else 1f, tween(120), label = "k")
                    Box(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.size(keySize)
                        .glass(p, CircleShape, 8.dp, .30f)
                        .background(if (pressed) p.glass2 else Color.Transparent)
                        .pointerInput(d) {
                            detectTapGestures(
                                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                                onTap = { if (tapHaptic) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); playTone(d); onKey(d) },
                                onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (onLong != null) onLong(d) else onKey(d) },
                            )
                        }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            LdText(d, if (small) 22f else keySize.value * .40f, FontWeight.Medium)
                            if (letters && sub.isNotEmpty()) LdText(sub, if (small) 8.5f else 9.5f, FontWeight.Bold, p.muted, letter = 1.6f)
                        }
                    }
                }
            }
        }
    }
}
