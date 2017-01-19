package com.asus.filemanager.utility;
import android.content.Context;
import android.os.AsyncTask;

import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.provider.AllFilesDatabase;

import java.util.List;

public class ScanDirFilesTask extends AsyncTask<VFile, Integer, Boolean> {

    private AllFilesDatabase allFilesDatabase;
    private VFile dir;
    private List<VFile> parentVFileList;
    private OnScanDirResultListener onScanDirResultListener;
    private long dirUsageSizes = 0;

    public interface OnScanDirResultListener {
        public void onScanDirStart();
        public void onScanDirResult(boolean isSuccess, List<VFile> parentVFileList,long usageSizes);
    }

    public ScanDirFilesTask(Context context, VFile dir, OnScanDirResultListener onScanDirResultListener)
    {

        allFilesDatabase = new AllFilesDatabase(context);
        this.dir = dir;
        this.onScanDirResultListener = onScanDirResultListener;
    }

    public void onPreExecute()
    {
        if (onScanDirResultListener != null)
            onScanDirResultListener.onScanDirStart();
    }

    @Override
    public Boolean doInBackground(VFile... files) {
        // TODO Auto-generated method stub
        if ((dir != null) &&  (dir.isDirectory()))
        {
            parentVFileList = allFilesDatabase.getFileList(FileUtility.getCanonicalPathNoException(dir));
            FunctionalDirectoryUtility functionalDirectoryUtility=FunctionalDirectoryUtility.getInstance();
            for(int i=parentVFileList.size()-1;i>-1;i--)
            {
                VFile file = parentVFileList.get(i);
                dirUsageSizes+=file.length();

                if(file.isDirectory() && functionalDirectoryUtility.inFunctionalDirectory(file))
                {
                    //remove it
                    parentVFileList.remove(i);
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {


        if (onScanDirResultListener != null) {
            onScanDirResultListener.onScanDirResult(result, parentVFileList, dirUsageSizes);
        }
    }
}
