package com.github.nickid2018.simple2048.multiplayer;

import org.json.JSONException;

import java.io.IOException;

public interface DataListener {

    void accept(NetworkData data) throws IOException, JSONException;
}
