package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;

    private static ArrayList<iPackInfo> appListFilter = new ArrayList<>();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(loadData());

        setContentView(R.layout.activity_main);

        Button startNew = findViewById(R.id.start_new);
        startNew.setOnClickListener(view -> start(false));

        Button startUpdate = findViewById(R.id.start_update);
        startUpdate.setOnClickListener(view -> start(true));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void start(boolean update) {
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