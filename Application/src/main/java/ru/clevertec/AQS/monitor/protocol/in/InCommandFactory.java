package ru.clevertec.AQS.monitor.protocol.in;

import java.nio.ByteBuffer;

public class InCommandFactory {
    private static InCommand
            statusCommand = new Status(),
            dataTransferCommand = new DataTransfer();

    public static InCommand getCommand(byte[] buffer) {
        switch (buffer[0]) {
            case 0:
                return statusCommand;
            case 1:
                return dataTransferCommand;
            default:
                return null;
        }

    }
}
