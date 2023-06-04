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
import android.widget.ImageView;
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
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static boolean updateOnly = false;
    private static int mode;
    private static boolean OnlyNew;
    private static boolean SecondIcon;
    private static boolean Shortcut;
    private static boolean firstrun;
    private final Context context = this;
    byte[] zipData = null;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean IPackChoosen = false;
    private RecyclerView recyclerView;
    private AppAdapter adapter;

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

    private ArrayList<AppInfo> compare() {
        ArrayList<AppInfo> newList = new ArrayList<>();
        ArrayList<AppInfo> Listdopp = new ArrayList<>();

        if (mode == 2) {
            for (AppInfo appInfo : appListAll) {
                if (newList.contains(appInfo)) {
                    newList.remove(appInfo);
                } else newList.add(appInfo);
            }
            return sort(newList);
        }
        if (mode == 3) {
            for (AppInfo appInfo : appListAll) {
                if (!newList.contains(appInfo)) {
                    newList.add(appInfo);
                } else {
                    AppInfo geticon = newList.get(newList.indexOf(appInfo));  //get the element by passing the index of the element
                    appInfo.icon2 = geticon.icon;
                    Listdopp.add(appInfo);
                }
            }

            return sort(Listdopp);
        }
        if (mode == 4) {
            for (AppInfo appInfo : appListAll) {
                if (!newList.contains(appInfo)) {
                    newList.add(appInfo);
                } else {
                    AppInfo geticon = newList.get(newList.indexOf(appInfo));  //get the element by passing the index of the element
                    appInfo.icon2 = geticon.icon;
                    Listdopp.add(appInfo);
                }
            }

            return sort(Listdopp);
        }
        if (mode == 5) {
            for (AppInfo appInfo : appListAll) {
                if (appInfo.icon == null) newList.add(appInfo);
            }
            return sort(newList);
        }
        return null;
    }

    private ArrayList<AppInfo> sort(ArrayList<AppInfo> chaos) {
        Collections.sort(chaos, (object1, object2) -> {
            Locale locale = Locale.getDefault();
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.TERTIARY);

            if (DEBUG)
                Log.v(TAG, "Comparing \"" + object1.label + "\" to \"" + object2.label + "\"");

            return collator.compare(object1.label, object2.label);
        });
        return chaos;
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
        super.onCreate(savedInstanceState);
        appListAll.clear();
        mode = getIntent().getIntExtra("update", 0);
        updateOnly = mode == 0;
        OnlyNew = loadDataBool("SettingOnlyNew");
        SecondIcon = loadDataBool("SettingRow");
        Shortcut = loadDataBool("Shortcut");
        firstrun = false;

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        if (OnlyNew || SecondIcon || (mode >= 2 && mode <= 5)) {
            adapter = new AppAdapter(prepareData(true));

        } else adapter = new AppAdapter(prepareData(false)); //show all apps
        if (!OnlyNew && !SecondIcon && (mode < 2 || mode > 5))
            findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
        recyclerView.setAdapter(adapter);
        switcherLoad.showNext();
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> actionSaveext(actionSave(), result));
    }

    public void IPackSelect(String packageName) {
        switcherLoad.showNext();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            try {
                parseXML(packageName);
                if (DEBUG) Log.v(TAG, packageName);

                if (mode < 2 || mode > 5) {
                    adapter = new AppAdapter(prepareData(false));
                }
                if (!(mode <= 1) && (mode != 2 && mode != 3 ||firstrun))  {
                    adapter = new AppAdapter(compare());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                // Show IPack chooser a second Time
                if (mode != 2 && mode != 3 || firstrun) {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                    IPackChoosen = true;
                    invalidateOptionsMenu();
                }
                firstrun = true;
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });
    }




    public boolean onCreateOptionsMenu(Menu menu) {
        if ((OnlyNew || (mode >= 2 && mode <= 5)) && !IPackChoosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_sharetext);
            MenuItem copy = menu.findItem(R.id.action_copy);


            if (updateOnly || (mode >= 2 && mode <= 5)) {
                save.setVisible(false);
                share.setVisible(false);
                share_text.setVisible(true);
                copy.setVisible(true);
            } else {
                share_text.setVisible(false);
                copy.setVisible(false);
                save.setVisible(true);
                share.setVisible(true);
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
            actionSendText(actionSave());
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            actionCopy(actionSave());
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.selectall) {
            adapter.setAllSelected(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }

    public void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private boolean actionCopy(String[] array) {
        if (array[0] == null) return false;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Icon Request", array[1]);
        clipboard.setPrimaryClip(clip);
        makeToast("Your icon request has been saved to the clipboard.");
        return true;
    }

    private boolean actionSend(String[] array) {
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
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.request_email_subject));
        intent.putExtra("android.intent.extra.TEXT", array[1]);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
        return true;
    }

    private boolean actionSendText(String[] array) {
        if (array[0] == null) return false;
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, array[1]);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
        return true;
    }

    private boolean actionSaveext(String[] array, ActivityResult result) {
        if (array[0] == null) return false;
        if (DEBUG) Log.i(TAG, String.valueOf(result));
        Intent data = result.getData();
        if (data != null) {
            try (InputStream is = new ByteArrayInputStream(zipData); OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        ArrayList<AppInfo> arrayList = adapter.getAllSelected();
        if (arrayList.size() <= 0) {
            // no apps are selected
            makeToast(getString(R.string.request_toast_no_apps_selected));
            return new String[]{null};
        }

        StringBuilder stringBuilderEmail = new StringBuilder();
        StringBuilder stringBuilderXML = new StringBuilder();
        stringBuilderEmail.append(getString(R.string.request_email_text));
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
                    Bitmap bitmap = getBitmapFromDrawable(arrayList.get(i).icon);
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
            zipData = baos.toByteArray();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{zipName, stringBuilderEmail.toString()};
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
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
            XmlPullParser xpp = null;
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", packageName);
            if (appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            } else {
                try {
                    InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    xpp = factory.newPullParser();
                    xpp.setInput(appfilterstream, "utf-8");
                } catch (IOException e1) {
                    makeToast(getString(R.string.appfilter_assets));
                    Log.v(TAG, "No appfilter.xml file");
                }
            }
            //write content of icon pack appfilter to the appListAll arraylist
            if (xpp != null) {
                int activity = xpp.getEventType();
                while (activity != XmlPullParser.END_DOCUMENT) {
                    String name = xpp.getName();
                    switch (activity) {
                        case XmlPullParser.END_TAG:
                            break;
                        case XmlPullParser.START_TAG:
                            if (name.equals("item")) {
                                try {
                                    String xmlLabel = xpp.getAttributeValue(null, "drawable");
                                    String xmlComponent = xpp.getAttributeValue(null, "component");

                                    String[] xmlCode = xmlComponent.split("/");
                                    if (xmlCode.length > 1) {
                                        String xmlPackage = xmlCode[0].substring(14);
                                        String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                        Drawable icon = null;
                                        if (SecondIcon || (mode >= 2 && mode <= 5)) {
                                            if (xmlLabel != null)
                                                icon = loadDrawable(xmlLabel, iconPackres, packageName);
                                        }
                                        appListAll.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false));
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

    private ArrayList<AppInfo> prepareData(boolean iPack) {
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent;

        if (iPack) {
            intent = new Intent("org.adw.launcher.THEMES", null);
        } else if (Shortcut) {
            intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
            intent.addCategory("android.intent.category.DEFAULT");
        } else {
            intent = new Intent("android.intent.action.MAIN", null);
            intent.addCategory("android.intent.category.LAUNCHER");
        }

        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

        if (DEBUG) Log.v(TAG, "list size: " + list.size());

        for (ResolveInfo resolveInfo : list) {
            Drawable icon1 = getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);

            if (SecondIcon && !iPack) {
                Drawable icon2 = null;
                if (appListAll.contains(appInfo)) {
                    AppInfo geticon = appListAll.get(appListAll.indexOf(appInfo));
                    icon2 = geticon.icon;
                }
                appInfo = new AppInfo(icon1, icon2, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);
            }

            if (OnlyNew && !iPack) {
                if (!appListAll.contains(appInfo)) {
                    arrayList.add(appInfo);
                    if (DEBUG) Log.i(TAG, "Added app: " + resolveInfo.loadLabel(pm));
                } else {
                    if (DEBUG) Log.v(TAG, "Removed app: " + resolveInfo.loadLabel(pm));
                }
            } else {
                arrayList.add(appInfo);
            }
        }

        return sort(arrayList);
    }


    private Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo) {

        Drawable icon;

        try {
            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon
            if (iconId != 0) {
                icon = ResourcesCompat.getDrawable(pm.getResourcesForActivity(componentName), iconId, null); //loads unthemed
                return icon;
            }
            return resolveInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            //fails return the normal icon
            return resolveInfo.loadIcon(pm);
        }
    }

    public boolean loadDataBool(String setting) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean(setting, false);
    }

    public class AppAdapter extends RecyclerView.Adapter<AppViewHolder> {
        private final List<AppInfo> appList;

        public AppAdapter(List<AppInfo> appList) {
            this.appList = appList;
        }

        public ArrayList<AppInfo> getAllSelected() {
            ArrayList<AppInfo> arrayList = new ArrayList<>();
            for (AppInfo app : appList) {
                if (app.selected) arrayList.add(app);
            }
            return arrayList;
        }

        public void setAllSelected(boolean selected) {
            for (AppInfo app : appList) {
                app.setSelected(selected);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
            return new AppViewHolder(v, appList);
        }


        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            AppInfo app = appList.get(position);
            holder.labelView.setText(app.getLabel());
            holder.packageNameView.setText(app.packageName);
            holder.classNameView.setText(app.className);
            holder.imageView.setImageDrawable(app.getIcon());
            if (app.selected) holder.checkBox.setDisplayedChild(1);
            else holder.checkBox.setDisplayedChild(0);
            if ((SecondIcon || mode == 3 || mode == 4) && IPackChoosen && !(mode == 2)) {
                holder.apkIconView.setVisibility(View.VISIBLE);
                holder.apkIconView.setImageDrawable(app.getIcon2());
            }

        }

        @Override
        public int getItemCount() {
            return appList.size();
        }
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        public TextView labelView;
        public TextView packageNameView;
        public TextView classNameView;
        public ImageView imageView;
        public ImageView apkIconView;
        public ViewSwitcher checkBox;

        public AppViewHolder(View v, List<AppInfo> appList) {
            super(v);
            labelView = v.findViewById(R.id.label_view);
            packageNameView = v.findViewById(R.id.packagename_view);
            classNameView = v.findViewById(R.id.classname_view);
            imageView = v.findViewById(R.id.icon_view);
            apkIconView = v.findViewById(R.id.apkicon_view);
            checkBox = v.findViewById(R.id.SwitcherChecked);

            v.setOnClickListener(v1 -> {
                int position = getAdapterPosition();
                AppInfo app = appList.get(position);
                app.setSelected(!app.isSelected());
                if (!IPackChoosen && (OnlyNew || SecondIcon || (mode >= 2 && mode <= 5)))
                    IPackSelect(app.packageName);
                Animation aniIn = AnimationUtils.loadAnimation(checkBox.getContext(), R.anim.request_flip_in_half_1);
                Animation aniOut = AnimationUtils.loadAnimation(checkBox.getContext(), R.anim.request_flip_in_half_2);
                checkBox.setInAnimation(aniIn);
                checkBox.setOutAnimation(aniOut);
                checkBox.showNext();

            });
        }
    }
}