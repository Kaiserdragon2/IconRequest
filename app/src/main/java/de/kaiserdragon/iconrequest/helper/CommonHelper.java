package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.widget.Toast;

public class CommonHelper {
    public static void makeToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
