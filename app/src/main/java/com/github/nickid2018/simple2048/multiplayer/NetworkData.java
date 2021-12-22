package com.github.nickid2018.simple2048.multiplayer;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkData {

    private final NetworkDataType type;
    private final String jsonData;

    public NetworkData(NetworkDataType type, String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }

    public static NetworkData deserialize(String data) throws JSONException {
        JSONObject object = new JSONObject(data);
        NetworkDataType type = NetworkDataType.valueOf(object.getString("dataType"));
        return new NetworkData(type, object.getString("data"));
    }

    public NetworkDataType getType() {
        return type;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String serialize() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("dataType", type);
        object.put("data", jsonData);
        return object.toString();
    }
}
