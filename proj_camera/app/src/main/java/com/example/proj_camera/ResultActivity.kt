package com.example.proj_camera

import Utils.RotateTransformation
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.proj_camera.databinding.ResultActivityBinding
import java.io.File

class ResultActivity : AppCompatActivity() {
    private lateinit var viewBinding: ResultActivityBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        viewBinding = ResultActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

//        val cameraId = intent.getStringExtra("cameraId")
//        val imgageFormat = intent.getStringExtra("imageFormat")
        val dngPath = intent.getStringExtra("dngPath")
        val bmpPath = intent.getStringExtra("bmpPath")

        val options = RequestOptions.skipMemoryCacheOf(true).diskCacheStrategy(DiskCacheStrategy.NONE)
        options.transform(RotateTransformation(this, -90f))

        val bmp_file = bmpPath.let{ File(it) }
        if(bmp_file.exists() == true){
            Glide.with(this)
                .load(bmp_file)
                .apply(options)
                .thumbnail()
                .into(viewBinding.resultImageView)
        }

        //openCV to pointed
//        val goodFeaturesToTrack = GoodFeaturesToTrack(bmpPath)
//        val resultBmp = goodFeaturesToTrack.update()

        val findContours = FindContours(bmpPath)
        val resultBmp = findContours.update()

        val pointedImageView = viewBinding.pointedImageView

        pointedImageView.setImageBitmap(resultBmp)

        viewBinding.backBtn.setOnClickListener {
            val intent = Intent(this@ResultActivity, RawActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}