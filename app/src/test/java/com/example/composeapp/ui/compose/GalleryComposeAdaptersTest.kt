package com.example.composeapp.ui.compose

import android.graphics.PointF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ImageItem
import com.example.composeapp.domain.ScratchSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryComposeAdaptersTest {

    @Test
    fun `scratch segment conversion preserves coordinates and radius`() {
        val segment = ScratchSegment(
            start = PointF(12.5f, 48.5f),
            end = PointF(30.5f, 64.25f),
            radiusPx = 24f
        )

        val result = segment.toCompose()

        assertEquals(Offset(12.5f, 48.5f), result.start)
        assertEquals(Offset(30.5f, 64.25f), result.end)
        assertEquals(24f, result.radiusPx, 0f)
    }

    @Test
    fun `gallery state adapter maps all scratch segments`() {
        val state = GalleryState(
            images = listOf(
                ImageItem(Uri.parse("https://example.com"), "Default", "image/jpeg")
            ),
            scratchSegments = listOf(
                ScratchSegment(PointF(0f, 0f), null, 10f),
                ScratchSegment(PointF(5f, 5f), PointF(10f, 10f), 12f)
            )
        )

        val converted = state.toComposeScratchSegments()

        assertEquals(2, converted.size)
        assertEquals(Offset(0f, 0f), converted[0].start)
        assertEquals(null, converted[0].end)
        assertEquals(Offset(5f, 5f), converted[1].start)
        assertEquals(Offset(10f, 10f), converted[1].end)
        assertEquals(12f, converted[1].radiusPx, 0f)
    }

    @Test
    fun `color conversions round trip compose int`() {
        val colorInt = 0xFFAABBCC.toInt()

        val composeColor = colorInt.toComposeColor()
        val restored = composeColor.toColorInt()

        assertEquals(colorInt, restored)
    }

    @Test
    fun `offset to pointf round trip preserves values`() {
        val offset = Offset(14.25f, 72.5f)

        val point = offset.toPointF()
        val backToOffset = point.toOffset()

        assertEquals(offset, backToOffset)
    }
}
