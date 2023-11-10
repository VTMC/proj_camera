package com.example.proj_camera

import Utils.RotateTransformation
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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
    private var resultBmp : Bitmap? = null
    private var resultBmp2 : Bitmap? = null
    private var croppedImgList : Array<String>? = null

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
            //existing method
//            val findContours2 = FindContours2(bmpPath)
//            resultBmp = findContours2.update()
//            if(resultBmp != null){
//                resultBmp2 = findContours2.update2()
//                croppedImgList = findContours2.cropImgFileList
//            }

            val findContours = FindContours(bmpPath)
            resultBmp = findContours.update()
            if(resultBmp != null){
                resultBmp2 = findContours.sqr
            }


            lifecycleScope.launch(Dispatchers.Main){
                val dialogBuilder = AlertDialog.Builder(this@ResultActivity)
                val intent = Intent(this@ResultActivity, RawActivity::class.java)

                if(resultBmp == null){
                    progressDialog.dismiss()

                    dialogBuilder.setTitle("이미지 처리 오류 발생 (1)")
                        .setMessage("이미지 처리가 제대로 되지 않았습니다. \n다시 촬영해주세요.")
                        .setIcon(com.google.android.material.R.drawable.ic_clear_black_24)
                        .setCancelable(false)
                        .setPositiveButton("확인",
                            DialogInterface.OnClickListener{ dialog, id ->
                                startActivity(intent)
                                finish()
                            })
                        .show()

                }else if(resultBmp2 == null){
                    progressDialog.dismiss()

                    dialogBuilder.setTitle("이미지 처리 오류 발생 (2)")
                        .setMessage("이미지 처리가 제대로 되지 않았습니다. \n다시 촬영해주세요.")
                        .setIcon(com.google.android.material.R.drawable.ic_clear_black_24)
                        .setCancelable(false)
                        .setPositiveButton("확인",
                            DialogInterface.OnClickListener{ dialog, id ->
                                startActivity(intent)
                                finish()
                            })
                        .show()
                }/*else if(croppedImgList == null){
                    progressDialog.dismiss()

                    dialogBuilder.setTitle("이미지 처리 오류 발생 (3)")
                        .setMessage("잘린 이미지를 로드할 수 없습니다. \n다시 촬영해주세요.")
                        .setIcon(com.google.android.material.R.drawable.ic_clear_black_24)
                        .setCancelable(false)
                        .setPositiveButton("확인",
                            DialogInterface.OnClickListener{ dialog, id ->
                                startActivity(intent)
                                finish()
                            })
                        .show()
                }*/else{
                    val pointedImageView = viewBinding.pointedImageView

                    pointedImageView.setImageBitmap(resultBmp)

                    val pointedImageView2 = viewBinding.pointedImageView2

                    pointedImageView2.setImageBitmap(resultBmp2)

//                    for(i in 0 until(croppedImgList!!.size)){
//                        Log.d("KSM", "croppedImg[${i+1}] = ${croppedImgList!![i]}")
//                    }

                    progressDialog.dismiss()
                }


            }
        }

        viewBinding.backBtn.setOnClickListener {
            val intent = Intent(this@ResultActivity, RawActivity::class.java)
            startActivity(intent)
            finish()
        }

        viewBinding.pointedImageView.setOnClickListener {
            viewBinding.pointedImageView.visibility = View.INVISIBLE
            viewBinding.pointedImageView2.visibility = View.VISIBLE
        }

        viewBinding.pointedImageView2.setOnClickListener {
            viewBinding.pointedImageView.visibility = View.VISIBLE
            viewBinding.pointedImageView2.visibility = View.INVISIBLE
        }
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}