package de.kaiserdragon.iconrequest;

import static androidx.core.content.ContextCompat.startActivity;

import static de.kaiserdragon.iconrequest.RequestActivity.deleteDirectory;

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

public class ShareHelper {
    public static boolean actionCopy(String[] array,Context context) {
        if (array[0] == null) return false;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Icon Request", array[1]);
        clipboard.setPrimaryClip(clip);
        //makeToast("Your icon request has been saved to the clipboard.");
        return true;
    }

    public static boolean actionSend(String[] array,byte[] zipData, Context context) {
        if (array[0] == null) return false;
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
            //makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
        return true;
    }

    public static boolean actionSendText(String[] array,Context context) {
        if (array[0] == null) return false;
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, array[1]);
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            //makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
        return true;
    }

    public static boolean actionSaveext(String[] array,byte[] zipData, ActivityResult result,Context context) {
        if (array[0] == null) return false;
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
        return true;
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
}
