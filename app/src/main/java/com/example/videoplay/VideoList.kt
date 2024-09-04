package com.example.videoplay

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplay.Adapter.MyAdapter
import com.example.videoplay.ViewModel.VideoListViewModel
import com.example.videoplay.databinding.ActivityVideoListBinding


class VideoList : AppCompatActivity() {
    private lateinit var binding: ActivityVideoListBinding
    private lateinit var myAdapter: MyAdapter
    private val videoListViewModel: VideoListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()

        // Observe the LiveData from the ViewModel
        videoListViewModel.videos.observe(this) { videos ->
            myAdapter = MyAdapter(this@VideoList, videos)
            binding.recyclerView.adapter = myAdapter
        }

        // Optionally, fetch videos explicitly if needed
        videoListViewModel.fetchAllVideos()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
    }

}
