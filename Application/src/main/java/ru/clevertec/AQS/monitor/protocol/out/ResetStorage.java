package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;

public class ResetStorage extends OutCommand {
    @Override
    protected byte getCode() {
        return 2;
    }

    @Override
    protected void fillBody(ByteBuffer b) {
    }
}
