package com.asus.filemanager.apprecommend;

import com.asus.filemanager.provider.GameAppProvider;
import com.asus.filemanager.utility.VFile;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class GameLaunchFile extends VFile {

    private static final String TAG = "LauncherFile";

    private PackageManager mPackageManager;
    private ContentValues mContentValues;

    public GameLaunchFile(Context context, String path, int type, ContentValues contentValues) {
        super(path, type);
        mPackageManager = context.getPackageManager();
        mContentValues = contentValues;
    }

    public String getAppPackageName() {
        return (String) mContentValues.get(GameAppProvider.PACKAGE_NAME);
    }

    public String getAppName() {
        String packageName = getAppPackageName();
        if (packageName != null) {
            ApplicationInfo ai;
            try {
                ai = mPackageManager.getApplicationInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                ai = null;
            }
            return (String) (ai != null ? mPackageManager.getApplicationLabel(ai) : "unknown");
        }
        return "unknown";
    }

    public String getAppCategory() {
        return (String) mContentValues.get(GameAppProvider.CATEGORY);
    }

    @Override
    public boolean isSearchable() {
        return false;
    }
}
