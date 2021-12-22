package com.github.nickid2018.simple2048.datasave;

import android.app.Activity;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.display.TableView;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.replay.ReplayData;
import com.github.nickid2018.simple2048.replay.RollbackData;
import com.github.nickid2018.simple2048.replay.SimpleGameRecorder;

import java.io.*;
import java.util.Arrays;
import java.util.Date;

public class GameDataStorage {

    public static final String tempGameDataFile = "game-data.cache";

    public static final byte[] HEAD = "GAME-DATA".getBytes();

    public static File FILE_CACHE;

    public static void init(Activity activity) {
        FILE_CACHE = new File(activity.getCacheDir(), tempGameDataFile);
    }

    public static boolean haveLastData() {
        return FILE_CACHE.isFile();
    }

    public static void loadData(TableRenderer renderer, SimpleGameRecorder recorder) throws IOException {
        InputStream is = new FileInputStream(FILE_CACHE);
        byte[] headDetect = new byte[HEAD.length];
        is.read(headDetect);
        if (!Arrays.equals(headDetect, HEAD))
            throw new IOException("Corrupt game data file");
        for (int i = 0; i < 16; i++) {
            int data = is.read();
            long dataS = data == 255 ? 0 : 1L << data;
            renderer.getTable().set(i / 4, i % 4, dataS);
        }
        byte[] dataScore = new byte[8];
        is.read(dataScore);
        renderer.getTable().setScore(ReplayData.bytesToLong(dataScore));
        renderer.getTable().validate();
        recorder.setRollback(RollbackData.deserialize(is));
        ReplayData data = new ReplayData(ReplayData.STATUS_READING);
        data.deserializeFromStream(is);
        data.setStatus(ReplayData.STATUS_RECORDING);
        ((SimpleGameRecorder) renderer.getEventListener()).setReplay(data);
        renderer.setNewGame(false);
        renderer.getTableView().invalidate();
        renderer.updateValues(TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE);
    }

    public static void saveData(TableRenderer renderer, SimpleGameRecorder recorder) throws IOException {
        OutputStream os = new FileOutputStream(FILE_CACHE);
        os.write(HEAD);
        GameTable table = renderer.getTable();
        for (int i = 0; i < 16; i++) {
            long now = table.get(i / 4, i % 4);
            os.write(now == 0 ? 255 : (int) (Math.log(now) / TableView.LN2));
        }
        os.write(ReplayData.longToBytes(renderer.getTable().getScore()));
        recorder.getRollback().serialize(os);
        ReplayData data = ((SimpleGameRecorder) renderer.getEventListener()).getReplay();
        data.setStatus(ReplayData.STATUS_RECORDING);
        data.setDate(new Date());
        data.setOwner("Temp Save");
        data.setName("Temp Save");
        data.serialize(os);
    }
}
