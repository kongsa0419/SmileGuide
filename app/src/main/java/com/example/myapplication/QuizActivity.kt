package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityQuizBinding
import com.example.myapplication.dto.AilabApiResponse
import com.example.myapplication.dto.QuizSetItem
import com.example.myapplication.retrofit.RetrofitApi
import com.example.myapplication.util.ContentUriRequestBody
import com.example.myapplication.util.ImageUtil
import com.example.myapplication.util.ProgressDialogUtil.hideProgressDialog
import com.example.myapplication.util.ProgressDialogUtil.showProgressDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

//TODO(5/10) : 인텐트 화면전환 구현
// 뒤로가기 버튼 처리
// 맞거나 틀렸을 때 해설(xml) 띄워주기 + 다음 액티비티로 넘어가기
class QuizActivity : AppCompatActivity() {
    companion object{
        /** 상수선언 : 퀴즈 정답이 틀렸는지 맞았는지*/
        const val CIRCLE_EFFECT = 0
        const val CROSS_EFFECT = 1

        /** change face options */
        const val BIG_LAUGH = 0 //default
        const val POUTING = 1
        const val FEEL_SAD = 2
        const val SMILE = 3
        const val OPENING_EYES = 4

        const val QUIZSET_FILE_PATH = "app/src/main/assets/"
    }


    private lateinit var dialogLayout: View //custom dialog
    private lateinit var viewBinding: ActivityQuizBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> //

    private lateinit var dialog: AlertDialog //dialog

    private lateinit var imgUri : Uri
    private var quizIndex : Int = 0
    private var quizItem : QuizSetItem ?= null




    private lateinit var btn1 :  Button
    private lateinit var btn2 :  Button
    private lateinit var btn3 :  Button
    private lateinit var btn4 :  Button
    private lateinit var btnGroup : List<Button>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        imgUri = Uri.parse(intent.getStringExtra("imgUri").toString()) //.toString()

        /* // 쓰지 않아도 될듯
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == INTENT_CODE_FROM_CAPTURE_TO_QUIZ){ // capture->quiz로 넘어온 인텐트

            }
        }*/



        viewBinding.quizBtnBack.setOnClickListener(){ finish() }
        viewBinding.quizBtnGallery.setOnClickListener(){/*TODO:*/ }
        quizInit()
        setQuizUI()
    }




    private fun quizInit(){
        val quizSetList : List<QuizSetItem> = parseJsonFile(applicationContext, "quiz_data.json")
        val quizSetCnt = quizSetList.size
        quizIndex =  (0..quizSetCnt-1).shuffled().first()
        Log.d("TAG", "quizIndex : $quizIndex, quizSetCnt : $quizSetCnt")

        quizItem = quizSetList[quizIndex]
    }

    private fun parseJsonFile(context: Context, fileName: String): List<QuizSetItem> {
        val jsonString = readJsonFileFromAssets(context, fileName)
        return if (jsonString != null) {
            val gson = Gson()
            val type = object : TypeToken<List<QuizSetItem>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }


    fun readJsonFileFromAssets(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    fun setQuizUI() {

        viewBinding.quizQuestionStatement.text = quizItem!!.problem //1) 문제
        viewBinding.quizQuestionSection.text = quizItem!!.context //2)상황 설명

        btn1 = viewBinding.quizAnswerBtn1
        btn2 =viewBinding.quizAnswerBtn2
        btn3 =viewBinding.quizAnswerBtn3
        btn4 =viewBinding.quizAnswerBtn4
        btnGroup = listOf(btn1,btn2,btn3,btn4)

        btnGroup.forEachIndexed { index, button ->
            button.text = quizItem!!.options[index]
        }

        val ans = quizItem!!.answer

        val onClickListener = View.OnClickListener {
            if ((it as TextView).text == ans) {
                /** 정답을 맞춘 경우
                 *  1) motion effect (동그라미 효과)
                 *  2) dialog 띄워주기
                 * */
                drawMotionEffect(it, CIRCLE_EFFECT) //1
                inflateDialog(true) //2
            } else {
                /** 틀린 걸 골랐을 경우
                 *  1) motion effect (틀렸다는 효과)
                 *  2) dialog 띄워주기
                 * */
                drawMotionEffect(it, CROSS_EFFECT) //1
                inflateDialog(false) //2
            }
        }

        btnGroup.forEach { it.setOnClickListener(onClickListener) }
    }









    private fun inflateDialog(b: Boolean) {
        //1 다이어로그를 루트뷰에 추가
        dialogLayout = LayoutInflater.from(this)
            .inflate(R.layout.dialog_quiz_answer, null)

        //2 각 뷰들의 text를 설정해주고, setOnclickListener 설정
        initDialogComponents(dialogLayout, b)

        // Build the dialog
        val builder = AlertDialog.Builder(this)
            .setView(dialogLayout)

        // Show the dialog
        dialog = builder.show()
        // 다이얼로그를 닫았을 때 호출되는 콜백 정의
        dialog.setOnDismissListener {
            // 액티비티를 다시 생성하지 않도록 설정
            dialog.dismiss()
        }

        // Set the dialog size
        val window = dialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


    // TODO : 3 문제가 틀리고 맞았을 경우에 따라서
    //  각 뷰들의 text를 설정해주고, setOnclickListener 설정
    private fun initDialogComponents(dialogLayout: View?, b: Boolean) {
        val resultStatusTV : TextView = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_status)
        val resultExplTV : TextView = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_expl)
        val nextBtn : Button = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_btn_next)

        // answer


        //code refactoring 필요
        if(b){ //정답일 경우 fixme 표정연습하는 다음 단계로 넘어감
            //1 텍스트 설정
            resultStatusTV.text = "정답! 훌륭합니다!"
            resultExplTV.text = quizItem!!.explanation
            nextBtn.text = "표정연습 하러가기"

            //TODO("코루틴을 적용해서 이미지 만들어놓기, 로직 정리")
            //2 setOnclickListener 설정
            nextBtn.setOnClickListener{
                dialog.dismiss()

                /*
                //Create a Channel to communicate between the apiThread and the mainThread
                val channel = Channel<Pair<File, /*base64-encoded*/String>>()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Step 1: Call the first API on the apiThread
                        showProgressDialog(applicationContext, "[API call]\nbackground-removal")
                        val imageUrl : String = withContext(Dispatchers.IO){
                            callBackgroundRemove()
                        }
                        hideProgressDialog()

                        // Step 2: Convert image URL to File format and call the second API on the apiThread
                        showProgressDialog(applicationContext, "[API call]\nchange-facial-expr")
                        val imageFile : File = withContext(Dispatchers.IO) {
                            convertToFile(imageUrl!!)
                        }


                        val secondApiResponse : String  = withContext(Dispatchers.IO) {
                            callChageFacialExpr(imageFile, BIG_LAUGH) /*Base64-encoded*/
                        }
                        hideProgressDialog()

                        //await()?
                        // Send the result back to the mainThread via the channel
                        channel.send(Pair(imageFile, secondApiResponse))
                    } catch (e: Exception) {
                        // Handle any exceptions that occurred during API calls
                        e.printStackTrace()
                        Log.e("TAG",e.printStackTrace().toString())
                    }
                }*/


                val intent : Intent = Intent(this@QuizActivity, BaseActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // 기존에 존재하던 액티비티라면 해당 액티비티로 돌아옴
                //TODO: 변환된 사진 보내주기 (well...SharedPreference에 저장되어 있지 않나?) 확인해야됌
                intent.putExtra(getString(R.string.trns_pic/**고칠 것*/), "임의값")
                startActivity(intent)
            }

        }else{ // 틀렸을 경우 TODO 새롭게 다른 문제로 버튼 텍스트들을 초기화
            //1 텍스트 설정
            resultStatusTV.text = "오답! 틀렸습니다!"
            resultExplTV.visibility = View.GONE //getString(R.string.Lorem_Ipsum)
            nextBtn.text = "퀴즈 재도전하기"
            //2 setOnclickListener 설정
            nextBtn.setOnClickListener{
                dialog.dismiss()
                // Remove overlay layout from content view
                val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                rootView.removeView(dialogLayout)
            }
        }
    }

    suspend private fun convertToFile(imageUrl: String /*url->File*/) : File {
        return ImageUtil.downloadImage(imageUrl)
    }

    suspend private fun callChageFacialExpr(file:File, opt: Int ?= 0) : String{
        return ""
    }


    suspend fun callBackgroundRemove(): String {
        lateinit var ret : String //imgBackX
        val requestFile = ContentUriRequestBody(applicationContext, imgUri).toFormData()

        val option ="whiteBK"
        val requestOption = option.toRequestBody("multipart/form-data".toMediaType())


        val call = RetrofitApi.getAilabtoolsService.getBackRmvdImg(requestFile, requestOption)
        call.enqueue(object: Callback<AilabApiResponse> {
            override fun onResponse(call: retrofit2.Call<AilabApiResponse>, response: Response<AilabApiResponse>){
                Log.d("TAG", "api 호출 성공 (네트워크 통신까진 됐음)")
                if(response.isSuccessful)
                {
                    Log.d("TAG", "이미지 POST api 성공 (Ailabtools)" + response.body()!!.data!!.image_url.toString())
                    ret = response.body()!!.data!!.image_url.toString() //img_url
                }else{
                    Log.d("TAG", "이미지 POST api 실패 (Ailabtools, 코드 로직 오류)" + response.body()!!.toString())
                }
            }

            override fun onFailure(call: retrofit2.Call<AilabApiResponse>, t: Throwable)
            {
                Log.e("TAG", t.message.toString()) // API 요청 실패 시의 처리
                Log.d("TAG", "api 호출 실패 (네트워크 통신 실패)")
            }
        })
        if(ret==null) throw IllegalArgumentException("")
        else return ret as String
    }



    //완성!
    //INFO : top, right, left, bottom 등은 View 부모의 x,y를 기준으로 좌표를 주지, 스크린의 절대적인 좌표가 아닌듯
    private fun drawMotionEffect(it: View?, shape: Int) {
        val bitmap = Bitmap.createBitmap(it?.width!!, it?.height!!, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.RED // 동그라미 색상
            style = Paint.Style.STROKE // 동그라미 윤곽선 스타일
            strokeWidth = 10f // 동그라미 윤곽선 두께
        }

        when (shape) {
            CIRCLE_EFFECT -> {
                canvas.drawCircle(it?.width!! / 2f, it?.height!! / 2f, it?.width!!/6f, paint) // 동그라미 그리기
                it?.foreground = BitmapDrawable(resources, bitmap)
                Log.d("CIRCLE_EFFECT","CIRCLE_EFFECT")
            }
            CROSS_EFFECT -> {
                canvas.drawLine(it!!.width.toFloat()/5, it!!.height.toFloat()/5, it!!.width.toFloat()*4/5, it!!.height.toFloat()*4/5, paint)
                it!!.foreground = BitmapDrawable(resources, bitmap)
                Log.d("CROSS_EFFECT","CROSS_EFFECT")
            }
        }
    }




    // Intent로 다음 액티비티로 넘어가기 전에, 저장해둬야할 것들을 저장하는 구간
    override fun onPause() {
        super.onPause()
        //TODO 기존 버튼에 텍스트 대입, 틀렸던 모먼트 등 유지
    }





    // 다시 이 액티비티로 화면이 돌아왔을때 저장해뒀던 텍스트를 놔야함
    override fun onResume() {
        super.onResume()
        //TODO onPause()에서 저장해뒀던 것들을 다시 불러와서 입력
    }





    override fun onDestroy() {
        super.onDestroy()
        //TODO onPause()에서 저장해뒀던 것들을 삭제
    }




}
