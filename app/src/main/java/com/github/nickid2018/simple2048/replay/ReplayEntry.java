package com.github.nickid2018.simple2048.replay;

import com.github.nickid2018.simple2048.display.SpawnData;
import com.github.nickid2018.simple2048.display.TableView;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReplayEntry {

    private final MoveDirection direction;
    private final SpawnData spawnData;

    public ReplayEntry(MoveDirection direction, SpawnData data) {
        this.direction = direction;
        spawnData = data;
    }

    public static ReplayEntry deserializeLegacy(InputStream is) throws IOException {
        MoveDirection direction = MoveDirection.values()[is.read()];
        SpawnData spawnData = SpawnData.create(is.read(), is.read());
        return new ReplayEntry(direction, spawnData);
    }

    public static ReplayEntry deserialize(InputStream is) throws IOException {
        MoveDirection direction = MoveDirection.values()[is.read()];
        SpawnData spawnData = SpawnData.create(is.read(), 1L << is.read());
        return new ReplayEntry(direction, spawnData);
    }

    public MoveDirection getDirection() {
        return direction;
    }

    public SpawnData getSpawnData() {
        return spawnData;
    }

    public void serialize(OutputStream os) throws IOException {
        os.write(direction.ordinal());
        os.write(spawnData.slot);
        os.write((int) (Math.log(spawnData.data) / TableView.LN2));
    }
}
