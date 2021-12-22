package com.github.nickid2018.simple2048.multiplayer;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class LanServerInterface extends Thread {

    public static final String BROADCAST_IP = "224.114.51.4";
    public static final int BROADCAST_PORT = 19198;
    public static final int INTERVAL_SEND = 1500;

    public static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    private final DatagramSocket socket;
    private final int port;
    private final String name;
    private volatile boolean running = true;

    public LanServerInterface(int port, String name) throws SocketException {
        super("Lan Server Interface" + THREAD_ID.incrementAndGet());
        socket = new DatagramSocket();
        this.port = port;
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String name() {
        return name;
    }

    public void stopNow() {
        running = false;
    }

    @Override
    public void run() {
        try {
            InetAddress address = InetAddress.getByName(BROADCAST_IP);
            String announcement = "port=" + port + ";name=" + name;
            byte[] bytes = announcement.getBytes(StandardCharsets.UTF_8);
            while (running) {
                DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, address, BROADCAST_PORT);
                socket.send(packet);
                sleep(INTERVAL_SEND);
            }
        } catch (Exception e) {
            Log.e("Network System", "Broadcast", e);
        }
    }


}
