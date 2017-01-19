package com.asus.filemanager.hiddenzone.state;

import com.asus.filemanager.R;

import android.os.Parcel;

public class UnlockFailState extends VerifyPinState {

    public static final Creator<UnlockFailState> CREATOR = new Creator<UnlockFailState>() {
        public UnlockFailState createFromParcel(Parcel in) {
            return new UnlockFailState(in);
        }

        public UnlockFailState[] newArray(int size) {
            return new UnlockFailState[size];
        }
    };

    private UnlockFailState(Parcel parcel) {
        super(parcel);
    }

    public UnlockFailState() {
        super();
    }

    @Override
    public boolean isNeedToShowBanner() {
        return true;
    }

    @Override
    public boolean isNeedToNotifyError() {
        return true;
    }

    @Override
    public BaseState nextState(String pinCode) {
        BaseState state = null;
        if (checkPinCode(pinCode)) {
            state = new UnlockDoneState();
        } else {
            state = this;
        }
        return state;
    }

    @Override
    public int getMessageId() {
        return R.string.hidden_zone_wrong_password;
    }

}
