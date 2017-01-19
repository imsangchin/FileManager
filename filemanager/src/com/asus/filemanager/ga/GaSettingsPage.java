package com.asus.filemanager.ga;

import com.asus.filemanager.activity.FileManagerSettingFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GaSettingsPage extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "settings_page";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-22";
    public static final String KEY_ID = "GA_SETTINGS_PAGE_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_SETTINGS_PAGE_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_SETTINGS_PAGE_SAMPLE_RATE";

    public static final String ACTION_HIDE_SYSTEM_FILES = "hide_system_files";
    public static final String ACTION_LARGE_FILES_NOTIFICATION = "large_files_notification";
    public static final String ACTION_RECENT_FILES_NOTIFICATION = "recent_files_notification";
    public static final String ACTION_USE_DARK_THEME = "use_dark_theme";

    public static final String LABEL_ON = "on";
    public static final String LABEL_OFF = "off";

    private static final String KEY_GA_HAS_SEND_DEFAULT = "ga_has_send_default_settings_page";
    private static final String KEY_GA_HAS_SEND_DEFAULT_DARK_THEME = "ga_has_send_default_dark_theme";

    private static GaSettingsPage mInstance;

    public static GaSettingsPage getInstance() {
        if (mInstance == null) {
            mInstance = new GaSettingsPage();
        }

        return mInstance;
    }

    private GaSettingsPage() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    protected void sendEvents(Context context, String category, String action, String label, Long value) {
        super.sendEvents(context, category, action, label, value);
    }

    public void sendPreferenceSwitchDefault(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        // only send once
        if (!sp.getBoolean(KEY_GA_HAS_SEND_DEFAULT, false)) {
            boolean isHideSystemFileChecked = sp.getBoolean(
                    FileManagerSettingFragment.PREF_KEY_HIDE_FILES, true);
            sendEvents(context, CATEGORY_NAME, ACTION_HIDE_SYSTEM_FILES,
                    isHideSystemFileChecked ? LABEL_ON : LABEL_OFF, Long.valueOf(1));

            boolean isAcceptLargeFilesNotification = sp.getBoolean(
                    FileManagerSettingFragment.PREF_KEY_LARGE_FILES, false);
            sendEvents(context, CATEGORY_NAME, ACTION_LARGE_FILES_NOTIFICATION,
                    isAcceptLargeFilesNotification ? LABEL_ON : LABEL_OFF, Long.valueOf(1));

            boolean isAcceptRecentFilesNotification = sp.getBoolean(
                    FileManagerSettingFragment.PREF_KEY_RECENT_FILES, false);
            sendEvents(context, CATEGORY_NAME, ACTION_RECENT_FILES_NOTIFICATION,
                    isAcceptRecentFilesNotification ? LABEL_ON : LABEL_OFF, Long.valueOf(1));

            sp.edit().putBoolean(KEY_GA_HAS_SEND_DEFAULT, true).commit();
        }

        if (!sp.getBoolean(KEY_GA_HAS_SEND_DEFAULT_DARK_THEME, false)) {
            boolean isDarkThemeEnabled = sp.getBoolean(
                    FileManagerSettingFragment.PREF_ENABLE_DARKMODE, false);
            sendEvents(context, CATEGORY_NAME, ACTION_USE_DARK_THEME,
                    isDarkThemeEnabled ? LABEL_ON : LABEL_OFF, Long.valueOf(1));

            sp.edit().putBoolean(KEY_GA_HAS_SEND_DEFAULT_DARK_THEME, true).commit();
        }
    }

    public void sendPreferenceSwitchChange(Context context, String action, boolean isOn) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sp.getBoolean(KEY_GA_HAS_SEND_DEFAULT, false)
                || !sp.getBoolean(KEY_GA_HAS_SEND_DEFAULT_DARK_THEME, false)) {
            return ;
        }
        sendEvents(context, CATEGORY_NAME, action, isOn ? LABEL_ON : LABEL_OFF, Long.valueOf(1));
        sendEvents(context, CATEGORY_NAME, action, isOn ? LABEL_OFF : LABEL_ON, Long.valueOf(-1));
    }
}
