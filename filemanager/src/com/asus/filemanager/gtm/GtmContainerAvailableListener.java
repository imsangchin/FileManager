package com.asus.filemanager.gtm;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.ga.GaActiveUser;
import com.asus.filemanager.ga.GaBrowseFile;
import com.asus.filemanager.ga.GaCategorySorting;
import com.asus.filemanager.ga.GaCloudStorage;
import com.asus.filemanager.ga.GaExperiment;
import com.asus.filemanager.ga.GaFailCaseCollection;
import com.asus.filemanager.ga.GaHiddenCabinet;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.ga.GaMoveToDialog;
import com.asus.filemanager.ga.GaNonAsusActiveUser;
import com.asus.filemanager.ga.GaOpenFile;
import com.asus.filemanager.ga.GaPhotoViewer;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.ga.GaSettingsPage;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.ga.GaUserPreference;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.ContainerHolder.ContainerAvailableListener;

import java.lang.ref.WeakReference;

public class GtmContainerAvailableListener implements ContainerAvailableListener {

    private static final String TAG = "GtmContainerAvailableListener";
    private static final boolean DBG = false;

    private WeakReference<FileManagerActivity> activity;
    private Container mContainer;

    public GtmContainerAvailableListener(FileManagerActivity fileManagerActivity) {
        this.activity = new WeakReference<FileManagerActivity>(fileManagerActivity);
    }

    @Override
    public void onContainerAvailable(ContainerHolder containerHolder, String arg1) {
        if(activity.get()==null)
            return;
        final FileManagerActivity mActivity = activity.get();

        mContainer = containerHolder.getContainer();

        if (!containerHolder.getStatus().isSuccess()) {
            return;
        }

        String mInsider = mContainer.getString("InsiderProgram");

        if (null != mInsider) {
            SharedPreferences sharedPreferences = mActivity.getSharedPreferences("MyPrefsFile", 0);
            if (mInsider.compareToIgnoreCase("1") == 0) {
                sharedPreferences.edit().putBoolean("EnableInsiderProgram", true).commit();
            } else {
                sharedPreferences.edit().putBoolean("EnableInsiderProgram", false).commit();
            }
        }

        String mVERSION_CODE;
        String mFORCE_VERSION_NOTIFY;
        mVERSION_CODE = mContainer.getString("VERSION_CODE");
        mFORCE_VERSION_NOTIFY = mContainer.getString("VERSION_FORCE_NOTIFY");

        if (null != mVERSION_CODE && null != mFORCE_VERSION_NOTIFY) {
            long serverVersionCode = 0;
            try {
                serverVersionCode = Long.parseLong(mVERSION_CODE);
            } catch(Exception e) {
                e.printStackTrace();
            }

            SharedPreferences mSharePrefence = mActivity.getSharedPreferences("MyPrefsFile", 0);
            PackageInfo pkgInfo = null;
            long myVersionCode= serverVersionCode;
            try {
                pkgInfo = mActivity.getPackageManager().getPackageInfo(
                        mActivity.getPackageName(), 0);
                myVersionCode = pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (myVersionCode < serverVersionCode) {
                long notifiedVersionCode = mSharePrefence.getLong("VERSION_NOTIFIED",0);
                if (mFORCE_VERSION_NOTIFY.compareTo("1") == 0 || notifiedVersionCode < serverVersionCode) {
                    mSharePrefence.edit().putLong("VERSION_NOTIFIED", serverVersionCode).commit();

                    // Avoid do ui task on background thread.
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            mActivity.displayDialog(DialogType.TYPE_NEWVERSION_NOTIFY_DIALOG, null);
                        }
                    });
                }
            }
        }

        String RETENTION_USB_MOD = mContainer.getString("RETENTION_USB_MOD");

        if (null != RETENTION_USB_MOD && !TextUtils.isEmpty(RETENTION_USB_MOD)) {
            long retention_usb_mod = 5;
            try {
                retention_usb_mod = Long.parseLong(RETENTION_USB_MOD);
            } catch(Exception e) {
                e.printStackTrace();
            }
            SharedPreferences sharedPreferences = mActivity.getSharedPreferences("MyPrefsFile", 0);
            sharedPreferences.edit().putLong("RETENTION_USB_MOD", retention_usb_mod).commit();
        }

        String RETENTION_MIN_PERIOD = mContainer.getString("RETENTION_MIN_PERIOD");

        if (null != RETENTION_MIN_PERIOD && !TextUtils.isEmpty(RETENTION_MIN_PERIOD)) {
            long retention_min_period = 2; //default value, two days
            try {
                retention_min_period = Long.parseLong(RETENTION_MIN_PERIOD);
            } catch(Exception e) {
                e.printStackTrace();
            }
            ItemOperationUtility.getInstance().setMinRetentionPeriod(mActivity, retention_min_period);
        }

        String RECOMMEND_CM_DIALOG= mContainer.getString("RECOMMEND_CM_DIALOG");

        if (null != RECOMMEND_CM_DIALOG) {
            SharedPreferences sharedPreferences = mActivity.getSharedPreferences("MyPrefsFile", 0);
            if (RECOMMEND_CM_DIALOG.compareToIgnoreCase("1") == 0) {
                sharedPreferences.edit().putBoolean("RECOMMEND_CM_DIALOG", true).commit();
            } else {
                sharedPreferences.edit().putBoolean("RECOMMEND_CM_DIALOG", false).commit();
            }
        }

        updateGaPreference();
    }

    private void updateGaPreference() {
        if (mContainer == null) {
            return;
        }
        if(activity.get()==null)
            return;
        FileManagerActivity mActivity = activity.get();

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("GaConfig", 0);

        updateGaPreference(sharedPreferences, GaAccessFile.KEY_ID, GaAccessFile.KEY_ENABLE_TRACKING, GaAccessFile.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaActiveUser.KEY_ID, GaActiveUser.KEY_ENABLE_TRACKING, GaActiveUser.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaNonAsusActiveUser.KEY_ID, GaNonAsusActiveUser.KEY_ENABLE_TRACKING, GaNonAsusActiveUser.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaBrowseFile.KEY_ID, GaBrowseFile.KEY_ENABLE_TRACKING, GaBrowseFile.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaCategorySorting.KEY_ID, GaCategorySorting.KEY_ENABLE_TRACKING, GaCategorySorting.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaCloudStorage.KEY_ID, GaCloudStorage.KEY_ENABLE_TRACKING, GaCloudStorage.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaMenuItem.KEY_ID, GaMenuItem.KEY_ENABLE_TRACKING, GaMenuItem.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaMoveToDialog.KEY_ID, GaMoveToDialog.KEY_ENABLE_TRACKING, GaMoveToDialog.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaOpenFile.KEY_ID, GaOpenFile.KEY_ENABLE_TRACKING, GaOpenFile.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaSearchFile.KEY_ID, GaSearchFile.KEY_ENABLE_TRACKING, GaSearchFile.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaUserPreference.KEY_ID, GaUserPreference.KEY_ENABLE_TRACKING, GaUserPreference.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaPhotoViewer.KEY_ID, GaPhotoViewer.KEY_ENABLE_TRACKING, GaPhotoViewer.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaShortcut.KEY_ID, GaShortcut.KEY_ENABLE_TRACKING, GaShortcut.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaPromote.KEY_ID, GaPromote.KEY_ENABLE_TRACKING, GaPromote.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaExperiment.KEY_ID, GaExperiment.KEY_ENABLE_TRACKING, GaExperiment.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaStorageAnalyzer.KEY_ID, GaStorageAnalyzer.KEY_ENABLE_TRACKING, GaStorageAnalyzer.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaSettingsPage.KEY_ID, GaSettingsPage.KEY_ENABLE_TRACKING, GaSettingsPage.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaFailCaseCollection.KEY_ID, GaFailCaseCollection.KEY_ENABLE_TRACKING, GaFailCaseCollection.KEY_SAMPLE_RATE);
        updateGaPreference(sharedPreferences, GaHiddenCabinet.KEY_ID, GaHiddenCabinet.KEY_ENABLE_TRACKING, GaHiddenCabinet.KEY_SAMPLE_RATE);
    }

    private void updateGaPreference(SharedPreferences sharedPreferences, String keyId,
            String keyEnableTracking, String keySampleRate) {

        String id = mContainer.getString(keyId);
        boolean enableTracking = mContainer.getBoolean(keyEnableTracking);
        double sampleRate = mContainer.getDouble(keySampleRate);

        if (!id.isEmpty()) {
            if (DBG) {
                Log.d(TAG, "Get: " + keyId + " = " + id);
            }
            sharedPreferences.edit().putString(keyId, id).commit();
        } else {
            if (DBG) {
                Log.w(TAG, "Cannot get: " + keyId);
            }
        }

        if (DBG) {
            Log.d(TAG, "Get: " + keyEnableTracking + " = " + enableTracking);
        }

        sharedPreferences.edit().putBoolean(keyEnableTracking,
                enableTracking).commit();

        if (sampleRate != 0) {
            if (DBG) {
                Log.d(TAG, "Get: " + keySampleRate + " = " + sampleRate);
            }
            sharedPreferences.edit().putFloat(keySampleRate,
                    (float) sampleRate).commit();
        } else {
            if (DBG) {
                Log.w(TAG, "Cannot get: " + keySampleRate);
            }
        }

        if (DBG) {
            Log.d(TAG, "----------------------------------");
        }
    }
}
