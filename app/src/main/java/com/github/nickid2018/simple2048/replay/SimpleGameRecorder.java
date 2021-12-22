package com.github.nickid2018.simple2048.replay;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.github.nickid2018.simple2048.R;
import com.github.nickid2018.simple2048.datasave.ScoreDataStorage;
import com.github.nickid2018.simple2048.display.SpawnData;
import com.github.nickid2018.simple2048.display.TableEventListener;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.display.TableView;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;
import com.github.nickid2018.simple2048.gamedata.RollbackEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class SimpleGameRecorder implements TableEventListener, RollbackEventListener {

    private ReplayData replay;
    private RollbackData rollback;

    public SimpleGameRecorder() {
    }

    @Override
    public boolean moveValid(TableRenderer renderer) {
        return true;
    }

    public ReplayData getReplay() {
        return replay;
    }

    public void setReplay(ReplayData replay) {
        this.replay = replay;
    }

    public RollbackData getRollback() {
        return rollback;
    }

    public void setRollback(RollbackData rollback) {
        this.rollback = rollback;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onEnd(TableRenderer renderer) {
        TableView view = renderer.getTableView();
        ScoreDataStorage.newRecord(renderer.getTable().getScore());
        AlertDialog dialog = new AlertDialog.Builder(view.main).create();
        if (Math.random() < 0.005) {
            dialog.setTitle(R.string.easterEgg_gameOver);
        } else {
            dialog.setTitle(R.string.gameOver);
        }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                view.getResources().getText(R.string.startNext), (dialogI, which) -> renderer.startNext());
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                view.getResources().getText(R.string.saveReplay), (dialogI, which) -> {
                    // Replay
                    EditText ownerText = new EditText(view.getContext());
                    ownerText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(255)});
                    new AlertDialog.Builder(view.main).setTitle(R.string.replayOwner).setView(ownerText)
                            .setCancelable(false).setPositiveButton(R.string.ok, (dialogII, whichI) -> {
                                String ownerName = ownerText.getText().toString();
                                if (ownerName.isEmpty())
                                    ownerName = "Unknown";
                                replay.setOwner(ownerName);
                                replay.setDate(new Date());
                                EditText nameText = new EditText(view.getContext());
                                nameText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(255)});
                                new AlertDialog.Builder(view.main).setCancelable(false).setTitle(R.string.replayName).setView(nameText)
                                        .setPositiveButton(R.string.ok, (dialogIII, whichII) -> {
                                            String name = nameText.getText().toString();
                                            if (name.isEmpty())
                                                name = String.format("%tF %tT", new Date(), new Date());
                                            String finalName = name;
                                            File base = view.main.getExternalFilesDir(null);
                                            File replayFile = new File(base, "replay");
                                            if (!replayFile.isDirectory())
                                                replayFile.mkdirs();
                                            try {
                                                replay.setName(finalName);
                                                replay.setMaxValue(renderer.getTable().getMaxValue());
                                                replay.setScore(renderer.getTable().getScore());
                                                replay.serialize(new FileOutputStream(new File(replayFile, finalName + ".replay")));
                                                Toast.makeText(view.getContext(), R.string.replaySaved, Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Log.e("Saving replay", "Error", e);
                                                Toast.makeText(view.getContext(), R.string.replaySaveFail, Toast.LENGTH_SHORT).show();
                                            }
                                            renderer.startNext();
                                        }).show();
                            }).show();
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onInitialize(TableRenderer renderer) {
        replay = new ReplayData(ReplayData.STATUS_RECORDING);
        replay.setInitial(renderer.getTable());
        rollback = new RollbackData();
    }

    @Override
    public void onMove(TableRenderer renderer, MoveDirection direction, SpawnData spawnData) {
        replay.move(direction, spawnData);
        renderer.updateValues(
                TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE | TableRenderer.FLAG_UPDATE_MAX_SCORE);
    }

    @Override
    public void onAnimationOver(TableRenderer renderer) {
    }

    @Override
    public void receiveRollbackData(RollbackEntry entry) {
        rollback.pushEntry(entry);
    }
}
