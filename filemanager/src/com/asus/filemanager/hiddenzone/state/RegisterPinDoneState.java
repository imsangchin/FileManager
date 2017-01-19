package com.asus.filemanager.hiddenzone.state;

import android.os.Parcel;

public class RegisterPinDoneState extends VerifyPinState {

    public static final Creator<RegisterPinDoneState> CREATOR = new Creator<RegisterPinDoneState>() {
        public RegisterPinDoneState createFromParcel(Parcel in) {
            return new RegisterPinDoneState(in);
        }

        public RegisterPinDoneState[] newArray(int size) {
            return new RegisterPinDoneState[size];
        }
    };

    private RegisterPinDoneState(Parcel parcel) {
        super(parcel);
    }

    public RegisterPinDoneState(String pinCode) {
        super();
        setPinCode(pinCode);
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
