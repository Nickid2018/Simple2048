package com.github.nickid2018.simple2048;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ChooseModeActivity extends Activity {

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_mode);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (getIntent().getBooleanExtra("mode", true))
            initializeAsSingle();
        else
            initializeAsMulti();
        ((ImageView) findViewById(R.id.chooseBack)).setImageDrawable(EntranceActivity.getLocaleDrawable(this, "icon_back"));
        findViewById(R.id.chooseBack).setOnClickListener(v -> finish());
    }

    private void initializeAsSingle() {
        ((ImageView) findViewById(R.id.chooseModeTitle)).setImageDrawable(
                EntranceActivity.getLocaleDrawable(this, "mode_single_title"));
        ImageView classic = findViewById(R.id.chooseMode1);
        classic.setImageDrawable(EntranceActivity.getLocaleDrawable(this, "mode_single_classic"));
        classic.setOnClickListener(view -> {
            Intent intent = new Intent(this, SimpleGameActivity.class);
            startActivity(intent);
        });
        ImageView custom = findViewById(R.id.chooseMode2);
        custom.setImageDrawable(EntranceActivity.getLocaleDrawable(this, "mode_single_custom"));
        custom.setOnClickListener(view -> {
            Intent intent = new Intent(this, CustomizeTableActivity.class);
            startActivity(intent);
        });
    }

    private void initializeAsMulti() {
        ((ImageView) findViewById(R.id.chooseModeTitle)).setImageDrawable(
                EntranceActivity.getLocaleDrawable(this, "mode_multi_title"));
        ImageView create = findViewById(R.id.chooseMode1);
        create.setImageDrawable(EntranceActivity.getLocaleDrawable(this, "mode_multi_create"));
        create.setOnClickListener(view -> {

        });
        ImageView join = findViewById(R.id.chooseMode2);
        join.setImageDrawable(EntranceActivity.getLocaleDrawable(this, "mode_multi_join"));
        join.setOnClickListener(view -> {

        });
    }

    public void settingClick(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void replayClick(View view) {
        Intent intent = new Intent(this, ReplaySelectActivity.class);
        startActivity(intent);
    }
}