package com.asus.filemanager.ga;

import android.content.Context;

public class GaBrowseFile extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "browse_file";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-5";
    public static final String KEY_ID = "GA_BROWSE_FILE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_BROWSE_FILE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_BROWSE_FILE_SAMPLE_RATE";

    public static final String ACTION_BROWSE_FROM_SHORTCUT = "browse_from_shortcut";
    public static final String ACTION_BROWSE_FROM_HOMEPAGE = "browse_from_homepage";
    public static final String ACTION_BROWSE_FROM_DRAWER = "browse_from_drawer";

    public static final String LABEL_IMAGE_SHORTCUT = "image";
    public static final String LABEL_VIDEO_SHORTCUT = "video";
    public static final String LABEL_MUSIC_SHORTCUT = "music";
    public static final String LABEL_DOWNLOAD_SHORTCUT = "download";
    public static final String LABEL_FAVORITE_SHORTCUT = "favorite";
    public static final String LABEL_APP_SHORTCUT = "app";
    public static final String LABEL_DOCUMENTS_SHORTCUT = "document";
    public static final String LABEL_COMPRESS_SHORTCUT = "compress";
    public static final String LABEL_RECENT_SHORTCUT = "recent";
    public static final String LABEL_LARGE_FILE_SHORTCUT = "large_file";
    public static final String LABEL_PDF_SHORTCUT = "pdf";
    public static final String LABEL_GAME_SHORTCUT = "game";

    public static final String LABEL_ASUS_WEBSTORAGE = "ASUS Webstorage";
    public static final String LABEL_ASUS_HOMECLOUD = "ASUS HomeCloud";
    public static final String LABEL_GOOGLE_DRIVE = "Drive";
    public static final String LABEL_ONEDRIVE = "OneDrive";
    public static final String LABEL_DROPBOX = "Dropbox";
    public static final String LABEL_YANDEX = "Yandex";
    public static final String LABEL_BAIDU = "Baidu";
    public static final String LABEL_NETWORK_PLACE = "Network place";

    private static GaBrowseFile mInstance;

    public static GaBrowseFile getInstance() {
        if (mInstance == null) {
            mInstance = new GaBrowseFile();
        }

        return mInstance;
    }

    private GaBrowseFile() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 10.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
