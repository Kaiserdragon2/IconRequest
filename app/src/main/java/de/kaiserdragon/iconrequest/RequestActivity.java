package de.kaiserdragon.iconrequest;



import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;

import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;



import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;


public class RequestActivity extends AppCompatActivity {
    private static final String TAG = "RequestActivity";

    private String ImgLocation;
    private String ZipLocation;

    private ViewSwitcher switcherLoad;

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private static final int BUFFER = 2048;
    private static final boolean DEBUG = true;

    private static String xmlString;
    private static boolean updateOnly;

    private static ArrayList<AppInfo> appListFilter = new ArrayList<>();
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private Context context;


    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG) Log.v(TAG, "onBackPressed");
        finish();
    }

    public static void deleteDirectory(File path) {
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

    public static void createZipFile(final String path,
                                     final boolean keepDirectoryStructure,
                                     final String out_file) {
        final File f = new File(path);
        if (!f.canRead() || !f.canWrite()) {
            if (DEBUG) Log.d(TAG, path + " cannot be compressed due to file permissions.");
            return;
        }
        try {
            ZipOutputStream zip_out = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(out_file), BUFFER));

            if (keepDirectoryStructure) {
                zipFile(path, zip_out, "");
            } else {
                final File[] files = f.listFiles();
                assert files != null;
                for (final File file : files) {
                    zip_folder(file, zip_out);
                }
            }
            zip_out.close();
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.e("File not found", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            if (DEBUG) Log.e("IOException", e.getMessage());
            e.printStackTrace();
        }

    }

    // keeps directory structure
    public static void zipFile(String path, ZipOutputStream out, String relPath) {
        final File file = new File(path);
        if (!file.exists()) {
            if (DEBUG) Log.d(TAG, file.getName() + " does not exist!");
            return;
        }

        final byte[] buf = new byte[1024];

        final String[] files = file.list();

        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file.getAbsolutePath())) {
                out.putNextEntry(new ZipEntry(relPath + file.getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
                out.closeEntry();
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        } else if (files.length > 0) {
            // non-empty folder
            for (String file1 : files) {
                zipFile(path + "/" + file1, out, relPath + file.getName() + "/");
            }
        }
    }

    private static void zip_folder(File file, ZipOutputStream zout) throws IOException {
        byte[] data = new byte[BUFFER];
        int read;
        if (file.isFile()) {
            ZipEntry entry = new ZipEntry(file.getName());
            zout.putNextEntry(entry);
            BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
            while ((read = instream.read(data, 0, BUFFER)) != -1)
                zout.write(data, 0, read);
            zout.closeEntry();
            instream.close();
        } else if (file.isDirectory()) {
            String[] list = file.list();
            //int len = list.length;
            for (String aList : list) zip_folder(new File(file.getPath() + "/" + aList), zout);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateOnly = getIntent().getBooleanExtra("update", false);

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);
        context = this;

        ImgLocation = context.getFilesDir() + "/Icons/IconRequest";
        ZipLocation = context.getFilesDir() + "/Icons";


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {

            ExecutorService executors = Executors.newSingleThreadExecutor();
            executors.execute(() -> {
                try {
                    // get included apps
                    parseXML();
                    // compare list to installed apps
                    prepareData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    populateView(appListFilter);
                    switcherLoad.showNext();
                });
            });

        } else {
            populateView(appListFilter);
            switcherLoad.showNext();
        }
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                actionSaveext(actionSave(),result);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (updateOnly) {
            getMenuInflater().inflate(R.menu.menu_request_update, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request_new, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.action_share) {
            actionSend(actionSave());
            return true;
        }else if(item.getItemId()==R.id.action_save) {
            actionSendSave();
            return true;
        }else if(item.getItemId()==R.id.action_sharetext) {
            actionSendText(actionSave());
            return true;
        }else if(item.getItemId()==R.id.action_copy) {
            actionSave();
            actionCopy();
            return true;
        }else if(item.getItemId()==android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }else  {
            super.onOptionsItemSelected(item);
            return true;
            }
       }

       private boolean visible(Drawable one,Drawable two){
           Bitmap bmp1 = getBitmapFromDrawable(one);
           Bitmap bmp2 = getBitmapFromDrawable(two);

           ByteBuffer buffer1 = ByteBuffer.allocate(bmp1.getHeight() * bmp1.getRowBytes());
           bmp1.copyPixelsToBuffer(buffer1);

           ByteBuffer buffer2 = ByteBuffer.allocate(bmp2.getHeight() * bmp2.getRowBytes());
           bmp2.copyPixelsToBuffer(buffer2);

           return Arrays.equals(buffer1.array(), buffer2.array());
       }

    public void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void actionCopy() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Icon Request", xmlString);
        clipboard.setPrimaryClip(clip);
        makeToast("Your icon request has been saved to the clipboard.");
    }

    private void actionSend(String[] array) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/zip");

        File file = new File(ZipLocation + "/" + array[0] + ".zip");

        Uri uri = FileProvider.getUriForFile(
                context, context.getApplicationContext().getPackageName() + ".provider", file);
        //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.request_email_subject));
        intent.putExtra("android.intent.extra.TEXT", array[1]);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
    }

    private void actionSendText(String[] array) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, xmlString);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
    }

    private void actionSaveext(String[] array, ActivityResult result) {

        if (DEBUG) Log.i(TAG, String.valueOf(result));
        File sourceFile = new File(ZipLocation + "/" + array[0] + ".zip");
        Intent data =result.getData();
        try (InputStream is = new FileInputStream(sourceFile); OutputStream os = getContentResolver().openOutputStream(data.getData())) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void actionSendSave() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss",Locale.US);
        String zipName = date.format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE,"IconRequest_" + zipName);
        activityResultLauncher.launch(intent);

    }


    private String[] actionSave() {
        final File imgLocation = new File(ImgLocation);
        final File zipLocation = new File(ZipLocation);

        // delete old zips and recreate
        deleteDirectory(zipLocation);
        imgLocation.mkdirs();
        zipLocation.mkdirs();

        ArrayList<AppInfo> arrayList = appListFilter;
        StringBuilder stringBuilderEmail = new StringBuilder();
        StringBuilder stringBuilderXML = new StringBuilder();
        stringBuilderEmail.append(getString(R.string.request_email_text));
        int amount = 0;

        // process selected apps
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).selected) {
                String iconName = arrayList.get(i).label
                        .replaceAll("[^a-zA-Z0-9.\\-;]+", "")
                        .toLowerCase();
                if (DEBUG) Log.i(TAG, "iconName: " + iconName);

                stringBuilderEmail.append(arrayList.get(i).label).append("\n");
                stringBuilderXML.append("\t<!-- ")
                        .append(arrayList.get(i).label)
                        .append(" -->\n\t<item component=\"ComponentInfo{")
                        .append(arrayList.get(i).getCode())
                        .append("}\" drawable=\"")
                        .append(iconName)
                        .append("\"/>")
                        .append("\n\n");

                try {
                    Bitmap bitmap = getBitmapFromDrawable(arrayList.get(i).icon);
                    FileOutputStream fOut = new FileOutputStream(ImgLocation + "/" + iconName + ".png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                amount++;
            }
        }

        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss",Locale.US);
        String zipName = date.format(new Date());
        xmlString = stringBuilderXML.toString();

        if (amount == 0) {
            // no apps are selected
            makeToast(getString(R.string.request_toast_no_apps_selected));
        } else {
            // write zip and start email intent
            try {
                FileWriter fstream = new FileWriter(ImgLocation + "/appfilter.xml");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(stringBuilderXML.toString());
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            createZipFile(ImgLocation, true, ZipLocation + "/" + zipName + ".zip");

            // delete all generated files except the zip
            deleteDirectory(imgLocation);
            if (updateOnly) {
                deleteDirectory(zipLocation);
            }
        }
        return new String[]{zipName, stringBuilderEmail.toString()};
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    private void parseXML() {
        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();

            AssetManager am = context.getAssets();
            String xmlLocation = "empty.xml";
            InputStream inputStream = am.open(xmlLocation);
            myparser.setInput(inputStream, null);

            int activity = myparser.getEventType();
            while (activity != XmlPullParser.END_DOCUMENT) {
                String name = myparser.getName();
                switch (activity) {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.END_TAG:
                        if (name.equals("item")) {
                            try {
                                String xmlLabel = myparser.getAttributeValue(null, "drawable");
                                String xmlComponent =
                                        myparser.getAttributeValue(null, "component");

                                String[] xmlCode = xmlComponent.split("/");
                                if (xmlCode.length > 1) {
                                    String xmlPackage = xmlCode[0].substring(14);
                                    String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                    appListAll.add(new AppInfo(null,null,
                                            xmlLabel, xmlPackage, xmlClass, false));
                                    if (DEBUG) Log.v(TAG, "XML APP: " + xmlLabel);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
                activity = myparser.next();
            }
        } catch (Exception e) {
            makeToast(getString(R.string.appfilter_assets));
            e.printStackTrace();
        }
    }


    private void prepareData() {
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        Iterator<ResolveInfo> localIterator = list.iterator();
        if (DEBUG) Log.v(TAG, "list size: " + list.size());
        boolean notVisible = loadDataBool("SettingOnlyNew");
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo resolveInfo = localIterator.next();
            Drawable icon1 = getHighResIcon(pm, resolveInfo);
            Drawable icon2 = getHighResIcon(pm, resolveInfo);//resolveInfo.loadIcon(pm);
            if (DEBUG) Log.v(TAG, String.valueOf(icon2));
            AppInfo appInfo = new AppInfo(icon1,
                    icon2,
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name,
                    false);
            if (notVisible){
                if (DEBUG) Log.v(TAG, "Not Done");
                if (visible(icon1,icon2)) arrayList.add(appInfo);
            }else arrayList.add(appInfo);

        }

        //Custom comparator to ensure correct sorting for characters like and apps
        // starting with a small letter like iNex
        Collections.sort(arrayList, (object1, object2) -> {
            Locale locale = Locale.getDefault();
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.TERTIARY);

            if (DEBUG)
                Log.v(TAG, "Comparing \"" + object1.label + "\" to \"" + object2.label + "\"");

            return collator.compare(object1.label, object2.label);
        });
        appListFilter = arrayList;
    }

    private Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo) {

        Drawable icon;

        try {
            ComponentName componentName = new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon

            if (iconId != 0) {
                icon = ResourcesCompat.getDrawable(pm.getResourcesForActivity(componentName), iconId, null);
                //Drawable adaptiveDrawable = resolveInfo.loadIcon(pm);
                //PackageManager packageManager = getPackageManager();
                //icon = resolveInfo.loadIcon(packageManager);
                //icon = context.getDrawable(iconId);
                return icon;
            }
            return resolveInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            //fails return the normal icon
            return resolveInfo.loadIcon(pm);
        }catch (Resources.NotFoundException e) {
            return resolveInfo.loadIcon(pm);
        }
    }

    private void populateView(ArrayList<AppInfo> arrayListFinal) {
        ArrayList<AppInfo> local_arrayList;
        local_arrayList = arrayListFinal;

        ListView grid = findViewById(R.id.app_list);
        grid.setFastScrollEnabled(true);
        grid.setFastScrollAlwaysVisible(true);
        grid.setAdapter(new AppAdapter(this, R.layout.item_request, local_arrayList));
        grid.setOnItemClickListener((AdapterView, view, position, row) -> {
            AppInfo appInfo = (AppInfo) AdapterView.getItemAtPosition(position);
            CheckBox checker = view.findViewById(R.id.CBappSelect);
            ViewSwitcher icon = view.findViewById(R.id.viewSwitcherChecked);
            LinearLayout localBackground = view.findViewById(R.id.card_bg);
            Animation aniIn = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_1);
            Animation aniOut = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_2);

            checker.toggle();
            appInfo.selected = checker.isChecked();

            icon.setInAnimation(aniIn);
            icon.setOutAnimation(aniOut);

            if (appInfo.selected) {
                if (DEBUG) Log.v(TAG, "Selected App: " + appInfo.label);
                localBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_pressed));
                if (icon.getDisplayedChild() == 0) {
                    icon.showNext();
                }
            } else {
                if (DEBUG) Log.v(TAG, "Deselected App: " + appInfo.label);
                localBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_unpressed));
                if (icon.getDisplayedChild() == 1) {
                    icon.showPrevious();
                }
            }
        });
    }
    public boolean loadDataBool(String setting) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean(setting, false);
    }

    private class AppAdapter extends ArrayAdapter<AppInfo> {
        private final ArrayList<AppInfo> appList = new ArrayList<>();

        public AppAdapter(Context context, int position, ArrayList<AppInfo> adapterArrayList) {
            super(context, position, adapterArrayList);
            appList.addAll(adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.item_request, null);
                holder = new ViewHolder();
                holder.apkIcon = convertView.findViewById(R.id.IVappIcon);
                holder.apkIconnow = convertView.findViewById(R.id.IVappIconnow);
                holder.apkName = convertView.findViewById(R.id.TVappName);
                holder.apkPackage = convertView.findViewById(R.id.TVappPackage);
                holder.apkClass = convertView.findViewById(R.id.TVappClass);
                holder.checker = convertView.findViewById(R.id.CBappSelect);
                holder.cardBack = convertView.findViewById(R.id.card_bg);
                holder.switcherChecked = convertView.findViewById(R.id.viewSwitcherChecked);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppInfo appInfo = appList.get(position);

            holder.apkPackage.setText(appInfo.packageName);
            holder.apkClass.setText(appInfo.className);
            holder.apkName.setText(appInfo.label);
            holder.apkIcon.setImageDrawable(appInfo.icon);
           holder.apkIconnow.setImageDrawable(appInfo.icon2);
            if(loadDataBool("SettingRow")==true){
                holder.apkIconnow.setVisibility(View.VISIBLE);
            }


            holder.switcherChecked.setInAnimation(null);
            holder.switcherChecked.setOutAnimation(null);
            holder.checker.setChecked(appInfo.selected);
            if (appInfo.selected) {
                holder.cardBack.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_pressed));
                if (holder.switcherChecked.getDisplayedChild() == 0) {
                    holder.switcherChecked.showNext();
                }
            } else {
                holder.cardBack.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_unpressed));
                if (holder.switcherChecked.getDisplayedChild() == 1) {
                    holder.switcherChecked.showPrevious();
                }
            }
            return convertView;
        }

        private class ViewHolder {
            TextView apkName;
            TextView apkPackage;
            TextView apkClass;
            ImageView apkIcon;
            ImageView apkIconnow;
            CheckBox checker;
            LinearLayout cardBack;
            ViewSwitcher switcherChecked;

        }
    }
}
