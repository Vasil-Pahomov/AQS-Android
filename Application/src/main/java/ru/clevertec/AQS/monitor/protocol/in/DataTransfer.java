package ru.clevertec.AQS.monitor.protocol.in;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.clevertec.AQS.monitor.protocol.DLog;
import ru.clevertec.AQS.monitor.protocol.Data;

public class DataTransfer extends InCommand {

    private DLog[] dlogs;

    private int fromIdx, toIdx;

    public DLog[] getDLogs() { return dlogs; }

    private ByteBuffer parseHead(byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.get();;//skip command code
        fromIdx = bb.getInt();
        toIdx = bb.getInt();
        return  bb;
    }

    @Override
    public int getCommandLength(byte[] buffer) {
        if (buffer.length < 9) {
            //not enough dlogs to determine true command length
            return 9;
        }
        parseHead(buffer);
        return 8 + DLog.DLogLength * (toIdx - fromIdx + 1);
    }

    @Override
    public void Parse(byte[] buffer) {
        ByteBuffer bb = parseHead(buffer);
        int pos = bb.position();
        dlogs = new DLog[toIdx - fromIdx+1];
        int i = 0;
        int fidx = fromIdx;
        while (fidx <= toIdx) {
            dlogs[i++] = new DLog(buffer, pos);
            pos += DLog.DLogLength;
            fidx++;
        }
    }

    public int getFromIdx() { return fromIdx;}

    public int getToIdx() {return toIdx;}
}
