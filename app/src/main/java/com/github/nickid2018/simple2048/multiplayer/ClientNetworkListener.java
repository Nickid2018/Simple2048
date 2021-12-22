package com.github.nickid2018.simple2048.multiplayer;

import android.content.Context;
import org.json.JSONException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ClientNetworkListener extends NetworkListener {

    private final Context context;
    private final String name;
    private final InetAddress address;
    private final int port;
    private Socket socket;
    private volatile boolean running = true;
    private BufferedWriter writer;
    private BufferedReader reader;

    public ClientNetworkListener(String dest, String name, Context context) throws IOException {
        this.context = context;
        this.name = name;
        int split = dest.lastIndexOf(':');
        address = InetAddress.getByName(dest.substring(0, split));
        port = Integer.parseInt(dest.substring(split + 1));
    }

    @Override
    public void init() throws IOException {
        socket = new Socket(address, port);
        new Thread(() -> {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                send(new NetworkData(NetworkDataType.ROOM_INIT, String.format("{\"name\":\"%s\"}", name)));
                while (running) {
                    String data = reader.readLine();
                    dataListener.accept(NetworkData.deserialize(data));
                }
            } catch (IOException | JSONException e) {
                if (running)
                    fatalListener.accept(e);
            }
        }, "Client Thread").start();
    }

    @Override
    public void send(NetworkData data) throws IOException, JSONException {
        writer.write(data.serialize());
        writer.newLine();
    }

    @Override
    public void disconnect() throws IOException {
        running = false;
        socket.close();
    }
}
