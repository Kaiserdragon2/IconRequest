package de.kaiserdragon.iconrequest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

        switch(loadData("DarkModeState")) {
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

        ((CheckBox)findViewById(R.id.checkBoxRows)).setChecked(loadDataBool("SettingRow"));
        ((CheckBox)findViewById(R.id.checkBoxOnly)).setChecked(loadDataBool("SettingOnlyNew"));


        Button setDark = findViewById(R.id.radioDark);
        setDark.setOnClickListener(view -> start(view,2));

        Button setLight = findViewById(R.id.radioLight);
        setLight.setOnClickListener(view -> start(view,1));

        Button setDefault = findViewById(R.id.radioDefault);
        setDefault.setOnClickListener(view -> start(view,-1));

        CheckBox SecondRow = findViewById(R.id.checkBoxRows);
        SecondRow.setOnClickListener(view -> start(view,0));

        CheckBox OnlyNew = findViewById(R.id.checkBoxOnly);
        OnlyNew.setOnClickListener(view -> start(view,0));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    public void start(View view ,int update) {
        if (update != 0){
        saveData("DarkModeState",update);
        AppCompatDelegate.setDefaultNightMode(update);
        } else{
            if (view ==(CheckBox) findViewById(R.id.checkBoxRows)){
                if (((CheckBox) view).isChecked()) {
                    saveDataBool("SettingRow", true);
                }else saveDataBool("SettingRow", false);
            }else  if (view ==(CheckBox) findViewById(R.id.checkBoxOnly)){
                if (((CheckBox) view).isChecked()) {
                    saveDataBool("SettingOnlyNew", true);
                }else saveDataBool("SettingOnlyNew", false);
            }
        }

    }

    public void saveData(String setting, int data) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(setting, data);
        editor.apply();
    }
    public void saveDataBool(String setting, boolean data) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(setting, data);
        editor.apply();
    }
    public int loadData(String setting) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt(setting, -1);
    }
    public boolean loadDataBool(String setting) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean(setting, false);
    }
}


