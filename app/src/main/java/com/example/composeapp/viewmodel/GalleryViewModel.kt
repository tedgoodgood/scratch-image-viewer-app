package com.example.composeapp.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.graphics.PointF
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.ColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ImageItem
import com.example.composeapp.domain.ScratchSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class GalleryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val defaultImage = ImageItem(
        uri = Uri.parse(DEFAULT_IMAGE_URL),
        displayName = "Golden Fortune",
        mimeType = "image/jpeg"
    )

    private val _state = MutableStateFlow(
        GalleryState(
            images = listOf(defaultImage),
            currentIndex = 0
        )
    )
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    private val naturalSortComparator = Comparator<String> { a, b ->
        Collator.getInstance(Locale.getDefault()).compare(a, b)
    }

    init {
        restorePersistedState()
    }

    fun selectImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            updateState(persist = false) { it.copy(isLoading = true, error = null) }

            val validItems = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    takePersistablePermission(uri)
                    extractImageItem(uri)
                }
            }

            if (validItems.isEmpty()) {
                updateState(persist = false) {
                    it.copy(
                        isLoading = false,
                        error = "No supported images were selected."
                    )
                }
                return@launch
            }

            val current = _state.value
            val persistedImages = (current.images + validItems)
                .filterNot { it.uri == defaultImage.uri }
                .distinctBy { it.uri }
                .sortedWith(compareBy(naturalSortComparator) { it.displayName })

            val merged = listOf(defaultImage) + persistedImages
            val previousPersistedCount = current.images.count { it.uri != defaultImage.uri }
            val newIndex = when {
                previousPersistedCount == 0 -> 1
                current.currentIndex >= merged.size -> merged.lastIndex
                else -> current.currentIndex
            }

            prefetchImages(merged)

            updateState {
                it.copy(
                    images = merged,
                    currentIndex = newIndex.coerceIn(0, merged.lastIndex),
                    isLoading = false,
                    scratchSegments = emptyList(),
                    hasScratched = false,
                    error = null
                )
            }
        }
    }

    fun selectOverlay(uri: Uri?) {
        uri?.let { takePersistablePermission(it) }
        updateState(persist = false) {
            it.copy(
                customOverlayUri = uri,
                scratchSegments = emptyList(),
                hasScratched = false
            )
        }
    }

    fun goToPrevious() {
        updateState {
            if (it.canGoPrevious) {
                it.copy(
                    currentIndex = it.currentIndex - 1,
                    scratchSegments = emptyList(),
                    hasScratched = false
                )
            } else it
        }
    }

    fun goToNext() {
        updateState {
            if (it.canGoNext) {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    scratchSegments = emptyList(),
                    hasScratched = false
                )
            } else it
        }
    }

    fun resetOverlay() {
        updateState(persist = false) {
            it.copy(
                scratchSegments = emptyList(),
                hasScratched = false
            )
        }
    }

    fun setBrushSize(size: Float) {
        updateState(persist = false) {
            it.copy(brushSize = size.coerceIn(MIN_BRUSH_RADIUS, MAX_BRUSH_RADIUS))
        }
    }

    fun setScratchColor(@ColorInt color: Int) {
        updateState(persist = false) {
            it.copy(
                scratchColor = color,
                customOverlayUri = null,
                scratchSegments = emptyList(),
                hasScratched = false
            )
        }
    }

    fun addScratchSegment(start: PointF, end: PointF?, radiusPx: Float) {
        updateState(persist = false) {
            it.copy(
                scratchSegments = it.scratchSegments + ScratchSegment(start, end, radiusPx),
                hasScratched = true
            )
        }
    }

    fun toggleFullscreen() {
        updateState(persist = false) { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun clearError() {
        updateState(persist = false) { it.copy(error = null) }
    }

    private fun takePersistablePermission(uri: Uri) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun extractImageItem(uri: Uri): ImageItem? {
        val resolver = getApplication<Application>().contentResolver
        return runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME)
                        ?: uri.lastPathSegment
                        ?: "image"
                    val mimeType = resolver.getType(uri) ?: inferMimeType(displayName)

                    if (isImageMimeType(mimeType)) {
                        ImageItem(uri, displayName, mimeType)
                    } else null
                } else null
            }
        }.getOrNull()
    }

    private fun isImageMimeType(mimeType: String?): Boolean {
        return mimeType?.lowercase(Locale.getDefault())?.startsWith("image/") ?: false
    }

    private fun inferMimeType(displayName: String?): String? {
        if (displayName.isNullOrBlank()) return null
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.getDefault())
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun prefetchImages(images: List<ImageItem>) {
        if (images.isEmpty()) return
        val appContext = getApplication<Application>()
        val loader = Coil.imageLoader(appContext)
        images.forEach { item ->
            val request = ImageRequest.Builder(appContext)
                .data(item.uri)
                .memoryCacheKey(item.uri.toString())
                .diskCacheKey(item.uri.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            loader.enqueue(request)
        }
    }

    private fun restorePersistedState() {
        val storedUris = savedStateHandle.get<List<String>>(KEY_PERSISTED_URIS).orEmpty()
        val storedIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: -1

        if (storedUris.isEmpty()) return

        viewModelScope.launch {
            updateState(persist = false) { it.copy(isLoading = true) }

            val persisted = withContext(Dispatchers.IO) {
                val uris = storedUris.mapNotNull { parseUri(it) }
                uris.mapNotNull { extractImageItem(it) }
                    .sortedWith(compareBy(naturalSortComparator) { it.displayName })
            }

            val merged = listOf(defaultImage) + persisted
            prefetchImages(merged)

            val normalizedIndex = when {
                persisted.isEmpty() -> 0
                storedIndex in persisted.indices -> storedIndex + 1
                else -> 1
            }

            updateState(persist = false) {
                it.copy(
                    images = merged,
                    currentIndex = normalizedIndex.coerceIn(0, merged.lastIndex),
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    private fun parseUri(value: String): Uri? = runCatching { Uri.parse(value) }.getOrNull()

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun updateState(
        persist: Boolean = true,
        transform: (GalleryState) -> GalleryState
    ) {
        val updated = transform(_state.value)
        _state.value = updated
        if (persist) {
            persistGalleryState(updated, savedStateHandle)
        }
    }

    companion object {
        private const val KEY_PERSISTED_URIS = "gallery:persisted_uris"
        private const val KEY_CURRENT_INDEX = "gallery:current_index"
        private const val DEFAULT_IMAGE_URL = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80"
        private const val MIN_BRUSH_RADIUS = 10f
        private const val MAX_BRUSH_RADIUS = 100f

        internal fun persistGalleryState(
            state: GalleryState,
            savedStateHandle: SavedStateHandle
        ) {
            val persistableImages = state.images.filter { it.uri.scheme == ContentResolver.SCHEME_CONTENT }
            savedStateHandle[KEY_PERSISTED_URIS] = persistableImages.map { it.uri.toString() }
            val currentUri = state.currentImage?.uri
            val persistedIndex = persistableImages.indexOfFirst { it.uri == currentUri }
            savedStateHandle[KEY_CURRENT_INDEX] = persistedIndex
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val savedStateHandle = createSavedStateHandle()
                GalleryViewModel(application, savedStateHandle)
            }
        }
    }
}
