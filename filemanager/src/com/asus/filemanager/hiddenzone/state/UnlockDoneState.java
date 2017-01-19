package com.asus.filemanager.hiddenzone.state;

import android.os.Parcel;

public class UnlockDoneState extends BaseState {

    public static final Creator<UnlockDoneState> CREATOR = new Creator<UnlockDoneState>() {
        public UnlockDoneState createFromParcel(Parcel in) {
            return new UnlockDoneState(in);
        }

        public UnlockDoneState[] newArray(int size) {
            return new UnlockDoneState[size];
        }
    };

    private UnlockDoneState(Parcel parcel) {
        super(parcel);
    }

    public UnlockDoneState() {
        super();
    }

    @Override
    public boolean isNeedToShowBanner() {
        return true;
    }

    @Override
    public BaseState nextState(String pinCode) {
        return this;
    }

    @Override
    public int getMessageId() {
        return -1;
    }

}
