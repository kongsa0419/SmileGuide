package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtil {
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
}