package com.asus.filemanager.ga;

import android.content.Context;

public class GaCategorySorting extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "category_sorting";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-6";
    public static final String KEY_ID = "GA_CATEGORY_SORTING_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_CATEGORY_SORTING_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_CATEGORY_SORTING_SAMPLE_RATE";

    public static final String ACTION_SORTING_RESULT_OK = "result_ok";
    public static final String ACTION_SORTING_RESULT_CANCEL = "result_cancel";

    private static GaCategorySorting mInstance;

    public static GaCategorySorting getInstance() {
        if (mInstance == null) {
            mInstance = new GaCategorySorting();
        }

        return mInstance;
    }

    private GaCategorySorting() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, CATEGORY_NAME, action, label, value);
    }
}
