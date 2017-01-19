package com.asus.filemanager.ga;

import android.content.Context;

public class GaExperiment extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "experiment";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-16";
    public static final String KEY_ID = "GA_EXPERIMENT_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_EXPERIMENT_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_EXPERIMENT_SAMPLE_RATE";

    public static final String CATEGORY_SECONDARY_STORAGE  = "secondary_storage";

    private static GaExperiment mInstance;

    public static GaExperiment getInstance() {
        if (mInstance == null) {
            mInstance = new GaExperiment();
        }

        return mInstance;
    }

    private GaExperiment() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
