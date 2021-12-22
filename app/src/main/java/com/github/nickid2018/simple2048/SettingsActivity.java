package com.github.nickid2018.simple2048;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.nickid2018.simple2048.datasave.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    public static final int RESULT_BACKGROUND_LOAD = 0;
    public static final int RESULT_CROP = 1;
    public static final int RESULT_COLOR = 2;
    private final List<Map<String, Object>> data = new ArrayList<>();
    private boolean backgroundChanged = false;
    private boolean colorChanged = false;
    private boolean alphaChanged = false;
    private SimpleAdapter adapter;
    private boolean canUse = true;
    private File tmpFile;
    private Uri tempBackground;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_settings);
        ListView listView = findViewById(R.id.settingsList);
        String[] settings = getResources().getStringArray(R.array.settings);
        Map<String, Object> animationDuring = new HashMap<>();
        animationDuring.put("itemName", settings[0]);
        animationDuring.put("itemValue", Settings.animationDuring + "ms");
        data.add(animationDuring);
        Map<String, Object> background = new HashMap<>();
        background.put("itemName", settings[1]);
        background.put("itemValue", Settings.backgroundSet ? getString(R.string.backgroundSet) : getString(R.string.noSet));
        data.add(background);
        Map<String, Object> foregroundAlpha = new HashMap<>();
        foregroundAlpha.put("itemName", settings[2]);
        foregroundAlpha.put("itemValue", Settings.alphaForeground);
        data.add(foregroundAlpha);
        Map<String, Object> replayDuring = new HashMap<>();
        replayDuring.put("itemName", settings[3]);
        replayDuring.put("itemValue", Settings.replayDuration + "ms");
        data.add(replayDuring);
        Map<String, Object> antiWrongMove = new HashMap<>();
        antiWrongMove.put("itemName", settings[4]);
        antiWrongMove.put("itemValue", Settings.antiMisTouch ? getString(R.string.on) : getString(R.string.off));
        data.add(antiWrongMove);
        Map<String, Object> numberColor = new HashMap<>();
        numberColor.put("itemName", settings[5]);
        numberColor.put("itemValue", "");
        data.add(numberColor);
        adapter = new SimpleAdapter(
                this, data, R.layout.item_setting, new String[]{"itemName", "itemValue"}, new int[]{R.id.itemName, R.id.itemValue});
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this::click);
    }

    private void click(AdapterView<?> view, View now, int pos, long id) {
        switch (pos) {
            case 0:
                setAnimationDuration();
                break;
            case 1:
                setBackground();
                break;
            case 2:
                setForegroundAlpha();
                break;
            case 3:
                setReplayDuration();
                break;
            case 4:
                switchAntiMisTouch();
                break;
            case 5:
                setColors();
                break;
            default:
        }
    }

    private void setAnimationDuration() {
        NumberPicker newDuration = new NumberPicker(this);
        newDuration.setMaxValue(5000);
        newDuration.setMinValue(10);
        newDuration.setWrapSelectorWheel(false);
        newDuration.setValue(Settings.animationDuring);
        new AlertDialog.Builder(this).setTitle(R.string.setAnimationDuration).setView(newDuration)
                .setPositiveButton(R.string.ok, (dialogI, view) -> {
                    Settings.animationDuring = newDuration.getValue();
                    data.get(0).put("itemValue", Settings.animationDuring + "ms");
                    adapter.notifyDataSetChanged();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void setBackground() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, RESULT_BACKGROUND_LOAD);
    }

    private void setForegroundAlpha() {
        EditText text = new EditText(this);
        text.setSingleLine();
        new AlertDialog.Builder(this).setTitle(R.string.setForegroundAlpha).setView(text)
                .setPositiveButton(R.string.yes, ((dialog, which) -> {
                    try {
                        float alpha = Settings.alphaForeground = Float.parseFloat(text.getText().toString());
                        if (alpha <= 0 || alpha > 1)
                            throw new NumberFormatException();
                        data.get(2).put("itemValue", alpha);
                        adapter.notifyDataSetChanged();
                        alphaChanged = true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                String.format(getString(R.string.illegalNumber), text.getText()), Toast.LENGTH_SHORT).show();
                    }
                }))
                .setNegativeButton(R.string.no, null)
                .setCancelable(false)
                .show();
    }

    private void setReplayDuration() {
        NumberPicker newDuration = new NumberPicker(this);
        newDuration.setMaxValue(5000);
        newDuration.setMinValue(200);
        newDuration.setWrapSelectorWheel(false);
        newDuration.setValue(Settings.replayDuration);
        new AlertDialog.Builder(this).setTitle(R.string.setReplayDuration).setView(newDuration)
                .setPositiveButton(R.string.ok, (dialogI, view) -> {
                    Settings.replayDuration = newDuration.getValue();
                    data.get(3).put("itemValue", Settings.replayDuration + "ms");
                    adapter.notifyDataSetChanged();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void switchAntiMisTouch() {
        Settings.antiMisTouch = !Settings.antiMisTouch;
        data.get(4).put("itemValue", Settings.antiMisTouch ? getString(R.string.on) : getString(R.string.off));
        adapter.notifyDataSetChanged();
    }

    private void setColors() {
        Intent intent = new Intent(this, ChunkColorActivity.class);
        startActivityForResult(intent, RESULT_COLOR);
    }

    private Uri generateTempBackground() throws IOException {
        tmpFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "tmpBackground.jpg");
        try {
            tmpFile.createNewFile();
        } catch (IOException e) {
            tmpFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmpBackground.jpg");
            tmpFile.createNewFile();
        }
        return tempBackground = Uri.fromFile(tmpFile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                canUse = false;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_BACKGROUND_LOAD) {
            if (resultCode == RESULT_OK && data != null) {
                new AlertDialog.Builder(this).setPositiveButton(R.string.useChop, (dialogI, view) -> {
                            int permission_write = ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            int permission_read = ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE);
                            if (permission_write != PackageManager.PERMISSION_GRANTED
                                    || permission_read != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                                if (!canUse) {
                                    Toast.makeText(this, R.string.noPermission, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            Intent intent = new Intent("com.android.camera.action.CROP");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.setDataAndType(data.getData(), "image/*");
                            intent.putExtra("crop", "true");
                            intent.putExtra("aspectX", findViewById(R.id.settingPanel).getWidth());
                            intent.putExtra("aspectY", findViewById(R.id.settingPanel).getHeight());
                            intent.putExtra("scale", true);
                            intent.putExtra("scaleUpIfNeeded", true);
                            intent.putExtra("noFaceDetection", true);
                            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                            intent.putExtra("return-data", false);
                            try {
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, generateTempBackground());
                                startActivityForResult(intent, RESULT_CROP);
                            } catch (IOException e) {
                                Log.e("Settings", "Set background", e);
                                Toast.makeText(this, R.string.errorWhenSet, Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton(R.string.useSource, (dialogI, view) -> {
                            try {
                                File background = new File(getDataDir(), "background.jpeg");
                                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(background));
                                Settings.backgroundSet = true;
                                this.data.get(1).put("itemValue", getResources().getString(R.string.backgroundSet));
                                backgroundChanged = true;
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                Log.e("Settings", "Set background", e);
                                Toast.makeText(this, R.string.errorWhenSet, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setCancelable(false)
                        .setTitle(R.string.setBackgroundFunction)
                        .show();

            } else {
                this.data.get(1).put("itemValue", getResources().getString(R.string.noSet));
                backgroundChanged = true;
                Settings.backgroundSet = false;
            }
            adapter.notifyDataSetChanged();
        } else if (requestCode == RESULT_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(tempBackground));
                    File background = new File(getDataDir(), "background.jpeg");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(background));
                    tmpFile.delete();
                    Settings.backgroundSet = true;
                    this.data.get(1).put("itemValue", getResources().getString(R.string.backgroundSet));
                    backgroundChanged = true;
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e("Settings", "Set background", e);
                    Toast.makeText(this, R.string.errorWhenSet, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == RESULT_COLOR) {
            if (data != null)
                colorChanged |= data.getBooleanExtra("colorChanged", false);
        }
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("background", backgroundChanged);
        intent.putExtra("colorChanged", colorChanged);
        intent.putExtra("alphaChanged", alphaChanged);
        setResult(SimpleGameActivity.RETURN_SETTINGS, intent);
        Settings.save();
        super.finish();
    }
}