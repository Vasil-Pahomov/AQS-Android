package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

//todo: refactor to static class (no need do instantiate - no memory garbading)
public abstract class OutCommand {
    protected abstract byte getCode();

    protected abstract void fillBody(ByteBuffer b);

    public byte[] getBytes() {
        ByteBuffer bb = ByteBuffer.allocate(100);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short)0xAFDE);
        bb.put(getCode());
        fillBody(bb);
        return Arrays.copyOf(bb.array(),bb.position());
    }
}
