package com.asus.filemanager.adapter.grouper.categoryparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A parser that use Jsoup to fetch and parse Google Play page for given package
 */
public class WanDouJiaParser {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "[WanDouJiaParser]";
    private static final boolean DEBUG = true;

    public static final String WANDOUJIA_CATEGORY_ID_PREFIX = "/tag/";
    public static final String WANDOUJIA_BASE_URL = "http://www.wandoujia.com/";
    public static final String WANDOUJIA_APPS_URL = "http://www.wandoujia.com/apps/";

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
    WanDouJiaParser(String name) {
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
        String urlStr = getUrlStr(getPackage());
        Log.v(TAG, "[doInBackground] parse url: " + urlStr);

        Document doc;
        Element nodeCategory = null;
        Element nodeCoverImage = null;
        Element nodeVersion = null;
        try {
            doc = Jsoup.connect(urlStr).get();

            // TODO: support version parsing
            // search for category, cover image link and version nodes
            nodeCategory = doc.select("a[itemprop=SoftwareApplicationCategory]").first();
            nodeCoverImage = doc.select("div[class=app-icon]").first();
            if (nodeCoverImage != null) nodeCoverImage = nodeCoverImage.select("img[itemprop=image]").first();

        } catch (Exception e) {
            Log.d(TAG, "[parse] exception: " + e.toString());
        }

        String categoryLink = nodeCategory != null ? nodeCategory.attr("href") : null;
        String categoryId = categoryLink != null ? getCategoryFromUrl(categoryLink) : null;
        String coverImageUrl = nodeCoverImage != null ? nodeCoverImage.absUrl("src") : null;
        String version = nodeVersion != null ? nodeVersion.text() : null;

        return new AppInfo(getPackage(), version, coverImageUrl, categoryId);
    }

    private String getUrlStr(String pkgName) {
        return WANDOUJIA_APPS_URL + pkgName;
    }

    /**
     * @param link: the category link from WanDouJia, may be absolute or relative path
     */
    private String getCategoryFromUrl(String link) {
        URL url;
        try {
            // get absolute path URL
            // e.g., http://www.wandoujia.com/tag/%E4%BC%91%E9%97%B2%E6%97%B6%E9%97%B4?pos=w/tags/detail_com.yodo1.ctr2.WANDOUJIA_01
            url = new URL(new URL(WANDOUJIA_BASE_URL), link);
        } catch (MalformedURLException e) {
            Log.d(TAG, "[getCategoryFromUrl] failed to parse url: " + link);
            return null;
        }

        // eliminate base path and GET parameters
        String path = url.getPath(); // e.g., /tag/%E4%BC%91%E9%97%B2%E6%97%B6%E9%97%B4
        return path.startsWith(WANDOUJIA_CATEGORY_ID_PREFIX) ?
                path.substring(WANDOUJIA_CATEGORY_ID_PREFIX.length()) : null; // e.g., %E4%BC%91%E9%97%B2%E6%97%B6%E9%97%B4
    }
}
