package com.asus.filemanager.utility;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.asus.filemanager.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public class CopyAndOpenFileTask extends AsyncTask<VFile, Integer, Boolean> {

    private Activity activity;
    private VFile srcFile;
    private VFile destFile;
    private boolean isAttachOp;
    private boolean isRemoteFile;
    private boolean bPreferPrebuilt;
    private boolean isShowSingleFile;
    private ProgressDialog progressDialog;

    public CopyAndOpenFileTask(Activity activity, VFile srcFile,VFile destFile, boolean isAttachOp, boolean isRemoteFile, boolean bPreferPrebuilt, boolean isShowSingleFile) {
        this.activity = activity;
        this.srcFile = srcFile;
        this.destFile = destFile;
        this.isAttachOp = isAttachOp;
        this.isRemoteFile = isRemoteFile;
        this.bPreferPrebuilt = bPreferPrebuilt;
        this.isShowSingleFile = isShowSingleFile;
    }

    public void onPreExecute() {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(activity.getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.setButton(ProgressDialog.BUTTON_POSITIVE,
                activity.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CopyAndOpenFileTask.this.cancel(true);
                    }
                });
        progressDialog.show();
    }

    @Override
    public Boolean doInBackground(VFile... files) {
        if(srcFile==null || destFile==null)
            return false;
        return copyFile();
    }

    @Override
    protected void onCancelled()
    {
        super.onCancelled();
        progressDialog.dismiss();
        destFile.delete();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        progressDialog.dismiss();
        if(result) {
            FileUtility.openFile(activity, destFile, isAttachOp, isRemoteFile, bPreferPrebuilt, isShowSingleFile);
        }
        else {
            destFile.delete();
            ToastUtility.show(activity, R.string.open_fail);
        }
    }

    private boolean copyFile() {
        boolean isSuccess = true;
        try {
            int length = srcFile.length() > 65536 ? 65536 : (int) srcFile.length();
            InputStream inputStream = new BufferedInputStream(new FileInputStream(srcFile));
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] buffer = new byte[length];
            int read = inputStream.read(buffer, 0, length);
            long writeDataSize = 0;
            while (read != -1)
            {
                outputStream.write(buffer, 0, read);
                read = inputStream.read(buffer, 0, length);
                writeDataSize+=read;
                if((srcFile.length() - writeDataSize) > destFile.getUsableSpace()){
                    isSuccess = false;
                    break;
                }
            }
            outputStream.flush();
            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return isSuccess;
    }

}
