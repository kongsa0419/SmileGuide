package com.example.myapplication.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.activity.BaseActivity.Companion.LOG_TAG
import com.example.myapplication.databinding.ActivityCompareBinding
import com.example.myapplication.util.ImageUtil
import com.example.myapplication.util.ImageUtil.getBitmapFromUri
import com.example.myapplication.util.ProgressDialogUtil
import com.example.myapplication.util.SharedPreferencesUtil
import com.example.myapplication.vision.FaceContourGraphic
import com.example.myapplication.vision.GraphicOverlay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.tasks.await
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt


/** 1. 두 표정을 로드
 *  2. 동시에 API 2개 호출
 *  (내일)->
 *  3. 점수화 알고리즘 동작
 *  4. 안내 다이어로그
 * */

class CompareActivity : AppCompatActivity(){
    private lateinit var viewBinding: ActivityCompareBinding

    private lateinit var mNewImgView : ImageView
    private lateinit var mTrnsImgView : ImageView

    private lateinit var showBtn : Button
    
    //화면비율 (가로,세로)
    lateinit var mViewResolution : Pair<Int,Int>
    lateinit var mBitmapResolution : Pair<Int,Int>
    
    private lateinit var trnsImgUri : Uri
    private lateinit var newImgUri : Uri

    var trnsVectorByContour : MutableMap<Int, MutableList<Float>> = mutableMapOf()
    var newVectorByContour : MutableMap<Int, MutableList<Float>> = mutableMapOf()

    var scaleX : Float = 1f
    var scaleY : Float = 1f

    var trnsEulerY : Float = 0.0f
    var newEulerY : Float = 0.0f

    var diffRec : MutableList<Float> = mutableListOf()
    val faceContourTypes : List<String> = listOf("null",
        "얼굴윤곽", "왼쪽 눈썹 (위쪽)", "왼쪽 눈썹 (아래)",
        "오른쪽 눈썹 (상단)", "오른쪽 눈썹 (아래)", "왼쪽 눈",
        "오른쪽 눈", "윗입술 (상단)",  "윗입술 (하단)",  "아랫입술 (상단)",
        "아랫입술 (하단)",   "콧날","코 밑부분") //size: 14 , (0번 쓰레기값) + 13개

    lateinit var resultExpl : String

    val channel = Channel<List<Face>>()

    var showPointsMode:Boolean = false
    var newShowPreps : Pair<List<Face>, GraphicOverlay> ?= null
    var trnsShowPreps : Pair<List<Face>, GraphicOverlay> ?= null



    var similarityScore = 0; //유사도 점수

    var trnsGraphicOverlay: GraphicOverlay? = null
    var newGraphicOverlay: GraphicOverlay? = null

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

        trnsGraphicOverlay = findViewById(R.id.trns_graphic_overlay)
        newGraphicOverlay = findViewById(R.id.new_graphic_overlay)

        /**실전용*/
//        val trnsBase64 = SharedPreferencesUtil.getString(getString(R.string.trns_pic)).toString()
//        val imageBytes = Base64.decode(trnsBase64, Base64.DEFAULT)
//        val trnsBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//        val newStr = SharedPreferencesUtil.getString(getString(R.string.new_pic)).toString()
//        trnsImgUri = ImageUtil.getImageUri(this@CompareActivity, trnsBitmap)
//        newImgUri = Uri.parse(newStr)

        //테스트용
        trnsImgUri = Uri.parse("content://media/external/images/media/1000005820") // 입벌리고 미소 지은거
        newImgUri =  Uri.parse("content://media/external/images/media/1000005819") // 찌뿌등(화낸거 같음)

        Log.d("URIURIURI", trnsImgUri.toString())
        Log.d("URIURIURI", newImgUri.toString())





        val image_new: InputImage
        val image_trns: InputImage
        try{
            image_new = InputImage.fromFilePath(this@CompareActivity, newImgUri)
            image_trns = InputImage.fromFilePath(this@CompareActivity, trnsImgUri)

            if(image_new == null || image_trns == null) throw java.lang.NullPointerException()


            //----------------------------------------------//
            mNewImgView = viewBinding.cmpNewPicImg
            mTrnsImgView = viewBinding.cmpTrnsPicImg

            mBitmapResolution =  Pair(image_new.width, image_new.height)
            mViewResolution = Pair(mNewImgView.layoutParams.width , mNewImgView.layoutParams.height)

            val scaleX : Float = mBitmapResolution.first/mViewResolution.first.toFloat()
            val scaleY : Float = mBitmapResolution.second/mViewResolution.second.toFloat()
            val scaleFactor : Float = Math.max(scaleX,scaleY)
            this.scaleX = scaleX
            this.scaleY = scaleY
            Log.e(LOG_TAG, "scaleX : ${scaleX}, scaleY: ${scaleY}, scaleFactor: ${scaleFactor}")

            var newBit = getBitmapFromUri(this@CompareActivity, newImgUri)
            var trnsBit = getBitmapFromUri(this@CompareActivity, trnsImgUri)

            val newResizedBitmap = Bitmap.createScaledBitmap(
                newBit,
                (newBit.getWidth() / scaleFactor).toInt(),
                (newBit.getHeight() / scaleFactor).toInt(),
                true
            )
            val trnsResizedBitmap = Bitmap.createScaledBitmap(
                trnsBit,
                (trnsBit.getWidth() / scaleFactor).toInt(),
                (trnsBit.getHeight() / scaleFactor).toInt(),
                true
            )

            if(scaleFactor!=null) FaceContourGraphic.globalScaleFactor = scaleFactor


            mNewImgView.setImageBitmap(newResizedBitmap)
            mTrnsImgView.setImageBitmap(trnsResizedBitmap)
            //----------------------------------------------//

            ProgressDialogUtil.showProgressDialog(this@CompareActivity, "특징점 추출중...")
            CoroutineScope(Dispatchers.Default).launch{
                val n = lifecycleScope.async {
                    processInputImage(image_new, newGraphicOverlay!!, 0)
                }
                val t = lifecycleScope.async {
                    processInputImage(image_trns,trnsGraphicOverlay!!, 1)
                }
                n.await() //끝날때까지 기다리기
                t.await() //끝날때까지 기다리기
                withContext(Dispatchers.Main){
                    val score = async{calcSimilarity()}.await()
                    similarityScore = score
                    updateUI(score)  //정답 여부에 따른 UI 업데이트
                    ProgressDialogUtil.hideProgressDialog()
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }







        val myListener = View.OnClickListener {
            when{
                (it is View)->{
                    Log.d("BTN_BTN_", it.id.toString())

                    if(it.id==R.id.cmp_goback){ //이전 액티비티로 이동 -> 다시 사진찍는 것임
                        finish() //테스트 요청
                    }
                    else if(it.id == R.id.cmp_gonext){ //표정 연습 다시
                        finish() //테스트 요청
                    }else if(it.id == R.id.cmp_app_reset){ // 앱 재시작
                        restartApp() //테스트요청
                    }
                }

            }
        }


        showBtn = findViewById(R.id.cmp_btn_show_points)
        showBtn.setOnClickListener{
            Log.e(LOG_TAG, "AAAAAAAAAAAAAAAAAA")
            try {
                if (newShowPreps == null || trnsShowPreps == null) throw Exception()
                it.isEnabled = !it.isEnabled
                showPointsMode = !showPointsMode
                CoroutineScope(Dispatchers.Default).launch{
                    val a = lifecycleScope.async { showToggle(newShowPreps!!.first, newShowPreps!!.second) }
                    val b = lifecycleScope.async  { showToggle(trnsShowPreps!!.first, trnsShowPreps!!.second)}
                    a.await()
                    b.await()
                    withContext(Dispatchers.Main){
                        if(showPointsMode){
                            viewBinding.cmpResultExpl.visibility = View.VISIBLE //정답 여부에 따른 UI 업데이트
                        }else{
                            viewBinding.cmpResultExpl.visibility = View.INVISIBLE
                        }
                        it.isEnabled = !it.isEnabled
                    }
                }
                Log.e(LOG_TAG, "토글버튼 들어왔음.")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        viewBinding.cmpGoback.setOnClickListener(myListener)

        viewBinding.cmpGonext.setOnClickListener(myListener)

        viewBinding.cmpAppReset.setOnClickListener(myListener)
    }





    //TODO 표정유사도 로직 구현
    // 특징점 추출 , 벡터값 구해져 있음.
    // 일단 러프하게 계산해보자
    private fun calcSimilarity() : Int{
        val cntContour = 13
        var isComparable = true
        if(!(newEulerY in -12.0f .. 12.0f) || !(trnsEulerY in -12f .. 12f)){
            Log.e(LOG_TAG, "두 사진이 정면을 바라보고 있지 않습니다.")
            Log.e(LOG_TAG, "newEulerY: $newEulerY | trnsEulerY: $trnsEulerY")
            //TODO UI에 이런 문구를 주는게 나을지 확인
        }
        if(trnsVectorByContour==null || newVectorByContour==null){
            Log.e(LOG_TAG, "제대로 특징 값이 탐색되지 않았습니다.")
            isComparable = false
        }
        var score : Float = 100f // 최종 합산 점수
        diffRec.add(0f) //처음 Contour 가 Box여서 건너뜀
        for(i in 0..cntContour){
            val trnsVector = trnsVectorByContour.get(i)
            val newVector = newVectorByContour.get(i)
            if(trnsVector == null || newVector == null) continue //0번이 BOX여서 건너뜀
            if(trnsVector?.size != newVector?.size){
                Log.e(LOG_TAG, "이상하다 Contour개수가 안맞지?")
                continue;
            }
            /** FACE_OVAL은 비교를 건너뛸까? 고려해볼 필요 있겠다? */
            val cnt = trnsVector!!.size
            var sub = 0.0f
            for(j in 0 until trnsVector!!.size){
                val t = trnsVector.get(j)
                val n = newVector!!.get(j)
                val isDiff = (t>0) xor (n>0) //두 수의 부호가 다른지 확인
                var weight = if(isDiff){ 1.4f/cnt  } else{ 1.0f/cnt } //Contour당 PointF 개수가 많을수록 차이는 적어야함. 개수가 적을수록 차이는 클 가능성이 높음. --> 1/N

                var norm = Math.abs(t-n)
                sub += (weight * norm)
            }

            val rec = roundFloat(sub)
            Log.e(LOG_TAG, "Contour${(i+1)}: ${rec} 점을 차감합니다. ")
            diffRec.add(rec) //특정 컨투어 마다의 유사도 불일치 정도를 저장
            score = score - sub
        }
        return score.roundToInt()
    }







    private suspend fun processInputImage(image: InputImage, graphicOverlay: GraphicOverlay, flag : Int) {
        withContext(Dispatchers.IO){
            detector.process(image)
                .addOnSuccessListener { faces ->
                    lifecycleScope.launch {
                        channel.send(faces)
                    }
                    processFaceContourDetectionResult(faces, graphicOverlay, flag)
                } //그림 그려주기?
                .addOnFailureListener { e ->e.printStackTrace() }
        }.await()
    }


    private fun processFaceContourDetectionResult(faces: List<Face>, graphicOverlay:GraphicOverlay, flag : Int) {
        // Task completed successfully
        if (faces.size == 0) {
            Log.d(LOG_TAG, "MLKIT_FACE_CONTOUR_ERROR")
        }

        // 주어진 사진의 각 Contour를 돌면서 두 포인트간의 기울기를 측정하는 함수 호출
        var gradientMapByContours : MutableMap<Int, MutableList<Float>>  = getGradientListByContours(faces)
        if(flag == 0){ /**NEW*/
            newVectorByContour = gradientMapByContours
            newEulerY = faces[0].headEulerAngleY
            newShowPreps = Pair(faces, graphicOverlay)
        }else if(flag == 1){ /**TRNS*/
            trnsVectorByContour = gradientMapByContours
            trnsEulerY = faces[0].headEulerAngleY
            trnsShowPreps = Pair(faces, graphicOverlay)
        }
    }

    private fun getGradientListByContours(faces: List<Face>) : MutableMap<Int, MutableList<Float>> {
        var ret : MutableMap<Int, MutableList<Float>> = mutableMapOf()
        for(face in faces){
            val cntContour = 13
            for(i in 0 until cntContour){
                val contour = face.getContour(i)
                if(contour==null) continue
                val points = contour.points
                var linears : MutableList<Float> = mutableListOf<Float>()
                for(i in 0 until contour.points.size-1){
                    try{
                        val curr = points[i]
                        val next = points[i+1]
                        val dx = next.x - curr.x
                        val dy = next.y - curr.y
                        if(dy==0f) {
                            linears.add(0f)
                            continue
                        }
                        val lin  = dx/dy
                        linears.add(lin)
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
                if(linears.size == points.size-1){
                    Log.d(LOG_TAG, "잘돌아가고있네 점수측정 잘 되겠어.")
                }
                ret.putIfAbsent(i, linears)
            }
        }
        return ret
    }




    //정답 여부에 따른 UI 업데이트
    private fun updateUI(score: Int) {
        Log.d(LOG_TAG, "$score 점이라서 이렇게 결과가 정해집니다.")
        viewBinding.cmpScorePlaceholder.text = "유사도 점수는"
        viewBinding.cmpScore.text = "$score 점"

        //----
        try{
            Log.e(LOG_TAG, "diffRec.size.toString(): "+diffRec.size.toString())
            val strBuilder = java.lang.StringBuilder()
            val ctrs = faceContourTypes.size-1 //13
            for(i in (1 until ctrs)){
                strBuilder.append("${faceContourTypes[i]}: ${diffRec[i-1]}\n")
            }
            strBuilder.append("${faceContourTypes[ctrs]}: ${diffRec[ctrs-1]}\n")
            resultExpl = strBuilder.toString()
            viewBinding.cmpResultExpl.text = resultExpl
        }catch (e:Exception){
            e.printStackTrace()
        }




//        val standard = 80
//        if(score >= standard){ //표정연습이 정답대로 한 경우 -> 점수화
//
//        }else{ //틀린 경우
//
//        }
    }


    //앱 재시작
    fun restartApp(){
        //SharedPreferencesUtil picture관련한 것들 다 지우기
        // 어차피 덮여 쓰일텐데 이게 필요할까?
        // SharedPreferencesUtil.removePicRelatedStrings()

        //flag 세팅
        BaseActivity.newPicSession=false

        val intent = Intent(applicationContext, BaseActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    suspend fun showToggle(faces: List<Face>, graphicOverlay:GraphicOverlay){
        if(showPointsMode){
            /** graphicOverlay로 그래픽 조절 필요 */
            for (i in faces.indices) { // 각 얼굴마다 (1)
                val face: Face = faces[i]
                graphicOverlay.setScaleY(scaleY)
                graphicOverlay.setScaleX(scaleX)
                val faceGraphic = FaceContourGraphic(graphicOverlay)
                graphicOverlay!!.add(faceGraphic)
                faceGraphic.updateFace(face)
                Log.d("ML_KIT", face.toString())
            }
        }else{
            graphicOverlay!!.clear()
        }
    }


    fun roundFloat(x:Float, dec:Int?=2) : Float{
        val square = (10f.pow(dec!!))
        return (round(x*square) /square)
    }

}