package de.kaiserdragon.iconrequest;

import static de.kaiserdragon.iconrequest.helper.IPackHelper.getIconPacks;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.DrawableHelper;
import de.kaiserdragon.iconrequest.helper.IPackHelper;

public class IPackSelectActivity extends AppCompatActivity {


    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean IPackChosen = false;
    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static int mode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        mode = getIntent().getIntExtra("update", 0);

        setContentView(R.layout.activity_select_icon_pack);
        switcherLoad = findViewById(R.id.viewSwitcherLoading);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        adapter = new AppAdapter(prepareData(),true,false,this);
        recyclerView.setAdapter(adapter);
        switcherLoad.showNext();

        Button startCompare = findViewById(R.id.ButtonSelect);
        startCompare.setOnClickListener(view -> startCompareIconPacks());

    }

    public void IPackSelect(String packageName) {}

    private void startCompareIconPacks(){
        ArrayList<AppInfo> arrayList = adapter.getAllSelected();
        ArrayList<String> stringList = new ArrayList<>();
        for (AppInfo appInfo : arrayList){
            stringList.add(appInfo.packageName);
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra("list", stringList);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".CompareActivity"));
        startActivity(intent);
    };

    private ArrayList<AppInfo> prepareData() {
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();

        List<ResolveInfo> list = getIconPacks(this);

        if (list.isEmpty()) {
        return null;
        }

        for (ResolveInfo resolveInfo : list) {
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);
            arrayList.add(appInfo);
        }
        return CommonHelper.sort(arrayList);
    }
}
