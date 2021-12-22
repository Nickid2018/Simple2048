package com.github.nickid2018.simple2048.gamedata;

import com.github.nickid2018.simple2048.replay.RollbackEntry;

@FunctionalInterface
public interface RollbackEventListener {

    void receiveRollbackData(RollbackEntry entry);
}
