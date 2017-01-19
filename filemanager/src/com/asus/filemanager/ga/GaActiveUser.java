package com.asus.filemanager.ga;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class GaActiveUser extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "active_user";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-14";
    public static final String KEY_ID = "GA_ACTIVE_USER_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_ACTIVE_USER_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_ACTIVE_USER_SAMPLE_RATE";

    public static final String ACTION_ON_START_ACTIVITY = "on_start_activity";

    private static GaActiveUser mInstance;
    private static GaNonAsusActiveUser mNonAsusInstance;
    private static boolean mIsAsusDevice;

    private Tracker mOriginalTracker; // Tracker for original track id

    public static GaActiveUser getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new GaActiveUser();
            mIsAsusDevice = isSystemApp(context);
        }

        if (!isAsusDevice() && mNonAsusInstance == null) {
            mNonAsusInstance = GaNonAsusActiveUser.getInstance();
        }

        return mInstance;
    }

    private GaActiveUser() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 5.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
        if (mNonAsusInstance != null) {
            mNonAsusInstance.sendEvents(context, GaNonAsusActiveUser.CATEGORY_NAME,
                    GaNonAsusActiveUser.ACTION_ON_START_ACTIVITY, Build.BRAND, value);
        }
        sendEventsToOriginalTracker(context, category, action, label, value);
    }

    /*
     * Experiment for tracking active user with old track id.
     * We can compare user and new user with the same app version
     * to verify sample rate mechanism.
     */
    private void sendEventsToOriginalTracker(Context context, String category,
            String action, String label, Long value) {

        if (value == null) {
            value = Long.valueOf(0);
        }

        if (category == null) {
            return;
        }

        Tracker tracker = getOriginalInstanceTracker(context);
        if (tracker != null) {
            tracker.send(new HitBuilders.EventBuilder()
            .setCategory(category)
            .setAction(action)
            .setLabel(label)
            .setValue(value)
            .build());
        }
    }

    private Tracker getOriginalInstanceTracker(Context context) {
        boolean isUser = Build.TYPE.equals("user");
        if (!isUser) {
            return null;
        }

        if (mOriginalTracker == null) {
            mOriginalTracker = GoogleAnalytics.getInstance(context).newTracker("UA-56127731-2");
        }
        return mOriginalTracker;
    }

    private static boolean isAsusDevice() {
        /*
         * Avoid old asus device may not set Build.BRAND
         */
        return Build.BRAND.equalsIgnoreCase("asus") || mIsAsusDevice;
    }

    private static boolean isSystemApp(Context context) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo("com.asus.filemanager", 0);
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            } else {
                return false;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
