package com.example.composeapp.viewer

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

@Immutable
data class ViewerImage(
    val id: String,
    val title: String,
    val model: Any
)

@Immutable
data class ViewerStroke(
    val points: List<Offset>,
    val brushSizeDp: Float
)

data class ViewerUiState(
    val images: List<ViewerImage> = defaultImages(),
    val currentIndex: Int = 0,
    val brushSizeDp: Float = 36f,
    val overlayColor: Color = Color(0xFF1B1B1B),
    val overlayAlpha: Float = 0.85f,
    val strokes: List<ViewerStroke> = emptyList(),
    val activeStroke: ViewerStroke? = null,
    val overlayBitmap: ImageBitmap? = null,
    val overlayBitmapUri: Uri? = null,
    val isFullscreen: Boolean = false
) {
    val currentImage: ViewerImage? get() = images.getOrNull(currentIndex)
    val canGoPrevious: Boolean get() = currentIndex > 0
    val canGoNext: Boolean get() = currentIndex < images.lastIndex
}

class ImageViewerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun selectImage(index: Int) {
        _uiState.update { state ->
            if (index in state.images.indices) state.copy(currentIndex = index) else state
        }
    }

    fun showPrevious() {
        _uiState.update { state ->
            if (state.canGoPrevious) state.copy(currentIndex = state.currentIndex - 1) else state
        }
    }

    fun showNext() {
        _uiState.update { state ->
            if (state.canGoNext) state.copy(currentIndex = state.currentIndex + 1) else state
        }
    }

    fun setBrushSize(sizeDp: Float) {
        _uiState.update { it.copy(brushSizeDp = sizeDp.coerceIn(4f, 120f)) }
    }

    fun setOverlayColor(color: Color) {
        _uiState.update { it.copy(overlayColor = color) }
    }

    fun resetOverlay() {
        _uiState.update { it.copy(strokes = emptyList(), activeStroke = null) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun beginStroke(offset: Offset) {
        _uiState.update { state ->
            state.copy(activeStroke = ViewerStroke(points = listOf(offset), brushSizeDp = state.brushSizeDp))
        }
    }

    fun appendStroke(offset: Offset) {
        _uiState.update { state ->
            val active = state.activeStroke ?: return@update state
            state.copy(activeStroke = active.copy(points = active.points + offset))
        }
    }

    fun completeStroke(commit: Boolean) {
        _uiState.update { state ->
            val active = state.activeStroke ?: return@update state.copy(activeStroke = null)
            if (!commit || active.points.size < 2) {
                state.copy(activeStroke = null)
            } else {
                state.copy(
                    strokes = state.strokes + active,
                    activeStroke = null
                )
            }
        }
    }

    fun setOverlayBitmap(bitmap: ImageBitmap?, uri: Uri?) {
        _uiState.update { it.copy(overlayBitmap = bitmap, overlayBitmapUri = uri) }
    }

    fun clearOverlayBitmap() {
        _uiState.update { it.copy(overlayBitmap = null, overlayBitmapUri = null) }
    }

    fun addImportedImage(uri: Uri, label: String) {
        _uiState.update { state ->
            val newImage = ViewerImage(
                id = UUID.randomUUID().toString(),
                title = label,
                model = uri
            )
            val newImages = state.images + newImage
            state.copy(images = newImages, currentIndex = newImages.lastIndex)
        }
    }

    private fun defaultImages(): List<ViewerImage> = listOf(
        ViewerImage(
            id = "fui-01",
            title = "云隐山谷",
            model = "https://images.unsplash.com/photo-1500530855697-5f2c4c79ad1c?auto=format&fit=crop&w=1600&q=80"
        ),
        ViewerImage(
            id = "fui-02",
            title = "苍穹晨光",
            model = "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1600&q=80"
        ),
        ViewerImage(
            id = "fui-03",
            title = "林间细语",
            model = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1600&q=80"
        )
    )
}
