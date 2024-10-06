package de.kaiserdragon.iconrequest.helper;

import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.kaiserdragon.iconrequest.AppInfo;

public class PrepareRequestData {
    private static final String TAG = "PrepareRequestData";

    private ArrayList<AppInfo> prepareDataOld(boolean iPack, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon, boolean Shortcut, boolean ActionMain, int mode,PackageManager pm) {


        //PackageManager pm = getPackageManager();
        //PackageManager pm = context.getPackageManager();
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

        if (list.isEmpty() && iPack){
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

    return null;
    }
    public static ArrayList<AppInfo> prepareDataActionMain(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon){
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm,list,appListAll,OnlyNew,SecondIcon);
    }

    public static ArrayList<AppInfo> prepareDataShortcuts(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon){
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
        intent.addCategory("android.intent.category.DEFAULT");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm,list,appListAll,OnlyNew,SecondIcon);
    }

    public static ArrayList<AppInfo> prepareDataIcons(Context context, ArrayList<AppInfo> appListAll, boolean OnlyNew, boolean SecondIcon){
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        return prepareSortData(pm,list,appListAll,OnlyNew,SecondIcon);
    }


    public static ArrayList<AppInfo> prepareDataIpack(Context context, ArrayList<AppInfo> appListAll){
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("org.adw.launcher.THEMES", null);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        if (list.isEmpty()) {
        //todo:implement maybe
        }
        return prepareSortData(pm,list,appListAll,false,false);
    }

    private static ArrayList<AppInfo> prepareSortData(PackageManager pm,List<ResolveInfo> list,ArrayList<AppInfo> appListAll,boolean OnlyNew,boolean SecondIcon){

        ArrayList<AppInfo> arrayList = new ArrayList<>();

        if (DEBUG) Log.v(TAG, "list size: " + list.size());

        for (ResolveInfo resolveInfo : list) {
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);

            if (SecondIcon) {
                Drawable icon2 = null;
                if (appListAll.contains(appInfo)) {
                    AppInfo geticon = appListAll.get(appListAll.indexOf(appInfo));
                    icon2 = geticon.icon;
                }
                appInfo = new AppInfo(icon1, icon2, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);
            }

            if (OnlyNew) {
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
