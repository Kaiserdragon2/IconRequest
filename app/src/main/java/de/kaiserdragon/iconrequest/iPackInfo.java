package de.kaiserdragon.iconrequest;

import android.graphics.drawable.Drawable;

public class iPackInfo {
    public Drawable icon;
    public String label;
    String packageName;

    iPackInfo(Drawable icon, String label, String packageName) {
        this.icon = icon;
        this.label = label;
        this.packageName = packageName;
    }


    /*
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
    */

}

