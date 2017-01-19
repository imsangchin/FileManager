package com.asus.filemanager.activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ChenHsin_Hsieh on 2016/2/5.
 */
public class AnalyzerLargeFilesFragment extends AnalyzerCardBaseFragment{

    public static final String TAG = "AnalyzerLargeFiles";

    public CalcLargeFilesSizesTask calcLargeFilesSizesTask;

    public void onActivityCreated(Bundle bundle)
    {
        Log.i(TAG, "AnalyzerLargeFilesFragment onActivityCreated");
        super.onActivityCreated(bundle);
        setTitle(getResources().getString(R.string.category_large_file1));
        setContent(getResources().getString(R.string.large_files_content_hint));
        setItemImage(R.drawable.asus_filemanager_ic_largefile);
        setClick();
        setGaAction(GaStorageAnalyzer.ACTION_ANALYZER_LARGE_FILE_PAGE);
    }

    public void onDetach()
    {
        Log.i(TAG, "AnalyzerLargeFilesFragment onDetach");
        super.onDetach();
        if(calcLargeFilesSizesTask!=null)
            calcLargeFilesSizesTask.cancel(true);
        calcLargeFilesSizesTask = null;
    }

    private void setClick()
    {
        setCloseSelf(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("categoryItemId", CategoryItem.LARGE_FILE);
        intent.putExtra(FileManagerActivity.KEY_FROM_STORAGE_ANALYZER, true);
        intent.setClassName(getActivity().getPackageName(), "com.asus.filemanager.activity.FileManagerActivity");
        setClickIntent(intent);
    }

    @Override
    public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements) {
        if(getActivity()==null || isDetached())
            return;

        calcLargeFilesSizesTask = new CalcLargeFilesSizesTask();
        calcLargeFilesSizesTask.execute();
        Log.i(TAG, "AnalyzerLargeFilesFragment CalcLargeFilesSizesTask start");
    }

    @Override
    public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement) {

    }

    public class CalcLargeFilesSizesTask extends AsyncTask<Object, Integer, Long> {

        @Override
        public Long doInBackground(Object... objects) {
            // TODO Auto-generated method stub
            if(getActivity()==null || isDetached())
                return 0l;

            long memoryUsed = 0;
            ArrayList<LocalVFile> vFileList = MediaProviderAsyncHelper.getFilesBySize(getActivity(),FileManagerActivity.SUPPORT_LARGE_FILES_THRESHOLD,false,false);
            for (int i =  0; i<vFileList.size(); i++) {
                    memoryUsed+=vFileList.get(i).length();
            }
            vFileList.clear();
            return memoryUsed;
        }

        protected void onPostExecute(Long result) {
            if(getActivity()==null || isDetached())
                return;

            setSizes(FileUtility.bytes2String(getActivity(), result, 1));
            Log.i(TAG, "AnalyzerLargeFilesFragment CalcLargeFilesSizesTask finished");
        }
    }

}
