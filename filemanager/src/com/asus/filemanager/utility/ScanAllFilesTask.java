package com.asus.filemanager.utility;

import android.content.Context;
import android.os.AsyncTask;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.provider.AllFilesDatabase;

import java.util.ArrayList;
import java.util.List;

public class ScanAllFilesTask extends AsyncTask<VFile, String, Boolean> {

    private FileManagerApplication fileManagerApplication;
    private List<VFile> fileList;
    private long storgeTotalSizes = 0;
    private int groupId;
    private boolean isRunning = true;

    private AllFilesDatabase allFilesDatabase;
    private OnScanAllFilesResultListener onScanAllFilesResultListener;
    
 
    public interface OnScanAllFilesResultListener {
        public void onScanAllFilesProgress(String progressPath);
        public void onScanAllFilesResult(boolean success);
    }
    
    public ScanAllFilesTask(Context context, OnScanAllFilesResultListener onScanAllFilesResultListener)
    {

        fileList = new ArrayList<VFile>();
        allFilesDatabase = new AllFilesDatabase(context);
        this.onScanAllFilesResultListener = onScanAllFilesResultListener;
    }

    public void setOnScanAllFilesResultListener(OnScanAllFilesResultListener onScanAllFilesResultListener) {
            this.onScanAllFilesResultListener = onScanAllFilesResultListener;
    }

    public void setFileManagerApplication(FileManagerApplication fileManagerApplication) {
        this.fileManagerApplication = fileManagerApplication;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Boolean doInBackground(VFile... files) {
        // TODO Auto-generated method stub
        if (files==null) 
                return false;
        
        for(int i = 0;i<files.length;i++)
        {
            VFile dirVFile = files[i];
            if(dirVFile.exists() && dirVFile.isDirectory())
            {
                groupId = allFilesDatabase.getGroupId(FileUtility.getCanonicalPathNoException(dirVFile));
                storgeTotalSizes = dirVFile.getTotalSpace();
                //can't not access totalSpace will be zero
                if(storgeTotalSizes==0)
                    continue;
                //----------scan dir start, and record all---------------
                long dirUsageSizes = scanDirSizes(dirVFile);
                //----------scan dir end---------------

                recordToDatabase();
                fileList.clear();
            }else{
                return false;
            }
        }
        
        
        
        return true;
    }

    protected void onProgressUpdate(String... paths) {
        if(onScanAllFilesResultListener!=null)
            onScanAllFilesResultListener.onScanAllFilesProgress(paths[0]);
    }

    protected void onPostExecute(Boolean result) {
        if(result)
            allFilesDatabase.setUpdateTime(System.currentTimeMillis());

        isRunning = false;
        if(onScanAllFilesResultListener!=null)
            onScanAllFilesResultListener.onScanAllFilesResult(result);

        if(fileManagerApplication!=null)
            fileManagerApplication.startScanDuplicateFiles(ScanDuplicateFilesTask.MODE_RESCAN,null,null);
    }
    /**
     * get directory's sizes
     * @param dirFile
     * @return
     */
    private long scanDirSizes(VFile dirFile) {
        long size = 0;

        VFile[] files = dirFile.listVFiles();
        if (files == null)
            return size;

        for (int i = 0; i < files.length; i++)
        {
            publishProgress(files[i].getAbsolutePath());
            if (files[i].isDirectory())
            {
                long parentSizes = scanDirSizes(files[i]);
                size += parentSizes;

                //record folder sizes
                recordVFile(files[i],parentSizes);
            }
            else
            {
                size += files[i].length();
                
                //record file sizes
                recordVFile(files[i],files[i].length());
            }
        }

        return size;
    }

    private void recordVFile(VFile vfile,long sizes)
    {
        vfile.setLength(sizes);
        vfile.setInStoragePercentage(((float)vfile.length())*100/ storgeTotalSizes);
        vfile.setGroupId(groupId);
        fileList.add(vfile);
    }
    
    private void recordToDatabase()
    {
        //delete same group files before insert
        allFilesDatabase.delete(groupId);
        allFilesDatabase.insertVFiles(fileList);
    }
}
