package com.example.myapplication

import android.app.ProgressDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import com.bumptech.glide.Glide
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
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private lateinit var webUrl : String

    private var imageView: ImageView?=null
    private var galleryButton:Button?=null
    private var backButton:Button?=null
    private var proceedButton:Button?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val imageUri = intent.getStringExtra("imageUri") // 로컬 파일 URI : String
        val imgUri = Uri.parse(imageUri) // URI 데이터타입 : Uri

        Glide.with(this)
            .load(imageUri)
            .into(viewBinding.captureResultImage)


        viewBinding.captureResultBtnBack.setOnClickListener {
            finish() // 사진 재촬영 가능 (Go back to the previous activity)
        }
        viewBinding.captureResultBtnGallery.setOnClickListener {
            // TODO: Navigate to the gallery
        }

        viewBinding.captureResultBtnProceed.setOnClickListener {
            //TODO: 1) 로컬URI를 File화? (파이어베이스에 올릴수 있게)

            //TODO: 2) Firebase에 저장하고, 웹 접근 가능한 URL 얻어 놓기
            uploadImage(imgUri)
//            val fbCustomBucket =  BuildConfig.firebase_bucket
//            val storage = Firebase.storage(fbCustomBucket)
//            var storageRef = storage.reference
            //TODO: 3) intent에 담아서 직전 액티비티 또는 Activity_quiz 로 넘어감
        }

    }


    // on below line creating a function to upload our image.
    fun uploadImage(fileUri:Uri) {
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
            val uploadedImageFileName : String = UUID.randomUUID().toString()
            //TODO: 이 이미지 파일이름을 기억해둬야함.
            val ref: StorageReference = FirebaseStorage.getInstance().getReference()
                    .child("og\\"+ uploadedImageFileName);//uploadedImageFileName이름으로 파일 업로드
            // on below line adding a file to our storage.
            ref.putFile(fileUri!!).addOnSuccessListener {
                // this method is called when file is uploaded.
                // in this case we are dismissing our progress dialog and displaying a toast message
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Image Uploaded..", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                // this method is called when there is failure in file upload.
                // in this case we are dismissing the dialog and displaying toast message
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Fail to Upload Image..", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun getFBStorageReference(prefix:String , filename:String) : StorageReference
    {
        return FirebaseStorage.getInstance().getReference().child(prefix +"\\"+ filename);
    }

}