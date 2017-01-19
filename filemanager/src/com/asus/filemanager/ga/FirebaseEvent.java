package com.asus.filemanager.ga;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.measurement.AppMeasurement;

import java.util.HashMap;

/**
 * Created by Tim_Lin on 2016/5/10.
 */
public class FirebaseEvent {
    public static final String DATASDK_EVENT_BOOT = "DATASDK_EVENT_BOOT";
    public static final String DATASDK_EVENT_BOOT_PARAM_VERSION = "Version";
    public static final String DATASDK_EVENT_BOOT_PARAM_FINGERPRINT = "Fingerprint";
    public static final String DATASDK_EVENT_BOOT_PARAM_REASON = "Reason";
    public static final String DATASDK_EVENT_BOOT_PARAM_BOOTTIME = "BootTime";
    public static final String DATASDK_EVENT_BOOT_PARAM_ISPOWERON = "IsPowerOn";
    public static final String DATASDK_EVENT_BOOT_VALUE_POWERON = "1";
    public static final String DATASDK_EVENT_BOOT_VALUE_POWEROFF = "0";

    private AppMeasurement mAppMeasurement;

    public void sendFireBaseEvent(Context context, String eventName, HashMap<String, String> param){
        if(mAppMeasurement == null) mAppMeasurement = AppMeasurement.getInstance(context);
        if(mAppMeasurement == null) return; // prevent still get null instance
        Bundle bundle = new Bundle();
        for(String key : param.keySet()){
            String value = param.get(key);
            if(value != null) bundle.putString(key, value);
        }

        if(eventName != null) mAppMeasurement.logEvent(eventName.replace(" ", "_"), bundle);
    }

    public void sendFireBaseEvent(Context context, String category, String action, String label) {
        sendFireBaseEvent(context, category, action, label, null);
    }

    public void sendFireBaseEvent(Context context, String category, String action, String label, Long value) {
        if(mAppMeasurement == null) mAppMeasurement = AppMeasurement.getInstance(context);
        if(mAppMeasurement == null) return; // prevent still get null instance
        Bundle bundle = new Bundle();
        if(action != null) bundle.putString("action", action);
        if(label != null) bundle.putString("label", label);
        if(value != null) bundle.putString("value", value.toString());
        if(category != null) mAppMeasurement.logEvent(category.replace(" ", "_"), bundle); // event name must consist of letters, digits or _ (underscores)
    }

}
