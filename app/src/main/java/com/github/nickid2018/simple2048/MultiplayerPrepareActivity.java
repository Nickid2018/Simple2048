package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.multiplayer.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class MultiplayerPrepareActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    public static ServerNetworkListener serverListener;
    @SuppressLint("StaticFieldLeak")
    public static ClientNetworkListener clientListener;
    private final List<Map<String, Object>> players = new ArrayList<>();
    private final Set<String> confirmed = new HashSet<>();
    private NetworkListener listener;
    private TextView chatView;
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_prepare);
        chatView = findViewById(R.id.multiRoom_messageBox);
        Intent intent = getIntent();
        if (intent.getBooleanExtra("isServer", true))
            initServer(intent.getStringExtra("name"));
        else {
            try {
                initClient(intent.getStringExtra("address"), intent.getStringExtra("name"));
            } catch (IOException e) {
                Toast.makeText(this,
                        String.format(getResources().getString(R.string.clientError) + "\n", e.getMessage()), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        ListView listPlayers = findViewById(R.id.multiRoom_nowPlayers);
        adapter = new SimpleAdapter(
                this, players, android.R.layout.simple_list_item_1, new String[]{"name"}, new int[]{android.R.id.text1});
        listPlayers.setAdapter(adapter);
    }

    private void initServer(String name) {
        listener = serverListener = new ServerNetworkListener(name, this);
        listener.setUpdater(data -> {
            if (data.getType() == NetworkDataType.INFORMATION) {
                chatView.append(data.getJsonData() + "\n");
                return;
            }
            JSONObject object = new JSONObject(data.getJsonData());
            String client = object.getString("name");
            switch (data.getType()) {
                case CHAT:
                    String chat = object.getString("text");
                    chatView.append(client + ":" + chat + "\n");
                    break;
                case ROOM_INIT:
                    serverListener.names.add(client);
                    serverListener.handlerMap.put(client, serverListener.nowHandler.get());
                    serverListener.send(new NetworkData(NetworkDataType.PLAYER_ADD, String.format("{\"name\":\"%s\"}", client)));
                    Map<String, Object> pair = new HashMap<>();
                    pair.put("name", client);
                    players.add(pair);
                    adapter.notifyDataSetChanged();
                    chatView.append(String.format(getResources().getString(R.string.clientAdd) + "\n", object.getString("name")));
                    break;
                case DISCONNECT:
                    int findIndex;
                    for (findIndex = 0; findIndex < players.size(); findIndex++)
                        if (players.get(findIndex).get("name").equals(client))
                            break;
                    players.remove(findIndex);
                    adapter.notifyDataSetChanged();
                    chatView.append(String.format(getResources().getString(R.string.clientLeave) + "\n", client));
                    break;
                case CONFIRM_START:
                    client = object.getString("name");
                    for (findIndex = 0; findIndex < players.size(); findIndex++)
                        if (players.get(findIndex).get("name").equals(client))
                            break;
                    if (serverListener.startConfirmed.contains(client)) {
                        serverListener.startConfirmed.remove(client);
                        adapter.getView(findIndex, null, null).setBackgroundColor(0xFFFF0000);
                    } else {
                        serverListener.startConfirmed.add(client);
                        adapter.getView(findIndex, null, null).setBackgroundColor(0xFF00FF00);
                    }
                    serverListener.send(data);
                    break;
                default:
                    throw new IOException("Unexpected network data (Type:" + data.getType().name() + ")");
            }
        });
        listener.setFatalListener(e -> chatView.append(String.format(getResources().getString(R.string.serverError) + "\n", e.getMessage())));
    }

    private void initClient(String remote, String name) throws IOException {
        listener = clientListener = new ClientNetworkListener(remote, name, this);
        listener.setUpdater(data -> {
            if (data.getType() == NetworkDataType.INFORMATION) {
                chatView.append(data.getJsonData() + "\n");
                return;
            }
            JSONObject object = new JSONObject(data.getJsonData());
            switch (data.getType()) {
                case CHAT:
                    String from = object.getString("name");
                    String chat = object.getString("text");
                    chatView.append(from + ":" + chat + "\n");
                    break;
                case ROOM_INIT:
                    ((TextView) findViewById(R.id.multiRoom_owner)).setText(object.getString("serverName"));
                    JSONArray array = object.getJSONArray("serverPlayers");
                    for (int i = 0; i < array.length(); i++) {
                        String client = array.getString(i);
                        Map<String, Object> pair = new HashMap<>();
                        pair.put("name", client);
                        players.add(pair);
                    }
                    adapter.notifyDataSetChanged();
                    break;
                case DISCONNECT:
                    String client = object.getString("name");
                    int findIndex;
                    for (findIndex = 0; findIndex < players.size(); findIndex++)
                        if (players.get(findIndex).get("name").equals(client))
                            break;
                    players.remove(findIndex);
                    adapter.notifyDataSetChanged();
                    break;
                case PLAYER_ADD:
                    String player = object.getString("name");
                    Map<String, Object> pair = new HashMap<>();
                    pair.put("name", player);
                    players.add(pair);
                    adapter.notifyDataSetChanged();
                    break;
                case CONFIRM_START:
                    String clientName = object.getString("name");
                    for (findIndex = 0; findIndex < players.size(); findIndex++)
                        if (players.get(findIndex).get("name").equals(clientName))
                            break;
                    if (confirmed.contains(clientName)) {
                        confirmed.remove(clientName);
                        adapter.getView(findIndex, null, null).setBackgroundColor(0xFFFF0000);
                    } else {
                        confirmed.add(clientName);
                        adapter.getView(findIndex, null, null).setBackgroundColor(0xFF00FF00);
                    }
                default:
                    throw new IOException("Unexpected network data (Type:" + data.getType().name() + ")");
            }
        });
        listener.setFatalListener(e -> {
            Toast.makeText(this,
                    String.format(getResources().getString(R.string.clientError) + "\n", e.getMessage()), Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}