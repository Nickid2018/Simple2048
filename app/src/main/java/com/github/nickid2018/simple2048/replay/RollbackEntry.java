package com.github.nickid2018.simple2048.replay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RollbackEntry {

    private final long previousPositions;
    private final long mergePositions;

    public RollbackEntry(long previousPositions, long mergePositions) {
        this.previousPositions = previousPositions;
        this.mergePositions = mergePositions;
    }

    public long getPreviousPositions() {
        return previousPositions;
    }

    public long getMergePositions() {
        return mergePositions;
    }

    public void serialize(OutputStream stream) throws IOException {
        stream.write(ReplayData.longToBytes(previousPositions));
        stream.write(ReplayData.longToBytes(mergePositions));
    }

    public static RollbackEntry deserialize(InputStream stream) throws IOException {
        byte[] transfer = new byte[8];
        stream.read(transfer);
        long previousPositions = ReplayData.bytesToLong(transfer);
        stream.read(transfer);
        long mergePositions = ReplayData.bytesToLong(transfer);
        return new RollbackEntry(previousPositions, mergePositions);
    }
}
