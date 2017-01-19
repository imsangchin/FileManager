package com.asus.filemanager.hiddenzone.activity;

import com.asus.filemanager.hiddenzone.state.BaseState;
import com.asus.filemanager.hiddenzone.state.RegisterPinState;

import android.os.Bundle;

public class SetupPasswordActivity extends LockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected BaseState getInitialState() {
        return new RegisterPinState();
    }
}
