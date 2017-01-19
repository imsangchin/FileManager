package com.asus.filemanager.hiddenzone.activity;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseActivity;
import com.asus.filemanager.utility.ColorfulLinearLayout;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class HiddenZoneIntroActivity extends BaseActivity {
    private Context mContext;
    private static final String TAG = "HiddenZoneIntroActivity";

    private static final int REGISTER_PIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorfulLinearLayout.setContentView(this, R.layout.hidden_zone_introduce_page, R.color.theme_color);
        mContext = HiddenZoneIntroActivity.this;
        initActionBar();
        Button registerPin = (Button) findViewById(R.id.button_setup_password);
        registerPin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                startActivityForResult(new Intent(mContext, SetupPasswordActivity.class),
                        REGISTER_PIN);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
            break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: " + requestCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == REGISTER_PIN) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(
                R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
