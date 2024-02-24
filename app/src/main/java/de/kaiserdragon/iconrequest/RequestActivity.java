package de.kaiserdragon.iconrequest;


import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;

import android.content.Context;
import android.content.Intent;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.DrawableHelper;
import de.kaiserdragon.iconrequest.helper.SettingsHelper;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;




public class RequestActivity extends AppCompatActivity {
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
    public static byte[] zipData = null;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean IPackChosen = false;
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
            if (((OnlyNew || SecondIcon)&& !Shortcut) || (mode >= 2 && mode <= 5)) {
                adapter = new AppAdapter(prepareData(true),true,false,this);
            } else adapter = new AppAdapter(prepareData(false),false,false,this); //show all apps
            runOnUiThread(() -> {
                if ((!OnlyNew && !SecondIcon||Shortcut )&& (mode < 2 || mode > 5))
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,updateOnly,mode,context),zipData,result,context));
    }

    public void IPackSelect(String packageName) {
        switcherLoad.showNext();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            try {
                XMLParserHelper.parseXML(packageName, SecondIcon || (mode >= 2 && mode <= 5), appListAll, context);
                if (mode < 2 || mode > 5) {
                    adapter = new AppAdapter(prepareData(false),false,SecondIcon||mode==3,this);
                }
                if (!(mode <= 1) && (mode != 2 && mode != 3 || firstRun)) {
                    adapter = new AppAdapter(CommonHelper.compareNew(mode,appListAll),false,mode==4||mode==3,this);
                }
            } catch (Exception e) {
                e.printStackTrace();
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


        if (((OnlyNew && !Shortcut)|| (mode >= 2 && mode <= 5)) && !IPackChosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
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
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ShareHelper.actionSend(ShareHelper.actionSave(adapter,updateOnly,mode,context),zipData,context);
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
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent;

        if (iPack) {
            intent = new Intent("org.adw.launcher.THEMES", null);
        } else if (Shortcut) {
            intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
            intent.addCategory("android.intent.category.DEFAULT");
        } else if (ActionMain) {
            intent = new Intent("android.intent.action.MAIN", null);
        } else {
            intent = new Intent("android.intent.action.MAIN", null);
            intent.addCategory("android.intent.category.LAUNCHER");
        }

        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

        if (list.size() < 1 && iPack){
            OnlyNew =false;
            SecondIcon=false;
            iPack = false;
            if (Shortcut && mode <= 1) {
                intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
                intent.addCategory("android.intent.category.DEFAULT");
            }
            else if ( mode <= 1 && ActionMain){
                    intent = new Intent("android.intent.action.MAIN", null);
            }
            else if ( mode <= 1 ){
                intent = new Intent("android.intent.action.MAIN", null);
                intent.addCategory("android.intent.category.LAUNCHER");
            }
            list = pm.queryIntentActivities(intent, 0);

        }

        if (DEBUG) Log.v(TAG, "list size: " + list.size());

        for (ResolveInfo resolveInfo : list) {
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);

            if (SecondIcon && !iPack) {
                Drawable icon2 = null;
                if (appListAll.contains(appInfo)) {
                    AppInfo geticon = appListAll.get(appListAll.indexOf(appInfo));
                    icon2 = geticon.icon;
                }
                appInfo = new AppInfo(icon1, icon2, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);
            }

            if (OnlyNew && !iPack) {
                if (!appListAll.contains(appInfo)) {
                    arrayList.add(appInfo);
                }
            } else {
                arrayList.add(appInfo);
            }
        }

        return CommonHelper.sort(arrayList);
    }

}