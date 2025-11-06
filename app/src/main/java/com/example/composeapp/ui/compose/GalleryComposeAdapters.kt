package com.example.composeapp.ui.compose

import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.composeapp.domain.GalleryState
import com.example.composeapp.domain.ScratchSegment
import com.example.composeapp.viewmodel.GalleryViewModel
import android.graphics.Color as AndroidColor

data class ComposeScratchSegment(
    val start: Offset,
    val end: Offset?,
    val radiusPx: Float
)

fun ScratchSegment.toCompose(): ComposeScratchSegment = ComposeScratchSegment(
    start = start.toOffset(),
    end = end?.toOffset(),
    radiusPx = radiusPx
)

fun GalleryState.toComposeScratchSegments(): List<ComposeScratchSegment> =
    scratchSegments.map { it.toCompose() }

fun PointF.toOffset(): Offset = Offset(x, y)

fun Offset.toPointF(): PointF = PointF(x, y)

@ColorInt
fun Color.toColorInt(): Int = toArgb()

fun @receiver:ColorInt Int.toComposeColor(): Color {
    val alpha = AndroidColor.alpha(this) / 255f
    val red = AndroidColor.red(this) / 255f
    val green = AndroidColor.green(this) / 255f
    val blue = AndroidColor.blue(this) / 255f
    return Color(red, green, blue, alpha)
}

fun GalleryViewModel.addScratchSegment(start: Offset, end: Offset?, radiusPx: Float) {
    addScratchSegment(start.toPointF(), end?.toPointF(), radiusPx)
}

fun GalleryViewModel.setScratchColor(color: Color) {
    setScratchColor(color.toColorInt())
}
