package de.kaiserdragon.iconrequest;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public Drawable icon;
    public String label;
    String packageName;
    String className;
    public boolean selected;

    AppInfo(Drawable icon, String label, String packageName, String className, boolean selected) {
        this.icon = icon;
        this.label = label;
        this.packageName = packageName;
        this.className = className;
        this.selected = selected;
    }

    public String getCode() {
        return packageName + "/" + className;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof AppInfo) {
            AppInfo appInfo = (AppInfo) object;
            return this.getCode().equals(appInfo.getCode());
        }
        return false;
    }
}
