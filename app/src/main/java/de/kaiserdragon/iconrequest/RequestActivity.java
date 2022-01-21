package de.kaiserdragon.iconrequest;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

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
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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


public class RequestActivity extends AppCompatActivity {
    private static final String TAG = "RequestActivity";
    private static final int BUFFER = 2048;
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static String xmlString;
    private static boolean updateOnly;
    private static boolean OnlyNew;
    private static boolean SecondIcon;
    private static ArrayList<AppInfo> appListFilter = new ArrayList<>();
    private static ArrayList<iPackInfo> IPackListFilter = new ArrayList<>();
    private String ImgLocation;
    private String ZipLocation;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private Context context;
    private boolean IPackChoosen = false;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //todo check if next line as something todo with saving the data even if you go back
        super.onCreate(savedInstanceState);

        updateOnly = getIntent().getBooleanExtra("update", false);
        OnlyNew = loadDataBool("SettingOnlyNew");
        SecondIcon = loadDataBool("SettingRow");

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);
        context = this;

        ImgLocation = context.getFilesDir() + "/Icons/IconRequest";
        ZipLocation = context.getFilesDir() + "/Icons";


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //if (savedInstanceState == null) {

        ExecutorService executors = Executors.newSingleThreadExecutor();
        executors.execute(() -> {
            try {
                if (OnlyNew | SecondIcon) {
                    prepareDataIPack(); //show only apps that arent in the selectable Icon Pack
                } else {
                    prepareData();  //show all apps
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (OnlyNew | SecondIcon) {
                    populateView_Ipack(IPackListFilter);
                } else {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                    populateView(appListFilter);
                }
                switcherLoad.showNext();
            });
        });

        //} else {
        //      populateView_Ipack(IPackListFilter);
        //populateView(appListFilter);
        //     switcherLoad.showNext();
        //  }
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> actionSaveext(actionSave(), result));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (OnlyNew && !IPackChoosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
            if (updateOnly) {
                getMenuInflater().inflate(R.menu.menu_request_update, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_request_new, menu);
            }
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            actionSend(actionSave());
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            actionSendSave();
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            actionSave();
            actionSendText();
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            actionSave();
            actionCopy();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
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

    private void actionSendText() {
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
        Intent data = result.getData();
        if (data != null) {
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

    }

    private void actionSendSave() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        String zipName = date.format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "IconRequest_" + zipName);
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
        ArrayList LabelList = new ArrayList();
        // process selected apps
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).selected) {
                String iconName = arrayList.get(i).label
                        .replaceAll("[^a-zA-Z0-9.\\-;]+", "")
                        .toLowerCase();
                if (DEBUG) Log.i(TAG, "iconName: " + iconName);
                int n = 0;
                while (LabelList.contains(iconName)) {
                    n++;
                    iconName = iconName + n;
                }
                LabelList.add(iconName);
                if (DEBUG) Log.i(TAG, "iconName: " + iconName);
                //check if icon is in an arraylist if not add else rename and check again
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

        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
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
//todo check what this does keepDirectoryStructure
            createZipFile(ImgLocation, true, ZipLocation + "/" + zipName + ".zip");

            // delete all generated files except the zip
            deleteDirectory(imgLocation);
            //todo why create the zipfile at updateonly is the appfilter and images really needed?
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

    private void parseXML(String packageName) {
        // load appfilter.xml from the icon pack package
        Resources iconPackres;
        PackageManager pm = getPackageManager();

        try {
            iconPackres = pm.getResourcesForApplication(packageName);
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = xmlFactoryObject.newPullParser();

            try {
                InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                xpp = factory.newPullParser();
                xpp.setInput(appfilterstream, "utf-8");
            } catch (IOException e1) {
                Log.v(TAG, "No appfilter.xml file");
            }
            //write content of icon pack appfilter to the appListAll arraylist
            if (xpp != null) {
                int activity = xpp.getEventType();
                while (activity != XmlPullParser.END_DOCUMENT) {
                    String name = xpp.getName();
                    switch (activity) {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.END_TAG:
                            if (name.equals("item")) {
                                try {
                                    String xmlLabel = xpp.getAttributeValue(null, "drawable");
                                    String xmlComponent =
                                            xpp.getAttributeValue(null, "component");

                                    String[] xmlCode = xmlComponent.split("/");
                                    if (xmlCode.length > 1) {
                                        String xmlPackage = xmlCode[0].substring(14);
                                        String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                        //if (DEBUG) Log.v(TAG, "XML APP: "+ xmlLabel);
                                        Drawable icon = null;
                                        if (SecondIcon) {
                                            if (xmlLabel != null)
                                                icon = loadDrawable(xmlLabel, iconPackres, packageName);
                                        }
                                        appListAll.add(new AppInfo(icon, null,
                                                xmlLabel, xmlPackage, xmlClass, false));
                                        // if (DEBUG) Log.v(TAG, "XML APP: " + xmlLabel +"  " + xmlPackage);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                    }
                    activity = xpp.next();
                }
            }
        } catch (Exception e) {
            makeToast(getString(R.string.appfilter_assets));
            e.printStackTrace();
        }
    }

    //get the drawable for an app from the icon Pack
    private Drawable loadDrawable(String drawableName, Resources iconPackres, String packageName) {
        int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
        if (id > 0) {
            return ResourcesCompat.getDrawable(iconPackres, id, null);
        }
        return null;
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

        for (int i = 0; i < list.size(); i++) {
            ResolveInfo resolveInfo = localIterator.next();
            Drawable icon1 = getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1,
                    null,
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name,
                    false);

            if (SecondIcon) {
                Drawable icon2 = null;
                if (appListAll.contains(appInfo)) {//check if the list contains the element
                    int o = appListAll.indexOf((appInfo));
                    if (DEBUG) Log.v(TAG, String.valueOf(o));
                    AppInfo geticon = appListAll.get(appListAll.indexOf(appInfo));//get the element by passing the index of the element
                    //if (DEBUG) Log.v(TAG, "label" + String.valueOf(geticon.label));
                    icon2 = geticon.icon;
                    // if (DEBUG) Log.v(TAG,"iconwert" + String.valueOf(icon2));
                }
                appInfo = new AppInfo(icon1,
                        icon2,
                        resolveInfo.loadLabel(pm).toString(),
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name,
                        false);
            }

            if (OnlyNew) {
                // filter out apps that are already included
                if (!appListAll.contains(appInfo)) {
                    arrayList.add(appInfo);
                    if (DEBUG) Log.i(TAG, "Added app: " + resolveInfo.loadLabel(pm));
                } else {
                    if (DEBUG) Log.v(TAG, "Removed app: " + resolveInfo.loadLabel(pm));
                }
            } else arrayList.add(appInfo);

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

    private void prepareDataIPack() {
        // sort the apps
        ArrayList<iPackInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent("org.adw.launcher.THEMES", null);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        Iterator<ResolveInfo> localIterator = list.iterator();
        if (DEBUG) Log.v(TAG, "list size: " + list.size());
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo resolveInfo = localIterator.next();

            iPackInfo ipackinfo = new iPackInfo(getHighResIcon(pm, resolveInfo),
                    //icon2,
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    // resolveInfo.activityInfo.name,
                    //todo remove unused data
                    false);
            arrayList.add(ipackinfo);

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
        IPackListFilter = arrayList;
    }

    private Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo) {

        Drawable icon;

        try {
            ComponentName componentName = new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon

            if (iconId != 0) {
                icon = ResourcesCompat.getDrawable(pm.getResourcesForActivity(componentName), iconId, null); //loads unthemed
                /*
                icon = context.getPackageManager().getApplicationIcon(resolveInfo.activityInfo.packageName); //loads themed OnePlus
                icon =pm.getDrawable(resolveInfo.activityInfo.packageName, iconId, null);               //loads unthemed
                Drawable adaptiveDrawable = resolveInfo.loadIcon(pm);                                     //loads themed OnePlus
                PackageManager packageManager = getPackageManager();
                icon = resolveInfo.loadIcon(packageManager);                                             //loads themed OnePlus
                */
                return icon;
            }
            return resolveInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            //fails return the normal icon
            return resolveInfo.loadIcon(pm);
        } catch (Resources.NotFoundException e) {
            return resolveInfo.loadIcon(pm);
        }
    }

    private void populateView(ArrayList<AppInfo> arrayListFinal) {
        ArrayList<AppInfo> local_arrayList;
        local_arrayList = arrayListFinal;

        ListView grid = findViewById(R.id.app_list);
        grid.setFastScrollEnabled(true);
        //grid.setFastScrollAlwaysVisible(true);
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

    private void populateView_Ipack(ArrayList<iPackInfo> arrayListFinal) {
        ArrayList<iPackInfo> local_arrayList;
        local_arrayList = arrayListFinal;

        ListView grid = findViewById(R.id.app_list);
        grid.setFastScrollEnabled(true);
        //grid.setFastScrollAlwaysVisible(true);
        grid.setAdapter(new RequestActivity.IPackAppAdapter(this, R.layout.item_iconpack, local_arrayList));
        grid.setOnItemClickListener((AdapterView, view, position, row) -> {
            iPackInfo ipackinfo = (iPackInfo) AdapterView.getItemAtPosition(position);
            switcherLoad.showNext();
            ExecutorService executors = Executors.newSingleThreadExecutor();
            executors.execute(() -> {
                try {
                    parseXML(ipackinfo.packageName);
                    if (DEBUG) Log.v(TAG, ipackinfo.packageName);
                    prepareData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                    populateView(appListFilter);
                    IPackChoosen = true;
                    invalidateOptionsMenu();
                    //populateView(appListFilter); //Orginal fill view
                    switcherLoad.showNext();
                });
            });
        });
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
            if (SecondIcon) {
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

    private class IPackAppAdapter extends ArrayAdapter<iPackInfo> {
        private final ArrayList<iPackInfo> appList = new ArrayList<>();

        public IPackAppAdapter(Context context, int position, ArrayList<iPackInfo> adapterArrayList) {
            super(context, position, adapterArrayList);
            appList.addAll(adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            RequestActivity.IPackAppAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.item_iconpack, null);
                holder = new RequestActivity.IPackAppAdapter.ViewHolder();
                holder.apkIcon = convertView.findViewById(R.id.ipackicon);
                holder.apkName = convertView.findViewById(R.id.ipacklabel);
                convertView.setTag(holder);
            } else {
                holder = (RequestActivity.IPackAppAdapter.ViewHolder) convertView.getTag();
            }

            iPackInfo appInfo = appList.get(position);
            holder.apkName.setText(appInfo.label);
            holder.apkIcon.setImageDrawable(appInfo.icon);
            return convertView;
        }

        private class ViewHolder {
            TextView apkName;
            ImageView apkIcon;
        }
    }
}

