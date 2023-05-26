package com.example.myapplication.util

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.core.net.toFile
import com.example.myapplication.R
import com.example.myapplication.activity.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

//연습장임






















//
//import android.content.ContentValues
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.media.MediaMetadataRetriever
//import android.media.MediaScannerConnection
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import android.os.storage.StorageManager
//import android.provider.MediaStore
//import androidx.annotation.RequiresApi
//import androidx.core.content.ContextCompat
//import androidx.core.content.ContextCompat.getSystemService
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileOutputStream
//import java.io.InputStream
//import java.net.HttpURLConnection
//import java.net.URL
//import java.net.URLConnection
//
//enum class MediaStoreFileType(
//    val externalContentUri: Uri,
//    val mimeType: String,
//    val pathByDCIM: String
//) {
//    IMAGE(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*", "/image"),
//    AUDIO(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*", "/audio"),
//    VIDEO(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*", "/video");
//}
//object ImageUtil {
//
//
//    suspend fun convertURLToFile(url: String): File {
//        val response = withContext(Dispatchers.IO) {
//            URL(url).openConnection().getInputStream().use { input ->
//                ByteArrayOutputStream().use { output ->
//                    input.copyTo(output)
//                    output.toByteArray()
//                }
//            }
//        }
//        val ext = url.substringAfterLast(".") // URL 구조에 맞게 수정할 것
//        val filename = url.substringAfterLast("/") // URL 구조에 맞게 수정할 것
//        val metadata = MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "image/$ext"
//
//        return File.createTempFile(filename, ".$ext").apply {
//            writeBytes(response)
//        }
//    }
//
//
//}