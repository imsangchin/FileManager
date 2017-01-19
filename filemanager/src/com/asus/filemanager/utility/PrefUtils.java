package com.asus.filemanager.utility;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class help to deal with {@link SharedPreferences}
 * related tasks.
 */
public class PrefUtils {

    /**
     * Desired preferences file name used in this application.
     */
    public static final String PREF_FILE_NAME = "MyPrefsFile";

    /**
     * Preferences name to get a value to indicate application is first
     * enter multi window mode or not.
     */
    public static final String PREF_FIRST_IN_MULTI_WINDOW_MODE = "FirstInMultiWindowMode";

    /**
     * Preferences name to get a value to indicate application needs to show
     * shortcut hint or not.
     */
    public static final String PREF_NEEDTOSHOW_SHORTCUTHINT = "NEEDTOSHOW_SHORTCUTHINT";

    /**
     * Preferences name to get a version code of what's new in the application.
     */
    public static final String PREF_VERSION_WHATS_NEW = "VERSION_WHATS_NEW";

    /**
     * get theme type
     */
    public static final String PREF_THEME = "PREF_THEME";

    /**
     * Get a {@link SharedPreferences} with name {@link PrefUtils#PREF_FILE_NAME}
     * which is used only in this application.
     *
     * @param context The application {@link Context}.
     *
     * @return The desired {@link SharedPreferences}.
     */
    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences(PREF_FILE_NAME, 0);
    }

    /**
     * Get a value from {@link SharedPreferences} which is kept track of application
     * is first enter multi window mode or not.
     *
     * @param context The application {@link Context}.
     *
     * @return True means application first enter multi window mode, false otherwise.
     */
    public static boolean getBooleanFirstEnterMultiWindowMode(Context context)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getBoolean(PREF_FIRST_IN_MULTI_WINDOW_MODE, true);
    }

    /**
     * Set a value into {@link SharedPreferences} which is kept track of application
     * is first enter multi window mode or not.
     *
     * @param context The application {@link Context}.
     * @param value The new value want to be stored in {@link SharedPreferences}.
     */
    public static void setBooleanFirstEnterMultiWindowMode(Context context, boolean value)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        preferences.edit().putBoolean(PREF_FIRST_IN_MULTI_WINDOW_MODE, value).apply();
    }

    /**
     * Get a value from {@link SharedPreferences} which is kept track of application
     * needs to show shortcut hint or not.
     *
     * @param context The application {@link Context}.
     *
     * @return True means needs to show shortcut hint, false otherwise.
     */
    public static boolean getBooleanNeedToShowShortcutHint(Context context)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getBoolean(PREF_NEEDTOSHOW_SHORTCUTHINT, true);
    }

    /**
     * Set a value into {@link SharedPreferences} which is kept track of application
     * needs to show shortcut hint or not.
     *
     * @param context The application {@link Context}.
     * @param value The new value want to be stored in {@link SharedPreferences}.
     */
    public static void setBooleanNeedToShowShortcutHint(Context context, boolean value)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        preferences.edit().putBoolean(PREF_NEEDTOSHOW_SHORTCUTHINT, value).apply();
    }

    /**
     * Get a value from {@link SharedPreferences} which is kept track of application
     * what's new version.
     *
     * @param context The application {@link Context}.
     *
     * @return The version code of what's new.
     */
    public static long getLongVersionWhatsNew(Context context)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getLong(PREF_VERSION_WHATS_NEW, 0);
    }

    /**
     * Set a value into {@link SharedPreferences} which is kept track of application
     * what's new version.
     *
     * @param context The application {@link Context}.
     * @param versionCode The new version code want to be stored in {@link SharedPreferences}.
     */
    public static void setLongVersionWhatsNew(Context context, long versionCode)
    {
        SharedPreferences preferences = getSharedPreferences(context);
        preferences.edit().putLong(PREF_VERSION_WHATS_NEW, versionCode).apply();
    }
}
