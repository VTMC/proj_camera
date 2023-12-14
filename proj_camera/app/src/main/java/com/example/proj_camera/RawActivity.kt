package com.example.proj_camera

import Utils.AndroidBmpUtil
import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.computeExifOrientation
import com.example.android.camera.utils.decodeExifOrientation
import com.example.android.camera.utils.getPreviewOutputSize
import com.example.proj_camera.databinding.RawActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.beyka.tiffbitmapfactory.TiffBitmapFactory
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import kotlin.concurrent.timer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max


class RawActivity : AppCompatActivity(), SensorEventListener{
    external fun simple_dcraw(argv: Array<String>, toPath: String) : Int

    external fun dngToTiff(fromPath : String, toPath : String) : Int

    external fun unprocessed_raw(argv: Array<String>, toPath: String) : Int

    external fun getRGB(x : Int, y : Int, path : String) : IntArray

    private lateinit var viewBinding: RawActivityBinding

    private lateinit var cameraExecutor: ExecutorService

    //camera2
    //카메라 장치(모든 카메라 작업에 사용)를 검색, 특성화 및 연결합니다
    private var camera2 : CameraDevice ?= null
    private var cameraId : String ?= null
    private var rawCameraInfo : FormatItem ?= null

    private val cameraManager: CameraManager by lazy {
        this.getSystemService(CAMERA_SERVICE) as CameraManager
    }

    private var characteristics : CameraCharacteristics ?= null

//    private lateinit var outputDirectory: File

    /** Readers used as buffers for camera still shots */
    private lateinit var imageReader: ImageReader

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply{ start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    //당사 매개 변수로 구성된 진행 중인 [Camera Capture Session]에 대한 내부 참조
    private lateinit var session: CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera
     *  > 카메라와 관련된 장치 방향 변경에 대한 라이브 데이터 수신기
     * */
    private lateinit var relativeOrientation: OrientationLiveData

    //outputUri를 저장할 변수
    private var outputUri : Uri ?= null

    //previewSize를 저장
    private lateinit var previewSize: Size

    //Torch상태를 저장
    private var torchState: Boolean = false

    /** Default Bitmap decoding options */
    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        // Keep Bitmaps at less than 1 MP
        if (max(outHeight, outWidth) > DOWNSAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWNSAMPLE_SIZE + 1
            val scaleFactorY = outHeight / DOWNSAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactorY)
        }
    }

    /** Bitmap transformation derived from passed arguments */
    private val bitmapTransformation: Matrix by lazy { decodeExifOrientation(relativeOrientation.value?: 0) }

    //for crop by borderRect
    private lateinit var borderRect : Rect

    private lateinit var dng_str : String
    private lateinit var jpg_str : String
    private lateinit var bmp_str : String

    //ZoomRatio 초기 변수
//    private var zoomRatio: Float = ZoomUtil().minZoom()
//
//    //ZoomGesture 변수
//    private val zoomGetstureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
//            val captureRequest = setCaptureRequest()
//
//            captureRequest.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
//
//            session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
//        }
//    }

    private lateinit var sensorManager : SensorManager
    private var accelerometerSensor: Sensor? = null
    private var accX : Double = 0.0
    private var accY : Double = 0.0
    private var accZ : Double = 0.0
    private var angleXZ : Double = 0.0
    private var angleYZ : Double = 0.0

    //set waitingTimer
    private var timer : Timer?= null
    private var progress = 0

    //check AF state
    private lateinit var stringAfState : String
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)

            //check AF
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            //change CaptureBtn to visible Handler
            val mainHandler = Handler(Looper.getMainLooper())

            when(afState){
                null -> {
//                    Log.d("KSM", "Auto-focus state: null")
                    stringAfState = "null"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
                CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
//                    Log.d("KSM", "Auto-focus state: Inactive")
                    stringAfState = "inActive"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
                CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN ->{
//                    Log.d("KSM", "Auto-focus state: Passive Scan")
                    stringAfState = "passiveScan"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
                CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> { //초점 잡힌 상태
//                    Log.d("KSM", "Auto-focus state: Passive Focused")
                    stringAfState = "passiveFocused"
                    mainHandler.post{
                        if(accX > -1 && accX < 1){
                            if(accY > -1 && accY < 2){
                                if(accZ >= 9 && accZ < 11){
                                    viewBinding.waitingLayout.visibility = View.VISIBLE

                                    val waitingBar = viewBinding.waitingBar

                                    if(timer != null){
                                        timer!!.cancel()
                                    }

                                    timer = timer(period = 10){
                                        progress++
                                        waitingBar.progress = progress
                                        if(progress == 200){ //200 * 10 = 2000ms = 2s
                                            this@timer.cancel()
                                            mainHandler.post{
                                                viewBinding.waitingLayout.visibility = View.GONE
                                                viewBinding.imageCaptureBtn.visibility = View.VISIBLE
                                            }
                                        }
                                    }
                                }else{
                                    viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                                    viewBinding.waitingLayout.visibility = View.GONE
                                }
                            }else{
                                viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                                viewBinding.waitingLayout.visibility = View.GONE
                            }
                        }else{
                            viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                            viewBinding.waitingLayout.visibility = View.GONE
                        }
                    }
                }
                CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> {
//                    Log.d("KSM", "Auto-focus state: Passive Unfocused")
                    stringAfState = "passiveUnfocused"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> {
//                    Log.d("KSM", "Auto-focus state: Active Scan")
                    stringAfState = "activeScan"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> { //초점 잡힌 상태
//                    Log.d("KSM", "Auto-focus state: Focused Locked")
                    stringAfState = "focusedLocked"
                    progress = 0
//                    mainHandler.post{
//                        if(accX > -1.0 && accX < 1.0){
//                            if(accY > -1.0 && accY < 2.0){
//                                if(accZ >= 9.0 && accZ < 11.0){
//                                    viewBinding.imageCaptureBtn.visibility = View.VISIBLE
//                                }else{
//                                    viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
//                                }
//                            }else{
//                                viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
//                            }
//                        }else{
//                            viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
//                        }
//                    }
                }
                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> {
//                    Log.d("KSM", "Auto-focus state: Not Focused Locked")
                    stringAfState = "notFocusedLocked"
                    progress = 0
                    mainHandler.post{
                        viewBinding.waitingLayout.visibility = View.GONE
                        viewBinding.imageCaptureBtn.visibility = View.INVISIBLE
                    }
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        viewBinding = RawActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //현재 실행중인 카메라 리스트 확인 및 cameraId, pixelFormat 저장
        cameraId = getCameraId(this@RawActivity, CameraSelector.LENS_FACING_BACK)

        val cameraList = enumerateCameras(cameraManager)

        //제공된 카메라 ID에 해당하는 '카메라 특성'을 불러오는 characteristics
        characteristics = cameraManager.getCameraCharacteristics(cameraId!!)

        Log.d("KSM", cameraList.toString())

        for(i in cameraList){
            if(i.cameraId == cameraId){
                Log.d("KSM", i.toString())
                if(i.format == ImageFormat.RAW_SENSOR){
                    rawCameraInfo = i
                    Log.d("KSM", "RAW EXIST!!!!")
                }else{
                    onPause()
                }
            }
        }

         viewBinding.rawViewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int) = Unit

            @RequiresApi(Build.VERSION_CODES.O)
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Selects appropriate preview size and configures view finder
                previewSize = getPreviewOutputSize(
                    viewBinding.rawViewFinder.display,
                    characteristics!!,
                    SurfaceHolder::class.java
                )

                Log.d("KSM", "View finder size: ${viewBinding.rawViewFinder.width} x ${viewBinding.rawViewFinder.height}")
                Log.d("KSM", "Selected preview size: $previewSize")
                viewBinding.rawViewFinder.setAspectRatio(
                    previewSize!!.width,
                    previewSize!!.height
                )

                if(allPermissionGranted()){
                    startCamera()
                }else{
                    ActivityCompat.requestPermissions(this@RawActivity,
                        REQUIRED_PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS
                    )
                }
            }
        })

        //orientation값을 실시간으로 설정
        relativeOrientation = OrientationLiveData(this, characteristics!!).apply{
            observe(this@RawActivity, Observer { orientation ->
                Log.d("KSM", "Orientation changed $orientation ")
//                characteristics.Key(CaptureResult.LENS_POSE_ROTATION, relativeOrientation)
                Log.d("KSM", "Camera Characteristics ORIENTATION : ${characteristics!![CameraCharacteristics.SENSOR_ORIENTATION]}")
            })
        }

        //Torch 버튼 설정.
        viewBinding.torchBtn.setOnClickListener{
            Log.d("KSM", "Torch Pressed!!")
            viewBinding.torchBtn.isEnabled = true
            when(torchState){
                false -> {
                    Log.d("KSM", "Torch On")
                    try{
                        val captureRequest = setCaptureRequest()

                        captureRequest.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)

                        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                    }catch(e: CameraAccessException){
                        Log.e("KSM", "Torch Error", e)
                    }

                    viewBinding.torchBtn.background = ContextCompat.getDrawable(this@RawActivity, R.drawable.roundcorner_clicked)
                    torchState = true
                }
                true -> {
                    Log.d("KSM", "Torch Off")
                    try{
                        val captureRequest = setCaptureRequest()

                        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                    }catch(e: CameraAccessException){
                        Log.e("KSM", "Torch Error", e)
                    }

                    viewBinding.torchBtn.background = ContextCompat.getDrawable(this@RawActivity, R.drawable.roundcorner)
                    torchState = false
                }
            }
        }

        try{
            //Normal 카메라 전환 버튼
            viewBinding.changeNormalBtn.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }catch(exc: Exception){
            Log.e("KSM", "ChangeNormalBtn Intent Error!!", exc)
        }
    }

    //카메라가 시작되면 실행하는 함수
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startCamera() = lifecycleScope.launch(Dispatchers.Main){
        camera2 = openCamera(cameraManager, rawCameraInfo!!.cameraId, cameraHandler)

        Log.d("KSM", "CameraActivated")

        val size = characteristics!!.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(rawCameraInfo!!.format).maxByOrNull{it.height*it.width}!!

        Log.d("KSM", "Size : ${size}")

        imageReader = ImageReader.newInstance(
            size.width, size.height, rawCameraInfo!!.format, IMAGE_BUFFER_SIZE)

        Log.d("KSM", "ImageReader size : ${imageReader.width} * ${imageReader.height}")

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(viewBinding.rawViewFinder.holder.surface , imageReader.surface)

        session = createCaptureSession(camera2!!, targets, cameraHandler)

        val captureRequest = camera2!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply{
                addTarget(viewBinding.rawViewFinder.holder.surface)
            }

        session.setRepeatingRequest(captureRequest.build(), mCaptureCallback, cameraHandler)

        //캡처버튼을 클릭했을 경우
        viewBinding.imageCaptureBtn.setOnClickListener{
            it.isEnabled = false
            val captureAccX = accX.toInt().toString()
            val captureAccY = accY.toInt().toString()
            val captureAccZ = accZ.toInt().toString()
            val captureAngleXZ = angleXZ.toInt().toString()
            val captureAngleYZ = angleYZ.toInt().toString()

            Log.d("KSM", "CaptureBtn Clicked!!")

            //capture animation
            viewBinding.flashView.visibility = View.VISIBLE
            val flashAni = AnimationUtils.loadAnimation(this@RawActivity,R.anim.alpha_anim)
            viewBinding.flashView.startAnimation(flashAni)
            Handler().postDelayed({
                viewBinding.flashView.visibility = View.GONE }, 500)

            lifecycleScope.launch(Dispatchers.IO) {
                takePhoto().use { result ->
                    Log.d("KSM", "Result received: $result")

                    Log.d("KSM", "Result image size : ${result.image.width} x ${result.image.height}")

                    //try to crop
//                    val resultImage = result.image
//                    val res_plane = resultImage.planes
//                    val crop_top = viewBinding.borderView.top
//                    val crop_left = viewBinding.borderView.left
//                    val crop_w = viewBinding.borderView.width
//                    val crop_h = viewBinding.borderView.height
//                    val crop_buf = ByteBuffer.allocateDirect(crop_w*crop_h)
//
//                    res_plane[0].buffer.position(crop_top * resultImage.width + crop_left)
//                    res_plane[0].buffer.limit((crop_top + crop_h) * resultImage.width + crop_left + crop_w)
//                    crop_buf.put(res_plane[0].buffer)

                    Log.i("KSM", "== save DNG file START! ==")
                    var timeStart = System.currentTimeMillis()

                    outputUri = saveResult(result)

                    var timeEnd = System.currentTimeMillis()
                    var takeTime = timeEnd - timeStart
                    Log.i("KSM", "== save DNG file END! ==")
                    Log.i("KSM", "== IT Takes "+takeTime+"ms ==")
//                    Log.d("KSM", "Image saved: ${outputUri.toString()}") //- 이전의 흔적 (이미지 저장)

                    //Path를 얻기 위한 과정
                    val cursor = contentResolver.query(outputUri!!, null, null, null, null)
                    cursor!!.moveToNext()
                    val pathName = cursor.getString(cursor.getColumnIndex("_data") ?: 0)
                    val dng_file = File(pathName)
                    val path = dng_file.parent

                    //for get dng's width, height
                    val dngWHBmp = BitmapFactory.decodeFile(pathName)

                    Log.d("KSM", "outputDirectory pathName : ${pathName}")
                    Log.d("KSM", "outputDirectory path : ${path}")

                    val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA).format(System.currentTimeMillis())

                    val bmp_fileName = "/BMP_$timestamp.bmp"
                    val tiffbmp_convertName = "/TIFFBMP_$timestamp.bmp"
                    val bmp_path = path+bmp_fileName
                    val tiffbmp_path = path+tiffbmp_convertName
                    val bmp_file = File(path+bmp_fileName)

                    val tiff_fileName = "/TIFF_$timestamp"
                    val tiff_path = path+tiff_fileName

                    Log.d("KSM", "tiff path in Kotlin : ${tiff_path}")

                    lifecycleScope.launch(Dispatchers.Main){
                        //libraw
//                        Log.d("KSM", "TESTING --- ${lib()}")

                        Log.d("KSM", "DNG Size (w*h) : ${dngWHBmp.width}, ${dngWHBmp.height}")
//                        Log.d("KSM", "DNG Size[H,W] : ${dngWHBmp.height}, ${dngWHBmp.width}")

                        val rotatedWidth = viewBinding.borderView.height
                        val rotatedHeight = viewBinding.borderView.width
                        val rotatedTop = viewBinding.borderView.left
                        val rotatedLeft = viewBinding.borderView.top

                        //get ScaleRatio width, height by (result.image => horizontal/previewSize => vertical)
//                        val scaleRatio_w = dngWHBmp.width.toFloat() / previewSize.height.toFloat()
//                        val scaleRatio_h = dngWHBmp.height.toFloat() / previewSize.width.toFloat()
                        val scaleRatio_w = dngWHBmp.width.toDouble() / viewBinding.rawViewFinder.height.toDouble()
                        val scaleRatio_h = dngWHBmp.height.toDouble() / viewBinding.rawViewFinder.width.toDouble()
                        Log.d("KSM", "scaleRatio[w, h] = ${scaleRatio_w}, ${scaleRatio_h}")

                        //set borderView size to result.image size
                        val borderLeft = (rotatedLeft * scaleRatio_w).toInt()
                        val borderTop = (rotatedTop * scaleRatio_h).toInt()
                        val borderWidth = (rotatedWidth * scaleRatio_w).toInt()
                        val borderHeight = (rotatedHeight * scaleRatio_h).toInt()

                        Log.d("KSM", "origin border[l,t,w,h] = " +
                                "${viewBinding.borderView.left}, " +
                                "${viewBinding.borderView.top}, " +
                                "${viewBinding.borderView.height}, " +
                                "${viewBinding.borderView.width}")
                        Log.d("KSM", "rotated border[l,t,w,h] = " +
                                "${rotatedLeft}, ${rotatedTop}, " +
                                "${rotatedWidth}, ${rotatedHeight}")
                        Log.d("KSM", "border[l,t,w,h] = ${borderLeft}, ${borderTop}, ${borderWidth}, ${borderHeight}")

                        Log.i("KSM", "== DNG to TIFF START! ==")
                        var timeStart = System.currentTimeMillis()

                        val ac_str = if(borderLeft != 0 || borderTop != 0 || borderWidth != 0 || borderHeight != 0){
                            arrayOf("-v", "-T", "-B", "${borderLeft}",
                                "${borderTop}", "${borderWidth}", "${borderHeight}", "${pathName}")
                        }else{
                            arrayOf("-v", "-T", "${pathName}")
                        }
                        val resultTiff = simple_dcraw(ac_str, tiff_path)
//                        val resultTiff = simple_dcraw2(ac_str)
//                        val resultTiff = unprocessed_raw(ac_str, tiff_path)
//                        val resultTiff = dngToTiff(pathName, tiff_path)
//                        val resultBitmap = dngToBitmap(pathName)
                        var timeEnd = System.currentTimeMillis()
                        Log.d("KSM", "resultTiff Result : ${resultTiff}")

                        var takeTime = timeEnd - timeStart
                        Log.i("KSM", "== DNG to TIFF END! ==")
                        Log.i("KSM", "== IT Takes "+takeTime+"ms ==")

//                        val tiffBmp = TiffConverter.convertTiffBmp(tiff_path+".tiff", tiffbmp_path, null, null)
//
//                        if(tiffBmp){
//                            Log.d("KSM", "tiff convert to bmp!!")
//                        }else{
//                            Log.d("KSM", "tiff convert to bmp failed!!")
//                        }

                        Log.i("KSM", "== TIFF to BMP START! ==")
                        timeStart = System.currentTimeMillis()
                        val a_bmp = TiffBitmapFactory.decodeFile(File(tiff_path+".tiff"))
                        Log.d("KSM", "tiff size (w*h) : ${a_bmp.width}*${a_bmp.height}")

                        var pixel_x = (a_bmp.width) / 2
                        var pixel_y = (a_bmp.height) / 2

                        /*val a_bmpRGB = getRGB(a_bmp, pixel_x, pixel_y)
                        Log.d("KSM", "tiff to androidBitmap R/G/B : Pos[${pixel_x},${pixel_y}] = ${a_bmpRGB[0]}/${a_bmpRGB[1]}/${a_bmpRGB[2]}")*/

                        val saveBmp = AndroidBmpUtil.save(a_bmp, bmp_path)
                        timeEnd = System.currentTimeMillis()

                        if(saveBmp){
                            var takeTime = timeEnd - timeStart
                            Log.i("KSM", "== TIFF to BMP END! ==")
                            Log.i("KSM", "== IT Takes "+takeTime+"ms ==")
                            Log.d("KSM", "AndroidBmpUtil Bitmap saved bmp!!")
                        }else{
                            Log.i("KSM", "== TIFF to BMP END! ==")
                            Log.i("KSM", "== IT Takes "+takeTime+"ms ==")
                            Log.d("KSM", "AndroidBmpUtil Bitmap saved bmp failed!!")
                        }

                        val dng_bitmap = BitmapFactory.decodeFile(pathName)
                        pixel_x = (dng_bitmap.width) / 2
                        pixel_y = (dng_bitmap.height) / 2
                        val dng_bitmap_RGB = getRGB(dng_bitmap, pixel_x, pixel_y)
                        Log.d("KSM", "dngToBitmap R/G/B : Pos[${pixel_x},${pixel_y}] = ${dng_bitmap_RGB[0]}/${dng_bitmap_RGB[1]}/${dng_bitmap_RGB[2]}")

                        //call tiffbitmap, bmp to Andorid bitmap
//                        var tiffbmp_bitmap = BitmapFactory.decodeFile(tiffbmp_path)
//                        val tiffbmp_w = tiffbmp_bitmap.width
//                        val tiffbmp_h = tiffbmp_bitmap.height
                        var bmp_bitmap = BitmapFactory.decodeFile(bmp_path)
                        val tiffbmp_w = bmp_bitmap.width
                        val tiffbmp_h = bmp_bitmap.height

//                        Log.d("KSM", "tiffbmp size : ${tiffbmp_bitmap.width}*${tiffbmp_bitmap.height}")
                        Log.d("KSM", "bmp size (w*h) : ${bmp_bitmap.width}*${bmp_bitmap.height}")

                        //tiffbmp_bitmap RGB
//                        pixel_x = (tiffbmp_bitmap.width) / 2
//                        pixel_y = (tiffbmp_bitmap.height) / 2
//                        var tiffbmp_RGB = getRGB(tiffbmp_bitmap, pixel_x, pixel_y)
//                        Log.d("KSM", "tiffbmp R/G/B : Pos[${pixel_x},${pixel_y}] = ${tiffbmp_RGB[0]}/${tiffbmp_RGB[1]}/${tiffbmp_RGB[2]}")

                        //bmp_bitmap RGB
//                        var bmp_RGB = getRGB(bmp_bitmap, saved_x, saved_y)
//                        Log.d("KSM", "bmp R/G/B : Pos[${saved_x},${saved_y}] = ${bmp_RGB[0]}/${bmp_RGB[1]}/${bmp_RGB[2]}")
                        pixel_x = (bmp_bitmap.width) / 2
                        pixel_y = (bmp_bitmap.height) / 2
                        var bmp_center_RGB = getRGB(bmp_bitmap, pixel_x, pixel_y)
                        Log.d("KSM", "bmp R/G/B : Pos[${pixel_x},${pixel_y}] = ${bmp_center_RGB[0]}/${bmp_center_RGB[1]}/${bmp_center_RGB[2]}")

                        val intent = Intent(this@RawActivity, ResultActivity::class.java)
//                        intent.putExtra("cameraId", rawCameraInfo!!.cameraId)
//                        intent.putExtra("imageFormat", rawCameraInfo!!.format)
                        intent.putExtra("dngPath", pathName)
                        intent.putExtra("bmpPath", bmp_path)
                        Log.d("KSM", "accX : ${captureAccX}, accY : ${captureAccY}, accZ : ${captureAccZ}\n" +
                                "angleXZ : ${captureAngleXZ}, angleYZ : ${captureAngleYZ}")
                        intent.putExtra("accX", captureAccX)
                        intent.putExtra("accY", captureAccY)
                        intent.putExtra("accZ", captureAccZ)
                        intent.putExtra("angleXZ", captureAngleXZ)
                        intent.putExtra("angleYZ", captureAngleYZ)
                        intent.putExtra("stringAfState", stringAfState)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    }

//                    //촬영 후 다시 자동 AF 모드로 설정
//                    session.stopRepeating()
//                    captureRequest.apply{
//                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
//                    }
//                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                }
            }

            it.post{
//                it.isEnabled = true
                Toast.makeText(this@RawActivity, "Image Captured! \n  location:${outputUri.toString()}", Toast.LENGTH_SHORT).show()

                val dialog = ProgressDialog(this@RawActivity)
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                dialog.setCancelable(false)
                dialog.setMessage("이미지 저장중...")
                dialog.show()

//                viewBinding.loadingCircle.visibility = View.VISIBLE

            }
        }


        //AutoFocus 관련 변수 및 제스처 설정
        var clickCount: Int = 0
        var startTime: Long = 0
        var duration: Long = 0
        var MAX_DURATION = 200;

        //두 번 터치하게 되면, AutoFocus 진행.
        viewBinding.texture.setOnTouchListener(View.OnTouchListener{ v: View, event: MotionEvent ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    startTime = System.currentTimeMillis()
                    clickCount++

                    Log.d("KSM", "clickCount:${clickCount} / startTime:${startTime} / duration:${duration}")

                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    var nowTime = System.currentTimeMillis() - startTime
                    duration += nowTime

                    if(clickCount >= 2){
                        if(duration <= MAX_DURATION){
//                            Log.d("KSM", "ManualFocused")

                            try{
                                cancelAutoFocus()
                                startAutoFocus(meteringRectangle(event))

                                clickCount = 0
                            }catch(e: CameraAccessException){
                                Log.e("KSM", "AutoFocusError!", e)
                                Toast.makeText(this@RawActivity, "This device doesn't support AutoFocus" ,Toast.LENGTH_SHORT).show()
                            }
                        }else {
                            clickCount = 0
                            duration = 0
                        }
                    }
                    return@OnTouchListener true
                }
                else -> return@OnTouchListener false
            }
        })


        //Zoom 관련 변수 및 제스처 설정


    }

    private fun chooseOptimalSize(characteristics: CameraCharacteristics, viewWidth: Int, viewHeight: Int): Size {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val outputSizes = map.getOutputSizes(SurfaceTexture::class.java)
        val desiredAspectRatio = viewWidth.toFloat() / viewHeight
        var optimalSize = outputSizes[0]

        for (size in outputSizes) {
            if (size.width.toFloat() / size.height == desiredAspectRatio &&
                size.width <= viewWidth && size.height <= viewHeight
            ) {
                optimalSize = size
            }
        }

        return optimalSize
    }

    private suspend fun takePhoto():
            CombinedCaptureResult = suspendCoroutine { cont ->

        // Flush any images left in the image reader
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {
        }

        // Start a new image queue
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            Log.d("KSM", "Image available in queue: ${image.timestamp}")
            imageQueue.add(image)
        }, imageReaderHandler)

        val captureRequest = session.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
            }

        Log.d("KSM", "captureRequest's JPEG_ORIENTATION : ${captureRequest.get(CaptureRequest.JPEG_ORIENTATION)}")

//        //autoFocus 진행했다면 정해진 AF_REGIONS를 값으로 저장
//        val autoFocusRegion = captureRequest.get(CaptureRequest.CONTROL_AF_REGIONS)
//
//        //맞춰진 Focus를 captureRequest에 적용.
//        captureRequest.apply{
//            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
//            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
//            set(CaptureRequest.CONTROL_AF_REGIONS, autoFocusRegion)
//        }

        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)

                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                Log.d("KSM", "Capture result received: $resultTimestamp")

                // AF 상태를 확인합니다.
//                val afState = result[CaptureResult.CONTROL_AF_STATE]
//                if (afState == null) {
//                    Log.d(TAG, "Auto-focus state is null")
//                } else {
//                    when (afState) {
//                        CaptureResult.CONTROL_AF_STATE_INACTIVE -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Inactive"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Passive Scan"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Passive Focused"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Passive Unfocused"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Active Scan"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Focused Locked"
//                        )
//
//                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> Log.d(
//                            "KSM",
//                            "Auto-focus state: Not Focused Locked"
//                        )
//                    }
//                }

                // Set a timeout in case image captured is dropped from the pipeline
                val exc = TimeoutException("Image dequeuing took too long")
                val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                // Loop in the coroutine's context until an image with matching timestamp comes
                // We need to launch the coroutine context again because the callback is done in
                //  the handler provided to the `capture` method, not in our coroutine context
                @Suppress("BlockingMethodInNonBlockingContext")
                lifecycleScope.launch(cont.context){
                    while (true) {

                        // Dequeue images while timestamps don't match
                        val image = imageQueue.take()
                        // TODO(owahltinez): b/142011420
                        // if (image.timestamp != resultTimestamp) continue
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            image.format != ImageFormat.DEPTH_JPEG &&
                            image.timestamp != resultTimestamp) continue
                        Log.d("KSM", "Matching image dequeued: ${image.timestamp}")
                        Log.d("KSM", "image size : ${image.width} x ${image.height}")

                        // Unset the image reader listener
                        imageReaderHandler.removeCallbacks(timeoutRunnable)
                        imageReader.setOnImageAvailableListener(null, null)

                        // Clear the queue of images, if there are left
                        while (imageQueue.size > 0) {
                            imageQueue.take().close()
                        }

                        // Compute EXIF orientation metadata
                        val rotation = relativeOrientation.value ?: 0
                        val mirrored = characteristics!!.get(CameraCharacteristics.LENS_FACING) ==
                                CameraCharacteristics.LENS_FACING_FRONT

                        //set FIXED orientation
//                        val exifOrientation = computeExifOrientation(rotation, mirrored)
                        val exifOrientation = computeExifOrientation(90, false)

                        Log.d("KSM", "rotation : ${rotation}")
                        Log.d("KSM", "exifOrientation: ${exifOrientation}")

                        // Build the result and resume progress
                        cont.resume(CombinedCaptureResult(
                            image, result, exifOrientation, imageReader.imageFormat))

                        // There is no need to break out of the loop, this coroutine will suspend
                    }
                }
            }
        }, cameraHandler)
    }

    //cameraId를 알아내오는 함수
    private fun getCameraId(context: Context, facing: Int) : String{
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        return manager.cameraIdList.first{
            manager
                .getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == facing
        }
    }

    //퍼미션이 제대로 되었는지 체크하기 위한 함수
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w("KSM", "Camera $cameraId has been disconnected")
                RawActivity().finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e("KSM", exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e("KSM", exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    /** Helper data class used to hold capture metadata with their associated image */
    data class CombinedCaptureResult(
        val image: Image,
        val metadata: CaptureResult,
        val orientation: Int,
        val format: Int
    ) : Closeable {
    override fun close() = image.close()
    }

    /** Helper function used to save a [CombinedCaptureResult] into a [File] */
    private suspend fun saveResult(result: CombinedCaptureResult) = suspendCoroutine { cont ->
        when (result.format) {
            // When the format is RAW we use the DngCreator utility library
            ImageFormat.RAW_SENSOR -> {
                Log.d("KSM", "result's metadata orientation : ${result.metadata.get(CaptureResult.JPEG_ORIENTATION)}")

                val dngCreator = DngCreator(characteristics!!, result.metadata)

                //yuvImagePart
//                val convertedImage = YUV_420_888toNV21(result.image)
//                val yuvImage = YuvImage(convertedImage, result.format, result.image.width, result.image.height, null)

                try {
//                    outputDirectory = getOutputDirectory() - 이전의 흔적 (이미지 저장)

                    val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA).format(System.currentTimeMillis())
//                    val fileName = "RAW_$timestamp.dng" - 이전의 흔적 (이미지 저장)
                    val fileName = "RAW_$timestamp"

                    //디렉토리 설정 (현재 - MediaStore 활용)
                    val contentValues = ContentValues().apply{
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){ //API Level 29 (Android 10.0) 이상
                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
//                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") //yuvImage
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraProj-Image Raw")
                        }else{
                            val imageDir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).absolutePath + "/CameraProj-Image Raw"
                            val imageDirFile = File(imageDir)
                            if(!imageDirFile.exists()){
                                imageDirFile.mkdirs()
                            }
                            val imageFile = File(imageDirFile, "${fileName}.dng")
//                            val imageFile = File(imageDirFile, "${fileName}.jpg") //yuvImage
                            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                        }
                    }

                    val resolver = this.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    /** 이전의 흔적 (이미지 저장)
                    val output = File(outputDirectory, fileName)
                    Log.d("KSM", "OutputFile.path : ${output.path}")
                    **/

                    //사진 회전
                    dngCreator.setOrientation(result.orientation)

                    uri?.let{imageUri ->
                        val outputStream = resolver.openOutputStream(imageUri)

                        if (outputStream != null) {
                            dngCreator.writeImage(outputStream, result.image)

                        }else{
                            Log.d("KSM", "Image Write Wrong!!!!")
                        }

                        outputStream?.close()
                    }

                    /** 이전의 흔적 (이미지 저장)
                    FileOutputStream(output).use {
                        dngCreator.writeImage(it, result.image)
                    }
                    **/

                    cont.resume(uri)
//                    cont.resume(output) - 이전의 흔적 (이미지 저장)
                } catch (exc: IOException) {
                    Log.e("KSM", "Unable to write DNG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // No other formats are supported by this sample
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                Log.e("KSM", exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }

    /** 이전의 흔적 (이미지 저장)
//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, "CameraProj-Image RAW").apply { mkdirs() }
////            File("/storage/emulated/0/Android/media/Pictures","CameraProj-Image").apply { mkdirs() }
//        }
//
////        Log.d("KSM", "mediaDir : ${mediaDir}")
////        Log.d("KSM", "mediaDir.exists() : ${mediaDir?.exists()}")
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//    }**/

    //터치한 위치를 간단하게 정규화 시키는 함수
    private fun meteringRectangle(event: MotionEvent): MeteringRectangle {
        val sensorOrientation = characteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        val sensorSize = characteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!

        val halfMeteringRectWidth = (METERING_RECTANGLE_SIZE * sensorSize.width()) / 2
        val halfMeteringRectHeight = (METERING_RECTANGLE_SIZE * sensorSize.height()) / 2

        //[x,y] 터치 포인트를 뷰포인트를 [0,1]로 맞춰서 정상화
        val normalizedPoint = floatArrayOf(event.x / previewSize.height, event.y / previewSize.width)

        Matrix().apply{
            postRotate(-sensorOrientation.toFloat(), 0.5f, 0.5f)
            postScale(sensorSize.width().toFloat(), sensorSize.height().toFloat())
            mapPoints(normalizedPoint)
        }

        val meteringRegion = Rect(
            (normalizedPoint[0] - halfMeteringRectWidth).toInt().coerceIn(0, sensorSize.width()),
            (normalizedPoint[1] - halfMeteringRectHeight).toInt().coerceIn(0, sensorSize.height()),
            (normalizedPoint[0] + halfMeteringRectWidth).toInt().coerceIn(0, sensorSize.width()),
            (normalizedPoint[1] + halfMeteringRectHeight).toInt().coerceIn(0, sensorSize.height())
        )

        return MeteringRectangle(meteringRegion, MeteringRectangle.METERING_WEIGHT_MAX)
    }

    //AutoFocus를 진행하는 함수
    private fun startAutoFocus(meteringRectangle: MeteringRectangle){
        val captureRequest = setCaptureRequest()

        captureRequest.apply{
            set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            if(torchState == true){
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            setTag(AUTO_FOCUS_TAG)
        }

        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

        Log.d("KSM", "AutoFocused")
        Toast.makeText(this@RawActivity, "Focused : ${meteringRectangle.x} / ${meteringRectangle.y}" ,Toast.LENGTH_SHORT).show()
    }

    //이전에 설정해놓은 AutoFocus를 해제
    private fun cancelAutoFocus(){
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)

        val captureRequest = setCaptureRequest()

        captureRequest.apply{
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            if(torchState == true){
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
        }

        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

        Log.d("KSM", "AutoFocus Cancelled")
    }

    //captureRequest 설정 함수
    private fun setCaptureRequest() : CaptureRequest.Builder {
        session.stopRepeating()

        val captureRequest = camera2!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply{
            addTarget(viewBinding.rawViewFinder.holder.surface)
        }

        return captureRequest
    }

    private fun loadInputBuffer(filePath : String): ByteArray {
        val inputFile = File(filePath)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }

    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {

        // Load bitmap from given buffer
        val bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)

        // Transform bitmap orientation using provided metadata
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, bitmapTransformation, true)
    }

    private fun YUV_420_888toNV21(image : Image) : ByteArray{
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize+uSize+vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize+vSize, uSize);

        return nv21
    }

    private fun getRGB(bmp1 : Bitmap, x : Int, y : Int) : Array<Int> {

        val bmp1RGB = bmp1.getPixel(x, y)

        val a = Color.alpha(bmp1RGB)
        val r = Color.red(bmp1RGB)
        val g = Color.green(bmp1RGB)
        val b = Color.blue(bmp1RGB)

        val intArray = arrayOf(r,g,b)

        return intArray
    }

    override fun onSensorChanged(event: SensorEvent){
        accX = event.values[0].toDouble()
        accY = event.values[1].toDouble()
        accZ = event.values[2].toDouble()

        angleXZ = Math.atan2(accX, accZ) * 180/Math.PI
        angleYZ = Math.atan2(accY, accZ) * 180/Math.PI

//        Log.i("KSM", "ACCELOMETER | X : ${String.format("%.4f", accX)} | Y : ${String.format("%.4f", accY)} | Z : ${String.format("%.4f", accZ)}" +
//                " | angleXZ : ${String.format("%.4f", angleXZ)} | angleYZ : ${String.format("%.4f", angleYZ)}")
        viewBinding.accXTextView.text = "accX : ${accX.toInt()}"
        viewBinding.accYTextView.text = "accY : ${accY.toInt()}"
        viewBinding.accZTextView.text = "accZ : ${accZ.toInt()}"
        viewBinding.angleXZTextView.text = "angleXZ : ${angleXZ.toInt()}"
        viewBinding.angleYZTextView.text = "angleYZ : ${angleYZ.toInt()}"
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int)  = Unit

    override fun onResume() {
        super.onResume()

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause(){
        super.onPause()

        try{
            camera2!!.close()
        }catch(exc: Throwable){
            Log.e("KSM", "Error closing camera", exc)
        }

        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
//        cameraExecutor.shutdown()
        //camera2
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
//        viewBinding.loadingCircle.visibility = View.GONE
        sensorManager.unregisterListener(this)
    }

    companion object{
        init {
            System.loadLibrary("proj_camera")
        }

        private const val TAG = "proj_Camera_RAW"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        const val REQUEST_CODE_PERMISSIONS = 10

        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply{
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        //Camera2 Basic 참고
        private fun enumerateCameras(cameraManager: CameraManager) : List<FormatItem>{
            val availableCameras: MutableList<FormatItem> = mutableListOf()

            //GET list of all compatible cameras
            val cameraIds = cameraManager.cameraIdList.filter{
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                ) ?: false
            }

            // Iterate over the list of cameras and return all the compatible ones
            cameraIds.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!)

                // Query the available capabilities and output formats
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val outputFormats = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

                // All cameras *must* support JPEG output so we don't need to check characteristics
                availableCameras.add(
                    FormatItem(
                        "$orientation JPEG ($id)", id, ImageFormat.JPEG
                    )
                )

                // Return cameras that support RAW capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                    outputFormats.contains(ImageFormat.RAW_SENSOR)) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR
                        )
                    )
                }

                // Return cameras that support JPEG DEPTH capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation DEPTH ($id)", id, ImageFormat.DEPTH_JPEG
                        )
                    )
                }
            }

            return availableCameras
        }

        /** Helper function used to convert a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int) = when(value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /** Maximum time allowed to wait for the result of an image capture */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /** Helper class used as a data holder for each selectable camera format item */
        private data class FormatItem(val title: String, val cameraId: String, val format: Int)

        /** Maximum size of [Bitmap] decoded */
        private const val DOWNSAMPLE_SIZE: Int = 1024  // 1MP

        //Camera2 Extension 참고
        //METERING_RECTANGLE_SIZE 초기값 설정
        private const val METERING_RECTANGLE_SIZE = 0.15f
        private const val AUTO_FOCUS_TAG = "auto_focus_tag"
        private const val AUTO_FOCUS_TIMEOUT_MILLIS = 5_000L
    }
}
