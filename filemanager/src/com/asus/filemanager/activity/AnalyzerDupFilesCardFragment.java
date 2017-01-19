package com.asus.filemanager.activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.provider.DuplicateFilesDatabase;
import com.asus.filemanager.utility.FileUtility;

import java.util.LinkedList;

/**
 * Created by chenhsin_hsieh on 2016/4/25.
 */
public class AnalyzerDupFilesCardFragment extends AnalyzerCardBaseFragment{

    public static final String TAG = "AnalyzerLargeFiles";
    public CalcDupFilesSizesTask calcDupFilesSizesTask;


    public void onActivityCreated(Bundle bundle)
    {
        Log.i(TAG, "AnalyzerDupFilesCardFragment onActivityCreated");
        super.onActivityCreated(bundle);
        setTitle(getResources().getString(R.string.duplicate_files));
        setContent(getResources().getString(R.string.duplicate_files_content_hint));
        setItemImage(R.drawable.asus_filemanager_ic_duplicatefile);
        setClick();
        setGaAction(GaStorageAnalyzer.ACTION_ANALYZER_DUP_FILE_PAGE);
    }

    public void onDetach()
    {
        Log.i(TAG, "AnalyzerDupFilesCardFragment onDetach");
        super.onDetach();
        if(calcDupFilesSizesTask!=null)
            calcDupFilesSizesTask.cancel(true);
        calcDupFilesSizesTask = null;
    }

    private void setClick()
    {
        Intent intent = new Intent(getActivity(),AnalyzerDupFilesActivity.class);
        setClickIntent(intent);
    }

    @Override
    public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements) {
        if(getActivity()==null || isDetached())
            return;

        String[] rootPaths = new String[localStorageItemElements.size()];
        for(int i = 0;i<rootPaths.length;i++)
            rootPaths[i] = FileUtility.getCanonicalPathNoException(localStorageItemElements.get(i).vFile);

        calcDupFilesSizesTask = new CalcDupFilesSizesTask();
        calcDupFilesSizesTask.execute(rootPaths);

    }

    @Override
    public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement) {

    }

    public class CalcDupFilesSizesTask extends AsyncTask<String, Integer, Long> {

        @Override
        public Long doInBackground(String... rootPaths) {
            // TODO Auto-generated method stub
            if(getActivity()==null || isDetached())
                return 0l;

            DuplicateFilesDatabase duplicateFilesDatabase = new DuplicateFilesDatabase(getActivity());
            return duplicateFilesDatabase.getDuplicateFilesTotalSizes(rootPaths);
        }

        protected void onPostExecute(Long result) {
            if(getActivity()==null || isDetached())
                return;

            setSizes(FileUtility.bytes2String(getActivity(), result, 1));
            Log.i(TAG, "AnalyzerLargeFilesFragment CalcLargeFilesSizesTask finished");
        }
    }
}
