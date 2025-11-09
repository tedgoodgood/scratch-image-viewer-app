package com.example.composeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
        scratchSegments = segments
        redrawScratches()
        invalidate()
    }

    fun resetOverlay() {
        Log.d("ScratchOverlayView", "resetOverlay called")
        scratchSegments = emptyList()
        setScratchColor(scratchColor) // Recreate overlay with current color
    }

    private fun redrawScratches() {
        overlayCanvas?.let { canvas ->
            canvas.drawColor(scratchColor) // Clear and redraw base color
            
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
        
        // Step 1: Draw underlay image (the image revealed when scratching)
        underlayImageBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        } ?: run {
            // Fallback to gray background to avoid black screen
            canvas.drawColor(0xFF808080.toInt())
            Log.w("ScratchOverlayView", "No underlay image available, using gray fallback")
        }
        
        // Step 2: Draw overlay (color that gets scratched off)
        overlayBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
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
                
                // Start new scratch segment
                val newSegment = ScratchSegment(
                    start = PointF(touchX, touchY),
                    end = null,
                    radiusPx = scratchPaint.strokeWidth / 2
                )
                scratchSegments = scratchSegments + newSegment
            }

            MotionEvent.ACTION_MOVE -> {
                if (isScratching) {
                    // Draw scratch on overlay bitmap
                    overlayCanvas?.let { canvas ->
                        scratchPath.reset()
                        scratchPath.moveTo(lastTouchX, lastTouchY)
                        scratchPath.lineTo(touchX, touchY)
                        canvas.drawPath(scratchPath, scratchPaint)
                    }
                    
                    // Update the last segment
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