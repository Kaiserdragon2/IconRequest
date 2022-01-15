package de.kaiserdragon.iconrequest;

import android.graphics.drawable.Drawable;

public class iPackInfo {
    public Drawable icon;
    public String label;
    public boolean selected;
    String packageName;
    String className;

    iPackInfo(Drawable icon, String label, String packageName, boolean selected) {
        this.icon = icon;
        this.label = label;
        this.packageName = packageName;
        this.selected = selected;
    }

    public String getCode() {
        return packageName + "/" + className;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof iPackInfo) {
            iPackInfo ipackinfo = (iPackInfo) object;
            return this.getCode().equals(ipackinfo.getCode());
        }
        return false;
    }
}

