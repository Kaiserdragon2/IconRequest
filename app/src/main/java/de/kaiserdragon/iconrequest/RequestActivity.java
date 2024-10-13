package de.kaiserdragon.iconrequest;

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

import de.kaiserdragon.iconrequest.helper.PrepareRequestData;
import de.kaiserdragon.iconrequest.helper.SettingsHelper;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;
import de.kaiserdragon.iconrequest.interfaces.OnAppSelectedListener;

public class RequestActivity extends AppCompatActivity implements OnAppSelectedListener {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static boolean updateOnly = false;
    private static boolean OnlyNew;
    private static boolean SecondIcon;
    private static boolean Shortcut;
    private static boolean ActionMain;
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
    public void onAppSelected(String packageName, String label) {
        IPackSelect(packageName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        int mode = getIntent().getIntExtra("update", 0);
        updateOnly = mode == 0;
        OnlyNew = SettingsHelper.loadDataBool("SettingOnlyNew", this);
        SecondIcon = SettingsHelper.loadDataBool("SettingRow", this);
        Shortcut = SettingsHelper.loadDataBool("Shortcut", this);
        ActionMain = SettingsHelper.loadDataBool("ActionMain", this);

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
            Looper.prepare();
            if (Shortcut) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataShortcuts(this, appListAll, false, false), false, false, this);
            } else if (OnlyNew || SecondIcon) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataIPack(this, appListAll), true, false, this);
            } else if (ActionMain) {
                adapter = new AppAdapter(PrepareRequestData.prepareDataActionMain(this, appListAll, false, false), false, false, this); //show all apps
            } else
                adapter = new AppAdapter(PrepareRequestData.prepareDataIcons(this, appListAll, false, false), false, false, this); //show all apps
            runOnUiThread(() -> {
                if (!OnlyNew && !SecondIcon || Shortcut) {
                    findViewById(R.id.text_iPack_chooser).setVisibility(View.GONE);
                }
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter, updateOnly, context), zip.getZipData(), result, context));
    }

    public void IPackSelect(String packageName) {
        switcherLoad.showNext();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            Looper.prepare();
            try {
                XMLParserHelper.parseXML(packageName, SecondIcon, appListAll, context);
                if (ActionMain) {
                    adapter = new AppAdapter(PrepareRequestData.prepareDataActionMain(this, appListAll, OnlyNew, SecondIcon), false, SecondIcon, this);
                } else {
                    Log.v(TAG, "Get System Icons");
                    adapter = new AppAdapter(PrepareRequestData.prepareDataIcons(this, appListAll, OnlyNew, SecondIcon), false, SecondIcon, this);
                }
            } catch (Exception e) {
                Log.e(TAG, "IPackSelect: ", e);
            }

            runOnUiThread(() -> {
                // Show IPack chooser a second Time
                findViewById(R.id.text_iPack_chooser).setVisibility(View.GONE);
                IPackChosen = true;
                invalidateOptionsMenu();
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ((OnlyNew || SecondIcon) && !Shortcut && !IPackChosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_shareText);
            MenuItem copy = menu.findItem(R.id.action_copy);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            MenuItem ShowIconPacks = menu.findItem(R.id.ShowIconPack);

            ShowIconPacks.setVisible(false);
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
            ShareHelper.actionSend(ShareHelper.actionSave(adapter, false, context), zip.getZipData(), context);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            ShareHelper.actionSendSave(activityResultLauncher);
            return true;
        } else if (item.getItemId() == R.id.action_shareText) {
            ShareHelper.actionSendText(ShareHelper.actionSave(adapter, true, context), context);
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            ShareHelper.actionCopy(ShareHelper.actionSave(adapter, true, context), context);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.selectAll) {
            adapter.setAllSelected(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }

}