package com.github.nickid2018.simple2048.replay;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import androidx.core.util.Pair;
import com.github.nickid2018.simple2048.display.SpawnData;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReplayData {

    public static final int STATUS_RECORDING = 0;
    public static final int STATUS_REPLAYING = 1;
    public static final int STATUS_SAVING = 2;
    public static final int STATUS_READING = 3;
    public static final int STATUS_USELESS = 4;

    public static final byte[] LEGACY_HEAD = "REP2048".getBytes(StandardCharsets.UTF_8);
    public static final byte[] HEAD = "RPY2048".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CUSTOM_HEAD = "CRY2048".getBytes(StandardCharsets.UTF_8);

    private boolean isLegacy;
    private boolean isCustom;

    private Date date;
    private String name;
    private String owner;
    private long maxValue = -1;
    private long score = -1;
    private InitialEntry initial;
    private int entrySize;
    private List<ReplayEntry> entries;
    private List<Pair<Long, Long>> valuesTo = new ArrayList<>();

    private int status;

    public ReplayData(int mode) {
        status = mode;
        if (mode == STATUS_RECORDING)
            entries = new ArrayList<>();
    }

    public static byte[] longToBytes(long value) {
        byte[] data = new byte[8];
        data[0] = (byte) ((value & 0xFF) - 128);
        data[1] = (byte) ((value >>> 8 & 0xFF) - 128);
        data[2] = (byte) ((value >>> 16 & 0xFF) - 128);
        data[3] = (byte) ((value >>> 24 & 0xFF) - 128);
        data[4] = (byte) ((value >>> 32 & 0xFF) - 128);
        data[5] = (byte) ((value >>> 40 & 0xFF) - 128);
        data[6] = (byte) ((value >>> 48 & 0xFF) - 128);
        data[7] = (byte) ((value >>> 56 & 0xFF) - 128);
        return data;
    }

    public static long bytesToLong(byte[] data) {
        long result = 0;
        result |= ((long) data[0]) + 128;
        result |= (((long) data[1]) + 128) << 8;
        result |= (((long) data[2]) + 128) << 16;
        result |= (((long) data[3]) + 128) << 24;
        result |= (((long) data[4]) + 128) << 32;
        result |= (((long) data[5]) + 128) << 40;
        result |= (((long) data[6]) + 128) << 48;
        result |= (((long) data[7]) + 128) << 56;
        return result;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public Pair<Long, Long> getRollback(int session) {
        return session < valuesTo.size() ? valuesTo.get(session) : null;
    }

    public void preLoad(File file) throws IOException {
        preLoadFromStream(new FileInputStream(file));
    }

    public void preLoad(Uri uri, ContentResolver resolver) throws IOException {
        preLoadFromStream(resolver.openInputStream(uri));
    }

    private void preLoadFromStream(InputStream is) throws IOException {
        analyzeHead(is).close();
    }

    public ReplayEntry popLast() {
        if (status != STATUS_RECORDING)
            throw new IllegalStateException();
        entrySize--;
        return entries.remove(entries.size() - 1);
    }

    public InitialEntry getInitial() {
        return initial;
    }

    public void setInitial(GameTable table) {
        if (status != STATUS_RECORDING)
            throw new IllegalStateException();
        int slot1 = -1;
        int slot2 = -1;
        long data1 = -1;
        long data2 = -1;
        for (int row = 0; row < table.size(); row++)
            for (int column = 0; column < table.size(); column++) {
                long value = table.get(row, column);
                if (value != 0) {
                    if (slot1 == -1) {
                        slot1 = row * table.size() + column;
                        data1 = value;
                    } else {
                        slot2 = row * table.size() + column;
                        data2 = value;
                    }
                }
            }
        initial = new InitialEntry(slot1, data1, slot2, data2);
    }

    public ReplayEntry getEntryNow(int index) {
        return entries.get(index);
    }

    public int entrySize() {
        return entrySize;
    }

    public void move(MoveDirection direction, SpawnData data) {
        if (status != STATUS_RECORDING)
            throw new IllegalStateException();
        entries.add(new ReplayEntry(direction, data));
        entrySize++;
    }

    public void recordRollback(long step, long nowValue) {
        Pair<Long, Long> pair = new Pair<>(step, nowValue);
        while (!valuesTo.isEmpty() && valuesTo.get(valuesTo.size() - 1).first >= step)
            valuesTo.remove(valuesTo.size() - 1);
        valuesTo.add(pair);
    }

    public void serialize(OutputStream file) throws IOException {
        if (status != STATUS_RECORDING)
            throw new IllegalStateException();
        status = STATUS_SAVING;
        file.write(HEAD);
        GZIPOutputStream stream = new GZIPOutputStream(file);
        byte[] name = owner.getBytes(StandardCharsets.UTF_8);
        if (name.length > 255)
            throw new IOException("Invalid owner: too long");
        stream.write(name.length);
        stream.write(name);
        byte[] nameFile = this.name.getBytes(StandardCharsets.UTF_8);
        if (nameFile.length > 255)
            throw new IOException("Invalid name: too long");
        stream.write(nameFile.length);
        stream.write(nameFile);
        long time = date.getTime();
        stream.write(longToBytes(time));
        stream.write(longToBytes(maxValue));
        stream.write(longToBytes(score));
        initial.serialize(stream);
        stream.write(longToBytes(entrySize));
        for (ReplayEntry entry : entries)
            entry.serialize(stream);
        if (!valuesTo.isEmpty()) {
            stream.write(longToBytes(valuesTo.size()));
            for (Pair<Long, Long> pair : valuesTo) {
                stream.write(ReplayData.longToBytes(pair.first));
                stream.write(ReplayData.longToBytes(pair.second));
            }
        }
        stream.close();
        status = STATUS_USELESS;
    }

    public void deserialize(File file) throws IOException {
        deserializeFromStream(new FileInputStream(file));
    }

    public void deserialize(Uri uri, ContentResolver resolver) throws IOException {
        deserializeFromStream(resolver.openInputStream(uri));
    }

    public void deserializeFromStream(InputStream is) throws IOException {
        GZIPInputStream stream = analyzeHead(is);
        entries = new ArrayList<>();
        for (int i = 0; i < entrySize; i++)
            entries.add(isLegacy ? ReplayEntry.deserializeLegacy(stream) : ReplayEntry.deserialize(stream));
        if (stream.available() == 1) {
            byte[] dataT = new byte[8];
            stream.read(dataT);
            long size = ReplayData.bytesToLong(dataT);
            for (int i = 0; i < size; i++) {
                stream.read(dataT);
                long step = ReplayData.bytesToLong(dataT);
                stream.read(dataT);
                long value = ReplayData.bytesToLong(dataT);
                valuesTo.add(new Pair<>(step, value));
            }
        }
        stream.close();
        status = STATUS_REPLAYING;
    }

    private GZIPInputStream analyzeHead(InputStream is) throws IOException {
        if (status != STATUS_READING)
            throw new IllegalStateException();
        byte[] head = new byte[LEGACY_HEAD.length];
        is.read(head);
        if (Arrays.equals(LEGACY_HEAD, head)) {
            isLegacy = true;
            isCustom = false;
            Log.i("Replay System", "Using legacy file struct");
        } else if (Arrays.equals(HEAD, head)) {
            isLegacy = false;
            isCustom = false;
        } else if (Arrays.equals(CUSTOM_HEAD, head)) {
            isLegacy = false;
            isCustom = true;
        } else
            throw new IOException("Invalid replay file: head");
        GZIPInputStream stream = new GZIPInputStream(is);
        int ownerLen = stream.read();
        byte[] ownerTransfer = new byte[ownerLen];
        stream.read(ownerTransfer);
        owner = new String(ownerTransfer, StandardCharsets.UTF_8);
        if (!isLegacy) {
            int nameLen = stream.read();
            byte[] nameTransfer = new byte[nameLen];
            stream.read(nameTransfer);
            name = new String(nameTransfer, StandardCharsets.UTF_8);
        }
        byte[] dataTransfer = new byte[8];
        stream.read(dataTransfer);
        date = new Date(bytesToLong(dataTransfer));
        if (!isLegacy) {
            stream.read(dataTransfer);
            maxValue = bytesToLong(dataTransfer);
            stream.read(dataTransfer);
            score = bytesToLong(dataTransfer);
        }
        initial = isLegacy ? InitialEntry.deserializeLegacy(stream) : InitialEntry.deserialize(stream);
        stream.read(dataTransfer);
        entrySize = Math.toIntExact(bytesToLong(dataTransfer));
        return stream;
    }

    public void computeMaxs() {
        if (isCustom)
            throw new IllegalArgumentException("Unsupported data");
        GameTable table = new GameTable(4);
        table.setInitial(initial);
        for (ReplayEntry data : entries) {
            table.internalMove(data.getDirection());
            table.spawnAt(data.getSpawnData());
        }
        if (table.checkContinue())
            throw new IllegalArgumentException("Invalid data");
        maxValue = table.getMaxValue();
        score = table.getScore();
    }
}
