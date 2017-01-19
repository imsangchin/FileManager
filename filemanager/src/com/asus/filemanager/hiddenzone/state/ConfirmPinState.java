package com.asus.filemanager.hiddenzone.state;

import com.asus.filemanager.R;

import android.os.Parcel;

public class ConfirmPinState extends BaseState {

    private static final int MAX_RETRY_TIMES = 3;

    private int mRetryTimes = 0;
    private String mPrepareUsedPinCode;

    public static final Creator<ConfirmPinState> CREATOR = new Creator<ConfirmPinState>() {
        public ConfirmPinState createFromParcel(Parcel in) {
            return new ConfirmPinState(in);
        }

        public ConfirmPinState[] newArray(int size) {
            return new ConfirmPinState[size];
        }
    };

    private ConfirmPinState(Parcel parcel) {
        super(parcel);
        mRetryTimes = parcel.readInt();
        mPrepareUsedPinCode = parcel.readString();
    }

    public ConfirmPinState(String prepareUsedPinCode) {
        super();
        mRetryTimes = 0;
        mPrepareUsedPinCode = prepareUsedPinCode;
    }

    protected boolean confirmCorrect(String pinCode) {
        return mPrepareUsedPinCode.equals(pinCode);
    }

    @Override
    public BaseState nextState(String pinCode) {
        BaseState state = null;

        if (confirmCorrect(pinCode)) {
            state = new RegisterPinDoneState(pinCode);
            mRetryTimes = 0;
        } else {
            // CONFIRM FAIL CASE
            mRetryTimes++;
            if (mRetryTimes == MAX_RETRY_TIMES) {
                state = this;
            } else {
                state = this;
            }
        }
        return state;
    }

    @Override
    public int getMessageId() {
        if (mRetryTimes != 0) {
            return R.string.hidden_zone_password_mismatch;
        }
        return R.string.hidden_zone_confirm_password;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mRetryTimes);
        dest.writeString(mPrepareUsedPinCode);
    }

}
