package com.asus.filemanager.ga;

import android.content.Context;

public class GaSearchFile extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "search_file";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-11";
    public static final String KEY_ID = "GA_SEARCH_FILE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_SEARCH_FILE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_SEARCH_FILE_SAMPLE_RATE";

    public static final String ACTION_SEARCH_KEYWORD = "search_keyword";
    public static final String ACTION_CLICK_SEARCH_ICON = "click_search_icon";
    public static final String ACTION_ACCESS_AFTER_SEARCH = "access_after_search";

    public static final String LABEL_MOVE_TO = "move_to";
    public static final String LABEL_COPY_TO = "copy_to";
    public static final String LABEL_DELETE = "delete";
    public static final String LABEL_ADD_TO_FAVORITE = "add_to_favorite";
    public static final String LABEL_REMOVE_FROM_FAVORITE = "remove_from_favorite";
    public static final String LABEL_SHARE = "share";
    public static final String LABEL_COMPRESS = "compress";
    public static final String LABEL_RENAME = "rename";
    public static final String LABEL_INFORMATION = "information";
    public static final String LABEL_OPEN = "open";
    public static final String LABEL_CREATE_SHORTCUT = "create_shortcut";
    public static final String LABEL_GO_TO_FOLDER = "go_to_folder";

    private static GaSearchFile mInstance;

    public static GaSearchFile getInstance() {
        if (mInstance == null) {
            mInstance = new GaSearchFile();
        }

        return mInstance;
    }

    private GaSearchFile() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, CATEGORY_NAME, action, label, value);
    }
}
