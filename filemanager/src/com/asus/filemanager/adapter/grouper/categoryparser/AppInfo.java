package com.asus.filemanager.adapter.grouper.categoryparser;

import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;


import android.text.TextUtils;

public class AppInfo {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "[AppInfo]";
    private static final boolean DEBUG = true;
    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private String mName;
    private String mVersion;
    private String mCategory;
    private String mCoverImageUrl;
    private byte[] mCoverImageRawData;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------
    AppInfo(String name, String version, String coverImageUrl, String category) {
        this.mName = name;
        this.mVersion = version;
        this.mCategory = category;
        this.mCoverImageUrl = coverImageUrl;
    }

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    public String getName() {
        return mName;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getCoverImageUrl() {
        return mCoverImageUrl;
    }

    public String getCategory() {
        return mCategory;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(mName) && !TextUtils.isEmpty(mCategory) && !TextUtils.isEmpty(mCoverImageUrl);
    }

    public String toString() {
        return String.format("Name(%s) version(%s) category(%s) cover(%s)", mName, mVersion, mCategory, mCoverImageUrl);
    }

    public boolean hasCoverImageRawData() {
        return mCoverImageRawData != null && mCoverImageRawData.length > 0;
    }

    public void fetchCoverImage() {
        if (!isValid()) {
            Log.d(TAG, "[fetchCoverImages] does not have url, return");
            return;
        }

        if (hasCoverImageRawData()) {
            Log.d(TAG, "[fetchCoverImages] already have cover image, return");
            return;
        }

        try {
            mCoverImageRawData = NetUtils.readByteArrayFromUrl(new URL(mCoverImageUrl));
        } catch (MalformedURLException e) {
            Log.d(TAG, "[fetchCoverImages] invalid URL: " + mCoverImageUrl);
        }

        if (DEBUG) {
            int dataLength = mCoverImageRawData == null ? 0 : mCoverImageRawData.length;
            Log.v(TAG, "[fetchCoverImage] get cover image " + dataLength + " bytes");
        }
    }
}
