package com.example.videoplay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videoplay.databinding.ActivityMainBinding
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {
    private lateinit var binding: ActivityMainBinding
    private var selectedFileUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chooseFile.setOnClickListener {
            openMediaChooser()
        }
        binding.btnUpload.setOnClickListener {
            uploadMedia()
        }

    }

    private fun openMediaChooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "video/*"
            val mimeTypes = arrayOf("video/mp4", "video/x-matroska", "video/webm", "video/avi", "video/3gpp")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_PICK_VIDEO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uri ->
                // Display video file name
                val fileName = getFileName(uri)
                binding.textView.text = fileName
            }
        }
    }
    private fun uploadMedia() {
        if (selectedFileUri == null) {
            binding.layoutRoot.snackbar("Select a Video First")
            return
        }

        val parcelFileDescriptor =  contentResolver.openFileDescriptor(selectedFileUri!!, "r", null) ?: return
        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedFileUri!!))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        binding.progressBar.progress = 0
        // Creating RequestBody for the file
        val requestBody = UploadRequestBody(file, "video", this)
        val videoPart = MultipartBody.Part.createFormData("file", file.name, requestBody)
        // Creating RequestBody for title and description
        val title = binding.etCustomBox.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val description = binding.etDescriptionBox.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        //upload
        MyAPI().uploadVideo(videoPart, title, description).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                binding.progressBar.progress = 100
                if (response.isSuccessful) {
                    binding.layoutRoot.snackbar("Video uploaded successfully")
                    Log.d("Upload", "Success: ${response.body()}")
                } else {
                    binding.layoutRoot.snackbar("Failed to upload video")
                    Log.e("Upload", "Server error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                binding.layoutRoot.snackbar("Failed to upload video: ${t.message}")
                Log.e("UploadVideoError", "Failed to upload video:${t.message}", t)
            }
        })
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Get the index of the column DISPLAY_NAME
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
    companion object {
        const val REQUEST_CODE_PICK_VIDEO = 101
    }

    override fun onProgressUpdate(percentage: Int) {
        binding.progressBar.progress = percentage
    }
}