package com.github.nickid2018.simple2048.gamedata;

public class IntegerStore {

    private int value;

    public IntegerStore(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }

    public int set(int value) {
        this.value = value;
        return value;
    }

    public void increase() {
        value++;
    }
}

