package com.asus.filemanager.hiddenzone.state;

import com.asus.filemanager.R;

import android.os.Parcel;

public class RegisterPinState extends BaseState {

    public static final Creator<RegisterPinState> CREATOR = new Creator<RegisterPinState>() {
        public RegisterPinState createFromParcel(Parcel in) {
            return new RegisterPinState(in);
        }

        public RegisterPinState[] newArray(int size) {
            return new RegisterPinState[size];
        }
    };

    private RegisterPinState(Parcel parcel) {
        super(parcel);
    }

    public RegisterPinState() {
        super();
    }

    @Override
    public BaseState nextState(String pinCode) {
        return new ConfirmPinState(pinCode);
    }

    @Override
    public int getMessageId() {
        return R.string.hidden_zone_setup_password;
    }
}
