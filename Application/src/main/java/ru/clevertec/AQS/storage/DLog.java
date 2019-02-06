package ru.clevertec.AQS.storage;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.TimeZone;

@Entity
public class DLog {
    @PrimaryKey
    public int id;

    public int ssecs;
    public int rtime;

    public float temp;
    public float hum;
    public int co2;
    public int pm1;
    public int pm25;
    public int pm10;
    public int tvoc;

    public DLog fillFromProtocol(int id, ru.clevertec.AQS.monitor.protocol.DLog dLog) {
        this.id = id;
        this.ssecs = dLog.getSSecs();
        this.rtime = dLog.getRTime();
        this.temp = dLog.getData().getTemperature();
        this.hum = dLog.getData().getHumidity();
        this.co2 = dLog.getData().getCO2();
        this.pm1 = dLog.getData().getPM1();
        this.pm25 = dLog.getData().getPM25();
        this.pm10 = dLog.getData().getPM10();
        this.tvoc = dLog.getData().getTVOC();
        return this;
    }

}
