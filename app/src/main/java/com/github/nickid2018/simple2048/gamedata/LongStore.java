package com.github.nickid2018.simple2048.gamedata;

public class LongStore {

    private long value;

    public LongStore(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

    public void increase() {
        value++;
    }
}
