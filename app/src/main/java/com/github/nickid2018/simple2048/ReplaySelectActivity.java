package com.github.nickid2018.simple2048;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.replay.ReplayData;

import java.io.File;
import java.util.*;

public class ReplaySelectActivity extends AppCompatActivity {

    public static final int RESULT_ADD_FILE = 0;
    private final List<Map<String, Object>> data = new ArrayList<>();
    private final List<Object> sources = new ArrayList<>();
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay_select);

        File[] replayFiles = new File(getExternalFilesDir(null), "replay")
                .listFiles(name -> name.getName().endsWith(".replay"));
        ListView listView = findViewById(R.id.replayFileLists);
        if (replayFiles != null)
            for (File now : replayFiles) {
                Map<String, Object> nowMap = new HashMap<>();
                String fileName = now.getName();
                ReplayData replay = new ReplayData(ReplayData.STATUS_READING);
                try {
                    replay.preLoad(now);
                    nowMap.put("replayName", replay.isLegacy() ? fileName.substring(0, fileName.lastIndexOf('.')) : replay.getName());
                    nowMap.put("replayAuthor", replay.getOwner());
                    nowMap.put("replayDate", String.format(Locale.getDefault(), "%tF %tT", replay.getDate(), replay.getDate()));
                    if (replay.isLegacy())
                        nowMap.put("replayOperations", getString(R.string.operationCount) + " " + replay.entrySize());
                    else
                        nowMap.put("replayOperations", getString(R.string.operationCount) + " " + replay.entrySize()
                                + " " + getString(R.string.nowMaxValue) + " " + replay.getMaxValue()
                                + " " + getString(R.string.replayScore) + " " + replay.getScore());
                } catch (Exception e) {
                    Log.e("Replay System", "Reading replay", e);
                    nowMap.put("replayName", fileName.substring(0, fileName.lastIndexOf('.')));
                    nowMap.put("replayAuthor", getString(R.string.unknownReplay));
                    nowMap.put("replayDate", getString(R.string.unknownReplay));
                    nowMap.put("replayOperations", getString(R.string.unknownReplay));
                }
                data.add(nowMap);
                sources.add(now);
            }
        TextView addPane = new TextView(this);
        addPane.setText(R.string.outsideReplay);
        addPane.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        addPane.setOnClickListener(this::onAddNewReplay);
        Drawable picAdd = getResources().getDrawable(R.drawable.pic_add, getTheme());
        float px = addPane.getTextSize();
        picAdd.setBounds(0, 0, (int) (px * 2 / 3), (int) (px * 2 / 3));
        addPane.setCompoundDrawables(picAdd, null, null, null);
        listView.setAdapter(adapter = new SimpleAdapter(this, data, R.layout.item_replay,
                new String[]{"replayName", "replayAuthor", "replayDate", "replayOperations"},
                new int[]{R.id.replayName, R.id.replayAuthor, R.id.replayDate, R.id.replayOperations}));
        listView.addFooterView(addPane);
        listView.setOnItemClickListener(this::selectReplay);
    }

    private void onAddNewReplay(View now) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, RESULT_ADD_FILE);
    }

    private void selectReplay(AdapterView<?> view, View now, int pos, long id) {
        Intent intent = null;
        Object nowDataSource = sources.get(pos);
        if (nowDataSource instanceof File) {
            intent = new Intent();
            intent.setData(Uri.fromFile((File) nowDataSource));
        }
        if (nowDataSource instanceof Uri) {
            intent = new Intent();
            intent.setData((Uri) nowDataSource);
        }
        assert intent != null;
        intent.putExtra("name", Objects.requireNonNull(data.get(pos).get("replayName")).toString());
        intent.setClass(this, ReplayActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_ADD_FILE && resultCode == RESULT_OK && data != null) {
            Uri now = data.getData();
            ReplayData replay = new ReplayData(ReplayData.STATUS_READING);
            Map<String, Object> nowMap = new HashMap<>();
            String path = now.getPath();
            try {
                replay.preLoad(now, getContentResolver());
                if (replay.isLegacy()) {
                    if (path == null)
                        nowMap.put("replayName", "Unknown Name");
                    else {
                        String[] split1 = path.split(":");
                        String name = split1[split1.length - 1];
                        nowMap.put("replayName", name.substring(0, name.lastIndexOf('.')));
                    }
                } else
                    nowMap.put("replayName", replay.getName());
                nowMap.put("replayDate", String.format(Locale.getDefault(), "%tF %tT", replay.getDate(), replay.getDate()));
                nowMap.put("replayAuthor", replay.getOwner());
                if (replay.isLegacy())
                    nowMap.put("replayOperations", getString(R.string.operationCount) + " " + replay.entrySize());
                else
                    nowMap.put("replayOperations", getString(R.string.operationCount) + " " + replay.entrySize()
                            + " " + getString(R.string.nowMaxValue) + " " + replay.getMaxValue()
                            + " " + getString(R.string.replayScore) + " " + replay.getScore());
            } catch (Exception e) {
                Log.e("Replay System", "Reading replay", e);
                Toast.makeText(this, R.string.unknownReplay, Toast.LENGTH_SHORT).show();
            }
            this.data.add(nowMap);
            sources.add(now);
            adapter.notifyDataSetChanged();
        }
    }
}