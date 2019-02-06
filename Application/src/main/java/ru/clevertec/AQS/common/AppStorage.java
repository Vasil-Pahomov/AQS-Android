package ru.clevertec.AQS.common;

import android.content.Context;
import android.content.SharedPreferences;

//todo: this doesn't look like good design pattern
public class AppStorage {

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences("ru.clevertec.AQS", Context.MODE_PRIVATE);
    }

    private static void setIntValue(Context context, String key, int value) {
        SharedPreferences sp = getSP(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.apply();
   }

   private static int getIntValue(Context context, String key, int defaultValue) {
       SharedPreferences sp = getSP(context);
       return sp.getInt(key, defaultValue);
   }

    public static void setLastLogIndex(Context context, int lastLogIndex) {
        setIntValue(context, "LastLogIdx", lastLogIndex);
    }

    public static int getLastLogIndex(Context context) {
        return getIntValue(context, "LastLogIdx", 0);
    }
}
