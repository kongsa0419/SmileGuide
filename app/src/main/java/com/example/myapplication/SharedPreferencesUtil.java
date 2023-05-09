package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
* 앱 전체에서 접근 가능한 sharedPreference (캐시 저장소 쯤으로 생각하자)
* */

public class SharedPreferencesUtil {
    private static final String PREFS_NAME = String.valueOf(R.string.preference_file_key);
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void removeString(String key){
        editor.remove(key);
        editor.apply();
    }

}
