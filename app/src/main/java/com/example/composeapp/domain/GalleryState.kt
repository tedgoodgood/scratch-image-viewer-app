package com.example.composeapp.domain

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class GalleryState(
    val images: List<ImageItem> = emptyList(),
    val currentIndex: Int = -1,
    val brushSize: Float = 40f,
    val scratchColor: Color = Color(0xFFD4AF37),
    val customOverlayUri: Uri? = null,
    val scratchSegments: List<ScratchSegment> = emptyList(),
    val hasScratched: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFullscreen: Boolean = false
) {
    val currentImage: ImageItem? = images.getOrNull(currentIndex)
    val hasImages: Boolean = images.isNotEmpty()
    val canGoPrevious: Boolean = currentIndex > 0
    val canGoNext: Boolean = currentIndex in 0 until images.lastIndex
}

data class ScratchSegment(
    val start: Offset,
    val end: Offset?,
    val radiusPx: Float
)
