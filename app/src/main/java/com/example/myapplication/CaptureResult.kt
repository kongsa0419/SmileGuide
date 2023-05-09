package com.example.myapplication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_CAPTURE_TO_BASE
import com.example.myapplication.BaseActivity.Companion.INTENT_CODE_FROM_CAPTURE_TO_QUIZ
import com.example.myapplication.databinding.ActivityCaptureResultBinding
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*

class CaptureResult : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCaptureResultBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)




        val imageUri = intent.getStringExtra(getString(R.string.orig_pic)) // 로컬 파일 URI : String
        val imgUri = Uri.parse(imageUri) // URI 데이터타입 : Uri

        Glide.with(this)
            .load(imageUri)
            .into(viewBinding.captureResultImage)

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



        viewBinding.captureResultBtnBack.setOnClickListener {
            // intent에 저장된 사진 local-uri는 자동 소멸
            // TODO: sharedPreference에 저장된 사진을 삭제해줘야함
            SharedPreferencesUtil.removeString(getString(R.string.orig_pic_local))
            SharedPreferencesUtil.removeString(getString(R.string.orig_pic_web))

            finish() // 사진 재촬영 가능 (Go back to the previous activity)
        }




        viewBinding.captureResultBtnGallery.setOnClickListener {
            // TODO: Navigate to the gallery
        }




        viewBinding.captureResultBtnProceed.setOnClickListener {
            //1)Firebase에 저장하고   2)웹 접근 가능한 URL 얻어 놓기->sharedPreference
            uploadImage(imgUri)

            //TODO: Activity_quiz 로 넘어감
            val intent = Intent(this@CaptureResult, QuizActivity::class.java)
            setResult(INTENT_CODE_FROM_CAPTURE_TO_QUIZ, intent)
            activityResultLauncher.launch(intent)
        }

    }



    // on below line creating a function to upload our image.
    fun uploadImage(fileUri: Uri) {
        // on below line checking weather our file uri is null or not.
        if (fileUri != null) {
            // on below line displaying a progress dialog when uploading an image.
            val progressDialog = ProgressDialog(this)
            // on below line setting title and message for our progress dialog and displaying our progress dialog.
            progressDialog.setTitle("Uploading...")
            progressDialog.setMessage("Uploading your image..")
            progressDialog.show()

            // on below line creating a storage refrence for firebase storage and creating a child in it with
            // random uuid.
            val uploadedImageFileName: String = UUID.randomUUID().toString()

            // 이미지 파일이름을 기억해둠 (sharedPreference)
            //INFO : uploadedImageFileName 는 파일이름을 뿐, 확장자(.jpg)가 붙어있지 않음
            SharedPreferencesUtil.putString(getString(R.string.orig_pic_local), uploadedImageFileName)


            val ref: StorageReference = FirebaseStorage.getInstance().getReference()
                .child("og\\" + uploadedImageFileName);//uploadedImageFileName이름으로 파일 업로드
            // on below line adding a file to our storage.
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
                    SharedPreferencesUtil.putString(getString(R.string.orig_pic_web),downloadUri.toString())

                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                } else { //Handle failures
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "[FB]이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            progressDialog.dismiss()
        }
    }


    override fun onResume() {
        super.onResume()
    }

    fun getFBStorageReference(prefix: String, filename: String): StorageReference {
        return FirebaseStorage.getInstance().getReference().child(prefix + "\\" + filename);
    }

}