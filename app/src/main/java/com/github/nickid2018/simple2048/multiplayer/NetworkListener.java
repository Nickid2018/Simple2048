package com.github.nickid2018.simple2048.multiplayer;

import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

public abstract class NetworkListener {

    protected StringWriter stringWriter;
    protected PrintWriter writer;
    protected Consumer<Exception> fatalListener;
    protected DataListener dataListener;

    public NetworkListener() {
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
    }

    public void setUpdater(DataListener updater) {
        dataListener = updater;
    }

    public void setFatalListener(Consumer<Exception> fatalListener) {
        this.fatalListener = fatalListener;
    }

    public abstract void init() throws IOException, JSONException;

    public abstract void send(NetworkData data) throws IOException, JSONException;

    public abstract void disconnect() throws IOException, JSONException;
}
