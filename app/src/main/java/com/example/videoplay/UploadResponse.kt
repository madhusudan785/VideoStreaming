package com.example.videoplay

import android.service.quicksettings.Tile

data class UploadResponse (
    val error: Boolean,
    val message: String,
    val video: String,
    )