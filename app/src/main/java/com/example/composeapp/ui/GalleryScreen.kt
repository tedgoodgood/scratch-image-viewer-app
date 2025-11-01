package com.example.composeapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ScratchSegment
import com.example.composeapp.viewmodel.GalleryViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val systemUiController = rememberSystemUiController()
    val backgroundColor = MaterialTheme.colorScheme.background
    val useDarkIcons = backgroundColor.luminance() > 0.5f

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.selectImages(uris)
        }
    }

    val overlayPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.selectOverlay(uri)
    }

    LaunchedEffect(state.error) {
        val message = state.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isFullscreen, backgroundColor, useDarkIcons) {
        if (state.isFullscreen) {
            systemUiController.isSystemBarsVisible = false
            systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
        } else {
            systemUiController.isSystemBarsVisible = true
            systemUiController.setSystemBarsColor(backgroundColor, darkIcons = useDarkIcons)
        }
    }

    if (state.isFullscreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (state.hasImages) {
                FullscreenViewer(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (state.hasImages) {
                                "${state.currentIndex + 1} / ${state.images.size}"
                            } else {
                                "Scratch Gallery"
                            }
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() },
                            enabled = state.hasImages
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen"
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            if (state.isLoading && !state.hasImages) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!state.hasImages) {
                EmptyGalleryContent(
                    onSelectImages = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                GalleryContent(
                    state = state,
                    viewModel = viewModel,
                    onSelectImages = { imagePicker.launch(arrayOf("image/*")) },
                    onSelectOverlay = { overlayPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun EmptyGalleryContent(
    onSelectImages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No images selected",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select images to start scratching",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectImages) {
            Text("Select Images")
        }
    }
}

@Composable
private fun GalleryContent(
    state: GalleryState,
    viewModel: GalleryViewModel,
    onSelectImages: () -> Unit,
    onSelectOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScratchViewerCard(
            state = state,
            viewModel = viewModel
        )

        NavigationControls(
            state = state,
            onPrevious = viewModel::goToPrevious,
            onNext = viewModel::goToNext
        )

        Divider()

        BrushControls(
            brushSize = state.brushSize,
            onBrushSizeChange = viewModel::setBrushSize
        )

        Divider()

        OverlayControls(
            overlayColor = state.scratchColor,
            hasCustomOverlay = state.customOverlayUri != null,
            onColorSelected = viewModel::setScratchColor,
            onSelectOverlay = onSelectOverlay,
            onClearOverlay = { viewModel.selectOverlay(null) }
        )

        Divider()

        if (state.hasScratched) {
            Button(
                onClick = viewModel::resetOverlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Overlay")
            }
        }

        TextButton(
            onClick = onSelectImages,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add More Images")
        }
    }
}

@Composable
private fun ScratchViewerCard(
    state: GalleryState,
    viewModel: GalleryViewModel
) {
    val context = LocalContext.current
    val currentImage = state.currentImage

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    var imageAspectRatio by remember { mutableStateOf(4f / 3f) }

    val basePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(currentImage?.uri)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader
    )

    LaunchedEffect(basePainter.state) {
        val painterState = basePainter.state
        if (painterState is AsyncImagePainter.State.Success) {
            val drawable = painterState.result.drawable
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
            val ratio = width.toFloat() / height.toFloat()
            imageAspectRatio = ratio.coerceIn(9f / 21f, 21f / 9f)
        }
    }

    val overlayPainter: Painter = state.customOverlayUri?.let { uri ->
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .size(Size.ORIGINAL)
                .build(),
            imageLoader = imageLoader
        )
    } ?: remember(state.scratchColor) {
        ColorPainter(state.scratchColor)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((300.dp * (1f / imageAspectRatio)).coerceIn(200.dp, 500.dp))
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black)
    ) {
        Image(
            painter = basePainter,
            contentDescription = "Current image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        ScratchCanvas(
            painter = overlayPainter,
            brushRadiusPx = state.brushSize,
            scratchSegments = state.scratchSegments,
            onScratch = { start, end, radius ->
                viewModel.addScratchSegment(start, end, radius)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!state.hasScratched) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Scratch to reveal!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ScratchCanvas(
    painter: Painter,
    brushRadiusPx: Float,
    scratchSegments: List<ScratchSegment>,
    onScratch: (start: Offset, end: Offset?, radius: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier.pointerInput(brushRadiusPx) {
            detectDragGestures(
                onDragStart = { offset ->
                    onScratch(offset, null, brushRadiusPx)
                    lastPosition = offset
                },
                onDrag = { change, _ ->
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

@Composable
private fun NavigationControls(
    state: GalleryState,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPrevious,
            enabled = state.canGoPrevious,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Previous")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onNext,
            enabled = state.canGoNext,
            modifier = Modifier.weight(1f)
        ) {
            Text("Next")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun BrushControls(
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Brush Size",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${brushSize.roundToInt()} px",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = brushSize,
            onValueChange = onBrushSizeChange,
            valueRange = 10f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        BrushPreview(brushSize = brushSize)
    }
}

@Composable
private fun BrushPreview(
    brushSize: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            val normalized = (brushSize / 100f).coerceIn(0f, 1f)
            val radius = normalized * (size.minDimension / 2f)
            drawCircle(
                color = primaryColor,
                radius = max(radius, 8f)
            )
            drawCircle(
                color = borderColor,
                radius = size.minDimension / 2f,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun OverlayControls(
    overlayColor: Color,
    hasCustomOverlay: Boolean,
    onColorSelected: (Color) -> Unit,
    onSelectOverlay: () -> Unit,
    onClearOverlay: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Overlay",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    selected = !hasCustomOverlay && overlayColor == color,
                    onClick = { onColorSelected(color) }
                )
            }
        }

        Button(
            onClick = onSelectOverlay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Custom Overlay Image")
        }

        if (hasCustomOverlay) {
            TextButton(
                onClick = onClearOverlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Custom Overlay")
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
    Box(
        modifier = Modifier
            .size(if (selected) 48.dp else 44.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun FullscreenViewer(
    state: GalleryState,
    viewModel: GalleryViewModel
) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val basePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(state.currentImage?.uri)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader
    )

    val overlayPainter: Painter = state.customOverlayUri?.let { uri ->
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .size(Size.ORIGINAL)
                .build(),
            imageLoader = imageLoader
        )
    } ?: remember(state.scratchColor) {
        ColorPainter(state.scratchColor)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = basePainter,
            contentDescription = "Current image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        ScratchCanvas(
            painter = overlayPainter,
            brushRadiusPx = state.brushSize,
            scratchSegments = state.scratchSegments,
            onScratch = { start, end, radius ->
                viewModel.addScratchSegment(start, end, radius)
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { viewModel.toggleFullscreen() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = "Exit fullscreen",
                tint = Color.White
            )
        }
    }
}
