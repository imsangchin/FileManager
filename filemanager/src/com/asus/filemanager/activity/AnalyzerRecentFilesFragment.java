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

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ChenHsin_Hsieh on 2016/3/7.
 */
public class AnalyzerRecentFilesFragment extends AnalyzerCardBaseFragment{

    public static final String TAG = "AnalyzerRecentFiles";

    private CalcRecentFilesSizesTask calcRecentFilesSizesTask;

    public void onActivityCreated(Bundle bundle)
    {
        Log.i(TAG, "AnalyzerRecentFilesFragment onActivityCreated");
        super.onActivityCreated(bundle);
        setTitle(getResources().getString(R.string.recently_used));
        setContent(getResources().getString(R.string.recent_files_content_hint));
        setItemImage(R.drawable.asus_filemanager_ic_recentfile);
        setClick();
        setGaAction(GaStorageAnalyzer.ACTION_ANALYZER_RECENT_FILE_PAGE);

    }

    public void onDetach()
    {
        super.onDetach();
        Log.i(TAG, "AnalyzerRecentFilesFragment onDetach");
        if(calcRecentFilesSizesTask!=null)
            calcRecentFilesSizesTask.cancel(true);
        calcRecentFilesSizesTask = null;
    }

    private void setClick()
    {
        setCloseSelf(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("categoryItemId", CategoryItem.RECENT);
        intent.putExtra(FileManagerActivity.KEY_FROM_STORAGE_ANALYZER, true);
        intent.setClassName(getActivity().getPackageName(), "com.asus.filemanager.activity.FileManagerActivity");
        setClickIntent(intent);
    }

    @Override
    public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements) {
        if(getActivity()==null || isDetached())
            return;

        calcRecentFilesSizesTask = new CalcRecentFilesSizesTask();
        calcRecentFilesSizesTask.execute();
        Log.i(TAG, "AnalyzerRecentFilesFragment start CalcRecentFilesSizesTask");
    }

    @Override
    public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement) {

    }

    public class CalcRecentFilesSizesTask extends AsyncTask<Object, Integer, Long> {

        @Override
        public Long doInBackground(Object... objects) {
            // TODO Auto-generated method stub
            long memoryUsed = 0;
            if(getActivity()==null || isDetached())
                return memoryUsed;

            List<LocalVFile> vFileList = MediaProviderAsyncHelper.getRecentFiles(getActivity(), false);
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
            Log.i(TAG, "AnalyzerRecentFilesFragment CalcRecentFilesSizesTask finished");
        }
    }

}
