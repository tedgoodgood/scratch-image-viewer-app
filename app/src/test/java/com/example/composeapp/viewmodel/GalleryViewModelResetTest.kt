package com.example.composeapp.viewmodel

import android.app.Application
import android.graphics.Color
import android.graphics.PointF
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryViewModelResetTest {

    private class TestApplication : Application()

    @Test
    fun `resetOverlay clears only scratch data`() {
        val viewModel = GalleryViewModel(TestApplication(), SavedStateHandle())

        viewModel.setOverlayOpacity(40)
        viewModel.setOverlayColor(Color.BLUE)
        viewModel.setBrushSize(24f)
        viewModel.addScratchSegment(PointF(0f, 0f), PointF(10f, 10f), 12f)

        val stateBeforeReset = viewModel.state.value
        val expectedOpacity = stateBeforeReset.overlayOpacity
        val expectedColor = stateBeforeReset.scratchColor
        val expectedBrushSize = stateBeforeReset.brushSize

        assertTrue(stateBeforeReset.hasScratched)
        assertFalse(stateBeforeReset.scratchSegments.isEmpty())

        viewModel.resetOverlay()

        val stateAfterReset = viewModel.state.value
        assertEquals(expectedOpacity, stateAfterReset.overlayOpacity)
        assertEquals(expectedColor, stateAfterReset.scratchColor)
        assertEquals(expectedBrushSize, stateAfterReset.brushSize, 0.0f)
        assertTrue(stateAfterReset.scratchSegments.isEmpty())
        assertFalse(stateAfterReset.hasScratched)
    }
}
