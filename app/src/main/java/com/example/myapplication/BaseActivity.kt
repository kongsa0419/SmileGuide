package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//NOTI @ref : [https://github.com/android-academy-minsk/CameraX/blob/master/app/src/main/java/com/psliusar/nicolas/camera/LuminosityAnalyzer.kt]
typealias LumaListener = (luma: Double) -> Unit
class BaseActivity : AppCompatActivity() {
    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }


    //members
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewBinding: ActivityMainBinding

    companion object{
        
        //액티비티 간에 스위치, 정보교환을 위한 콜백 코드
        const val INTENT_CODE_FROM_CAPTURE_TO_BASE = 9210
        const val INTENT_CODE_FROM_CAPTURE_TO_QUIZ = 9230
        const val INTENT_CODE_FROM_QUIZ_TO_QUIZ = 9330
        const val INTENT_CODE_FROM_QUIZ_TO_BASE = 9310

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }



    //private var imageCapture: ImageCapture? = null
    private var imageCapture: ImageCapture? = null

//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    private var isFacingFront = true




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        //SharedPreference를 앱 전체에서 사용가능
        SharedPreferencesUtil.init(this);

        //ActivityMainBinding : activity_main.xml에 연결해줌
        viewBinding = ActivityMainBinding.inflate(layoutInflater) 
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }



        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result:ActivityResult->run {//TODO 구현
                if (result.resultCode == INTENT_CODE_FROM_CAPTURE_TO_BASE) {
                    // 액티비티 재시작 (사진 재촬영 기회 제공) => OK
                } else if (result.resultCode == INTENT_CODE_FROM_QUIZ_TO_BASE) {
                    //1 로딩창 + 각종 API 호출
                    //2 이미지 띄워주기
                }
            }
        }


        // Set up the listeners for take photo and video capture buttons
        //viewBinding.mainPreviewview
        viewBinding.mainShutter.setOnClickListener { takePhoto() }
        viewBinding.mainShutter.setOnLongClickListener { captureVideo(); true;}
        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        viewBinding.mainCameraFlip.setOnClickListener { flipCamera() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        showInitialDialog()
    }

    private fun flipCamera() {
        isFacingFront = !isFacingFront
        startCamera()
    }

    private fun showInitialDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("만나서 반갑습니다!").setMessage("진행에 앞서 당신의 사진을 부탁드려요^^")
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {}, ContextCompat.getMainExecutor(this))

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.mainPreviewview.surfaceProvider)
                }

            //INFO: 머신러닝을 위한 코드 -> 실행시키려면 밑에 생명주기에 묶어둔, 주석처리한 imageAnalyzer를 풀어주면 됌
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        Log.d(TAG, "Average luminosity: $luma")
//                    })
//                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = if(isFacingFront) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }else{
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture/*, imageAnalyzer*/)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = "SmileGuide" + SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                //put(MediaStore.Images.Media.RELATIVE_PATH, "{}")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // NOTI: 사진 촬칵 하는 효과
                    // preview 대신에 mainShutter써봐
                    viewBinding.mainPreviewview.animate().alpha(0f).setDuration(50)
                        .withEndAction { viewBinding.mainPreviewview.alpha = 1f }

                    val msg = "Photo capture succeeded: ${output.savedUri}" //로컬파일위치
                    //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    //content://media/external/images/media/1000004694
                    //val savedUri = output.savedUri ?: Uri.fromFile(File(output.savedUri.toString()))

                    //TODO : intent 처리 with registerForActivityResult
                    //INFO : 여기서 orig_pic은 intent에 저장하는 거지, sharedPreference에 저장하는 것이 아님
                    val intent = Intent(this@BaseActivity, CaptureResult::class.java).apply {
                        putExtra(getString(R.string.orig_pic), output.savedUri.toString())
                    }
                    activityResultLauncher.launch(intent)
                }
            }
        )
    }

    private fun captureVideo() {}


    @SuppressLint("MissingSuperCall")
    //NOTI : super 무시해도 동작 (빨간줄 무시하셈)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    /**이동한 액티비티에서 finish로 액티비티를 끝내는 경우, onResume()으로 돌아오게 됌*/
    override fun onResume() {
        super.onResume()
        val origPicture = SharedPreferencesUtil.getString(getString(R.string.orig_pic))

        if(origPicture != null){
            //TODO :
            //1 API call(이미지 변환, 백그라운드 제거)
            //2 바뀐 이미지를 카메라 위에 띄우기
        }else{
            //TODO 확인 후 삭제 요청
            Log.d(getString(R.string.log_key_universal), "표정변환 하기로 한 파일의 URL이 SharedPreference에 저장되어있지 않음. 다른 액티비티에서 저장이 안 됐거나 로직이 이상할 수 있음.")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

