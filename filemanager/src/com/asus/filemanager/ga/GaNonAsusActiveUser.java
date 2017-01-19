package com.asus.filemanager.ga;

import android.content.Context;

public class GaNonAsusActiveUser extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "non_asus_active_user";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-19";
    public static final String KEY_ID = "GA_NON_ASUS_ACTIVE_USER_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_NON_ASUS_ACTIVE_USER_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_NON_ASUS_ACTIVE_USER_SAMPLE_RATE";

    public static final String ACTION_ON_START_ACTIVITY = "on_start_activity";

    private static GaNonAsusActiveUser mInstance;

    public static GaNonAsusActiveUser getInstance() {
        if (mInstance == null) {
            mInstance = new GaNonAsusActiveUser();
        }

        return mInstance;
    }

    private GaNonAsusActiveUser() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 5.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
