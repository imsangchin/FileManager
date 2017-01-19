package com.asus.filemanager.activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.IFunctionalDirectory;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.VFile;

import java.util.LinkedList;

/**
 * Created by ChenHsin_Hsieh on 2016/2/5.
 */
public class AnalyzerRecycleBinFragment extends AnalyzerCardBaseFragment{

    public static final String TAG = "AnalyzerRecycleBin";

    public CalcRecycleBinSizesTask calcRecycleBinSizesTask;

    public void onActivityCreated(Bundle bundle)
    {
        Log.i(TAG, "AnalyzerRecycleBin onActivityCreated");
        super.onActivityCreated(bundle);
        setTitle(getResources().getString(R.string.tools_recycle_bin));
        setContent(getResources().getString(R.string.recycle_bin_content_hint));
        setItemImage(R.drawable.asus_filemanager_ic_recyclebin);
        setClick();
        setGaAction(GaStorageAnalyzer.ACTION_ANALYZER_RECYCLE_BIN_PAGE);
    }

    public void onDetach()
    {
        Log.i(TAG, "AnalyzerRecycleBin onDetach");
        super.onDetach();
        if(calcRecycleBinSizesTask !=null)
            calcRecycleBinSizesTask.cancel(true);
        calcRecycleBinSizesTask = null;
    }

    private void setClick()
    {
        setCloseSelf(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(FileManagerActivity.KEY_FROM_STORAGE_ANALYZER, true);
        intent.putExtra(FileManagerActivity.KEY_SWITCH_FRAGMENT_TYPE,FileManagerActivity.FragmentType.RECYCLE_BIN);
        intent.setClassName(getActivity().getPackageName(), "com.asus.filemanager.activity.FileManagerActivity");
        setClickIntent(intent);
    }

    @Override
    public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements) {
        if(getActivity()==null || isDetached())
            return;
        if(localStorageItemElements==null)
            return;
        IFunctionalDirectory iFunctionalDirectory = FunctionalDirectoryUtility.getInstance().getIFunctionalDirectory(FunctionalDirectoryUtility.DirectoryType.RecycleBin);
        VFile[] vFiles = new VFile[localStorageItemElements.size()];
        for(int i =0;i<localStorageItemElements.size();i++)
        {
            vFiles[i] = new VFile(iFunctionalDirectory.getFunctionalDirectoryRootPath(FileUtility.getCanonicalPathNoException(localStorageItemElements.get(i).vFile)));
        }
        calcRecycleBinSizesTask = new CalcRecycleBinSizesTask();
        calcRecycleBinSizesTask.execute(vFiles);
        Log.i(TAG, "AnalyzerRecycleBin CalcRecycleBinSizesTask start");
    }

    @Override
    public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement) {

    }

    public class CalcRecycleBinSizesTask extends AsyncTask<VFile, Integer, Long> {

        @Override
        public Long doInBackground(VFile... vFiles) {
            // TODO Auto-generated method stub
            if(getActivity()==null || isDetached())
                return 0l;

            long memoryUsed = 0;
            if(vFiles!=null)
                for(int i =0;i<vFiles.length;i++)
                    memoryUsed += scanDirSizes(vFiles[i]);

            return memoryUsed;
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
                if (files[i].isDirectory())
                {
                    long parentSizes = scanDirSizes(files[i]);
                    size += parentSizes;
                }
                else
                {
                    size += files[i].length();
                }
            }
            return size;
        }

        protected void onPostExecute(Long result) {
            if(getActivity()==null || isDetached())
                return;

            setSizes(FileUtility.bytes2String(getActivity(), result, 1));
            Log.i(TAG, "AnalyzerRecycleBin CalcRecycleBinSizesTask finished");
        }
    }

}
