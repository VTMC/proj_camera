package com.example.proj_camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.CalendarContract.Instances
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.computeExifOrientation
import com.example.android.camera.utils.getPreviewOutputSize
import com.example.proj_camera.MainActivity.Companion.REQUEST_CODE_PERMISSIONS
import com.example.proj_camera.MainActivity.Companion.REQUIRED_PERMISSIONS
import com.example.proj_camera.databinding.RawActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files.createFile
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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

    private lateinit var outputDirectory: File

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

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

//    /** Live data listener for changes in the device orientation relative to the camera */
//    //카메라와 관련된 장치 방향 변경에 대한 라이브 데이터 수신기
//    private lateinit var relativeOrientation: OrientationLiveData

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

            override fun surfaceCreated(holder: SurfaceHolder) {
                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                    viewBinding.rawViewFinder.display,
                    characteristics!!,
                    SurfaceHolder::class.java
                )
                Log.d("KSM", "View finder size: ${viewBinding.rawViewFinder.width} x ${viewBinding.rawViewFinder.height}")
                Log.d("KSM", "Selected preview size: $previewSize")
                viewBinding.rawViewFinder.setAspectRatio(
                    previewSize.width,
                    previewSize.height
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

        relativeOrientation = OrientationLiveData(this, characteristics!!).apply{
            observe(this@RawActivity, Observer { orientation ->
                //제공된 카메라 ID에 해당하는 '카메라 특성'을 불러오는 characteristics
                characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                Log.d("KSM", "Orientation changed $orientation ")
                Log.d("KSM", "Camera Characteristics : ${characteristics.toString()}")
            })
        }

        viewBinding.changeNormalBtn.setOnClickListener {
            onPause()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraProj-Image RAW").apply { mkdirs() }
//            File("/storage/emulated/0/Android/media/Pictures","CameraProj-Image").apply { mkdirs() }
        }
//        Log.d("KSM", "mediaDir : ${mediaDir}")
//        Log.d("KSM", "mediaDir.exists() : ${mediaDir?.exists()}")
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    //카메라가 시작되면 실행하는 함수
    private fun startCamera() = lifecycleScope.launch(Dispatchers.Main){
        camera2 = openCamera(cameraManager, rawCameraInfo!!.cameraId, cameraHandler)

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

        val captureRequest = camera2!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW).apply{addTarget(viewBinding.rawViewFinder.holder.surface)}

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

                    // Save the result to disk
                    val output = saveResult(result)

                    Log.d("KSM", "Image saved: ${output.absolutePath}")

                }
            }

            it.post{
                it.isEnabled = true
                Toast.makeText(this@RawActivity, "Image Captured! \n It will take some time to get image", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("KSM", "CameraActivated")
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
            CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
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
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {
            // When the format is RAW we use the DngCreator utility library
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristics!!, result.metadata)

                try {
                    //디렉토리 설정
                    outputDirectory = getOutputDirectory()
                    Log.d("KSM", "outputDirectory : ${outputDirectory}")
                    val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA).format(System.currentTimeMillis())
                    val fileName = "RAW_$timestamp.dng"
                    val output = File(outputDirectory, fileName)


//                    val outputFile = File("Pictures/CameraProj-Image", fileName)

//                    Log.d("KSM", "OutputFile.parent : ${output.parent}")
                    Log.d("KSM", "OutputFile.path : ${output.path}")

                    FileOutputStream(output).use {
                        dngCreator.writeImage(it, result.image)
                    }

                    cont.resume(output)
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

    //camera2
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
        cameraExecutor.shutdown()
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

        //Camera2 Project
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
    }
}

