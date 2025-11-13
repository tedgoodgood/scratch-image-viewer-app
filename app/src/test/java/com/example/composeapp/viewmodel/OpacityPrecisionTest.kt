package com.example.composeapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.when

/**
 * Test to verify that opacity values don't decrease each time settings are opened.
 * This test ensures the fix for the bug where opacity would decrease by 1 each time
 * the settings dialog was opened.
 */
class OpacityPrecisionTest {

    @Test
    fun `setOverlayOpacity saves percentage without precision loss`() {
        // Create mock application and shared preferences
        val mockApplication = mock(Application::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        when(mockApplication.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        when(mockSharedPreferences.edit()).thenReturn(mockEditor)
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor)

        // Create ViewModel
        val viewModel = GalleryViewModel(mockApplication, SavedStateHandle())

        // Set opacity to 99%
        viewModel.setOverlayOpacity(99)

        // Verify state was updated correctly
        val state = viewModel.state.value
        assertEquals("overlayOpacityPercent should be 99", 99, state.overlayOpacityPercent)
        // 99 * 255 / 100 = 252.45 which truncates to 252
        assertEquals("overlayOpacity should be 252 (99 * 255 / 100)", 252, state.overlayOpacity)
    }

    @Test
    fun `setOverlayOpacity preserves percentage across multiple changes`() {
        val mockApplication = mock(Application::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        when(mockApplication.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        when(mockSharedPreferences.edit()).thenReturn(mockEditor)
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor)

        val viewModel = GalleryViewModel(mockApplication, SavedStateHandle())

        // Test multiple opacity values
        val testValues = listOf(99, 98, 95, 50, 75, 100, 1)

        for (value in testValues) {
            viewModel.setOverlayOpacity(value)
            val state = viewModel.state.value
            assertEquals("overlayOpacityPercent should be $value after setOverlayOpacity($value)",
                value, state.overlayOpacityPercent)
        }
    }

    @Test
    fun `overlayOpacityPercent matches the input percentage exactly`() {
        val mockApplication = mock(Application::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        when(mockApplication.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        when(mockSharedPreferences.edit()).thenReturn(mockEditor)
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor)

        val viewModel = GalleryViewModel(mockApplication, SavedStateHandle())

        // Set to 99 and verify it stays 99 (not decreasing to 98)
        viewModel.setOverlayOpacity(99)
        assertEquals("After first setOverlayOpacity(99)", 99, viewModel.state.value.overlayOpacityPercent)

        // Set to 99 again and verify it stays 99 (should not be 98)
        viewModel.setOverlayOpacity(99)
        assertEquals("After second setOverlayOpacity(99)", 99, viewModel.state.value.overlayOpacityPercent)

        // Set to 98 and verify it's 98 (not decreasing further)
        viewModel.setOverlayOpacity(98)
        assertEquals("After setOverlayOpacity(98)", 98, viewModel.state.value.overlayOpacityPercent)

        // Set to 98 again and verify it stays 98 (not becoming 97)
        viewModel.setOverlayOpacity(98)
        assertEquals("After second setOverlayOpacity(98)", 98, viewModel.state.value.overlayOpacityPercent)
    }
}
