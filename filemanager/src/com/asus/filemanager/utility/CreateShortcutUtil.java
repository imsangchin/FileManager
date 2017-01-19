package com.asus.filemanager.utility;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.CategoryItem;

public class CreateShortcutUtil {
    private final static String TAG = CreateShortcutUtil.class.getSimpleName();

    public static void createFolderShortcut(Context context, String path, String name) {
        Log.i(TAG, "Create folder shortcut:");
        Log.i(TAG, "    path = "+path);
        Log.i(TAG, "    name = "+name);
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("path", path);
        shortcutIntent.setClassName(context.getPackageName(),
                "com.asus.filemanager.activity.FileManagerActivity");
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        installShortcut(context, shortcutIntent, name, R.mipmap.app_icon_release);
    }

    public static void createCategoryShortcut(Context context, int categoryItemId) {
        Log.i(TAG, "Create category shortcut:");
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("categoryItemId", categoryItemId);
        shortcutIntent.setClassName(context.getPackageName(),
                "com.asus.filemanager.activity.FileManagerActivity");
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        String name = CategoryItem.findNameById(context.getResources(), categoryItemId);
        Log.i(TAG, "    category name = "+name);
        ImageView aImageView = new ImageView(context);
        aImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Some devices under api level 19 may encounter the problem of
        // shortcut icon size too big, so we calculate the icon size here.
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int defSize =  activityManager.getLauncherLargeIconSize();
        int size = context.getResources().getDimensionPixelSize(R.dimen.category_image_height);
        float ratio = 1;

        if(defSize < size)
        {
            ratio = (float)defSize / (float)size;
            Log.i(TAG, "createCategoryShortcut, need to resize icon, ratio: " + ratio);
        }

        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap will be null
        aImageView.layout(0, 0, (int)(size * ratio), (int)(size * ratio));

        // Resize image in ImageView.
        Drawable imageSrc = CategoryItem.findDrawableById(context.getResources(),categoryItemId);
        int imageWidth = imageSrc.getIntrinsicWidth();
        int imageHeight = imageSrc.getIntrinsicHeight();
        Bitmap bitmap = ((BitmapDrawable) imageSrc).getBitmap();
        Drawable scaledImage = new BitmapDrawable(context.getResources(),
                Bitmap.createScaledBitmap(bitmap, (int)(imageWidth * ratio), (int)(imageHeight * ratio), true));

        aImageView.setImageDrawable(scaledImage);
        aImageView.setBackgroundResource(R.drawable.category_circle);

        // Change background color based on different category.
        Drawable background = aImageView.getBackground();
        int backgroundColor = ContextCompat.getColor(context, CategoryItem.findColorIdById(categoryItemId));
        CategoryItem.setBackgroundColorAndRetainShape(backgroundColor, background);

        aImageView.setDrawingCacheEnabled(true);
        aImageView.buildDrawingCache(true);
        Bitmap aIconBitmap= Bitmap.createBitmap(aImageView.getDrawingCache());
        aImageView.setDrawingCacheEnabled(false); // clear drawing cache

        if (null == aIconBitmap)
            installShortcut(context, shortcutIntent, name, R.mipmap.app_icon_release);
        else
            installShortcut(context, shortcutIntent, name, aIconBitmap);
    }

    private static void installShortcut(Context context, Intent shortcutIntent, String name, int iconId) {
        Intent putShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        putShortcutIntent.putExtra("duplicate", false);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        Parcelable icon = Intent.ShortcutIconResource.fromContext(context, iconId);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        context.sendBroadcast(putShortcutIntent);
        Toast.makeText(context, context.getResources().getString(R.string.create_shortcut_toast), Toast.LENGTH_SHORT)
            .show();
    }

    private static void installShortcut(Context context, Intent shortcutIntent, String name, Bitmap aBitmap) {
        Intent putShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        putShortcutIntent.putExtra("duplicate", false);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, aBitmap);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        context.sendBroadcast(putShortcutIntent);
        Toast.makeText(context, context.getResources().getString(R.string.create_shortcut_toast), Toast.LENGTH_SHORT)
            .show();
    }

    public static Intent getInstallShortcutIntent(Context context, String path, String name) {
        Log.i(TAG, "get InstallShortcut Intent:");
        Log.i(TAG, "    path = "+path);
        Log.i(TAG, "    name = "+name);
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("path", path);
        shortcutIntent.setClassName(context.getPackageName(),
                "com.asus.filemanager.activity.FileManagerActivity");
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Intent putShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        putShortcutIntent.putExtra("duplicate", false);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        Parcelable icon = Intent.ShortcutIconResource.fromContext(context, R.mipmap.app_icon_release);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        return putShortcutIntent;
    }

}
