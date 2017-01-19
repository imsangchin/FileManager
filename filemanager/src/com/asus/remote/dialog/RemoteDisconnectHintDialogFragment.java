package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class RemoteDisconnectHintDialogFragment extends DialogFragment implements OnClickListener{
    private static final String TAG = "RemoteConnectHintDialogFragment";
    String mDeviceName;

    public static RemoteDisconnectHintDialogFragment newInstance(String name) {
        RemoteDisconnectHintDialogFragment fragment = new RemoteDisconnectHintDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("name", name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDeviceName = (String) getArguments().getSerializable("name");

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_remote_disconnect_hint, null);
        TextView viewHint = (TextView) view.findViewById(R.id.disconnect_hint);
        viewHint.setText(getResources().getString(R.string.wifidirect_disconnect_hint) + " " + mDeviceName + "?");

        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(this.getResources().getString(R.string.wifidirect_disconnect_hint_title))
            /*TODO: setView
            .setView(view, 32, 8, 32, 8)
             */
            .setView(view)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE) {
            ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
            if (shortcutFragment != null) {
                shortcutFragment.sendRemoteStorage(null, CloudStorageServiceHandlerMsg.MSG_APP_DISCONNECT_DEVICE);
            } else {
                Log.w(TAG, "shortcutFragment is null when calling onClick");
            }
        }
    }
}
