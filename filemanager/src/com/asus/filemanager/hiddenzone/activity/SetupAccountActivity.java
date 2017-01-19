package com.asus.filemanager.hiddenzone.activity;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseActivity;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.google.android.gms.common.AccountPicker;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SetupAccountActivity extends BaseActivity {
    private static final String TAG = "SetupAccountActivity";
    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final int REQUEST_ACCOUNT_PICKER = 1;

    private Context mContext;
    private Button mChooseAccountButton;
    private Button mSkipButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorfulLinearLayout.setContentView(this, R.layout.hidden_zone_setup_account_page, R.color.theme_color);
        initActionBar();

        mContext = SetupAccountActivity.this;

        setupLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // fall back to FileManager to ask run time permission
            Toast.makeText(SetupAccountActivity.this, "Please grant the storage pemission.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
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

    private void setupLayout() {
        mChooseAccountButton = (Button) findViewById(R.id.choose_account);
        mChooseAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gmsIntent = AccountPicker.newChooseAccountIntent(null, null, new String[]{ACCOUNT_TYPE_GOOGLE},
                        true, null, null, null, null);
                try {
                    startActivityForResult(gmsIntent, REQUEST_ACCOUNT_PICKER);
                } catch (ActivityNotFoundException e) {
                    Intent accountManagerIntent = android.accounts.AccountManager.newChooseAccountIntent(
                            null, null, new String[]{ACCOUNT_TYPE_GOOGLE}, true, null, null, null, null);
                    startActivityForResult(accountManagerIntent, REQUEST_ACCOUNT_PICKER);
                }
            }
        });
        mSkipButton = (Button) findViewById(R.id.skip);
        mSkipButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
                pinCodeAccessHelper.clearRecoveryAccount();
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ACCOUNT_PICKER) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                String accountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
                PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
                pinCodeAccessHelper.setRecoveryAccount(accountName, accountType);
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
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }
}
