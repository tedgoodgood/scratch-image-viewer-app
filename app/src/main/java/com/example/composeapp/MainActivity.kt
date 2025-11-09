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
import androidx.core.view.ViewCompat
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
        
        // Fix navigation bar obstruction
        ViewCompat.setOnApplyWindowInsetsListener(binding.controlsContainer) { v, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                navBarHeight + 16  // 16dp extra spacing
            )
            insets
        }
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

        // Opacity slider control
        binding.opacitySlider.max = 100
        binding.opacitySlider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.opacityValue.text = "$progress%"
                if (fromUser) {
                    viewModel.setOverlayOpacity(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Color picker control
        binding.colorPickerButton.setOnClickListener {
            showColorPickerDialog()
        }

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
        // Use current gallery image as underlay
        val targetUnderlayUri = state.currentImage?.uri
        android.util.Log.d("MainActivity", "Underlay update: currentUri=${state.currentImage?.uri}, target=$targetUnderlayUri")
        if (targetUnderlayUri != currentImageUri) {
            android.util.Log.d("MainActivity", "Updating underlay image to: $targetUnderlayUri")
            updateUnderlayImage(targetUnderlayUri)
        }
        
        // Update scratch color if it changed
        if (state.scratchColor != currentScratchColor) {
            android.util.Log.d("MainActivity", "Setting color overlay: ${state.scratchColor}")
            binding.scratchOverlay.setScratchColor(state.scratchColor)
            currentScratchColor = state.scratchColor
        }
        
        // Update opacity slider to saved value
        val opacityPercent = (state.overlayOpacity * 100) / 255
        binding.opacitySlider.progress = opacityPercent
        binding.opacityValue.text = "$opacityPercent%"
        
        // Update color picker button to saved color
        val colorWithoutAlpha = state.scratchColor and 0x00FFFFFF
        binding.colorPickerButton.setBackgroundColor(colorWithoutAlpha)
        
        // Update current image URI tracking
        currentImageUri = state.currentImage?.uri
        
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
        binding.scratchOverlay.setUnderlayImage(imageUri)
    }
    
    private fun showColorPickerDialog() {
        val colors = listOf(
            "Red" to android.graphics.Color.RED,
            "Blue" to android.graphics.Color.BLUE,
            "Green" to android.graphics.Color.GREEN,
            "Yellow" to android.graphics.Color.YELLOW,
            "Magenta" to android.graphics.Color.MAGENTA,
            "Cyan" to android.graphics.Color.CYAN,
            "Black" to android.graphics.Color.BLACK,
            "White" to android.graphics.Color.WHITE
        )
        
        val colorNames = colors.map { it.first }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose Color")
            .setItems(colorNames) { _, which ->
                val selectedColor = colors[which].second
                viewModel.setOverlayColor(selectedColor)
            }
            .show()
    }
}