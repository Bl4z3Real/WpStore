package com.metrostore.app.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrostore.app.data.StoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

/** Metro "turnstile" entrance: tiles flip in one after the other. */
fun Modifier.metroAppear(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        a.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = a.value
        rotationX = (1f - a.value) * -75f
        cameraDistance = 14f * density
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}

/** Metro press feedback: the element shrinks slightly while touched. */
fun Modifier.metroPress(onClick: () -> Unit): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val cb by rememberUpdatedState(onClick)
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, tween(110), label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { cb() }
            )
        }
}

@Composable
fun MetroTile(
    modifier: Modifier,
    color: Color,
    index: Int,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.metroAppear(index).metroPress(onClick).background(color), content = content)
}

// ---------------------------------------------------------------------------------------------
// Images / icons
// ---------------------------------------------------------------------------------------------

private val imageCache = LruCache<String, ImageBitmap>(40)

@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    var img by remember(url) { mutableStateOf(url?.let { imageCache.get(it) }) }
    LaunchedEffect(url) {
        if (url != null && img == null && url.startsWith("https://")) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { URL(url).openStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
            if (loaded != null) { imageCache.put(url, loaded); img = loaded }
        }
    }
    return img
}

@Composable
fun AppIcon(app: StoreApp, size: Dp, modifier: Modifier = Modifier) {
    val bmp = rememberRemoteImage(app.iconUrl)
    Box(modifier.size(size).background(Color(app.color)), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            BasicText(app.name.take(1).uppercase(), style = MetroText.tile.copy(fontSize = (size.value * 0.5f).sp))
        }
    }
}

@Composable
fun Stars(rating: Float) {
    if (rating > 0f) {
        val full = rating.roundToInt().coerceIn(0, 5)
        BasicText(
            "★".repeat(full) + "☆".repeat(5 - full),
            style = MetroText.caption.copy(fontFamily = SegoeSymbol, fontSize = 11.sp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Text pieces
// ---------------------------------------------------------------------------------------------

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    BasicText(text.uppercase(), style = MetroText.section, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
fun PivotHeader(small: String, big: String) {
    Column(Modifier.padding(start = 16.dp, top = 20.dp, bottom = 14.dp)) {
        BasicText(small.uppercase(), style = MetroText.section)
        BasicText(big, style = MetroText.pivotTitle)
    }
}

@Composable
fun ActionText(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    BasicText(
        label,
        style = MetroText.body.copy(color = LocalAccent.current, fontSize = 17.sp),
        modifier = modifier.metroPress(onClick).padding(end = 24.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun AppRow(app: StoreApp, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().metroPress(onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app, 64.dp, Modifier.metroAppear(index))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(app.name, style = MetroText.listTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(app.publisher, style = MetroText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(app.price, style = MetroText.caption)
                Spacer(Modifier.width(8.dp))
                Stars(app.rating)
            }
        }
    }
}

@Composable
fun AppCard(app: StoreApp, index: Int, onClick: () -> Unit) {
    Column(Modifier.width(144.dp).metroPress(onClick)) {
        AppIcon(app, 144.dp, Modifier.metroAppear(index))
        Spacer(Modifier.height(6.dp))
        BasicText(app.name.uppercase(), style = MetroText.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(app.price, style = MetroText.caption.copy(fontSize = 12.sp))
            Spacer(Modifier.width(6.dp))
            Stars(app.rating)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Glyph buttons + bottom bars (application bar, as in the reference screenshots)
// ---------------------------------------------------------------------------------------------

enum class Glyph { Search, Download, Share, Play }

private fun DrawScope.drawGlyph(g: Glyph, c: Color) {
    val w = size.width
    val sw = w * 0.05f
    val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(w * x, w * y)
    drawCircle(c, radius = w / 2f - sw, style = st)
    when (g) {
        Glyph.Search -> {
            drawCircle(c, radius = w * 0.14f, center = p(0.46f, 0.45f), style = st)
            drawLine(c, p(0.56f, 0.55f), p(0.70f, 0.69f), sw * 1.3f, StrokeCap.Round)
        }
        Glyph.Download -> {
            drawLine(c, p(0.5f, 0.28f), p(0.5f, 0.62f), sw, StrokeCap.Round)
            drawLine(c, p(0.37f, 0.50f), p(0.5f, 0.63f), sw, StrokeCap.Round)
            drawLine(c, p(0.63f, 0.50f), p(0.5f, 0.63f), sw, StrokeCap.Round)
            drawLine(c, p(0.34f, 0.74f), p(0.66f, 0.74f), sw, StrokeCap.Round)
        }
        Glyph.Share -> {
            drawLine(c, p(0.68f, 0.33f), p(0.34f, 0.50f), sw, StrokeCap.Round)
            drawLine(c, p(0.34f, 0.50f), p(0.68f, 0.67f), sw, StrokeCap.Round)
            drawCircle(c, w * 0.055f, p(0.68f, 0.33f))
            drawCircle(c, w * 0.055f, p(0.34f, 0.50f))
            drawCircle(c, w * 0.055f, p(0.68f, 0.67f))
        }
        Glyph.Play -> {
            val path = Path().apply {
                moveTo(w * 0.42f, w * 0.30f); lineTo(w * 0.42f, w * 0.70f)
                lineTo(w * 0.72f, w * 0.50f); close()
            }
            drawPath(path, c, style = st)
        }
    }
}

@Composable
fun CircleGlyphButton(glyph: Glyph, modifier: Modifier = Modifier, size: Dp = 48.dp, onClick: () -> Unit) {
    Box(modifier.metroPress(onClick).padding(6.dp)) {
        Canvas(Modifier.size(size)) { drawGlyph(glyph, Color.White) }
    }
}

@Composable
fun EllipsisButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.metroPress(onClick).padding(horizontal = 14.dp, vertical = 16.dp)) {
        Canvas(Modifier.size(width = 28.dp, height = 6.dp)) {
            val r = 2.2.dp.toPx()
            for (i in 0..2) {
                drawCircle(Color.White, r, Offset(r + i * (size.width - 2 * r) / 2f, size.height / 2f))
            }
        }
    }
}

class MenuItem(val label: String, val onClick: () -> Unit)
class ActionButton(val glyph: Glyph, val label: String, val onClick: () -> Unit)

/**
 * Bottom bar: round button in the centre + "..." on the right. Tapping "..." expands the bar
 * upwards, showing the button label and a list of large text entries (reference image 3).
 */
@Composable
fun MetroBottomBar(
    glyph: Glyph,
    label: String,
    onCenter: () -> Unit,
    items: List<MenuItem>,
    open: Boolean,
    setOpen: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Color.Black)) {
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            CircleGlyphButton(glyph, Modifier.align(Alignment.Center), onClick = { setOpen(false); onCenter() })
            EllipsisButton(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) { setOpen(!open) }
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(160))
        ) {
            Column(Modifier.fillMaxWidth()) {
                BasicText(
                    label,
                    style = MetroText.small.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                )
                items.forEach { item ->
                    BasicText(
                        item.label,
                        style = MetroText.menuItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .metroPress { setOpen(false); item.onClick() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** Bottom bar with a few round buttons + labels (reference image 4). */
@Composable
fun MetroActionBar(actions: List<ActionButton>) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { a ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircleGlyphButton(a.glyph, onClick = a.onClick)
                BasicText(a.label, style = MetroText.small)
            }
        }
    }
}

/** Screen with content on top and an application bar below; a tap outside closes the menu. */
@Composable
fun MetroScaffold(
    bar: @Composable (open: Boolean, setOpen: (Boolean) -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            content()
            if (open) {
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { open = false } })
            }
        }
        bar(open) { open = it }
    }
}
