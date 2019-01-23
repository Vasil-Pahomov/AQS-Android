package ru.clevertec.AQS.monitor.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Status {

    private byte stat;
    private byte bat;
    private long logidx;
    private Data data;

    public Status(byte[] buffer, int pos) {
        ByteBuffer bb = ByteBuffer.wrap(buffer, pos, buffer.length-pos);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        stat = bb.get();
        bat = bb.get();
        logidx = bb.getLong();
        data = new Data(buffer, pos + 6);
    }

    public Data getData() {
        return data;
    }
}
