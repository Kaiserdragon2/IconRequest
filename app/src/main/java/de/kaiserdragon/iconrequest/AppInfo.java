package de.kaiserdragon.iconrequest;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String label;
    final String packageName;
    final String className;
    final String IconPackPackageName;
    private final Drawable icon;
    public boolean selected;
    private Drawable icon2;


    public AppInfo(Drawable icon, Drawable icon2, String label, String packageName, String className, boolean selected, String IconPackPackageName) {
        this.icon = icon;
        this.icon2 = icon2;
        this.label = label;
        this.packageName = packageName;
        this.className = className;
        this.selected = selected;
        this.IconPackPackageName = IconPackPackageName;
    }

    public String getCode() {
        return packageName + "/" + className;
    }

    public String getLabel() {
        return label;
    }

    public String getIconPackPackageName() {
        return IconPackPackageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void SetIcon2(Drawable icon) {
        this.icon2 = icon;
    }

    public Drawable getIcon2() {
        return icon2;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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
