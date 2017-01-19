package com.asus.remote.dialog;

import android.app.Activity;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

import java.util.concurrent.TimeUnit;

public class RemoteFilePasteDialogFramgment extends DialogFragment implements OnClickListener{
	public static final String TAG = "RemoteFilePasteDialogFramgment";
	public static final int PASTE_DOWNLOAD = 1;
	public static final int PASTE_UPLOAD = 2;
	public static final int NOW_IS_COPY = 1;
	public static final int NOW_IS_DELETE = 2;
	private static final String EDITPOOL_KEY = "editpool_key";
    private int mPasteAction = -1;
    private String mCloudStorageName="";
    private int mCloudStorageType = -1;
    private TextView txt_total_percent;
    private TextView txt_total_size;
   // private TextView txt_every_percent;
    private TextView txt_every__size;
    private ProgressBar progressBar_total;
    private ProgressBar progressBar_every;
    private TextView txt_operate_status;

    public static RemoteFilePasteDialogFramgment newInstance(EditPool arg) {
    	RemoteFilePasteDialogFramgment fragment = new RemoteFilePasteDialogFramgment();
        Bundle args = new Bundle();
        args.putSerializable(EDITPOOL_KEY, arg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	EditPool editPool = (EditPool) getArguments().getSerializable(EDITPOOL_KEY);
    	int filesCount = 0;
    	 if(editPool != null  && editPool.getFiles() != null){
    		 filesCount = editPool.getFiles().length;
             if (editPool.getFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                 mCloudStorageType = ((RemoteVFile)editPool.getFile()).getMsgObjType();
                 mCloudStorageName = ((RemoteVFile)editPool.getFile()).getStorageName();

                 mPasteAction = VFileType.TYPE_CLOUD_STORAGE;
             }else if (editPool.getFile().getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
             	mPasteAction = VFileType.TYPE_SAMBA_STORAGE;
             }
             if (editPool.getTargetDataType() == VFileType.TYPE_CLOUD_STORAGE) {
                 FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                 if (fileListFragment != null && (fileListFragment.getIndicatorFile() instanceof RemoteVFile)) {
                     mCloudStorageType = ((RemoteVFile)fileListFragment.getIndicatorFile()).getMsgObjType();
                     mCloudStorageName = ((RemoteVFile)fileListFragment.getIndicatorFile()).getStorageName();

                 }
                 mPasteAction = VFileType.TYPE_CLOUD_STORAGE;
             }else if (editPool.getTargetDataType() == VFileType.TYPE_SAMBA_STORAGE) {
             	mPasteAction = VFileType.TYPE_SAMBA_STORAGE;
             }
        }
    	int paste_type = editPool.getPasteDialogType();
    	AlertDialog dialog ;
    	LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	LinearLayout contentView = (LinearLayout) inflater.inflate(R.layout.dialog_remote_paste, null);
    	txt_total_percent = (TextView) contentView.findViewById(R.id.txt_total_percent);
    	txt_total_size = (TextView) contentView.findViewById(R.id.txt_total_size);
    	//txt_every_percent = (TextView) contentView.findViewById(R.id.txt_every_percent);
    	txt_every__size = (TextView) contentView.findViewById(R.id.txt_every_size);
    	txt_operate_status = (TextView) contentView.findViewById(R.id.txt_operate_status);
    	progressBar_total = (ProgressBar) contentView.findViewById(R.id.remote_progress_bar_total);

    	progressBar_every = (ProgressBar) contentView.findViewById(R.id.remote_progress_bar_every);
    	setProgress(0, filesCount, 0, 0);

    	dialog = new AlertDialog.Builder(getActivity(),ThemeUtility.getAsusAlertDialogThemeId())
    							.setTitle(getString(paste_type == PASTE_DOWNLOAD ?R.string.cloud_paste_downloading:R.string.cloud_paste_uploading))
    							.setNegativeButton(R.string.cancel, this)
    							.setPositiveButton(R.string.cloud_paste_backgroud, this)
    							.create();
    	dialog.setCanceledOnTouchOutside(false);
    	dialog.setView(contentView);


    	return dialog;
    }
	@Override
	public void onClick(DialogInterface arg0, int which) {
		switch (which) {
		case Dialog.BUTTON_NEGATIVE:
		     EditorAsyncHelper.setPasteFileTerminate();
		     EditorUtility.sEditIsProcessing = false;
		        Log.d(TAG,"onClick: cancel paste dialog");
		        Log.d(TAG," mPasteAction = " + mPasteAction);
		        switch (mPasteAction) {
		        case VFileType.TYPE_CLOUD_STORAGE:
		            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(mCloudStorageName, null, null, mCloudStorageType, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
		            break;
		        case VFileType.TYPE_SAMBA_STORAGE:
		        	SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_PASTE_CANCEL, null, null, null, null, null, 0, -1, null);
		        	break;
		        default:
		            break;
		        }
			dismissAllowingStateLoss();
			break;
		case Dialog.BUTTON_POSITIVE:
			dismissAllowingStateLoss();
			break;

		}

	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		super.onCancel(dialog);
        EditorAsyncHelper.setPasteFileTerminate();
        Log.d(TAG," onCancel: cancel paste dialog");
        Log.d(TAG," mPasteAction = " + mPasteAction);
        EditorUtility.sEditIsProcessing = false;
        switch (mPasteAction) {
        case VFileType.TYPE_CLOUD_STORAGE:
            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(mCloudStorageName, null, null, mCloudStorageType, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
            break;
        case VFileType.TYPE_SAMBA_STORAGE:
        	SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_PASTE_CANCEL, null, null, null, null, null, 0,-1,null);
        	break;
        default:
            break;
        }
	}
	public void setProgress(int currentFileCount,int totalFileCount,double currentFileSize,double currentFileTotalSize){
		setTotalProgress(currentFileCount, totalFileCount);
		setEveryProgress(currentFileSize, currentFileTotalSize);
	}
	public void setTotalProgress(int currentFileCount,int totalFileCount ){
		if(currentFileCount > 0){
			currentFileCount--;
		}
        if(progressBar_total != null && txt_total_percent != null && txt_total_size != null){
          String txtSize =	currentFileCount  +"/"+totalFileCount;
          String txtpercent =	(currentFileCount * 100 / totalFileCount ) + "%";
          txt_total_percent.setText(txtpercent);
          txt_total_size.setText(txtSize);
          progressBar_total.setMax(totalFileCount);
          progressBar_total.setProgress(currentFileCount);
        }
	}
	public  void setOperateStatus(int status){
		if (txt_operate_status != null) {
			int stringId = R.string.cloud_paste_copying;
			if (status == NOW_IS_COPY) {
				stringId = R.string.cloud_paste_copying;
			}else {
				stringId = R.string.cloud_paste_deleting;
			}
			txt_operate_status.setText(stringId);
		}
	}
	public void setEveryProgress(double currentFileSize,double currentFileTotalSize ){
		String format = FileUtility.bytes2String(getActivity().getApplicationContext(), currentFileSize, 2) + " / " + FileUtility.bytes2String(getActivity().getApplicationContext(), currentFileTotalSize, 2);
		  if(progressBar_total != null && txt_every__size != null ){
			   // txt_every_percent.setText((currentSize * 100 / (totalSize * 100)) + "%");
			    txt_every__size.setText(format);
			    progressBar_every.setProgress((int)((currentFileSize/currentFileTotalSize) * 100));
//	        	progressBar_every.setMax((int)(currentFileTotalSize/1000));
//	        	progressBar_every.setProgress((int)(currentFileSize/1000));
	        }
	}
}
