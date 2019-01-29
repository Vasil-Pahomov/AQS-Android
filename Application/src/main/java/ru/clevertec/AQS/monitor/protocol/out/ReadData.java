package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;

public class ReadData extends OutCommand {

    private int fromIdx, toIdx;

    public ReadData(int fromIdx, int toIdx) {
        this.fromIdx = fromIdx;
        this.toIdx = toIdx;
    }
    @Override
    protected byte getCode() {
        return 1;
    }

    @Override
    protected void fillBody(ByteBuffer b) {
        b.putInt(fromIdx);
        b.putInt(toIdx);
    }
}
