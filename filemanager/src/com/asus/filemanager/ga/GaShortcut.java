package com.asus.filemanager.ga;

import android.content.Context;

public class GaShortcut extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "shortcut";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-15";
    public static final String KEY_ID = "GA_SHORTCUT_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_SHORTCUT_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_SHORTCUT_SAMPLE_RATE";

    public static final String ACTION_CREATE_FROM_HOMEPAGE = "create_from_homepage";
    public static final String ACTION_CREATE_FROM_NON_HOMEPAGE = "create_from_non_homepage";
    public static final String ACTION_CREATE_FROM_WIDGET = "create_from_widget";

    public static final String LABEL_CATEGORY = "category";
    public static final String LABEL_NON_CATEGORY = "non_category";

    private static GaShortcut mInstance;

    public static GaShortcut getInstance() {
        if (mInstance == null) {
            mInstance = new GaShortcut();
        }

        return mInstance;
    }

    private GaShortcut() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
