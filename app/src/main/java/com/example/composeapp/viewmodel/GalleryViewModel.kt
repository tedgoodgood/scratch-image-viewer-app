package com.example.composeapp.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.ColorInt
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ImageItem
import com.example.composeapp.domain.OverlayType
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

    fun selectFolder(folderUri: Uri) {
        viewModelScope.launch {
            updateState(persist = false) { it.copy(isLoading = true, error = null) }

            val folderImages = withContext(Dispatchers.IO) {
                try {
                    takePersistablePermission(folderUri)
                    val images = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        enumerateImagesFromDocumentFolder(folderUri)
                    } else {
                        enumerateImagesFromLegacyFolder(folderUri)
                    }
                    
                    // Performance warning for large folders
                    if (images.size > 1000) {
                        updateState(persist = false) {
                            it.copy(
                                isLoading = false,
                                error = "Found ${images.size} images. Large folders may impact performance. Consider selecting a smaller folder."
                            )
                        }
                    }
                    
                    images
                } catch (e: Exception) {
                    updateState(persist = false) {
                        it.copy(
                            isLoading = false,
                            error = "Failed to access folder: ${e.message}"
                        )
                    }
                    emptyList()
                }
            }

            if (folderImages.isEmpty()) {
                updateState(persist = false) {
                    it.copy(
                        isLoading = false,
                        error = "No supported images found in the selected folder."
                    )
                }
                return@launch
            }

            // Merge with existing images using the same logic as selectImages
            val current = _state.value
            val persistedImages = (current.images + folderImages)
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
        updateState(persist = true) {
            it.copy(brushSize = size.coerceIn(MIN_BRUSH_RADIUS, MAX_BRUSH_RADIUS))
        }
    }

    fun setScratchColor(@ColorInt color: Int) {
        updateState(persist = false) {
            it.copy(
                scratchColor = color,
                scratchSegments = emptyList(),
                hasScratched = false
            )
        }
    }

    fun setOverlayOpacity(opacityPercent: Int) {
        // Convert 0-100% to 0-255 for Color.argb()
        val opacity255 = (opacityPercent * 255) / 100
        
        updateState(persist = true) {
            // Keep existing RGB values, just change opacity
            val currentColor = it.scratchColor
            val newColor = android.graphics.Color.argb(
                opacity255,
                android.graphics.Color.red(currentColor),
                android.graphics.Color.green(currentColor),
                android.graphics.Color.blue(currentColor)
            )
            it.copy(
                overlayOpacity = opacity255,
                scratchColor = newColor,
                scratchSegments = emptyList(),
                hasScratched = false
            )
        }
    }

    fun setOverlayColor(color: Int) {
        updateState(persist = true) {
            // Keep existing opacity, just change RGB values
            val currentOpacity = it.overlayOpacity
            val newColor = android.graphics.Color.argb(
                currentOpacity,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color)
            )
            it.copy(
                scratchColor = newColor,
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
        // No prefetching needed with Glide as it handles caching automatically
        // This method is kept for compatibility but doesn't need to do anything
    }

    private fun restorePersistedState() {
        val storedUris = savedStateHandle.get<List<String>>(KEY_PERSISTED_URIS).orEmpty()
        val storedIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: -1
        val storedScratchColor = savedStateHandle.get<Int>(KEY_SCRATCH_COLOR)
        val storedOverlayOpacity = savedStateHandle.get<Int>(KEY_OVERLAY_OPACITY) ?: DEFAULT_OPACITY
        val storedOverlayColor = savedStateHandle.get<Int>(KEY_OVERLAY_COLOR) ?: DEFAULT_COLOR
        val storedBrushSize = savedStateHandle.get<Float>(KEY_BRUSH_SIZE) ?: DEFAULT_BRUSH_SIZE

        if (storedUris.isEmpty()) {
            // Even with no stored images, load saved opacity and color settings
            updateState(persist = false) {
                it.copy(
                    overlayOpacity = storedOverlayOpacity,
                    scratchColor = android.graphics.Color.argb(
                        storedOverlayOpacity,
                        android.graphics.Color.red(storedOverlayColor),
                        android.graphics.Color.green(storedOverlayColor),
                        android.graphics.Color.blue(storedOverlayColor)
                    ),
                    brushSize = storedBrushSize
                )
            }
            return
        }

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
                    scratchColor = storedScratchColor ?: android.graphics.Color.argb(
                        storedOverlayOpacity,
                        android.graphics.Color.red(storedOverlayColor),
                        android.graphics.Color.green(storedOverlayColor),
                        android.graphics.Color.blue(storedOverlayColor)
                    ),
                    overlayOpacity = storedOverlayOpacity,
                    brushSize = storedBrushSize,
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

    private fun enumerateImagesFromDocumentFolder(folderUri: Uri): List<ImageItem> {
        val resolver = getApplication<Application>().contentResolver
        val documentFile = DocumentFile.fromTreeUri(getApplication(), folderUri)
            ?: return emptyList()

        val imageItems = mutableListOf<ImageItem>()
        val maxDepth = 10 // Prevent infinite recursion
        val visitedUris = mutableSetOf<String>()

        fun traverseFolder(documentFile: DocumentFile, depth: Int = 0) {
            if (depth > maxDepth) return
            if (documentFile.uri.toString() in visitedUris) return
            visitedUris.add(documentFile.uri.toString())

            documentFile.listFiles().forEach { file ->
                if (file.isDirectory) {
                    traverseFolder(file, depth + 1)
                } else if (file.isFile && isImageMimeType(file.type)) {
                    extractImageItem(file.uri)?.let { imageItems.add(it) }
                }
            }
        }

        traverseFolder(documentFile)
        return imageItems
    }

    private fun enumerateImagesFromLegacyFolder(folderUri: Uri): List<ImageItem> {
        val resolver = getApplication<Application>().contentResolver
        val imageItems = mutableListOf<ImageItem>()

        // For API < 21, folder selection is very limited
        // We'll try to extract path information and use MediaStore
        try {
            // Try to get the folder path from the URI
            val folderPath = folderUri.path ?: return emptyList()
            
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA
            )

            // Try to filter by folder path if possible
            val selectionClause: String
            val selectionArgs: Array<String>?
            
            if (folderPath.contains("/")) {
                selectionClause = "${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%' AND ${MediaStore.Images.Media.DATA} LIKE ?"
                selectionArgs = arrayOf("%$folderPath%")
            } else {
                selectionClause = "${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'"
                selectionArgs = null
            }

            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selectionClause,
                selectionArgs,
                MediaStore.Images.Media.DISPLAY_NAME
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dataColumn = c.getColumnIndex(MediaStore.Images.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn)
                    val mimeType = c.getString(mimeColumn)
                    val dataPath = if (dataColumn >= 0) c.getString(dataColumn) else null
                    
                    // Additional filtering to ensure we're getting images from the right folder
                    if (dataPath == null || folderPath.contains("/") || dataPath.contains(folderPath)) {
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        imageItems.add(ImageItem(uri, name, mimeType))
                    }
                }
            }
        } catch (e: Exception) {
            // If everything fails, return empty list
            // The error will be handled by the calling function
        }

        return imageItems
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
        private const val KEY_PERSISTED_FOLDERS = "gallery:persisted_folders"
        private const val KEY_OVERLAY_TYPE = "gallery:overlay_type"
        private const val KEY_SCRATCH_COLOR = "gallery:scratch_color"
        private const val KEY_UNDERLAY_IMAGE_URI = "gallery:underlay_image_uri"
        private const val KEY_OVERLAY_OPACITY = "gallery:overlay_opacity"
        private const val KEY_OVERLAY_COLOR = "gallery:overlay_color"
        private const val KEY_BRUSH_SIZE = "gallery:brush_size"
        private const val DEFAULT_IMAGE_URL = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80"
        private const val MIN_BRUSH_RADIUS = 10f
        private const val MAX_BRUSH_RADIUS = 100f
        private const val DEFAULT_BRUSH_SIZE = 40f
        private const val DEFAULT_OPACITY = 250  // 98%
        private const val DEFAULT_COLOR = android.graphics.Color.RED

        internal fun persistGalleryState(
            state: GalleryState,
            savedStateHandle: SavedStateHandle
        ) {
            val persistableImages = state.images.filter { it.uri.scheme == ContentResolver.SCHEME_CONTENT }
            savedStateHandle[KEY_PERSISTED_URIS] = persistableImages.map { it.uri.toString() }
            val currentUri = state.currentImage?.uri
            val persistedIndex = persistableImages.indexOfFirst { it.uri == currentUri }
            savedStateHandle[KEY_CURRENT_INDEX] = persistedIndex
            savedStateHandle[KEY_SCRATCH_COLOR] = state.scratchColor
            savedStateHandle[KEY_OVERLAY_OPACITY] = state.overlayOpacity
            savedStateHandle[KEY_BRUSH_SIZE] = state.brushSize
            
            // Save color without alpha for persistence
            val colorWithoutAlpha = state.scratchColor and 0x00FFFFFF
            savedStateHandle[KEY_OVERLAY_COLOR] = colorWithoutAlpha
            
            // Note: Folder URIs are not persisted here to avoid complexity
            // Users can re-select folders after app restart
            savedStateHandle[KEY_PERSISTED_FOLDERS] = emptyList<String>()
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
