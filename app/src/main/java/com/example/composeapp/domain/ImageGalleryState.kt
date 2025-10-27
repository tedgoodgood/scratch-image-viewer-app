package com.example.composeapp.domain

import com.example.composeapp.domain.ImageGalleryState.Companion.EMPTY_INDEX

data class ImageGalleryState(
    val images: List<ImageMetadata> = emptyList(),
    val currentIndex: Int = EMPTY_INDEX,
    val isLoading: Boolean = false,
    val error: String? = null,
    val warnings: List<String> = emptyList()
) {
    val totalCount: Int = images.size

    val currentImage: ImageMetadata? = images.getOrNull(currentIndex)

    val hasImages: Boolean = images.isNotEmpty()

    val canGoPrevious: Boolean = currentIndex > 0

    val canGoNext: Boolean = currentIndex in 0 until images.lastIndex

    val positionLabel: String? = currentImage?.let { "${currentIndex + 1} / $totalCount" }

    val currentFilename: String? = currentImage?.displayName

    val mimeType: String? = currentImage?.mimeType?.takeIf { it.isNullOrBlank().not() }

    companion object {
        const val EMPTY_INDEX = -1
    }
}
