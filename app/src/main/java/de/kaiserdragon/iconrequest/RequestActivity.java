package de.kaiserdragon.iconrequest;

import android.content.Context;
import android.content.Intent;

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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.interfaces.OnAppSelectedListener;
import de.kaiserdragon.iconrequest.helper.SettingsHelper;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;
import de.kaiserdragon.iconrequest.helper.PrepareRequestData;

public class RequestActivity extends AppCompatActivity implements OnAppSelectedListener {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static boolean updateOnly = false;
    private static int mode;
    private static boolean OnlyNew;
    private static boolean SecondIcon;
    private static boolean Shortcut;
    private static boolean ActionMain;
    private static boolean firstRun;
    private final Context context = this;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean IPackChosen = false;
    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private ZipData zip;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onAppSelected(String packageName) {
        IPackSelect(packageName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        mode = getIntent().getIntExtra("update", 0);
        updateOnly = mode == 0;
        OnlyNew = SettingsHelper.loadDataBool("SettingOnlyNew",this);
        SecondIcon = SettingsHelper.loadDataBool("SettingRow",this);
        Shortcut = SettingsHelper.loadDataBool("Shortcut",this);
        ActionMain = SettingsHelper.loadDataBool("ActionMain",this);
        firstRun = false;

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                if (DEBUG) Log.v(TAG, "onBackPressed");
                finish();
            }
        });

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
            if (Shortcut) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataShortcuts(this,appListAll,false,false),true,false,this);
            }
            else if ((OnlyNew || SecondIcon) || (mode >= 2 && mode <= 5)) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataIpack(this,appListAll),true,false,this);
            } else if (ActionMain) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataActionMain(this,appListAll,false,false),false,false,this); //show all apps
            }else adapter = new AppAdapter(PrepareRequestData.prepareDataIcons(this,appListAll,false,false),false,false,this); //show all apps
            runOnUiThread(() -> {
                if ((!OnlyNew && !SecondIcon||Shortcut )&& (mode < 2 || mode > 5)) {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                }
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,updateOnly,mode,context),zip.getZipData(),result,context));
    }

    public void IPackSelect(String packageName) {
        switcherLoad.showNext();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            try {
                XMLParserHelper.parseXML(packageName, SecondIcon || (mode >= 2 && mode <= 5), appListAll, context);
                if (mode < 2 || mode > 5) {
                    if(ActionMain){
                        adapter = new AppAdapter(PrepareRequestData.prepareDataActionMain(this,appListAll,OnlyNew,SecondIcon),false,SecondIcon||mode==3,this);
                    }
                    else adapter = new AppAdapter(PrepareRequestData.prepareDataIcons(this,appListAll,OnlyNew,SecondIcon),false,SecondIcon||mode==3,this);
                }
                if (!(mode <= 1) && (mode != 2 && mode != 3 || firstRun)) {
                    adapter = new AppAdapter(CommonHelper.compareNew(mode,appListAll),false,mode==4||mode==3,this);
                }
            } catch (Exception e) {
                Log.e(TAG, "IPackSelect: ", e);
            }

            runOnUiThread(() -> {
                // Show IPack chooser a second Time
                if (mode != 2 && mode != 3 || firstRun) {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                    IPackChosen = true;
                    invalidateOptionsMenu();
                }
                firstRun = true;
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {


        if ((OnlyNew && !Shortcut) && !IPackChosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_sharetext);
            MenuItem copy = menu.findItem(R.id.action_copy);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();

            if (updateOnly) {
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
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ShareHelper.actionSend(ShareHelper.actionSave(adapter,true,mode,context),zip.getZipData(),context);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            ShareHelper.actionSendSave(activityResultLauncher);
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            ShareHelper.actionSendText(ShareHelper.actionSave(adapter,false,mode,context),context);
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