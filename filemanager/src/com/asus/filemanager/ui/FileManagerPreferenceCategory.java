package com.asus.filemanager.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.utility.ThemeUtility;

import static com.asus.filemanager.utility.ThemeUtility.getThemeType;
import static com.asus.filemanager.utility.ThemeUtility.THEME;

/**
 * Created by hsuan-yulin on 2016/10/25.
 */

public class FileManagerPreferenceCategory extends PreferenceCategory {
    public FileManagerPreferenceCategory(Context context) {
        super(context);
    }

    public FileManagerPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FileManagerPreferenceCategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setBackground(new ColorDrawable(getThemeType() == THEME.DARK ? Color.BLACK : Color.WHITE));
        TextView title = (TextView) view.findViewById(android.R.id.title);
        title.setTextColor(Color.GRAY);
    }
}
