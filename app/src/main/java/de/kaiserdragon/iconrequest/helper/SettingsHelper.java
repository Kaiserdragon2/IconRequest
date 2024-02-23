package de.kaiserdragon.iconrequest.helper;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {

    public static void saveData(String setting, int data, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(setting, data);
        editor.apply();
    }

    public static void saveDataBool(String setting, boolean data, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(setting, data);
        editor.apply();
    }

    public static int loadData(String setting, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt(setting, -1);
    }

    public static boolean loadDataBool(String setting,Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean(setting, false);
    }
}
