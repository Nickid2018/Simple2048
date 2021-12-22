package com.github.nickid2018.simple2048.replay;

import androidx.core.util.Pair;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.gamedata.GameTable;

public class ReplayThread extends Thread {

    public static final int JUMP_SUCCESS = 0;
    // Impossible
    public static final int JUMP_ERROR_EXCEED = 1;
    // Impossible
    public static final int JUMP_ERROR_NEGATIVE = 2;
    // Ignore
    public static final int JUMP_ERROR_NO_JUMP = 3;
    private final ReplayData data;
    private final TableRenderer renderer;
    private final ReplayRunner runner;
    private int replayNowIndex = 0;
    private int replayRollSession = 0;
    private volatile boolean running = true;
    private volatile boolean kill = false;

    public ReplayThread(ReplayData data, TableRenderer renderer, ReplayRunner runner) {
        this.data = data;
        this.renderer = renderer;
        this.runner = runner;
    }

    public void stopNow() {
        running = false;
    }

    public void startNow() {
        running = true;
    }

    public void killNow() {
        kill = true;
    }

    public boolean isRunning() {
        return running;
    }

    public int getReplayNowIndex() {
        return replayNowIndex;
    }

    public int forceJumpTo(int index) {
        if (index > data.entrySize())
            return JUMP_ERROR_EXCEED;
        if (index < 0)
            return JUMP_ERROR_NEGATIVE;
        if (index == replayNowIndex)
            return JUMP_ERROR_NO_JUMP;
        while (running)
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        if (index < replayNowIndex)
            move(false, index);
        else
            move(true, index - replayNowIndex);
        return JUMP_SUCCESS;
    }

    private void move(boolean fromNow, int steps) {
        GameTable table;
        if (fromNow)
            table = renderer.getTable().deepCopy();
        else {
            table = new GameTable(renderer.getTable().size());
            table.setInitial(data.getInitial());
            replayNowIndex = 0;
            replayRollSession = 0;
        }
        renderer.doClear();
        for (int i = 0; i < steps; i++) {
            Pair<Long, Long> value;
            if ((value = this.data.getRollback(replayRollSession)) != null) {
                if (value.first == replayNowIndex) {
                    replayRollSession++;
                    renderer.getTable().setScore(value.second);
                }
            }
            ReplayEntry data = this.data.getEntryNow(replayNowIndex++);
            table.internalMove(data.getDirection());
            table.spawnAt(data.getSpawnData());
        }
        renderer.setTable(table);
        renderer.updateValues(TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE | TableRenderer.FLAG_UPDATE_EXTRA_VALUE);
        renderer.getTableView().invalidate();
        runner.replayEnd = !renderer.getTable().checkContinue();
    }

    @Override
    public void run() {
        while (replayNowIndex < data.entrySize() && !kill) {
            try {
                Thread.sleep(Settings.animationDuring + Settings.replayDuration);
            } catch (InterruptedException ignored) {
            }
            Pair<Long, Long> value;
            if ((value = data.getRollback(replayRollSession)) != null) {
                if(value.first == replayNowIndex) {
                    replayRollSession++;
                    renderer.getTable().setScore(value.second);
                    renderer.updateValues(TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE | TableRenderer.FLAG_UPDATE_EXTRA_VALUE);
                    try {
                        Thread.sleep(Settings.replayDuration * 2L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            if (running) {
                if (replayNowIndex < data.entrySize()) {
                    ReplayEntry data = this.data.getEntryNow(replayNowIndex++);
                    renderer.getTable().internalMove(data.getDirection());
                    renderer.getTable().spawnAt(data.getSpawnData());
                    renderer.coolDown();
                    renderer.updateValues(TableRenderer.FLAG_UPDATE_NOW_VALUE | TableRenderer.FLAG_UPDATE_MAX_VALUE | TableRenderer.FLAG_UPDATE_EXTRA_VALUE);
                    renderer.getTableView().invalidate();
                }
                runner.replayEnd = !renderer.getTable().checkContinue();
            }
        }
    }
}
