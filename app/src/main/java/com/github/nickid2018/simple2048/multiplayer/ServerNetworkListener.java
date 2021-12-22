package com.github.nickid2018.simple2048.multiplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import com.github.nickid2018.simple2048.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerNetworkListener extends NetworkListener {

    public final Map<String, ServerNetworkHandler> handlerMap = new HashMap<>();
    public final Set<String> names = new HashSet<>();
    public final Set<String> startConfirmed = new HashSet<>();
    private final Context context;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public ThreadLocal<ServerNetworkHandler> nowHandler;
    private int port;
    private String name;
    private ServerSocket server;
    private volatile boolean serverRunning = true;

    public ServerNetworkListener(String name, Context context) {
        this.context = context;
    }

    @Override
    public void init() throws IOException, JSONException {
        server = new ServerSocket();
        port = server.getLocalPort();
        dataListener.accept(new NetworkData(
                NetworkDataType.INFORMATION, String.format(
                context.getResources().getString(R.string.serverBind), findLocalIP() + ":" + port)));
        new Thread(() -> {
            try {
                while (serverRunning) {
                    Socket socket = server.accept();
                    ServerNetworkHandler handler = new ServerNetworkHandler(socket);
                    try {
                        JSONObject object = new JSONObject();
                        object.put("serverName", name);
                        JSONArray array = new JSONArray();
                        for (String name : names)
                            array.put(name);
                        object.put("serverPlayers", array);
                        JSONArray array2 = new JSONArray();
                        for (String name : startConfirmed)
                            array.put(name);
                        object.put("serverConfirms", array2);
                        internalSend(handler, new NetworkData(NetworkDataType.ROOM_INIT, object.toString()));
                        executor.execute(handler);
                    } catch (JSONException ignored) {
                    }
                }
            } catch (IOException e) {
                if (serverRunning)
                    fatalListener.accept(e);
            }
        }, "Server Thread").start();
    }

    @Override
    public void send(NetworkData data) throws IOException, JSONException {
        if (data.getType() == NetworkDataType.CHAT)
            dataListener.accept(data);
        for (ServerNetworkHandler handler : handlerMap.values())
            internalSend(handler, data);
    }

    public void internalSend(ServerNetworkHandler handler, NetworkData data) throws JSONException, IOException {
        handler.writer.write(data.serialize());
        handler.writer.newLine();
    }

    @Override
    public void disconnect() throws IOException, JSONException {
        serverRunning = false;
        server.close();
        dataListener.accept(new NetworkData(
                NetworkDataType.INFORMATION, context.getResources().getString(R.string.serverStop)));
    }

    private void clientDisconnect(ServerNetworkHandler handler) throws JSONException, IOException {
        NetworkData data = new NetworkData(NetworkDataType.DISCONNECT, String.format("{\"name\":\"%s\"}", handler.name));
        dataListener.accept(data);
        handlerMap.remove(handler.name);
        names.remove(handler.name);
        startConfirmed.remove(handler.name);
        for (ServerNetworkHandler handlerNow : handlerMap.values()) {
            try {
                internalSend(handlerNow, data);
            } catch (Exception ignored) {
            }
        }
    }

    private String findLocalIP() throws SocketException {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                for (Enumeration<NetworkInterface> enumeration1 = NetworkInterface.getNetworkInterfaces(); enumeration1.hasMoreElements(); ) {
                    for (Enumeration<InetAddress> enumeration2 = enumeration1.nextElement().getInetAddresses(); enumeration2.hasMoreElements(); ) {
                        InetAddress address = enumeration2.nextElement();
                        if (!address.isLoopbackAddress() && !(address instanceof Inet6Address))
                            return address.getHostAddress();
                    }
                }
                return null;
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (!wifi.isWifiEnabled())
                    wifi.setWifiEnabled(true);
                WifiInfo wifiInfo = wifi.getConnectionInfo();
                int ip = wifiInfo.getIpAddress();
                return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
            }
        }
        return null;
    }

    public class ServerNetworkHandler implements Runnable {

        public final Socket client;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        public String name;

        private ServerNetworkHandler(Socket client) throws IOException {
            this.client = client;
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        }

        @Override
        public void run() {
            try {
                while (client.isConnected()) {
                    nowHandler.set(this);
                    String data = reader.readLine();
                    dataListener.accept(NetworkData.deserialize(data));
                }
            } catch (Exception e) {
                if (client.isConnected())
                    fatalListener.accept(e);
            }
            try {
                clientDisconnect(this);
            } catch (Exception ignored) {
            }
        }
    }
}
