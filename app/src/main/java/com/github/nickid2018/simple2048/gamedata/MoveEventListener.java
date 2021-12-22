package com.github.nickid2018.simple2048.gamedata;

@FunctionalInterface
public interface MoveEventListener {

    void move(int line, int fromSlot, int endSlot, MoveDirection direction, long sourceData, long endData);
}
