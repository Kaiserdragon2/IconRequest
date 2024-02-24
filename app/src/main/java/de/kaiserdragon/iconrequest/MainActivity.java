package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.loadData("DarkModeState",this));

        setContentView(R.layout.activity_main);

        Button startNew = findViewById(R.id.start_new);
        startNew.setOnClickListener(view -> start(requestNew));

        Button startUpdate = findViewById(R.id.start_update);
        startUpdate.setOnClickListener(view -> start(updateExisting));

        Button CompareIconPacks = findViewById(R.id.CompareIconPacks);
        CompareIconPacks.setOnClickListener(view -> setDialog(getString(R.string.CompareIconPacks), getString(R.string.MessageDialogCompare), getString(R.string.difference), getString(R.string.similarities), compareIconPack_diff, compareIconPack_sim));

        Button check = findViewById(R.id.CheckIconPack);
        check.setOnClickListener(view -> setDialog(getString(R.string.checkButton), getString(R.string.MessageDialogCheck), getString(R.string.duplicate), getString(R.string.missingIcon), check_duplicate, check_missing_icon));

        Button allActivity = findViewById(R.id.allActivities);
        allActivity.setOnClickListener(view -> startAll(requestNew));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void setDialog(String title, String message, String button1text, String button2text, int opt1, int opt2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view_title = getLayoutInflater().inflate(R.layout.dialog_title, null);
        builder.setCustomTitle(view_title);
        TextView text_title = view_title.findViewById(R.id.title_text);
        text_title.setText(title);
        builder.setMessage(message);

        // Set the layout for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog, null);
        builder.setView(view);

        // Set custom buttons for the dialog
        Button button1 = view.findViewById(R.id.button1);
        button1.setText(button1text);
        button1.setOnClickListener(v -> start(opt1));
        Button button2 = view.findViewById(R.id.button2);
        button2.setText(button2text);
        button2.setOnClickListener(v -> start(opt2));
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void start(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".RequestActivity"));
        startActivity(intent);
    }
    public void startAll(int update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".AllPackagesActivity"));
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