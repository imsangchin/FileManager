package com.asus.filemanager.ga;

import com.asus.filemanager.utility.VFile.VFileType;

import android.content.Context;

public class GaAccessFile extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "access_file";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-18";

    public static final String KEY_ID = "GA_ACCESS_FILE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_ACCESS_FILE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_ACCESS_FILE_SAMPLE_RATE";

    public static final String ACTION_MOVE_TO = "move_to";
    public static final String ACTION_COPY_TO = "copy_to";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_ADD_TO_FAVORITE = "add_to_favorite";
    public static final String ACTION_REMOVE_FROM_FAVORITE = "remove_from_favorite";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_COMPRESS = "compress";
    public static final String ACTION_RENAME = "rename";
    public static final String ACTION_INFORMATION = "information";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_CREATE_SHORTCUT = "create_shortcut";
    public static final String ACTION_MOVE_TO_HIDDEN_CABINET = "move_to_hidden_cabinet";

    public static final String LABEL_FROM_MENU = "from_menu";
    public static final String LABEL_FROM_FAVORITE_PAGE = "from_favorite_page";

    private static GaAccessFile mInstance;

    public static GaAccessFile getInstance() {
        if (mInstance == null) {
            mInstance = new GaAccessFile();
        }

        return mInstance;
    }

    private GaAccessFile() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 10.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }

    public void sendEvents(Context context, String action, int fromPlace, int toPlace, int numFiles) {

        String source = getVFileTypeName(fromPlace);
        String target = getVFileTypeName(toPlace);
        String label = numFiles > 1 ? "multiple" : "single";
        StringBuilder sb = new StringBuilder(label);

        if (source != null) {
            sb.append(String.format("_from_%s", source));
        }

        if (target != null) {
            sb.append(String.format("_to_%s", target));
        }

        sendEvents(context, CATEGORY_NAME, action, label, Long.valueOf(numFiles));
    }

    private String getVFileTypeName(int type) {

        String result = null;

        switch (type) {
        case VFileType.TYPE_LOCAL_STORAGE:
            result = "local";
            break;
        case VFileType.TYPE_CLOUD_STORAGE:
            result = "cloud";
            break;
        case VFileType.TYPE_SAMBA_STORAGE:
            result = "samba";
            break;
        default:
            result = null;
            break;
        }

        return result;
    }
}
