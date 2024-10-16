package de.kaiserdragon.iconrequest;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

import de.kaiserdragon.iconrequest.helper.SettingsHelper;

public class SettingActivity extends AppCompatActivity {
    Button setDark;
    Button setLight;
    Button setDefault;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        setDark = findViewById(R.id.ButtonDarkMode);
        setLight = findViewById(R.id.ButtonLightMode);
        setDefault = findViewById(R.id.ButtonSystem);
        painThemeButton();
        TextView PolicyView = findViewById(R.id.PrivacyPolicy);
        PolicyView.setMovementMethod(LinkMovementMethod.getInstance());


        ((CheckBox) findViewById(R.id.checkBoxRows)).setChecked(SettingsHelper.loadDataBool("SettingRow", this));
        ((CheckBox) findViewById(R.id.checkBoxOnly)).setChecked(SettingsHelper.loadDataBool("SettingOnlyNew", this));
        ((CheckBox) findViewById(R.id.checkShortcut)).setChecked(SettingsHelper.loadDataBool("Shortcut", this));
        ((CheckBox) findViewById(R.id.checkActionMain)).setChecked(SettingsHelper.loadDataBool("ActionMain", this));


        //Button setDark = findViewById(R.id.radioDark);
        setDark.setOnClickListener(view -> start(view, 2));

        //Button setLight = findViewById(R.id.radioLight);
        setLight.setOnClickListener(view -> start(view, 1));

        //Button setDefault = findViewById(R.id.radioDefault);
        setDefault.setOnClickListener(view -> start(view, -1));

        CheckBox SecondRow = findViewById(R.id.checkBoxRows);
        SecondRow.setOnClickListener(view -> start(view, 0));

        CheckBox OnlyNew = findViewById(R.id.checkBoxOnly);
        OnlyNew.setOnClickListener(view -> start(view, 0));

        CheckBox Shortcut = findViewById(R.id.checkShortcut);
        Shortcut.setOnClickListener(view -> start(view, 0));
        CheckBox ActionMain = findViewById(R.id.checkActionMain);
        ActionMain.setOnClickListener(view -> start(view, 0));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);
    }

    public void start(View view, int update) {
        if (update != 0) {
            SettingsHelper.saveData("DarkModeState", update, this);
            Log.d("Update", String.valueOf(update));
            AppCompatDelegate.setDefaultNightMode(update);
            painThemeButton();
        } else {
            if (view == findViewById(R.id.checkBoxRows)) {
                SettingsHelper.saveDataBool("SettingRow", ((CheckBox) view).isChecked(), this);
            } else if (view == findViewById(R.id.checkBoxOnly)) {
                SettingsHelper.saveDataBool("SettingOnlyNew", ((CheckBox) view).isChecked(), this);
            } else if (view == findViewById(R.id.checkShortcut)) {
                SettingsHelper.saveDataBool("Shortcut", ((CheckBox) view).isChecked(), this);
            } else if (view == findViewById(R.id.checkActionMain)) {
                SettingsHelper.saveDataBool("ActionMain", ((CheckBox) view).isChecked(), this);
            }
        }

    }
    public void painThemeButton(){
        resetButtonColors();
        switch (SettingsHelper.loadData("DarkModeState", this)) {
            case -1:
                setDefault.setBackgroundColor(getResources().getColor(R.color.secondary, null));
                break;
            case 1:
                setLight.setBackgroundColor(getResources().getColor(R.color.secondary,null));
                break;
            case 2:
                setDark.setBackgroundColor(getResources().getColor(R.color.secondary,null));
                break;
        }

    }
    // Method to reset all button colors to default
    private void resetButtonColors() {
        setDefault.setBackgroundColor(getResources().getColor(R.color.primary,null));
        setDark.setBackgroundColor(getResources().getColor(R.color.primary,null));
        setLight.setBackgroundColor(getResources().getColor(R.color.primary,null));
    }
}


