package de.kaiserdragon.iconrequest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CompareIPacks extends AppCompatActivity {
    private static final String TAG = "CompareActivity";
    private static final boolean DEBUG = true;
    private ViewSwitcher switcherLoad;
    private static ArrayList<iPackInfo> IPackListFilter = new ArrayList<>();
    private Context context;
    private static ArrayList<AppInfo> appListFilter = new ArrayList<>();
    private static ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static ArrayList<AppInfo> appListPack1 = new ArrayList<>();
    private static ArrayList<AppInfo> appListPack2 = new ArrayList<>();
    String Label1;
    String Label2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        appListPack1.clear();
        appListPack2.clear();
        appListFilter.clear();
        //updateOnly = getIntent().getBooleanExtra("update", false);
        //OnlyNew = loadDataBool("SettingOnlyNew");
        //SecondIcon = loadDataBool("SettingRow");

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);
        context = this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //if (savedInstanceState == null) {

        ExecutorService executors = Executors.newSingleThreadExecutor();
        executors.execute(() -> {
            try {
               // if (OnlyNew | SecondIcon) {
                    prepareDataIPack(); //show only apps that arent in the selectable Icon Pack
              //  } else {
               //     prepareData();  //show all apps
              //  }

            } catch (Exception e) {
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
              //  if (OnlyNew | SecondIcon) {

                    TextView chooser = (TextView)findViewById(R.id.text_ipack_chooser);
                    chooser.setText("Choose your first Icon Pack");
                    populateView_Ipack(IPackListFilter,true);


             //   } else {
                   // findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                 //   populateView(appListFilter);
              //  }
                switcherLoad.showNext();
            });
        });

        //} else {
        //      populateView_Ipack(IPackListFilter);
        //populateView(appListFilter);
        //     switcherLoad.showNext();
        //  }
        //activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> actionSaveext(actionSave(), result));
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_request_update, menu);
        return true;
    }

    public void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void parseXML(String packageName, boolean firstrun) {
        // load appfilter.xml from the icon pack package
        Resources iconPackres;
        PackageManager pm = getPackageManager();

        try {
            iconPackres = pm.getResourcesForApplication(packageName);
            XmlPullParser xpp = null;
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", packageName);
            if (appfilterid > 0)
            {
                xpp = iconPackres.getXml(appfilterid);
            }
            else {
                try {
                    InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    xpp = factory.newPullParser();
                    xpp.setInput(appfilterstream, "utf-8");
                } catch (IOException e1) {
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
                                    String xmlComponent =
                                            xpp.getAttributeValue(null, "component");

                                    String[] xmlCode = xmlComponent.split("/");
                                    if (xmlCode.length > 1) {
                                        String xmlPackage = xmlCode[0].substring(14);
                                        String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                        Drawable icon = null;
                                        if (xmlLabel != null)
                                            icon = loadDrawable(xmlLabel, iconPackres, packageName);
                                        if (firstrun)
                                            appListPack1.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false));
                                        else
                                            appListPack2.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false));
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

        appListAll = appListPack1;
        if (DEBUG) Log.v(TAG, "list size: " + appListAll.size());
        for (int i = 0; i < appListPack2.size(); i++) {
            AppInfo appInfo = appListPack2.get(i);
            if (!appListAll.contains(appInfo)) {
                arrayList.add(appInfo);
            }


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
        Intent intent2 = new Intent("com.gau.go.launcherex.theme", null);
        List<ResolveInfo> list2 = pm.queryIntentActivities(intent2, 0);
        Iterator<ResolveInfo> localIterator2 = list.iterator();
        for (int i = 0; i < list2.size(); i++) {
            ResolveInfo resolveInfo = localIterator2.next();

            iPackInfo ipackinfo = new iPackInfo(getHighResIcon(pm, resolveInfo),
                    //icon2,
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    // resolveInfo.activityInfo.name,
                    //todo remove unused data
                    false);
            if (!arrayList.contains(ipackinfo))
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

    private void populateView_Ipack(ArrayList<iPackInfo> arrayListFinal,boolean firstrun) {
        ArrayList<iPackInfo> local_arrayList;
        local_arrayList = arrayListFinal;

        ListView grid = findViewById(R.id.app_list);
        grid.setFastScrollEnabled(true);
        //grid.setFastScrollAlwaysVisible(true);
        grid.setAdapter(new IPackAppAdapter(this, R.layout.item_iconpack, local_arrayList));
        grid.setOnItemClickListener((AdapterView, view, position, row) -> {
            iPackInfo ipackinfo = (iPackInfo) AdapterView.getItemAtPosition(position);
            switcherLoad.showNext();
            ExecutorService executors = Executors.newSingleThreadExecutor();
            executors.execute(() -> {
                try {
                    parseXML(ipackinfo.packageName,firstrun);
                    if (DEBUG) Log.v(TAG, ipackinfo.packageName);

                    populateView_Ipack(IPackListFilter,false);
                    if (firstrun) Label1 = ipackinfo.label ;

                    if (!firstrun) {
                       Label2 = ipackinfo.label ;
                        prepareData();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        TextView chooser = (TextView) findViewById(R.id.text_ipack_chooser);
                        if (!firstrun) {
                        //findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                        populateView(appListFilter);
                        invalidateOptionsMenu();
                        switcherLoad.showNext();

                            chooser.setText("Unique Apps"+"\n"+Label1+": "+appListPack1.size()+"\n"+Label2+": "+appListPack2.size());
                        }else {
                            switcherLoad.showPrevious();

                            chooser.setText("Choose your second Icon Pack");
                        }

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
            AppAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.item_request, null);
                holder = new AppAdapter.ViewHolder();
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
                holder = (AppAdapter.ViewHolder) convertView.getTag();
            }

            AppInfo appInfo = appList.get(position);

            holder.apkPackage.setText(appInfo.packageName);
            holder.apkClass.setText(appInfo.className);
            holder.apkName.setText(appInfo.label);
            holder.apkIcon.setImageDrawable(appInfo.icon);
            holder.apkIconnow.setImageDrawable(appInfo.icon2);
            if (false) {
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
            IPackAppAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.item_iconpack, null);
                holder = new IPackAppAdapter.ViewHolder();
                holder.apkIcon = convertView.findViewById(R.id.ipackicon);
                holder.apkName = convertView.findViewById(R.id.ipacklabel);
                convertView.setTag(holder);
            } else {
                holder = (IPackAppAdapter.ViewHolder) convertView.getTag();
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
