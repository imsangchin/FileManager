package com.asus.filemanager.ui;

import android.content.Context;
import android.graphics.Color;
import android.preference.CheckBoxPreference;
import android.preference.TwoStatePreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asus.filemanager.utility.ThemeUtility;

/**
 * Created by hsuan-yulin on 2016/10/21.
 */

public class FileManagerCheckBoxPreference extends CheckBoxPreference {
    public FileManagerCheckBoxPreference(Context context) {
        super(context);
    }

    public FileManagerCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FileManagerCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView title = (TextView) view.findViewById(android.R.id.title);
        title.setTextColor(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK ? Color.WHITE : Color.BLACK);
    }
}
