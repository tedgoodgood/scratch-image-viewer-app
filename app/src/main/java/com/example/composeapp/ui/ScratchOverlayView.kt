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
import android.net.Uri
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
    private var scratchSegments: List<ScratchSegment> = emptyList()
    private var isScratching = false
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    companion object {
        private const val DEFAULT_SCRATCH_COLOR = 0xFFD4AF37.toInt()
    }

    fun setScratchColor(color: Int) {
        scratchColor = color
        customOverlayUri = null
        clearScratches()
        invalidate()
    }

    fun setCustomOverlay(uri: Uri?) {
        customOverlayUri = uri
        if (uri != null) {
            loadCustomOverlay(uri)
        } else {
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

    fun clearScratches() {
        overlayBitmap?.let { bitmap ->
            overlayCanvas?.let { canvas ->
                if (customOverlayUri != null) {
                    overlayBitmap = overlayBitmap?.copy(bitmap.config, true)
                    overlayCanvas = Canvas(overlayBitmap!!)
                } else {
                    canvas.drawColor(scratchColor)
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
                    
                    overlayBitmap = scaledBitmap?.copy(scaledBitmap.config, true)
                    overlayCanvas = Canvas(overlayBitmap!!)
                    invalidate()
                }
            } catch (e: Exception) {
                // Fallback to color overlay if loading fails
                customOverlayUri = null
                clearScratches()
            }
        }
    }

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
            overlayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
            
            if (customOverlayUri != null) {
                customOverlayUri?.let { loadCustomOverlay(it) }
            } else {
                overlayCanvas?.drawColor(scratchColor)
            }
            
            redrawScratches()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        overlayBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
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
}