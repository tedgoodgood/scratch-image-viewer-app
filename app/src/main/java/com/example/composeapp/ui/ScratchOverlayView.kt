package com.example.composeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.composeapp.domain.ScratchSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var underlayImageBitmap: Bitmap? = null
    private var scratchSegments: List<ScratchSegment> = emptyList()
    private var isScratching = false
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    
    // Callback to notify when scratches are updated (for persistence)
    var onScratchesUpdated: ((List<ScratchSegment>) -> Unit)? = null
    
    // Flag to prevent infinite loops during synchronization
    private var isSynchronizing = false

    companion object {
        private const val DEFAULT_SCRATCH_COLOR = 0xFAD4AF37.toInt() // Semi-transparent gold (98% opacity)
    }

    fun setScratchColor(color: Int) {
        Log.d("ScratchOverlayView", "setScratchColor called with color: $color")
        scratchColor = color
        
        // Create new overlay with the scratch color
        if (width > 0 && height > 0) {
            overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
            overlayCanvas?.drawColor(scratchColor)
            Log.d("ScratchOverlayView", "Created color overlay: ${width}x${height}")
            
            // Redraw any existing scratches
            redrawScratches()
        } else {
            Log.w("ScratchOverlayView", "Cannot create overlay: width=$width, height=$height")
        }
        
        invalidate()
    }

    fun setBrushSize(size: Float) {
        scratchPaint.strokeWidth = size * 2
    }

    fun setScratchSegments(segments: List<ScratchSegment>) {
        Log.d("ScratchOverlayView", "setScratchSegments called with ${segments.size} segments")
        isSynchronizing = true
        scratchSegments = segments
        
        // Always redraw scratches to ensure overlay bitmap is properly updated
        // This is critical when segments list is cleared (empty) to remove scratches from overlay
        redrawScratches()
        invalidate()
        isSynchronizing = false
    }

    fun resetOverlay() {
        Log.d("ScratchOverlayView", "resetOverlay called")
        scratchSegments = emptyList()
        scratchPath.reset()

        if (overlayBitmap == null || overlayCanvas == null) {
            if (width > 0 && height > 0) {
                overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                overlayCanvas = Canvas(overlayBitmap!!)
                Log.d(
                    "ScratchOverlayView",
                    "Created overlay bitmap during reset: ${width}x${height}"
                )
            } else {
                Log.w(
                    "ScratchOverlayView",
                    "Cannot reset overlay: view has no size yet (width=$width, height=$height)"
                )
                invalidate()
                return
            }
        }

        overlayCanvas?.drawColor(scratchColor)
        invalidate()
    }

    private fun redrawScratches() {
        overlayCanvas?.let { canvas ->
            // Always clear and redraw base color first
            canvas.drawColor(scratchColor)
            
            if (scratchSegments.isNotEmpty()) {
                // Only draw scratches if there are segments
                scratchSegments.forEach { segment ->
                    scratchPath.reset()
                    scratchPath.moveTo(segment.start.x, segment.start.y)
                    segment.end?.let { end ->
                        scratchPath.lineTo(end.x, end.y)
                    }
                    
                    scratchPaint.strokeWidth = segment.radiusPx * 2
                    canvas.drawPath(scratchPath, scratchPaint)
                }
                Log.d("ScratchOverlayView", "Redrew ${scratchSegments.size} scratch segments")
            } else {
                // No segments - just ensure clean overlay with base color
                Log.d("ScratchOverlayView", "No scratch segments to redraw, overlay is clean")
            }
        } ?: run {
            Log.w("ScratchOverlayView", "No overlay canvas available for redraw")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Create overlay bitmap if it doesn't exist or size changed
        if (overlayBitmap == null || overlayBitmap?.width != w || overlayBitmap?.height != h) {
            overlayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
            overlayCanvas?.drawColor(scratchColor)
            Log.d("ScratchOverlayView", "Created overlay bitmap: ${w}x${h}")
            
            redrawScratches()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        Log.d("ScratchOverlayView", "onDraw: canvas=${canvas.width}x${canvas.height}, underlay=${underlayImageBitmap?.width}x${underlayImageBitmap?.height}, overlay=${overlayBitmap?.width}x${overlayBitmap?.height}")
        
        // Step 1: Draw underlay image (the image revealed when scratching) - SCALED to fill canvas
        underlayImageBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, canvas.width, canvas.height)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            Log.d("ScratchOverlayView", "Drew underlay: src=$srcRect dst=$dstRect")
        } ?: run {
            // Fallback to gray background to avoid black screen
            canvas.drawColor(0xFF808080.toInt())
            Log.w("ScratchOverlayView", "No underlay image available, using gray fallback")
        }
        
        // Step 2: Draw overlay (color that gets scratched off) - SCALED to fill canvas
        overlayBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, canvas.width, canvas.height)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            Log.d("ScratchOverlayView", "Drew overlay: src=$srcRect dst=$dstRect")
        } ?: run {
            Log.w("ScratchOverlayView", "No overlay bitmap available")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isScratching = true
                lastTouchX = touchX
                lastTouchY = touchY
                
                // Start new scratch segment - only store locally for drawing
                val newSegment = ScratchSegment(
                    start = PointF(touchX, touchY),
                    end = null,
                    radiusPx = scratchPaint.strokeWidth / 2
                )
                scratchSegments = scratchSegments + newSegment
                Log.d("ScratchOverlayView", "Started new scratch segment. Total segments: ${scratchSegments.size}")
            }

            MotionEvent.ACTION_MOVE -> {
                if (isScratching) {
                    // Draw scratch on overlay bitmap immediately for visual feedback
                    overlayCanvas?.let { canvas ->
                        scratchPath.reset()
                        scratchPath.moveTo(lastTouchX, lastTouchY)
                        scratchPath.lineTo(touchX, touchY)
                        canvas.drawPath(scratchPath, scratchPaint)
                    }
                    
                    // Update the last segment with end point
                    if (scratchSegments.isNotEmpty()) {
                        val lastSegment = scratchSegments.last()
                        scratchSegments = scratchSegments.dropLast(1) + lastSegment.copy(
                            end = PointF(touchX, touchY)
                        )
                    }
                    
                    lastTouchX = touchX
                    lastTouchY = touchY
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScratching && scratchSegments.isNotEmpty()) {
                    Log.d("ScratchOverlayView", "Finished scratch. Total segments: ${scratchSegments.size}")
                    // Notify callback that scratches have been made (for persistence if needed)
                    // Only update if we're not in the middle of synchronizing from ViewModel
                    if (!isSynchronizing) {
                        onScratchesUpdated?.invoke(scratchSegments)
                    }
                }
                isScratching = false
            }
        }

        return true
    }

    fun setUnderlayImage(uri: Uri?) {
        Log.d("ScratchOverlayView", "setUnderlayImage called with URI: $uri")
        
        if (uri == null) {
            underlayImageBitmap = null
            invalidate()
            return
        }

        // Load image in background thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .submit()
                        .get()
                }
                
                underlayImageBitmap = bitmap
                Log.d("ScratchOverlayView", "Underlay image loaded successfully: ${bitmap.width}x${bitmap.height}")
                invalidate()
            } catch (e: Exception) {
                Log.e("ScratchOverlayView", "Failed to load underlay image", e)
                underlayImageBitmap = null
                invalidate()
            }
        }
    }
}