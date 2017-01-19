package com.asus.remote.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class CloudStorageLoadingDialogFragment extends DialogFragment implements OnClickListener {

    public static final int TYPE_LOGGING_DIALOG = 0;
    public static final int TYPE_LOADING_DIALOG = 1;
    public static final int TYPE_RETRIEVING_URL =2;
    private int type = TYPE_LOGGING_DIALOG;

    public static CloudStorageLoadingDialogFragment newInstance(int typeDialog) {
        CloudStorageLoadingDialogFragment fragment= new CloudStorageLoadingDialogFragment();
        Bundle args = new Bundle();
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog;
         type = getArguments().getInt("type");

        dialog = new ProgressDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT);

        if (type == TYPE_LOGGING_DIALOG) {
            ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.logging_cloud_storage));
        } else if (type == TYPE_LOADING_DIALOG) {
            ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.loading));
        }else if (type == TYPE_RETRIEVING_URL) {
        	((ProgressDialog) dialog).setMessage(getResources().getString(R.string.dialog_load_stream_url_info));
		}

        ((ProgressDialog) dialog).setIndeterminate(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dialog.setCanceledOnTouchOutside(false);
        setCancelable(true);

        return dialog;

    }

	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		if (type == TYPE_LOADING_DIALOG) {
			RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(null, null, null, -1, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
		}
		super.onCancel(dialog);

	}
    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}
