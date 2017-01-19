package com.asus.filemanager.ga;
import android.content.Context;


/**
 * Created by ChenHsin_Hsieh on 2016/3/4.
 */
public class GaStorageAnalyzer extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "storage_analyzer";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-21";
    public static final String KEY_ID = "GA_STORAGE_ANALYZER_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_STORAGE_ANALYZER_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_STORAGE_ANALYZER_SAMPLE_RATE";

    public static final String ACTION_ANALYZER_PAGE = "storage_analyzer_page";
    public static final String ACTION_ANALYZER_PAGE_RECENTFILE_NOTIFICATION = "storage_analyzer_page_recentfile_notification";
    public static final String ACTION_ANALYZER_PAGE_LARGEFILE_NOTIFICATION = "storage_analyzer_page_largefile_notification";
    public static final String ACTION_DELETE = "delete_file";
    public static final String ACTION_ALL_FILES_INTERNAL_PAGE = "analyzer_all_files_internal_page";
    public static final String ACTION_ALL_FILES_EXTERNAL_PAGE = "analyzer_all_files_external_page";
    public static final String ACTION_ANALYZER_LARGE_FILE_PAGE = "analyzer_large_file_page";
    public static final String ACTION_ANALYZER_RECENT_FILE_PAGE = "analyzer_recent_file_page";
    public static final String ACTION_ANALYZER_DUP_FILE_PAGE = "analyzer_duplicate_file_page";
    public static final String ACTION_ANALYZER_REFRESH_ALL = "analyzer_refresh_all";
    public static final String ACTION_ANALYZER_RECYCLE_BIN_PAGE = "analyzer_recycle_bin_page";

    private static GaStorageAnalyzer mInstance;

    public static GaStorageAnalyzer getInstance() {
        if (mInstance == null) {
            mInstance = new GaStorageAnalyzer();
        }
        return mInstance;
    }

    private GaStorageAnalyzer() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
