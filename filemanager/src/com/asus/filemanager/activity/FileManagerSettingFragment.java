package com.asus.filemanager.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.asus.filemanager.R;
import com.asus.filemanager.ga.GaSettingsPage;
import com.asus.filemanager.ui.DarkThemeToast;
import com.asus.filemanager.utility.ThemeUtility.THEME;
import com.asus.filemanager.utility.ViewUtility;

import static com.asus.filemanager.utility.ThemeUtility.getThemeType;
import static com.asus.filemanager.utility.ThemeUtility.setThemeType;

public class FileManagerSettingFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "FileManagerSettingFragment";
    private static final String PREF_NAME = "MyPrefsFile";
    private static final String PREF_KEY_SHOW_HIDDEN = "mShowHidden";
    public static final String PREF_KEY_HIDE_FILES = "pref_hide_system_files";
    public static final String PREF_KEY_LARGE_FILES = "pref_large_files_notification";
    public static final String PREF_KEY_RECENT_FILES = "pref_recent_files_notification";
    public static final String PREF_ENABLE_DARKMODE = "pref_enable_darktheme";
    public static final String PREF_NEWFEATURE_DARKMODE = "pref_newfewature_darkmode";

    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting_preferences);
        mSharedPreferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if(getThemeType() == THEME.DARK) {
            view.setBackgroundColor(getResources().getColor(android.R.color.black));
        }

        // check need to show new icon in items
        if(!getPreferenceScreen().getSharedPreferences().contains(PREF_NEWFEATURE_DARKMODE)) {
            addItemNewIcon(PREF_ENABLE_DARKMODE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        restoreSettingValues();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        storeSettingValues();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_KEY_HIDE_FILES)) {
            GaSettingsPage.getInstance().sendPreferenceSwitchChange(
                    getActivity(), GaSettingsPage.ACTION_HIDE_SYSTEM_FILES, sharedPreferences.getBoolean(key, false));
        } else if (key.equals(PREF_KEY_LARGE_FILES)) {
            GaSettingsPage.getInstance().sendPreferenceSwitchChange(
                    getActivity(), GaSettingsPage.ACTION_LARGE_FILES_NOTIFICATION, sharedPreferences.getBoolean(key, false));
        } else if (key.equals(PREF_KEY_RECENT_FILES)) {
            GaSettingsPage.getInstance().sendPreferenceSwitchChange(
                    getActivity(), GaSettingsPage.ACTION_RECENT_FILES_NOTIFICATION, sharedPreferences.getBoolean(key, false));
        } else if(key.equals(PREF_ENABLE_DARKMODE)) {
            GaSettingsPage.getInstance().sendPreferenceSwitchChange(
                    getActivity(), GaSettingsPage.ACTION_USE_DARK_THEME, sharedPreferences.getBoolean(key, false));

            if(!sharedPreferences.contains(PREF_NEWFEATURE_DARKMODE)) {
                // remove promote dark theme toast when user doesn't click toast and change theme directly from settings page
                DarkThemeToast.neverShowToastAgain(getActivity());
                // disable new feature icon in settings page
                sharedPreferences.edit().putBoolean(PREF_NEWFEATURE_DARKMODE, false).commit();
            }

            // leave settings page to show new theme on home page directly
            getActivity().finish();
        }
    }

    private void restoreSettingValues () {
            PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_preferences, false);
        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();

        Boolean showHidden = mSharedPreferences.getBoolean(PREF_KEY_SHOW_HIDDEN, false);
        editor.putBoolean(PREF_KEY_HIDE_FILES, !showHidden).commit();
    }

    private void storeSettingValues () {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Boolean hideFiles = getPreferenceScreen().getSharedPreferences().getBoolean(PREF_KEY_HIDE_FILES, true);
        editor.putBoolean(PREF_KEY_SHOW_HIDDEN, !hideFiles).commit();

        // set theme type
        boolean isDarkTheme = getPreferenceScreen().getSharedPreferences().getBoolean(PREF_ENABLE_DARKMODE, false);
        if(isDarkTheme) {
            setThemeType(THEME.DARK);
        } else {
            setThemeType(THEME.DEFAULT);
        }
    }

    private void addItemNewIcon(String preferenceKey) {
        Preference currentPref = findPreference(preferenceKey);
        SpannableStringBuilder s = new SpannableStringBuilder(currentPref.getTitle()+"  ");
        s = ViewUtility.addNewIcon(getActivity().getApplicationContext(), s);
        currentPref.setTitle(s);
    }
}
