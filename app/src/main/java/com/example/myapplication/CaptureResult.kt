package com.example.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_CAPTURE_TO_QUIZ
import com.example.myapplication.databinding.ActivityCaptureResultBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class CaptureResult : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCaptureResultBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        /** INFO: 다른 액티비티에서 이 액티비티로 넘어왔을때,
         *  INFO resultCode로 어느 액티비티에서 온 것인지구분할 수 있음.
         * */
        val activityResultLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                run {
                    if (result.resultCode == RESULT_OK) { //이 액티비티로 넘어오는 곳이 BaseActivity뿐임

                    }
                }
            }



        //INFO: imgPath와 imgUri는 각각 String, Uri 데이터타입인 것 외에는 내부적인 String 값은 같다.
        val imgPath = intent.getStringExtra(getString(R.string.orig_pic)) // 로컬 jpg 파일 URI : String
        val imgUri = Uri.parse(imgPath) // URI 데이터타입 : Uri
        val filename : String = imgUri.lastPathSegment.toString()


        Glide.with(this)
            .load(imgPath)
            .into(viewBinding.captureResultImage)








        viewBinding.captureResultBtnGallery.setOnClickListener {
            // TODO: Navigate to the gallery
        }



        viewBinding.captureResultBtnProceed.setOnClickListener {
            //1)Firebase에 저장하고(o)   2)웹 접근 가능한 URL 얻어 놓기->sharedPreference (o)
            uploadImage(imgUri, filename)

            //TODO: Activity_quiz 로 넘어감
            val intent = Intent(this@CaptureResult, QuizActivity::class.java)
            setResult(INTENT_CODE_FROM_CAPTURE_TO_QUIZ, intent)
            activityResultLauncher.launch(intent)
        }


        viewBinding.captureResultBtnBack.setOnClickListener {
            // TODO: sharedPreference에 저장된 사진을 삭제
            // intent에 저장된 사진 local-uri는 자동 소멸
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
            SharedPreferencesUtil.putString(getString(R.string.orig_pic_web_filename), fbFilepath)

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
                    // this method is called when file is uploaded.
                    // in this case we are dismissing our progress dialog and displaying a toast message
                    val downloadUri = task.result
                    //INFO : 다운로드 가능한 URI = downloadUri.toString()
                    Log.d("[FB]downloadUri.toString()", downloadUri.toString())

                    //이 이미지 url을 기억해둬야함. (sharedPreference)
                    SharedPreferencesUtil.putString(getString(R.string.orig_pic_web_downloadable_url),
                        downloadUri.toString())
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                } else { //Handle failures
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            progressDialog.dismiss()
        }
    }


    override fun onResume() {
        Log.d("CAPRES","onResume 들어왔음")
        super.onResume()
    }

    fun getFBStorageReference(prefix: String, filename: String): StorageReference {
        return FirebaseStorage.getInstance().getReference().child(prefix + "\\" + filename);
    }

}