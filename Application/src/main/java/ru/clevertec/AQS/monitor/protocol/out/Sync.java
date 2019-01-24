package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;
import java.util.TimeZone;

public class Sync extends OutCommand {

    @Override
    protected byte getCode() {
        return 0;
    }

    @Override
    protected void fillBody(ByteBuffer b) {
        int offset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        long now = System.currentTimeMillis() + offset;
        b.putInt((int)(now/1000));
    }
}
