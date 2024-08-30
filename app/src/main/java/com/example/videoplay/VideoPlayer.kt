package com.example.videoplay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.videoplay.databinding.ActivityVideoPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import java.io.File
import java.io.InputStream

class VideoPlayer : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var player: ExoPlayer
    private val viewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(VideoRepository(apiService = MyAPI()))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        val videoId = "32f8a8bf-7eca-4f79-8aac-b271a333f8a7"
        viewModel.loadVideo(videoId)
        observeVideoStream()

    }

    private fun observeVideoStream() {
        lifecycleScope.launchWhenStarted {
            viewModel.videoStream.collect { result ->
                result.onSuccess { inputStream ->
                    // Create a MediaSource from InputStream
                    val mediaSource = createMediaSourceFromInputStream(inputStream)
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.playWhenReady = true
                }.onFailure { exception ->
                    Toast.makeText(this@VideoPlayer, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createMediaSourceFromInputStream(inputStream: InputStream): MediaSource {
        val tempFile = File.createTempFile("video", ".mp4", cacheDir).apply {
            outputStream().use { output ->
                inputStream.copyTo(output)
            }
        }

        val dataSourceFactory = DefaultDataSource.Factory(this)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(tempFile.toUri()))
        return mediaSource

    }
}