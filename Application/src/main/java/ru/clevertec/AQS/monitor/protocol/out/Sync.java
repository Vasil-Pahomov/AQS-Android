package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;

import ru.clevertec.AQS.common.UnixTimeUtils;

public class Sync extends OutCommand {

    @Override
    protected byte getCode() {
        return 0;
    }

    @Override
    protected void fillBody(ByteBuffer b) {
        b.putInt(UnixTimeUtils.getCurrentUnixTime());
    }
}
