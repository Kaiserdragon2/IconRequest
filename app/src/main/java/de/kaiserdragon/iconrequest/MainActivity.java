package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;




public class MainActivity extends AppCompatActivity {
 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startNew = findViewById(R.id.start_new);
        startNew.setOnClickListener(view -> start(false));

        Button startUpdate = findViewById(R.id.start_update);
        startUpdate.setOnClickListener(view -> start(true));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void start(boolean update) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("update", update);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".RequestActivity"));
        startActivity(intent);
    }
}
