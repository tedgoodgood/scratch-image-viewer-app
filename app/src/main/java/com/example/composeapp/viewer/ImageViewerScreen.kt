package com.example.composeapp.viewer

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.composeapp.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ImageViewerScreen(
    viewModel: ImageViewerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemUiController = rememberSystemUiController()
    val focusRequester = remember { FocusRequester() }
    val backgroundColor = MaterialTheme.colorScheme.background
    val useDarkIcons = backgroundColor.luminance() > 0.5f

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(uiState.isFullscreen, backgroundColor, useDarkIcons) {
        if (uiState.isFullscreen) {
            systemUiController.isSystemBarsVisible = false
            systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
        } else {
            systemUiController.isSystemBarsVisible = true
            systemUiController.setSystemBarsColor(backgroundColor, darkIcons = useDarkIcons)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            viewModel.showPrevious()
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.showNext()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        var showSettings by rememberSaveable { mutableStateOf(true) }
        val density = LocalDensity.current
        val swipeThresholdPx = remember(density) { with(density) { 180.dp.toPx() } }
        val verticalTolerancePx = remember(density) { with(density) { 120.dp.toPx() } }

        val context = LocalContext.current
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEachIndexed { index, uri ->
                    val name = uri.displayName(context).ifBlank { context.getString(R.string.image_viewer) }
                    viewModel.addImportedImage(uri, name)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(visible = !uiState.isFullscreen) {
                ViewerTopBar(
                    uiState = uiState,
                    onImportImages = { importLauncher.launch("image/*") },
                    onSelectImage = { index -> viewModel.selectImage(index) },
                    onPrevious = viewModel::showPrevious,
                    onNext = viewModel::showNext,
                    onResetOverlay = viewModel::resetOverlay,
                    onToggleFullscreen = viewModel::toggleFullscreen,
                    onToggleSettings = { showSettings = !showSettings },
                    settingsVisible = showSettings
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = if (uiState.isFullscreen) 0.dp else 16.dp)
                ) {
                    ViewerSurface(
                        uiState = uiState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = if (uiState.isFullscreen) 0.dp else 16.dp, bottom = 16.dp)
                            .fillMaxHeight(),
                        swipeThresholdPx = swipeThresholdPx,
                        verticalTolerancePx = verticalTolerancePx,
                        onBeginStroke = viewModel::beginStroke,
                        onAppendStroke = viewModel::appendStroke,
                        onCompleteStroke = viewModel::completeStroke,
                        onSwipeLeft = viewModel::showNext,
                        onSwipeRight = viewModel::showPrevious,
                        onExitFullscreen = viewModel::toggleFullscreen
                    )

                    AnimatedVisibility(visible = !uiState.isFullscreen && showSettings) {
                        SettingsPanel(
                            uiState = uiState,
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 16.dp, start = 16.dp)
                                .width(320.dp)
                                .fillMaxHeight(),
                            onBrushSizeChange = viewModel::setBrushSize,
                            onColorSelected = viewModel::setOverlayColor,
                            onOverlaySelected = { bitmap, uri -> viewModel.setOverlayBitmap(bitmap, uri) },
                            onOverlayCleared = viewModel::clearOverlayBitmap
                        )
                    }
                }
            } else {
                ViewerSurface(
                    uiState = uiState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = if (uiState.isFullscreen) 0.dp else 16.dp,
                            vertical = if (uiState.isFullscreen) 0.dp else 16.dp),
                    swipeThresholdPx = swipeThresholdPx,
                    verticalTolerancePx = verticalTolerancePx,
                    onBeginStroke = viewModel::beginStroke,
                    onAppendStroke = viewModel::appendStroke,
                    onCompleteStroke = viewModel::completeStroke,
                    onSwipeLeft = viewModel::showNext,
                    onSwipeRight = viewModel::showPrevious,
                    onExitFullscreen = viewModel::toggleFullscreen
                )

                AnimatedVisibility(visible = !uiState.isFullscreen && showSettings) {
                    SettingsPanel(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        onBrushSizeChange = viewModel::setBrushSize,
                        onColorSelected = viewModel::setOverlayColor,
                        onOverlaySelected = { bitmap, uri -> viewModel.setOverlayBitmap(bitmap, uri) },
                        onOverlayCleared = viewModel::clearOverlayBitmap
                    )
                }
            }

            AnimatedVisibility(visible = !uiState.isFullscreen) {
                Footer()
            }
        }

        if (uiState.isFullscreen) {
            IconButton(
                onClick = viewModel::toggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Filled.FullscreenExit,
                    contentDescription = stringResource(R.string.exit_fullscreen)
                )
            }
        } else {
            FilledIconButton(
                onClick = { showSettings = !showSettings },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp, bottom = 32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    uiState: ViewerUiState,
    onImportImages: () -> Unit,
    onSelectImage: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetOverlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleSettings: () -> Unit,
    settingsVisible: Boolean
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    TopAppBar(
        title = {
            Column {
                Text(stringResource(R.string.viewer_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = uiState.currentImage?.title ?: stringResource(R.string.no_images_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            val currentIndex = uiState.currentIndex

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = uiState.currentImage?.title ?: stringResource(R.string.no_images_selected),
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .width(220.dp),
                    readOnly = true,
                    label = { Text(stringResource(R.string.viewer_select_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    singleLine = true
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    uiState.images.forEachIndexed { index, image ->
                        DropdownMenuItem(
                            text = { Text(image.title) },
                            onClick = {
                                onSelectImage(index)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onImportImages) {
                Icon(imageVector = Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.select_images))
            }

            IconButton(onClick = onPrevious, enabled = uiState.canGoPrevious) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.previous))
            }
            IconButton(onClick = onNext, enabled = uiState.canGoNext) {
                Icon(Icons.Filled.ArrowForward, contentDescription = stringResource(R.string.next))
            }

            IconButton(onClick = onToggleSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = if (settingsVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedButton(onClick = onResetOverlay) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.reset_overlay))
            }

            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (uiState.isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (uiState.isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.fullscreen)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ViewerSurface(
    uiState: ViewerUiState,
    modifier: Modifier = Modifier,
    swipeThresholdPx: Float,
    verticalTolerancePx: Float,
    onBeginStroke: (Offset) -> Unit,
    onAppendStroke: (Offset) -> Unit,
    onCompleteStroke: (Boolean) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onExitFullscreen: () -> Unit
) {
    val shape = if (uiState.isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = if (uiState.isFullscreen) 0.dp else 6.dp,
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val currentImage = uiState.currentImage
            if (currentImage != null) {
                AsyncImage(
                    model = currentImage.model,
                    contentDescription = stringResource(R.string.image_viewer),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = stringResource(R.string.no_images_selected),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }

            ScratchOverlayCanvas(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
                swipeThresholdPx = swipeThresholdPx,
                verticalTolerancePx = verticalTolerancePx,
                onBeginStroke = onBeginStroke,
                onAppendStroke = onAppendStroke,
                onCompleteStroke = onCompleteStroke,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            )

            if (uiState.overlayBitmapUri != null && !uiState.isFullscreen) {
                Text(
                    text = stringResource(R.string.overlay_applied),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (uiState.isFullscreen) {
                IconButton(
                    onClick = onExitFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.exit_fullscreen))
                }
            }
        }
    }
}

@Composable
private fun ScratchOverlayCanvas(
    uiState: ViewerUiState,
    modifier: Modifier = Modifier,
    swipeThresholdPx: Float,
    verticalTolerancePx: Float,
    onBeginStroke: (Offset) -> Unit,
    onAppendStroke: (Offset) -> Unit,
    onCompleteStroke: (Boolean) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(uiState.brushSizeDp, swipeThresholdPx, verticalTolerancePx) {
                var dragAccumulation = Offset.Zero
                detectDragGestures(
                    onDragStart = { offset ->
                        dragAccumulation = Offset.Zero
                        onBeginStroke(offset)
                    },
                    onDrag = { change, dragAmount ->
                        dragAccumulation += dragAmount
                        onAppendStroke(change.position)
                        change.consume()
                    },
                    onDragEnd = {
                        val isSwipe = abs(dragAccumulation.x) > swipeThresholdPx && abs(dragAccumulation.y) < verticalTolerancePx
                        if (isSwipe) {
                            onCompleteStroke(false)
                            if (dragAccumulation.x < 0f) onSwipeLeft() else onSwipeRight()
                        } else {
                            onCompleteStroke(true)
                        }
                    },
                    onDragCancel = {
                        onCompleteStroke(false)
                    }
                )
            }
    ) {
        uiState.overlayBitmap?.let { drawOverlayBitmap(it, uiState.overlayAlpha) }
        drawRect(color = uiState.overlayColor.copy(alpha = uiState.overlayAlpha))
        val strokes = uiState.strokes + listOfNotNull(uiState.activeStroke)
        strokes.forEach { stroke ->
            if (stroke.points.size >= 2) {
                val path = Path()
                stroke.points.forEachIndexed { index, offset ->
                    if (index == 0) {
                        path.moveTo(offset.x, offset.y)
                    } else {
                        path.lineTo(offset.x, offset.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color.Transparent,
                    style = Stroke(
                        width = stroke.brushSizeDp.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    uiState: ViewerUiState,
    modifier: Modifier = Modifier,
    onBrushSizeChange: (Float) -> Unit,
    onColorSelected: (Color) -> Unit,
    onOverlaySelected: (ImageBitmap?, Uri?) -> Unit,
    onOverlayCleared: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = loadOverlayBitmap(context, uri)
                onOverlaySelected(bitmap, uri)
            }
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.brush_size), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = uiState.brushSizeDp,
                        onValueChange = onBrushSizeChange,
                        valueRange = 8f..160f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = uiState.brushSizeDp.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
                }
                BrushPreview(color = uiState.overlayColor, sizeDp = uiState.brushSizeDp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.brush_color), style = MaterialTheme.typography.labelLarge)
                ColorPicker(selected = uiState.overlayColor, onColorSelected = onColorSelected)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.overlay_tools_title), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { overlayLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.select_overlay))
                    }
                    if (uiState.overlayBitmap != null) {
                        OutlinedButton(onClick = onOverlayCleared) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.overlay_clear))
                        }
                    }
                }
                uiState.overlayBitmapUri?.let { uri ->
                    Text(
                        text = stringResource(R.string.overlay_selected, uri.displayName(context)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BrushPreview(color: Color, sizeDp: Float) {
    val clampedSize = sizeDp.coerceIn(8f, 160f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((clampedSize + 32f).dp)) {
            drawCircle(color = color, radius = clampedSize.dp.toPx() / 2f)
        }
    }
}

@Composable
private fun ColorPicker(selected: Color, onColorSelected: (Color) -> Unit) {
    val palette = listOf(
        Color.Black,
        Color(0xFF1B1B1B),
        Color(0xFF414141),
        Color(0xFF7F5539),
        Color(0xFF9C27B0),
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF9A825),
        Color(0xFFE53935),
        Color(0xFF00838F),
        Color(0xFFC0CA33),
        Color.White
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        palette.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) }
                    .semantics { },
                contentAlignment = Alignment.Center
            ) {
                if (selected == color) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.footer_text),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            textAlign = TextAlign.Center
        )
    }
}

private suspend fun loadOverlayBitmap(context: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }
    }.getOrNull()
}

private fun Uri.displayName(context: Context): String {
    return path?.substringAfterLast('/') ?: toString()
}

private fun DrawScope.drawOverlayBitmap(bitmap: ImageBitmap, alpha: Float) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()
    val scale = max(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val scaledWidth = imageWidth * scale
    val scaledHeight = imageHeight * scale
    val left = (canvasWidth - scaledWidth) / 2f
    val top = (canvasHeight - scaledHeight) / 2f
    drawImage(
        image = bitmap,
        srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, bitmap.height),
        dstSize = androidx.compose.ui.unit.IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt()),
        dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt()),
        alpha = alpha
    )
}
