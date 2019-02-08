package ru.clevertec.AQS.common;

import android.content.Context;
import android.content.SharedPreferences;

import ru.clevertec.AQS.monitor.DataType;

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

    private static void setStringValue(Context context, String key, String value) {
        SharedPreferences sp = getSP(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private static String getStringValue(Context context, String key, String defaultValue) {
        SharedPreferences sp = getSP(context);
        return sp.getString(key, defaultValue);
    }

    public static void setLastLogIndex(Context context, int lastLogIndex) {
        setIntValue(context, "LastLogIdx", lastLogIndex);
    }

    public static int getLastLogIndex(Context context) {
        return getIntValue(context, "LastLogIdx", 0);
    }

    public static void setDataType1(Context context, DataType dataType) {
        setStringValue(context, "DataType1", dataType.name());
    }

    public static DataType getDataType1(Context context) {
        return DataType.valueOf(getStringValue(context, "DataType1", DataType.TEMPERATURE.name()));
    }

    public static void setDataType2(Context context, DataType dataType) {
        setStringValue(context, "DataType2", dataType.name());
    }

    public static DataType getDataType2(Context context) {
        return DataType.valueOf(getStringValue(context, "DataType2", DataType.HUMIDITY.name()));
    }

}
