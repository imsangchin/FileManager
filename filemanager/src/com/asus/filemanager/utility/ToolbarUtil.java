package com.asus.filemanager.utility;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Visibility;
import android.view.View;

import com.asus.filemanager.R;

public class ToolbarUtil {
	public static void setupToolbar(AppCompatActivity activity) {
    	// Notes:
    	// appcompatactivity content view need to contains: 
    	// <include layout="@layout/toolbar" />
    	Toolbar toolbar = (Toolbar)activity.findViewById(R.id.toolbar);
		if(toolbar == null) return;
		toolbar.setVisibility(View.VISIBLE);
    	activity.setSupportActionBar(toolbar);
    }
}
