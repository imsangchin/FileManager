package com.asus.filemanager.activity;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import com.asus.filemanager.utility.AnalyticsReflectionUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by ChenHsin_Hsieh on 2016/5/30.
 */
public class AnalyticsObserver extends ContentObserver {
    public static final String TAG = "AnalyticsObserver";

    public AnalyticsObserver(Handler handler) {
        super(handler);
    }

    public void onChange(Context context,boolean selfChange) {
        // enable/disable Google Analytics according to global option in Settings->Legal->InspireASUS
        boolean isEnable = AnalyticsReflectionUtility
                .getEnableAsusAnalytics(context);
        if(WrapEnvironment.IS_VERIZON || ItemOperationUtility.getInstance().enableCtaCheck()){
            isEnable = false;
        }
        GoogleAnalytics.getInstance(context).setAppOptOut(!isEnable);
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(isEnable);
        Log.d(TAG, "Set GA enable: " + isEnable);
    }
}
