package com.asus.filemanager.ga;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.asus.filemanager.utility.ConstantsUtil;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public abstract class GoogleAnalyticsBase {

    private static final String TAG = "GoogleAnalyticsBase";

    private static final String TRACKER_ID_DEBUG = "UA-56127731-1";
    private static final boolean isUser = Build.TYPE.equals("user");

    private static final boolean DEFAULT_ENABLE_TRACKING = true;

    private Tracker mTracker = null;
    private boolean mEnableTracking;
    private double mSampleRate;
    private String mTrackID;

    private String mKeyId;
    private String mKeyEnableTracking;
    private String mKeySampleRate;
    private String mDefaultTrackId;
    float mDefaultSampleRate;

    private FirebaseEvent firebaseEvent = new FirebaseEvent();

    private static final boolean DEBUG = ConstantsUtil.DEBUG;

    protected GoogleAnalyticsBase(String keyId, String keyEnableTracking,
            String keySampleRate, String defaultTrackId, float defaultSampleRate) {
        mKeyId = keyId;
        mKeyEnableTracking = keyEnableTracking;
        mKeySampleRate = keySampleRate;
        mDefaultTrackId = defaultTrackId;
        mDefaultSampleRate = defaultSampleRate;
    }

    protected void sendEvents(Context context, String category, String action, String label, Long value) {
        if (value == null) {
            value = Long.valueOf(0);
        }

        if (category == null) {
            Log.w(TAG, "cannot send this event because category == null");
            return;
        }

        Tracker tracker = getInstanceTracker(context);

        if (tracker == null) {
            Log.w(TAG, "cannot send this event because tracker == null");
            return;
        } else if (!mEnableTracking) {
            if (DEBUG) {
                Log.w(TAG, "do not send GA data because tracker is disabled");
            }
            return;
        } else {
            if (DEBUG) {
                Log.d(TAG, "sendEvents: " + category + ", " + action + ", " + label
                        + ", " + value + ", " + mSampleRate);
            }
        }

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());

        if (!isUser) {
            GoogleAnalytics.getInstance(context).dispatchLocalHits();
        }

        firebaseEvent.sendFireBaseEvent(context, category, action, label, value);
    }

    protected void sendTiming(Context context, String category, String variable, String label, Long value) {

        if (value == null) {
            value = Long.valueOf(0);
        }

        if (category == null) {
            Log.w(TAG, "cannot send this event because category == null");
            return;
        }

        Tracker tracker = getInstanceTracker(context);

        if (tracker == null) {
            Log.w(TAG, "cannot send this event because tracker == null");
            return;
        } else if (!mEnableTracking) {
            if (DEBUG) {
                Log.w(TAG, "do not send GA data because tracker is disabled");
            }
            return;
        } else {
            if (DEBUG) {
                Log.d(TAG, "sendEvents: " + category + ", " + variable + ", " + label
                        + ", " + value + ", " + mSampleRate);
            }
        }

        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setVariable(variable)
                .setLabel(label)
                .setValue(value)
                .build());

        if (!isUser) {
            GoogleAnalytics.getInstance(context).dispatchLocalHits();
        }
    }

    private Tracker getInstanceTracker(Context context) {
        if (mTracker == null) {

            SharedPreferences sp = context.getSharedPreferences("GaConfig", Context.MODE_PRIVATE);

            mTrackID = sp.getString(mKeyId, mDefaultTrackId);
            mEnableTracking = sp.getBoolean(mKeyEnableTracking, DEFAULT_ENABLE_TRACKING);
            mSampleRate = sp.getFloat(mKeySampleRate, mDefaultSampleRate);

            if(isUser) {
                mTracker = GoogleAnalytics.getInstance(context).newTracker(mTrackID);
                mTracker.setSampleRate(mSampleRate);
            }else {
                mTracker = GoogleAnalytics.getInstance(context).newTracker(TRACKER_ID_DEBUG);
            }
        }
        return mTracker;
    }
}
