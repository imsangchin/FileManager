package com.asus.filemanager.provider;

import java.util.ArrayList;

import com.asus.filemanager.apprecommend.GameLaunchFile;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.utility.VFile;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class GameAppDbHelper {
    private static final String TAG = "GameAppDbHelper";

    public enum CategoryStatus {
        NOT_GAME_APP,
        GAME_APP,
        UNKNOWN
    }

    public static boolean isExistInDb(Context context, String packageName) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(GameAppProvider.CONTENT_URI,
                new String[] {GameAppProvider.PACKAGE_NAME},
                GameAppProvider.PACKAGE_NAME + "=?"
                + " and "+ GameAppProvider.CATEGORY + " NOTNULL",
                new String[] {packageName}, null);
        if (cursor != null && cursor.getCount() != 0) {
            return true;
        }
        return false;
    }

    public static void updateDbByPackageName(Context context, ContentValues values) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver.update(GameAppProvider.CONTENT_URI, values,
                GameAppProvider.PACKAGE_NAME + "=?",
                new String[] {(String) values.get(GameAppProvider.PACKAGE_NAME)}) == 0) {
            resolver.insert(GameAppProvider.CONTENT_URI, values);
        }
    }

    public static ArrayList<GameLaunchFile> queryGameLaunchFileFromDb(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(GameAppProvider.CONTENT_URI,
                new String[] {
                GameAppProvider.PACKAGE_NAME,
                GameAppProvider.CATEGORY,
                GameAppProvider.IS_GAME},
                GameAppProvider.IS_GAME + "=?",
                new String[] {String.valueOf(CategoryStatus.GAME_APP.ordinal())}, null);
        ArrayList<GameLaunchFile> resultList = new ArrayList<GameLaunchFile>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String packageName = cursor.getString(
                        cursor.getColumnIndex(GameAppProvider.PACKAGE_NAME));
                if (!Utility.isEnabledAndInstalledPackage(context, packageName)) {
                    continue;
                }

                String category = cursor.getString(
                        cursor.getColumnIndex(GameAppProvider.CATEGORY));
                int isGame = cursor.getInt(
                        cursor.getColumnIndex(GameAppProvider.IS_GAME));
                ContentValues values = new ContentValues();
                values.put(GameAppProvider.PACKAGE_NAME, packageName);
                values.put(GameAppProvider.CATEGORY, category);
                values.put(GameAppProvider.IS_GAME, isGame);
                resultList.add(new GameLaunchFile(context,
                        packageName, VFile.VFileType.TYPE_GAME_LAUNCH, values));
             }
            cursor.close();
        }
        return resultList;
    }
}
