package com.asus.filemanager.hiddenzone.state;

import android.os.Parcel;

import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;

public abstract class VerifyPinState extends BaseState {
    public static final int DEFAULT_PIN_LENGTH = 4;

    private PinCodeAccessHelper mPinCodeAccessHelper = null;

    protected VerifyPinState(Parcel parcel) {
        super(parcel);
        mPinCodeAccessHelper = PinCodeAccessHelper.getInstance();
    }

    public VerifyPinState() {
        super();
        mPinCodeAccessHelper = PinCodeAccessHelper.getInstance();
    }

    protected boolean checkPinCode(String pinCode) {
        if (pinCode.length() == DEFAULT_PIN_LENGTH &&
                mPinCodeAccessHelper != null) {
            return mPinCodeAccessHelper.checkPinCode(pinCode);
        }
        return false;
    }

    protected void setPinCode(String pinCode) {
        if (pinCode.length() == DEFAULT_PIN_LENGTH &&
                mPinCodeAccessHelper != null) {
            mPinCodeAccessHelper.setPinCode(pinCode);
        }
    }
}
