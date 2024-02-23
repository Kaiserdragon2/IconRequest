package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.kaiserdragon.iconrequest.AppInfo;
import de.kaiserdragon.iconrequest.R;

public class XMLParserHelper {
    private static final String TAG = "XMLParserHelper";
    public static void parseXML(String packageName, Boolean loadSecondIcon, ArrayList<AppInfo> appListAll, Context context) {
        // load appfilter.xml from the icon pack package
        Resources iconPackRes;
        PackageManager pm = context.getPackageManager();
        try {
            iconPackRes = pm.getResourcesForApplication(packageName);
            XmlPullParser xpp = getAppfilterFile(packageName,iconPackRes,context);
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
                                    if (xmlLabel != null) {
                                        String xmlComponent = xpp.getAttributeValue(null, "component");
                                        if (xmlComponent != null) {

                                            String[] xmlCode = xmlComponent.split("/");
                                            if (xmlCode.length > 1) {
                                                String xmlPackage = xmlCode[0].substring(14);
                                                String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                                Drawable icon = null;
                                                if (loadSecondIcon) {
                                                    icon = DrawableHelper.loadDrawable(xmlLabel, iconPackRes, packageName);
                                                }
                                                appListAll.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false));
                                            }
                                        }
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
            CommonHelper.makeToast(context.getString(R.string.appfilter_assets),context);
            e.printStackTrace();
        }
    }

    private static XmlPullParser getAppfilterFile(String packageName,Resources iconPackRes ,Context context){
            XmlPullParser xpp = null;
            int appFilterId = iconPackRes.getIdentifier("appfilter", "xml", packageName);
            if (appFilterId > 0) {
                xpp = iconPackRes.getXml(appFilterId);
            } else {
                try {
                    InputStream appFilterStream = iconPackRes.getAssets().open("appfilter.xml");
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    xpp = factory.newPullParser();
                    xpp.setInput(appFilterStream, "utf-8");
                } catch (IOException | XmlPullParserException e1) {
                    CommonHelper.makeToast(context.getString(R.string.appfilter_assets), context);
                    Log.v(TAG, "No appfilter.xml file");
                }
            }
            return xpp;
    }
}
