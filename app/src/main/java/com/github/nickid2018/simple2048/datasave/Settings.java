package com.github.nickid2018.simple2048.datasave;

import android.app.Activity;
import android.util.Log;
import com.github.nickid2018.simple2048.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;

public class Settings {

    public static File SETTINGS_JSON_FILE;

    public static boolean backgroundSet;
    public static float alphaForeground = 0.8f;
    public static int animationDuring = 200;
    public static int replayDuration = 500;
    public static boolean antiMisTouch = false;
    public static int[] numberColors = new int[63]; // 1-63 (2-2^63)

    private static boolean initialized = false;

    public static void initialize(Activity activity) {
        if (initialized)
            return;
        SETTINGS_JSON_FILE = new File(activity.getDataDir(), "settings.json");
        defaultColorFill(activity);
        if (SETTINGS_JSON_FILE.isFile()) {
            try {
                load();
            } catch (Exception e) {
                save();
                Log.e("Settings", "Reading settings file", e);
            }
        } else {
            try {
                SETTINGS_JSON_FILE.createNewFile();
                save();
            } catch (IOException e) {
                Log.e("Settings", "Creating settings file", e);
            }
        }
        initialized = true;
    }

    private static void defaultColorFill(Activity activity) {
        for (int i = 0; i < 63; i++)
            numberColors[i] = getNumberColor(2L << i, activity);
    }

    public static int getNumberColor(long number, Activity activity) {
        try {
            Field fieldR = R.color.class.getField("number_" + number);
            return activity.getColor(fieldR.getInt(null));
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    private static void load() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_JSON_FILE));
        StringBuilder builder = new StringBuilder();
        reader.lines().forEach(builder::append);
        JSONObject object = new JSONObject(builder.toString());
        animationDuring = object.getInt("animationDuring");
        backgroundSet = object.getBoolean("background");
        replayDuration = object.getInt("replayDuration");
        antiMisTouch = object.getBoolean("anti-mis-touch");
        alphaForeground = (float) object.getDouble("alphaForeground");
        JSONArray array = object.getJSONArray("numbers");
        for (int i = 0; i < 63; i++)
            numberColors[i] = array.getInt(i);
        reader.close();
    }

    public static void save() {
        try {
            JSONObject object = new JSONObject();
            object.put("animationDuring", animationDuring);
            object.put("background", backgroundSet);
            object.put("replayDuration", replayDuration);
            object.put("anti-mis-touch", antiMisTouch);
            object.put("alphaForeground", alphaForeground);
            JSONArray array = new JSONArray();
            for (int color : numberColors)
                array.put(color);
            object.put("numbers", array);
            FileWriter writer = new FileWriter(SETTINGS_JSON_FILE);
            writer.write(object.toString());
            writer.close();
        } catch (Exception e) {
            Log.e("Settings", "Saving JSON", e);
        }
    }
}
