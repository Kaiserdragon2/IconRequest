package de.kaiserdragon.iconrequest.helper;

import android.content.Context;
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

    private static XmlPullParserFactory xmlPullParserFactory;

    public static void parseXML(String packageName, Boolean loadSecondIcon, ArrayList<AppInfo> appListAll, Context context) {
        try {
            Resources iconPackRes = context.getPackageManager().getResourcesForApplication(packageName);
            XmlPullParser xpp = getAppfilterFile(packageName, iconPackRes, context);
            if (xpp != null) {
                parseXmlContent(context, xpp, loadSecondIcon, appListAll, iconPackRes, packageName);
            }
        } catch (Exception e) {
            handleParsingException(context, e);
        }
    }

    private static void parseXmlContent(Context context, XmlPullParser xpp, Boolean loadSecondIcon, ArrayList<AppInfo> appListAll, Resources iconPackRes, String packageName) throws XmlPullParserException, IOException {
        final String startTag = "item";
        final String drawableAttribute = "drawable";
        final String componentAttribute = "component";
        final int packageSubstringLength = 14;
        final char slash = '/';
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            try {
                if (eventType == XmlPullParser.START_TAG && startTag.equals(xpp.getName())) {
                    String xmlLabel = xpp.getAttributeValue(null, drawableAttribute);
                    String xmlComponent = xpp.getAttributeValue(null, componentAttribute);
                    if (xmlLabel != null && xmlComponent != null) {
                        int slashIndex = xmlComponent.indexOf(slash);
                        int endIndex = xmlComponent.length() - 1;
                        if (slashIndex > 0 && slashIndex < endIndex) {
                            String xmlPackage = xmlComponent.substring(packageSubstringLength, slashIndex);
                            String xmlClass = xmlComponent.substring(slashIndex + 1, endIndex);
                            Drawable icon = loadSecondIcon ? DrawableHelper.loadDrawable(xmlLabel, iconPackRes, packageName) : null;
                            appListAll.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false, packageName));
                        }
                    }
                }
            } catch (Exception e) {
                handleParsingException(context, e);
            }

            eventType = xpp.next();
        }
    }


    private static void handleParsingException(Context context, Exception e) {
        CommonHelper.makeToast(context.getString(R.string.appfilter_assets), context);
        Log.e(TAG, "Error parsing XML", e);
    }


    private static synchronized XmlPullParserFactory getXmlPullParserFactory() throws XmlPullParserException {
        if (xmlPullParserFactory == null) {
            xmlPullParserFactory = XmlPullParserFactory.newInstance();
        }
        return xmlPullParserFactory;
    }

    private static XmlPullParser getAppfilterFile(String packageName, Resources iconPackRes, Context context) {
        XmlPullParser xpp = null;
        int appFilterId = iconPackRes.getIdentifier("appfilter", "xml", packageName);
        if (appFilterId > 0) {
            xpp = iconPackRes.getXml(appFilterId);
        } else {
            try {
                InputStream appFilterStream = iconPackRes.getAssets().open("appfilter.xml");
                xpp = getXmlPullParserFactory().newPullParser();
                xpp.setInput(appFilterStream, "utf-8");
            } catch (IOException | XmlPullParserException e) {
                CommonHelper.makeToast(context.getString(R.string.appfilter_assets), context);
                Log.e(TAG, "Error opening appfilter.xml", e);
            }
        }
        return xpp;
    }
}

