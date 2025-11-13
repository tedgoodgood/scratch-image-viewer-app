package com.example.composeapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
        
        // Set fullscreen flags
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        
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
        // Settings button with popup menu
        binding.settingsMenuButton.setOnClickListener { view ->
            showSettingsPopup(view)
        }

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

        // Initialize with default color overlay to ensure overlay bitmap exists
        binding.scratchOverlay.post {
            binding.scratchOverlay.setScratchColor(0xFAD4AF37.toInt()) // Semi-transparent gold
        }
    }

    private fun showSettingsPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.settings_popup, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.popup_opacity -> {
                    showOpacityDialog()
                    true
                }
                R.id.popup_brush_size -> {
                    showBrushSizeDialog()
                    true
                }
                R.id.popup_color -> {
                    showColorPickerDialog()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
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
            // Reset overlay when image changes to clear old scratches
            binding.scratchOverlay.resetOverlay()
        }
        
        // Update scratch color if it changed
        if (state.scratchColor != currentScratchColor) {
            android.util.Log.d("MainActivity", "Setting color overlay: ${state.scratchColor}")
            binding.scratchOverlay.setScratchColor(state.scratchColor)
            currentScratchColor = state.scratchColor
        }
        
        // Update current image URI tracking
        currentImageUri = state.currentImage?.uri
        
        binding.scratchOverlay.setScratchSegments(state.scratchSegments)

        // Update controls visibility based on fullscreen mode
        if (state.isFullscreen) {
            binding.controlsContainer.visibility = View.GONE
            binding.settingsMenuButton.visibility = View.GONE
            binding.fullscreenControlsContainer.visibility = View.VISIBLE
        } else {
            binding.controlsContainer.visibility = View.VISIBLE
            binding.settingsMenuButton.visibility = View.VISIBLE
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
    
    private fun showOpacityDialog() {
        val currentOpacity = viewModel.state.value?.overlayOpacity?.let { 
            (it * 100) / 255 
        } ?: 98
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Opacity (%)")
        
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentOpacity.toString())
            setSelectAllOnFocus(true)
        }
        
        builder.setView(input)
        builder.setPositiveButton("Save") { dialog, _ ->
            val opacity = input.text.toString().toIntOrNull()
            if (opacity != null && opacity in 0..100) {
                viewModel.setOverlayOpacity(opacity)
                Toast.makeText(this, "Opacity set to $opacity%", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a number between 0-100", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showBrushSizeDialog() {
        val currentBrushSize = viewModel.state.value?.brushSize ?: 10f
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Brush Size (pixels)")
        
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentBrushSize.toString())
            setSelectAllOnFocus(true)
        }
        
        builder.setView(input)
        builder.setPositiveButton("Save") { dialog, _ ->
            val brushSize = input.text.toString().toFloatOrNull()
            if (brushSize != null && brushSize > 0) {
                viewModel.setBrushSize(brushSize)
                Toast.makeText(this, "Brush size set to $brushSize", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showColorPickerDialog() {
        val currentColor = viewModel.state.value?.scratchColor ?: android.graphics.Color.RED
        val rgbColor = android.graphics.Color.argb(
            255,  // No alpha for color selection
            android.graphics.Color.red(currentColor),
            android.graphics.Color.green(currentColor),
            android.graphics.Color.blue(currentColor)
        )
        
        // Simple preset colors dialog
        val colors = intArrayOf(
            android.graphics.Color.RED,
            android.graphics.Color.BLUE,
            android.graphics.Color.GREEN,
            android.graphics.Color.YELLOW,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.CYAN,
            android.graphics.Color.BLACK,
            android.graphics.Color.WHITE,
            android.graphics.Color.GRAY,
            android.graphics.Color.DKGRAY,
            android.graphics.Color.LTGRAY,
            android.graphics.Color.parseColor("#FFA500")  // Orange
        )
        
        val colorNames = arrayOf(
            "Red", "Blue", "Green", "Yellow", 
            "Magenta", "Cyan", "Black", "White",
            "Gray", "Dark Gray", "Light Gray", "Orange"
        )
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Color")
        
        // Find current selection
        val currentSelection = colors.indexOfFirst { 
            it and 0x00FFFFFF == rgbColor and 0x00FFFFFF 
        }.coerceAtLeast(0)
        
        builder.setSingleChoiceItems(colorNames, currentSelection) { _, which ->
            val selectedColor = colors[which]
            viewModel.setOverlayColor(selectedColor)
            Toast.makeText(this, "Color set to ${colorNames[which]}", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.setPositiveButton("Apply") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}