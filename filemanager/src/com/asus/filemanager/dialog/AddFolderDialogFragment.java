
package com.asus.filemanager.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;

import java.io.File;


public class AddFolderDialogFragment extends DialogFragment implements TextWatcher, OnClickListener {

    private static final String TAG = AddFolderDialogFragment.class.getSimpleName();
    public final static String DIALOG_TAG = TAG;
    private static final int MIN_FOLDER_SIZE = 4096;
    public static final int TYPE_ADD_DIALOG = 0;
    public static final int TYPE_ROGRESS_DIALOG = 1;
    private static final boolean DEBUG = false;

    private boolean mIsFirstCreate = true;

    private TextView editToast;

    public static AddFolderDialogFragment newInstance(VFile file, int typeDialog) {
        AddFolderDialogFragment fragment = new AddFolderDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");

        if (savedInstanceState != null) {
            mIsFirstCreate = savedInstanceState.getBoolean("mIsFirstCreate");
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        VFile parent = (VFile) getArguments().getSerializable("file");
        int type = getArguments().getInt("type");

        final AlertDialog dialog;

        if (type == TYPE_ADD_DIALOG) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View renameView = inflater.inflate(R.layout.dialog_add_folder, null);

            EditText editText = (EditText) renameView.findViewById(R.id.edit_name);
            editText.addTextChangedListener(this);

            editText.setTag(parent);

            editToast = (TextView) renameView.findViewById(R.id.edit_toast);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            // int themeAsusLightDialogAlertId = ThemeUtility.sThemeAsusLightDialogAlertId;

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(R.string.new_folder_dialog)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();

            dialog.setView(renameView, spacing_left, spacing_top, spacing_right, spacing_bottom);
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            dialog = new ProgressDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
            dialog.setMessage(getResources().getString(R.string.add_progress));
            ((ProgressDialog) dialog).setIndeterminate(true);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setCancelable(false);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume");

        // We disabled "OK" button initially, then the button will be controlled after every text changed.
        if (mIsFirstCreate) {
            onEnableProceedButtons(false);
            mIsFirstCreate = false;
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mIsFirstCreateKey", mIsFirstCreate);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (getDialog() != null) {
            String text = s.toString().trim();
            boolean isSpecialChar = EditorUtility.isSpecialChar(text);
            boolean isNameTooLong = EditorUtility.isNameTooLong(text);

            s.setFilters(isNameTooLong ? new InputFilter[]{new InputFilter.LengthFilter(s.length())} : new InputFilter[]{});

            onEnableProceedButtons(!isSpecialChar && !isNameTooLong);

            if(!text.isEmpty() && isSpecialChar) {
                editToast.setText(getResources().getString(R.string.edit_toast_special_char));
                editToast.setVisibility(View.VISIBLE);
            }
            else if(isNameTooLong) {
                editToast.setText(getResources().getString(R.string.edit_toast_name_too_long));
                editToast.setVisibility(View.VISIBLE);
            }
            else {
                editToast.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE) {
            EditText editText = (EditText) ((AlertDialog) dialog).findViewById(R.id.edit_name);
            String folderName = editText.getText().toString().trim();
            VFile parent = (VFile) editText.getTag();

            switch (parent.getVFieType()) {
                case VFileType.TYPE_LOCAL_STORAGE:
                    createFolder(editText.getContext(),parent,folderName);
                    break;
                case VFileType.TYPE_CLOUD_STORAGE:
                    Log.v("Johnson", "add folder => parent id: " + ((RemoteVFile)parent).getFileID());
                    int msgType = -1;
                    switch(((RemoteVFile)parent).getStorageType()) {
                        case StorageType.TYPE_GOOGLE_DRIVE:
                            msgType = MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                            break;
                        case StorageType.TYPE_DROPBOX:
                            msgType = MsgObj.TYPE_DROPBOX_STORAGE;
                            break;
                        case StorageType.TYPE_BAIDUPCS:
                            msgType = MsgObj.TYPE_BAIDUPCS_STORAGE;
                            break;
                        case StorageType.TYPE_SKYDRIVE:
                            msgType = MsgObj.TYPE_SKYDRIVE_STORAGE;
                            break;
                        case StorageType.TYPE_ASUSWEBSTORAGE:
                            msgType = MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
                            break;
                        case StorageType.TYPE_HOME_CLOUD:
                            msgType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                            break;
                        case StorageType.TYPE_YANDEX:
                            msgType = MsgObj.TYPE_YANDEX_STORAGE;
                            break;
                        default:
                            Log.w(TAG, "you should define remote storage type");
                    }
                    Log.d(TAG,"want to create folder at: " + parent.getAbsolutePath() + " and parent file type: " + parent.getVFieType()
                            + " , it maybe send msg obj type: " + msgType);

                    RemoteVFile[] remoteVFiles = new RemoteVFile[1];
                    if (msgType != MsgObj.TYPE_HOMECLOUD_STORAGE) {
                        remoteVFiles[0] = new RemoteVFile("/" + ((RemoteVFile)parent).getStorageName() + "/" + folderName);
                    } else {
                        remoteVFiles[0] = new RemoteVFile("/" + ((RemoteVFile)parent).getStorageName() + "/"+RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile)parent).getmDeviceId()).getDeviceName()+File.separator + folderName);
                    }
                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(((RemoteVFile)parent).getStorageName(), parent, remoteVFiles, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_CREATE_FOLDER);
                    ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS, remoteVFiles[0]);
                        break;
                case VFileType.TYPE_SAMBA_STORAGE:
                    SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.ADD_NEW_FOLDER, parent.getAbsolutePath(), folderName, null, null, null, 0,-1,null);
                    ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS, null);
                    break;
            }

            GaMenuItem.getInstance().sendEvents(getActivity(), GaMenuItem.CATEGORY_NAME,
                        GaMenuItem.ACTION_ADD_FOLDER, null, null);
        }
    }

    // ++ Willie
    // Set the "OK" button to be enabled or disabled.
    private void onEnableProceedButtons(boolean enabled) {
        if (getDialog() != null) {
            Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(enabled);
        }
    }

    // create function
    public void createFolder(Context context, VFile parent, String folderName) {
        int result = doCreateFolder(parent, folderName);

        switch (result) {
            case EditResult.E_EXIST:
                ToastUtility.show(context, R.string.target_exist, Toast.LENGTH_LONG);
                break;
            case EditResult.E_PERMISSION:
                ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
                break;
            case EditResult.E_FAILURE:
                ToastUtility.show(context, R.string.new_folder_fail, Toast.LENGTH_LONG);
                break;
            case EditResult.E_NOSPC:
                ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
                break;
            case EditResult.E_SUCCESS:
                ToastUtility.show(context, R.string.new_folder_success, folderName);
                /*ShortCutFragment fragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(
                        FileManagerActivity.sShortCutFragmentTag);
                if (fragment != null) {
                    fragment.updateTreeFromAddNewFolder(new LocalVFile(parent, folderName).getAbsolutePath());
                }*/
                if (((FileManagerActivity) getActivity()).isMoveToDialogShowing()) {
                    MoveToDialogFragment moveToDialogFragment = (MoveToDialogFragment) getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                    if (moveToDialogFragment != null) {
                        moveToDialogFragment.startScanFile(new LocalVFile(parent), ScanType.SCAN_CHILD);
                    }
                } else if (((FileManagerActivity) getActivity()).isFilePickerDialogShowing()) {
                    FilePickerDialogFragment filePickerDialogFragment= (FilePickerDialogFragment) getFragmentManager().findFragmentByTag(FilePickerDialogFragment.DIALOG_TAG);
                    if (filePickerDialogFragment != null) {
                        filePickerDialogFragment.startScanFile(new LocalVFile(parent), ScanType.SCAN_CHILD);
                    }

                } else {
                    FileListFragment fileListFragment = (FileListFragment)getFragmentManager().findFragmentById(R.id.filelist);
                    if (fileListFragment != null) {
                        fileListFragment.startScanFile(new LocalVFile(parent), ScanType.SCAN_CHILD);
                    }
                }
                break;
            default:
                break;
        }
    }

    private int doCreateFolder(VFile parent, String folderName) {
        int result;
        VFile newFolder = new VFile(parent, folderName);
        
        if (parent == null || EditorUtility.isSpecialChar(folderName)) {
            result = EditResult.E_FAILURE;
        } else if (newFolder.exists() && newFolder.isDirectory()) {
            result = EditResult.E_EXIST;
        } else {
            boolean success = false;

            if(SafOperationUtility.getInstance().isNeedToWriteSdBySaf(parent.getAbsolutePath())) {
                DocumentFile destparent = SafOperationUtility.getInstance().getDocFileFromPath(parent.getAbsolutePath());
                if(destparent != null){
                    success = destparent.createDirectory(folderName) != null;
                }
            } else {
                // create parent folder if need
                if(!parent.exists())
                    parent.mkdir();

                if (!parent.canWrite()) {
                    result = EditResult.E_PERMISSION;
                }else{
                    success = newFolder.mkdir();
                }
            }

            if (success) {
                // notify change to media provider
                MediaProviderAsyncHelper.addFolder(newFolder);

                result = EditResult.E_SUCCESS;
            } else {
                if (parent.getUsableSpace() < MIN_FOLDER_SIZE) {
                    Log.w(TAG,"no space");
                    result = EditResult.E_NOSPC;
                } else {
                    result = EditResult.E_FAILURE;
                }
            }
        }

        return result;
    }
}
