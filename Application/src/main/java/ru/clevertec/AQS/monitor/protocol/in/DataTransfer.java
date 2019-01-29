package ru.clevertec.AQS.monitor.protocol.in;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.clevertec.AQS.monitor.protocol.Data;

public class DataTransfer extends InCommand {

    private Data[] data;

    public Data[] getData() { return data; }

    @Override
    public int getCommandLength(byte[] buffer) {
        if (buffer.length < 9) {
            //not enough data to determine true command length
            return 9;
        }
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.get();;//skip command code
        int fromIdx = bb.getInt();
        int toIdx = bb.getInt();
        return 8 + Data.DataLength*(toIdx - fromIdx + 1);
    }

    @Override
    public void Parse(byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.get();;//skip command code
        int fromIdx = bb.getInt();
        int toIdx = bb.getInt();
        int pos = 8;
        data = new Data[toIdx - fromIdx+1];
        int i = 0;
        while (fromIdx <= toIdx) {
            data[i++] = new Data(buffer, pos);
            pos += Data.DataLength;
            fromIdx++;
        }
    }
}
