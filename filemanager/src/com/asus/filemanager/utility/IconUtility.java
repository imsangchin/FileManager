
package com.asus.filemanager.utility;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class IconUtility {

    private static final String TAG = "IconUtility";
    private static final boolean DEBUG = false;

    public static class ThumbnailItem {
        public Bitmap thumbnail;
        public long modifyTime;

        public ThumbnailItem (Bitmap bitmap, long time) {
            thumbnail = bitmap;
            modifyTime = time;
        }
    }

    public static Bitmap loadResizedBitmap(String filename, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        if (options.outHeight > 0 && options.outWidth > 0)
        {
            int oldWidth = options.outWidth;
            int oldHeight = options.outHeight;
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > width
                    || options.outHeight / options.inSampleSize > height)
            {
                options.inSampleSize++;
            }
            BitmapFactory.decodeFile(filename, options);
            if (options.inSampleSize > 1 && ((options.outWidth == oldWidth) || (options.outHeight == oldHeight))) {
                Log.d(TAG, "set " + filename + " thumbnail = null");
            } else {
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(filename, options);
            }
        }
        return bitmap;
    }

   /* public static Drawable getApkIcon(Context context, String path) {
        Drawable icon = null;
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(path, 0);
        // if the apk file is broken, then packageInfo will be null
        if (packageInfo == null) {
            return null;
        }
        ApplicationInfo appInfo = packageInfo.applicationInfo;
        //icon = pm.getApplicationLogo(appInfo);
      AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(path);
        Resources r = context.getResources();
        Resources res = new Resources(assmgr, r.getDisplayMetrics(), r.getConfiguration());
        if (appInfo.icon > 0) {
            icon = res.getDrawable(appInfo.icon);
            Bitmap bmp=BitmapFactory.decodeResource(res, appInfo.icon);
        }
        assmgr.close();
        return icon;
    }*/
    public static Bitmap getApkIcon(Context context, String path) {
    	Bitmap icon = null;
    	PackageManager pm = context.getPackageManager();
    	PackageInfo packageInfo = pm.getPackageArchiveInfo(path, 0);
    	// if the apk file is broken, then packageInfo will be null
    	if (packageInfo == null) {
    		return null;
    	}
    	ApplicationInfo appInfo = packageInfo.applicationInfo;
    	//icon = pm.getApplicationLogo(appInfo);

    	AssetManager assmgr = reflectionApis.getAssetManagerWithPath(path);
    	if (null != assmgr){
    		Resources r = context.getResources();
    		Resources res = new Resources(assmgr, r.getDisplayMetrics(), r.getConfiguration());
    		if (appInfo.icon > 0) {
    			//icon = res.getDrawable(appInfo.icon);
    			icon = BitmapFactory.decodeResource(res, appInfo.icon);
    		}
    		assmgr.close();
    	}
    	return icon;
    }

}
