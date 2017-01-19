
package com.asus.filemanager.dialog;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Config;
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
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneVFile;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class RenameDialogFragment extends DialogFragment implements TextWatcher, OnClickListener, OnScanCompletedListener {

    public static final int TYPE_RENAME_DIALOG = 0;
    public static final int TYPE_ROGRESS_DIALOG = 1;
    private static final String TAG = "RenameDialogFragment";
    private static final boolean DEBUG = Config.DEBUG;

    private TextView editToast;
    private FileManagerActivity mActivity;

    public static RenameDialogFragment newInstance(VFile file, int typeDialog) {
        RenameDialogFragment fragment = new RenameDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        VFile file = (VFile) getArguments().getSerializable("file");
        int type = getArguments().getInt("type");

        if (type == TYPE_RENAME_DIALOG) {
            if (DEBUG) {
                Log.d(TAG, "RenameDialog : " + file.getAbsolutePath());
            }
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View renameView = inflater.inflate(R.layout.dialog_rename, null);

            EditText editText = (EditText) renameView.findViewById(R.id.edit_name);
            editText.addTextChangedListener(this);

            editToast = (TextView) renameView.findViewById(R.id.edit_toast);
            mActivity = null;

            String name = file.getName();
            editText.setText(name);
            if (file.isDirectory())
                editText.setSelectAllOnFocus(true);
            else
                //editText.setSelection(name.lastIndexOf(".") == -1 ? name.length() : name.lastIndexOf("."));
                editText.setSelection(0, name.lastIndexOf(".") == -1 ? name.length() : name.lastIndexOf("."));
            editText.setTag(file);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);


            AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(R.string.rename)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(R.string.cancel, this)
                    .create();

            dialog.setView(renameView, spacing_left, spacing_top, spacing_right, spacing_bottom);
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return dialog;
        } else {
            Dialog dialog = new ProgressDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
            ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.renaming_file));
            ((ProgressDialog) dialog).setIndeterminate(true);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setCancelable(false);

            return dialog;
        }
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

            Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(!isSpecialChar && !isNameTooLong);

            if(!text.isEmpty() && isSpecialChar){
                editToast.setText(getResources().getString(R.string.edit_toast_special_char));
                editToast.setVisibility(View.VISIBLE);
            }
            else if(isNameTooLong){
                editToast.setText(getResources().getString(R.string.edit_toast_name_too_long));
                editToast.setVisibility(View.VISIBLE);
            }
            else{
                editToast.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE) {
            EditText editText = (EditText) ((AlertDialog) dialog).findViewById(R.id.edit_name);
            String rename = editText.getText().toString().trim();
            VFile file = (VFile) editText.getTag();
            if(file!=null && !rename.equals(file.getName())) {
                rename(editText.getContext(),file,rename);
                if(null != mActivity){
                    GaAccessFile.getInstance().sendEvents(mActivity,
                        GaAccessFile.ACTION_RENAME, file.getVFieType(), -1, 1);
                }
            }
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        if (null == mActivity) {
            Log.e(TAG, "activity was gone, skip onScanCompleted");
            return ;
        }
        // The below process need to run on the ui thread.
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finishRename(mActivity);
                mActivity = null;
            }
        });
    }

    // rename function
    public void rename(Context context, VFile file, String rename) {
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            String account = ((RemoteVFile)file).getStorageName();
            int type = ((RemoteVFile)file).getMsgObjType();
            RemoteVFile[] remoteVFiles = new RemoteVFile[1];
            // parent file is not important, we use fake parent path: /device_name/xxx
            remoteVFiles[0] = new RemoteVFile(File.separatorChar + account + File.separatorChar + rename);
            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(account, file, remoteVFiles, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_RENAME_FILE);

            ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_RENAME_PROGRESS_DIALOG, null);
        }
        //++yiqiu_huang, samba case
        else if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
            String indicatorPath = ((SambaVFile)file).getIndicatorPath();
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(getActivity());
            String rootPath = sambaFileUtility.getRootScanPath();
            String[] folderStrs = indicatorPath.trim().substring(1).split("/");
            String tmp = "";
            int smbcount = folderStrs.length;
            if (smbcount <= 1){

            }else{
                for (int i=0;i < smbcount - 1;i++){
                    tmp += (folderStrs[i] + File.separatorChar);
                }
            }
            sambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_RENAME, rootPath + tmp, rename, file.getName(), null, null, 0,-1,null);
            ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_RENAME_PROGRESS_DIALOG, null);
        }
        else {
            // local file case
            int result = doRename(file,rename);;

            if (result == EditResult.E_EXIST) {
                ToastUtility.show(context, R.string.target_exist, Toast.LENGTH_LONG);
            } else if (result == EditResult.E_PERMISSION) {
                ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
            } else if (result == EditResult.E_FAILURE) {
                ToastUtility.show(context, R.string.rename_fail, Toast.LENGTH_LONG);
            } else if (result == EditResult.E_SUCCESS) {
                // update recentlyopen db
                ProviderUtility.RecentlyOpen.rename(
                    getActivity().getContentResolver(), file.getPath(),
                    rename, FilenameUtils.concat(file.getParent(), rename)
                );
                // prepare to rescan
                FileListFragment fileListFragment = (FileListFragment)getFragmentManager().findFragmentById(R.id.filelist);
                // hold the temp activity reference to access it since getActivity() will return null at onScanCompleted
                mActivity = (FileManagerActivity)getActivity();
                if (fileListFragment.belongToCategoryFromMediaStore()) {
                    // update the new file to media store
                    MediaScannerConnection.scanFile(
                        mActivity,
                        new String[] { FilenameUtils.concat(file.getParent(), rename) }, null,
                        this
                    );
                } else {
                    finishRename((FileManagerActivity)getActivity());
                }
            }
        }
    }

    private static void finishRename(FileManagerActivity activity) {
        if (activity.getCurrentFragmentType() == FileManagerActivity.FragmentType.FILE_LIST) {
        FileListFragment fileListFragment = (FileListFragment) activity.getFragmentManager().findFragmentById(R.id.filelist);
        if (null == fileListFragment) {
            Log.e(TAG, "fileListFragment was gone, can not finishRename");
            return ;
        }
        if (fileListFragment.belongToCategoryFromMediaStore()) {
            fileListFragment.startScanCategory(fileListFragment.getIndicatorFile(), activity.getIsShowSearchFragment());
        } else {
            if (activity.getIsShowSearchFragment()){
                activity.reSearch(activity.getSearchQueryKey());
            } else {
                fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD);
            }
        }
        } else {
            activity.getEditHandler().sendEmptyMessage(FileListFragment.MSG_RENAME_COMPLETE);
        }
    }

    private int doRename(VFile file, String rename) {
        int result = EditResult.E_SUCCESS;

        if (file == null) {
            result = EditResult.E_FAILURE;
            return result;
        }

        String fileName = file.getName();
        VFile renameFile = new VFile(file.getParent(), rename);

        if (EditorUtility.isSpecialChar(rename)) {
            result = EditResult.E_FAILURE;
        } else if (file.getName().equals(rename)){
            result = EditResult.E_SUCCESS;
        } else if (renameFile.exists() && !fileName.equalsIgnoreCase(rename)) {
            result = EditResult.E_EXIST;
        } else {
            if(SafOperationUtility.getInstance().isNeedToWriteSdBySaf(file.getAbsolutePath())
                    && !(file instanceof HiddenZoneVFile)){
                DocumentFile destFile = SafOperationUtility.getInstance().getDocFileFromPath(file.getAbsolutePath());
                if(destFile != null){
                    boolean succussed = false;
                    succussed = destFile.renameTo(rename);
                    if (!succussed){
                        result = EditResult.E_FAILURE;
                    }
                }
            }else{
                if (file.getParentFile() != null && !file.getParentFile().canWrite()
                        && !(file instanceof HiddenZoneVFile)) {
                    result = EditResult.E_PERMISSION;
                }else{
                    boolean succussed = false;
                    succussed = file.renameTo(renameFile);
                    if (!succussed){
                        result = EditResult.E_FAILURE;
                    }
                }
            }
            if (result == EditResult.E_SUCCESS) {
                FileManagerActivity activity = (FileManagerActivity)getActivity();
                if (null == activity) {
                    Log.e(TAG, "activity was gone, skip doRename");
                    return result;
                }

                FileListFragment fileListFragment = (FileListFragment) activity.getFragmentManager().findFragmentById(R.id.filelist);

                // notify change to media provider
                if(!fileName.equalsIgnoreCase(rename))
                    MediaProviderAsyncHelper.rename(file, renameFile,
                            fileListFragment != null ? fileListFragment.belongToCategoryFromMediaStore() : false);
            }
        }

        return result;
    }
}
