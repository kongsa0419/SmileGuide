package com.example.myapplication.activity

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import com.example.myapplication.R
import com.example.myapplication.activity.BaseActivity.Companion.TAG
import com.example.myapplication.databinding.ActivityCaptureResultBinding
import com.example.myapplication.databinding.ActivityCompareBinding
import com.example.myapplication.dto.LuxadApiResponse
import com.example.myapplication.retrofit.RetrofitApi
import com.example.myapplication.util.ContentUriRequestBody
import com.example.myapplication.util.ImageUtil
import com.example.myapplication.util.ProgressDialogUtil
import com.example.myapplication.util.SharedPreferencesUtil
import kotlinx.coroutines.*

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


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

//        val trnsStr = "/data/data/com.example.myapplication/files/image_20230527_044858.jpg" //URI
//        val newStr = "/data/data/com.example.myapplication/files/image_20230527_044326.jpg" //URI

        val trnsBase64 = SharedPreferencesUtil.getString(getString(R.string.trns_pic)).toString()
        val imageBytes = Base64.decode(trnsBase64, Base64.DEFAULT)
        val trnsBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val newStr = SharedPreferencesUtil.getString(getString(R.string.new_pic)).toString()

        trnsImgUri = ImageUtil.getImageUri(this@CompareActivity, trnsBitmap)
        newImgUri = Uri.parse(newStr)

        //1
        viewBinding.cmpTrnsPicImg.setImageBitmap(trnsBitmap)
        viewBinding.cmpNewPicImg.setImageURI(newImgUri)

        //2
        val newRequestFile = ContentUriRequestBody(this@CompareActivity, newImgUri).toFormData("photo")
        val trnsRequestFile = ContentUriRequestBody(this@CompareActivity,  trnsImgUri).toFormData("photo")

        ProgressDialogUtil.showProgressDialog(this@CompareActivity, "API 호출중...")
        CoroutineScope(Dispatchers.IO).launch{
            try{
                 val newRespDeferred  : Deferred<LuxadApiResponse> = async (Dispatchers.IO){ //비동기 1 (suspend)
                    RetrofitApi.getLuxadService.getEmotionResult(newRequestFile)
                }
                val trnsRespDeferred  : Deferred<LuxadApiResponse> = async (Dispatchers.IO){ //비동기 2 (suspend)
                    RetrofitApi.getLuxadService.getEmotionResult(trnsRequestFile)
                }

                val newResp = newRespDeferred.await() // 비동기 작업 완료 대기
                val trnsResp = trnsRespDeferred.await() // 비동기 작업 완료 대기


                withContext(Dispatchers.Main) {
                    ProgressDialogUtil.hideProgressDialog()
                    // UI 작업 수행 (근데 표정 값으로 null인 것들이 많이 오고, 얼굴 속 장애물까지 파악하진 않음. 시간 약 3초)
                    viewBinding.root.setBackgroundColor(R.color.black)
                }
            }catch (e:Exception){
                Log.e(TAG, "$TAG Compare작업 실패")
                e.printStackTrace()
            }
        }
    }
}