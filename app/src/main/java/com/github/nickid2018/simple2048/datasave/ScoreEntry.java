package com.github.nickid2018.simple2048.datasave;

import com.github.nickid2018.simple2048.replay.ReplayData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class ScoreEntry implements Comparable<ScoreEntry> {

    private final long score;
    private final Date date;

    public ScoreEntry(long score, Date time) {
        this.score = score;
        this.date = time;
    }

    public static ScoreEntry deserialize(InputStream stream) throws IOException {
        byte[] data = new byte[8];
        stream.read(data);
        long score = ReplayData.bytesToLong(data);
        stream.read(data);
        Date date = new Date(ReplayData.bytesToLong(data));
        return new ScoreEntry(score, date);
    }

    public long getScore() {
        return score;
    }

    public Date getDate() {
        return date;
    }

    public void serialize(OutputStream stream) throws IOException {
        stream.write(ReplayData.longToBytes(score));
        stream.write(ReplayData.longToBytes(date.getTime()));
    }

    @Override
    public int compareTo(ScoreEntry scoreEntry) {
        return (int) Math.signum(scoreEntry.score - score);
    }
}
