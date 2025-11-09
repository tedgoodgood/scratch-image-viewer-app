package com.example.composeapp.domain

import android.graphics.PointF
import android.net.Uri
import androidx.annotation.ColorInt

data class GalleryState(
    val images: List<ImageItem> = emptyList(),
    val currentIndex: Int = -1,
    val brushSize: Float = 40f,
    @ColorInt val scratchColor: Int = DEFAULT_SCRATCH_COLOR,
    val overlayOpacity: Int = 250, // 0-255, default 98%
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
    val start: PointF,
    val end: PointF?,
    val radiusPx: Float
)

@ColorInt
val DEFAULT_SCRATCH_COLOR: Int = 0xFAD4AF37.toInt() // Semi-transparent gold (98% opacity)
