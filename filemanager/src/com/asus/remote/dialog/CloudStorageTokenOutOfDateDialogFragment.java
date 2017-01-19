package com.asus.remote.dialog;

import android.R.raw;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.remote.utility.RemoteAccountUtility;

public class CloudStorageTokenOutOfDateDialogFragment extends DialogFragment implements DialogInterface.OnClickListener{
public static  final String TAG = "CloudStorageTokenOutOfDateDialogFragment";
private int msgType;
private String accountName;
public static CloudStorageTokenOutOfDateDialogFragment newInstance(int msgType,String accountName){
	CloudStorageTokenOutOfDateDialogFragment fragment = new CloudStorageTokenOutOfDateDialogFragment();
	Bundle args = new Bundle();
	args.putInt("msgType", msgType);
	args.putString("accountName", accountName);
	fragment.setArguments(args);
	return fragment;
}
@Override
public Dialog onCreateDialog(Bundle savedInstanceState) {
	// TODO Auto-generated method stub
	Dialog dialog;
	msgType = getArguments().getInt("msgType");
	accountName = getArguments().getString("accountName");
	dialog = new AlertDialog.Builder(getActivity()).setMessage(getResources().getString(R.string.cloud_token_invalidate)).setPositiveButton(getResources().getString(R.string.ok), this).setNegativeButton(getResources().getString(R.string.cancel), this).create();
	 dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	setCancelable(false);

	return dialog;
}
@Override
public void onClick(DialogInterface arg0, int which) {
	switch (which) {
	case DialogInterface.BUTTON_POSITIVE:
		//RemoteAccountUtility.addAccount(msgType);
        RemoteAccountUtility.getInstance(getActivity()).refreshToken(msgType, accountName);
		dismissAllowingStateLoss();
		break;
    case DialogInterface.BUTTON_NEGATIVE:
    FileListFragment	fileListFragment = (FileListFragment) (getActivity().getFragmentManager().findFragmentById(R.id.filelist));
	    if (fileListFragment!=null) {
	    	fileListFragment.backToDefaultPath();
	    	/*fileListFragment.updateNofileLayout(3);
			fileListFragment.setListShown(true);*/
		}
        RemoteAccountUtility.getInstance(getActivity()).remoteAccountToken(msgType);
    	dismissAllowingStateLoss();
	default:
		break;
	}

}

}
