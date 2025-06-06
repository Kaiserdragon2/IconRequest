package de.kaiserdragon.iconrequest;

import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.PrepareRequestData;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;
import de.kaiserdragon.iconrequest.interfaces.OnAppSelectedListener;

public class ChecksActivity extends AppCompatActivity implements OnAppSelectedListener {
    private static final String TAG = "ChecksActivity";
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static int mode;
    private final Context context = this;
    private ViewSwitcher switcherLoad;
    //private ActivityResultLauncher<Intent> activityResultLauncher;
    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private ProgressBar SelectedApps;
    private TextView ProgressText;

    @Override
    public void onAppSelected(String packageName, String label, Boolean iPackMode) {
        if (iPackMode) {
            Log.i(TAG, "onAppSelected: " + packageName);
            switcherLoad.showNext();
            startChecks(packageName);
        } else {
            SelectedApps.setMax(adapter.AdapterSize());
            SelectedApps.setProgress(adapter.getSelectedItemCount());
            ProgressText.setText(String.format(Locale.ENGLISH,"%d Selected", adapter.getSelectedItemCount()));
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

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        SelectedApps = findViewById(R.id.progressBarSelectedApps);
        ProgressText = findViewById(R.id.Apps_Selected);

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            // Prepare your data in a background thread
            Looper.prepare();
            adapter = new AppAdapter(PrepareRequestData.prepareDataIPack(this, appListAll), true, false, this);

            // Create a Handler for the main Looper
            Handler mainHandler = new Handler(Looper.getMainLooper());

            // Post the UI updates to the main thread
            mainHandler.post(() -> {
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });

        //activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,true,mode,context),zipData,result,context));

    }

    private void startChecks(String packageName) {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            try {
                Looper.prepare();
                XMLParserHelper.parseXML(packageName, true, appListAll, context);
                if (mode == 4) {
                    adapter = new AppAdapter(CommonHelper.findDuplicates(appListAll), false, true, this);
                } else if (mode == 5) {
                    adapter = new AppAdapter(CommonHelper.MissingIcon(appListAll), false, false, this);
                } else CommonHelper.makeToast("Error", this);

            } catch (Exception e) {
                Log.e(TAG, "startChecks: ", e);
            }
            runOnUiThread(() -> {
                findViewById(R.id.text_iPack_chooser).setVisibility(View.GONE);
                if (adapter.AdapterSize() < 1) {
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }else{
                    SelectedApps.setVisibility(View.VISIBLE);
                    ProgressText.setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);

                switcherLoad.showNext();
            });
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_request, menu);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem share_text = menu.findItem(R.id.action_shareText);
        MenuItem copy = menu.findItem(R.id.action_copy);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        MenuItem ShowIconPacks = menu.findItem(R.id.ShowIconPack);

        ShowIconPacks.setVisible(false);
        save.setVisible(false);
        share.setVisible(false);
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
        //if (item.getItemId() == R.id.action_share) {
        //    ShareHelper.actionSend(ShareHelper.actionSave(adapter,true,mode,context),zipData,context);
        //    return true;
        //} else if (item.getItemId() == R.id.action_save) {
        //    ShareHelper.actionSendSave(activityResultLauncher);
        //    return true;
        // } else
        if (item.getItemId() == R.id.action_shareText) {
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
