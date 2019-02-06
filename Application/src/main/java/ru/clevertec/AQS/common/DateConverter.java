package ru.clevertec.AQS.common;

import java.util.Date;
import java.util.TimeZone;

public class DateConverter {
    public Date fromUnix(int unix) {
        return new Date(unix*1000L + TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings());
    }

    public long toUnix(Date date) {
        return date.getTime()/1000;
    }

}
