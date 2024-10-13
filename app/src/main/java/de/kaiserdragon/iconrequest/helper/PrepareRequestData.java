package de.kaiserdragon.iconrequest.helper;

import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.kaiserdragon.iconrequest.AppInfo;

public class PrepareRequestData {

    private static final String TAG = "PrepareRequestData";

    public static ArrayList<AppInfo> prepareDataActionMain(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm, list, appListAll, OnlyNew, SecondIcon);
    }

    public static ArrayList<AppInfo> prepareDataShortcuts(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
        intent.addCategory("android.intent.category.DEFAULT");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm, list, appListAll, OnlyNew, SecondIcon);
    }

    public static ArrayList<AppInfo> prepareDataIcons(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm, list, appListAll, OnlyNew, SecondIcon);
    }


    public static ArrayList<AppInfo> prepareDataIPack(Context context, ArrayList<AppInfo> appListAll) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("org.adw.launcher.THEMES", null);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        if (list.isEmpty()) {
            CommonHelper.makeToast("There are no icon packs installed", context);
        }
        return prepareSortData(pm, list, appListAll, false, false);
    }

    private static ArrayList<AppInfo> prepareSortData(PackageManager pm, List<ResolveInfo> list, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon) {

        ArrayList<AppInfo> arrayList = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (AppInfo appInfo : appListAll) {
            set.add(appInfo.getCode());
        }

        if (DEBUG) Log.v(TAG, "list size: " + list.size());

        for (ResolveInfo resolveInfo : list) {
            String label = resolveInfo.loadLabel(pm).toString();
            String packageName = resolveInfo.activityInfo.packageName;
            String className = resolveInfo.activityInfo.name;
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, label, packageName, className, false, null);

            if (SecondIcon) {
                Drawable icon2 = null;
                if (set.contains(appInfo.getCode())) {
                    AppInfo appInfoIcon = appListAll.get(appListAll.indexOf(appInfo));
                    icon2 = appInfoIcon.getIcon();
                }
                appInfo = new AppInfo(icon1, icon2, label, packageName, className, false, null);
            }
            if (OnlyNew) {
                if (!set.contains(appInfo.getCode())) {
                    arrayList.add(appInfo);
                }
            } else {
                arrayList.add(appInfo);
            }
        }
        return CommonHelper.sort(arrayList);
    }
}
