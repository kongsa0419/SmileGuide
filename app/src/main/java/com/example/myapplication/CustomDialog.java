package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

public class CustomDialog extends Dialog implements View.OnClickListener {
    public CustomDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onClick(View view) {

    }
}
