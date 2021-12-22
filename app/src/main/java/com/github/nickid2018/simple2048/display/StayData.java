package com.github.nickid2018.simple2048.display;

public class StayData {

    public int row;
    public int column;

    public static StayData create(int slot) {
        StayData data = new StayData();
        data.row = slot / 4;
        data.column = slot % 4;
        return data;
    }
}
