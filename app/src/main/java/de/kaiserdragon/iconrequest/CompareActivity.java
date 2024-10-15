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
import android.widget.TextView;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.PrepareRequestData;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;
import de.kaiserdragon.iconrequest.interfaces.OnAppSelectedListener;

public class CompareActivity extends AppCompatActivity implements OnAppSelectedListener {

    private static final String TAG = "CompareActivity";
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static int mode;
    private final Context context = this;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private ZipData zip;
    private String iconPack1PackageName;
    private String iconPack2PackageName;
    private MenuItem IconPack1;
    private MenuItem IconPack2;

    @Override
    public void onAppSelected(String packageName, String label) {
        //IPackSelect(packageName);
        Log.i(TAG, "onAppSelected: " + packageName);
        switch (adapter.getSelectedItemCount()) {
            case 1:
                IconPack1.setTitle(label);
                iconPack1PackageName = packageName;
                break;
            case 2:
                IconPack2.setTitle(label);
                iconPack2PackageName = packageName;
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
            adapter = new AppAdapter(PrepareRequestData.prepareDataIPack(this, appListAll), true, false, this);
            runOnUiThread(() -> {
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                TextView ipackChooserTextView = findViewById(R.id.text_iPack_chooser);
                if (ipackChooserTextView != null) {
                    ipackChooserTextView.setText(R.string.choose2IPack);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter, false, context), zip.getZipData(), result, context));

    }

    private void startCompareIconPacksDifference() {
        ArrayList<AppInfo> arrayList = adapter.getAllSelected();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            Looper.prepare();
            try {
                parseAppsInParallel(arrayList, context);
                if (mode == 2) {
                    Log.i(TAG, "startCompareIconPacksDifference: " + appListAll.size());
                    adapter = new AppAdapter(CommonHelper.findUnique(appListAll), false, false, this);
                } else if (mode == 3) {
                    adapter = new AppAdapter(CommonHelper.findDuplicates(appListAll), false, true, this);
                } else CommonHelper.makeToast("Error", this);

            } catch (Exception e) {
                Log.e(TAG, "startCompareIconPacksDifference: ", e);
            }
            runOnUiThread(() -> {
                findViewById(R.id.text_iPack_chooser).setVisibility(View.GONE);
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });

    }

    public void parseAppsInParallel(List<AppInfo> arrayList, Context context) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = new ArrayList<>();

        for (AppInfo appInfo : arrayList) {
            futures.add(executor.submit(() -> {
                // Synchronize access to appListAll when calling parseXML
                synchronized (appListAll) {
                    XMLParserHelper.parseXML(appInfo.packageName, true, appListAll, context);
                }
                return null; // No need for a return value here
            }));
        }

        // Shutdown the executor
        executor.shutdown();

        // Optionally wait for all tasks to complete and handle results or exceptions
        for (Future<Void> future : futures) {
            try {
                future.get(); // This will block until the task is complete
                if (future.isDone()){
                    Log.i(TAG, "parseAppsInParallel: done");
                }
            } catch (InterruptedException | ExecutionException e) {
                // Handle exceptions
                Log.e(TAG, "parseAppsInParallel: ", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_request, menu);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem share_text = menu.findItem(R.id.action_shareText);
        MenuItem copy = menu.findItem(R.id.action_copy);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        MenuItem ShowIconPacks = menu.findItem(R.id.ShowIconPack);
        IconPack1 = menu.findItem(R.id.IconPack1);
        IconPack2 = menu.findItem(R.id.IconPack2);
        ShowIconPacks.setVisible(true);
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
        } else if (item.getItemId() == R.id.IconPack1) {
            adapter.showIPack(iconPack1PackageName);
            return true;
        } else if (item.getItemId() == R.id.IconPack2) {
            adapter.showIPack(iconPack2PackageName);
            return true;
        } else if (item.getItemId() == R.id.IconPackAll) {
            adapter.showIPack("");
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }

}
