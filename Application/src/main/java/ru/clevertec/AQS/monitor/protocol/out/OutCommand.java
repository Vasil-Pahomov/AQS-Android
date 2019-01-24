package ru.clevertec.AQS.monitor.protocol.out;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class OutCommand {
    protected abstract byte getCode();

    protected abstract void fillBody(ByteBuffer b);

    public byte[] getBytes() {
        ByteBuffer bb = ByteBuffer.allocate(100);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short)0xDEAF);
        bb.put(getCode());
        fillBody(bb);
        bb.limit(bb.position());
        return bb.array();
    }
}
