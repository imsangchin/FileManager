package com.asus.filemanager.activity;

import android.content.Context;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.asus.filemanager.R;

/**
 * Created by Wesley_Lee on 2016/12/27.
 */

public class DoubleLayerActionModeWrapper {
    private Context mContext;
    private ActionMode mActionMode;
    private MenuInflater mMenuInflater;
    private boolean mIsInSecondLayer;

    public DoubleLayerActionModeWrapper(Context context, ActionMode actionMode) {
        mContext = context;
        mActionMode = actionMode;
        mMenuInflater = mActionMode.getMenuInflater();
    }

    public void enterToSecondLayer() {
        mIsInSecondLayer = true;
        updateLayout();
    }

    public boolean isInSecondLayer() {
        return mIsInSecondLayer;
    }

    private void updateLayout() {
        if (mIsInSecondLayer) {
            Menu menu = mActionMode.getMenu();
            if (menu != null) {
                menu.clear();
            }
            mMenuInflater.inflate(R.menu.cab_yes_no, menu);
            LayoutInflater inflater=(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.cab_actionbar_layer2, null);
            mActionMode.setCustomView(view);
        }
    }
}
