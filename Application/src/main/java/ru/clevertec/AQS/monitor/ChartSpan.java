package ru.clevertec.AQS.monitor;

import java.util.Calendar;
import java.util.Date;

public enum ChartSpan {
    MINUTE(60, 10),
    FIVE_MINUTES(5*60, 60),
    HALF_HOUR(30*60, 300),
    HOUR(60*60, 600),
    EIGHT_HOURS(8*3600, 3600),
    DAY(24*2600, 4*3600),
    WEEK(24*7*3600, 24*3600);

    private final long spanSec;
    private final long granularitySec;

    ChartSpan(long spanSec, long granularitySec) {
        this.spanSec = spanSec;
        this.granularitySec = granularitySec;
    }

    public long getSpanSec()
    {
        return this.spanSec;
    }

    public long getGranularitySec()
    {
        return this.granularitySec;
    }

    public long adjustTimeSec(long timeSec)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000L*timeSec);
        switch (this) {
            case MINUTE:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), (int)Math.round(cal.get(Calendar.SECOND)/10.0)*10).getTime()/1000;
            case FIVE_MINUTES:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0).getTime()/1000;
            case HALF_HOUR:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), (int)Math.floor(cal.get(Calendar.MINUTE)/10.0)*10, 0).getTime()/1000;
            case HOUR:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), (int)Math.floor(cal.get(Calendar.MINUTE)/20.0)*20, 0).getTime()/1000;
            case EIGHT_HOURS:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), 0, 0).getTime()/1000;
            case DAY:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), (int)Math.floor(cal.get(Calendar.HOUR_OF_DAY)/8.0)*8, 0, 0).getTime()/1000;
            case WEEK:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0).getTime()/1000;
            default:
                return timeSec;
        }
    }

    public ChartSpan getNext() {
        switch (this) {

            case MINUTE:
                return FIVE_MINUTES;
            case FIVE_MINUTES:
                return HALF_HOUR;
            case HALF_HOUR:
                return HOUR;
            case HOUR:
                return EIGHT_HOURS;
            case EIGHT_HOURS:
                return DAY;
            case DAY:
                return WEEK;
            case WEEK:
                return this;
            default:
                return null;
        }
    }

    public ChartSpan getPrev() {
        switch (this) {
            case MINUTE:
                return this;
            case FIVE_MINUTES:
                return MINUTE;
            case HALF_HOUR:
                return FIVE_MINUTES;
            case HOUR:
                return HALF_HOUR;
            case EIGHT_HOURS:
                return HOUR;
            case DAY:
                return EIGHT_HOURS;
            case WEEK:
                return DAY;
            default:
                return null;
        }
    }

};
