package com.example.proj_camera

import Utils.RotateTransformation
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    private var resultBmp : Bitmap? = null
    private var resultBmp2 : Bitmap? = null
    private var croppedCheckImg : Array<Bitmap>? = null
    private var suitabilityList : BooleanArray? = null
    private var croppedImgList : Array<String>? = null
    private var croppedImgRGB : Array<DoubleArray>? = null

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
        val accX = intent.getStringExtra("accX")
        val accY = intent.getStringExtra("accY")
        val accZ = intent.getStringExtra("accZ")
        val angleXZ = intent.getStringExtra("angleXZ")
        val angleYZ = intent.getStringExtra("angleYZ")

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

        viewBinding.accXTextView.text = "accX : ${accX}"
        viewBinding.accYTextView.text = "accY : ${accY}"
        viewBinding.accZTextView.text = "accZ : ${accZ}"
        viewBinding.angleXZTextView.text = "angleXZ : ${angleXZ}"
        viewBinding.angleYZTextView.text = "angleYZ : ${angleYZ}"

        lifecycleScope.launch(Dispatchers.Default){
            val dialogBuilder = AlertDialog.Builder(this@ResultActivity)
            val intent = Intent(this@ResultActivity, RawActivity::class.java)

            val findContours = FindContours(bmpPath)
            try{
                resultBmp = findContours.update()
                if(resultBmp != null){
                    resultBmp2 = findContours.sqr
                }

                if(resultBmp2 != null){
                    croppedCheckImg = findContours.checkCropImg()
                    suitabilityList = findContours.getSuitabilityList()
                    croppedImgList = findContours.getCropImgFileList()
                    croppedImgRGB = findContours.getCropImgRGBList()
                }
            }catch(e : Exception){
                Log.e("KSM", "findContour Error!", e)
            }

            lifecycleScope.launch(Dispatchers.Main){
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
                }else if(croppedImgList == null){
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
                }else{
                    //check sqr for retake picture
//                    for(i in 1 until 11){
//                        if(suitabilityList!![i] == false){
//                            progressDialog.dismiss()
//
//                            dialogBuilder.setTitle("이미지 처리 오류 발생 (5-${i})")
//                                .setMessage("UrineStrip 내 사각형이 제대로 이미지 처리가 안 됐습니다.\n다시 촬영해주세요.")
//                                .setIcon(com.google.android.material.R.drawable.ic_clear_black_24)
//                                .setCancelable(false)
//                                .setPositiveButton("확인",
//                                    DialogInterface.OnClickListener { dialog, id ->
//                                        startActivity(intent)
//                                        finish()
//                                    })
//                                .show()
//                            break
//                        }
//                    }

                    val pointedImageView = viewBinding.pointedImageView

                    pointedImageView.setImageBitmap(resultBmp)

                    val pointedImageView2 = viewBinding.pointedImageView2

                    pointedImageView2.setImageBitmap(resultBmp2)

                    for(i in 0 until 11){
                        val cropBmp = BitmapFactory.decodeFile(croppedImgList!![i])

                        Log.d("KSM", "sqr[$i]'s suitability : ${suitabilityList!![i]}")

                        when(i){
                            0 -> {
                                val cropImageView = viewBinding.cropImg1
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg1
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView1
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img1RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            1 -> {
                                val cropImageView = viewBinding.cropImg2
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg2
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView2
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img2RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            2 -> {
                                val cropImageView = viewBinding.cropImg3
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg3
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView3
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img3RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            3 -> {
                                val cropImageView = viewBinding.cropImg4
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg4
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView4
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img4RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            4 -> {
                                val cropImageView = viewBinding.cropImg5
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg5
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView5
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img5RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            5 -> {
                                val cropImageView = viewBinding.cropImg6
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg6
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView6
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img6RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            6 -> {
                                val cropImageView = viewBinding.cropImg7
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg7
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView7
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img7RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            7 -> {
                                val cropImageView = viewBinding.cropImg8
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg8
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView8
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img8RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            8 -> {
                                val cropImageView = viewBinding.cropImg9
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg9
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView9
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img9RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            9 -> {
                                val cropImageView = viewBinding.cropImg10
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg10
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView10
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img10RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                            10 -> {
                                val cropImageView = viewBinding.cropImg11
                                cropImageView.setImageBitmap(cropBmp)

                                val cropCheckImageView = viewBinding.cropCheckImg11
                                cropCheckImageView.setImageBitmap(croppedCheckImg?.get(i) ?: null)

                                val suitabilityTxtView = viewBinding.suitabilityTxtView11
                                if(suitabilityList!![i] == true){
                                    suitabilityTxtView.text = "OK"
                                }else{
                                    suitabilityTxtView.text = "NO"
                                }

                                viewBinding.img11RGBTxt.text = "R : ${croppedImgRGB!![i][0].toInt()}\nG : ${croppedImgRGB!![i][1].toInt()}\nB : ${croppedImgRGB!![i][2].toInt()}"
                            }
                        }
                    }

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
//            viewBinding.pointedImageView3.visibility = View.VISIBLE
        }

        viewBinding.pointedImageView3.setOnClickListener {
            viewBinding.pointedImageView.visibility = View.VISIBLE
            viewBinding.pointedImageView3.visibility = View.INVISIBLE
        }
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}