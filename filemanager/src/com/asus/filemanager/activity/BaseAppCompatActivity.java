package com.asus.filemanager.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToolbarUtil;

/**
 * Created by Tim_Lin on 2016/9/1.
 */

public class BaseAppCompatActivity extends AppCompatActivity {

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

        ThemeUtility.setAppCompatTheme(this);
    }
}
