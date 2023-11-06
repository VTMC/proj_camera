package com.example.proj_camera

import Utils.RotateTransformation
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.proj_camera.databinding.ResultActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        lifecycleScope.launch(Dispatchers.Main){
            //openCV to pointed
//        val goodFeaturesToTrack = GoodFeaturesToTrack(bmpPath)
//        val resultBmp = goodFeaturesToTrack.update()

//            val findContours = FindContours(bmpPath)
//            val resultBmp = findContours.update()
//            val resultBmp2 = findContours.update2()
//            val croppedImgList = findContours.cropImgFileList

            val findContours2 = FindContours2(bmpPath)
            val resultBmp = findContours2.update()
            val resultBmp2 = findContours2.update2()
            val croppedImgList = findContours2.cropImgFileList

            val pointedImageView = viewBinding.pointedImageView

            pointedImageView.setImageBitmap(resultBmp)

            val pointedImageView2 = viewBinding.pointedImageView2

            pointedImageView2.setImageBitmap(resultBmp2)

            for(i in 0 until(croppedImgList.size)){
                Log.d("KSM", "croppedImg[${i+1}] = ${croppedImgList[i]}")
            }
        }

        viewBinding.backBtn.setOnClickListener {
            val intent = Intent(this@ResultActivity, RawActivity::class.java)
            startActivity(intent)
            finish()
        }

        viewBinding.pointedImageView.setOnClickListener {
            viewBinding.pointedImageView2.visibility = View.VISIBLE
        }

        viewBinding.pointedImageView2.setOnClickListener {
            viewBinding.pointedImageView2.visibility = View.INVISIBLE
        }
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}