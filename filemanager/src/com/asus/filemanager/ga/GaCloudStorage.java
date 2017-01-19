package com.asus.filemanager.ga;

import android.content.Context;

public class GaCloudStorage extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "cloud_storage";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-7";
    public static final String KEY_ID = "GA_CLOUD_STORAGE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_CLOUD_STORAGE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_CLOUD_STORAGE_SAMPLE_RATE";

    public static final String ACTION_OPEN_ASUS_WEBSTORAGE = "ASUS Webstorage";
    public static final String ACTION_OPEN_ASUS_HOMECLOUD = "ASUS HomeCloud";
    public static final String ACTION_OPEN_GOOGLE_DRIVE = "Drive";
    public static final String ACTION_OPEN_ONEDRIVE = "OneDrive";
    public static final String ACTION_OPEN_DROPBOX = "Dropbox";
    public static final String ACTION_OPEN_YANDEX = "Yandex";
    public static final String ACTION_OPEN_BAIDU = "Baidu";
    public static final String ACTION_OPEN_NETWORK_PLACE = "Network place";

    private static GaCloudStorage mInstance;

    public static GaCloudStorage getInstance() {
        if (mInstance == null) {
            mInstance = new GaCloudStorage();
        }

        return mInstance;
    }

    private GaCloudStorage() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
