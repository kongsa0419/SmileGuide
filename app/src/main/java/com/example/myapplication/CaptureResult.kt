package com.example.myapplication

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityCaptureResultBinding
import com.example.myapplication.dto.AilabApiResponse
import com.example.myapplication.retrofit.RetrofitApi
import com.example.myapplication.service.AilabtoolsService
import com.example.myapplication.util.ContentUriRequestBody
import com.example.myapplication.util.ImageUploader
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.create
import okhttp3.MultipartBody.Builder
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.Callback
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.*


class CaptureResult : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCaptureResultBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var imgPath : String
    private lateinit var imgUri : Uri
    private lateinit var filename : String

    private val ailabtools_api_key : String = BuildConfig.api_key_ailabtools


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        /** INFO: 다른 액티비티에서 이 액티비티로 넘어왔을때,
         *  INFO resultCode로 어느 액티비티에서 온 것인지구분할 수 있음.
         * */
        /*
        val activityResultLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                run {
                    if (result.resultCode == INTENT_CODE_FROM_BASE_TO_CAPTURE) { //Base->Capture로 인텐트가 돌아온 경우
                        //INFO: imgPath와 imgUri는 각각 String, Uri 데이터타입인 것 외에는 내부적인 String 값은 같다.
                        Log.d("[캡쳐리절트]","여기들어왔어요!!") //안들어옴
                    }
                }
            }
        */

        //imgPath: content://media/external/images/media/1000000174
        imgPath = intent.getStringExtra(getString(R.string.orig_pic)).toString() // 로컬 jpg 파일 URI : String
        imgUri = Uri.parse(imgPath) // URI 데이터타입 : Uri
        filename  = imgUri.lastPathSegment.toString()


        Glide.with(this)
            .load(imgPath)
            .into(viewBinding.captureResultImage)




        /**1. 절대경로 uri 를 가지고 파일을 만든다.
        2. (생략가능) 파일 이름을 정해준다.
        3. RequestBody를 만들어 준다  mediatype는 이미지로..
        4. 멀티파츠 바디를 만들어준다 */

        /**
         * File을 다룰때, 파일 경로가 절대적인지, 혹은 상대적인지를 고려해서 절대적인 것으로 바꿔줘야함.
         * */

        // TODO: Navigate to the gallery
        viewBinding.captureResultBtnGallery.visibility = View.VISIBLE //INFO (testing)
        viewBinding.captureResultBtnGallery.setOnClickListener {
            val fileName = filename+".jpg"
            val option ="whiteBK"

            val requestFile = ContentUriRequestBody(applicationContext, imgUri).toFormData()
            val requestOption = option.toRequestBody("multipart/form-data".toMediaType())


            val call = RetrofitApi.getAilabtoolsService.getBackRmvdImg(requestFile, requestOption) /*apiService.getBackRmvdImg(imagePart, optionReqBody)*/
            call.enqueue(object: Callback<AilabApiResponse> {
                override fun onResponse(call: retrofit2.Call<AilabApiResponse>, response: Response<AilabApiResponse>) {
                    Toast.makeText(applicationContext,"api성공",Toast.LENGTH_LONG).show()
                    Log.d("TAG", "api성공")
                    if(response.isSuccessful)
                    {
                        Toast.makeText(applicationContext,"222api성공222",Toast.LENGTH_LONG).show()
                        Log.d("TAG", "데이터도 성공리에 도착!!" + response.body()!!.data!!.image_url.toString())
                    }
                }

                override fun onFailure(call: retrofit2.Call<AilabApiResponse>, t: Throwable)
                {
                    //게속 여기로 들어옴...
                    Log.e("TAG", t.message.toString())
                    Toast.makeText(applicationContext,"api실패",Toast.LENGTH_LONG).show()
                    // API 요청 실패 시의 처리
                    Log.d("TAG", "api실패")
                }
            })
        }

        viewBinding.captureResultBtnProceed.setOnClickListener {
            //1)Firebase에 저장하고(o)   2)웹 접근 가능한 URL 얻어 놓기->sharedPreference (o)
            uploadImage(imgUri, filename)

            val intent = Intent(this@CaptureResult, QuizActivity::class.java)
            startActivity(intent)
        }


        viewBinding.captureResultBtnBack.setOnClickListener {
            // TODO: sharedPreference에 저장된 사진을 삭제 (intent로 넘어온 값은 자동소멸)
            SharedPreferencesUtil.removeString(getString(R.string.orig_pic_web_filename))
            SharedPreferencesUtil.removeString(getString(R.string.orig_pic_web_downloadable_url))
            finish() // 사진 재촬영 (Go back to the previous activity)
        }

    }




    


    // on below line creating a function to upload our image.
    fun uploadImage(fileUri: Uri, fName : String) {
        if (fileUri != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.setMessage("Uploading your image..")
            progressDialog.show()

            val fbFilepath = "og/" + fName

            val ref: StorageReference = FirebaseStorage.getInstance().getReference()
                .child(fbFilepath);//fbFilepath 이름으로 파일 업로드

            var uploadTask = ref.putFile(fileUri!!)
            val urlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) { //Handle success
                    //INFO : 다운로드 가능한 URI = downloadUri.toString()
                    val downloadUri = task.result
                    Log.d("[FB]downloadUri.toString()", downloadUri.toString())

                    //이 이미지 url을 기억해둬야함. (sharedPreference)
                    SharedPreferencesUtil.putString(getString(R.string.orig_pic_web_filename), fbFilepath)
                    SharedPreferencesUtil.putString(getString(R.string.orig_pic_web_downloadable_url), downloadUri.toString())
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                } else { //Handle failures
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            progressDialog.dismiss()
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("CAPRES","onResume 들어왔음")
    }


    override fun onDestroy() {
        super.onDestroy()
        //만약에 SharedPreferences에 저장된 값들이 있다면 삭제해줘야함
        //스택에 쌓였으면 아직 여기까지는 안 왔을거니까
        SharedPreferencesUtil.removeString(getString(R.string.orig_pic_web_filename))
        SharedPreferencesUtil.removeString(getString(R.string.orig_pic_web_downloadable_url))
    }





    fun getFBStorageReference(prefix: String, filename: String): StorageReference {
        return FirebaseStorage.getInstance().getReference().child(prefix + "\\" + filename);
    }

}
