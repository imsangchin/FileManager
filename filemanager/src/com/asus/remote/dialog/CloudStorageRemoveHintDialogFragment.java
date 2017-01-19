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
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteVFile;

public class CloudStorageRemoveHintDialogFragment extends DialogFragment implements OnClickListener{
    private static final String TAG = "CloudStorageRemoveHintDialogFragment";
    VFile file;
    String mAccountName;

    public static CloudStorageRemoveHintDialogFragment newInstance(VFile file) {
        CloudStorageRemoveHintDialogFragment fragment = new CloudStorageRemoveHintDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        file = (VFile) getArguments().getSerializable("file");
        mAccountName = "";
        String cloudname = "";
        if (file != null && file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            mAccountName = ((RemoteVFile)file).getStorageName();
            cloudname = RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), ((RemoteVFile) file).getMsgObjType());
        } else {
            Log.d(TAG, "cannot get the accout name");
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_cloud_storage_remove_hint, null);
        TextView viewHint = (TextView) view.findViewById(R.id.remove_hint);
        viewHint.setText(getResources().getString(R.string.cloud_storage_sign_out, cloudname));

        return new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(this.getResources().getString(R.string.cloud_storage_sign_out_title))
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
            if (file != null && file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            	removeAccount();

                if(getFragmentManager() != null) {
	                FileListFragment fileListFragment = (FileListFragment)getFragmentManager().findFragmentById(R.id.filelist);
	                if (fileListFragment != null) {
						fileListFragment.startScanFile(new LocalVFile(FileListFragment.DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
					}
                }

            } else {
                Log.d(TAG, "cannot update msg type because file type is not cloud storage case");
            }
           // RemoteFileUtility.sendCloudStorageMsg(mAccountName, null, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_DISCONNECT_DEVICE);
            //RemoteAccountUtility.removeAccountFromShortCutFragment(mAccountName,msgType,getActivity());
        }
    }

    private void removeAccount(){
    	Thread unMountThread = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				int msgType = ((RemoteVFile)file).getMsgObjType();
                RemoteAccountUtility.getInstance(getActivity()).removeAccount(getActivity(), msgType, mAccountName);
			}

		});
		unMountThread.start();
	}
}
