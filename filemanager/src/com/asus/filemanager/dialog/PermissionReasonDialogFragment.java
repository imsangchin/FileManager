package com.asus.filemanager.dialog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.AddCloudAccountActivity;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.permission.PermissionDialog;
import com.asus.filemanager.utility.permission.PermissionManager;

import java.util.ArrayList;

public class PermissionReasonDialogFragment extends DialogFragment{

    public static final String TAG = "PermissionReasonDialogFragment";
    public static final String KEY_REQUIRED_PERMISSION = "required_permission";

    public static PermissionReasonDialogFragment newInstance(String[] required_permission) {
        PermissionReasonDialogFragment fragment = new PermissionReasonDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArray(KEY_REQUIRED_PERMISSION, required_permission);
        fragment.setArguments(bundle);

        return fragment;
    }

    private String mPermissionGroup;

    public PermissionReasonDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String[] permissions = getArguments().getStringArray(KEY_REQUIRED_PERMISSION);
        Activity activity = getActivity();
        for (String str : permissions){
            if (str.compareToIgnoreCase(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==0 ||
                str.compareToIgnoreCase(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 0) {
                mPermissionGroup = PermissionDialog.getPermissionGroupInfo(activity,
                    PermissionDialog.getPermissionInfo(activity,str));
                break;
            }
            if (str.compareToIgnoreCase(android.Manifest.permission.GET_ACCOUNTS) == 0){
                mPermissionGroup = PermissionDialog.getPermissionGroupInfo(activity,
                    PermissionDialog.getPermissionInfo(activity,str));
                break;
            }
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View about_view = inflater.inflate(R.layout.forced_permission, null);

        String msg = getString(R.string.m_permission_dialog_message, getActivity().getString(R.string.file_manager),mPermissionGroup);
        ((TextView) about_view.findViewById(R.id.m_permission_dialog_message)).setText(msg);

        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.file_manager)
            .setPositiveButton(R.string.m_permission_dialog_positive_button, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Activity aHost = getActivity();
                    if (null == aHost || (!(aHost instanceof FileManagerActivity) && !(aHost instanceof AddCloudAccountActivity)))
                        return;
                    dismissAllowingStateLoss();

                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + aHost.getPackageName()));
                    intent.putExtra(":settings:fragment_args_key", "permission_settings");
                    intent.putExtra(":settings:fragment_args_key_highlight_times", 3);
                    try {
                        startActivityForResult(intent,FileManagerActivity.FILE_MANAGER_SETTING_PERMISSION);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissAllowingStateLoss();
                }
            }
            )
            .create();
        dialog.setView(about_view, spacing_left, spacing_top, spacing_right, spacing_bottom);

        dialog.setCancelable(true);
        setCancelable(true);

        return dialog;
    }
}
