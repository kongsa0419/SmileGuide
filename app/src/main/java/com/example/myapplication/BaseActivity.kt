package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.FileNotFoundException
import java.io.FileOutputStream
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
        //어쩌면... 내 경우에는 단계별로 액티비티를 이동시키니까 이게 필요가 없을 수도 있어
        const val INTENT_CODE_FROM_BASE_TO_CAPTURE = 9120
        const val INTENT_CODE_FROM_CAPTURE_TO_QUIZ = 9230
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


        /* // 필요없을 가능성이 있어서 일단 주석처리
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result:ActivityResult->run {
            //INTENT_CODE_FROM_QUIZ_TO_BASE 가 적절히 비교되는지 확인
            if (result.resultCode == INTENT_CODE_FROM_QUIZ_TO_BASE) {
                    //1 로딩 다이어로그와 함께 각종 API 호출
                    //2 이미지 띄우기 (배경 블러 효과)
                }
            }
        }
        */



        //TODO : sharedPreference에서 quiz->base로 넘어온 경우를 다뤄줌
        // 새로운 액티비티가 만들어져 스택에 쌓일거기 때문에 onCreate() 호출됌
        //1 로딩 다이어로그와 함께 각종 API 호출
        //2 이미지 띄우기 (배경 블러 효과)





        viewBinding.mainPicTrans.alpha = 0.0f

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

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1275,1900))
                .build()

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

        val metadata = ImageCapture.Metadata()
        metadata.setReversedHorizontal(true); // This method will fix mirror issue
        // Create output options object which contains file + metadata

        val outputOptionsBuilder = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (isFacingFront) {
            outputOptionsBuilder.setMetadata(metadata)
        }
        val outputOptions = outputOptionsBuilder.build()


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
                    // 사진 촬칵 하는 효과
                    viewBinding.mainPreviewview.animate().alpha(0f).setDuration(50)
                        .withEndAction { viewBinding.mainPreviewview.alpha = 1f }

                    val msg = "Photo capture succeeded: ${output.savedUri}" //로컬파일위치
                    Log.d(TAG, msg)
                    //content://media/external/images/media/1000004694
                    //val savedUri = output.savedUri ?: Uri.fromFile(File(output.savedUri.toString()))

                    //INFO : 여기서 orig_pic은 intent에 저장하는 거지, sharedPreference에 저장하는 것이 아님
                    val intent = Intent(this@BaseActivity, CaptureResult::class.java).apply {
                        putExtra(getString(R.string.orig_pic), output.savedUri.toString())
                    }
                    startActivity(intent)
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



    /**
    onCreate(): This method is called when the activity is first created. It is where you should perform any initialization tasks, such as setting up the UI components or initializing any data structures.

    onStart(): This method is called when the activity becomes visible to the user. At this stage, the activity is not yet interactive, but it is visible on the screen.
        * onRestoreInstanceState()는 여기쯤 위치함
        * 메모리릭 등으로 강제종료 된 경우에 파괴됐다가 다시 초기화됐을때 호출
        * 따라서 맨 처음에는 이것이 호출되진 않음
        * saved state는 Bundle 객체에 저장됌
        *
    onResume(): This method is called when the activity becomes fully interactive and is ready to accept user input. This is where you should start any animations, play sounds or start any other background tasks that should run while the activity is in the foreground.

    onPause(): This method is called when the activity is no longer in the foreground and is partially obscured by another activity. At this stage, you should pause any ongoing animations, release any system resources that are not needed, and save any data that needs to be persisted.
        * save necessary state here for onRestoreInstanceState()
        *
    onStop(): This method is called when the activity is no longer visible to the user. At this stage, you should release any resources that are not needed and stop any background tasks that are running.

    onRestart(): This method is called when the activity is about to be restarted after it was stopped.

    onDestroy(): This method is called when the activity is being destroyed, either because the user has navigated away from it or because the system needs to reclaim resources. At this stage, you should release any system resources that are being used, such as open files or network connections.
     */

    // INFO : 다음 액티비티에서 finishActivity()로 이 액티비티에 되돌아왔다면,
    // INFO : onResume()이 호출되게 된다. 따라서 액티비티에서 intent로 넘어갈때, onPause()에서 저장해줄 것들을 저장해줘야한다.
    // INFO : onCreate() or onRestoreInstanceState() = 액티비티가 destroyed된 후 처음으로 생겨났는지, destroyed된 후 재창출된 것인지






    // 기존 액티비티 스택에 있던 BaseActivity를 스택 맨 위로 올리고 onResume()호출
    // onNewIntent() → onResume()
    // TODO: QuizActivity에서 넘어온 사진을 흐릿하게 바꾸어 띄워주기
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            var imgPath =
                SharedPreferencesUtil.getString(getString(R.string.orig_pic_web_filename)) //firebase url
            Glide.with(this)
                .load(imgPath)
                .into(viewBinding.mainPicTrans)

            viewBinding.mainPicTrans.alpha = 1.0f

            Toast.makeText(this, imgPath, Toast.LENGTH_SHORT).show()
            val data = intent.getStringExtra(getString(R.string.trns_pic))

            if (data != null) {
            }
        }
    }





    /**이동한 액티비티에서 finish로 액티비티를 끝내는 경우, onResume()으로 돌아오게 됌
     * */
    //TODO finish()인 경우 처리 :: result -> base
    // finish()로 왔을 경우에 그대로 카메라만 쓰면 되는거 아닌가? 다이어로그도 당연히 안뜰거고
    override fun onResume() {
        super.onResume()
        viewBinding.mainPicTrans.alpha = 1.0f
        /*
        val origPicture = SharedPreferencesUtil.getString(getString(R.string.orig_pic))

        if(origPicture != null){
            //TODO : 아무것도 할게 없지않나?
            //1 API call(이미지 변환, 백그라운드 제거)
            //2 SharedPreference에 저장해둔 바뀐 이미지를 카메라 위에 띄우기
        }else{
            //TODO 확인 후 삭제 요청
            Log.d(getString(R.string.log_key_universal), "표정변환 하기로 한 파일의 URL이 SharedPreference에 저장되어있지 않음. 다른 액티비티에서 저장이 안 됐거나 로직이 이상할 수 있음.")
        }
        */
    }





    // Intent로 다음 액티비티로 넘어가기 전에, 저장해둬야할 것들을 저장하는 구간
    override fun onPause() {
        super.onPause()
    }





    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

