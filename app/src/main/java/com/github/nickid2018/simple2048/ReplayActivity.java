package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.display.TableView;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.replay.ReplayData;
import com.github.nickid2018.simple2048.replay.ReplayRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class ReplayActivity extends AppCompatActivity {

    private final ReplayData dataNow = new ReplayData(ReplayData.STATUS_READING);
    private boolean prevState;
    private boolean corrupted = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.initialize(this);
        setContentView(R.layout.activity_replay);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initTableReplay(findViewById(R.id.replayTableView));
        findViewById(R.id.button_replayRun).setOnClickListener(view -> ReplayRunner.instance.pause());
        findViewById(R.id.button_replaySetting).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivityForResult(intent, 0);
        });
        findViewById(R.id.button_replayStop).setOnClickListener(view -> {
            ReplayRunner.instance.pause();
            new AlertDialog.Builder(this).setTitle(R.string.forceStopReplay)
                    .setNegativeButton(R.string.yes, (dialog, i) -> ReplayRunner.instance.forceStop())
                    .setPositiveButton(R.string.no, (dialog, i) -> ReplayRunner.instance.unpause())
                    .setOnCancelListener(dialog -> ReplayRunner.instance.unpause())
                    .show();
        });
        initBackground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prevState = ReplayRunner.instance.getReplayThread().isRunning();
        ReplayRunner.instance.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prevState)
            ReplayRunner.instance.unpause();
    }

    public void onJump(View view) {
        prevState = ReplayRunner.instance.getReplayThread().isRunning();
        ReplayRunner.instance.pause();
        NumberPicker picker = new NumberPicker(this);
        picker.setMaxValue(dataNow.entrySize());
        picker.setValue(ReplayRunner.instance.getReplayThread().getReplayNowIndex());
        picker.setMinValue(0);
        new AlertDialog.Builder(this).setView(picker)
                .setTitle(R.string.replayJump).setCancelable(false)
                .setPositiveButton(R.string.ok, (dialogI, v) -> {
                    TextView info = new TextView(this);
                    info.setText(R.string.replayJumping);
                    AlertDialog dialog = new AlertDialog.Builder(this).setView(info).setCancelable(false).show();
                    int result = ReplayRunner.instance.forceJump(picker.getValue());
                    if (result == 0 || result == 3)
                        dialog.cancel();
                    if (prevState)
                        ReplayRunner.instance.unpause();
                })
                .setNegativeButton(R.string.no, (dialogI, v) -> {
                    if (prevState)
                        ReplayRunner.instance.unpause();
                })
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void initTableReplay(TableView table) {
        Intent intent = getIntent();
        try {
            dataNow.deserialize(intent.getData(), getContentResolver());
            ((TextView) findViewById(R.id.replayingInfoLabel))
                    .setText(getString(R.string.replaying)
                            + (intent.getExtras() == null ? dataNow.getName() : intent.getStringExtra("name"))
                            + " - " + dataNow.getOwner());
            ProgressBar bar = findViewById(R.id.replayProgressBar);
            ImageView replayRun = findViewById(R.id.button_replayRun);
            TextView progress = findViewById(R.id.progressLabel);
            bar.setMax(dataNow.entrySize());
            progress.setText(getString(R.string.progress) + " 0/" + bar.getMax());
            table.setNowMaxListener(val -> ((TextView) findViewById(R.id.replayMaxValue)).setText(val + ""));
            table.setNowScoreListener(val -> ((TextView) findViewById(R.id.replayNowValue)).setText(val + ""));
            table.setExtraValueHandler(() -> {
                int now = ReplayRunner.instance.getReplayThread().getReplayNowIndex();
                bar.setProgress(now, true);
                progress.setText(getString(R.string.progress) + " " + now + "/" + bar.getMax());
            });
            table.setStatusListener(status -> {
                switch (status) {
                    case ReplayRunner.STATUS_FINISH:
                        finish();
                        break;
                    case ReplayRunner.STATUS_PAUSE:
                        replayRun.setOnClickListener(view -> ReplayRunner.instance.unpause());
                        replayRun.setImageDrawable(getResources().getDrawable(R.drawable.run_play, getTheme()));
                        break;
                    case ReplayRunner.STATUS_UNPAUSE:
                        replayRun.setOnClickListener(view -> ReplayRunner.instance.pause());
                        replayRun.setImageDrawable(getResources().getDrawable(R.drawable.run_pause, getTheme()));
                        break;
                    case ReplayRunner.STATUS_RE_PLAY:
                        bar.setProgress(0, true);
                        progress.setText(getString(R.string.progress) + " 0/" + bar.getMax());
                        break;
                }
            });
            ReplayRunner runner = ReplayRunner.instance;
            TableRenderer renderer = new TableRenderer(table, new GameTable(4), runner);
            runner.init(renderer, dataNow);
            table.setTableRenderer(renderer);
        } catch (Exception e) {
            Toast.makeText(this, R.string.unknownReplay, Toast.LENGTH_SHORT).show();
            Log.e("Replay System", "Start Replay", e);
            corrupted = true;
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && data != null) {
            Bundle bundle = data.getExtras();
            if (bundle.getBoolean("background"))
                initBackground();
            if (bundle.getBoolean("alphaChanged"))
                findViewById(R.id.replayTableView).setAlpha(Settings.alphaForeground);
        }
    }

    private void initBackground() {
        if (Settings.backgroundSet)
            try {
                findViewById(R.id.replayBase).setBackground(Drawable.createFromStream(
                        new FileInputStream(new File(getDataDir(), "background.jpeg")), null));
            } catch (Exception e) {
                Log.e("Render", "Set Background", e);
            }
        else
            findViewById(R.id.replayBase).setBackground(null);
    }
}