package ru.clevertec.AQS.monitor;

import java.util.Calendar;
import java.util.Date;

public enum ChartSpan {
    FIVE_MINUTES(5*60, 60),
    HALF_HOUR(30*60, 300),
    HOUR(60*60, 600),
    FOUR_HOURS(4*3600, 3600),
    EIGHT_HOURS(8*3600, 2*3600),
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
            case FIVE_MINUTES:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0).getTime()/1000;
            case HALF_HOUR:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), (int)Math.floor(cal.get(Calendar.MINUTE)/10.0)*10, 0).getTime()/1000;
            case HOUR:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), (int)Math.floor(cal.get(Calendar.MINUTE)/20.0)*20, 0).getTime()/1000;
            case FOUR_HOURS:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), 0, 0).getTime()/1000;
            case EIGHT_HOURS:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), (int)Math.round(cal.get(Calendar.HOUR_OF_DAY)/2.0)*2, 0, 0).getTime()/1000;
            case DAY:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), (int)Math.floor(cal.get(Calendar.HOUR_OF_DAY)/8.0)*8, 0, 0).getTime()/1000;
            case WEEK:
                return new Date(cal.get(Calendar.YEAR)-1900, cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0).getTime()/1000;
            default:
                return timeSec;
        }
    }

    public ChartSpan getNext() {
        return values()[this.ordinal() < values().length-1 ? this.ordinal() + 1 : values().length-1];
    }

    public ChartSpan getPrev() {
        return values()[this.ordinal() > 0 ?  this.ordinal() - 1 : 0];
    }

};
