package com.example.myapplication.util

//연습장임


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL


enum class MediaStoreFileType(
    val externalContentUri: Uri,
    val mimeType: String,
    val pathByDCIM: String
) {
    IMAGE(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*", "/image"),
    AUDIO(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*", "/audio"),
    VIDEO(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*", "/video");
}
object ImageUtil {
    fun getImageUri(context: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(context.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    suspend fun convertURLToFile(url: String): File {
        val response = withContext(Dispatchers.IO) {
            URL(url).openConnection().getInputStream().use { input ->
                ByteArrayOutputStream().use { output ->
                    input.copyTo(output)
                    output.toByteArray()
                }
            }
        }
        val ext = url.substringAfterLast(".") // URL 구조에 맞게 수정할 것
        val filename = url.substringAfterLast("/") // URL 구조에 맞게 수정할 것
        val metadata = MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "image/$ext"

        return File.createTempFile(filename, ".$ext").apply {
            writeBytes(response)
        }
    }

    fun getImgResFromUri(uri: Uri) : Pair<Int,Int> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(File(uri.path).absolutePath, options)
        val imageHeight = options.outHeight
        val imageWidth = options.outWidth
        return Pair(imageWidth, imageHeight)
    }

    fun getBitmapFromUri(context: Context, uri:Uri) : Bitmap{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            return android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

}