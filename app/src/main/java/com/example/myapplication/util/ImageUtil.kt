package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ImageUtil {

    fun downloadImage(context : Context , url: String): File {
        val fileUri = Uri.parse(url)
        val fileName = fileUri.lastPathSegment ?: "image.jpg"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)

        val connection = URL(url).openConnection()
        connection.connect()
        val contentLength = connection.contentLength

        val inputStream = connection.getInputStream()
        val buffer = ByteArray(contentLength)
        var bytesRead: Int
        var totalBytesRead = 0
        while (inputStream.read(buffer, totalBytesRead, contentLength - totalBytesRead).also { bytesRead = it } != -1) {
            totalBytesRead += bytesRead
        }

        outputStream.write(buffer, 0, totalBytesRead)

        outputStream.flush()
        outputStream.close()
        inputStream.close()

        return file
    }

    fun getFileNameFromUrl(url: String): String {
        val lastIndex = url.lastIndexOf('/')
        return if (lastIndex != -1 && lastIndex < url.length - 1) {
            url.substring(lastIndex + 1)
        } else {
            throw IllegalArgumentException("Invalid URL: $url")
        }
    }



    fun bitmapToJpgUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = File(context.cacheDir, "temp.jpg")
        val fileOutputStream = FileOutputStream(path)
        fileOutputStream.write(bytes.toByteArray())
        fileOutputStream.close()
        return Uri.fromFile(path)
    }

    fun jpgUriToBitmap(context: Context, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }





    // 이미지 파일을 Bitmap으로 변환하는 함수
    fun fileToBitmap(file: File): Bitmap? {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    // Bitmap을 ByteArray로 변환하는 함수
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }


}