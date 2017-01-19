package com.asus.filemanager.adapter.grouper.categoryparser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "AppInstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "intent = " + intent.toString());
        String packageName = intent.getData().getEncodedSchemeSpecificPart();

        asyncParseAppCategory(context, packageName);
    }

    private void asyncParseAppCategory(final Context context, final String packageName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                GameUtils.parseAppCategoryByPackageName(context, packageName);
            }
        }).start();
    }

}
