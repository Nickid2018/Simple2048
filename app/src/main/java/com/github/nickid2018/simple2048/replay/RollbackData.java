package com.github.nickid2018.simple2048.replay;

import androidx.core.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RollbackData {

    private final List<RollbackEntry> entries = new ArrayList<>();
    private final List<Pair<Integer, Long>> valuesTo = new ArrayList<>();

    public void pushEntry(RollbackEntry entry) {
        entries.add(entry);
    }

    public void recordRollback(int step, long nowValue) {
        Pair<Integer, Long> pair = new Pair<>(step, nowValue);
        while (!valuesTo.isEmpty() && valuesTo.get(valuesTo.size() - 1).first >= step)
            valuesTo.remove(valuesTo.size() - 1);
        valuesTo.add(pair);
    }

    public RollbackEntry popLastEntry() {
        return entries.remove(entries.size() - 1);
    }

    public void serialize(OutputStream stream) throws IOException {
        stream.write(ReplayData.longToBytes(entries.size()));
        for (RollbackEntry entry : entries)
            entry.serialize(stream);
        stream.write(ReplayData.longToBytes(valuesTo.size()));
        for (Pair<Integer, Long> pair : valuesTo) {
            stream.write(ReplayData.longToBytes(pair.first));
            stream.write(ReplayData.longToBytes(pair.second));
        }
    }

    public static RollbackData deserialize(InputStream stream) throws IOException {
        RollbackData data = new RollbackData();
        byte[] dataT = new byte[8];
        stream.read(dataT);
        for (int i = 0; i < ReplayData.bytesToLong(dataT); i++)
            data.entries.add(RollbackEntry.deserialize(stream));
        stream.read(dataT);
        long size = ReplayData.bytesToLong(dataT);
        for (int i = 0; i < size; i++) {
            stream.read(dataT);
            int step = Math.toIntExact(ReplayData.bytesToLong(dataT));
            stream.read(dataT);
            long value = ReplayData.bytesToLong(dataT);
            data.valuesTo.add(new Pair<>(step, value));
        }
        return data;
    }

    public boolean hasRollback() {
        return !entries.isEmpty();
    }
}
