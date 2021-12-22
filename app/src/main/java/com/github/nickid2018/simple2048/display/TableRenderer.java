package com.github.nickid2018.simple2048.display;

import android.graphics.*;
import android.widget.Toast;
import com.github.nickid2018.simple2048.R;
import com.github.nickid2018.simple2048.datasave.ScoreDataStorage;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TableRenderer {

    public static final int FLAG_UPDATE_NOW_VALUE = 1;
    public static final int FLAG_UPDATE_MAX_VALUE = 2;
    public static final int FLAG_UPDATE_MAX_SCORE = 4;
    public static final int FLAG_UPDATE_SCORE_HIGHEST = 8;
    public static final int FLAG_UPDATE_EXTRA_VALUE = 16;

    private final TableView view;
    private final Paint paint = new Paint();
    private final MoveData[] moves = new MoveData[16];
    private final Set<StayData> stays = new HashSet<>();
    private final TableEventListener eventListener;
    private GameTable table;
    private int moveCoolDown = 0;
    private long lastTime;
    private SpawnData spawn;
    private boolean isNewGame;

    public TableRenderer(TableView view, GameTable table, TableEventListener listener) {
        this.view = view;
        setTable(table);
        eventListener = listener;
        eventListener.onInitialize(this);
    }

    public TableEventListener getEventListener() {
        return eventListener;
    }

    public boolean isNewGame() {
        return isNewGame;
    }

    public void setNewGame(boolean newGame) {
        isNewGame = newGame;
    }

    public TableView getTableView() {
        return view;
    }

    public GameTable getTable() {
        return table;
    }

    public void setTable(GameTable table) {
        this.table = table;
        table.setMoveListener(this::onMove);
        table.setSpawnListener(this::onSpawn);
        table.setStayListener(
                (line, index, direction) -> stays.add(StayData.create(table.fromLineToSlot(line, index, direction))));
    }

    private void onSpawn(int row, int column, long data) {
        spawn = SpawnData.create(row * table.size() + column, data);
    }

    private void onMove(int line, int from, int to, MoveDirection direction, long source, long result) {
        int slotNow = table.fromLineToSlot(line, from, direction);
        int slotTo = table.fromLineToSlot(line, to, direction);
        moves[slotNow] = MoveData.create(slotTo, source, result);
        stays.removeIf(data -> data.row * table.size() + data.column == slotTo);
    }

    public boolean canMove() {
        return eventListener.moveValid(this);
    }

    public void fireMove(MoveDirection direction) {
        if (table.doMove(direction)) {
            eventListener.onMove(this, direction, spawn);
            isNewGame = false;
            if (checkContinue())
                coolDown();
        } else
            Toast.makeText(view.getContext(), R.string.invalidMove, Toast.LENGTH_SHORT).show();
    }

    public void coolDown() {
        lastTime = System.currentTimeMillis();
        moveCoolDown = Settings.animationDuring;
    }

    public boolean isCooling() {
        if (moveCoolDown > 0) {
            long now = System.currentTimeMillis();
            moveCoolDown -= now - lastTime;
            lastTime = now;
        }
        return moveCoolDown > 0;
    }

    public boolean checkContinue() {
        if (!table.checkContinue()) {
            eventListener.onEnd(this);
            return false;
        }
        return true;
    }

    public void doClear() {
        updateValues(FLAG_UPDATE_NOW_VALUE | FLAG_UPDATE_MAX_VALUE | FLAG_UPDATE_SCORE_HIGHEST);
        Arrays.fill(moves, null);
        spawn = null;
        stays.clear();
        isNewGame = true;
    }

    public void startNext() {
        table.reset();
        doClear();
        eventListener.onInitialize(this);
        view.invalidate();
    }

    public void updateValues(int flag) {
        if ((flag & FLAG_UPDATE_NOW_VALUE) != 0)
            view.setNowScore(table.getScore());
        if ((flag & FLAG_UPDATE_MAX_VALUE) != 0)
            view.setNowMax(table.getMaxValue());
        if ((flag & FLAG_UPDATE_MAX_SCORE) != 0)
            if (ScoreDataStorage.getHighest() < table.getScore())
                view.setMaxScore(table.getScore());
        if ((flag & FLAG_UPDATE_SCORE_HIGHEST) != 0)
            view.setMaxScore(ScoreDataStorage.getHighest());
        if ((flag & FLAG_UPDATE_EXTRA_VALUE) != 0)
            view.updateExtra();
    }

    public void sendStatus(int status) {
        view.sendStatus(status);
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(view.getBackgroundBitmap(), 0, view.transformPositionY(0), paint);
        if (isCooling()) {
            if (moveCoolDown > Settings.animationDuring / 4) {
                for (int nowSlot = 0; nowSlot < 16; nowSlot++) {
                    MoveData data = moves[nowSlot];
                    if (data == null)
                        continue;
                    int endSlot = data.toSlot;
                    float progress = (Settings.animationDuring - moveCoolDown) / (Settings.animationDuring * 3 / 4f);
                    float x = view.getSlotX(endSlot) * progress + view.getSlotX(nowSlot) * (1 - progress);
                    float y = view.getSlotY(endSlot) * progress + view.getSlotY(nowSlot) * (1 - progress);
                    if (data.source == data.result)
                        canvas.drawBitmap(view.requestNumber(data.source), x, y, paint);
                    else if (progress > 0.5f)
                        canvas.drawBitmap(view.requestNumber(data.result), x, y, paint);
                    else
                        canvas.drawBitmap(view.requestNumber(data.source), x, y, paint);
                }
                for (StayData stay : stays) {
                    long number = table.get(stay.row, stay.column);
                    if (number != 0) {
                        int nowId = stay.row * 4 + stay.column;
                        canvas.drawBitmap(view.requestNumber(number), view.getSlotX(nowId),
                                view.getSlotY(nowId), paint);
                    }
                }
            } else if (spawn != null && spawn.data != 0) { // Thread Safe
                float scaleMin1 = moveCoolDown / (Settings.animationDuring / 4f);
                Bitmap dataNow = view.requestNumber(spawn.data);
                Rect src = new Rect(0, 0, dataNow.getWidth(), dataNow.getHeight());
                float border = dataNow.getHeight() * scaleMin1 / 2;
                float end = dataNow.getHeight() - border;
                int slot = spawn.slot;
                RectF dst = new RectF(view.getSlotX(slot) + border, view.getSlotY(slot) + border,
                        view.getSlotX(slot) + end, view.getSlotY(slot) + end);
                canvas.drawBitmap(dataNow, src, dst, paint);
                for (int row = 0; row < 4; row++)
                    for (int column = 0; column < 4; column++) {
                        long number = table.get(row, column);
                        if (number != 0 && row * 4 + column != slot) {
                            int nowId = row * 4 + column;
                            canvas.drawBitmap(view.requestNumber(number), view.getSlotX(nowId),
                                    view.getSlotY(nowId), paint);
                        }
                    }
            }
            drawCopyRight(canvas);
            view.invalidate();
        } else {
            for (int row = 0; row < 4; row++)
                for (int column = 0; column < 4; column++) {
                    long number = table.get(row, column);
                    if (number != 0) {
                        int nowId = row * 4 + column;
                        canvas.drawBitmap(view.requestNumber(number), view.getSlotX(nowId),
                                view.getSlotY(nowId), paint);
                    }
                }
            Arrays.fill(moves, null);
            spawn = null;
            stays.clear();
            drawCopyRight(canvas);
            eventListener.onAnimationOver(this);
        }
    }

    private void drawCopyRight(Canvas canvas) {
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(view.getResources().getColor(R.color.gray_400, view.getContext().getTheme()));
        paint.setTextSize(30);
        canvas.drawText(view.getResources().getText(R.string.copyright).toString(), view.getWidth(), view.getHeight() - 10, paint);
    }
}
