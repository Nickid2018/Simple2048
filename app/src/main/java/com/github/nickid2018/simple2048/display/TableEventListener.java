package com.github.nickid2018.simple2048.display;

import com.github.nickid2018.simple2048.gamedata.MoveDirection;

public interface TableEventListener {

    boolean moveValid(TableRenderer renderer);

    void onEnd(TableRenderer renderer);

    void onInitialize(TableRenderer renderer);

    void onMove(TableRenderer renderer, MoveDirection direction, SpawnData spawnData);

    void onAnimationOver(TableRenderer renderer);
}
