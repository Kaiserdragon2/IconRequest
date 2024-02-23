package de.kaiserdragon.iconrequest.helper;

import static de.kaiserdragon.iconrequest.BuildConfig.DEBUG;
import static de.kaiserdragon.iconrequest.helper.CommonHelper.makeToast;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.kaiserdragon.iconrequest.AppInfo;
import de.kaiserdragon.iconrequest.R;
import de.kaiserdragon.iconrequest.RequestActivity;
import de.kaiserdragon.iconrequest.AppAdapter;

public class ShareHelper {
    private static final String TAG = "ShareHelper";
    public static void actionCopy(String[] array,Context context) {
        if (array[0] == null) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Icon Request", array[1]);
        clipboard.setPrimaryClip(clip);
        makeToast("Your icon request has been saved to the clipboard.",context);
    }

    public static void actionSend(String[] array,byte[] zipData, Context context) {
        if (array[0] == null) return;
        final File ZipLocation = new File(context.getFilesDir() + "/share");
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/zip");
        deleteDirectory(ZipLocation);
        ZipLocation.mkdir();

        File file = new File(ZipLocation, array[0] + ".zip");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(zipData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra("android.intent.extra.SUBJECT", context.getString(R.string.request_email_subject));
        intent.putExtra("android.intent.extra.TEXT", array[1]);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(context.getString(R.string.no_email_clients),context);
            e.printStackTrace();
        }
    }

    public static void actionSendText(String[] array,Context context) {
        if (array[0] == null) return;
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, array[1]);
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(context.getString(R.string.no_email_clients),context);
            e.printStackTrace();
        }
    }

    public static void actionSaveExt(String[] array, byte[] zipData, ActivityResult result, Context context) {
        if (array[0] == null) return;
        //if (DEBUG) Log.i(TAG, String.valueOf(result));
        Intent data = result.getData();
        if (data != null) {
            try (InputStream is = new ByteArrayInputStream(zipData); OutputStream os = context.getContentResolver().openOutputStream(Objects.requireNonNull(data.getData()))) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    assert os != null;
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void actionSendSave(ActivityResultLauncher<Intent> activityResultLauncher) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        String zipName = date.format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "IconRequest_" + zipName);
        activityResultLauncher.launch(intent);
    }
    public static String[] actionSave(AppAdapter adapter, Boolean updateOnly, int mode, Context context) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        ArrayList<AppInfo> arrayList = adapter.getAllSelected();
        if (arrayList.size() == 0) {
            // no apps are selected
            makeToast(context.getString(R.string.request_toast_no_apps_selected),context);
            return new String[]{null};
        }

        StringBuilder stringBuilderEmail = new StringBuilder();
        StringBuilder stringBuilderXML = new StringBuilder();
        stringBuilderEmail.append(context.getString(R.string.request_email_text));
        ArrayList<String> LabelList = new ArrayList<>();
        stringBuilderXML.append("<appfilter>\n\n");
        // process selected apps
        for (int i = 0; i < arrayList.size(); i++) {
            //if (arrayList.get(i).selected) {
            String iconName = arrayList.get(i).label.replaceAll("[^a-zA-Z0-9 ]+", "").replaceAll("[ ]+", "_").toLowerCase();
            if (DEBUG) Log.i(TAG, "iconName: " + iconName);
            if (!updateOnly) {
                //if a name is a duplicate rename 1 so nothing gets replaced while saving
                int n = 0;
                while (LabelList.contains(iconName)) {
                    n++;
                    iconName = iconName + n;
                }
                LabelList.add(iconName);

                try {
                    Bitmap bitmap = DrawableHelper.getBitmapFromDrawable(arrayList.get(i).icon);
                    ByteArrayOutputStream baosimg = new ByteArrayOutputStream();
                    ZipEntry ze = new ZipEntry(iconName + ".png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baosimg);
                    zos.putNextEntry(ze);
                    zos.write(baosimg.toByteArray());
                    zos.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (DEBUG) Log.i(TAG, "iconName: " + iconName);
            //check if icon is in an arraylist if not add else rename and check again
            stringBuilderEmail.append(arrayList.get(i).label).append("\n");
            stringBuilderXML.append("\t<!-- ").append(arrayList.get(i).label).append(" -->\n\t<item component=\"ComponentInfo{").append(arrayList.get(i).getCode()).append("}\" drawable=\"").append(iconName).append("\"/>").append("\n\n");

        }
        stringBuilderXML.append("</appfilter>");
        // }
        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        String zipName = date.format(new Date());
        //xmlString = stringBuilderXML.toString();
        if (updateOnly || mode >= 2) return new String[]{zipName, stringBuilderXML.toString()};

        try {
            ZipEntry entry = new ZipEntry("appfilter.xml");
            zos.putNextEntry(entry);
            // Write the contents of the file
            byte[] data = stringBuilderXML.toString().getBytes();
            zos.write(data, 0, data.length);

            // Close the entry and the stream
            zos.closeEntry();
            zos.close();

            // You can then access the contents of the ZIP file as a byte array
            RequestActivity.zipData  = baos.toByteArray();

            return new String[]{zipName, stringBuilderEmail.toString()};
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{zipName, stringBuilderEmail.toString()};
    }

    private static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

}
