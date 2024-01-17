package com.example.proj_camera

import Utils.RotateTransformation
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.proj_camera.databinding.ResultActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

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
    private var selectedRGB : Array<IntArray>? = null

    private lateinit var suitabilityTxtList:List<TextView>
    private lateinit var cropImgList:List<ImageView>
    private lateinit var cropCheckImgList:List<ImageView>
    private lateinit var imgRGBTxtList:List<TextView>

    private lateinit var testDrawableList : List<IntArray>
    private lateinit var RGBtoBitmap : List<ImageView>
    private lateinit var selectedImgList : List <ImageView>

    //show logText
    private lateinit var logText : String

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        val progressDialog = ProgressDialog(this)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("이미지 처리중...")
        progressDialog.show()

        //display info
        val smartphoneDisplay = windowManager.defaultDisplay
        val displaySize = Point()
        smartphoneDisplay.getRealSize(displaySize)
        val displayWidth = displaySize.x

        logText = "========== ResultActivity.kt LOG START ==========\n"

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
        RGBtoBitmap = listOf(
            viewBinding.RGBtoBitmap1, viewBinding.RGBtoBitmap2, viewBinding.RGBtoBitmap3, viewBinding.RGBtoBitmap4, viewBinding.RGBtoBitmap5,
            viewBinding.RGBtoBitmap6, viewBinding.RGBtoBitmap7, viewBinding.RGBtoBitmap8, viewBinding.RGBtoBitmap9, viewBinding.RGBtoBitmap10,
            viewBinding.RGBtoBitmap11
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
        val afState = intent.getStringExtra("stringAfState")

        //add LogText String
        logText += "----- 1. capture Info -----\n"
        logText += "dngPath : ${dngPath}\nbmpPath : ${bmpPath}\naccX : ${accX}\naccY : $accY\naccZ : $accZ\nangleXZ : $angleXZ\nangleYZ : $angleYZ\nafState : $afState\n"
        logText += "---------------------------\n\n"

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
        viewBinding.afStateTxtView.text = "AF state : ${afState}"

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

                    val sqr_h = findContours.sqr_h_return
                    val fbh = findContours.fbh_return
                    val bh = findContours.bh_return

                    viewBinding.sqrHTxtView.text = "sqr_h : ${String.format("%.1f", sqr_h)}"
                    viewBinding.fbhTxtView.text = "fbh : ${String.format("%.1f", fbh)}"
                    viewBinding.bhTxtView.text = "bh : ${String.format("%.1f", bh)}"

                    //add LogText String
                    logText += "----- 2. Image process Info -----\n" //LOGTEXT
                    logText += "sqr_h : $sqr_h\nfbh : $fbh\nbh : $bh\n" //LOGTEXT
                    logText += "---------------------------\n\n" //LOGTEXT
                    logText += "----- 3. RGB Distance Log -----\n" //LOGTEXT

                    selectedRGB = selectRGB(croppedImgRGB!!)

                    val pointedImageView = viewBinding.pointedImageView

                    pointedImageView.setImageBitmap(resultBmp)

                    val pointedImageView2 = viewBinding.pointedImageView2

                    pointedImageView2.setImageBitmap(resultBmp2)

                    for (i in selectedRGB!!.indices) {
                        Log.d(
                            "KSM",
                            "selectedRGB[$i] | ${selectedRGB!![i][0]} | ${selectedRGB!![i][1]}"
                        )
                        logText += "selectedRGB[$i] | ${selectedRGB!![i][0]} | ${selectedRGB!![i][1]}\n" //LOGTEXT
                    }

                    //add LogText String
                    logText += "---------------------------\n\n" //LOGTEXT
                    logText += "----- 4. Suitability Info -----\n" //LOGTEXT


                    for(i in 0 until 11){
                        val cropBmp = BitmapFactory.decodeFile(croppedImgList!![i])

                        Log.d("KSM", "sqr[$i]'s suitability : ${suitabilityList!![i]}")
                        logText += "sqr[$i]'s suitability : ${suitabilityList!![i]}\n" //LOGTEXT

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
                        logText += "---------------------------\n\n" //LOGTEXT

                        imgRGBTxtList[i].text = "R : ${croppedImgRGB!![i][0]}\nG : ${croppedImgRGB!![i][1]}\nB : ${croppedImgRGB!![i][2]}"

                        val avgRGBbmp = Bitmap.createBitmap(cropBmp.width, cropBmp.height, Bitmap.Config.ARGB_8888)
                        val avgRGBcanvas = Canvas(avgRGBbmp)
                        val paint = Paint()
                        paint.color = Color.rgb(croppedImgRGB!![i][0], croppedImgRGB!![i][1], croppedImgRGB!![i][2])
                        avgRGBcanvas.drawRect(0f, 0f, cropBmp.width.toFloat(), cropBmp.height.toFloat(), paint)

                        RGBtoBitmap[i].setImageBitmap(avgRGBbmp)

                        if(i != 0){
                            val resBmp = decodeResource(this@ResultActivity, selectedRGB!![i][0])
                            val res = Bitmap.createScaledBitmap(resBmp, 80, 64, true)

                            selectedImgList[i].setImageBitmap(res)
                        }

                        viewBinding.logText.text = logText
                        if(displayWidth < 2000){
                            viewBinding.logText.textSize = 10F
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

    fun selectRGB(cropSqrRGB : Array<IntArray>) : Array<IntArray>{
        val resArray = Array(11) { IntArray(2) { 0 } }
        val testArray = testDrawableList

        // distance list of diffrence between testRGB with startPointRGB
        val testArrayDis: MutableList<DoubleArray> = mutableListOf()
        val minDistance = DoubleArray(cropSqrRGB.size - 1) { 0.0 }
//        resArray[0] = 0 // The first Sqr have no meaning

        for(i in 1 until cropSqrRGB.size) {
            val innerTestArrayDis = DoubleArray(testArray[i - 1].size) { 0.0 }

            Log.d("KSM", "=== CropSqr[$i] ===")
            logText += "=== CropSqr[$i] ===\n" //LOGTEXT

            //get min distance by RGB Point.
            for (j in testArray[i-1].indices) { //testArray.indices = 0 until testArray.size
                val bmp = decodeResource(this, testArray[i-1][j])
                val x = bmp.width / 2
                val y = bmp.height / 2

                val testRGB = getRGB(bmp, x, y)

                Log.d("KSM", "=== testSqr[$j] ===")
                logText += "=== testSqr[$j] ===\n" //LOGTEXT
                val distance = getDistance(cropSqrRGB[i], testRGB)

                if (j == 0) {
                    minDistance[i - 1] = distance
                } else {
                    if (distance < minDistance[i - 1]) {
                        minDistance[i - 1] = distance
                    }
                }

                innerTestArrayDis[j] = distance
            }

            testArrayDis.add(innerTestArrayDis)
        }

        for(i in 0 until cropSqrRGB.size - 1){
            //get drawable which is min distance
            for(j in testArrayDis[i].indices){
                Log.d(
                    "KSM",
                    "Test$i-$j | minDistance : ${minDistance[i]} | testDistance : ${testArrayDis[i][j]}"
                )
                logText += "Test$i-$j | minDistance : ${minDistance[i]} | testDistance : ${testArrayDis[i][j]}\n" //LOGTEXT

                if(minDistance[i] == testArrayDis[i][j]){
                    resArray[i+1][0] = testArray[i][j]
                    resArray[i+1][1] = j
                }
            }
            Log.d("KSM", "-----------------------------------")
            logText += "-----------------------------------\n" //LOGTEXT
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

    //CIE-Labs_GetDistance()
    private fun getDistance(RGBIntArray1: IntArray, RGBIntArray2: IntArray): Double {
        val doubleLab1:DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
        val doubleLab2:DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
        ColorUtils.RGBToLAB(RGBIntArray1[0], RGBIntArray1[1], RGBIntArray1[2], doubleLab1)
        ColorUtils.RGBToLAB(RGBIntArray2[0], RGBIntArray2[1], RGBIntArray2[2], doubleLab2)

        Log.d("KSM", "RGB1_RGB : [R : ${RGBIntArray1[0]} / G : ${RGBIntArray1[1]} / B : ${RGBIntArray1[2]}]")
        logText += "RGB1_RGB : [R : ${RGBIntArray1[0]} / G : ${RGBIntArray1[1]} / B : ${RGBIntArray1[2]}]\n" //LOGTEXT
        Log.d("KSM", "RGB2_RGB : [R : ${RGBIntArray2[0]} / G : ${RGBIntArray2[1]} / B : ${RGBIntArray2[2]}]")
        logText += "RGB2_RGB : [R : ${RGBIntArray2[0]} / G : ${RGBIntArray2[1]} / B : ${RGBIntArray2[2]}]\n" //LOGTEXT
        Log.d("KSM", "RGB1_Lab : [L : ${doubleLab1[0]} / a : ${doubleLab1[1]} / b : ${doubleLab1[2]}]")
        logText += "RGB1_Lab : [L : ${doubleLab1[0]} / a : ${doubleLab1[1]} / b : ${doubleLab1[2]}]\n" //LOGTEXT
        Log.d("KSM", "RGB2_Lab : [L : ${doubleLab2[0]} / a : ${doubleLab2[1]} / b : ${doubleLab2[2]}]")
        logText += "RGB2_Lab : [L : ${doubleLab2[0]} / a : ${doubleLab2[1]} / b : ${doubleLab2[2]}]\n" //LOGTEXT

        val L_dis = (doubleLab1[0] - doubleLab2[0]).pow(2.0)
        val A_dis = (doubleLab1[1] - doubleLab2[1]).pow(2.0)
        val B_dis = (doubleLab1[2] - doubleLab2[2]).pow(2.0)

        val LAB_dis = sqrt(L_dis + A_dis + B_dis)

        return LAB_dis
    }

    //RGB_GetDistance()
//    private fun getDistance(RGBIntArray1 : IntArray, RGBIntArray2 : IntArray) : Double{
//        val R_dis = Math.pow((RGBIntArray1[0].toDouble() - RGBIntArray2[0].toDouble()),2.0)
//        val G_dis = Math.pow((RGBIntArray1[1].toDouble() - RGBIntArray2[1].toDouble()),2.0)
//        val B_dis = Math.pow((RGBIntArray1[2].toDouble() - RGBIntArray2[2].toDouble()),2.0)
//
//        val RGB_dis = Math.sqrt(R_dis+G_dis+B_dis)
//
//        return RGB_dis
//    }

    fun decodeResource(context : Context, resourceId : Int): Bitmap{
        return BitmapFactory.decodeResource(context.resources, resourceId)
    }

    companion object{
        init{
            System.loadLibrary("opencv_java4")
        }
    }
}