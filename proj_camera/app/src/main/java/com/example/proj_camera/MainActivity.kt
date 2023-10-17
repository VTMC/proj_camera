package com.example.proj_camera

import Utils.rawSDK
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proj_camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    //사진 촬영 선언
    private var imageCapture: ImageCapture? = null

    //카메라 설정 선언
    private var camera : Camera? = null
    private var cameraController : CameraControl? = null

    //카메라 정보 선언
    private var cameraInfo: CameraInfo? = null

    //카메라 토치 설정 변수
    private var torchState: Int = TorchState.OFF

    //카메라 전후면 기본 설정 변수
    private var lensfacing = CameraSelector.LENS_FACING_BACK

    //줌 배율 기본 설정 변수
    private var zoomRatio: Float = 0.1F

    //timer 기본 설정 변수
    private var timerNum : Int = 0 //타이머 초 숫자 변수
    private var timerFlag : Int = 0 //타이머 설정 상태 변수

    //캡처하는 방식 설정 변수
    private var captureFlag : Int = 0 //기본 설정은 0(일반 촬영)

    //resolution width, height 설정 변수
    private var imageWidth:Int = 0
    private var imageHeight:Int = 0

    //flashAuto on off 변수
    private var flashAutoOn : Boolean = false

    //imageType 변수 선언
    private var onRaw : Boolean = false
    private var imageType : String = "image/jpeg"

    //cameraSettings
    private val isoArray = arrayOf("50","100","200","400","800","1600","3200")
    private val s_sArray = arrayOf("1s","1/2s","1/4s","1/8s","1/15s","1/30s", "1/60s", "1/125s", "1/250s", "1/500s", "1/1000s")
    private val irisArray = arrayOf("")

    private lateinit var cameraExecutor: ExecutorService

    //timer 설정을 위한 handler 사용
    private val handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            viewBinding.timerTxtLayout.visibility = View.VISIBLE
            viewBinding.timerTxt.text = timerNum.toString()
            timerNum--
            if(timerNum == -1){
                takePhoto()
                viewBinding.timerTxtLayout.visibility = View.GONE
            }
        }
    }

    //퍼미션 설정
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //요청한 코드가 맞는지 확인 아니면 무시
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            //모든 퍼미션 권한이 부여됐다면 카메라 실행
            if(allPermissionGranted()){
                startCamera()
            //모든 권한이 부여되지 않았다면 Toast로 권한 부여가 안 됐다고 알림
            }else{
                Toast.makeText(this,
                "이 어플에 권한이 모두 부여되지 않았습니다. 권한을 부여해주세요.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        //test CPP
//        val nativeTest = rawSDK()
//        val testCppText = nativeTest.testNative()
//
//        Log.d("KSM", "Show Text From CPP : ${testCppText}")

        //뷰 바인딩
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //카메라 퍼미션 확인
        if(allPermissionGranted()){
            //퍼미션이 전부 확인되면 실행
            startCamera()
        }else{
            //아닐 경우 실행
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        //RAW 페이지로 이동하기 위한 버튼 클릭 설정
        viewBinding.changeRawBtn.setOnClickListener{
            val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraId = getCameraId(this@MainActivity, lensfacing)

            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

            val cameraList = enumerateCameras(cameraManager)

            Log.d("KSM", "Camera List : ${cameraList}")

            if(cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                    .contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats
                    .contains(ImageFormat.RAW_SENSOR)){
                try{
                    val intent = Intent(this, RawActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }catch(e: Exception){
                    Log.e("KSM", "changeRawBtn Intent Error!", e)
                }
            }else{
                var alertText = "This Smartphone doesn't support Raw Image \n"
                for(i in cameraList){
                    alertText += "카메라 : ${i.title} / "

                    alertText += "카메라 ID : ${i.cameraId} / "

                    if(i.format == ImageFormat.JPEG){
                        alertText += "지원 이미지 포맷 : JPEG \n"
                    }else if(i.format === ImageFormat.RAW_SENSOR){
                        alertText += "지원 이미지 포맷 : DNG \n"
                    }else{
                        alertText += "지원 이미지 포맷 : 기타 \n"
                    }
                }

                AlertDialog.Builder(this).run{
                    setTitle("Error")
                    setIcon(android.R.drawable.ic_dialog_alert)
                    setMessage(alertText)
                    setPositiveButton("OK", null)
                    show()
                }
            }
        }

        //사진 찍기 버튼을 위한 Listener 설정
        viewBinding.imageCaptureBtn.setOnClickListener{
            when(timerFlag){
                0 -> takePhoto()
                1 -> {
                    timerNum = 3
                    thread(start = true){
                        while(timerNum >= 0){
                            handler.sendEmptyMessage(0)
                            Thread.sleep(1000)
                        }
                    }
                }
                2 -> {
                    timerNum = 5
                    thread(start = true){
                        while(timerNum >= 0){
                            handler.sendEmptyMessage(0)
                            Thread.sleep(1000)
                        }
                    }
                }
                3 -> {
                    timerNum = 10
                    thread(start = true){
                        while(timerNum >= 0){
                            handler.sendEmptyMessage(0)
                            Thread.sleep(1000)
                        }
                    }
                }
            }
        }

        //Torch(Flash) 설정
        viewBinding.torchBtn.setOnClickListener{ flashTurnOnOff() }

        //카메라 앞/뒤 설정
        viewBinding.changeCameraBtn.setOnClickListener{ lensfacing_to() }

        //moreBtn 설정
        viewBinding.moreSettingBtn.setOnClickListener{
            //moreSetting 화면이 보일 경우 없앤다.
            if(viewBinding.moreBtnSetting.visibility == View.VISIBLE){
                viewBinding.moreBtnSetting.visibility = View.INVISIBLE
                //zoomBar가 보여져 있을 경우 같이 없앤다.
                if(viewBinding.zoomBar.visibility == View.VISIBLE){
                    viewBinding.zoomBar.visibility = View.INVISIBLE
                }
                //timerLayout이 보여져 있을 경우 같이 없앤다.
                if(viewBinding.timerLayout.visibility == View.VISIBLE){
                    viewBinding.timerLayout.visibility = View.INVISIBLE
                }
                //resolutionLayout이 보여져 있을 경우 같이 없앤다.
                if(viewBinding.resolutionLayout.visibility == View.VISIBLE){
                    viewBinding.resolutionLayout.visibility = View.GONE
                }
            // 안 보일 경우 보인다.
            }else{
                viewBinding.moreBtnSetting.visibility = View.VISIBLE
            }
        }

        //zoomBtn 버튼 설정
        viewBinding.zoomBtn.setOnClickListener{
            //zoomBar가 안 보일 경우 (zoomBtn을 클릭 안 했을 경우)
            if(viewBinding.zoomBar.visibility == View.INVISIBLE){
                viewBinding.zoomBar.visibility = View.VISIBLE
                viewBinding.timerLayout.visibility = View.INVISIBLE
                viewBinding.resolutionLayout.visibility = View.GONE
            //zoomBar가 보여질 경우
            }else{
                viewBinding.zoomBar.visibility = View.INVISIBLE
            }
        }

        var listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                //현재 카메라의 zoom 배율을 알아오는 변수 선언
                val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 0F

                //pinch 제스처의 비율 factor를 얻기
                val delta = detector.scaleFactor

                //zoomRatio에 현재 줌 Ratio를 저장
                zoomRatio = currentZoomRatio * delta

                //카메라 줌 배율을 업데이트 하는 구문
                cameraController?.setZoomRatio(zoomRatio)

                //Return true, 이벤트가 제대로 다뤄졌다면
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this, listener)

        var clickCount : Int = 0
        var startTime : Long = 0
        var duration: Long = 0
        val MAX_DURATION = 300;

        //viewer 수동 초점 설정 및 Pinch 줌 설정
        //수동 초점을 더블 탭을 해야 가능, 줌은 두 손가락을 통해 pinch 제스처를 취해야함.
        viewBinding.viewFinder.setOnTouchListener(View.OnTouchListener { v: View, event: MotionEvent ->

            when(event.action){
                MotionEvent.ACTION_DOWN ->{
                    startTime = System.currentTimeMillis()
                    clickCount++

                    Log.d("ksm", "clickCount:${clickCount} / startTime:${startTime} / duration:${duration}")

                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    var nowTime : Long = System.currentTimeMillis() - startTime
                    duration +=  nowTime
                    if(clickCount >= 2){
                        if(duration <= MAX_DURATION){
                            //뷰파인더로부터 MeteringPointFactory 가져오기
                            val factory = viewBinding.viewFinder.meteringPointFactory
                            //탭 좌표에서 MeteringPoint 생성하기
                            val point = factory.createPoint(event.x, event.y)
                            //MeteringPoint에서 MeteringAction을 만들고 측정 모드를 지정하도록 구성
                            val action = FocusMeteringAction.Builder(point).build()

                            //cameraControl을 통해 수동초점을 맞추기 시작.
                            cameraController?.startFocusAndMetering(action)
                            Toast.makeText(this, "수동 초점 맞춰짐 \n 초점 위치 : ${event.x} | ${event.y}", Toast.LENGTH_SHORT).show()
                        }else{
                            clickCount = 0
                            duration = 0
                        }
                    }
                    viewBinding.viewZoomRatio.visibility = View.INVISIBLE
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    viewBinding.viewZoomRatio.visibility = View.VISIBLE

                    var txtZoomRatio = String.format("%.1f", zoomRatio)
                    viewBinding.viewZoomRatio.text="x${txtZoomRatio}"
                    viewBinding.zoomPer.text = "x${txtZoomRatio}"

                    scaleGestureDetector.onTouchEvent(event)
                    return@OnTouchListener true
                }
                else -> return@OnTouchListener false
            }
        })

        //timer 설정
        //timer 버튼 클릭했을 경우
        viewBinding.timerBtn.setOnClickListener{
            if(viewBinding.timerLayout.visibility == View.INVISIBLE){
                viewBinding.timerLayout.visibility = View.VISIBLE
                viewBinding.zoomBar.visibility = View.INVISIBLE
                viewBinding.resolutionLayout.visibility = View.GONE
            }else{
                viewBinding.timerLayout.visibility = View.INVISIBLE
            }
        }
        //timer - X 버튼 클릭 시 (타이머가 중지, 및 타이머가 안 보여야 함)
        viewBinding.noTimerBtn.setOnClickListener{
            timerFlag = 0
            Toast.makeText(this, "타이머 실행 안 함",Toast.LENGTH_SHORT).show()
        }
        //timer - 'n's 버튼 클릭 시 (n이 무엇이냐에 따라 timerNum의 숫자가 달라지고, 타이머가 보여져야함)
        viewBinding.s3TimerBtn.setOnClickListener{
            timerFlag = 1
            Toast.makeText(this, "타이머 3초로 설정",Toast.LENGTH_SHORT).show()
        }

        viewBinding.s5TimerBtn.setOnClickListener{
            timerFlag = 2
            Toast.makeText(this, "타이머 5초로 설정",Toast.LENGTH_SHORT).show()
        }

        viewBinding.s10TimerBtn.setOnClickListener{
            timerFlag = 3
            Toast.makeText(this, "타이머 10초로 설정",Toast.LENGTH_SHORT).show()
        }

        viewBinding.resolutionBtn.setOnClickListener{
            if(viewBinding.resolutionLayout.visibility == View.GONE){
                viewBinding.resolutionLayout.visibility = View.VISIBLE
                viewBinding.zoomBar.visibility = View.INVISIBLE
                viewBinding.timerLayout.visibility = View.INVISIBLE
            }else{
                viewBinding.resolutionLayout.visibility = View.GONE
            }
        }

        viewBinding.resRadioGrouup.setOnCheckedChangeListener{
            radioGroup, i ->
                when(i){
                    com.example.proj_camera.R.id.radio640480 -> {
                        if(flashAutoOn == true) captureFlag = 3
                        else captureFlag = 2

                        imageWidth = 480
                        imageHeight = 640
                        startCamera()
                    }
                    com.example.proj_camera.R.id.radio1280720 -> {
                        if(flashAutoOn == true) captureFlag = 3
                        else captureFlag = 2

                        imageWidth = 720
                        imageHeight = 1280
                        startCamera()
                    }
                    com.example.proj_camera.R.id.radio19201080 -> {
                        if(flashAutoOn == true) captureFlag = 3
                        else captureFlag = 2

                        imageWidth = 1080
                        imageHeight = 1920
                        startCamera()
                    }
                    com.example.proj_camera.R.id.radio25601440 -> {
                        if(flashAutoOn == true) captureFlag = 3
                        else captureFlag = 2

                        imageWidth = 1440
                        imageHeight = 2560
                        startCamera()
                    }
                    com.example.proj_camera.R.id.radio38402160 -> {
                        if(flashAutoOn == true) captureFlag = 3
                        else captureFlag = 2

                        imageWidth = 2160
                        imageHeight = 3840
                        startCamera()
                    }
                    com.example.proj_camera.R.id.radioMax -> {
                        if(flashAutoOn == true) captureFlag = 1
                        else captureFlag = 0

                        imageWidth = 0
                        imageHeight = 0

                        startCamera()
                    }
                }
                if(captureFlag == 2 || captureFlag == 3){
                    Toast.makeText(this, "${imageWidth}*${imageHeight} 선택됨", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "최고해상도 선택됨", Toast.LENGTH_SHORT).show()
                }
        }

        var txtImageType : String ?= null

        //저장할 사진 타입 선택 가능
        viewBinding.typeRadioGroup.setOnCheckedChangeListener{
            radioGroup, i -> when(i){
                com.example.proj_camera.R.id.radioJPEG -> {
                    imageType = "image/jpeg"
                    txtImageType = "JPEG"
                }
                com.example.proj_camera.R.id.radioPNG -> {
                    imageType = "image/png"
                    txtImageType = "PNG"
                }
                com.example.proj_camera.R.id.radioHEIC -> {
                    imageType = "image/heic"
                    txtImageType = "HEIC"
                }
            }

            if(viewBinding.radioJPEG.isChecked || viewBinding.radioPNG.isChecked || viewBinding.radioHEIC.isChecked){
                Toast.makeText(this, "${txtImageType} 선택됨", Toast.LENGTH_SHORT).show()
                onRaw = false
                viewBinding.typeRadioGroupRaw.clearCheck()
            }
        }

        viewBinding.typeRadioGroupRaw.setOnCheckedChangeListener{
            radioGroup, i -> when(i){
                com.example.proj_camera.R.id.radioDNG -> {

                    imageType = "image/x-adobe-dng"
                    txtImageType = "DNG"
                }
                com.example.proj_camera.R.id.radioARW -> {
                    imageType = "image/x-sony-arw"
                    txtImageType = "ARW"
                }
                com.example.proj_camera.R.id.radioCRW -> {
                    imageType = "image/x-canon-crw"
                    txtImageType = "CRW"
                }
                com.example.proj_camera.R.id.radioNEF -> {
                    imageType = "image/x-nikon-nef"
                    txtImageType = "NEF"
                }
                com.example.proj_camera.R.id.radioRAW -> {
                    imageType = "image/x-pananonic-raw"
                    txtImageType = "RAW"
                }
            }
            if(viewBinding.radioDNG.isChecked || viewBinding.radioARW.isChecked ||
                    viewBinding.radioCRW.isChecked || viewBinding.radioNEF.isChecked ||
                    viewBinding.radioRAW.isChecked){
                Toast.makeText(this, "${txtImageType} 선택됨", Toast.LENGTH_SHORT).show()
                onRaw = true
                viewBinding.typeRadioGroup.clearCheck()
            }
        }

        //flash Auto 설정
        viewBinding.torchAutoBtn.setOnClickListener{
            if(flashAutoOn == false){
                flashAutoOn = true
                captureFlag = 1 //flash Auto 설정
                viewBinding.torchAutoBtn.text = "AUTO OFF"
                viewBinding.torchBtn.isEnabled = false
                if(imageWidth > 0 || imageHeight > 0) captureFlag = 3 //flashAuto & Resolution 설정
            }else{ //flashAutoOn == true
                flashAutoOn = false
                captureFlag = 0 //기본 설정(Default 설정)
                viewBinding.torchAutoBtn.text = "AUTO ON"
                viewBinding.torchBtn.isEnabled = true
                if(imageWidth > 0 || imageHeight > 0) captureFlag = 2 //Resolution만 설정
            }

            startCamera()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    //사진을 찍게 되면 실행하는 함수
    private fun takePhoto(){

        //먼저 ImageCapture에 사용할 레퍼런스를 가져온다. 사용사례가 null이면 함수 종료.
        // 만약 return문이 없으면, null인 경우 앱이 비정상 종료 된다.
        val imageCapture = imageCapture ?: return

        //MediaStore 항목과 타임스탬프를 생성
        //이미지를 보관할 MediaStore 콘텐츠 값을 만든다. 이름이 고유로 표시되도록 타임스탬프를 활용
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, imageType)
                //put(MediaStore.MediaColumns.COMPOSER, ImageFormat.RAW12)
                //put(MediaStore.MediaColumns.DATA, ImageFormat.RAW12)
                put(MediaStore.Images.Media.RELATIVE_PATH
                    ,"Pictures/CameraProj-Image")
            }else{
//                val envImageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath+"/CameraProj-Image"
//                val imagesDir = File(envImageDir)
//                if(!imagesDir.exists()){
//                    imagesDir.mkdir()
//                }
                val imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath+"/CameraProj-Image"
                val imageDirFile = File(imageDir)
                if(!imageDirFile.exists()){
                    imageDirFile.mkdirs()
                }
                val imageFile = File(imageDirFile, "${name}.jpg")
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                Log.d("KSM", "image Path : ${imageFile.absolutePath}")
            }
        }

        //outputOptions 객체를 만든다. 이 객체에서 원하는 출력 방법에 관한 사항을 지정할 수 있다.
        //파일 + 메타데이터를 포함한 객체 옵션들을 출력 생성
        val outputOptions = ImageCapture.OutputFileOptions
                            .Builder(contentResolver,
                                     MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                     contentValues)
                            .build()

        //사진이 찍힌 후 이미지 캡처 리스너를 설정
        /*imageCapture 객체에서 takePicture() 함수를 호출, outputOptions, 실행자, 이미지가 저장될 때
        * 콜백을 전달.*/
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                //이미지 캡처에 실패했을 경우, 오류 사례를 추가하여 실패했음을 기록
                override fun onError(exc: ImageCaptureException) {
                    Log.e("KSM", "사진 촬영 실패 : ${exc.message}", exc)
                }

                //사진이 성공적으로 촬영이 됐다면, 앞서 만든 파일을 저장, 사용자에게 토스트메시지로 알림
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    //찍는 모션을 보이기 위해 만든 애니메이션
                    viewBinding.flashView.visibility = View.VISIBLE
                    val flashAni = AnimationUtils.loadAnimation(this@MainActivity,com.example.proj_camera.R.anim.alpha_anim)
                    viewBinding.flashView.startAnimation(flashAni)
                    Handler().postDelayed({
                        viewBinding.flashView.visibility = View.GONE
                    }, 500)

                    val msg = "사진 촬영 성공 : ${output.savedUri}"
                    Log.d("KSM", "outputURI : ${output.savedUri}")
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    //Torch(Flash) 켜고 끄는 설정
    private fun flashTurnOnOff(){
        Log.d("ksm", "Torch click ${torchState}")
        when(torchState){
            //cameraTorch == true
            TorchState.ON -> {
                Log.d("ksm", "Torch off")
                viewBinding.torchAutoBtn.isEnabled = true
                cameraController?.enableTorch(false)
                viewBinding.torchBtn.text = "FLASH ON"
                torchState = TorchState.OFF
            }
            //cameraTorch == false
            TorchState.OFF -> {
                Log.d("ksm", "Torch on")
                viewBinding.torchAutoBtn.isEnabled = false
                cameraController?.enableTorch(true)
                viewBinding.torchBtn.text = "FLASH OFF"
                torchState = TorchState.ON
            }
        }
        Log.d("ksm", "Torch clicked")
    }

    //전/후면 카메라 설정
    private fun lensfacing_to(){
        lensfacing = if(lensfacing == CameraSelector.LENS_FACING_BACK){
            CameraSelector.LENS_FACING_FRONT
        }else{
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    //카메라가 시작되면 실행하는 함수
    private fun startCamera(){
//        var cameraId  = getCameraId(this@MainActivity, lensfacing)
//
//        Log.d("KSM", cameraId)

        //ProcessCameraProvider 인스턴스 생성. 카메라 생명주기를 소유자와 바인딩하는데 사용.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        /*cameraProviderFuture에 리스너 추가. Runnable을 하나의 인수로 추가. ContextCompat.getMainExecutor()를
        * 두 번째 인수로 추가. 그러면 기본 스레드에서 실행되는 Executor가 변환 (Executor는 Runnable을 실행시키는 개체*/
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //Preview 부분 : Preview 객체 초기화, build()를 호출하여 뷰파인더 노출 영역 제공자를 가져온 다음 미리보기 설정
             val preview = Preview.Builder()
                 .build()
                 .also{
                     it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                 }

            //사진 촬영 (captureFlag에 따라 사진 촬영 방식이 달라짐
            //imageCapture = ImageCapture.Builder().build() <- 기존의 방식
            imageCapture = when(captureFlag){
                0 ->{ //아무 설정 안 했을 경우
                    //ImageCapture.Builder().build()
                    ImageCapture.Builder()
                        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                }
                1 -> { //자동 플래시 모드가 켜졌을 경우
                    ImageCapture.Builder()
                        .setFlashMode(FLASH_MODE_AUTO)
                        .build()
                }
                2 -> { //이 부분은 좀 공부 필요할 듯 (해상도 설정)
                    ImageCapture.Builder()
                        .setTargetResolution(Size(imageWidth,imageHeight))
                        .build()
                }
                3 -> { //자동 플래시 & 해상도 설정 완료.
                    ImageCapture.Builder()
                        .setFlashMode(FLASH_MODE_AUTO)
                        .setTargetResolution(Size(imageWidth, imageHeight))
                        .build()
                }
                /*3 -> {
                    ImageCapture.Builder()
                        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                }*/
                else ->{ // 오류
                    Toast.makeText(this, "사진촬영 설정 미적용", Toast.LENGTH_SHORT).show()
                    ImageCapture.Builder().build()
                }
            }

            //뒷카메라가 기본으로 실행되도록 설정
            var cameraSelector = CameraSelector.Builder().requireLensFacing(lensfacing).build()

            //cameraProvider에 바인딩 된 항목을 없애서 초기화 시킨 다음.
            //cameraSelector 및 미리보기 객체, 사진 촬영 등을 바인딩 하도록 진행.
            try{
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                //줌을 위한 Seekbar 조절 - zoomBar 설정
                viewBinding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        zoomRatio = progress / 100.toFloat()
                        cameraController?.setLinearZoom(zoomRatio)

                        var txtZoomRatio = String.format("%.1f", cameraInfo?.zoomState?.value?.zoomRatio)
                        viewBinding.zoomPer.text= "x${txtZoomRatio}"
                        viewBinding.viewZoomRatio.text = "x${txtZoomRatio}"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })


                cameraController = camera?.cameraControl
                cameraInfo = camera?.cameraInfo
            }catch(exc: Exception){
                Log.e(TAG, "카메라 바인딩 실패.", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    //퍼미션이 제대로 되었는지 체크하기 위한 함수
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCameraId(context: Context, facing: Int) : String{
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        return manager.cameraIdList.first{
            manager
                .getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == facing
        }
    }

    override fun onResume(){
        super.onResume()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onPause(){
        super.onPause()

        cameraExecutor.shutdown()
    }

    //어플이 꺼지게 될 경우에 사용하기 위한 함수
    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }

    //필요로 하는 상수를 한데 모아놓은 객체
    companion object{
        //
        private const val TAG = "proj_Camera"
        //파일 이름 포맷을 나타냄
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        //퍼미션에 대한 코드 번호를 나타냄
        const val REQUEST_CODE_PERMISSIONS = 10
        //필요로 하는 퍼미션을 리스트로 나타냄
        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply{
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        //Camera2
            private fun enumerateCameras(cameraManager: CameraManager) : List<MainActivity.Companion.FormatItem>{
                val availableCameras: MutableList<MainActivity.Companion.FormatItem> = mutableListOf()

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
                        characteristics.get(CameraCharacteristics.LENS_FACING)!!
                    )

                    // Query the available capabilities and output formats
                    val capabilities = characteristics.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                    val outputFormats = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

                    // All cameras *must* support JPEG output so we don't need to check characteristics
                    availableCameras.add(
                        MainActivity.Companion.FormatItem(
                            "$orientation JPEG ($id)", id, ImageFormat.JPEG
                        )
                    )

                    // Return cameras that support RAW capability
                    if (capabilities.contains(
                            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                        outputFormats.contains(ImageFormat.RAW_SENSOR)) {
                        availableCameras.add(
                            MainActivity.Companion.FormatItem(
                                "$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR
                            )
                        )
                    }

                    // Return cameras that support JPEG DEPTH capability
                    if (capabilities.contains(
                            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
                        outputFormats.contains(ImageFormat.DEPTH_JPEG)) {
                        availableCameras.add(
                            MainActivity.Companion.FormatItem(
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

        /** Helper class used as a data holder for each selectable camera format item */
        private data class FormatItem(val title: String, val cameraId: String, val format: Int)
    }
}