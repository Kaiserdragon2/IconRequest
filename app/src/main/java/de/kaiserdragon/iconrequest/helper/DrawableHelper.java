package de.kaiserdragon.iconrequest.helper;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

public class DrawableHelper {
    private static final String TAG ="DrawableHelper";
    public static Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo) {

        Drawable icon;
        try {
            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon
            if (iconId != 0) {
                icon = ResourcesCompat.getDrawable(pm.getResourcesForActivity(componentName), iconId, null); //loads unthemed
                return icon;
            }
            return resolveInfo.loadIcon(pm);
        } catch (Exception e) {
            try {
                //fails return the normal icon
                return resolveInfo.loadIcon(pm);
            }catch(Exception exception){
                Log.e(TAG, String.valueOf(exception));
                return null;
            }
        }
    }

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    //get the drawable for an app from the icon Pack
    public static Drawable loadDrawable(String drawableName, Resources iconPackres, String packageName) {
        int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
        if (id > 0) {
            return ResourcesCompat.getDrawable(iconPackres, id, null);
        }
        return null;
    }

}
