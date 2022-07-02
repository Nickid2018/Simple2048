package com.github.nickid2018.simple2048.replay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InitialEntry {

    private final int slot1;
    private final long value1;
    private final int slot2;
    private final long value2;

    public InitialEntry(int slot1, long value1, int slot2, long value2) {
        this.slot1 = slot1;
        this.value1 = value1;
        this.slot2 = slot2;
        this.value2 = value2;
    }

    public static InitialEntry deserialize(InputStream is) throws IOException {
        byte[] data = new byte[8];
        int slot1 = is.read();
        is.read(data);
        long value1 = ReplayData.bytesToLong(data);
        int slot2 = is.read();
        is.read(data);
        long value2 = ReplayData.bytesToLong(data);
        return new InitialEntry(slot1, value1, slot2, value2);
    }

    public int getSlot1() {
        return slot1;
    }

    public int getSlot2() {
        return slot2;
    }

    public long getValue1() {
        return value1;
    }

    public long getValue2() {
        return value2;
    }

    public void serialize(OutputStream os) throws IOException {
        os.write(slot1);
        os.write(ReplayData.longToBytes(value1));
        os.write(slot2);
        os.write(ReplayData.longToBytes(value2));
    }
}
