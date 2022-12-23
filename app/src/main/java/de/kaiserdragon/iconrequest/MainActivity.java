package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private final int updateExisting = 0;
    private final int requestNew = 1;
    private final int compareIconPack_diff = 2;
    private final int compareIconPack_sim = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(loadData());

        setContentView(R.layout.activity_main);

        Button startNew = findViewById(R.id.start_new);
        startNew.setOnClickListener(view -> start(requestNew));

        Button startUpdate = findViewById(R.id.start_update);
        startUpdate.setOnClickListener(view -> start(updateExisting));

        Button CompareIconPacks = findViewById(R.id.CompareIconPacks);
        CompareIconPacks.setOnClickListener(view -> setDialog());

        Button check = findViewById(R.id.CheckIconPack);
        check.setOnClickListener(view -> start(4));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void setDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.CompareIconPacks));
        builder.setMessage("Select how you want to compare the icon packs");

        // Set the layout for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog, null);
        builder.setView(view);

        // Set custom buttons for the dialog
        Button button1 = view.findViewById(R.id.button1);
        button1.setText("Show differences");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(compareIconPack_diff);
            }
        });
        Button button2 = view.findViewById(R.id.button2);
        button2.setText("Show similarities");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(compareIconPack_sim);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void start(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        //if (DEBUG) Log.v(TAG, String.valueOf(getAvailableIconPacks(true)));
        //populateView(appListFilter);

        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".RequestActivity"));
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".SettingActivity"));
            startActivity(intent);
        }
        return true;
    }

    public int loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("DarkModeState", -1);
    }


}