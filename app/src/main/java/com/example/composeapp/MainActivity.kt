package com.example.composeapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.composeapp.R
import com.example.composeapp.databinding.ActivityMainBinding
import com.example.composeapp.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GalleryViewModel by viewModels { GalleryViewModel.Factory }
    
    // Track current overlay state to avoid unnecessary updates
    private var currentOverlayType: com.example.composeapp.domain.OverlayType = com.example.composeapp.domain.OverlayType.COLOR
    private var currentOverlayUri: android.net.Uri? = null
    private var currentUnderlayImageUri: android.net.Uri? = null
    private var currentImageUri: android.net.Uri? = null
    private var currentScratchColor: Int = 0

    private val selectImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = result.data?.clipData?.let { clipData ->
                (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
            } ?: result.data?.data?.let { listOf(it) } ?: emptyList()
            
            if (uris.isNotEmpty()) {
                viewModel.selectImages(uris)
            }
        }
    }

    private val selectOverlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            viewModel.selectOverlay(uri)
        }
    }

    private val selectFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                viewModel.selectFolder(uri)
            }
        }
    }

    private val selectUnderlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            viewModel.selectUnderlayImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Image selection
        binding.selectImagesButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            selectImagesLauncher.launch(Intent.createChooser(intent, "Select Images"))
        }

        // Folder selection
        binding.selectFolderButton.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
            } else {
                // Fallback for API 14-20: Use ACTION_PICK for directory selection
                Intent(Intent.ACTION_PICK).apply {
                    type = "vnd.android.cursor.dir/primary"
                }
            }
            selectFolderLauncher.launch(Intent.createChooser(intent, "Select Folder"))
        }

        // Underlay image selection
        binding.selectUnderlayButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            selectUnderlayLauncher.launch(Intent.createChooser(intent, "Select Underlay Image"))
        }

        // Navigation controls
        binding.previousButton.setOnClickListener {
            viewModel.goToPrevious()
        }

        binding.nextButton.setOnClickListener {
            viewModel.goToNext()
        }

        binding.fullscreenButton.setOnClickListener {
            viewModel.toggleFullscreen()
        }

        // Fullscreen overlay controls
        binding.fullscreenPreviousButton.setOnClickListener {
            viewModel.goToPrevious()
        }

        binding.fullscreenNextButton.setOnClickListener {
            viewModel.goToNext()
        }

        binding.fullscreenExitButton.setOnClickListener {
            viewModel.toggleFullscreen()
        }

        // Brush size control
        binding.brushSizeSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val brushSize = 10f + progress // Range from 10 to 100
                binding.brushSizeText.text = brushSize.toInt().toString()
                if (fromUser) {
                    viewModel.setBrushSize(brushSize)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Overlay color selection
        binding.colorGoldButton.setOnClickListener {
            viewModel.setScratchColor(0xFAD4AF37.toInt()) // Semi-transparent gold (98% opacity)
        }

        binding.colorSilverButton.setOnClickListener {
            viewModel.setScratchColor(0xFAC0C0C0.toInt()) // Semi-transparent silver (98% opacity)
        }

        binding.colorBronzeButton.setOnClickListener {
            viewModel.setScratchColor(0xFACD7F32.toInt()) // Semi-transparent bronze (98% opacity)
        }

        binding.customOverlayButton.setOnClickListener {
            // Debug: Custom overlay button clicked
            android.util.Log.d("MainActivity", "Custom overlay button clicked")
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            selectOverlayLauncher.launch(Intent.createChooser(intent, "Select Overlay Image"))
        }

        binding.frostedGlassButton.setOnClickListener {
            // Debug: Frosted glass button clicked
            android.util.Log.d("MainActivity", "Frosted glass button clicked")
            viewModel.setFrostedGlassOverlay()
        }

        binding.resetButton.setOnClickListener {
            // Clear scratch segments in ViewModel
            viewModel.resetOverlay()
            // Also trigger immediate overlay reset in the view
            binding.scratchOverlay.resetOverlay()
        }

        // Error handling
        binding.dismissErrorButton.setOnClickListener {
            viewModel.clearError()
        }

        // Initialize brush size
        binding.brushSizeText.text = "40"
        
        // Initialize with default color overlay to ensure overlay bitmap exists
        binding.scratchOverlay.post {
            binding.scratchOverlay.setScratchColor(0xFAD4AF37.toInt()) // Semi-transparent gold
            // Set default underlay to ensure we never have black background
            setDefaultUnderlay()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.example.composeapp.domain.GalleryState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Error state
        if (state.error != null) {
            binding.errorContainer.visibility = View.VISIBLE
            binding.errorText.text = state.error
        } else {
            binding.errorContainer.visibility = View.GONE
        }

        // Gallery visibility
        binding.galleryContainer.visibility = if (state.hasImages && !state.isLoading) View.VISIBLE else View.GONE

        // Update current image
        state.currentImage?.let { imageItem ->
            Glide.with(this)
                .load(imageItem.uri)
                .into(binding.mainImage)
            android.util.Log.d("MainActivity", "Updated main image: ${imageItem.uri}")
        }

        // Update image counter
        if (state.hasImages) {
            binding.imageCounter.text = "${state.currentIndex + 1}/${state.images.size}"
        }

        // Update navigation buttons
        binding.previousButton.isEnabled = state.canGoPrevious
        binding.nextButton.isEnabled = state.canGoNext

        // Update fullscreen button icon
        binding.fullscreenButton.setImageResource(
            if (state.isFullscreen) R.drawable.ic_fullscreen_exit 
            else R.drawable.ic_fullscreen
        )

        // Update scratch overlay
        binding.scratchOverlay.setBrushSize(state.brushSize)
        
        // Update underlay image - ALWAYS show something when scratching
        // Priority: explicit underlay -> current gallery image -> default image
        val targetUnderlayUri = state.underlayImageUri ?: state.currentImage?.uri
        android.util.Log.d("MainActivity", "Underlay update: type=${state.overlayType}, underlayUri=${state.underlayImageUri}, currentUri=${state.currentImage?.uri}, target=$targetUnderlayUri")
        if (targetUnderlayUri != currentUnderlayImageUri) {
            android.util.Log.d("MainActivity", "Updating underlay image to: $targetUnderlayUri")
            updateUnderlayImage(targetUnderlayUri)
        }
        
        // Only update overlay when it actually changes
        if (state.overlayType != currentOverlayType || 
            state.customOverlayUri != currentOverlayUri ||
            state.scratchColor != currentScratchColor) {
            
            android.util.Log.d("MainActivity", "Overlay type changed: ${state.overlayType}, URI: ${state.customOverlayUri}")
            
            when (state.overlayType) {
                com.example.composeapp.domain.OverlayType.CUSTOM_IMAGE -> {
                    android.util.Log.d("MainActivity", "Setting custom overlay with URI: ${state.customOverlayUri}")
                    if (state.customOverlayUri != null) {
                        binding.scratchOverlay.setCustomOverlay(state.customOverlayUri)
                    } else {
                        // If no custom overlay URI is set, clear overlay to fallback state
                        android.util.Log.d("MainActivity", "Custom overlay URI is null, clearing overlay")
                        binding.scratchOverlay.setCustomOverlay(null)
                    }
                }
                com.example.composeapp.domain.OverlayType.FROSTED_GLASS -> {
                    // For frosted glass, use the underlay image URI if available, otherwise current image
                    val frostedGlassUri = state.underlayImageUri ?: state.currentImage?.uri
                    android.util.Log.d("MainActivity", "Setting frosted glass overlay with URI: $frostedGlassUri")
                    binding.scratchOverlay.setFrostedGlassOverlay(frostedGlassUri)
                }
                com.example.composeapp.domain.OverlayType.COLOR -> {
                    android.util.Log.d("MainActivity", "Setting color overlay: ${state.scratchColor}")
                    binding.scratchOverlay.setScratchColor(state.scratchColor)
                }
            }
            
            // Update tracking variables
            currentOverlayType = state.overlayType
            currentOverlayUri = state.customOverlayUri
            currentScratchColor = state.scratchColor
        }
        
        // Update current image URI tracking
        currentImageUri = state.currentImage?.uri
        currentUnderlayImageUri = state.underlayImageUri ?: state.currentImage?.uri
        
        binding.scratchOverlay.setScratchSegments(state.scratchSegments)

        // Update controls visibility based on fullscreen mode
        if (state.isFullscreen) {
            binding.controlsContainer.visibility = View.GONE
            binding.topControls.visibility = View.GONE
            binding.fullscreenControlsContainer.visibility = View.VISIBLE
        } else {
            binding.controlsContainer.visibility = View.VISIBLE
            binding.topControls.visibility = View.VISIBLE
            binding.fullscreenControlsContainer.visibility = View.GONE
        }

        // Update fullscreen navigation buttons state
        binding.fullscreenPreviousButton.isEnabled = state.canGoPrevious
        binding.fullscreenNextButton.isEnabled = state.canGoNext
    }
    
    private fun updateUnderlayImage(imageUri: android.net.Uri?) {
        android.util.Log.d("MainActivity", "updateUnderlayImage called with URI: $imageUri")
        imageUri?.let { uri ->
            // Load the underlay image for scratch rendering
            lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        if (uri.scheme == "file") {
                            val file = uri.toFile()
                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            // For content URIs, use Glide to load the bitmap
                            Glide.with(this@MainActivity)
                                .asBitmap()
                                .load(uri)
                                .submit()
                                .get()
                        }
                    }
                    
                    bitmap?.let {
                        android.util.Log.d("MainActivity", "Underlay bitmap loaded successfully: ${it.width}x${it.height}")
                        android.util.Log.d("MainActivity", "ScratchOverlayView dimensions: ${binding.scratchOverlay.width}x${binding.scratchOverlay.height}")
                        
                        // Scale bitmap to fit the view dimensions
                        val scaledBitmap = if (binding.scratchOverlay.width > 0 && binding.scratchOverlay.height > 0) {
                            android.graphics.Bitmap.createScaledBitmap(it, binding.scratchOverlay.width, binding.scratchOverlay.height, true)
                        } else {
                            android.util.Log.w("MainActivity", "ScratchOverlayView has zero dimensions, using original bitmap")
                            it
                        }
                        android.util.Log.d("MainActivity", "Setting underlay bitmap in ScratchOverlayView: ${scaledBitmap.width}x${scaledBitmap.height}")
                        binding.scratchOverlay.setBaseImage(scaledBitmap)
                    } ?: run {
                        android.util.Log.e("MainActivity", "Failed to load underlay bitmap")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error loading underlay image", e)
                }
            }
        } ?: run {
            android.util.Log.d("MainActivity", "No underlay URI provided, clearing base image")
            binding.scratchOverlay.setBaseImage(null)
        }
    }
    
    private fun setDefaultUnderlay() {
        // Set a default underlay image to prevent black background
        val defaultUri = android.net.Uri.parse("https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80")
        android.util.Log.d("MainActivity", "Setting default underlay image")
        updateUnderlayImage(defaultUri)
    }
}