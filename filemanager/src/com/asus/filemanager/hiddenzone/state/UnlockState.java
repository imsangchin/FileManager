package com.asus.filemanager.hiddenzone.state;

import com.asus.filemanager.R;

import android.os.Parcel;

public class UnlockState extends VerifyPinState {

    public static final Creator<UnlockState> CREATOR = new Creator<UnlockState>() {
        public UnlockState createFromParcel(Parcel in) {
            return new UnlockState(in);
        }

        public UnlockState[] newArray(int size) {
            return new UnlockState[size];
        }
    };

    private UnlockState(Parcel parcel) {
        super(parcel);
    }

    public UnlockState() {
        super();
    }

    @Override
    public boolean isNeedToShowBanner() {
        return true;
    }

    @Override
    public BaseState nextState(String pinCode) {
        BaseState state = null;
        if (checkPinCode(pinCode)) {
            state = new UnlockDoneState();
        } else {
            state = new UnlockFailState();
        }
        return state;
    }

    @Override
    public int getMessageId() {
        return R.string.hidden_zone_enter_password;
    }

}
