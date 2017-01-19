package com.asus.filemanager.hiddenzone;

import com.asus.filemanager.hiddenzone.state.BaseState;
import com.asus.filemanager.hiddenzone.state.VerifyPinState;

public class LockStateMachine {

    public interface LockStateListener {
        public void onLockStateChanged(BaseState state);
    }

    private static final String TAG = "LockStateMachine";

    private BaseState mCurrentState;
    private LockStateListener mLockStateListener;

    public LockStateMachine(BaseState initState, LockStateListener listener) {
        mCurrentState = initState;
        mLockStateListener = listener;
    }

    public void updateState(String pinCode) {
        if (pinCode.length() != VerifyPinState.DEFAULT_PIN_LENGTH) {
            return;
        }

        if (mCurrentState != null) {
            mCurrentState = mCurrentState.nextState(pinCode);
        }

        if (mLockStateListener != null) {
            mLockStateListener.onLockStateChanged(mCurrentState);
        }
    }
}
