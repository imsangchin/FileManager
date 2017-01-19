
package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Notification.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FixedListFragment;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDialogFragment extends DialogFragment implements TextWatcher, OnClickListener {

    private static final boolean DEBUG = Config.DEBUG;
    private static final String TAG = "ZipDialogFragment";

    public static final int TYPE_ZIP_DIALOG = 0;
    public static final int TYPE_PROGRESS_DIALOG = 1;

    protected static final int MSG_UPDATE_PROGRESS = 0;
    public static final int MSG_COMPLETE = 1;

    private static final int BUFFER = 4096;

    private int mType;
    private EditPool mEditpool;

    private View mZipView;
    private EditText editText;
    private VFile mNewZipFile = null;

    private TextView editToast;

    private boolean cancel = false;
//    private boolean isFirst = true;
//    private boolean isFinish = false;
//    private static boolean isNewCreated = true;

    private ZipRunnable mRunnable;
    private Thread mThread;
    private int mProgressValue = 0;

    private static NotificationManager manager = null;
    private static FileManagerActivity mContext = null;
    private Builder builder = null;
    private static final int ZIP_NOTIFICATION_ID = 715;
    private static final int CALL_BY_NOTI_FLAG = 714;

    private int LAST_PROGRESS = 0;

    private class ProgressInfo {
        public int progress;
        public double writeSize;
        public double numSize;
        public int seq_num;
    }

    private class ZipRunnable implements Runnable {

        private boolean isStop = false;
        private long totalLength;

        private double mWriteSize = 0;

        @Override
        public void run() {

            Log.d(TAG, "zip name : " + mEditpool.getZipName());

            VFile[] files = mEditpool.getFiles();
            LocalVFile zipFile = new LocalVFile(files[0].getParent(), mEditpool.getZipName());
            mNewZipFile = zipFile;
            EditResult result = new EditResult();

            totalLength = FileUtility.getArrayTotalLength(files);

            if (isStop) {
                result.ECODE = EditResult.E_FAILURE;
            }

            int seq_num = 0;
            if (zipFile.exists()) {
                result.ECODE = EditResult.E_EXIST;
            } else {

                Log.i(TAG, "start compress ...");
                ////
                long startTime = 0;
                long endTime = 0;
                startTime = System.currentTimeMillis();

                seq_num = (int)startTime;
                result = zip(files, zipFile, seq_num);
                endTime = System.currentTimeMillis();
                Log.i(TAG,"unzip time : " + (endTime - startTime) + " ms");
                ////
                Log.i(TAG, "finish compress ...");

                if (result.ECODE == EditResult.E_FAILURE)
                    zipFile.delete();
            }

            if (result.ECODE == EditResult.E_SUCCESS) {
                MediaProviderAsyncHelper.addFile(zipFile, false);
            }

            Message msg = mHandler.obtainMessage(MSG_COMPLETE);
            msg.arg1 = result.ECODE;
            msg.arg2 = seq_num;
            mHandler.sendMessage(msg);
//            isFinish = true;
        }

        private EditResult zip(VFile[] files, VFile zipFile, int seq_num) {

            Log.i(TAG, "start compress + seq " + seq_num);
            EditResult result = new EditResult();

            result.ECODE = result.E_SUCCESS;
        	SafOperationUtility mSaf = null;
    		DocumentFile destFile = null;
        	mSaf = SafOperationUtility.getInstance();
        	boolean useSaf = mSaf.isNeedToWriteSdBySaf(zipFile.getAbsolutePath());
            try {
            	BufferedOutputStream dest = null;
            	if(useSaf){

            		DocumentFile destFileParent = mSaf.getDocFileFromPath(zipFile.getParentFile().getAbsolutePath());
            		if(destFileParent != null){
            		    destFile = destFileParent.createFile("*/*", mEditpool.getZipName());
            		}
            		if(destFile != null){
            		    dest = mSaf.getDocFileOutputStream(destFile);
            		}
            	}else{
                    dest = new BufferedOutputStream(new FileOutputStream(zipFile));
            	}

                ZipOutputStream out = null;
                if (dest != null)
                    out = new ZipOutputStream(dest);
                else
                    result.ECODE = EditResult.E_FAILURE;

                if (out != null) {
                    try {
                        out.setLevel(5);
                        doZip(files, out, zipFile,seq_num);
                    } finally {
                        out.close();
                        if(useSaf){
                        	mSaf.closeParcelFile();
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                result.ECODE = EditResult.E_FAILURE;
                if(destFile != null){
                	destFile.delete();
                }
            }

            return result;
        }

        private void doZip(VFile[] files, ZipOutputStream out, VFile zipFile, int seq_num) throws IOException {
            byte data[] = new byte[BUFFER];
            BufferedInputStream origin = null;
            String path;
            ZipEntry entry;
            ProgressInfo progressInfo = new ProgressInfo();
            progressInfo.seq_num = seq_num;
            updateProgress(progressInfo);
            for (int i = 0; i < files.length; i++) {
                if (isStop) {
                    throw new IOException("cancel thread");
                }
                if (!files[i].isDirectory()) {
                    if (DEBUG)
                        Log.v(TAG, "Adding: " + files[i]);

                    FileInputStream fi = new FileInputStream(files[i]);
                    origin = new BufferedInputStream(fi, BUFFER);
                    path = files[i].getAbsolutePath().substring(zipFile.getParentFile().getAbsolutePath().length() + 1);
                    entry = new ZipEntry(path);
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        if (isStop)
                            throw new IOException("cancel thread");
                        out.write(data, 0, count);
                        mWriteSize = mWriteSize + count;
                            updateProgress(progressInfo);
                    }
                    updateProgress(progressInfo);
                    origin.close();
                } else {
                    VFile[] childFiles = files[i].listVFiles();

                    path = files[i].getAbsolutePath().substring(zipFile.getParentFile().getAbsolutePath().length() + 1);
                    entry = new ZipEntry(path + "/");
                    out.putNextEntry(entry);

                    if (null != childFiles && childFiles.length > 0) {
                        doZip(childFiles, out, zipFile, seq_num);
                    }
                }
            }
        }

        private void updateProgress(ProgressInfo progressInfo) {
            Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
            progressInfo.progress = (int) ((mWriteSize / totalLength) * 100);
            progressInfo.writeSize = mWriteSize;
            progressInfo.numSize = totalLength;
            msg.obj = progressInfo;
            mHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mHandler.sendMessage(msg);
        }

        public void terminate() {
            isStop = true;
        }

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    ProgressDialog dialog = (ProgressDialog) getDialog();
                    if (getDialog() != null) {
                        ProgressInfo progressInfo = (ProgressInfo) msg.obj;
                        dialog.setProgressNumberFormat(FileUtility.bytes2String(getActivity().getApplicationContext(), progressInfo.writeSize, 2) + " / " + FileUtility.bytes2String(getActivity().getApplicationContext(), progressInfo.numSize, 2));
                        dialog.setProgress(progressInfo.progress);
                        updateNotificationBar(progressInfo.progress, progressInfo.seq_num);
                    }
                    break;
                case MSG_COMPLETE:
                    boolean result = showZipFileResult(FileManagerApplication.getAppContext(), msg.arg1);
                    Dialog mDialog = getDialog();
                    if(mDialog != null){
                        onCancel(mDialog);
                        mDialog.dismiss();
                    }

//                    FileManagerActivity mFActivity = (FileManagerActivity)getActivity();
                    if (result && mContext != null && mContext.getIsShowSearchFragment()) {
                    	SearchResultFragment mSFragment = (SearchResultFragment) mContext.getFragmentManager().findFragmentById(R.id.searchlist);
                    	if(mSFragment != null){
                    	    mSFragment.updateSearchAddFileView(mNewZipFile);
                    	}else{
                    		mContext.reSearch(mContext.getSearchQueryKey());
                    	}
                    }

                    clearNotificationBar(msg.arg2);
                    mThread = null;
                    break;
                default:
                    break;
            }
        }
    };

    public static ZipDialogFragment newInstance(EditPool arg, int typeDialog) {
        ZipDialogFragment fragment = new ZipDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("editpool", arg);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");

        mType = getArguments().getInt("type");
        mEditpool = (EditPool) getArguments().getSerializable("editpool");

        if (mType == TYPE_PROGRESS_DIALOG && isNeedToShow()) {
            if (DEBUG)
                Log.d(TAG, "set RetainInstance");
            setRetainInstance(true);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mType == TYPE_PROGRESS_DIALOG) {
            Window w = getDialog().getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (isNeedToShow()) {
//                isNewCreated = true;
            	if(mThread == null){
                    mRunnable = new ZipRunnable();
                    mThread = new Thread(mRunnable);
                    mThread.setPriority(Thread.MIN_PRIORITY);
                    mThread.start();
            	}
//                isFirst = false;
            }
        }
    }

    public void onResume() {
        super.onResume();

        if (mType == TYPE_PROGRESS_DIALOG) {

        	ProgressDialog mDialog = (ProgressDialog) getDialog();
        	if(mDialog == null){
        		return;
        	}

            if(isNeedToShow()){
                if(!mDialog.isShowing()){
                	mDialog.show();
                }
            }else{
            	if(mDialog.isShowing()){
            		mDialog.dismiss();
            	}
            }
        } else {
            if (getDialog() != null) {
                Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setEnabled(!EditorUtility.isSpecialChar(editText.getText().toString()));
            }
        }
//        isNewCreated = false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG)
            Log.d(TAG, "onCancel");
        mEditpool.clear();
        if (getFragmentManager() != null) {
            if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()){
            /****will be used when multi select is enable***/
            }else{
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                fileListFragment.updateEditMode();
                getActivity().invalidateOptionsMenu();
            }
        }

        cancel = true;
        if (mRunnable != null)
            mRunnable.terminate();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (DEBUG)
            Log.d(TAG, "onDismiss");
        if (getDialog() != null && getRetainInstance() && !cancel) {
            return;
        }
        super.onDismiss(dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mEditpool = (EditPool) getArguments().getSerializable("editpool");
        int type = getArguments().getInt("type");
        mContext = (FileManagerActivity)getActivity();
        Dialog dialog = null;

        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        if (type == TYPE_ZIP_DIALOG) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mZipView = inflater.inflate(R.layout.dialog_zip, null);
            VFile[] files = mEditpool.getFiles();
            editText = (EditText) mZipView.findViewById(R.id.edit_name);
            if(files != null && files.length > 0){
            	editText.setText(files[0].getNameNoExtension());
            }
            editText.addTextChangedListener(this);
            TextView tv = (TextView)mZipView.findViewById(R.id.zip_extension);
            tv.setText(".zip");

            editToast = (TextView) mZipView.findViewById(R.id.edit_toast);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(R.string.dialog_create_zip_title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();

            ((AlertDialog)dialog).setView(mZipView, spacing_left, spacing_top, spacing_right, spacing_bottom);
             dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        } else if (type == TYPE_PROGRESS_DIALOG) {
            dialog = new ProgressDialog(getActivity(),
                    themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
            ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.zip_progress_msg));
            ((ProgressDialog) dialog).setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            ((ProgressDialog) dialog).setMax(100);
            ((ProgressDialog) dialog).setProgress(mProgressValue);
            ((ProgressDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.cloud_paste_backgroud), this);
            ((ProgressDialog) dialog).setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), this);
            setCancelable(false);
        }

        return dialog;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (getDialog() != null) {
        	String text = s.toString().trim();
        	boolean isSpecialChar = EditorUtility.isSpecialChar(text);
        	boolean isNameTooLong = text.getBytes().length > 250;

        	s.setFilters(isNameTooLong ? new InputFilter[]{new InputFilter.LengthFilter(s.length())} : new InputFilter[]{});

            Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(!isSpecialChar && !isNameTooLong);

            if(!text.isEmpty() && isSpecialChar){
            	editToast.setText(getResources().getString(R.string.edit_toast_special_char));
            	editToast.setVisibility(View.VISIBLE);
            }
            else if(isNameTooLong){
            	editToast.setText(getResources().getString(R.string.edit_toast_name_too_long));
            	editToast.setVisibility(View.VISIBLE);
            }
            else{
            	editToast.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which == Dialog.BUTTON_POSITIVE) {
        	if( getArguments().getInt("type") == TYPE_ZIP_DIALOG){
                EditText editText = (EditText) mZipView.findViewById(R.id.edit_name);
                mEditpool.setZipName(editText.getText() + ".zip");

                if (getFragmentManager() != null) {
                    FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    fileListFragment.onDeselectAll();
                }

                ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_ZIP_PROGRESS_DIALOG, mEditpool);

                GaAccessFile.getInstance().sendEvents(getActivity(),
                        GaAccessFile.ACTION_COMPRESS, mEditpool.getFile().getVFieType(), -1, mEditpool.getSize());
        	}else{
        		dialog.dismiss();
        	}
        } else {
            onCancel(dialog);
        }
    }

    public static boolean showZipFileResult(Context context, int result) {
        boolean success = false;
        switch (result) {
            case EditResult.E_EXIST:
                ToastUtility.show(context, R.string.target_exist, Toast.LENGTH_LONG);
                break;
            case EditResult.E_FAILURE:
                ToastUtility.show(context, R.string.zip_fail, Toast.LENGTH_LONG);
                break;
            case EditResult.E_PERMISSION:
                ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
                break;
            case EditResult.E_SUCCESS:
                ToastUtility.show(context, R.string.zip_success, Toast.LENGTH_LONG);
                success = true;
                break;
            case EditResult.E_NOSPC:
                ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
                break;
        }
        return success;
    }

    private void updateNotificationBar(int progress, int seq_num){
        if(manager == null || builder == null){
            manager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new Builder(mContext);

            Intent mShowDialogIntent = new Intent();
            mShowDialogIntent.setAction(Intent.ACTION_MAIN);
            mShowDialogIntent.setClass(mContext, FileManagerActivity.class);
            mShowDialogIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            PendingIntent mIntent = PendingIntent.getActivity(mContext, 0, mShowDialogIntent, 0);

            builder.setSmallIcon(R.drawable.noti_zip)
            .setContentTitle(mContext.getResources().getString(R.string.zip_progress_msg))
            .setContentText(mEditpool.getZipName())
            .setContentIntent(mIntent);
//            .setOngoing(true);
        }

        if (LAST_PROGRESS != progress){
            builder
                .setProgress(100, progress, false);
            manager.notify(ZIP_NOTIFICATION_ID + seq_num, builder.build());
            LAST_PROGRESS = progress;
        }
    }

    private void clearNotificationBar(int seq_num){
    	if((manager == null || builder == null) && mContext != null){
            manager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new Builder(mContext);
        }

        if(manager != null){
            manager.cancel(ZIP_NOTIFICATION_ID + seq_num);
        }
        manager = null;
        builder = null;
    }

    public static void showZipDialog(FixedListFragment fragment,VFile[] array,boolean isChecked){
    	if(fragment == null || array.length < 0){
    		return;
    	}
    	EditPool pool = new EditPool();
    	pool.setFiles(array, isChecked);
    	if(fragment instanceof FileListFragment){
    	    ((FileListFragment)fragment).showDialog(DialogType.TYPE_ZIP_DIALOG, pool);
    	}else{
    		((SearchResultFragment)fragment).showDialog(DialogType.TYPE_ZIP_DIALOG, pool);
    	}
    }

    private boolean isNeedToShow(){
    	boolean need = true;
    	if(mEditpool == null || mEditpool.getFiles() == null){
    		need = false;
    	}
    	return need;
    }


}
