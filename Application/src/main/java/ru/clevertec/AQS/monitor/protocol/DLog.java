package ru.clevertec.AQS.monitor.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DLog {

    public static int DLogLength = 8 + Data.DataLength;
    private int ssecs;
    private int rtime;

    private Data data;

    public DLog(byte[] buffer, int pos) {
        ByteBuffer bb = ByteBuffer.wrap(buffer, pos, buffer.length-pos);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        ssecs = bb.getInt();
        rtime = bb.getInt();
        data = new Data(bb);
    }

    public int getSSecs() {
        return ssecs;
    }

    public int getRTime() {
        return rtime;
    }

    public void setRTime(int rTime) { rtime = rTime; }

    public Data getData() { return data; }
}
