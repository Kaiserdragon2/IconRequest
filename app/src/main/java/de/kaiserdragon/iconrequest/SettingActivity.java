package de.kaiserdragon.iconrequest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        switch(loadData()) {
            case -1:
                ((RadioGroup)findViewById(R.id.radioGroup)).check(R.id.radioDefault);
                break;
            case 1:
                ((RadioGroup)findViewById(R.id.radioGroup)).check(R.id.radioLight);
                break;
            case 2:
                ((RadioGroup)findViewById(R.id.radioGroup)).check(R.id.radioDark);
                break;
        }

        Button setDark = findViewById(R.id.radioDark);
        setDark.setOnClickListener(view -> start(2));

        Button setLight = findViewById(R.id.radioLight);
        setLight.setOnClickListener(view -> start(1));

        Button setDefault = findViewById(R.id.radioDefault);
        setDefault.setOnClickListener(view -> start(-1));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    public void start(int update) {
        saveData(update);
        AppCompatDelegate.setDefaultNightMode(update);
    }

    public void saveData(int data) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("DarkModeState", data);
        editor.apply();
    }
    public int loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("DarkModeState", 0);
    }
}


