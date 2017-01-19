package com.asus.filemanager.hiddenzone.state;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class BaseState implements Parcelable {

    protected BaseState(Parcel parcel) {
        // DO NOTHING
    }

    public BaseState() {
    }

    public boolean isNeedToShowBanner() {
        return false;
    }

    public boolean isNeedToNotifyError() {
        return false;
    }

    abstract public BaseState nextState(String pinCode);
    abstract public int getMessageId();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // DO NOTHING
    }
}
