package com.example.myapplication.util

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.view.ViewGroup.LayoutParams
import android.widget.ProgressBar
import android.widget.TextView

object ProgressDialogUtil {
    private var progressDialog: Dialog? = null

    fun showProgressDialog(context: Context, message: String) {
        progressDialog?.dismiss()

        progressDialog = Dialog(context)
        progressDialog?.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)

            val progressBar = ProgressBar(context)
            val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            addContentView(progressBar, layoutParams)

            val messageTextView = TextView(context)
            messageTextView.text = message
            val messageLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            addContentView(messageTextView, messageLayoutParams)
        }

        progressDialog?.show()
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
