package ru.clevertec.AQS.common;

import java.util.TimeZone;

public class UnixTimeUtils {

    public static int getCurrentUnixTime() {
        int offset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        long now = System.currentTimeMillis() + offset;
        return  (int)(now/1000);
    }
}
