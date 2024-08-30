package com.example.videoplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class VideoViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _videoStream = MutableStateFlow<Result<InputStream>>(Result.failure(Exception("No data")))
    val videoStream: StateFlow<Result<InputStream>> = _videoStream

    fun loadVideo(videoId: String) {
        viewModelScope.launch {
            val result = repository.fetchVideo(videoId)
            _videoStream.value = result
        }
    }
}