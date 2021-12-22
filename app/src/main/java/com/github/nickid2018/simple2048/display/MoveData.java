package com.github.nickid2018.simple2048.display;

public class MoveData {

    public int toSlot;
    public long source;
    public long result;

    public static MoveData create(int toSlot, long source, long result) {
        MoveData data = new MoveData();
        data.toSlot = toSlot;
        data.source = source;
        data.result = result;
        return data;
    }
}
