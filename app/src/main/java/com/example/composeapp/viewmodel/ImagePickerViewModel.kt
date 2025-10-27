package com.example.composeapp.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.composeapp.domain.ImageGalleryState
import com.example.composeapp.domain.ImageMetadata
import com.example.composeapp.util.NaturalSortComparator
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.Locale

class ImagePickerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver = application.contentResolver
    private val naturalSortComparator = NaturalSortComparator()

    private val _state = MutableStateFlow(ImageGalleryState())
    val state: StateFlow<ImageGalleryState> = _state.asStateFlow()

    init {
        restorePersistedUris()
    }

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            updateState(persist = false) { state ->
                if (state.hasImages) state else state.copy(error = "No images were selected.")
            }
            return
        }

        viewModelScope.launch {
            updateState(persist = false) {
                it.copy(isLoading = true, error = null, warnings = emptyList())
            }

            val currentStateSnapshot = _state.value
            val outcome = withContext(Dispatchers.IO) {
                processSelection(uris, currentStateSnapshot)
            }

            if (outcome.newImages.isNotEmpty()) {
                prefetchImages(outcome.newImages)
            }

            updateState {
                it.copy(
                    images = outcome.mergedImages,
                    currentIndex = outcome.targetIndex,
                    isLoading = false,
                    error = outcome.error,
                    warnings = outcome.warnings
                )
            }
        }
    }

    fun goToNext() {
        updateState { state ->
            if (state.canGoNext) state.copy(currentIndex = state.currentIndex + 1) else state
        }
    }

    fun goToPrevious() {
        updateState { state ->
            if (state.canGoPrevious) state.copy(currentIndex = state.currentIndex - 1) else state
        }
    }

    fun clearError() {
        updateState(persist = false) { it.copy(error = null) }
    }

    fun clearWarnings() {
        updateState(persist = false) { it.copy(warnings = emptyList()) }
    }

    private suspend fun processSelection(
        uris: List<Uri>,
        currentState: ImageGalleryState
    ): SelectionOutcome {
        return withContext(Dispatchers.IO) {
            val warnings = mutableListOf<String>()
            val validImages = mutableListOf<ImageMetadata>()
            for (uri in uris) {
                try {
                    takePersistablePermission(uri)
                    val metadata = extractImageMetadata(uri)
                    if (metadata == null) {
                        warnings.add("Unable to load: ${uri.lastPathSegment ?: uri}")
                        continue
                    }
                    if (isSupportedImage(metadata.mimeType)) {
                        validImages.add(metadata)
                    } else {
                        warnings.add("Unsupported format: ${metadata.displayName}")
                    }
                } catch (securityException: SecurityException) {
                    warnings.add("Permission denied: ${uri.lastPathSegment ?: uri}")
                } catch (error: Exception) {
                    warnings.add("Failed to process: ${uri.lastPathSegment ?: uri}")
                }
            }

            val mergedMap = LinkedHashMap<Uri, ImageMetadata>()
            currentState.images.forEach { mergedMap[it.uri] = it }
            validImages.forEach { mergedMap[it.uri] = it }

            val mergedList = mergedMap.values.sortedWith(compareBy(naturalSortComparator) { it.displayName })

            val firstNewIndex = validImages.firstNotNullOfOrNull { newImage ->
                mergedList.indexOfFirst { it.uri == newImage.uri }.takeIf { it >= 0 }
            }

            val targetIndex = when {
                mergedList.isEmpty() -> ImageGalleryState.EMPTY_INDEX
                firstNewIndex != null -> firstNewIndex
                currentState.currentIndex in mergedList.indices -> currentState.currentIndex
                else -> 0
            }

            val errorMessage = when {
                mergedList.isEmpty() -> "No images available."
                validImages.isEmpty() -> "No supported image formats were selected."
                else -> null
            }

            SelectionOutcome(
                mergedImages = mergedList,
                newImages = validImages,
                targetIndex = targetIndex,
                warnings = warnings,
                error = errorMessage
            )
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers (e.g. Photo Picker) do not support persistable permissions.
        }
    }

    private fun extractImageMetadata(uri: Uri): ImageMetadata? {
        return try {
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME)
                        ?: uri.lastPathSegment
                        ?: uri.toString()
                    val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        ?.takeIf { it.isNotBlank() }
                        ?: contentResolver.getType(uri)
                        ?: inferMimeType(displayName)
                    return ImageMetadata(uri, displayName, mimeType)
                }
                null
            } ?: run {
                val fallbackName = uri.lastPathSegment ?: uri.toString()
                val fallbackMime = contentResolver.getType(uri) ?: inferMimeType(fallbackName)
                ImageMetadata(uri, fallbackName, fallbackMime)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun isSupportedImage(mimeType: String?): Boolean {
        return mimeType
            ?.lowercase(Locale.getDefault())
            ?.startsWith("image/")
            ?: false
    }

    private fun inferMimeType(displayName: String?): String? {
        if (displayName.isNullOrBlank()) return null
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.getDefault())
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun prefetchImages(images: List<ImageMetadata>) {
        if (images.isEmpty()) return
        val appContext = getApplication<Application>()
        val loader = Coil.imageLoader(appContext)
        images.forEach { metadata ->
            val request = ImageRequest.Builder(appContext)
                .data(metadata.uri)
                .memoryCacheKey(metadata.uri.toString())
                .diskCacheKey(metadata.uri.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            loader.enqueue(request)
        }
    }

    private fun restorePersistedUris() {
        val storedUris = savedStateHandle.get<List<String>>(KEY_PERSISTED_URIS).orEmpty()
        val storedIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: ImageGalleryState.EMPTY_INDEX
        if (storedUris.isEmpty()) return

        viewModelScope.launch {
            updateState(persist = false) { it.copy(isLoading = true) }

            val restoration = withContext(Dispatchers.IO) {
                val uris = storedUris.mapNotNull { parsePersistedUri(it) }
                val metadata = uris.mapNotNull { extractImageMetadata(it) }
                metadata.sortedWith(compareBy(naturalSortComparator) { it.displayName }) to metadata
            }

            val sorted = restoration.first
            if (sorted.isNotEmpty()) {
                prefetchImages(sorted)
            }

            val normalizedIndex = when {
                sorted.isEmpty() -> ImageGalleryState.EMPTY_INDEX
                storedIndex in sorted.indices -> storedIndex
                else -> 0
            }

            updateState {
                it.copy(
                    images = sorted,
                    currentIndex = normalizedIndex,
                    isLoading = false,
                    error = null,
                    warnings = emptyList()
                )
            }
        }
    }

    private fun parsePersistedUri(value: String): Uri? = runCatching { Uri.parse(value) }.getOrNull()

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun updateState(
        persist: Boolean = true,
        transform: (ImageGalleryState) -> ImageGalleryState
    ) {
        val updated = transform(_state.value)
        _state.value = updated
        if (persist) {
            savedStateHandle[KEY_PERSISTED_URIS] = updated.images.map { it.uri.toString() }
            savedStateHandle[KEY_CURRENT_INDEX] = updated.currentIndex
        }
    }

    private data class SelectionOutcome(
        val mergedImages: List<ImageMetadata>,
        val newImages: List<ImageMetadata>,
        val targetIndex: Int,
        val warnings: List<String>,
        val error: String?
    )

    companion object {
        private const val KEY_PERSISTED_URIS = "image_gallery:persisted_uris"
        private const val KEY_CURRENT_INDEX = "image_gallery:current_index"

        val Factory = viewModelFactory {
            initializer {
                val application = this[AndroidViewModel.APPLICATION_KEY] as Application
                val savedStateHandle = createSavedStateHandle()
                ImagePickerViewModel(application, savedStateHandle)
            }
        }
    }
}
