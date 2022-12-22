package de.kaiserdragon.iconrequest;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public Drawable icon;
    public Drawable icon2;
    public String label;
    public boolean selected;
    String packageName;
    String className;

    public AppInfo(Drawable icon, Drawable icon2, String label, String packageName, String className, boolean selected) {
        this.icon = icon;
        this.icon2 = icon2;
        this.label = label;
        this.packageName = packageName;
        this.className = className;
        this.selected = selected;
    }

    public String getCode() {
        return packageName + "/" + className;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
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
