package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.display.ColorPickerDialog;

import java.util.*;

public class ChunkColorActivity extends AppCompatActivity {

    private final List<Map<String, Object>> data = new ArrayList<>();
    private ColorAdapter adapter;
    private boolean colorChanged = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_chunk_color);
        ListView listView = findViewById(R.id.numberColorPane);
        for (int i = 0; i < 63; i++) {
            Map<String, Object> color = new HashMap<>();
            color.put("itemName", (2L << i) + "");
            color.put("itemValue", Settings.numberColors[i]);
            data.add(color);
        }
        adapter = new ColorAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this::click);
        listView.setOnItemLongClickListener(this::longClick);
    }

    private void click(AdapterView<?> view, View now, int pos, long id) {
        new ColorPickerDialog(this, Settings.numberColors[pos], "hello", color -> {
            Settings.numberColors[pos] = color;
            data.get(pos).put("itemValue", color);
            adapter.notifyDataSetChanged();
            colorChanged = true;
        }).show();
    }

    public boolean longClick(AdapterView<?> parent, View view, int position, long id) {
        int def = Settings.getNumberColor(2L << position, this);
        Settings.numberColors[position] = def;
        data.get(position).put("itemValue", def);
        adapter.notifyDataSetChanged();
        colorChanged = true;
        return true;
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("colorChanged", colorChanged);
        setResult(SettingsActivity.RESULT_COLOR, intent);
        super.finish();
    }

    private class ColorAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(ChunkColorActivity.this).inflate(R.layout.item_setting, parent, false);
            ((TextView) convertView.findViewById(R.id.itemName)).setText(data.get(position).get("itemName").toString());
            ((TextView) convertView.findViewById(R.id.itemValue)).setText(
                    Integer.toHexString((Integer) data.get(position).get("itemValue")).toUpperCase(Locale.ROOT));
            convertView.setBackgroundColor((Integer) data.get(position).get("itemValue"));
            return convertView;
        }
    }
}