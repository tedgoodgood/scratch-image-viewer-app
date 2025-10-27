package com.example.composeapp.domain

import android.net.Uri

data class ImageMetadata(
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)
