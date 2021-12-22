package com.github.nickid2018.simple2048;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class CustomizeTableActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_table);
        ((ImageView) findViewById(R.id.customTitle)).setImageDrawable(EntranceActivity.getLocaleDrawable(this, "custom_title"));
        ((ImageView) findViewById(R.id.customStart)).setImageDrawable(EntranceActivity.getLocaleDrawable(this, "custom_start"));
    }
}