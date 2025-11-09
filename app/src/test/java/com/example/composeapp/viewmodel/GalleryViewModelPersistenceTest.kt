package com.example.composeapp.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ImageItem
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryViewModelPersistenceTest {

    @Test
    fun `persistGalleryState stores content uris and current index`() {
        val savedStateHandle = SavedStateHandle()
        val defaultImage = ImageItem(
            uri = Uri.parse("https://example.com/default"),
            displayName = "Default",
            mimeType = "image/jpeg"
        )
        val firstContent = ImageItem(
            uri = Uri.parse("content://images/1"),
            displayName = "First",
            mimeType = "image/jpeg"
        )
        val secondContent = ImageItem(
            uri = Uri.parse("content://images/2"),
            displayName = "Second",
            mimeType = "image/jpeg"
        )

        val state = GalleryState(
            images = listOf(defaultImage, firstContent, secondContent),
            currentIndex = 2
        )

        GalleryViewModel.persistGalleryState(state, savedStateHandle)

        val storedUris = savedStateHandle.get<List<String>>("gallery:persisted_uris")
        val storedIndex = savedStateHandle.get<Int>("gallery:current_index")

        assertEquals(listOf(firstContent.uri.toString(), secondContent.uri.toString()), storedUris)
        assertEquals(1, storedIndex)
    }

    @Test
    fun `persistGalleryState clears persisted state when no content uris`() {
        val savedStateHandle = SavedStateHandle()
        val defaultImage = ImageItem(
            uri = Uri.parse("https://example.com/default"),
            displayName = "Default",
            mimeType = "image/jpeg"
        )
        val secondaryImage = ImageItem(
            uri = Uri.parse("https://example.com/second"),
            displayName = "Second",
            mimeType = "image/jpeg"
        )

        val state = GalleryState(
            images = listOf(defaultImage, secondaryImage),
            currentIndex = 1
        )

        GalleryViewModel.persistGalleryState(state, savedStateHandle)

        val storedUris = savedStateHandle.get<List<String>>("gallery:persisted_uris")
        val storedIndex = savedStateHandle.get<Int>("gallery:current_index")

        assertEquals(emptyList<String>(), storedUris)
        assertEquals(-1, storedIndex)
    }
}
