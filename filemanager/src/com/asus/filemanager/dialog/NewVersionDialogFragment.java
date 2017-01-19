package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

public class NewVersionDialogFragment extends DialogFragment{

    public final static String DIALOG_TAG = "NewVersionDialogFragment";

    public interface OnNewVersionDialogFragmentListener {
        public void onOnNewVersionDialogConfirmed();
        public void onOnNewVersionDismissed();
    }
    private OnNewVersionDialogFragmentListener mListener;

    public static NewVersionDialogFragment newInstance() {
        NewVersionDialogFragment fragment = new NewVersionDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View about_view = inflater.inflate(R.layout.dialog_new_version_notify, null);

        //String msg = getString(R.string.kk_sd_permission_warning);
        //((TextView) about_view.findViewById(R.id.msg)).setText(msg);

        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);


        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.newversion_title)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onOnNewVersionDialogConfirmed();
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onOnNewVersionDismissed();
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
            mListener = (OnNewVersionDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRecommandDialogFragmentListener");
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != mListener){
            mListener.onOnNewVersionDismissed();
        }
    }
}
