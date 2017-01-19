package com.asus.filetransfer.utility;

import android.content.Context;

import com.asus.filemanager.ga.GoogleAnalyticsBase;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;

/**
 * Created by Yenju_Lai on 2016/1/28.
 */
public class GaFileTransfer extends GoogleAnalyticsBase implements IGoogleAnalytics {

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-20";
    private static final float DEFAULT_SAMPLE_RATE = 100.0f;

    private static final String KEY_ID = "GA_FILE_TRANSFER_ID";
    private static final String KEY_ENABLE_TRACKING = "GA_FILE_TRANSFER_TRACKING";
    private static final String KEY_SAMPLE_RATE = "GA_FILE_TRANSFER_SAMPLE_RATE";

    private Context context;

    protected GaFileTransfer(Context context) {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, DEFAULT_SAMPLE_RATE);
        this.context = context;
    }

    @Override
    public void sendEvents(String category, String action, String label, Long value) {
        if (AnalyticsReflectionUtility.getEnableAsusAnalytics(context))
            super.sendEvents(context, category, action, label, value);
    }

    @Override
    public void sendTiming(String category, String variable, String label, Long value) {
        if (AnalyticsReflectionUtility.getEnableAsusAnalytics(context))
            super.sendTiming(context, category, variable, label, value);
    }
}
