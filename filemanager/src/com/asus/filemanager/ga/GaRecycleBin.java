package com.asus.filemanager.ga;

import android.content.Context;

import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;

/**
 * Created by Yenju_Lai on 2016/1/28.
 */
public class GaRecycleBin extends GoogleAnalyticsBase {

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-23";
    private static final float DEFAULT_SAMPLE_RATE = 10.0f;

    private static final String KEY_ID = "GA_RECYCLE_BIN_ID";
    private static final String KEY_ENABLE_TRACKING = "GA_RECYCLE_BIN_TRACKING";
    private static final String KEY_SAMPLE_RATE = "GA_RECYCLE_BIN_SAMPLE_RATE";

    private static final String GA_CATEGORY_ACTION = "Action";
    private static final String GA_CATEGORY_RECYCLE_BIN = "RecycleBin";
    private static final String GA_CATEGORY_PERMANENTLY_DELETE = "PermanentlyDelete";
    private static GaRecycleBin mInstance;

    public enum DeleteCategory {
        RecycleBin,
        PermanentlyDelete,
        InsufficientStorage
    }

    public enum Action {
        Delete,
        Restore,
        Information
    }

    public static GaRecycleBin getInstance() {
        if (mInstance == null) {
            mInstance = new GaRecycleBin();
        }
        return mInstance;
    }

    private GaRecycleBin() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, DEFAULT_SAMPLE_RATE);
    }

    public void sendDeleteEvent(Context context, EditorUtility.RequestFrom source, int fileCount, DeleteCategory deleteCategory) {
        if (AnalyticsReflectionUtility.getEnableAsusAnalytics(context))
            super.sendEvents(context, deleteCategory.name()
                    , source.name(), null, (long)fileCount);
    }

    public void sendAction(Context context, Action action, int fileCount) {
        if (AnalyticsReflectionUtility.getEnableAsusAnalytics(context))
            super.sendEvents(context, GA_CATEGORY_ACTION,
                    action.name(), null, (long)fileCount);
    }
}
