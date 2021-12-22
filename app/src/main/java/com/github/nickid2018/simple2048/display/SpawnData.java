package com.github.nickid2018.simple2048.display;

public class SpawnData {

    public int slot;
    public long data;

    public static SpawnData create(int slot, long data) {
        SpawnData d = new SpawnData();
        d.slot = slot;
        d.data = data;
        return d;
    }
}
