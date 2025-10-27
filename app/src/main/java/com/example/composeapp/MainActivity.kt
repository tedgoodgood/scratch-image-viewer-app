package com.example.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.composeapp.ui.theme.ComposeAppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val systemUiController = rememberSystemUiController()
    val colorScheme = MaterialTheme.colorScheme
    val useDarkIcons = colorScheme.background.luminance() > 0.5f

    LaunchedEffect(systemUiController, useDarkIcons, colorScheme.background, colorScheme.surface) {
        systemUiController.setStatusBarColor(
            color = colorScheme.background,
            darkIcons = useDarkIcons
        )
        systemUiController.setNavigationBarColor(
            color = colorScheme.surface,
            darkIcons = useDarkIcons
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://images.unsplash.com/photo-1517816428104-76709c0e7ecf?auto=format&fit=crop&w=600&q=80")
                    .crossfade(true)
                    .build(),
                contentDescription = "Compose app hero image",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(220.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "ComposeApp",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Kickstart your Jetpack Compose project with modern tooling and a delightful UI out of the box.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { /* TODO: Navigate to next screen */ }) {
                Text(text = "Get Started")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Built with Kotlin & Jetpack Compose",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ComposeAppTheme {
        HomeScreen()
    }
}
