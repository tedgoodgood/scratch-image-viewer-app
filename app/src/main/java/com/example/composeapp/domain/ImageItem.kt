package com.example.composeapp.domain

import android.net.Uri

data class ImageItem(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?
)
