package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.datasave.GameDataStorage;
import com.github.nickid2018.simple2048.datasave.ScoreDataStorage;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.display.TableView;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.replay.SimpleGameRecorder;

import java.io.File;
import java.io.FileInputStream;

public class SimpleGameActivity extends AppCompatActivity {

    public static final int RETURN_SETTINGS = 0;
    public static final int RETURN_SCORE = 1;

    public TableView table;

    @SuppressLint({"SourceLockedOrientationActivity", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_simple_game);
        ScoreDataStorage.initialize(this);
        table = findViewById(R.id.tableView);
        SimpleGameRecorder recorder = new SimpleGameRecorder();
        table.setTableRenderer(new TableRenderer(table, new GameTable(4), recorder));
        table.getRenderer().getTable().setRollbackListener(recorder);
        table.setMaxScoreListener(val -> ((TextView) findViewById(R.id.maxResultText)).setText(val + ""));
        table.setNowMaxListener(val -> ((TextView) findViewById(R.id.maxValueText)).setText(val + ""));
        table.setNowScoreListener(val -> ((TextView) findViewById(R.id.nowValueText)).setText(val + ""));
        table.main = this;
        table.getRenderer().doClear();
        GameDataStorage.init(this);
        if (GameDataStorage.haveLastData())
            new AlertDialog.Builder(this).setTitle(R.string.haveLastData)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, (dialogI, view) -> {
                        try {
                            GameDataStorage.loadData(table.getRenderer(), recorder);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, R.string.gameDataLoadFailed, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.no, null).show();
        findViewById(R.id.restart).setOnClickListener(this::restart);
        findViewById(R.id.setting).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivityForResult(intent, RETURN_SETTINGS);
        });
        findViewById(R.id.scores).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(this, ScoreActivity.class);
            startActivityForResult(intent, RETURN_SCORE);
        });
        initBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!table.getRenderer().isNewGame()) {
            try {
                GameDataStorage.saveData(table.getRenderer(), (SimpleGameRecorder) (table.getRenderer().getEventListener()));
            } catch (Exception e) {
                Log.e("2048 Save", "Save error", e);
            }
        } else
            GameDataStorage.FILE_CACHE.delete();
    }

    private void restart(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.restartWarning)
                .setPositiveButton(R.string.yes, (dialog, v) -> table.getRenderer().startNext())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public void rollback(View view) {
        SimpleGameRecorder recorder = (SimpleGameRecorder) table.getRenderer().getEventListener();
        if (recorder.getRollback().hasRollback()) {
            table.getRenderer().getTable().rollback(recorder.getRollback().popLastEntry(), recorder.getReplay().popLast());
            recorder.getRollback().recordRollback(recorder.getReplay().entrySize(), table.getRenderer().getTable().getScore());
            recorder.getReplay().recordRollback(recorder.getReplay().entrySize(), table.getRenderer().getTable().getScore());
            table.getRenderer().updateValues(
                    TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE | TableRenderer.FLAG_UPDATE_MAX_SCORE);
            table.invalidate();
        } else
            Toast.makeText(this, R.string.noRollback, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RETURN_SETTINGS && resultCode == RETURN_SETTINGS && data != null) {
            Bundle bundle = data.getExtras();
            if (bundle.getBoolean("background"))
                initBackground();
            if (bundle.getBoolean("colorChanged")) {
                table.clearNumbers();
                table.invalidate();
            }
            if (bundle.getBoolean("alphaChanged"))
                table.setAlpha(Settings.alphaForeground);
        }
    }

    private void initBackground() {
        if (Settings.backgroundSet)
            try {
                findViewById(R.id.base).setBackground(Drawable.createFromStream(
                        new FileInputStream(new File(getDataDir(), "background.jpeg")), null));
            } catch (Exception e) {
                Log.e("Render", "Set Background", e);
            }
        else
            findViewById(R.id.base).setBackground(null);
    }
}