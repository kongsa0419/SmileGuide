package com.example.myapplication

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

        // TODO: Navigate to the gallery
        viewBinding.captureResultBtnGallery.visibility = View.VISIBLE //INFO (testing)
        viewBinding.captureResultBtnGallery.setOnClickListener {
            val BASE_URL = "https://www.ailabapi.com/"

            val retrofit = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(AilabtoolsService::class.java)

            val file = File(imgPath)
            val fileName = filename+".jpg"
            val option ="whiteBK"

            val requestFile = file.asRequestBody("multipart/form-data".toMediaType())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val optionReqBody = option.toRequestBody("text/plain".toMediaType())


            val call = apiService.getBackRmvdImg(imagePart, optionReqBody)
            call.enqueue(object: Callback<AilabApiResponse> {
                override fun onResponse(
                    call: retrofit2.Call<AilabApiResponse>,
                    response: Response<AilabApiResponse>
                ) {
                    Toast.makeText(applicationContext,"api성공",Toast.LENGTH_LONG).show()
                    Log.d("TAG", "api성공")
                }

                override fun onFailure(
                    call: retrofit2.Call<AilabApiResponse>,
                    t: Throwable)
                {
                    //게속 여기로 들어옴...
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




    // Function to call the API using Retrofit





    /* API 요청 함수(Volley)
    fun requestPortraitBackgroundRemoval(file: File) {
        val bitmap = ImageUtil.fileToBitmap(file)
        val byteArray = ImageUtil.bitmapToByteArray(bitmap!!)

        val url = "https://www.ailabapi.com/api/cutout/portrait/portrait-background-removal"
        val volleyRequest = object : StringRequest(
            Request.Method.POST, url,
            com.android.volley.Response.Listener<String> { response ->
                // 요청에 대한 응답 처리
                val jsonResponse = JSONObject(response)
                Toast.makeText(applicationContext, "volley 성공!", Toast.LENGTH_LONG).show()
                // 응답 데이터를 처리하는 코드 추가
                val data = jsonResponse.getJSONObject("data")
                val url : String = data.getString("image_url")
                Log.d("TAG", url)
                viewBinding.captureResultImage.setImageURI(Uri.parse(url))
                Toast.makeText(applicationContext, "이미지 세팅됐나?", Toast.LENGTH_LONG).show()
            },
            com.android.volley.Response.ErrorListener { error ->
                // 요청에 대한 오류 처리
                error.printStackTrace()
                Toast.makeText(applicationContext, "volley 실패ㅠㅠ", Toast.LENGTH_LONG).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "multipart/form-data"
                headers["ailabapi-api-key"] = BuildConfig.api_key_ailabtools
                return headers
            }

            override fun getBodyContentType(): String {
                return "application/octet-stream"
            }

            override fun getBody(): ByteArray {
                return byteArray
            }
        }

        val requestQueue = Volley.newRequestQueue(applicationContext) // context는 액티비티나 애플리케이션의 컨텍스트입니다.
        requestQueue.add(volleyRequest)
    }
    */

    /* API 요청 함수(Okhttp)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getBackgroundRemovedImg(filePath: String) {
        // Create a map of headers to include in the request
        val headers: MutableMap<String, String> = HashMap()
        headers["ailabapi-api-key"] = BuildConfig.api_key_ailabtools.toString()

        // Create a File object representing the image to upload
        val imageFile = File(filePath)

        // Call the uploadImageWithHeader method
        try {
            val response: okhttp3.Response? = ImageUploader.uploadImageWithHeader(
                "https://www.ailabapi.com/api/portrait/effects/emotion-editor",
                imageFile,
                headers
            )

            // Handle the server's response
            if (response!!.isSuccessful) {
                println("Image uploaded successfully!")
                //TODO
                Toast.makeText(applicationContext, "배경제거 성공!", Toast.LENGTH_SHORT).show()
                Log.d("TAG", response.body.toString())
            } else {
                println("Error uploading image: " + response!!.code + " " + response.message)
                Toast.makeText(applicationContext, "배경제거 실패!", Toast.LENGTH_SHORT).show()
                Log.d("TAG", response.body.toString())
            }
        } catch (e: IOException) {
            System.err.println("Error uploading image: " + e.message)
        }
    }*/


    //return type: file Url


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
