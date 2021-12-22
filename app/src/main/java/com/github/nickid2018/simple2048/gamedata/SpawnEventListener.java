package com.github.nickid2018.simple2048.gamedata;

@FunctionalInterface
public interface SpawnEventListener {

    void spawn(int row, int column, long number);
}
