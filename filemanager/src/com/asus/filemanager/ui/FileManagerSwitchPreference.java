package com.asus.filemanager.ui;

import android.content.Context;
import android.graphics.Color;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.utility.ThemeUtility;

/**
 * Created by hsuan-yulin on 2016/10/25.
 */

public class FileManagerSwitchPreference extends SwitchPreference {
    public FileManagerSwitchPreference(Context context) {
        super(context);
    }

    public FileManagerSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FileManagerSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView title = (TextView) view.findViewById(android.R.id.title);
        title.setTextColor(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK ? Color.WHITE : Color.BLACK);
    }
}
