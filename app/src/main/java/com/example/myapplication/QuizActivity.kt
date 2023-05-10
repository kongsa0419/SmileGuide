package com.example.myapplication

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_CAPTURE_TO_QUIZ
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_QUIZ_TO_BASE
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_QUIZ_TO_QUIZ
import com.example.myapplication.databinding.ActivityQuizBinding
import org.w3c.dom.Text

//TODO(5/10) : 인텐트 화면전환 구현
// 뒤로가기 버튼 처리
// 맞거나 틀렸을 때 해설(xml) 띄워주기 + 다음 액티비티로 넘어가기
class QuizActivity : AppCompatActivity() {
    companion object{ /** 상수선언 */
        const val CIRCLE_EFFECT = 0
        const val CROSS_EFFECT = 1
    }

    private lateinit var dialogLayout: View //custom dialog
    private lateinit var viewBinding: ActivityQuizBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> //

    private lateinit var dialog: AlertDialog //dialog


    private lateinit var btn1 :  Button
    private lateinit var btn2 :  Button
    private lateinit var btn3 :  Button
    private lateinit var btn4 :  Button
    private lateinit var ansButtons : Array<Button>

    private var isBtnClickable: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        //TODO:
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == INTENT_CODE_FROM_QUIZ_TO_QUIZ){
                //restart()? activity?
            }
        }

        init() // onClickListener(), ... 등 따로 빼냄
    }


    fun init(){
        viewBinding.quizBtnBack.setOnClickListener(){ finish() }
        viewBinding.quizBtnGallery.setOnClickListener(){/*TODO:*/ }
        initBtns()
    }



    fun initBtns(){
        btn1 = viewBinding.quizAnswerBtn1
        btn2 = viewBinding.quizAnswerBtn2
        btn3 = viewBinding.quizAnswerBtn3
        btn4 = viewBinding.quizAnswerBtn4

        isBtnClickable = true

        // generate a random number between 0 and 3 to choose the answer button
        val answerIndex = (0..3).random()
        //TODO: answerIndex에 정답 텍스트 세팅

        ansButtons = arrayOf(btn1, btn2, btn3, btn4)

        // set the OnClickListener for each button (iterator)
        for (i in ansButtons.indices) {
            ansButtons[i].setOnClickListener {
                if (i == answerIndex) {
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

                // 다른 버튼 모두 UnClickable
                isBtnClickable = false
                updateBtnClickable(ansButtons)
            }
        }
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

        // Set the dialog size
        val window = dialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )



        }


    // TODO : 3 각 뷰들의 text를 설정해주고, setOnclickListener 설정
    private fun initDialogComponents(dialogLayout: View?, b: Boolean) {
        val resultStatusTV : TextView = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_status)
        val resultExplTV : TextView = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_expl)
        val nextBtn : Button = dialogLayout!!.findViewById<Button>(R.id.dialog_quiz_result_btn_next)


        //code refactoring 필요
        if(b){ //정답일 경우 fixme 표정연습하는 다음 단계로 넘어감
            //1 텍스트 설정
            resultStatusTV.setText("정답! 훌륭합니다!")
            resultExplTV.setText(getString(R.string.Lorem_Ipsum))
            nextBtn.setText("표정연습 하러가기")
            //2 setOnclickListener 설정
            nextBtn.setOnClickListener{
                //
                val intent : Intent = Intent(this@QuizActivity, BaseActivity::class.java)
                setResult(INTENT_CODE_FROM_QUIZ_TO_BASE, intent)
                activityResultLauncher.launch(intent)
            }

        }else{ // 틀렸을 경우 fixme 새롭게 다른 문제로 버튼 텍스트들을 초기화
            //1 텍스트 설정
            resultStatusTV.setText("오답! 틀렸습니다!")
            resultExplTV.setText(getString(R.string.Lorem_Ipsum))
            nextBtn.setText("퀴즈 재도전하기")
            //2 setOnclickListener 설정
            nextBtn.setOnClickListener{
                recreate() //액티비티 재시작
                dialog.dismiss()
            }
        }
    }


    private fun updateBtnClickable(buttons: Array<Button>) {
        for(btn in buttons) btn.isClickable = isBtnClickable
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
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()

        // Remove overlay layout from content view
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.removeView(dialogLayout)
    }




}
