package com.asus.filemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerSettingActivity;

/* Toast dismisses on (release toast, listener)
1. cancel button clicked
2. toast message clicked
3. dark theme setting switch clicked
 */
public class DarkThemeToast {

    private static final String TAG = "DarkThemeToast";
    private static final String kKeyNeedShowToast = "key_show_toast";

    private Activity mActivity = null;
    private ActivityToast mToast = null;

    public DarkThemeToast (Activity activity) {
        mActivity = activity;
        mToast = null;

        if (needShowToast(mActivity)) {
            mToast = PromoteUtils.showPromoteToastWithTargetTextUnderline(mActivity,
                activity.getString(R.string.promote_darkmode_title),
                activity.getString(R.string.promote_darkmode_message, activity.getString(R.string.action_settings)),
                activity.getString(R.string.action_settings),
                R.drawable.asus_ic_tip,
                new PromoteUtils.OnPromoteClickListener() {
                    @Override
                    public void onClick() {
                        mActivity.startActivity(new Intent(mActivity, FileManagerSettingActivity.class));
                        neverShowToastAgain(mActivity);
                    }

                    @Override
                    public void onCancel() {
                        neverShowToastAgain(mActivity);
                    }
                }, "Dark Mode"
                //AsusTracker.TrackerEvents.Action_DarkTheme
            );
        }
    }

    public void cancel() {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }

    public static void neverShowToastAgain(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        sp.edit().putBoolean(kKeyNeedShowToast, false).commit();
    }

    private static boolean needShowToast(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        return sp.getBoolean(kKeyNeedShowToast, true);
    }
}
