package ru.clevertec.AQS.monitor.protocol.in;

import java.nio.ByteBuffer;

public abstract class InCommand {
    //command length, not including type byte
    public abstract int getCommandLength(byte[] buffer);

    public abstract void Parse(byte[] buffer);
}
