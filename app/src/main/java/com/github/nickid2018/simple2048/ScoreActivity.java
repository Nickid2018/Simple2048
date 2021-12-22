package com.github.nickid2018.simple2048;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.nickid2018.simple2048.datasave.ScoreDataStorage;
import com.github.nickid2018.simple2048.datasave.ScoreEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_score);
        ListView list = findViewById(R.id.scoreList);
        List<ScoreEntry> entries = ScoreDataStorage.getEntries();
        List<String> str = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ScoreEntry score = entries.get(i);
            str.add((i + 1) + ": " + getResources().getString(R.string.scoreData) + ": " + score.getScore() + " "
                    + getResources().getString(R.string.scoreTime) + ": "
                    + String.format(Locale.getDefault(), "%tF %tT", score.getDate(), score.getDate()));
        }
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, str));
    }
}