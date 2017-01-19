package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class RequestSDPermissionDialogFragment extends DialogFragment{

    public final static String DIALOG_TAG = "RequestSDPermissionDialogFragment";
    public int action;
    public String diskLabel;
    public interface OnRequestSDPermissionFragmentListener {
        void onRequestConfirmed(int action, String deskLabel);
        void onRequestDenied();
    }
    private OnRequestSDPermissionFragmentListener mListener;

    public static RequestSDPermissionDialogFragment newInstance(int newAction, String diskLabel) {
        RequestSDPermissionDialogFragment fragment = new RequestSDPermissionDialogFragment();
        fragment.action = newAction;
        fragment.diskLabel = diskLabel;
        return fragment;
    }

    public static RequestSDPermissionDialogFragment newInstance(int newAction) {
        RequestSDPermissionDialogFragment fragment = new RequestSDPermissionDialogFragment();
        fragment.action = newAction;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View about_view = inflater.inflate(R.layout.dialog_warn_saf_sd_permission, null);
        String aTitle = getActivity().getResources().getString(R.string.saf_tutorial_title);

        if (!TextUtils.isEmpty(diskLabel)){
            TextView tvMsg= (TextView)about_view.findViewById(R.id.msg);
            if (null != tvMsg){
                String aText = getActivity().getResources().getString(R.string.saf_permission_tutorial_content_v2, diskLabel);
                tvMsg.setText(aText);
                aTitle = getActivity().getResources().getString(R.string.saf_tutorial_title_v2, diskLabel);
            }
        }
        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);


        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(aTitle)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (null != mListener) {
                        mListener.onRequestConfirmed(action,diskLabel);
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onRequestDenied();
                    }
                }
            })
            .create();

        dialog.setView(about_view, spacing_left, spacing_top, spacing_right, spacing_bottom);

        return dialog;
    }
    @Override
    public void show(FragmentManager manager, String tag) {
        manager.executePendingTransactions();
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnRequestSDPermissionFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRequestSDPermissionFragmentListener");
        }
    }
}
