package ru.clevertec.AQS.storage;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.TimeZone;

import ru.clevertec.AQS.monitor.protocol.Data;

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
    public int rad;

    public DLog fillFromProtocol(int id, ru.clevertec.AQS.monitor.protocol.DLog dLog) {
        this.id = id;
        this.ssecs = dLog.getSSecs();
        this.rtime = dLog.getRTime();
        return fillValuesFromData(dLog.getData());
    }

    public DLog fillValuesFromData(Data data) {
        this.temp = data.getTemperature();
        this.hum = data.getHumidity();
        this.co2 = data.getCO2();
        this.pm1 = data.getPM1();
        this.pm25 = data.getPM25();
        this.pm10 = data.getPM10();
        this.tvoc = data.getTVOC();
        this.rad = data.getRad();
        return this;
    }

}
