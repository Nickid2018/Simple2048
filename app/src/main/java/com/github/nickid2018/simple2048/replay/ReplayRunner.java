package com.github.nickid2018.simple2048.replay;

import android.app.AlertDialog;
import com.github.nickid2018.simple2048.R;
import com.github.nickid2018.simple2048.display.SpawnData;
import com.github.nickid2018.simple2048.display.TableEventListener;
import com.github.nickid2018.simple2048.display.TableRenderer;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;

public class ReplayRunner implements TableEventListener {

    public static final int STATUS_FINISH = 0;
    public static final int STATUS_PAUSE = 1;
    public static final int STATUS_UNPAUSE = 2;
    public static final int STATUS_RE_PLAY = 3;

    public static ReplayRunner instance = new ReplayRunner();

    protected boolean replayEnd;
    private ReplayData replay;
    private ReplayThread replayThread;
    private TableRenderer renderer;

    public ReplayThread getReplayThread() {
        return replayThread;
    }

    public void pause() {
        if (replayThread != null)
            replayThread.stopNow();
        renderer.sendStatus(STATUS_PAUSE);
    }

    public void unpause() {
        if (replayThread != null)
            replayThread.startNow();
        renderer.sendStatus(STATUS_UNPAUSE);
    }

    public void forceStop() {
        if (replayThread != null)
            replayThread.killNow();
        renderer.sendStatus(STATUS_FINISH);
    }

    public int forceJump(int index) {
        return replayThread.forceJumpTo(index);
    }

    public void init(TableRenderer renderer, ReplayData data) {
        if (replayThread != null)
            replayThread.killNow();
        replayEnd = false;
        replay = data;
        renderer.doClear();
        renderer.getTable().setInitial(data.getInitial());
        renderer.getTableView().invalidate();
        replayThread = new ReplayThread(replay, renderer, this);
        replayThread.start();
        this.renderer = renderer;
    }

    @Override
    public boolean moveValid(TableRenderer renderer) {
        return false;
    }

    @Override
    public void onEnd(TableRenderer renderer) {
    }

    @Override
    public void onInitialize(TableRenderer renderer) {
    }

    @Override
    public void onMove(TableRenderer renderer, MoveDirection direction, SpawnData spawnData) {
    }

    @Override
    public void onAnimationOver(TableRenderer renderer) {
        if (replayEnd)
            new AlertDialog.Builder(renderer.getTableView().getContext()).setTitle(R.string.replayEndAndLoop)
                    .setNegativeButton(R.string.yes, (dialog, v) -> {
                        renderer.sendStatus(STATUS_RE_PLAY);
                        init(renderer, replay);
                    })
                    .setPositiveButton(R.string.no, (dialog, v) -> renderer.sendStatus(STATUS_FINISH))
                    .setCancelable(false)
                    .show();
    }
}
