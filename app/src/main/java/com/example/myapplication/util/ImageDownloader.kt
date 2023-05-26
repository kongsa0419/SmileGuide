//package com.example.myapplication.util
//
//import android.content.ContentValues
//import android.content.Context
//import android.os.AsyncTask
//import android.os.Environment
//import android.provider.MediaStore
//import android.util.Log
//import java.io.File
//import java.io.FileOutputStream
//import java.io.InputStream
//import java.net.HttpURLConnection
//import java.net.URL
//
//class ImageDownloader(private val context: Context) : AsyncTask<String, Void, String>() {
//    private val TAG = "ImageDownloader"
//
//    override fun doInBackground(vararg params: String): String? {
//        val imageUrl = params[0]
//        return try {
//            val url = URL(imageUrl)
//            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
//            connection.doInput = true
//            connection.connect()
//
//            val input: InputStream = connection.inputStream
//            val fileName = "image.jpg" // Set the desired file name
//            val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//            val file = File(directory, fileName)
//
//            val output = FileOutputStream(file)
//            val buffer = ByteArray(1024)
//            var bytesRead: Int
//            while (input.read(buffer).also { bytesRead = it } != -1) {
//                output.write(buffer, 0, bytesRead)
//            }
//            output.close()
//            input.close()
//
//            // Get the content URI for the saved file
//            val contentUri = MediaStore.Images.Media.insertImage(
//                context.contentResolver,
//                file.absolutePath,
//                fileName,
//                null
//            )
//            contentUri
//        } catch (e: Exception) {
//            Log.e(TAG, "Error downloading image: ${e.message}")
//            null
//        }
//    }
//}