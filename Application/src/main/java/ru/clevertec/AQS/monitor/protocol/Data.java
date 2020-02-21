package ru.clevertec.AQS.monitor.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Data {

    public static final int DataLength = 16;

    private int temp;
    private int hum;
    private int co2;
    private int pm1;
    private int pm25;
    private int pm10;
    private int tvoc;
    private int rad;

    public Data(byte[] buffer, int pos) {
        ByteBuffer bb = ByteBuffer.wrap(buffer, pos, buffer.length-pos);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        FillFromByteBuffer(bb);
    }

    public Data(ByteBuffer bb) {
        FillFromByteBuffer(bb);
    }

    private void FillFromByteBuffer(ByteBuffer bb) {
        temp = bb.getShort();
        hum = bb.getShort();
        co2 = bb.getShort();
        pm1 = bb.getShort();
        pm25 = bb.getShort();
        pm10 = bb.getShort();
        tvoc = bb.getShort();
        rad = bb.getShort();
    }

    public float getTemperature() {
        return (float)temp / 100;
    }

    public float getHumidity() {
        return (float)hum / 100;
    }

    public int getCO2() {
        return co2;
    }

    public int getPM1() {
        return pm1;
    }

    public int getPM25() {
        return pm25;
    }

    public int getPM10() {
        return pm10;
    }

    public int getTVOC() {
        return tvoc;
    }

    public int getRad() { return rad; }
}
