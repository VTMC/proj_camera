package com.example.proj_camera

import Utils.RotateTransformation
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
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
    private lateinit var resultBmp : Bitmap
    private lateinit var resultBmp2 : Bitmap
    private lateinit var croppedImgList : Array<String>

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        val progressDialog = ProgressDialog(this)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("이미지 처리중...")
        progressDialog.show()

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

        lifecycleScope.launch(Dispatchers.Default){
            //openCV to pointed
//        val goodFeaturesToTrack = GoodFeaturesToTrack(bmpPath)
//        val resultBmp = goodFeaturesToTrack.update()

//            val findContours = FindContours(bmpPath)
//            val resultBmp = findContours.update()
//            val resultBmp2 = findContours.update2()
//            val croppedImgList = findContours.cropImgFileList

            val findContours2 = FindContours2(bmpPath)
            resultBmp = findContours2.update()
            resultBmp2 = findContours2.update2()
            croppedImgList = findContours2.cropImgFileList

            lifecycleScope.launch(Dispatchers.Main){
                val pointedImageView = viewBinding.pointedImageView

                pointedImageView.setImageBitmap(resultBmp)

                val pointedImageView2 = viewBinding.pointedImageView2

                pointedImageView2.setImageBitmap(resultBmp2)

                for(i in 0 until(croppedImgList!!.size)){
                    Log.d("KSM", "croppedImg[${i+1}] = ${croppedImgList!![i]}")
                }

                progressDialog.dismiss()
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