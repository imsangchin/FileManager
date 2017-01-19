package com.asus.filemanager.hiddenzone.activity;

import java.io.IOException;

import com.asus.filemanager.R;
import com.asus.filemanager.ga.GaHiddenCabinet;
import com.asus.filemanager.hiddenzone.encrypt.CryptoFactory;
import com.asus.filemanager.hiddenzone.encrypt.FingerprintUiHelper;
import com.asus.filemanager.hiddenzone.encrypt.FingerprintUtils;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.hiddenzone.state.BaseState;
import com.asus.filemanager.hiddenzone.state.UnlockState;
import com.asus.filemanager.utility.Utility;
import com.google.android.gms.common.GoogleApiAvailability;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class UnlockActivity extends LockActivity implements FingerprintUiHelper.Callback {
    private static final String TAG = "UnlockActivity";

    private TextView mDescriptionView;
    private ImageView mFingerprintIconView;

    private PinCodeAccessHelper mPinCodeAccessHelper;
    private FingerprintUiHelper mFingerprintUiHelper;
    private CryptoObject mCryptoObject;

    private MenuItem mForgetPassword;

    public static final String KEY_SHOULD_SHOW_MENU = "should_show_menu";
    public static final String KEY_UNLOCK_VIA_FINGERPRINT = "unlock_via_fingerprint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPinCodeAccessHelper = PinCodeAccessHelper.getInstance();

        mDescriptionView = (TextView) findViewById(R.id.description);
        mFingerprintIconView = (ImageView) findViewById(R.id.fingerprint_icon);

        if (mState instanceof UnlockState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCryptoObject = new CryptoFactory().createCryptoObject();
            mFingerprintIconView.setImageDrawable(
                    getDrawable(R.drawable.ic_fingerprint_normal));
            mFingerprintUiHelper = new FingerprintUiHelper.FingerprintUiHelperBuilder(
                    mContext.getSystemService(FingerprintManager.class)).build(
                            mFingerprintIconView, mDescriptionView, this);
        }
    }

    @Override
    protected BaseState getInitialState() {
        return new UnlockState();
    }

    protected boolean isSupportAutoCommit() {
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFingerprintUiHelper != null) {
            mFingerprintUiHelper.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuItemVisibility();

        if (FingerprintUtils.getUserHasAndAllowFingerprint(mContext)) {
            mFingerprintIconView.setVisibility(View.VISIBLE);
        } else {
            mFingerprintIconView.setVisibility(View.GONE);
        }

        if (mFingerprintUiHelper != null) {
            mFingerprintUiHelper.startListening(mContext, mCryptoObject);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Intent intent = getIntent();

        if (intent != null && intent.getBooleanExtra(KEY_SHOULD_SHOW_MENU, false)) {
            getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
            mForgetPassword = menu.findItem(R.id.forget_password);
            updateMenuItemVisibility();
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void updateMenuItemVisibility() {
        boolean hasRecoveryAccount = mPinCodeAccessHelper.hasRecoveryAccount();
        if (mForgetPassword != null) {
            mForgetPassword.setVisible(hasRecoveryAccount
                    && Utility.isEnabledAndInstalledPackage(mContext,
                            GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.forget_password: {
            verifyAccount(mPinCodeAccessHelper.getRecoveryAccount());
            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthenticated() {
        Log.i(TAG, "onAuthenticated");
        resetPassword();
        Intent intent = new Intent();
        intent.putExtra(KEY_UNLOCK_VIA_FINGERPRINT, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError() {
        Log.i(TAG, "onError");
    }

    private void verifyAccount(Account account) {
        if (account == null) {
            Log.w(TAG, "account == null");
            return;
        }
        AccountManager accountManager = AccountManager.get(this);

        accountManager.confirmCredentials(account, null,
                UnlockActivity.this,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Bundle result = future.getResult();
                            if (result == null) {
                                return;
                            }
                            boolean isVerifySuccess = result
                                    .getBoolean(AccountManager.KEY_BOOLEAN_RESULT);
                            GaHiddenCabinet.getInstance().sendForgetPasswordEvent(
                                    mContext, isVerifySuccess);
                            if (isVerifySuccess) {
                                startActivity(new Intent(mContext, SetupPasswordActivity.class));
                                finish();
                            } else {
                                // verify fail case
                            }
                        } catch (OperationCanceledException e) {
                            e.printStackTrace();
                        } catch (AuthenticatorException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(mContext, R.string.setup_account_no_internet_connection,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }, null);
    }
}
