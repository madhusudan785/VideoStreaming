import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplay.VideoPlayer.VideoPlayer
import com.example.videoplay.data.VideoData
import com.example.videoplay.databinding.ItemListBinding
import com.squareup.picasso.Picasso

class MyAdapter(private val videoList: List<VideoData>) :
    RecyclerView.Adapter<MyAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        with(holder.binding) {
            videoTitle.text = video.title
            videoDescription.text = video.description

            // Load thumbnail using Glide or Picasso
            Picasso.get()
                .load(video.thumbnail)
                .into(videoThumbnail)

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, VideoPlayer::class.java)
                intent.putExtra("VIDEO_ID", video.videoId)
                context.startActivity(intent)
            }
        }

    }

    override fun getItemCount(): Int = videoList.size
}