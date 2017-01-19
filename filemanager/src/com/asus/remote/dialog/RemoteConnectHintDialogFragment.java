package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class RemoteConnectHintDialogFragment extends DialogFragment implements OnClickListener {
    private static final String TAG = "RemoteConnectHintDialogFragment";
    VFile mFile;

    public static RemoteConnectHintDialogFragment newInstance(VFile file) {
        RemoteConnectHintDialogFragment fragment = new RemoteConnectHintDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = (VFile) getArguments().getSerializable("file");

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_remote_connect_hint, null);
        TextView viewHint = (TextView) view.findViewById(R.id.connect_hint);
        viewHint.setText(getResources().getString(R.string.wifidirect_connect_hint) + " " + ((RemoteVFile)mFile).getStorageName() + "?");

        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.wifidirect_connect_hint_title)
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
                shortcutFragment.sendRemoteStorage(mFile, CloudStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);

                FileListFragment fileListtFragment = (FileListFragment)  getFragmentManager().findFragmentById(R.id.filelist);
                Handler handler = fileListtFragment.getHandler();
                if (handler != null) {
                    handler.sendMessage(handler.obtainMessage(FileListFragment.MSG_CONNECTING_REMOTE_DIALOG, mFile));
                } else {
                    Log.w(TAG, "onClick get handler is null");
                }
            } else {
                Log.w(TAG, "shortcutFragment is null when calling onClick");
            }
        }
    }
}
