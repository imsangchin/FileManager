package com.asus.filemanager.ga;

import android.content.Context;

public class GaUserPreference extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "user_preference";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-12";
    public static final String KEY_ID = "GA_USER_PREFERENCE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_USER_PREFERENCE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_USER_PREFERENCE_SAMPLE_RATE";

    public static final String ACTION_SORT_FILE_LIST = "sort_file_list";
    public static final String ACTION_SWITCH_DISPLAY_MODE = "switch_display_mode";

    public static final String LABEL_LISTVIEW_MODE = "list_view";
    public static final String LABEL_GRIDVIEW_MODE = "grid_view";

    public static final String LABEL_SORT_BY_TYPE = "type";
    public static final String LABEL_SORT_BY_NAME_Z_TO_A = "z_to_a";
    public static final String LABEL_SORT_BY_NAME_A_TO_Z = "a_to_z";
    public static final String LABEL_SORT_BY_DATE_OLDEST_TO_LATEST = "oldest_to_latest";
    public static final String LABEL_SORT_BY_DATE_LATEST_TO_OLDEST = "latest_to_oldest";
    public static final String LABEL_SORT_BY_SIZE_LARGE_TO_SMALL = "large_to_small";
    public static final String LABEL_SORT_BY_SIZE_SMALL_TO_LARGE = "small_to_large";

    private static GaUserPreference mInstance;

    public static GaUserPreference getInstance() {
        if (mInstance == null) {
            mInstance = new GaUserPreference();
        }

        return mInstance;
    }

    private GaUserPreference() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
