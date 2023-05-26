package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source



/**알게된 점 :
 * MediaStore든 뭐든 어떻게 접근하든,
 * 'file://', 'content://'  모두 다 접근 가능한 file이다. 다만 코드를 생성해내는 과정(searching)에서 불필요한 코드들이 섞여있었음. -> 그래서 안 됐던 것임.
 * */
class ContentUriRequestBody(
    context: Context,
    private val uri: Uri
) : RequestBody() {
    private val contentResolver = context.contentResolver
    private var fileName = ""
    private var size = -1L

    init {
        contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            }
        }
    }

    fun getFileName() = fileName
    override fun contentLength(): Long = size
    override fun contentType(): MediaType? =
        contentResolver.getType(uri)?.toMediaTypeOrNull()

    override fun writeTo(sink: BufferedSink) {
        contentResolver.openInputStream(uri)?.source()?.use { source ->
            sink.writeAll(source)
        }
    }

    fun toFormData() = MultipartBody.Part.createFormData("image", getFileName(), this)
    fun toFormData(paramName: String) = MultipartBody.Part.createFormData(paramName, paramName, this)
}
