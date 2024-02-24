package de.kaiserdragon.iconrequest;


import static de.kaiserdragon.iconrequest.helper.ActivityHelper.getAllActivities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.DrawableHelper;
import de.kaiserdragon.iconrequest.helper.SettingsHelper;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;


public class AllPackagesActivity extends AppCompatActivity {
    private static final String TAG = "AllPackagesActivity";
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static boolean updateOnly = false;
    private static int mode;

    private final Context context = this;

    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        mode = getIntent().getIntExtra("update", 0);
        updateOnly = mode == 0;


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                if (DEBUG) Log.v(TAG, "onBackPressed");
                finish();
            }
        });

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
            adapter = new AppAdapter(prepareData(false),false,false,null); //show all apps
            runOnUiThread(() -> {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        executor.shutdown();
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,updateOnly,mode,context),ShareHelper.zipData,result,context));
    }

    public boolean onCreateOptionsMenu(Menu menu) {



            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_sharetext);
            MenuItem copy = menu.findItem(R.id.action_copy);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();



            if (updateOnly || (mode >= 2 && mode <= 5)) {
                save.setVisible(false);
                share.setVisible(false);
                share_text.setVisible(true);
                copy.setVisible(true);
            } else {
                share_text.setVisible(false);
                copy.setVisible(false);
                save.setVisible(true);
                share.setVisible(true);
            }
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
            ShareHelper.actionSend(ShareHelper.actionSave(adapter,updateOnly,mode,context),ShareHelper.zipData,context);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            ShareHelper.actionSendSave(activityResultLauncher);
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            ShareHelper.actionSendText(ShareHelper.actionSave(adapter,updateOnly,mode,context),context);
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            ShareHelper.actionCopy(ShareHelper.actionSave(adapter,updateOnly,mode,context),context);
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




    private ArrayList<AppInfo> prepareData(boolean iPack) {
        PackageManager pm = getPackageManager();
        ArrayList<AppInfo> arrayList = new ArrayList<>();


        List<ActivityInfo> list = getAllActivities(context);


        for (ActivityInfo activityInfo : list) {
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, activityInfo);
            AppInfo appInfo = new AppInfo(icon1, null, activityInfo.loadLabel(pm).toString(), activityInfo.packageName, activityInfo.name, false);
            arrayList.add(appInfo);

        }

        return CommonHelper.sort(arrayList);
    }

}