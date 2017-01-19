package com.asus.filemanager.ga;

import android.content.Context;

public class GaPhotoViewer extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "photo_viewer";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-13";

    public static final String KEY_ID = "GA_PHOTO_VIEWER_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_PHOTO_VIEWER_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_PHOTO_VIEWER_SAMPLE_RATE";

    public static final String ACTION_SHRARE = "share";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_OPEN_WITH = "open_with";
    public static final String ACTION_SET_AS = "set_as";

    private static GaPhotoViewer mInstance;

    public static GaPhotoViewer getInstance() {
        if (mInstance == null) {
            mInstance = new GaPhotoViewer();
        }

        return mInstance;
    }

    private GaPhotoViewer() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
