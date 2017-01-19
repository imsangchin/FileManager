package com.asus.filemanager.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

/**
 * Created by Tim_Lin on 2016/9/1.
 */

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTheme();
    }

    private void initTheme(){
        boolean mIsTranslucentEnabled = (getResources().getIdentifier("windowTranslucentStatus", "attr", "android") != 0);
        if (!mIsTranslucentEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                getTheme().applyStyle(R.style.Theme_FileManagerActivity_DarkTheme_UnSupportTranslucent, true);
            } else {
                //+++ tsungching_lin@asus.com modify for SearchView history UI problem when run on the JELLY_BEAN(v16)
                getTheme().applyStyle(R.style.Theme_FileManagerActivity_UnSupportTranslucent_V16, true);
                //--- tsungching_lin@asus.com
            }
        }
        ThemeUtility.setTheme(this);
    }
}
