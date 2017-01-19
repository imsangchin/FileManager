package com.asus.filemanager.ga;

import android.content.Context;

public class GaPromote extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "promote";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-17";
    public static final String KEY_ID = "GA_PROMOTE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_PROMOTE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_PROMOTE_SAMPLE_RATE";

    public static final String PROMOTE_LOW_STORAGE = "promote_low_storage";
    public static final String PROMOTE_REDEEM_GDRIVE= "promote_redeem_gdrive";
    public static final String PROMOTE_CLICK_ACTION = "promote_click_action";

    public static final String PROMOTE_AD_MOVE_TO_NON_ASUS = "promote_ad_move_to_non_asus";
    public static final String PROMOTE_AD_DLMGR_NON_ASUS   = "promote_ad_dlmgr_non_asus";
    public static final String PROMOTE_AD_MOVE_TO_ASUS = "promote_ad_move_to_asus";
    public static final String PROMOTE_AD_DLMGR_ASUS   = "promote_ad_dlmgr_asus";
    public static final String PROMOTE_AD_VIEW= "promote_adview_action";

    public static final String PROMOTE_LARGEFILE_NOTIFICATION = "promote_largefile_notification";
    public static final String PROMOTE_RECENTFILE_NOTIFICATION = "promote_recentfile_notification";
    public static final String PROMOTE_USB_NOTIFICATION = "promote_usb_http_notification";

    public static final String PROMOTE_CM_LAUNCH = "promote_cm_launch";
    public static final String PROMOTE_CM_REDIRECT_TO_STORE = "promote_cm_redirect_to_store";

    public static final String PROMOTE_FROM_SHORTCUT = "promote_from_shortcut";

    public static final String PROMOTE_APP_RECOMMENDATION = "promote_app_recommendation";
    public static final String ACTION_CLICK_RECOMMEND_APP_IN_APK = "click_recommend_app_in_apk";
    public static final String ACTION_CLICK_RECOMMEND_APP_IN_GAME = "click_recommend_app_in_game";
    public static final String LABEL_CURATED_APP = "curated_app";
    public static final String LABEL_AD_NETWORK_APP = "ad_network_app";

    private static GaPromote mInstance;

    public static GaPromote getInstance() {
        if (mInstance == null) {
            mInstance = new GaPromote();
        }

        return mInstance;
    }

    private GaPromote() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }
}
