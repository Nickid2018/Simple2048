package com.github.nickid2018.simple2048.datasave;

import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ScoreDataStorage {

    public static final byte[] HEAD = "SCORE2048".getBytes(StandardCharsets.UTF_8);
    private static final List<ScoreEntry> entries = new ArrayList<>();
    public static File SCORE_DATA_FILE;
    private static boolean loaded = false;

    public static void initialize(Activity activity) {
        if (loaded)
            return;
        SCORE_DATA_FILE = new File(activity.getDataDir(), "scores.dat");
        if (SCORE_DATA_FILE.isFile()) {
            try {
                load();
                loaded = true;
            } catch (IOException e) {
                Log.e("Score System", "Reading score file", e);
            }
        } else {
            try {
                SCORE_DATA_FILE.createNewFile();
                save();
            } catch (IOException e) {
                Log.e("Score System", "Creating score file", e);
            }
        }
    }

    public static long getHighest() {
        return entries.isEmpty() ? 0 : entries.get(0).getScore();
    }

    public static List<ScoreEntry> getEntries() {
        return entries;
    }

    public static void newRecord(long score) {
        ScoreEntry entry = new ScoreEntry(score, new Date());
        entries.add(entry);
        entries.sort((sc1, sc2) -> Math.toIntExact(sc2.getScore() - sc1.getScore()));
        while (entries.size() > 30)
            entries.remove(30);
        save();
    }

    public static void load() throws IOException {
        FileInputStream fis = new FileInputStream(SCORE_DATA_FILE);
        byte[] head = new byte[HEAD.length];
        fis.read(head);
        if (!Arrays.equals(head, HEAD))
            throw new IOException("Invalid score file: head");
        GZIPInputStream stream = new GZIPInputStream(fis);
        int size = stream.read();
        for (int i = 0; i < size; i++)
            entries.add(ScoreEntry.deserialize(stream));
        stream.close();
        Log.i("Score System", "Read " + size + " score entries");
    }

    public static void save() {
        try {
            FileOutputStream fos = new FileOutputStream(SCORE_DATA_FILE);
            fos.write(HEAD);
            GZIPOutputStream stream = new GZIPOutputStream(fos);
            stream.write(entries.size());
            for (ScoreEntry entry : entries)
                entry.serialize(stream);
            stream.close();
        } catch (IOException e) {
            Log.e("Score System", "Saving score file", e);
        }
    }
}
