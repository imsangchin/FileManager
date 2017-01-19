package com.asus.filemanager.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatImageView;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerSettingFragment;

public class ThemeUtility {
    public static int sThemeAsusLightId;
    public static int sThemeAsusDialogId;
    public static int sThemeAsusDialogAlertId;
    public static int sThemeAsusNoActionBarId;

    public static int sThemeAsusLightDialogAlertId;

    private static final String THEME_ASUS_LIGHT = "android:style/Theme.DeviceDefault.Light";
    private static final String THEME_ASUS_DIALOG = "android:style/Theme.DeviceDefault.Dialog";
    private static final String THEME_ASUS_DIALOG_ALERT = "android:style/Theme.DeviceDefault.Dialog.Alert";
    private static final String THEME_ASUS_LIGHT_DIALOG_ALERT = "android:style/Theme.DeviceDefault.Light.Dialog.Alert";
    private static final String THEME_ASUS_DARK_DIALOG_ALERT = "android:style/ThemeDarkAlertDialog";
    private static final String THEME_ASUS_NO_ACTION_BAR = "android:style/Theme.DeviceDefault.NoActionBar";

    private static final String PREF_NAME = "MyPrefsFile";

    public enum THEME {
        DEFAULT,
        DARK
    }

    private static THEME themeType = THEME.DEFAULT;

    public static void initThemeType(Context context) {
        // get theme type when run app
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkTheme = sharedPreferences.getBoolean(FileManagerSettingFragment.PREF_ENABLE_DARKMODE, false);
        themeType = isDarkTheme ? THEME.DARK : THEME.DEFAULT;
    }

    public static THEME getThemeType() {
        return themeType;
    }

    public static void setThemeType(THEME type) {
        themeType = type;
    }

    public static void retrieveAsusThemeId(Context context) {
        sThemeAsusLightId = context.getResources().getIdentifier(THEME_ASUS_LIGHT, null, null);
        sThemeAsusDialogId = context.getResources().getIdentifier(THEME_ASUS_DIALOG, null, null);
        sThemeAsusDialogAlertId = context.getResources().getIdentifier(THEME_ASUS_DIALOG_ALERT, null, null);
        sThemeAsusLightDialogAlertId = context.getResources().getIdentifier(THEME_ASUS_LIGHT_DIALOG_ALERT, null, null);
        sThemeAsusNoActionBarId = context.getResources().getIdentifier(THEME_ASUS_NO_ACTION_BAR, null, null);
    }

    public static void setAppCompatTheme(Activity activity) {
        final Context contextThemeWrapper;
        switch (themeType) {
            case DARK:
                activity.setTheme(R.style.Theme_Dark);
                activity.setTheme(R.style.ThemeAppCompatDarkNoActionBar);
                // set inflater for future fragement used
                contextThemeWrapper = new ContextThemeWrapper(activity, R.style.Theme_Dark);
                activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
                break;
            default:
                activity.setTheme(R.style.Theme_Default);
                activity.setTheme(R.style.ThemeAppCompatLightNoActionBar);
                contextThemeWrapper = new ContextThemeWrapper(activity, R.style.Theme_Default);
                activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
                break;
        }
    }

    public static void setTheme(Activity activity) {
        final Context contextThemeWrapper;
        switch (themeType) {
            case DARK:
                activity.setTheme(R.style.Theme_Dark);
                activity.setTheme(R.style.Theme_Dark_BaseActivity);
                // set inflater for future fragement used
                contextThemeWrapper = new ContextThemeWrapper(activity, R.style.Theme_Dark);
                activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
                break;
            default:
                activity.setTheme(R.style.Theme_Default);
                activity.setTheme(R.style.Theme_BaseActivity);
                contextThemeWrapper = new ContextThemeWrapper(activity, R.style.Theme_Default);
                activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
                break;
        }
    }

    public static void setItemIconColor(Context context, Drawable drawable) {
        if(drawable == null) return;
        switch (themeType) {
            case DARK:
                drawable.mutate();
                drawable.setColorFilter(context.getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                break;
        }
    }

    public static void setMenuIconColor(Context context, Menu menu) {
        switch (themeType) {
            case DARK:
                for(int i = 0; i < menu.size(); i++) {
                    Drawable drawable = menu.getItem(i).getIcon();
                    if(drawable == null) continue;
                    drawable.mutate();
                    drawable.setColorFilter(context.getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                }
                break;
        }
    }


    public static void setThemeOverflowButton(final Activity activity, final boolean hasNewN) {
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if(decorView == null) return;
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        if(viewTreeObserver == null) return;
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int resId = themeType == THEME.DARK ? R.drawable.asus_ic_menu_light :
                        R.drawable.asus_ic_menu_grey;
                AppCompatImageView overflow=(AppCompatImageView)decorView.findViewById(R.id.overflow_menu_id);
                if (overflow == null) {
                    return;
                }
                if (hasNewN) {
                    Bitmap resBmp = ViewUtility.combineImages(
                            BitmapFactory.decodeResource(activity.getResources(), resId),
                            BitmapFactory.decodeResource(activity.getResources(), R.drawable.asus_new_feature_icon)
                    );
                    overflow.setImageBitmap(resBmp);
                } else {
                    overflow.setImageResource(resId);
                }
                ViewUtility.removeOnGlobalLayoutListener(decorView,this);
            }
        });

    }

    public static int getAsusAlertDialogThemeId() {
        return themeType == THEME.DARK ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT;
    }

    public static int getThemeAsusLightDialogAlertId() {
        return sThemeAsusLightDialogAlertId;
    }

    public static boolean isNeedToSwitchTheme(Activity activity) {
        TypedValue outValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.theme_name, outValue, true);

        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK && !"dark".equals(outValue.string)) {
            // restart activity
            return true;
        } else if(!(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) && !"default".equals(outValue.string)) {
            return true;
        }

        return false;
    }

    public static int getItemSelectedBackgroundColor() {
        switch (themeType) {
            case DARK:
                return R.color.dark_theme_item_select_background_color;
            default:
                return R.color.select_gray;
        }
    }

    public static void colorDrawableByTheme(Context context, Drawable drawable) {
        if(drawable == null) return;
        int colorId;
        switch (themeType) {
            case DARK:
                colorId = R.color.white;
                break;
            default:
                colorId = R.color.grey;
        }
        drawable.mutate();
        drawable.setColorFilter(context.getResources().getColor(colorId), PorterDuff.Mode.SRC_ATOP);
    }
}
