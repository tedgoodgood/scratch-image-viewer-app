package com.example.composeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.composeapp.domain.ScratchSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class ScratchOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val scratchPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val scratchPath = Path()
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private var scratchColor: Int = DEFAULT_SCRATCH_COLOR
    private var customOverlayUri: Uri? = null
    private var frostedGlassUri: Uri? = null
    private var baseImageBitmap: Bitmap? = null
    private var scratchSegments: List<ScratchSegment> = emptyList()
    private var isScratching = false
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    
    // Cache for blurred bitmaps to avoid recomputation
    private val blurCache = mutableMapOf<String, Bitmap>()

    companion object {
        private const val DEFAULT_SCRATCH_COLOR = 0xFAD4AF37.toInt() // Semi-transparent gold (98% opacity)
        private const val BLUR_RADIUS = 40f // Increased for stronger frosted glass effect
        private const val MAX_BLUR_RADIUS_API_16 = 35f // Increased for older devices
    }

    fun setScratchColor(color: Int) {
        scratchColor = color
        customOverlayUri = null
        frostedGlassUri = null
        clearBlurCache()
        
        // Create new overlay with the scratch color
        if (width > 0 && height > 0) {
            overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
            overlayCanvas?.drawColor(scratchColor)
            
            // Redraw any existing scratches
            redrawScratches()
        }
        
        invalidate()
    }

    fun setCustomOverlay(uri: Uri?) {
        android.util.Log.d("ScratchOverlayView", "setCustomOverlay called with URI: $uri")
        customOverlayUri = uri
        frostedGlassUri = null
        clearBlurCache()
        if (uri != null) {
            loadCustomOverlay(uri)
        } else {
            android.util.Log.d("ScratchOverlayView", "Custom overlay URI is null, clearing scratches")
            clearScratches()
            invalidate()
        }
    }

    fun setFrostedGlassOverlay(uri: Uri?) {
        android.util.Log.d("ScratchOverlayView", "setFrostedGlassOverlay called with URI: $uri")
        frostedGlassUri = uri
        customOverlayUri = null
        if (uri != null) {
            loadFrostedGlassOverlay(uri)
        } else {
            android.util.Log.d("ScratchOverlayView", "Frosted glass URI is null, clearing cache and scratches")
            clearBlurCache()
            clearScratches()
            invalidate()
        }
    }

    fun setScratchSegments(segments: List<ScratchSegment>) {
        scratchSegments = segments
        redrawScratches()
    }

    fun setBrushSize(size: Float) {
        scratchPaint.strokeWidth = size * 2
    }

    private fun clearBlurCache() {
        blurCache.values.forEach { it.recycle() }
        blurCache.clear()
    }

    fun clearScratches() {
        // Clear scratch segments first
        scratchSegments = emptyList()
        
        overlayBitmap?.let { bitmap ->
            overlayCanvas?.let { canvas ->
                when {
                    customOverlayUri != null -> {
                        // Reload the custom image to clear scratches
                        customOverlayUri?.let { loadCustomOverlay(it) }
                    }
                    frostedGlassUri != null -> {
                        // Reload the frosted glass to clear scratches
                        frostedGlassUri?.let { loadFrostedGlassOverlay(it) }
                    }
                    else -> {
                        // Clear and redraw with scratch color
                        canvas.drawColor(scratchColor)
                    }
                }
            }
        } ?: run {
            // If no overlay bitmap exists, create one with scratch color
            if (width > 0 && height > 0) {
                overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                overlayCanvas = Canvas(overlayBitmap!!)
                overlayCanvas?.drawColor(scratchColor)
            }
        }
        invalidate()
    }
    
    fun resetOverlay() {
        android.util.Log.d("ScratchOverlayView", "resetOverlay called")
        // Force a complete reset by clearing everything and reloading
        scratchSegments = emptyList()
        scratchPath.reset()
        
        when {
            customOverlayUri != null -> {
                android.util.Log.d("ScratchOverlayView", "Reset: Reloading custom overlay")
                customOverlayUri?.let { loadCustomOverlay(it) }
            }
            frostedGlassUri != null -> {
                android.util.Log.d("ScratchOverlayView", "Reset: Reloading frosted glass overlay")
                frostedGlassUri?.let { loadFrostedGlassOverlay(it) }
            }
            else -> {
                android.util.Log.d("ScratchOverlayView", "Reset: Recreating color overlay")
                // Recreate color overlay
                if (width > 0 && height > 0) {
                    overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    overlayCanvas = Canvas(overlayBitmap!!)
                    overlayCanvas?.drawColor(scratchColor)
                }
            }
        }
        invalidate()
    }

    private fun loadCustomOverlay(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (uri.scheme == "file") {
                        val file = uri.toFile()
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        // For content URIs, use Glide to load the bitmap
                        Glide.with(context)
                            .asBitmap()
                            .load(uri)
                            .submit(width, height)
                            .get()
                    }
                }
                
                bitmap?.let {
                    // Scale bitmap to fit the view dimensions
                    val scaledBitmap = if (width > 0 && height > 0) {
                        Bitmap.createScaledBitmap(it, width, height, true)
                    } else {
                        it
                    }
                    
                    // Create overlay bitmap with the custom image
                    overlayBitmap = scaledBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                    overlayCanvas = Canvas(overlayBitmap!!)
                    
                    // Redraw any existing scratches after loading the new overlay
                    redrawScratches()
                    invalidate()
                }
            } catch (e: Exception) {
                // Fallback to color overlay if loading fails
                customOverlayUri = null
                clearScratches()
            }
        }
    }

    private fun loadFrostedGlassOverlay(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cacheKey = "${uri.toString()}_${width}x${height}"
                
                // Check cache first
                blurCache[cacheKey]?.let { cachedBitmap ->
                    overlayBitmap = cachedBitmap.copy(cachedBitmap.config, true)
                    overlayCanvas = Canvas(overlayBitmap!!)
                    
                    // Redraw any existing scratches after loading from cache
                    redrawScratches()
                    invalidate()
                    return@launch
                }
                
                val bitmap = withContext(Dispatchers.IO) {
                    if (uri.scheme == "file") {
                        val file = uri.toFile()
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        // For content URIs, use Glide to load the bitmap
                        Glide.with(context)
                            .asBitmap()
                            .load(uri)
                            .submit(width, height)
                            .get()
                    }
                }
                
                bitmap?.let {
                    // Don't set baseImageBitmap here - it should be managed by setBaseImage()
                    // Scale bitmap to fit the view dimensions
                    val scaledBitmap = if (width > 0 && height > 0) {
                        Bitmap.createScaledBitmap(it, width, height, true)
                    } else {
                        it
                    }
                    
                    // Apply blur effect based on API level
                    val blurredBitmap = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                            applyBlurWithRenderScript(scaledBitmap)
                        }
                        else -> {
                            applyBlurWithStackBlur(scaledBitmap)
                        }
                    }
                    
                    blurredBitmap?.let { blurred ->
                        // Cache the result (read-only copy)
                        blurCache[cacheKey] = blurred.copy(blurred.config, false)
                        
                        // Create mutable overlay bitmap for scratching
                        overlayBitmap = blurred.copy(Bitmap.Config.ARGB_8888, true)
                        overlayCanvas = Canvas(overlayBitmap!!)
                        
                        // Redraw any existing scratches after loading the new overlay
                        redrawScratches()
                        invalidate()
                    }
                }
            } catch (e: Exception) {
                // Fallback to color overlay if loading fails
                frostedGlassUri = null
                clearScratches()
            }
        }
    }

    private fun applyBlurWithRenderEffect(bitmap: Bitmap): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // For API 31+, use RenderEffect with a different approach
                // Since RenderEffect doesn't work directly on Canvas in this context,
                // we'll fall back to RenderScript for now
                applyBlurWithRenderScript(bitmap)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private fun applyBlurWithRenderScript(bitmap: Bitmap): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                val rs = RenderScript.create(context)
                val input = Allocation.createFromBitmap(rs, bitmap)
                val output = Allocation.createTyped(rs, input.type)
                val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                
                blur.setRadius(BLUR_RADIUS)
                blur.setInput(input)
                blur.forEach(output)
                
                output.copyTo(bitmap)
                rs.destroy()
                bitmap
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private fun applyBlurWithStackBlur(bitmap: Bitmap): Bitmap? {
        return try {
            // Stack blur implementation for API 14-16
            val radius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                BLUR_RADIUS.toInt()
            } else {
                MAX_BLUR_RADIUS_API_16.toInt()
            }
            
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Apply stack blur algorithm
            stackBlur(pixels, width, height, radius)
            
            val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            blurredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            blurredBitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun stackBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val wm = width - 1
        val hm = height - 1
        val wh = width * height
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        
        val vmin = IntArray(max(width, height))
        val divsum = IntArray(div * 256)
        
        for (i in 0 until div * 256) {
            divsum[i] = i / div
        }
        
        val stack = Array(div) { IntArray(3) }
        
        // Horizontal blur
        for (yPos in 0 until height) {
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var yp = -radius * width
            
            for (i in -radius..radius) {
                val yi = max(0, min(yPos + i, hm)) * width
                val sir = stack[i + radius]
                sir[0] = pixels[yi]
                sir[1] = pixels[yi + 1]
                sir[2] = pixels[yi + 2]
                
                val rbs = radius - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                
                if (i > 0) {
                    vmin[yPos + i] = min(yPos + i, hm)
                }
            }
            
            var xPos = 0
            while (xPos < width) {
                gsum += if (xPos <= radius) -1 else 1
                bsum += if (xPos <= radius) -1 else 1
                rsum += if (xPos <= radius) -1 else 1
                
                val stackpointer = (xPos + radius) % div
                val sir = stack[stackpointer]
                
                val yi = xPos + vmin[yPos + radius]
                sir[0] = pixels[yi]
                sir[1] = pixels[yi + 1]
                sir[2] = pixels[yi + 2]
                
                rsum += sir[0]
                gsum += sir[1]
                bsum += sir[2]
                
                val yiPrev = xPos - radius
                if (yiPrev >= 0) {
                    val sirPrev = stack[(yiPrev + div) % div]
                    rsum -= sirPrev[0]
                    gsum -= sirPrev[1]
                    bsum -= sirPrev[2]
                }
                
                val yiFinal = xPos
                r[yiFinal] = divsum[rsum]
                g[yiFinal] = divsum[gsum]
                b[yiFinal] = divsum[bsum]
                xPos++
            }
        }
        
        // Vertical blur
        for (xPos in 0 until width) {
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var yp = -radius * width
            
            for (i in -radius..radius) {
                val yi = max(0, min(xPos + i, wm))
                
                val sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                
                val rbs = radius - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                
                if (i > 0) {
                    vmin[xPos + i] = min(xPos + i, wm)
                }
            }
            
            var yPos = 0
            while (yPos < height) {
                gsum += if (yPos <= radius) -1 else 1
                bsum += if (yPos <= radius) -1 else 1
                rsum += if (yPos <= radius) -1 else 1
                
                val stackpointer = (yPos + radius) % div
                val sir = stack[stackpointer]
                
                val yi = xPos + vmin[yPos + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                
                rsum += sir[0]
                gsum += sir[1]
                bsum += sir[2]
                
                val yiPrev = yPos - radius
                if (yiPrev >= 0) {
                    val sirPrev = stack[(yiPrev + div) % div]
                    rsum -= sirPrev[0]
                    gsum -= sirPrev[1]
                    bsum -= sirPrev[2]
                }
                
                val pixelIndex = xPos + yPos * width
                pixels[pixelIndex] = -0x1000000 and pixels[pixelIndex] or (rsum shl 16) or (gsum shl 8) or bsum
                yPos++
            }
        }
    }

    private fun max(a: Int, b: Int): Int = if (a > b) a else b
    private fun min(a: Int, b: Int): Int = if (a < b) a else b

    private fun redrawScratches() {
        overlayCanvas?.let { canvas ->
            scratchSegments.forEach { segment ->
                scratchPaint.strokeWidth = segment.radiusPx * 2
                scratchPath.reset()
                scratchPath.moveTo(segment.start.x, segment.start.y)
                
                segment.end?.let { end ->
                    scratchPath.lineTo(end.x, end.y)
                } ?: run {
                    scratchPath.lineTo(segment.start.x, segment.start.y)
                }
                
                canvas.drawPath(scratchPath, scratchPaint)
            }
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (w > 0 && h > 0) {
            // Create new overlay bitmap for the new size
            overlayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
            
            when {
                customOverlayUri != null -> {
                    customOverlayUri?.let { loadCustomOverlay(it) }
                }
                frostedGlassUri != null -> {
                    frostedGlassUri?.let { loadFrostedGlassOverlay(it) }
                }
                else -> {
                    overlayCanvas?.drawColor(scratchColor)
                    redrawScratches()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // CRITICAL FIX: Two-layer rendering system
        // Layer 1 (bottom): Always draw underlay image first - this is what shows when scratching
        var underlayDrawn = false
        baseImageBitmap?.let { baseBitmap ->
            canvas.drawBitmap(baseBitmap, 0f, 0f, null)
            underlayDrawn = true
            Log.d("ScratchOverlayView", "Drew base image bitmap as underlay")
        }
        
        if (!underlayDrawn) {
            Log.w("ScratchOverlayView", "No underlay image available - this will cause black background when scratching!")
        }
        
        // Layer 2 (top): Draw the overlay (gets scratched away to reveal underlay below)
        overlayBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            Log.d("ScratchOverlayView", "Drew overlay bitmap")
        } ?: run {
            Log.w("ScratchOverlayView", "No overlay bitmap available")
        }
        
        Log.d("ScratchOverlayView", "onDraw complete: underlayDrawn=$underlayDrawn, overlayBitmap=${overlayBitmap != null}, scratches=${scratchSegments.size}")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isScratching = true
                lastTouchX = event.x
                lastTouchY = event.y
                
                // Start a new scratch segment
                val segment = ScratchSegment(
                    start = PointF(event.x, event.y),
                    end = null,
                    radiusPx = scratchPaint.strokeWidth / 2
                )
                scratchSegments = scratchSegments + segment
                
                scratchPath.reset()
                scratchPath.moveTo(event.x, event.y)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isScratching) {
                    val currentX = event.x
                    val currentY = event.y
                    
                    overlayCanvas?.let { canvas ->
                        scratchPath.lineTo(currentX, currentY)
                        canvas.drawPath(scratchPath, scratchPaint)
                    }
                    
                    // Update the last segment with the end point
                    if (scratchSegments.isNotEmpty()) {
                        val lastSegment = scratchSegments.last()
                        val updatedSegment = lastSegment.copy(end = PointF(currentX, currentY))
                        scratchSegments = scratchSegments.dropLast(1) + updatedSegment
                    }
                    
                    lastTouchX = currentX
                    lastTouchY = currentY
                    
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScratching) {
                    isScratching = false
                    scratchPath.reset()
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }

    fun getScratchSegments(): List<ScratchSegment> {
        return scratchSegments
    }
    
    fun setBaseImage(bitmap: Bitmap?) {
        baseImageBitmap = bitmap
        invalidate()
    }
}