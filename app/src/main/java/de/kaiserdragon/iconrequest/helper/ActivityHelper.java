package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivityHelper {
    public static List<ActivityInfo> getAllActivities(Context context) {
        List<ActivityInfo> allActivities = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);

        for (PackageInfo packageInfo : installedPackages) {
            String packageName = packageInfo.packageName;
            try {
                PackageInfo pi = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                if (pi.activities != null) {
                    allActivities.addAll(Arrays.asList(pi.activities));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return allActivities;
    }
}
