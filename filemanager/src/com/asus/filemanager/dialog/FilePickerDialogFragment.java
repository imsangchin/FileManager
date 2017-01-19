package com.asus.filemanager.dialog;

import java.util.ArrayList;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.utility.CreateShortcutUtil;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

public class FilePickerDialogFragment extends MoveToDialogFragment
        implements OnClickListener {

    private final static String TAG = "FilePickerDialogFragment";
    public final static String DIALOG_TAG = "FilePickerDialogFragment";
    public final static String KEY_FROM_CREATE_SHORTCUT = "KeyFromCreateShortcut";
    private static boolean bIsFromCreateShortcut = false;

    public static FilePickerDialogFragment newInstance(Bundle args) {
        FilePickerDialogFragment filePickerDialogFragment = new FilePickerDialogFragment();
        filePickerDialogFragment.setArguments(args);
        bIsFromCreateShortcut = args.getBoolean(KEY_FROM_CREATE_SHORTCUT, false);
        return filePickerDialogFragment;
    }

    @Override
    public void switchFragmentView() {

        ArrayList<Object> storageVolume = ((FileManagerApplication) getActivity()
                .getApplication()).getStorageVolume();

        VFile[] storageFile = ((FileManagerApplication) getActivity()
                .getApplication()).getStorageFile();
        VFile scanTargetFile = null;

        StorageManager storageManager = (StorageManager) getActivity()
                .getSystemService(Context.STORAGE_SERVICE);

        int numMountedLocalStorage = 0;

        if (storageManager != null) {

            for (int i = 0; i < storageVolume.size(); i++) {
                if (reflectionApis.getVolumeState(storageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)) {
                    numMountedLocalStorage++;
                    scanTargetFile = storageFile[i];
                }
            }
        }

        if (numMountedLocalStorage > 1) {
            super.switchFragmentView();
        } else {
            mMainPageList.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
            startScanFile(scanTargetFile, ScanType.SCAN_CHILD);
        }
    }

    protected  DialogInterface.OnClickListener getDialogInterface(){
        return this;
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        switch(button){
        case Dialog.BUTTON_POSITIVE:
            if (bIsFromCreateShortcut) {
                GaShortcut.getInstance().sendEvents(getActivity(),GaShortcut.CATEGORY_NAME,
                        GaShortcut.ACTION_CREATE_FROM_WIDGET, null, null);
                getActivity().setResult(Activity.RESULT_OK, CreateShortcutUtil.getInstallShortcutIntent(
                        getActivity(), mIndicatorFile.getPath(), mIndicatorFile.getNameNoExtension()));
                getActivity().finish();
            } else {
                mFileListFrament.onFolderSelected(mIndicatorFile, null);
            }
            dialog.dismiss();
            break;
        case Dialog.BUTTON_NEGATIVE:
            if (bIsFromCreateShortcut) {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
            dialog.dismiss();
            break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(bIsFromCreateShortcut) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keycode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_UP && keycode == KeyEvent.KEYCODE_BACK) {
            if (null != mIndicatorFile && !isRootFolder(mIndicatorFile)) {
                startScanFile(mIndicatorFile.getParentFile() , ScanType.SCAN_CHILD);
                return true;
            } else {
                return false;
            }
        }

        return false;
    }
}
