package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

import de.kaiserdragon.iconrequest.AppInfo;

public class IPackHelper {

    public static List<ResolveInfo> getIconPacks(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("org.adw.launcher.THEMES", null);
        return pm.queryIntentActivities(intent, 0);
    }

}
