package de.kaiserdragon.iconrequest;

import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.PrepareRequestData;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;
import de.kaiserdragon.iconrequest.interfaces.OnAppSelectedListener;

public class CompareActivity extends AppCompatActivity implements OnAppSelectedListener {

    private static final String TAG = "CompareActivity";
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static int mode;
    private final Context context = this;
    private ZipData zip;
    private String iconPackPackageName;

    @Override
    public void onAppSelected(String packageName) {
        //IPackSelect(packageName);
        Log.i(TAG, "onAppSelected: " + packageName);
        if(adapter.getSelectedItemCount() == 2) {
            iconPackPackageName = packageName;
            switcherLoad.showNext();
            startCompareIconPacksDifference();
        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                if (DEBUG) Log.v(TAG, "onBackPressed");
                finish();
            }
        });
        mode = getIntent().getIntExtra("update", 0);
        appListAll.clear();
        zip = (ZipData) getApplicationContext();

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            Looper.prepare();
            adapter = new AppAdapter(PrepareRequestData.prepareDataIpack(this,appListAll),true,false,this);
            runOnUiThread(() -> {
                findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,false,mode,context),zip.getZipData(),result,context));

    }

    private void startCompareIconPacksDifference(){
        ArrayList<AppInfo> arrayList = adapter.getAllSelected();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            Looper.prepare();
            try {
                for (AppInfo appInfo : arrayList) {
                    XMLParserHelper.parseXML(appInfo.packageName,appInfo.label, true, appListAll, context);
                }
                adapter = new AppAdapter(CommonHelper.compareNew(mode,appListAll),false,mode==4||mode==3,this);
                adapter.showIPack(iconPackPackageName);

            } catch (Exception e) {
                Log.e(TAG, "startCompareIconPacksDifference: ", e);
            }
            runOnUiThread(() -> {
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_sharetext);
            MenuItem copy = menu.findItem(R.id.action_copy);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            save.setVisible(true);
            share.setVisible(true);
            share_text.setVisible(true);
            copy.setVisible(true);

            // Set up search functionality
            assert searchView != null;

            searchView.setMaxWidth(700);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // Handle search query submission
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Handle search query text change
                    adapter.filter(newText);
                    return true;
                }
            });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ShareHelper.actionSend(ShareHelper.actionSave(adapter,false,mode,context),zip.getZipData(),context);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            ShareHelper.actionSendSave(activityResultLauncher);
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            ShareHelper.actionSendText(ShareHelper.actionSave(adapter,true,mode,context),context);
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            ShareHelper.actionCopy(ShareHelper.actionSave(adapter,true,mode,context),context);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.selectall) {
            adapter.setAllSelected(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }

}
