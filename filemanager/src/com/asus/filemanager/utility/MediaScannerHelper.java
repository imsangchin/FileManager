package com.asus.filemanager.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.media.AudioManager;

public class MediaScannerHelper {
    private static final String TAG = MediaScannerHelper.class.getSimpleName();

    private static Method sMethodIsNoMediaPath = null;

    static {
        try {
            Class<?> cls = AudioManager.class.getClassLoader().loadClass("android.media.MediaScanner");
            sMethodIsNoMediaPath = cls.getMethod( "isNoMediaPath", new Class[] { String.class } );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Note:
    // isNoMediaPath define:
    // Ensure the path is NOT a directory, directory always return false
    // return true
    // - if file or any parent directory has name starting with a dot
    // - if any parent directories have a ".nomedia" file
    // - ignore album art files created by Windows Media Player:
    //   Folder.jpg, AlbumArtSmall.jpg, AlbumArt_{...}_Large.jpg
    //   and AlbumArt_{...}_Small.jpg
    //
    // Verified this hide method on:
    // - Nexus5 android-6.0
    // - HTC one E8 android-5.0.2
    // - Samsung S4 android-5.0.1
    // - ASUS pf500kl android-5.0
    // - ASUS me302c android-4.3
    public static boolean isNoMediaPath(String path) {
        try {
            return (Boolean)sMethodIsNoMediaPath.invoke(null, path);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }
}
