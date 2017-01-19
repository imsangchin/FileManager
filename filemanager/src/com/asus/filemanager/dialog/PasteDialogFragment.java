
package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

public class PasteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
	public static final String TAG = "PasteDialogFragment";
    private int mPercent;
    private String mFormat;
    private final String KEY_PERCENT = "percent";
    private final String KEY_FORMAT = "format";
    private int mPasteAction = -1;
    private String mCloudStorageName="";
    private String mCloudStorageId="";
    private int mCloudStorageType = -1;
    private int mDialogType = -1;
//    private boolean mIsLocalFileOperate = false;
    public static final int DIALOGTYPE_PREVIEW = 5;

    public static final String PREVIEW_DIALOG_PROCESS = "preview_process_dialog";

    public static PasteDialogFragment newInstance(EditPool arg) {
        PasteDialogFragment fragment = new PasteDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("editpool", arg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        EditPool editpool = (EditPool) getArguments().getSerializable("editpool");

        if(editpool != null && editpool.getSize() != 0){
        	mDialogType = editpool.getPasteDialogType();
            if (editpool.getFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                mCloudStorageType = ((RemoteVFile)editpool.getFile()).getMsgObjType();
                mCloudStorageName = ((RemoteVFile)editpool.getFile()).getStorageName();
                mCloudStorageId = ((RemoteVFile)editpool.getFile()).getStorageAddress();

                mPasteAction = VFileType.TYPE_CLOUD_STORAGE;
            }else if (editpool.getFile().getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
            	mPasteAction = VFileType.TYPE_SAMBA_STORAGE;
            }
//            else if (editpool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE && editpool.getTargetDataType() == VFileType.TYPE_LOCAL_STORAGE) {
//            	mIsLocalFileOperate = true;
//			}
            if (editpool.getTargetDataType() == VFileType.TYPE_CLOUD_STORAGE) {
            	
            	

            	MoveToDialogFragment moveToFragment = (MoveToDialogFragment)getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                if(moveToFragment != null){
            		mCloudStorageType = ((RemoteVFile)moveToFragment.getIndicatorFile()).getMsgObjType();
                    mCloudStorageName = ((RemoteVFile)moveToFragment.getIndicatorFile()).getStorageName();
                    mCloudStorageId = ((RemoteVFile)moveToFragment.getIndicatorFile()).getStorageAddress();

            	}
//            	else{
//            		FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
//                    if (fileListFragment != null) {
//                        mCloudStorageType = ((RemoteVFile)fileListFragment.getIndicatorFile()).getMsgObjType();
//                        mCloudStorageName = ((RemoteVFile)fileListFragment.getIndicatorFile()).getStorageName();
//                        mCloudStorageId = ((RemoteVFile)fileListFragment.getIndicatorFile()).getStorageAddress();
//                    }
//            	}
                mPasteAction = VFileType.TYPE_CLOUD_STORAGE;
            }else if (editpool.getTargetDataType() == VFileType.TYPE_SAMBA_STORAGE) {
            	mPasteAction = VFileType.TYPE_SAMBA_STORAGE;
            }
        }


        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        ProgressDialog pasteProgress = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);

        // keep the data if rotate the device
        if (savedInstanceState != null) {
            mPercent = savedInstanceState.getInt(KEY_PERCENT);
            mFormat = savedInstanceState.getString(KEY_FORMAT);
        }

        pasteProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pasteProgress.setMax(100);
        if (mDialogType == DIALOGTYPE_PREVIEW) {
        	pasteProgress.setMessage(getResources().getString(R.string.cloud_paste_downloading));
        	mPasteAction = VFileType.TYPE_CLOUD_STORAGE;
		}else {
			pasteProgress.setMessage(getResources().getString(R.string.paste_progress));
			pasteProgress.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), this);
//			if (!mIsLocalFileOperate && editpool != null) {
				pasteProgress.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.cloud_paste_backgroud), this);
//			}
		}
        pasteProgress.setCanceledOnTouchOutside(false);
        pasteProgress.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return pasteProgress;
    }

    public static void showCopyFileReady(Context context, VFile[] files, boolean delete) {
        if (files == null)
            return;
        if (files.length == 1) {
            ToastUtility.show(context, delete ? R.string.cut_ready_one : R.string.copy_ready_one, files[0].getName());
        } else if (files.length > 1) {
            ToastUtility.show(context, delete ? R.string.cut_ready_more : R.string.copy_ready_more, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        EditorAsyncHelper.setPasteFileTerminate();
        Log.d(TAG," cancel paste dialog");
        Log.d(TAG," mPasteAction = " + mPasteAction);
//        if (!mIsLocalFileOperate) {
        EditorUtility.sEditIsProcessing = false;
//        }
        /*switch (mPasteAction) {
        case VFileType.TYPE_CLOUD_STORAGE:
            RemoteFileUtility.sendCloudStorageMsg(mCloudStorageName, null, null, mCloudStorageType, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
            break;
        case VFileType.TYPE_SAMBA_STORAGE:
        	SambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_PASTE_CANCEL, null, null, null, null, null, 0,-1,null);
        	break;
        default:
            break;
        }*/

        if(mPasteAction > 0){
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(getActivity());
        	if(!sambaFileUtility.isBuilderEmpty()){
                sambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_PASTE_CANCEL, null, null, null, null, null, 0,-1,null);
        	}else {
        		RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(mCloudStorageName, null, null, mCloudStorageType, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
        	}
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_PERCENT, mPercent);
        outState.putString(KEY_FORMAT, mFormat);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getDialog() != null && mFormat != null) {
            ((ProgressDialog) getDialog()).setProgressNumberFormat(mFormat);
            ((ProgressDialog) getDialog()).setProgress(mPercent);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    	switch (which) {
		case Dialog.BUTTON_NEGATIVE:
			onCancel(dialog);
//		     EditorAsyncHelper.setPasteFileTerminate();
//		     if (!mIsLocalFileOperate) {
//		     EditorUtility.sEditIsProcessing = false;
////		     }
//		        switch (mPasteAction) {
//		        case VFileType.TYPE_CLOUD_STORAGE:
//		            RemoteFileUtility.sendCloudStorageMsg(mCloudStorageName, null, null, mCloudStorageType, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
//		            break;
//		        case VFileType.TYPE_SAMBA_STORAGE:
//		        	SambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_PASTE_CANCEL, null, null, null, null, null, 0,-1,null);
//		        	break;
//		        default:
//		            //RemoteFileUtility.sendRemoteMessage(null, CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);
//		            break;
//		        }
			break;
		case Dialog.BUTTON_POSITIVE:
			dismissAllowingStateLoss();
			break;
		default:
			break;
		}

    }

    public void setInitProgressByNotification(Context context,int percent, double countSize, double totalSize){
        countSize = countSize * 1024;//resume to right size.
        totalSize = totalSize * 1024;
        String format = FileUtility.bytes2String(context.getApplicationContext(), countSize, 2) + " / " + FileUtility.bytes2String(context.getApplicationContext(), totalSize, 2);
        mFormat = format;
        mPercent = percent;
        if(getDialog() != null) {
            ((ProgressDialog) getDialog()).setProgressNumberFormat(format);
            ((ProgressDialog) getDialog()).setProgress(percent);
        }
    }

    public void setProgress(int percent, double countSize, double totalSize) {
        String format = FileUtility.bytes2String(getActivity().getApplicationContext(), countSize, 2) + " / " + FileUtility.bytes2String(getActivity().getApplicationContext(), totalSize, 2);
        mFormat = format;
        mPercent = percent;
        if(getDialog() != null) {
            ((ProgressDialog) getDialog()).setProgressNumberFormat(format);
            ((ProgressDialog) getDialog()).setProgress(percent);
        }
    }

}
