package com.github.nickid2018.simple2048.display;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.github.nickid2018.simple2048.R;
import com.github.nickid2018.simple2048.SimpleGameActivity;
import com.github.nickid2018.simple2048.datasave.Settings;
import com.github.nickid2018.simple2048.gamedata.GameTable;
import com.github.nickid2018.simple2048.gamedata.MoveDirection;
import com.github.nickid2018.simple2048.replay.SimpleGameRecorder;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class TableView extends View {

    public static final double LN2 = Math.log(2);

    private final Drawable background;
    private final Drawable foreground;
    private final Bitmap[] numberFores = new Bitmap[64];
    public SimpleGameActivity main;
    private LongConsumer historyMax;
    private LongConsumer nowMax;
    private LongConsumer nowScore;
    private Runnable extraValueHandler;
    private IntConsumer statusListener;
    private Bitmap backgroundBitmap;
    private TableRenderer renderer;
    private boolean moveValid = false;
    private float startX;
    private float startY;

    // Render Functions -------

    public TableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        background = context.getResources().getDrawable(R.drawable.table_background, context.getTheme());
        foreground = context.getResources().getDrawable(R.drawable.table_foreground, context.getTheme());
        setOnTouchListener(this::onTouch);
        setWillNotDraw(false);
        setAlpha(Settings.alphaForeground);
    }

    public void setTableRenderer(TableRenderer renderer) {
        this.renderer = renderer;
    }

    public TableRenderer getRenderer() {
        return renderer;
    }

    private void prepareBackground() {
        int size = getWidth();
        backgroundBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(backgroundBitmap);
        background.setBounds(0, 0, size, size);
        background.draw(canvas);
    }

    public Bitmap getBackgroundBitmap() {
        if (backgroundBitmap == null)
            prepareBackground();
        return backgroundBitmap;
    }

    public Bitmap requestNumber(long value) {
        int index = (int) (Math.log(value) / LN2);
        if (numberFores[index - 1] != null)
            return numberFores[index - 1];
        int size = getWidth() * 22 / 108;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        foreground.setBounds(0, 0, size, size);
        foreground.draw(canvas);
        int numberColor = Settings.numberColors[index - 1];
        String numStr = value + "";
        int count = numStr.length();
        canvas.drawColor(numberColor, PorterDuff.Mode.MULTIPLY);
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setLinearText(true);
        textPaint.setTextSize(count < 4 ? size / 2f : size / 3f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float[] hsv = new float[3];
        Color.colorToHSV(numberColor, hsv);
        if (hsv[2] <= 0.8f)
            textPaint.setColor(0xFFEEEEEE);
        canvas.drawText(numStr, size / 2f, count < 4 ? size / 24f * 17 : size / 8f * 5, textPaint);
        numberFores[index - 1] = bitmap;
        return bitmap;
    }

    public float transformPositionX(int pos) {
        return pos * getWidth() / 108f;
    }

    public float transformPositionY(int pos) {
        return (getHeight() - getWidth()) / 2f + transformPositionX(pos);
    }

    public float getSlotX(int slot) {
        int slotX = slot % 4;
        return transformPositionX(4 + slotX * 26);
    }

    public float getSlotY(int slot) {
        int slotY = slot / 4;
        return transformPositionY(4 + slotY * 26);
    }

    // Status Functions ------

    public void setNowScoreListener(LongConsumer nowScore) {
        this.nowScore = nowScore;
    }

    public void setNowMaxListener(LongConsumer nowMax) {
        this.nowMax = nowMax;
    }

    public void setMaxScoreListener(LongConsumer historyMax) {
        this.historyMax = historyMax;
    }

    public void setExtraValueHandler(Runnable extraValueHandler) {
        this.extraValueHandler = extraValueHandler;
    }

    public void setStatusListener(IntConsumer statusListener) {
        this.statusListener = statusListener;
    }

    public void setNowScore(long score) {
        if (nowScore != null)
            nowScore.accept(score);
    }

    public void setNowMax(long max) {
        if (nowMax != null)
            nowMax.accept(max);
    }

    public void setMaxScore(long max) {
        if (historyMax != null)
            historyMax.accept(max);
    }

    public void updateExtra() {
        if (extraValueHandler != null)
            extraValueHandler.run();
    }

    public void sendStatus(int status) {
        if (statusListener != null)
            statusListener.accept(status);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderer.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        setMeasuredDimension(w, h);
        backgroundBitmap = null;
        clearNumbers();
    }

    public void clearNumbers() {
        Arrays.fill(numberFores, null);
    }

    private boolean onTouch(View view, MotionEvent event) {
        if (!moveValid && event.getAction() == MotionEvent.ACTION_MOVE) {
            moveValid = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP && moveValid) {
            moveValid = false;
            if (!renderer.isCooling() && renderer.canMove()) {
                float horizonDelta = event.getX() - startX;
                float verticalDelta = event.getY() - startY;
                float absH = Math.abs(horizonDelta);
                float absV = Math.abs(verticalDelta);
                MoveDirection direction;
                if (absH > absV) {
                    if (absH < 100 && Settings.antiMisTouch)
                        return true;
                    direction = horizonDelta < 0 ? MoveDirection.LEFT : MoveDirection.RIGHT;
                } else {
                    if (absV < 100 && Settings.antiMisTouch)
                        return true;
                    direction = verticalDelta < 0 ? MoveDirection.UP : MoveDirection.DOWN;
                }
                renderer.fireMove(direction);
                invalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
        }
        return true;
    }
}