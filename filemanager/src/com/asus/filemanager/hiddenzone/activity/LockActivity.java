package com.asus.filemanager.hiddenzone.activity;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseAppCompatActivity;
import com.asus.filemanager.hiddenzone.KeyboardButtonClickedListener;
import com.asus.filemanager.hiddenzone.KeyboardButtonEnum;
import com.asus.filemanager.hiddenzone.KeyboardView;
import com.asus.filemanager.hiddenzone.LockStateMachine;
import com.asus.filemanager.hiddenzone.PasswordTextView;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.hiddenzone.state.BaseState;
import com.asus.filemanager.hiddenzone.state.RegisterPinDoneState;
import com.asus.filemanager.hiddenzone.state.RegisterPinState;
import com.asus.filemanager.hiddenzone.state.UnlockDoneState;
import com.asus.filemanager.hiddenzone.state.VerifyPinState;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ToolbarUtil;
import com.asus.filemanager.utility.Utility;
import com.google.android.gms.common.GoogleApiAvailability;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class LockActivity extends BaseAppCompatActivity {

    private static final String TAG = "LockActivity";
    private static final int AUTO_COMMIT_DELAY_MS = 300; // ms
    private static final String KEY_SAVED_PIN_CODE = "save_pin_code";
    private static final String KEY_SAVED_STATE = "saved_state";

    protected Context mContext;

    private String mPinCode = "";

    private ImageView mBannerView;
    private TextView mTitleView;
    private TextView mDescriptionView;
    private RelativeLayout mCancelView;
    private RelativeLayout mNextView;
    private PasswordTextView mPasswordView1;
    private PasswordTextView mPasswordView2;
    private PasswordTextView mPasswordView3;
    private PasswordTextView mPasswordView4;
    private KeyboardView mKeyboardView;
    private ImageView mBackSpaceView;

    private Handler mHandler;
    private LockStateMachine mLockStateMachine;
    protected BaseState mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ColorfulLinearLayout.setContentView(this, R.layout.hidden_zone_lockscreen, R.color.theme_color);
        ToolbarUtil.setupToolbar(this);
        mContext = LockActivity.this;
        mHandler = new Handler();

        // restore to previous state
        if (savedInstanceState != null) {
            mPinCode = savedInstanceState.getString(KEY_SAVED_PIN_CODE, "");
            mState = savedInstanceState.getParcelable(KEY_SAVED_STATE);
        } else {
            mPinCode = "";
            mState = getInitialState();
        }

        initActionBar();
        setupLayout();
        restorePreviousPasswordTextView();
        updateLayout();

        mLockStateMachine = new LockStateMachine(mState, new LockStateMachine.LockStateListener() {
            @Override
            public void onLockStateChanged(BaseState state) {
                mState = state;
                resetPassword();
                updateLayout();
                if (mState instanceof RegisterPinDoneState) {
                    if (Utility.isEnabledAndInstalledPackage(mContext,
                            GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)) {
                        PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
                        pinCodeAccessHelper.clearRecoveryAccount();
                        Intent intent = new Intent(mContext, SetupAccountActivity.class);
                        startActivity(intent);
                    }
                }
                if (mState instanceof RegisterPinDoneState
                        || mState instanceof UnlockDoneState) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // fall back to FileManager to ask run time permission
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SAVED_PIN_CODE, mPinCode);
        outState.putParcelable(KEY_SAVED_STATE, mState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
        return true;
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

    protected BaseState getInitialState() {
        return new RegisterPinState();
    }

    protected boolean isSupportAutoCommit() {
        return false;
    }

    private void setActionBarVisibility(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(isVisible);
            actionBar.setDisplayHomeAsUpEnabled(isVisible);
        }
    }

    private void setBannerVisibility(boolean isVisible) {
        if (mBannerView != null) {
            mBannerView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void setTitleVisibility(boolean isVisible) {
        if (mTitleView != null) {
            mTitleView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void setNextButtonVisibility(boolean isVisible) {
        if (mNextView != null) {
            mNextView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void setCancelButtonVisibility(boolean isVisible) {
        if (mCancelView != null) {
            mCancelView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void setupLayout() {
        mBannerView = (ImageView) findViewById(R.id.banner);
        mTitleView = (TextView) findViewById(R.id.title);
        mDescriptionView = (TextView) findViewById(R.id.description);
        mPasswordView1 = (PasswordTextView) findViewById(R.id.password1);
        mPasswordView2 = (PasswordTextView) findViewById(R.id.password2);
        mPasswordView3 = (PasswordTextView) findViewById(R.id.password3);
        mPasswordView4 = (PasswordTextView) findViewById(R.id.password4);

        mKeyboardView = (KeyboardView) findViewById(R.id.pin_code_keyboard_view);
        if (mKeyboardView != null) {
            mKeyboardView.setKeyboardButtonClickedListener(new KeyboardButtonClickedListener() {
                @Override
                public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum) {
                    int value = keyboardButtonEnum.getButtonValue();

                    if (value != KeyboardButtonEnum.BUTTON_OK.getButtonValue()) {
                        if (value == KeyboardButtonEnum.BUTTON_CANCEL.getButtonValue()) {
                            finish();
                            return;
                        }
                        if (getPasswordLength() < VerifyPinState.DEFAULT_PIN_LENGTH) {
                            appendPasswordChar(Character.forDigit(value, 10 /* decimal */));
                        }
                    } else {
                        if (mLockStateMachine != null
                                && value == KeyboardButtonEnum.BUTTON_OK.getButtonValue()) {
                            mLockStateMachine.updateState(mPinCode);
                        }
                    }
                }
            });
        }
        mBackSpaceView = (ImageView) findViewById(R.id.button_back_space);
        if (mBackSpaceView != null) {
            // set back icon to transparent:70%
            mBackSpaceView.setAlpha(178);
            mBackSpaceView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mPinCode.isEmpty()) {
                        deleteLastPasswordChar();
                    } else {
                        resetPassword();
                    }
                }
            });
        }
        mCancelView = (RelativeLayout) findViewById(R.id.pin_code_button_cancel);
        mNextView = (RelativeLayout) findViewById(R.id.pin_code_button_next);
    }

    protected void setMessageByState(BaseState state) {
        if (mDescriptionView != null) {
            int messageId = state.getMessageId();
            if (messageId != -1) {
                mDescriptionView.setText(messageId);
            } else {
                mDescriptionView.setText(null);
            }
        }
        if (state.isNeedToNotifyError()) {
            Animation shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);
            mDescriptionView.startAnimation(shakeAnimation);
        }
    }

    private void appendPasswordChar(char text) {
        PasswordTextView ptv = getSuitablePasswordTextView(PasswordTextView.INDEX_NEXT);
        if (ptv != null) {
            ptv.append(text);
        }
        updatePinCode();
        updateNextButton();
        removePreviousPasswordTextViewAnimation();

        if (getPasswordLength() == VerifyPinState.DEFAULT_PIN_LENGTH
                && isSupportAutoCommit()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLockStateMachine.updateState(mPinCode);
                }
            }, AUTO_COMMIT_DELAY_MS);
        }
    }

    private void restorePreviousPasswordTextView() {
        if (!mPinCode.isEmpty()) {
            for (int i = 0; i < mPinCode.length(); i++) {
                PasswordTextView ptv = getSpecifyPasswordTextView(i);
                if (ptv != null) {
                    ptv.append(mPinCode.charAt(i));
                }
            }
        }
    }

    private void removePreviousPasswordTextViewAnimation() {
        PasswordTextView ptv = getSuitablePasswordTextView(PasswordTextView.INDEX_PREVIOUS);
        if (ptv != null) {
            ptv.removeLastCharAnimation();
        }
    }

    private void deleteLastPasswordChar() {
        PasswordTextView ptv = getSuitablePasswordTextView(PasswordTextView.INDEX_CURRENT);
        if (ptv != null) {
            ptv.deleteLastChar();
        }
        updatePinCode();
        updateNextButton();
    }

    protected void resetPassword() {
        if (mPasswordView1 != null) {
            mPasswordView1.reset(true);
        }
        if (mPasswordView2 != null) {
            mPasswordView2.reset(true);
        }
        if (mPasswordView3 != null) {
            mPasswordView3.reset(true);
        }
        if (mPasswordView4 != null) {
            mPasswordView4.reset(true);
        }
        updatePinCode();
        updateNextButton();
    }

    private int getPasswordLength() {
        return mPinCode.length();
        // return mPasswordView.getText().length();
    }

    private PasswordTextView getSpecifyPasswordTextView(int index) {
        switch (index) {
        case 0:
            return mPasswordView1;
        case 1:
            return mPasswordView2;
        case 2:
            return mPasswordView3;
        case 3:
            return mPasswordView4;
        default:
            return null;
        }
    }

    private PasswordTextView getSuitablePasswordTextView(int cursorIndex) {
        int pinCodeLength = mPinCode.length() + cursorIndex;
        switch (pinCodeLength) {
        case 1:
            return mPasswordView1;
        case 2:
            return mPasswordView2;
        case 3:
            return mPasswordView3;
        case 4:
            return mPasswordView4;
        default:
            return null;
        }
    }

    private void updateLayout() {
        Log.i(TAG, "updateLayout: " + mState);
        setMessageByState(mState);

        boolean isSupportAutoCommit = isSupportAutoCommit();
        setActionBarVisibility(isSupportAutoCommit);
        setNextButtonVisibility(!isSupportAutoCommit);
        setCancelButtonVisibility(!isSupportAutoCommit);

        boolean isNeedToShowBanner = mState.isNeedToShowBanner();
        setBannerVisibility(isNeedToShowBanner);
        setTitleVisibility(!isNeedToShowBanner);
    }

    private void updatePinCode() {
        String pinCode = "";
        if (mPasswordView1 != null) {
            pinCode += mPasswordView1.getText();
        }
        if (mPasswordView2 != null) {
            pinCode += mPasswordView2.getText();
        }
        if (mPasswordView3 != null) {
            pinCode += mPasswordView3.getText();
        }
        if (mPasswordView4 != null) {
            pinCode += mPasswordView4.getText();
        }
        mPinCode = pinCode;
    }

    private void updateNextButton() {
        if (mNextView != null) {
            mNextView.setEnabled(
                    getPasswordLength() == VerifyPinState.DEFAULT_PIN_LENGTH ? true : false);
        }
    }

    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
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
