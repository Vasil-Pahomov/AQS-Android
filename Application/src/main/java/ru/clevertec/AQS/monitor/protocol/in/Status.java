package ru.clevertec.AQS.monitor.protocol.in;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.clevertec.AQS.monitor.protocol.Data;

public class Status extends InCommand {

    private byte stat;
    private byte bat;
    private int logidx;
    private Data data;

    @Override
    public int getCommandLength(byte[] buffer) {
        return 6 + Data.DataLength;
    }

    public void Parse(byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.get();;//skip command code
        stat = bb.get();
        bat = bb.get();
        logidx = bb.getInt();
        data = new Data(buffer, 7);
    }

    public Data getData() {
        return data;
    }

    public int getLogIdx() { return logidx; }
}
