package com.asus.filemanager.utility;

import com.asus.filemanager.activity.FileManagerActivity;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;

/*
 *  The root layout of window overlay on Phone layout.
 */

public class ShortCutFrame extends FrameLayout {

    private final static String TAG = "ShortCutFrame";
    private final boolean DBG = true;
    FileManagerActivity mActivity;

    public ShortCutFrame(Context context) {
        super(context);
        mActivity = (FileManagerActivity) context;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_UP
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (DBG) {
                Log.d(TAG, "dispatchKeyEvent - closeShortCut");
            }
            mActivity.closeShortCut();
        }
        return super.dispatchKeyEvent(event);
    }

}
