package com.example.myapplication.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.activity.BaseActivity.Companion.LOG_TAG
import com.example.myapplication.databinding.ActivityCompareBinding
import com.example.myapplication.util.ImageUtil
import com.example.myapplication.util.SharedPreferencesUtil
import com.example.myapplication.vision.FaceContourGraphic
import com.example.myapplication.vision.GraphicOverlay

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import java.io.IOException


/** 1. 두 표정을 로드
 *  2. 동시에 API 2개 호출
 *  (내일)->
 *  3. 점수화 알고리즘 동작
 *  4. 안내 다이어로그
 * */
class CompareActivity : AppCompatActivity(){
    private lateinit var viewBinding: ActivityCompareBinding

    private lateinit var trnsImgUri : Uri
    private lateinit var newImgUri : Uri

    var mGraphicOverlay: GraphicOverlay? = null


    // High-accuracy landmark detection and face classification
    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    // Or, to use the default option:
    // val detector = FaceDetection.getClient();
    val detector = FaceDetection.getClient(highAccuracyOpts)

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mGraphicOverlay = findViewById(R.id.graphic_overlay)

        val trnsBase64 = SharedPreferencesUtil.getString(getString(R.string.trns_pic)).toString()
        val imageBytes = Base64.decode(trnsBase64, Base64.DEFAULT)
        val trnsBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val newStr = SharedPreferencesUtil.getString(getString(R.string.new_pic)).toString()

        trnsImgUri = ImageUtil.getImageUri(this@CompareActivity, trnsBitmap)
        newImgUri = Uri.parse(newStr)

        Log.d("URIURIURI", trnsImgUri.toString())
        Log.d("URIURIURI", newImgUri.toString())

        //1
        viewBinding.cmpTrnsPicImg.setImageBitmap(trnsBitmap)
        viewBinding.cmpNewPicImg.setImageURI(newImgUri)

        val image_new: InputImage
        val image_trns: InputImage
        try {
            image_new = InputImage.fromFilePath(this@CompareActivity, newImgUri)
            image_trns = InputImage.fromFilePath(this@CompareActivity, trnsImgUri)

            if(image_new == null || image_trns == null) throw java.lang.NullPointerException()

            processInputImage(image_new)
            processInputImage(image_trns)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        //2 TODO 표정유사도 로직 구현
        //  2-1. 특징점 추출 , 벡터값 구하기, 값 도출,
        CoroutineScope(Dispatchers.IO).launch{


        }




//        val newRequestFile = ContentUriRequestBody(this@CompareActivity, newImgUri).toFormData("photo")
//        val trnsRequestFile = ContentUriRequestBody(this@CompareActivity,  trnsImgUri).toFormData("photo")

//        ProgressDialogUtil.showProgressDialog(this@CompareActivity, "API 호출중...")
//        CoroutineScope(Dispatchers.IO).launch{
//            try{
//                 val newRespDeferred  : Deferred<LuxadApiResponse> = async (Dispatchers.IO){ //비동기 1 (suspend)
//                    RetrofitApi.getLuxadService.getEmotionResult(newRequestFile)
//                }
//                val trnsRespDeferred  : Deferred<LuxadApiResponse> = async (Dispatchers.IO){ //비동기 2 (suspend)
//                    RetrofitApi.getLuxadService.getEmotionResult(trnsRequestFile)
//                }
//
//                val newResp = newRespDeferred.await() // 비동기 작업 완료 대기
//                val trnsResp = trnsRespDeferred.await() // 비동기 작업 완료 대기
//
//
//                withContext(Dispatchers.Main) {
//                    ProgressDialogUtil.hideProgressDialog()
//                    // UI 작업 수행 (근데 표정 값으로 null인 것들이 많이 오고, 얼굴 속 장애물까지 파악하진 않음. 시간 약 3초)
//                    viewBinding.root.setBackgroundColor(R.color.black)
//                }
//            }catch (e:Exception){
//                Log.e(TAG, "$TAG Compare작업 실패")
//                e.printStackTrace()
//            }
//        }

        val myListener = View.OnClickListener {
            when{
                (it is Button)->{
                    Log.d("BTN_BTN_", it.id.toString())

                    if(it.id==R.id.cmp_goback){ //이전 액티비티로 이동 -> 다시 사진찍는 것임
                        //TODO 테스트
                        finish() //테스트 요청
                    }
                    else if(it.id == R.id.cmp_gonext){ //표정 연습 다시
                        //TODO 테스트
                        finish() //테스트 요청
//                        facialPracAgain()
                    }else if(it.id == R.id.cmp_app_reset){ // 앱 재시작
                        restartApp() //테스트요청
                    }
                }

            }
        }


        val corr = false
        updateUI(corr)  //정답 여부에 따른 UI 업데이트


        viewBinding.cmpGoback.setOnClickListener(myListener)

        viewBinding.cmpGonext.setOnClickListener(myListener)

        viewBinding.cmpAppReset.setOnClickListener(myListener)
    }

    private fun processInputImage(image: InputImage) {
        val result_new = detector.process(image)
            .addOnSuccessListener { faces -> processFaceContourDetectionResult(faces)} //그림 그려주기?
            .addOnFailureListener { e ->e.printStackTrace() }
    }


    private fun processFaceContourDetectionResult(faces: List<Face>) {
        // Task completed successfully
        if (faces.size == 0) {
            Log.d(LOG_TAG, "MLKIT_FACE_CONTOUR_ERROR")
            return
        }
        mGraphicOverlay!!.clear()
        //간단하게 표시
        for (i in faces.indices) {
            val face: Face = faces[i]
            val faceGraphic = FaceContourGraphic(mGraphicOverlay)
            mGraphicOverlay!!.add(faceGraphic)
            faceGraphic.updateFace(face)
            Log.d("ML_KIT", face.toString())
        }

//        for(face in faces){
//
//        }
    }











    //정답 여부에 따른 UI 업데이트
    private fun updateUI(isCorrect : Boolean) {
        if(isCorrect){ //표정연습이 정답대로 한 경우 -> 점수화
            //1 점수측정
            val score = 100 //TODO 점수측정
            //2 UI 업데이트
            viewBinding.cmpScorePlaceholder.text = "유사도 점수는"
            viewBinding.cmpScore.text = "$score 점"
        }else{ //틀린 경우
            //TODO
            viewBinding.cmpScorePlaceholder.text = "오해 사기 딱이겠는데요!"
            viewBinding.cmpScore.text = "다시 시도해봐요^^"
        }
    }


    //표정연습 다시 하도록
    private fun facialPracAgain() {

    }



    //앱 재시작
    fun restartApp(){
        //SharedPreferencesUtil picture관련한 것들 다 지우기
        SharedPreferencesUtil.removePicRelatedStrings()

        //flag 세팅
        BaseActivity.newPicSession=false

        val intent = Intent(applicationContext, BaseActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }


}