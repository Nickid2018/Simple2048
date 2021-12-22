package com.github.nickid2018.simple2048.gamedata;

public enum MoveDirection {

    UP(1), DOWN(0), LEFT(3), RIGHT(2);

    public final int opposite;

    MoveDirection(int direction){
        opposite = direction;
    }

    public MoveDirection getOpposite() {
        return MoveDirection.values()[opposite];
    }
}
