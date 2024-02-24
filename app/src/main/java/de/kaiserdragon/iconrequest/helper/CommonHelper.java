package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import de.kaiserdragon.iconrequest.AppInfo;

public class CommonHelper {
    private static final String TAG = "CommonHelper";

    public static void makeToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static ArrayList<AppInfo> compareNew(int mode, ArrayList<AppInfo> appListAll) {
        ArrayList<AppInfo> newList = new ArrayList<>();
        ArrayList<AppInfo> Listdopp = new ArrayList<>();
        HashSet<String> set = new HashSet<>();
        if (mode == 2) {
            for (AppInfo appInfo : appListAll) {
                if (set.contains(appInfo.getCode())) {
                    set.remove(appInfo.getCode());
                    newList.remove(appInfo);
                } else {
                    set.add(appInfo.getCode());
                    newList.add(appInfo);
                }
            }
            return sort(newList);
        }
        if (mode == 3 || mode == 4) {
            for (AppInfo appInfo : appListAll) {
                if (set.contains(appInfo.getCode())) {
                    AppInfo existingApp = findExistingApp(newList, appInfo);
                    assert existingApp != null;
                    appInfo.icon2 = existingApp.icon;
                    Listdopp.add(appInfo);
                } else {
                    set.add(appInfo.getCode());
                    newList.add(appInfo);
                }
            }
        }
        if (mode == 5) {
            ArrayList<AppInfo> filteredList = new ArrayList<>();
            for (AppInfo appInfo : appListAll) {
                if (appInfo.icon == null) {
                    filteredList.add(appInfo);
                }
            }
            return sort(filteredList);
        }

        return sort(Listdopp);
    }

    private static AppInfo findExistingApp(ArrayList<AppInfo> list, AppInfo target) {
        for (AppInfo appInfo : list) {
            if (appInfo.equals(target)) {
                return appInfo;
            }
        }
        return null; // Or throw an exception if not found, depending on your requirements.
    }


    public static ArrayList<AppInfo> sort(ArrayList<AppInfo> chaos) {
        Collections.sort(chaos, (object1, object2) -> {
            Locale locale = Locale.getDefault();
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.TERTIARY);

            return collator.compare(object1.label, object2.label);
        });
        return chaos;
    }
}
