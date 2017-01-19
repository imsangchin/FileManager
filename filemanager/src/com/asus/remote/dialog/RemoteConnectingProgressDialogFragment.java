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

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class RemoteConnectingProgressDialogFragment extends DialogFragment implements OnClickListener{
    private static final String TAG = "RemoteConnectingProgressDialogFragment";
    VFile mFile;

    public static RemoteConnectingProgressDialogFragment newInstance(VFile file) {
        RemoteConnectingProgressDialogFragment fragment = new RemoteConnectingProgressDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = (VFile) getArguments().getSerializable("file");

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View progress_view = inflater.inflate(R.layout.dialog_remote_connecting_progress, null);

        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.wifidirect_connecting_hint_title)
            /*TODO: setView
            .setView(progress_view, 32, 8, 32, 8)
            */
            .setView(progress_view)
            .setNegativeButton(android.R.string.cancel, this)
            .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
        if (shortcutFragment != null) {
            shortcutFragment.sendRemoteStorage(mFile, CloudStorageServiceHandlerMsg.MSG_APP_CANCEL_CONNECT_DEVICE);
        } else {
            Log.w(TAG, "shortcutFragment is null when calling onClick");
        }
    }
}
