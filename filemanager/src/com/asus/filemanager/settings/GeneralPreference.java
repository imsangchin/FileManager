package com.asus.filemanager.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asus.filemanager.R;

/**
 * GeneralPreference is the general setting page.
 * We can get the preference value by using the static field "KEY_XXXXXX".
 *
 * @see SettingActivity
 * @author jason_uang
 * @version 1.0
 */
public class GeneralPreference extends PreferenceFragment
        implements OnSharedPreferenceChangeListener {

    // Preference keys
    public static final String KEY_SPACE_USED = "preferences_space_used";
    public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";
    public static final String KEY_HIDE_SYSTEM_FILES = "preferences_hide_system_files";
    public static final String KEY_TRASH_CAN = "preferences_trash_can";

    private CheckBoxPreference mHideSystemFiles;
    private Preference mClearSearchHistory;
    private Preference mClearTrashCan;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.general_preferences);

        mHideSystemFiles = (CheckBoxPreference) findPreference(KEY_HIDE_SYSTEM_FILES);
        mClearSearchHistory = (Preference) findPreference(KEY_CLEAR_SEARCH_HISTORY);
        mClearTrashCan = (Preference) findPreference(KEY_TRASH_CAN);
    }

    /** Set the default shared preferences in the proper context */
    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.general_preferences, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Log.v("Johnson", "onSharedPreferenceChanged key: " + key);
        Preference pref = findPreference(key);
        if (pref.equals(mHideSystemFiles)) {
            if (mHideSystemFiles.isChecked()) {
                mHideSystemFiles.setChecked(true);
            } else {
                mHideSystemFiles.setChecked(false);
            }
        }
    }
}
