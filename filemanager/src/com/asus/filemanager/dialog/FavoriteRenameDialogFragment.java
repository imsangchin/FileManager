package com.asus.filemanager.dialog;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
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

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;

public class FavoriteRenameDialogFragment extends DialogFragment implements TextWatcher, OnClickListener {

    public static final int TYPE_FAVORITE_RENAME_DIALOG = 0;
    public static final int TYPE_FAVORITE_RENAME_NOTICE_DIALOG = 1;
    private static final String TAG = "RenameFavoriteDialogFragment";
    private static final boolean DEBUG = true;

    private int mType;
    private VFile mFile;
    private TextView editToast;

    public static FavoriteRenameDialogFragment newInstance(VFile file, int typeDialog) {
        FavoriteRenameDialogFragment fragment = new FavoriteRenameDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = (VFile) getArguments().getSerializable("file");
        mType = getArguments().getInt("type");

        if (mType == TYPE_FAVORITE_RENAME_DIALOG) {
            if (DEBUG) {
                Log.d(TAG, "FavoriteRenameDialog : " + mFile.getAbsolutePath());
            }
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View renameView = inflater.inflate(R.layout.dialog_rename, null);

            EditText editText = (EditText) renameView.findViewById(R.id.edit_name);
            editText.addTextChangedListener(this);

            editToast = (TextView) renameView.findViewById(R.id.edit_toast);

            String name = mFile.isFavoriteFile() ? mFile.getFavoriteName() : mFile.getName();
            editText.setText(name);
            editText.setSelectAllOnFocus(true);

            editText.setTag(mFile);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

            AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(R.string.rename_favorite_folder)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(R.string.cancel, this)
                    .create();

            dialog.setView(renameView, spacing_left, spacing_top, spacing_right, spacing_bottom);
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return dialog;
        } else{
            AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.rename_dialog)
            .setMessage(R.string.rename_favorite_notice)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(R.string.cancel, this)
            .create();
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
            if(mType == TYPE_FAVORITE_RENAME_DIALOG){
                EditText editText = (EditText) ((AlertDialog) dialog).findViewById(R.id.edit_name);
                String rename = editText.getText().toString().trim();
                VFile file = (VFile) editText.getTag();
                if(file != null) {
                    rename(rename, file);
                }
            } else{
                ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_FAVORITE_RENAME_DIALOG, mFile);
            }
        }else{
            dialog.cancel();
        }
    }

    // rename function
    public void rename(String rename, VFile file) {
        ContentResolver cr = getActivity().getContentResolver();
        boolean isExists = ProviderUtility.FavoriteFiles.exists(cr, rename);

        if(isExists){
            ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_FAVORITE_RENAME_NOTICE_DIALOG, mFile);
        } else{
            String path = null;
            try {
                path = FileUtility.getCanonicalPathForUser(file.getCanonicalPath());
            } catch (IOException e2) {
                path = file.getAbsolutePath();
                e2.printStackTrace();
            }
            if(ProviderUtility.FavoriteFiles.insertFile(cr, rename, path) != null){
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                FileManagerActivity activity = (FileManagerActivity) getActivity();
                    if (activity != null && activity.getIsShowSearchFragment()) {
                        activity.reSearch(activity.getSearchQueryKey());
                    } else {
                        fileListFragment.reScanFile();
                    }
                }
                ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.rename_favorite_success));
            }
        }
    }
}
