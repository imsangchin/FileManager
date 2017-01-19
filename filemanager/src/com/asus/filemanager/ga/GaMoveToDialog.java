package com.asus.filemanager.ga;

import android.content.Context;

public class GaMoveToDialog extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "move_to_dialog";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-9";
    public static final String KEY_ID = "GA_MOVE_TO_DIALOG_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_MOVE_TO_DIALOG_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_MOVE_TO_DIALOG_SAMPLE_RATE";

    public static final String ACTION_GO_TO_CURRENT_FOLDER = "current_folder";
    public static final String ACTION_GO_TO_PREVIOUS_FOLDER = "previous_folder";
    public static final String ACTION_GO_TO_LOCAL_STORAGE = "local_storage";
    public static final String ACTION_GO_TO_SAMBA_STORAGE = "samba_storage";
    public static final String ACTION_GO_TO_CLOUD_STORAGE = "cloud_storage";
    public static final String ACTION_ADD_FOLDER = "add_folder";

    private static GaMoveToDialog mInstance;

    public static GaMoveToDialog getInstance() {
        if (mInstance == null) {
            mInstance = new GaMoveToDialog();
        }

        return mInstance;
    }

    private GaMoveToDialog() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, CATEGORY_NAME, action, label, value);
    }
}
