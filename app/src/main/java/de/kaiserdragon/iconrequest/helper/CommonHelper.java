package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import de.kaiserdragon.iconrequest.AppInfo;

public class CommonHelper {
    public static void makeToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static ArrayList<AppInfo> findUnique(ArrayList<AppInfo> appListAll) {
        // Using a LinkedHashSet to maintain insertion order and avoid duplicates
        Set<String> seenCodes = new LinkedHashSet<>();
        ArrayList<AppInfo> newList = new ArrayList<>();

        for (AppInfo appInfo : appListAll) {
            // Check if the code has been seen before
            if (seenCodes.add(appInfo.getCode())) {
                // If the code is unique, add the appInfo to the newList
                newList.add(appInfo);
            }else
                newList.remove(appInfo);
        }

        // Return the sorted list of unique AppInfo objects
        return sort(newList);
    }


    public static ArrayList<AppInfo> findDuplicates(ArrayList<AppInfo> appListAll) {
        //ArrayList<AppInfo> uniqueApps = new ArrayList<>();
        ArrayList<AppInfo> duplicateApps = new ArrayList<>();
        HashSet<String> seenCodes = new HashSet<>();
        HashMap<String, AppInfo> codeToAppMap = new HashMap<>();

        // Iterate through the original app list
        for (AppInfo appInfo : appListAll) {
            String code = appInfo.getCode();

            // Check for duplicate code
            if (seenCodes.contains(code)) {
                AppInfo existingApp = codeToAppMap.get(code);
                if (existingApp != null) {
                    appInfo.SetIcon2(existingApp.getIcon()); // Link duplicate app's icon
                    duplicateApps.add(appInfo);
                }
            } else {
                // First time seeing this code
                seenCodes.add(code);
                //uniqueApps.add(appInfo);
                codeToAppMap.put(code, appInfo); // Map the code to the original app
            }
        }

        return sort(duplicateApps); // Return sorted list of duplicates
    }

    public static ArrayList<AppInfo> MissingIcon(ArrayList<AppInfo> appListAll) {
        ArrayList<AppInfo> filteredList = new ArrayList<>();
        for (AppInfo appInfo : appListAll) {
            if (appInfo.getIcon() == null) {
                filteredList.add(appInfo);
            }
        }
        return sort(filteredList);
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
