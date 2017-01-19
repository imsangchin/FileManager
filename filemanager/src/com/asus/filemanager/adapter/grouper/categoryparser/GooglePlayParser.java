package com.asus.filemanager.adapter.grouper.categoryparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import android.util.Log;

/**
 * A parser that use Jsoup to fetch and parse Google Play page for given package
 */
public class GooglePlayParser {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "[GooglePlayParser]";
    private static final boolean DEBUG = true;

    public static final String LANGUAGE_ID_EN_US = "en-US";
    public static final String PLAY_STORE_LINK_PREFIX = "/store/apps/category/";

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private String mName = null;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------
    GooglePlayParser(String name) {
        this.mName = name;
    }

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    public void setPackage(String name) {
        this.mName = name;
    }

    public String getPackage() {
        return mName;
    }

    public AppInfo parse() {
        String languageID = LANGUAGE_ID_EN_US;
        String url = getPlayStoreUrl(getPackage(), languageID);
        Log.v(TAG, "[doInBackground] parse url: " + url);

        Document doc;
        Element nodeCategory = null;
        Element nodeCoverImage = null;
        Element nodeVersion = null;
        try {
            doc = Jsoup.connect(url).get();

            // search for category, cover image link and version nodes
            nodeCategory = doc.select("a[class=document-subtitle category]").first();
            nodeCoverImage = doc.select("img[alt=Cover art]").first();
            nodeVersion = doc.select("div[itemprop=softwareVersion]").first();

        } catch (Exception e) {
            Log.d(TAG, "[parse] exception: " + e.toString());
        }

        String categoryLink = nodeCategory != null ? nodeCategory.attr("href") : null;
        String categoryId = categoryLink != null ?
                categoryLink.substring(PLAY_STORE_LINK_PREFIX.length(), categoryLink.length()) : null;
        String coverImageUrl = nodeCoverImage != null ? nodeCoverImage.absUrl("src") : null;
        String version = nodeVersion != null ? nodeVersion.text() : null;

        return new AppInfo(getPackage(), version, coverImageUrl, categoryId);
    }

    private String getPlayStoreUrl(String pkgName, String languageID) {
        return "https://play.google.com/store/apps/details?id=" + pkgName + "&hl=" + languageID;
    }
}
