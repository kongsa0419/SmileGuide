//package com.example.myapplication.util;
//
//
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.view.WindowManager;
//
//import androidx.annotation.NonNull;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class CustomCallBack<T> implements Callback<T> {
//    private ProgressDialog mProgressDialog;
//    Context context;
//
//    CustomCallBack(Context context) {
//        this.context = context;
//        mProgressDialog = new ProgressDialog(context);
//        ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//        mProgressDialog.setIndeterminate(true);
//        mProgressDialog.setMessage("Loading...");
//        mProgressDialog.setCanceledOnTouchOutside(false);
//        mProgressDialog.show();
//    }
//
//    @Override
//    public void onResponse(@NonNull Call call, @NonNull Response response) {
//        if (mProgressDialog.isShowing()) {
//            mProgressDialog.dismiss();
//            ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//        }
//    }
//
//    @Override
//    public void onFailure(Call<T> call, Throwable t) {
//        if (mProgressDialog.isShowing()) {
//            mProgressDialog.dismiss();
//            ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//        }
//    }
//}
