package com.asus.filemanager.activity;

import java.security.interfaces.RSAKey;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ConstantsUtil;

import android.R.integer;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.asus.filemanager.utility.ColorfulLinearLayout;


public class FileManagerEULATemplActivity extends BaseActivity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String layoutContent = "";
		String layoutTitle = "";
		
		Intent myIntent = getIntent();
		int infoType = myIntent.getIntExtra("infoType", ConstantsUtil.TEMPL_OPENSOURCE);
		switch(infoType){
		case ConstantsUtil.TEMPL_USERAGREEMENT:
			layoutContent = getResources().getString(R.string.eula_content_description);
			layoutTitle = getResources().getString(R.string.eula_content_title);
			break;
		case ConstantsUtil.TEMPL_OPENSOURCE:
			layoutContent = getResources().getString(R.string.attribution_notice_content);
			layoutTitle = getResources().getString(R.string.attribution_notice);
			break;
		default:
			layoutContent = getResources().getString(R.string.attribution_notice_content);
			layoutTitle = getResources().getString(R.string.attribution_notice);
		}
		
		initLayout(layoutTitle, layoutContent, infoType);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }

        return false;
    }
	
	private void initLayout(String title, String content, int infoType){
		LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.license_dialog, null);
		TextView textView = (TextView)layout.findViewById(R.id.wvAttributionNotice);
		
		textView.setText("\n" + title + "\n\n" + content);
		
		textView.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

        if (isColorfulTextViewNeeded()){
            ColorfulLinearLayout colorfulLinearLayout= new ColorfulLinearLayout(this);
            colorfulLinearLayout.setOrientation(LinearLayout.VERTICAL);
            colorfulLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            TextView textViewColorful = new TextView(this);
            colorfulLinearLayout.removeAllViews();
            textViewColorful = new TextView(this);
            int statusH = getStatusBarHeight(this);
            int actionbarH = getActionBarHeight(this);
            textViewColorful.setHeight(statusH + actionbarH);
            textViewColorful.setBackgroundColor(this.getResources().getColor(R.color.theme_color));
            colorfulLinearLayout.addView(textViewColorful);
            colorfulLinearLayout.addView(layout);
            setContentView(colorfulLinearLayout);
		}else{
			setContentView(layout);
		}

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
            actionBar.setDisplayShowTitleEnabled(true);
            if (infoType == ConstantsUtil.TEMPL_USERAGREEMENT){
                actionBar.setTitle(this.getResources().getString(R.string.eula_title));
            }else{
                actionBar.setTitle(this.getResources().getString(R.string.open_source_license_title));
            }
            actionBar.setIcon(android.R.color.transparent);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
	}
    private static int getStatusBarHeight(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        if (display != null
            && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return 0;
        }
        // ignore status bar height for api level <= 18
        if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
            return 0;
        }
        int h = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height",
            "dimen", "android");
        if (resourceId > 0) {
            h = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return h;
    }

    private static int getActionBarHeight(Activity activity) {
        int h = 0;
        TypedValue tv = new TypedValue();
        activity.getBaseContext().getTheme().resolveAttribute(
            android.R.attr.actionBarSize, tv, true);
        h = activity.getResources().getDimensionPixelSize(tv.resourceId);
        return h;
    }

    private static boolean isColorfulTextViewNeeded() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP ? true
            : false;
    }
}
