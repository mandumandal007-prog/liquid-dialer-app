package com.mandu.liquiddialer.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/** .bg-blobs: three big accent blobs with blur(72px), drifting. */
@Composable
fun BlobBackground(modifier: Modifier = Modifier, strong: Boolean = false) {
    val p = LocalLd.current
    val cfg = LocalConfiguration.current
    val vmax = maxOf(cfg.screenWidthDp, cfg.screenHeightDp).dp
    val inf = rememberInfiniteTransition(label = "blobs")
    val t1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b1")
    val t2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(32000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b2")
    val t3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(38000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b3")
    val m = if (p.motion) 1f else 0f
    val blurDp = (72f * (p.blur / 22f)).coerceIn(0f, 140f).dp
    Box(modifier.fillMaxSize().background(p.bg)) {
        if (strong) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bg2, p.bg))))
        val size = vmax * 0.62f
        @Composable
        fun blob(x: Dp, y: Dp, t: Float, color: Color, centerX: Float, centerY: Float, alpha: Float) {
            Box(
                Modifier
                    .offset(x + vmax * 0.06f * t * m, y + vmax * 0.04f * t * m)
                    .size(size)
                    .graphicsLayer { val s = 1f + 0.12f * t * m; scaleX = s; scaleY = s; this.alpha = alpha }
                    .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(blurDp, BlurredEdgeTreatment.Unbounded) else Modifier)
                    .background(Brush.radialGradient(0f to color, 0.66f to Color.Transparent), CircleShape)
            )
        }
        val w = cfg.screenWidthDp.dp; val h = cfg.screenHeightDp.dp
        blob(-vmax * 0.22f, -vmax * 0.26f, t1, p.accent.pct(.75f), .35f, .35f, p.blobAlpha)
        blob(w - size + vmax * 0.26f, h - size + vmax * 0.30f, t2, p.accent2.pct(.65f), .6f, .4f, p.blobAlpha)
        blob(w - size + vmax * 0.34f, h * 0.28f, t3, p.accent.pct(.40f), .5f, .5f, if (p.isLight) p.blobAlpha else .35f)
    }
}

/** .glass painted (gradient glass2->glass, 1px stroke, inset top highlight, soft shadow). */
fun Modifier.glass(p: LdPalette, shape: Shape, shadow: Dp = 8.dp, highlight: Float = .26f): Modifier = this
    .shadow(shadow, shape, clip = false, ambientColor = Color.Black.copy(alpha = if (p.isLight) .10f else .22f), spotColor = Color.Black.copy(alpha = if (p.isLight) .12f else .30f))
    .clip(shape)
    .background(Brush.linearGradient(listOf(p.glass2, p.glass), start = Offset(0f, 0f), end = Offset.Infinite))
    .drawWithContent {
        drawContent()
        // inset 0 1px 0 rgba(255,255,255,.26)
        drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = highlight), (2.dp.toPx() / size.height).coerceIn(0.001f, 1f) to Color.Transparent))
    }
    .border(1.dp, p.stroke, shape)

/** Floating glass with real backdrop blur (haze) when a source exists — top bar, nav, banner, drawer, sheet. */
@Composable
fun Modifier.liquid(shape: Shape, shadow: Dp = 16.dp): Modifier {
    val p = LocalLd.current
    val haze = LocalHaze.current
    var m = this.shadow(shadow, shape, clip = false, ambientColor = Color.Black.copy(alpha = .3f), spotColor = Color.Black.copy(alpha = .45f)).clip(shape)
    if (haze != null) {
        m = m.hazeChild(haze, HazeStyle(
            backgroundColor = p.bg,
            tint = HazeTint(p.glass2.copy(alpha = (p.glass2.alpha + .04f).coerceAtMost(1f))),
            blurRadius = p.blur.dp,
            noiseFactor = 0.04f,
            fallbackTint = HazeTint(p.bg2.copy(alpha = .86f)),
        ))
    }
    return m
        .background(Brush.linearGradient(listOf(p.glass2.copy(alpha = p.glass2.alpha * .6f), p.glass.copy(alpha = p.glass.alpha * .5f))))
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = .26f), (2.dp.toPx() / size.height).coerceIn(0.001f, 1f) to Color.Transparent))
        }
        .border(1.dp, p.stroke, shape)
}

@Composable
fun pressScale(src: MutableInteractionSource, pressed: Float = .92f): Float {
    val isP by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (isP) pressed else 1f, tween(140), label = "press")
    return s
}

@Composable
fun LdIcon(v: ImageVector, size: Dp = 22.dp, tint: Color = LocalLd.current.text, modifier: Modifier = Modifier) =
    Icon(v, null, modifier.size(size), tint = tint)

/** .icon-btn (+ .glass / .danger / .on) */
@Composable
fun IconBtn(icon: ImageVector, size: Dp = 42.dp, iconSize: Dp = 22.dp, on: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val p = LocalLd.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src)
    val base = Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(size)
    val m = when {
        danger -> base.shadow(10.dp, CircleShape, ambientColor = Color(0x66FF3C50), spotColor = Color(0x66FF3C50)).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xF2FF5D6C), Color(0xE6D6263A)))).border(1.dp, Color.White.copy(alpha = .22f), CircleShape)
        on -> base.clip(CircleShape).background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .86f), p.accent)))
        else -> base.glass(p, CircleShape, 6.dp)
    }
    Box(m.clickable(src, null, onClick = onClick), contentAlignment = Alignment.Center) {
        LdIcon(icon, iconSize, if (danger || on) Color.White else p.text)
    }
}

fun avatarColor(seed: String): Color {
    val palette = listOf(0xFF6EA8FF, 0xFFC084FC, 0xFF34D399, 0xFFFBBF24, 0xFFF472B6, 0xFF38BDF8, 0xFFA3E635, 0xFFFB923C, 0xFF818CF8, 0xFF2DD4BF)
    return Color(palette[(seed.hashCode() and 0x7fffffff) % palette.size])
}

fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() && it[0].isLetter() }
    return when {
        parts.isEmpty() -> "#"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}

/** .avatar (gradient var(--av) -> 52% mix with #0b1224, inset highlight, coloured shadow) */
@Composable
fun Avatar(name: String, photo: String?, size: Dp = 46.dp, fontSize: Float = 15f, glow: Boolean = false, seed: String = name) {
    val av = avatarColor(seed)
    val m = Modifier
        .then(if (glow) Modifier.drawBehind {
            drawCircle(av.copy(alpha = .14f), radius = size.toPx() / 2 + 10.dp.toPx())
        } else Modifier)
        .shadow(if (glow) 26.dp else 6.dp, CircleShape, ambientColor = av.copy(alpha = .38f), spotColor = av.copy(alpha = .38f))
        .size(size).clip(CircleShape)
        .background(Brush.linearGradient(listOf(av, mix(av, Color(0xFF0B1224), .52f))))
        .drawWithContent { drawContent(); drawCircle(Brush.verticalGradient(0f to Color.White.copy(alpha = .42f), 0.04f to Color.Transparent), style = Stroke(1.dp.toPx())) }
    Box(m, contentAlignment = Alignment.Center) {
        val bmp = rememberPhoto(photo)
        if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(initials(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, letterSpacing = .4.sp)
    }
}

@Composable
fun LdText(text: String, size: Float, weight: FontWeight = FontWeight.Normal, color: Color = LocalLd.current.text, modifier: Modifier = Modifier,
           maxLines: Int = 1, letter: Float = 0f) =
    Text(text, modifier, color = color, fontSize = size.sp, fontWeight = weight, maxLines = maxLines, overflow = TextOverflow.Ellipsis, letterSpacing = letter.sp)

/** .switch */
@Composable
fun LdSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalLd.current
    val x by animateDpAsState(if (checked) 18.dp else 0.dp, tween(250), label = "sw")
    Box(Modifier.size(46.dp, 28.dp).clip(RoundedCornerShape(999.dp))
        .background(if (checked) p.accent else if (p.isLight) Color(0x290F172A) else Color.White.copy(alpha = .16f))
        .border(1.dp, if (checked) Color.Transparent else p.stroke, RoundedCornerShape(999.dp))
        .clickable { onChange(!checked) }) {
        Box(Modifier.padding(start = 3.dp + x, top = 3.dp).size(20.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
    }
}

/** .segmented */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalLd.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (p.isLight) Color(0x0D0F172A) else Color.White.copy(alpha = .06f))
        .border(1.dp, p.stroke, RoundedCornerShape(16.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .then(if (on) Modifier.background(Brush.linearGradient(listOf(mix(p.accent, Color.White, .85f), p.accent))) else Modifier)
                .clickable { onSelect(i) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                LdText(o, 13f, FontWeight.SemiBold, if (on) Color.White else p.muted)
            }
        }
    }
}

/** .sim-chip */
@Composable
fun SimChip(text: String, color: Color = LocalLd.current.muted, live: Boolean = false, onClick: (() -> Unit)? = null) {
    val p = LocalLd.current
    Row(Modifier.glass(p, RoundedCornerShape(999.dp), 4.dp).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (live) LiveDot()
        LdText(text, 11.5f, FontWeight.SemiBold, color, letter = .4f)
    }
}

@Composable
fun LiveDot(color: Color = LocalLd.current.ok) {
    val inf = rememberInfiniteTransition(label = "ping")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000)), label = "ping")
    Box(Modifier.size(8.dp).drawBehind {
        val r = size.minDimension / 2
        if (t < .7f) drawCircle(color.copy(alpha = .55f * (1 - t / .7f)), radius = r + 9.dp.toPx() * (t / .7f))
        drawCircle(color, r)
    })
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, radius: Dp = 24.dp, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalLd.current
    Column(modifier.fillMaxWidth().glass(p, RoundedCornerShape(radius)).padding(padding), content = content)
}

@Composable
fun rememberPhoto(uri: String?): ImageBitmap? {
    val ctx = LocalContext.current
    val st = produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(Uri.parse(uri))?.use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (e: Exception) { null }
        }
    }
    return st.value
}
