package com.github.nickid2018.simple2048.multiplayer;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LanServerQueryManager extends Thread {

    public static final List<LanServerEntry> entries = new ArrayList<>();

    private static LanServerQueryManager manager;
    private final InetAddress address;
    private final MulticastSocket socket;
    private volatile boolean running = true;
    private Consumer<LanServerEntry> entryListener;

    private LanServerQueryManager() throws IOException {
        address = InetAddress.getByName(LanServerInterface.BROADCAST_IP);
        socket = new MulticastSocket(LanServerInterface.BROADCAST_PORT);
        socket.setSoTimeout(5000);
        socket.joinGroup(address);
    }

    public static void startQuery(Consumer<LanServerEntry> entryListener) throws IOException {
        endQuery();
        manager = new LanServerQueryManager();
        manager.entryListener = entryListener;
        manager.start();
    }

    public static void endQuery() {
        if (manager != null) {
            manager.stopNow();
            entries.clear();
            manager = null;
        }
    }

    public static void refresh() {
        entries.clear();
    }

    public void stopNow() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(packet);
                String data = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                LanServerEntry entry = new LanServerEntry(data, packet.getAddress());
                if (!entries.contains(entry)) {
                    entries.add(entry);
                    entryListener.accept(entry);
                }
            } catch (SocketException ignored) {
            } catch (Exception e) {
                Log.e("Network System", "Error in querying", e);
                break;
            }
        }
        try {
            socket.leaveGroup(address);
        } catch (IOException e) {
            Log.e("Network System", "Error in leaving", e);
        }
        socket.close();
    }
}
