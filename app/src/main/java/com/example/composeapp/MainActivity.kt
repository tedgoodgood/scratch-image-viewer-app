package com.example.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.composeapp.ui.ImagePickerScreen
import com.example.composeapp.ui.theme.ComposeAppTheme
import com.example.composeapp.viewmodel.ImagePickerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ImagePickerViewModel = viewModel(
                        factory = ImagePickerViewModel.Factory
                    )
                    ImagePickerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
