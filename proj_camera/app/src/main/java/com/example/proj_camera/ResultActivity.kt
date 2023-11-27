package com.example.proj_camera

import Utils.RotateTransformation
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
    var test2 = intArrayOf(
        R.drawable.test2_1,
        R.drawable.test2_2,
        R.drawable.test2_3,
        R.drawable.test2_4,
        /*R.drawable.test2_5,
        R.drawable.test2_6,
        R.drawable.test2_7*/
    )
    var test3 =
        intArrayOf(R.drawable.test3_1, R.drawable.test3_2, R.drawable.test3_3, R.drawable.test3_4)
    var test4 = intArrayOf(
        R.drawable.test4_1,
        R.drawable.test4_2,
        R.drawable.test4_3,
        R.drawable.test4_4,
        R.drawable.test4_5,
        R.drawable.test4_6
    )
    var test5 = intArrayOf(
        R.drawable.test5_1,
        R.drawable.test5_2,
        R.drawable.test5_3,
        R.drawable.test5_4,
        R.drawable.test5_5
    )
    var test6 = intArrayOf(
        R.drawable.test6_1,
        R.drawable.test6_2,
        R.drawable.test6_3,
        R.drawable.test6_4,
        R.drawable.test6_5,
        R.drawable.test6_6
    )
    var test7 = intArrayOf(R.drawable.test7_1, R.drawable.test7_2)
    var test8 = intArrayOf(
        R.drawable.test8_1,
        R.drawable.test8_2,
        R.drawable.test8_3,
        R.drawable.test8_4,
        R.drawable.test8_5
    )
    var test9 = intArrayOf(
        R.drawable.test9_1,
        R.drawable.test9_2,
        R.drawable.test9_3,
        R.drawable.test9_4,
        R.drawable.test9_5,
        R.drawable.test9_6
    )
    var test10 = intArrayOf(
        R.drawable.test10_1,
        R.drawable.test10_2,
        R.drawable.test10_3,
        R.drawable.test10_4,
        R.drawable.test10_5,
        R.drawable.test10_6,
        R.drawable.test10_7
    )
    var test11 = intArrayOf(
        R.drawable.test11_1,
        R.drawable.test11_2,
        R.drawable.test11_3,
        R.drawable.test11_4,
        R.drawable.test11_5
    )

    private lateinit var viewBinding: ResultActivityBinding
    private var resultBmp : Bitmap? = null
    private var resultBmp2 : Bitmap? = null
    private var croppedCheckImg : Array<Bitmap>? = null
    private var suitabilityList : BooleanArray? = null
    private var croppedImgList : Array<String>? = null
    private var croppedImgRGB : Array<IntArray>? = null
    private var selectedRGB : IntArray? = null

    private lateinit var suitabilityTxtList:List<TextView>
    private lateinit var cropImgList:List<ImageView>
    private lateinit var cropCheckImgList:List<ImageView>
    private lateinit var imgRGBTxtList:List<TextView>

    private lateinit var testDrawableList : List<IntArray>
    private lateinit var selectedImgList : List <ImageView>

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        val progressDialog = ProgressDialog(this)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("이미지 처리중...")
        progressDialog.show()

        viewBinding = ResultActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        suitabilityTxtList = listOf(
            viewBinding.suitabilityTxtView1, viewBinding.suitabilityTxtView2, viewBinding.suitabilityTxtView3,
            viewBinding.suitabilityTxtView4, viewBinding.suitabilityTxtView5, viewBinding.suitabilityTxtView6,
            viewBinding.suitabilityTxtView7, viewBinding.suitabilityTxtView8, viewBinding.suitabilityTxtView9,
            viewBinding.suitabilityTxtView10, viewBinding.suitabilityTxtView11
        )
        cropImgList = listOf(
            viewBinding.cropImg1, viewBinding.cropImg2, viewBinding.cropImg3, viewBinding.cropImg4, viewBinding.cropImg5,
            viewBinding.cropImg6, viewBinding.cropImg7, viewBinding.cropImg8, viewBinding.cropImg9, viewBinding.cropImg10,
            viewBinding.cropImg11
        )
        cropCheckImgList = listOf(
            viewBinding.cropCheckImg1, viewBinding.cropCheckImg2, viewBinding.cropCheckImg3, viewBinding.cropCheckImg4,
            viewBinding.cropCheckImg5, viewBinding.cropCheckImg6, viewBinding.cropCheckImg7, viewBinding.cropCheckImg8,
            viewBinding.cropCheckImg9, viewBinding.cropCheckImg10, viewBinding.cropCheckImg11
        )
        imgRGBTxtList = listOf(
            viewBinding.img1RGBTxt, viewBinding.img2RGBTxt, viewBinding.img3RGBTxt, viewBinding.img4RGBTxt, viewBinding.img5RGBTxt,
            viewBinding.img6RGBTxt, viewBinding.img7RGBTxt, viewBinding.img8RGBTxt, viewBinding.img9RGBTxt, viewBinding.img10RGBTxt,
            viewBinding.img11RGBTxt
        )
        testDrawableList = listOf(
            test2, test3, test4, test5, test6, test7, test8, test9, test10, test11
        )
        selectedImgList = listOf(
            viewBinding.selectedImg1, viewBinding.selectedImg2, viewBinding.selectedImg3, viewBinding.selectedImg4,
            viewBinding.selectedImg5, viewBinding.selectedImg6, viewBinding.selectedImg7, viewBinding.selectedImg8,
            viewBinding.selectedImg9, viewBinding.selectedImg10, viewBinding.selectedImg11
        )

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

                    selectedRGB = selectRGB(croppedImgRGB!!)

                    val pointedImageView = viewBinding.pointedImageView

                    pointedImageView.setImageBitmap(resultBmp)

                    val pointedImageView2 = viewBinding.pointedImageView2

                    pointedImageView2.setImageBitmap(resultBmp2)

                    for(i in 0 until 11){
                        val cropBmp = BitmapFactory.decodeFile(croppedImgList!![i])

                        Log.d("KSM", "sqr[$i]'s suitability : ${suitabilityList!![i]}")

                        cropImgList[i].setImageBitmap(cropBmp)
                        cropCheckImgList[i].setImageBitmap(croppedCheckImg?.get(i) ?: null)

                        if(suitabilityList!![i] == true){
                            suitabilityTxtList[i].text = "OK"
                        }else{
                            suitabilityTxtList[i].text = "NO"

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
                        }

                        imgRGBTxtList[i].text = "R : ${croppedImgRGB!![i][0]}\nG : ${croppedImgRGB!![i][1]}\nB : ${croppedImgRGB!![i][2]}"

                        if(i != 0){
                            val resBmp = decodeResource(this@ResultActivity, selectedRGB!![i])
                            val res = Bitmap.createScaledBitmap(resBmp, 80, 64, true)

                            selectedImgList[i].setImageBitmap(res)
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

    fun selectRGB(cropSqrRGB : Array<IntArray>) : IntArray{
        var resArray = IntArray(11, {0})

        resArray[0] = 0 // The first Sqr have no meaning

        for(i in 1 until cropSqrRGB.size){
            val testArray = testDrawableList[i-1]

            var minDistance = 0.0
            var testArrayDis = DoubleArray(testArray.size, {0.0})

            //get min distance by RGB Point.
            for(j in testArray.indices){ //testArray.indices = 0 until testArray.size
                val bmp = decodeResource(this, testArray[j])
                val x = bmp.width/2
                val y = bmp.height/2

                val testRGB = getRGB(bmp, x, y)
                val distance = getDistance(cropSqrRGB[i], testRGB)

                if(j == 0){
                    minDistance = distance
                }else{
                    if(distance < minDistance){
                        minDistance = distance
                    }
                }

                testArrayDis[j] = distance
            }

            //get drawable which is min distance
            for(j in testArrayDis.indices){
                if(minDistance == testArrayDis[j]){
                    resArray[i] = testArray[j]
                }
            }
        }

        return resArray
    }

    private fun getRGB(bmp1 : Bitmap, x : Int, y : Int) : IntArray {

        val bmp1RGB = bmp1.getPixel(x, y)

        val a = Color.alpha(bmp1RGB)
        val r = Color.red(bmp1RGB)
        val g = Color.green(bmp1RGB)
        val b = Color.blue(bmp1RGB)

        val intArray = intArrayOf(r,g,b)

        return intArray
    }

    private fun getDistance(RGBIntArray1 : IntArray, RGBIntArray2 : IntArray) : Double{
        val R_dis = Math.pow((RGBIntArray1[0].toDouble() - RGBIntArray2[0].toDouble()),2.0)
        val G_dis = Math.pow((RGBIntArray1[1].toDouble() - RGBIntArray2[1].toDouble()),2.0)
        val B_dis = Math.pow((RGBIntArray1[2].toDouble() - RGBIntArray2[2].toDouble()),2.0)

        val RGB_dis = Math.sqrt(R_dis+G_dis+B_dis)

        return RGB_dis
    }

    fun decodeResource(context : Context, resourceId : Int): Bitmap{
        return BitmapFactory.decodeResource(context.resources, resourceId)
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}