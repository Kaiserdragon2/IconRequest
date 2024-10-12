package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import de.kaiserdragon.iconrequest.helper.SettingsHelper;


public class MainActivity extends AppCompatActivity {

    private final int updateExisting = 0;
    private final int requestNew = 1;
    private final int compareIconPack_diff = 2;
    private final int compareIconPack_sim = 3;
    private final int check_duplicate = 4;
    private final int check_missing_icon = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.loadData("DarkModeState", this));

        setContentView(R.layout.activity_main);

        Button startNew = findViewById(R.id.ButtonStart_new);
        startNew.setOnClickListener(view -> startRequestActivity(requestNew));

        Button startUpdate = findViewById(R.id.ButtonStart_update);
        startUpdate.setOnClickListener(view -> startRequestActivity(updateExisting));

        Button Similarities = findViewById(R.id.ButtonSimilarities);
        Similarities.setOnClickListener(view -> startCompareActivity(compareIconPack_sim));

        Button Differences = findViewById(R.id.ButtonDifference);
        Differences.setOnClickListener(view -> startCompareActivity(compareIconPack_diff));

        Button missingIcon = findViewById(R.id.ButtonMissingIcon);
        missingIcon.setOnClickListener(view -> startChecksActivity(check_missing_icon));

        Button duplicate = findViewById(R.id.ButtonDuplicates);
        duplicate.setOnClickListener(view -> startChecksActivity(check_duplicate));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void startRequestActivity(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".RequestActivity"));
        startActivity(intent);
    }

    public void startCompareActivity(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".CompareActivity"));
        startActivity(intent);
    }

    public void startChecksActivity(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".ChecksActivity"));
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
}