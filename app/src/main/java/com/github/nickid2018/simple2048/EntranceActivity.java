package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import com.github.nickid2018.simple2048.datasave.Settings;

public class EntranceActivity extends Activity {

    public static Drawable getLocaleDrawable(Activity activity, String name) {
        try {
            return activity.getDrawable(
                    R.drawable.class.getDeclaredField(name + "_" + activity.getString(R.string.locale)).getInt(null));
        } catch (Exception e) {
            try {
                return activity.getDrawable(
                        R.drawable.class.getDeclaredField(name + "_en_us").getInt(null));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Settings.initialize(this);
        ((ImageView) findViewById(R.id.entrance_singleplayer)).setImageDrawable(getLocaleDrawable(this, "mode_singleplayer"));
        ((ImageView) findViewById(R.id.entrance_multiplayer)).setImageDrawable(getLocaleDrawable(this, "mode_multiplayer"));
    }

    public void settingClick(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void replayClick(View view) {
        Intent intent = new Intent(this, ReplaySelectActivity.class);
        startActivity(intent);
    }

    public void startSingle(View view) {
        Intent intent = new Intent(this, ChooseModeActivity.class);
        intent.putExtra("mode", true);
        startActivity(intent);
    }

    public void startMulti(View view) {
        Intent intent = new Intent(this, ChooseModeActivity.class);
        intent.putExtra("mode", false);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this).setTitle(R.string.confirmExit)
                    .setNegativeButton(R.string.yes, (dialogInterface, i) -> finish())
                    .setPositiveButton(R.string.no, null)
                    .show();
        }
        return super.onKeyDown(keyCode, event);
    }
}