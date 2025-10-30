package com.example.composeapp.scratch

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.WebpDecoder
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_BRUSH_RADIUS_PX = 40f
private const val MIN_BRUSH_RADIUS_PX = 10f
private const val MAX_BRUSH_RADIUS_PX = 100f
private const val DEFAULT_IMAGE_URL = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80"

@Composable
fun ScratchOverlayViewer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                    add(WebpDecoder.Factory(context))
                }
            }
            .build()
    }

    var activeImageData by remember { mutableStateOf<Any>(DEFAULT_IMAGE_URL) }
    var activeImageUri by remember { mutableStateOf<Uri?>(null) }
    var overlayImageUri by remember { mutableStateOf<Uri?>(null) }
    var overlayColor by remember { mutableStateOf(Color(0xFFD4AF37)) }
    var brushRadiusPx by remember { mutableFloatStateOf(DEFAULT_BRUSH_RADIUS_PX) }
    var imageAspectRatio by remember { mutableFloatStateOf(4f / 3f) }
    var hasScratched by remember { mutableStateOf(false) }

    val scratchSegments = remember { mutableStateListOf<ScratchStrokeSegment>() }

    val baseImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val resolver = context.contentResolver
            runCatching {
                resolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            activeImageData = it
            activeImageUri = it
            scope.launch {
                scratchSegments.clear()
                hasScratched = false
            }
        }
    }

    val overlayImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val resolver = context.contentResolver
            runCatching {
                resolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            overlayImageUri = it
        }
    }

    val baseImagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(activeImageData)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader
    )

    LaunchedEffect(baseImagePainter.state) {
        val state = baseImagePainter.state
        if (state is AsyncImagePainter.State.Success) {
            val drawable = state.result.drawable
            val width = drawable.intrinsicWidth.takeIf { it > 0 }
                ?: drawable.bounds.width().takeIf { it > 0 }
                ?: 1
            val height = drawable.intrinsicHeight.takeIf { it > 0 }
                ?: drawable.bounds.height().takeIf { it > 0 }
                ?: 1
            val ratio = width.toFloat() / height.toFloat()
            imageAspectRatio = ratio.coerceIn(9f / 21f, 21f / 9f)
        }
    }

    val overlayPainter: Painter = overlayImageUri?.let { uri ->
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .size(Size.ORIGINAL)
                .build(),
            imageLoader = imageLoader
        )
    } ?: remember(overlayColor) {
        ColorPainter(overlayColor)
    }

    LaunchedEffect(overlayImageUri, overlayColor) {
        scratchSegments.clear()
        hasScratched = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ScratchViewerSurface(
            basePainter = baseImagePainter,
            overlayPainter = overlayPainter,
            aspectRatio = imageAspectRatio,
            brushRadiusPx = brushRadiusPx,
            scratchSegments = scratchSegments,
            onScratchStart = { hasScratched = true },
            onScratch = { start, end, radius ->
                scratchSegments += ScratchStrokeSegment(start = start, end = end, radiusPx = radius)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BrushControls(
                brushRadiusPx = brushRadiusPx,
                onRadiusChanged = { brushRadiusPx = it }
            )

            OverlayControls(
                overlayColor = overlayColor,
                hasOverlayImage = overlayImageUri != null,
                onOverlayColorSelected = { color ->
                    overlayColor = color
                    overlayImageUri = null
                },
                onOverlayImageRequested = { overlayImagePicker.launch(arrayOf("image/*")) },
                onOverlayCleared = {
                    overlayImageUri = null
                }
            )

            Divider()

            ImageControls(
                onPickBaseImage = { baseImagePicker.launch(arrayOf("image/*")) },
                onResetImage = {
                    activeImageData = DEFAULT_IMAGE_URL
                    activeImageUri = null
                    scratchSegments.clear()
                    hasScratched = false
                }
            )

            if (!hasScratched) {
                Text(
                    text = "提示：指尖在卡面上滑动即可刮开好运。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = {
                        scratchSegments.clear()
                        hasScratched = false
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "重新覆盖")
                }
            }
        }
    }
}

@Composable
private fun ScratchViewerSurface(
    basePainter: AsyncImagePainter,
    overlayPainter: Painter,
    aspectRatio: Float,
    brushRadiusPx: Float,
    scratchSegments: List<ScratchStrokeSegment>,
    onScratchStart: () -> Unit,
    onScratch: (start: Offset, end: Offset?, radius: Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val targetHeight: Dp = if (aspectRatio > 0f) {
            (maxWidth / aspectRatio).coerceIn(240.dp, 520.dp)
        } else {
            320.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(targetHeight)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.Black)
        ) {
            AsyncImage(
                painter = basePainter,
                contentDescription = "Active image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            ScratchCanvasOverlay(
                painter = overlayPainter,
                brushRadiusPx = brushRadiusPx,
                scratchSegments = scratchSegments,
                onScratchStart = onScratchStart,
                onScratch = onScratch,
                modifier = Modifier.fillMaxSize()
            )

            if (scratchSegments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "开始接福咯！",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScratchCanvasOverlay(
    painter: Painter,
    brushRadiusPx: Float,
    scratchSegments: List<ScratchStrokeSegment>,
    onScratchStart: () -> Unit,
    onScratch: (start: Offset, end: Offset?, radius: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastPosition by remember { mutableStateOf<Offset?>(null) }
    
    Canvas(
        modifier = modifier.pointerInput(brushRadiusPx) {
            detectDragGestures(
                onDragStart = { offset ->
                    onScratchStart()
                    onScratch(offset, null, brushRadiusPx)
                    lastPosition = offset
                },
                onDrag = { change, _ ->
                    change.consume()
                    val end = change.position
                    val start = lastPosition ?: change.previousPosition
                    onScratch(start, end, brushRadiusPx)
                    lastPosition = end
                },
                onDragEnd = { lastPosition = null },
                onDragCancel = { lastPosition = null }
            )
        }
    ) {
        val canvas = drawContext.canvas
        val overlayBounds = androidx.compose.ui.geometry.Rect(Offset.Zero, size)
        val paint = Paint()
        canvas.saveLayer(overlayBounds, paint)

        with(painter) {
            draw(size = size)
        }

        scratchSegments.forEach { segment ->
            if (segment.end == null) {
                drawCircle(
                    color = Color.Transparent,
                    radius = segment.radiusPx,
                    center = segment.start,
                    blendMode = BlendMode.Clear
                )
            } else {
                drawLine(
                    color = Color.Transparent,
                    start = segment.start,
                    end = segment.end,
                    strokeWidth = segment.radiusPx * 2f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Clear
                )
            }
        }

        canvas.restore()
    }
}

private data class ScratchStrokeSegment(
    val start: Offset,
    val end: Offset?,
    val radiusPx: Float
)

@Composable
private fun BrushControls(
    brushRadiusPx: Float,
    onRadiusChanged: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "画笔大小",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${brushRadiusPx.roundToInt()} px",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Slider(
                value = brushRadiusPx,
                onValueChange = onRadiusChanged,
                valueRange = MIN_BRUSH_RADIUS_PX..MAX_BRUSH_RADIUS_PX,
                steps = 0,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            BrushPreview(radiusPx = brushRadiusPx)
        }
    }
}

@Composable
private fun BrushPreview(
    radiusPx: Float,
    modifier: Modifier = Modifier
) {
    val previewSize = 56.dp
    Canvas(
        modifier = modifier
            .size(previewSize)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(8.dp)
    ) {
        val maxRadius = (MAX_BRUSH_RADIUS_PX / 2f)
        val normalized = (radiusPx / MAX_BRUSH_RADIUS_PX).coerceIn(0f, 1f)
        val radius = normalized * (size.minDimension / 2f)
        drawCircle(
            color = MaterialTheme.colorScheme.primary,
            radius = max(radius, 6f)
        )
        drawCircle(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            radius = size.minDimension / 2f,
            style = Stroke(width = 2f)
        )
    }
}

@Composable
private fun OverlayControls(
    overlayColor: Color,
    hasOverlayImage: Boolean,
    onOverlayColorSelected: (Color) -> Unit,
    onOverlayImageRequested: () -> Unit,
    onOverlayCleared: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "覆盖层",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(
                Color(0xFFD4AF37),
                Color(0xFF303030),
                Color(0xFFE53935),
                Color(0xFF1976D2),
                Color(0xFF43A047)
            )
            colors.forEach { color ->
                OverlayColorChip(
                    color = color,
                    selected = !hasOverlayImage && overlayColor == color,
                    onClick = { onOverlayColorSelected(color) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onOverlayImageRequested) {
                Text(text = "自定义图片")
            }
        }

        if (hasOverlayImage) {
            TextButton(onClick = onOverlayCleared) {
                Text(text = "移除覆盖图")
            }
        }
    }
}

@Composable
private fun OverlayColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 36.dp else 32.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
private fun ImageControls(
    onPickBaseImage: () -> Unit,
    onResetImage: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "底图",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPickBaseImage,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "选择图片")
            }
            TextButton(onClick = onResetImage) {
                Text(text = "恢复默认")
            }
        }
    }
}
