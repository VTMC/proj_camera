package com.example.proj_camera

import Utils.AndroidBmpUtil
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
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
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

import android.graphics.Color


class RawActivity : AppCompatActivity() {
    private lateinit var viewBinding: RawActivityBinding

    private lateinit var cameraExecutor: ExecutorService

    //camera2
    //카메라 장치(모든 카메라 작업에 사용)를 검색, 특성화 및 연결합니다
    private var camera2 : CameraDevice ?= null
    private var cameraId : String ?= null
    private var rawCameraInfo : FormatItem ?= null

    private val cameraManager: CameraManager by lazy {
        this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)


        //캡처버튼을 클릭했을 경우
        viewBinding.imageCaptureBtn.setOnClickListener{
            it.isEnabled = false

            Log.d("KSM", "CaptureBtn Clicked!!")

            viewBinding.flashView.visibility = View.VISIBLE
            val flashAni = AnimationUtils.loadAnimation(this@RawActivity,R.anim.alpha_anim)
            viewBinding.flashView.startAnimation(flashAni)
            Handler().postDelayed({
                viewBinding.flashView.visibility = View.GONE }, 500)

            lifecycleScope.launch(Dispatchers.IO) {
                takePhoto().use { result ->
                    Log.d("KSM", "Result received: $result")

                    // Save the result to disk - 이전의 흔적 (이미지 저장)
//                    val output = saveResult(result) - 이전의 흔적 (이미지 저장)

                    outputUri = saveResult(result)
//                    Log.d("KSM", "Image saved: ${outputUri.toString()}") //- 이전의 흔적 (이미지 저장)

                    //Path를 얻기 위한 과정
                    val cursor = contentResolver.query(outputUri!!, null, null, null, null)
                    cursor!!.moveToNext()
                    val pathName = cursor.getString(cursor.getColumnIndex("_data") ?: 0)
                    val dng_file = File(pathName)
                    val path = dng_file.parent

                    Log.d("KSM", "outputDirectory pathName : ${pathName}")
                    Log.d("KSM", "outputDirectory path : ${path}")

                    //bitmap으로 뽑아서 PNG로 저장하는 과정
                    val dng_bitmap = BitmapFactory.decodeFile(pathName)

                    //색 측정할 픽셀 위치 설정
                    val pixel_x = (dng_bitmap.width) / 2
                    val pixel_y = (dng_bitmap.height) / 2

                    val dng_str = getRGB(dng_bitmap, pixel_x, pixel_y)
                    Log.d("KSM", "DNG RGB : $dng_str")

//                    val dng_buf = ByteBuffer.allocate(dng_bitmap.byteCount)
//                    dng_bitmap.copyPixelsToBuffer(dng_buf)
//                    val dng_byte = dng_buf.array()

//                    Log.d("KSM", "${dng_bitmap}")

                    //직접 Bytes로 읽어서 비트맵으로 변환
//                    val dng_bytes = dng_file.readBytes()
//                    val dng_bitmap = BitmapFactory.decodeByteArray(dng_bytes, 0, dng_bytes.size)

                    //imageDecoder 사용
//                    val dng_imgDec = ImageDecoder.createSource(dng_file)
//                    val dng_bitmap = ImageDecoder.decodeBitmap(dng_imgDec).copy(Bitmap.Config.ARGB_8888, true)

                    //YUVImage 활용
//                    val dngFis = FileInputStream(dng_file)
//                    val dng_bytes = dngFis.readBytes()
//                    dngFis.close()
//
//                    val dng_w = dng_bitmap.width
//                    val dng_h = dng_bitmap.height
//
//                    val yuvImage = YuvImage(dng_bytes, ImageFormat.NV21, dng_w, dng_h, null)

                    //Camera2Basic 참고
//                    val dng_buf = loadInputBuffer(pathName)
//                    val dng_bitmap = decodeBitmap(dng_buf, 0, dng_buf.size)

                    //dng SDK 활용 - 사용 불가
//                    val dng_data = rawSDK().dngFileInputStream(pathName)

                    //fileInputStream
//                    val baos = ByteArrayOutputStream()
//
//                    val fis = FileInputStream(dng_file)
//                    var bytesRead = fis.read()
//                    val dng_bytes = dng_file.readBytes()



//                    val libraw = LibRaw.newInstance()
//
//                    val dng_bitmap = libraw.decodeBitmap(pathName, null)

                    val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA).format(System.currentTimeMillis())

                    val jpg_fileName = "/JPG_$timestamp.jpg"
                    val jpg_file = File(path+jpg_fileName)

                    val bmp_fileName = "/BMP_$timestamp.bmp"
                    val bmp_path = path+bmp_fileName
                    val bmp_file = File(path+bmp_fileName)

//                    val jpg_yuvimg = File(path+txt_fileName)

                    Log.d("KSM", "outputDirectory PNG pathName : ${jpg_file.absolutePath}")

                    val m_rotate = Matrix()

                    try{
                        val fos = FileOutputStream(jpg_file)
//                        fos.write(dng_byte)


//                        val o_s = FileOutputStream(jpg_yuvimg)
//                        yuvImage.compressToJpeg(Rect(0,0,dng_w, dng_h), 100, o_s)

//                        m_rotate.postRotate((relativeOrientation.value?:90).toFloat())
//
//                        val rotated_bitmap = Bitmap.createBitmap(dng_bitmap, 0, 0, dng_bitmap.width, dng_bitmap.height, m_rotate, true)
//
//                        val patchedBitmap = SetRGBValue(rotatedPngBitmap, 0, 255, 0)

//                        val rotatedPngBitmap = Bitmap.createBitmap(dng_bitmap, 0, 0, 1080, 1920, m_rotate, true)

//                        val resizePngBitmap = Bitmap.createScaledBitmap(rotatedPngBitmap, viewBinding.rawViewFinder.width, viewBinding.rawViewFinder.height, true)
//
//                        val borderViewLeft = viewBinding.border.left
//                        val borderViewTop = viewBinding.border.top
//
//                        val cart_w = viewBinding.border.width
//                        val cart_h = viewBinding.border.height
//
//                        Log.d(TAG, "BorderView Info")
//                        Log.d(TAG, "x : ${borderViewLeft}")
//                        Log.d(TAG, "y : ${borderViewTop}")
//                        Log.d(TAG, "width : ${cart_w}")
//                        Log.d(TAG, "height : ${cart_h}")
//
//
//                        val cropCartPngBitmap = Bitmap.createBitmap(resizePngBitmap, borderViewLeft, borderViewTop, cart_w, cart_h)

                        val compressed = dng_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                        val saveBmp = AndroidBmpUtil.save(dng_bitmap, bmp_path)

                        if(compressed){
                            Log.d("KSM", "Bitmap compressed jpg!!")
                        }else{
                            Log.d("KSM", "Bitmap compressed failed!!")
                        }

                        if(saveBmp){
                            Log.d("KSM", "Bitmap saved bmp!!")
                        }else{
                            Log.d("KSM", "Bitmap saved bmp failed!!")
                        }
                    }catch(exc : Exception){
                        Log.e("KSM", "PNG_Errored!", exc)
                    }

                    //jpg_android bitmap 비교
                    val jpg_bitmap = BitmapFactory.decodeFile(jpg_file.absolutePath)

                    val jpg_str = getRGB(jpg_bitmap, pixel_x, pixel_y)
                    Log.d("KSM", "JPG RGB : $jpg_str")

                    //bmp_android bitmap 비교
                    val bmp_bitmap = BitmapFactory.decodeFile(bmp_file.absolutePath)

                    val bmp_str = getRGB(bmp_bitmap, pixel_x, pixel_y)
                    Log.d("KSM", "BMP RGB : $bmp_str")

                    //촬영 후 다시 자동 AF 모드로 설정
                    session.stopRepeating()
                    captureRequest.apply{
                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    }
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

                }
            }

            it.post{
                it.isEnabled = true
                Toast.makeText(this@RawActivity, "Image Captured! \n  location:${outputUri.toString()}", Toast.LENGTH_SHORT).show()
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

        //autoFocus 진행했다면 정해진 AF_REGIONS를 값으로 저장
        val autoFocusRegion = captureRequest.get(CaptureRequest.CONTROL_AF_REGIONS)

        //맞춰진 Focus를 captureRequest에 적용.
        captureRequest.apply{
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            set(CaptureRequest.CONTROL_AF_REGIONS, autoFocusRegion)
        }

        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)

                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                Log.d("KSM", "Capture result received: $resultTimestamp")

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
                        val exifOrientation = computeExifOrientation(rotation, mirrored)

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

                            //yuvImage
//                            yuvImage.compressToJpeg(Rect(0,0,result.image.width, result.image.height), 100, outputStream)
                        }else{
                            Log.d("KSM", "Image Write Wrong!!!!")
                        }

//                        outputStream?.close()
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

    private fun getRGB(bmp1 : Bitmap, x : Int, y : Int) : String {

        val bmp1RGB = bmp1.getPixel(x, y)

        val a = Color.alpha(bmp1RGB)
        val r = Color.red(bmp1RGB)
        val g = Color.green(bmp1RGB)
        val b = Color.blue(bmp1RGB)

        val str = "pos[$x, $y] diffrence ARGB = [$a, $r, $g, $b]\n"

        return str
    }

    override fun onPause(){
        super.onPause()

        try{
            camera2!!.close()
        }catch(exc: Throwable){
            Log.e("KSM", "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        cameraExecutor.shutdown()
        //camera2
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }


    companion object{
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
        private fun enumerateCameras(cameraManager: CameraManager) : List<RawActivity.Companion.FormatItem>{
            val availableCameras: MutableList<RawActivity.Companion.FormatItem> = mutableListOf()

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
                    Companion.FormatItem(
                        "$orientation JPEG ($id)", id, ImageFormat.JPEG
                    )
                )

                // Return cameras that support RAW capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                    outputFormats.contains(ImageFormat.RAW_SENSOR)) {
                    availableCameras.add(
                        Companion.FormatItem(
                            "$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR
                        )
                    )
                }

                // Return cameras that support JPEG DEPTH capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)) {
                    availableCameras.add(
                        RawActivity.Companion.FormatItem(
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
