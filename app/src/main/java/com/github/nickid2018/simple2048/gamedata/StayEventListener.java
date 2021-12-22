package com.github.nickid2018.simple2048.gamedata;

@FunctionalInterface
public interface StayEventListener {

    void stay(int line, int index, MoveDirection direction);
}
