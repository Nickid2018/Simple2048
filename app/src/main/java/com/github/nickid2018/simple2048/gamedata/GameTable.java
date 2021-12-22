package com.github.nickid2018.simple2048.gamedata;

import com.github.nickid2018.simple2048.display.SpawnData;
import com.github.nickid2018.simple2048.replay.InitialEntry;
import com.github.nickid2018.simple2048.replay.ReplayEntry;
import com.github.nickid2018.simple2048.replay.RollbackEntry;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

public class GameTable {

    private final int size;
    private final long[][] table;
    private final Random random = new Random();
    private final int[] valueSpared;
    private int spareCount;
    private long maxValue;
    private long score;

    private MoveEventListener moveListener;
    private SpawnEventListener spawnListener;
    private StayEventListener stayListener;
    private RollbackEventListener rollbackListener;

    public GameTable(int size) {
        if (size > 8 || size < 3)
            throw new IllegalArgumentException();
        this.size = size;
        table = new long[size][size];
        score = 0;
        int[] valueSpared = new int[spareCount = size * size];
        for (int i = 0; i < size * size; i++)
            valueSpared[i] = i;
        this.valueSpared = valueSpared;
        spawnRandomValue();
        spawnRandomValue();
    }

    public void reset() {
        clear();
        spawnRandomValue();
        spawnRandomValue();
    }

    public GameTable deepCopy() {
        GameTable table = new GameTable(size);
        table.spareCount = spareCount;
        table.maxValue = maxValue;
        table.score = score;
        System.arraycopy(valueSpared, 0, table.valueSpared, 0, spareCount);
        for (int i = 0; i < size; i++)
            System.arraycopy(this.table[i], 0, table.table[i], 0, size);
        return table;
    }

    private void clear() {
        for (int row = 0; row < size; row++)
            for (int column = 0; column < size; column++)
                set(row, column, 0);
        for (int i = 0; i < size * size; i++)
            valueSpared[i] = i;
        spareCount = size * size;
        score = 0;
        maxValue = 0;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public void setMoveListener(MoveEventListener listener) {
        moveListener = listener;
    }

    public void setSpawnListener(SpawnEventListener listener) {
        spawnListener = listener;
    }

    public void setStayListener(StayEventListener listener) {
        stayListener = listener;
    }

    public void setRollbackListener(RollbackEventListener rollbackListener) {
        this.rollbackListener = rollbackListener;
    }

    public boolean doMove(MoveDirection direction) {
        boolean success = internalMove(direction);
        if (success)
            // If moved successfully, spawn new value
            spawnRandomValue();
        return success;
    }

    public void setInitial(InitialEntry init) {
        clear();
        set(init.getSlot1() / size, init.getSlot1() % size, init.getValue1());
        set(init.getSlot2() / size, init.getSlot2() % size, init.getValue2());
    }

    public int fromLineToSlot(int line, int index, MoveDirection direction) {
        switch (direction) {
            case UP:
                return index * size + line;
            case DOWN:
                return (size - 1 - index) * size + line;
            case LEFT:
                return line * size + index;
            case RIGHT:
                return (line + 1) * size - 1 - index;
            default:
                return -1;
        }
    }

    public void spawnAt(SpawnData spawn) {
        if (spareCount == 0)
            return;
        set(spawn.slot / size, spawn.slot % size, spawn.data);
        Arrays.sort(valueSpared, 0, spareCount);
        int index = Arrays.binarySearch(valueSpared, 0, spareCount--, spawn.slot);
        System.arraycopy(valueSpared, index + 1, valueSpared, index, spareCount - index);  // Delete value has been spawned
        valueSpared[spareCount] = 0;
        if (spawnListener != null)
            spawnListener.spawn(spawn.slot / size, spawn.slot % size, spawn.data);
    }

    public boolean internalMove(MoveDirection direction) {
        boolean success = false;
        // Re-calculate spare count
        spareCount = 0;
        long rollbackPosition = 0;
        LongStore mergePosition = new LongStore(0);
        if (rollbackListener != null)
            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++)
                    if (get(i, j) != 0)
                        rollbackPosition += 1L << (i * size + j);
        for (int line = 0; line < size; line++) {
            int finalLine = line;  // Effective final statement (lambda restricted)
            success |= calculateSingleWithDirection(line, direction, stream -> {
                // Module to handle moving
                // Stream: First->Last = bottom moving->top moving
                LongArrayStream outStream = new LongArrayStream(size);
                IntegerStore nowPosition = new IntegerStore(0);
                IntegerStore lastIndex = new IntegerStore(0);
                IntegerStore moved = new IntegerStore(0);
                LongStore lastNumber = new LongStore(0);
                stream.accept((index, value) -> {
                    long last = lastNumber.get();
                    if (last != 0 && last == value) {
                        // If last value isn't 0 (Condition: Merged or first element)
                        // and the value equals last value, merge them and set last value to 0
                        long result = outStream.set(nowPosition.get() - 1, last + value);
                        score += result;
                        if (result > maxValue)
                            maxValue = result;
                        if (moveListener != null) {
                            moveListener.move(finalLine, lastIndex.get(), nowPosition.get() - 1, direction, value, result);
                            moveListener.move(finalLine, index, nowPosition.get() - 1, direction, value, result);
                        }
                        if (rollbackListener != null)
                            mergePosition.set(
                                    mergePosition.get() + (1L << fromLineToSlot(finalLine, nowPosition.get() - 1, direction)));
                        lastNumber.set(0);
                        moved.set(1);
                    } else if (value != 0) {
                        // If the value is 0, ignore it
                        // Otherwise, add it to the result
                        lastNumber.set(value);
                        outStream.set(nowPosition.get(), value);
                        if (moved.get() == 1 && moveListener != null)
                            moveListener.move(finalLine, index, nowPosition.get(), direction, value, value);
                        if (moved.get() == 0 && stayListener != null)
                            stayListener.stay(finalLine, index, direction);
                        lastIndex.set(index);
                        nowPosition.increase();
                    } else
                        moved.set(1);
                });
                // Update values
                supplyNewDataWithDirection(finalLine, direction, outStream);
                // If the result equals the input, the operation in this line is invalid
                return !outStream.equals(stream);
            });
        }
        if (rollbackListener != null && success)
            rollbackListener.receiveRollbackData(new RollbackEntry(rollbackPosition, mergePosition.get()));
        return success;
    }

    public boolean checkContinue() {
        if (spareCount > 0)
            return true;
        for (int row = 0; row < size; row++)
            for (int column = 0; column < size; column++) {
                long value = get(row, column);
                if (row != size - 1 && get(row + 1, column) == value)
                    return true;
                if (column != size - 1 && get(row, column + 1) == value)
                    return true;
            }
        // When checked the table has neither spare area nor merge chance, the game is over
        return false;
    }

    private void spawnRandomValue() {
        if (spareCount == 0)
            // It won't be invoked
            return;
        int index = spareCount == 1 ? --spareCount : random.nextInt(--spareCount);  // Select random position
        int at = valueSpared[index];
        int value = random.nextFloat() < 0.95f ? 2 : 4;
        if (value > maxValue)
            maxValue = value;
        set(at / size, at % size, value);  // 95% spawns 2, 5% spawns 4
        System.arraycopy(valueSpared, index + 1, valueSpared, index, spareCount - index);  // Delete value has been spawned
        valueSpared[spareCount] = 0;
        if (spawnListener != null)
            spawnListener.spawn(at / size, at % size, value);
    }

    private boolean calculateSingleWithDirection(int line, MoveDirection direction, Function<LongArrayStream, Boolean> calculator) {
        // Get the stream of the line in certain direction
        switch (direction) {
            case UP:
                return calculator.apply(LongArrayStream.of(getColumn(line)));
            case DOWN:
                return calculator.apply(LongArrayStream.of(getColumn(line)).reverse());
            case LEFT:
                return calculator.apply(LongArrayStream.of(getRow(line)));
            case RIGHT:
                return calculator.apply(LongArrayStream.of(getRow(line)).reverse());
            default:
                return false;
        }
    }

    private void supplyNewDataWithDirection(int line, MoveDirection direction, LongArrayStream stream) {
        // Update the line in certain direction
        // Re-calculate the spare area
        switch (direction) {
            case UP:
                fillColumn(line, 0);
                stream.accept((index, value) -> {
                    set(index, line, value);
                    if (value == 0)
                        valueSpared[spareCount++] = index * size + line;
                });
                break;
            case DOWN:
                fillColumn(line, 0);
                stream.accept((index, value) -> {
                    set(size - index - 1, line, value);
                    if (value == 0)
                        valueSpared[spareCount++] = (size - index - 1) * size + line;
                });
                break;
            case LEFT:
                fillRow(line, 0);
                stream.accept((index, value) -> {
                    set(line, index, value);
                    if (value == 0)
                        valueSpared[spareCount++] = line * size + index;
                });
                break;
            case RIGHT:
                fillRow(line, 0);
                stream.accept((index, value) -> {
                    set(line, size - index - 1, value);
                    if (value == 0)
                        valueSpared[spareCount++] = (line + 1) * size - index - 1;
                });
                break;
        }
    }

    public synchronized void rollback(RollbackEntry entry, ReplayEntry replay) {
        long prev = entry.getPreviousPositions();
        long merge = entry.getMergePositions();
        spareCount = 0;
        MoveDirection direction = replay.getDirection();
        maxValue = 0;
        set(replay.getSpawnData().slot / size, replay.getSpawnData().slot % 4, 0);
        for (int line = 0; line < size; line++) {
            int finalLine = line;
            calculateSingleWithDirection(line, direction, stream -> {
                LongArrayStream remade = new LongArrayStream(size);
                IntegerStore storeNowPos = new IntegerStore(0);
                stream.accept((index, value) -> {
                    if (value == 0)
                        return;
                    if ((merge & (1L << (fromLineToSlot(finalLine, index, direction)))) != 0) {
                        while ((prev & (1L << fromLineToSlot(finalLine, storeNowPos.get(), direction))) == 0)
                            storeNowPos.increase();
                        remade.set(storeNowPos.get(), value >> 1);
                        storeNowPos.increase();
                        while ((prev & (1L << fromLineToSlot(finalLine, storeNowPos.get(), direction))) == 0)
                            storeNowPos.increase();
                        remade.set(storeNowPos.get(), value >> 1);
                        storeNowPos.increase();
                        score -= value;
                        maxValue = Math.max(maxValue, value >> 1);
                    } else {
                        while ((prev & (1L << fromLineToSlot(finalLine, storeNowPos.get(), direction))) == 0)
                            storeNowPos.increase();
                        remade.set(storeNowPos.get(), value);
                        storeNowPos.increase();
                        maxValue = Math.max(maxValue, value);
                    }
                });
                supplyNewDataWithDirection(finalLine, direction, remade);
                return true;
            });
        }
        // Decrease 0.5%
        score -= Math.max(0, Math.floorDiv(score, 200));
    }

    public void rollbackToValue(long score) {
        this.score = score;
    }

    public void set(int row, int column, long value) {
        table[row][column] = value;
    }

    public void validate() {
        spareCount = 0;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                long now;
                if ((now = get(i, j)) == 0)
                    valueSpared[spareCount++] = i * size + j;
                maxValue = Math.max(maxValue, now);
            }
    }

    public long get(int row, int column) {
        return table[row][column];
    }

    public long[] getRow(int row) {
        return table[row];
    }

    public long[] getColumn(int column) {
        long[] columnData = new long[size];
        for (int i = 0; i < size; i++)
            columnData[i] = get(i, column);
        return columnData;
    }

    public void fillRow(int row, int value) {
        Arrays.fill(table[row], value);
    }

    public void fillColumn(int column, int value) {
        for (int i = 0; i < size; i++)
            table[i][column] = value;
    }

    public int size() {
        return size;
    }
}
