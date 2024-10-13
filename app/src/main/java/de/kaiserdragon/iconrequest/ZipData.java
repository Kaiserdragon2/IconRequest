package de.kaiserdragon.iconrequest;

import android.app.Application;

public class ZipData extends Application {
    private byte[] zipData;

    public byte[] getZipData() {
        return zipData;
    }

    public void setZipData(byte[] zipData) {
        this.zipData = zipData;
    }
}
