package com.asus.filemanager.ga;

import android.content.Context;

public class GaMenuItem extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "menu_item";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-8";
    public static final String KEY_ID = "GA_MENU_ITEM_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_MENU_ITEM_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_MENU_ITEM_SAMPLE_RATE";

    public static final String ACTION_ADD_FOLDER = "add_folder";

    public static final String ACTION_HIDE_SYSTEM_FILE = "hide_system_file";
    public static final String ACTION_CLEAR_SEARCH_HISTORY = "clear_search_history";
    public static final String ACTION_ENCOURAGE_US = "encourage_us";
    public static final String ACTION_TELL_FRIENDS = "tell_friends";
    public static final String ACTION_INVITEFB = "invite_fb";
	public static final String ACTION_INVITE_GOOGLE = "invite_google";
    public static final String ACTION_INSTANT_UPDATE = "instant_update";
    public static final String ACTION_REPORT_BUG = "report_bug";
    public static final String ACTION_ABOUT = "about";
    public static final String ACTION_TUTORIAL = "tutorial";

    private static GaMenuItem mInstance;

    public static GaMenuItem getInstance() {
        if (mInstance == null) {
            mInstance = new GaMenuItem();
        }

        return mInstance;
    }

    private GaMenuItem() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
