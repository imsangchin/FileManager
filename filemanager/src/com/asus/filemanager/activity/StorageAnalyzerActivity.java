package com.asus.filemanager.activity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.provider.AllFilesDatabase;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.DebugLog;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ScanDuplicateFilesTask;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
/**
 * Created by ChenHsin_Hsieh on 2016/1/28.
 */
public class StorageAnalyzerActivity extends BaseActivity implements Observer ,ScanDuplicateFilesTask.OnDuplicateFileResultListener{

    public static final String TAG = "StorageAnalyzerActivity";
    public static final int KEY_NOTIFY_CHANGED = 0;
    public static final String KEY_GA_ACTION = "KEY_GA_ACTION";
    public static final String KEY_SCAN_ALL_FILES = "KEY_SCAN_ALL_FILES";

    private LinkedList<StorageListAdapger.StorageItemElement> mLocalStorageItemElements = new LinkedList<StorageListAdapger.StorageItemElement>();
    private ArrayList<AnalyzerChartFragment> chartFragments = new ArrayList<>();
    private final int[] ANALYZER_FRAGMENT_IDS = {R.id.activity_storage_analyzer_fragment_large_files, R.id.activity_storage_analyzer_fragment_recent_files, R.id.activity_storage_analyzer_fragment_duplicate_files,R.id.activity_storage_analyzer_fragment_recycle_bin};

    private String previousPath;
    private String previousEvent;

    private FileManagerApplication fileManagerApplication;
    private AllFilesDatabase allFilesDatabase;
    //24*60*60*1000 = 86400000
    private final long RESCAN_TIME_MILLIS = 86400000;

    private boolean forceLoadingMenuFlag = false;

    public interface OnStorageChangedListener {
        public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements);
        public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "StorageAnalyzerActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_analyzer);
        fileManagerApplication =  ((FileManagerApplication) getApplication());
        // init theme
        initActionBar();
        ckeckIntent();
        updateLocalStorageItemElements();
        updateChartFragments();
        notifyFragmentsStorageChanged();

        initialScanAllFiles();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "StorageAnalyzerActivity onResume");
        fileManagerApplication.mVolumeStateObserver.addObserver(this);
        invalidateOptionsMenu();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "StorageAnalyzerActivity onStop");
        fileManagerApplication.mVolumeStateObserver.deleteObserver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof AnalyzerChartFragment) {
            if (!chartFragments.contains(fragment)) {
                Log.i(TAG, "StorageAnalyzerActivity loss AnalyzerChartFragment reference add it");
                chartFragments.add((AnalyzerChartFragment) fragment);
            }
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(this.getResources().getString(R.string.tools_storage_analyzer));
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void ckeckIntent() {
        String gaAction = getIntent().getStringExtra(KEY_GA_ACTION);
        if (gaAction != null) {
            GaStorageAnalyzer.getInstance().sendEvents(this, GaStorageAnalyzer.CATEGORY_NAME, gaAction, null, null);
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (null != data) {
            Bundle aBundle = (Bundle) data;
            String event;
            String path;
            event = aBundle.getString(VolumeStateObserver.KEY_EVENT);
            path = aBundle.getString(VolumeStateObserver.KEY_PATH);
            if (path == null) {
                return;
            }
            if (path.equals(previousPath) && event.equals(previousEvent))
                return;

            invalidateOptionsMenu();

            if (event.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                //rescan path
                fileManagerApplication.startScanAllFiles(null, FileUtility.getCanonicalPathNoException(new VFile(path)));

            }
            Log.i(TAG, "StorageAnalyzerActivity update storage");
            updateLocalStorageItemElements();
            updateChartFragments();
            notifyFragmentsStorageChanged();

            previousPath = path;
            previousEvent = event;
        }
    }

    private void updateLocalStorageItemElements() {
        Log.i(TAG, "StorageAnalyzerActivity updateLocalStorageItemElements");
        ArrayList<Object> homePageLocalStorageElementList = new ArrayList<Object>();
        ArrayList<VFile> homePageLocalStorageFile = new ArrayList<VFile>();

        final StorageManager mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume = fileManagerApplication.getStorageVolume();
        VFile[] tmpVFiles = fileManagerApplication.getStorageFile();
        for (int i = 0; i < storageVolume.size(); i++) {
            if (mStorageManager != null && reflectionApis.getVolumeState(
                    mStorageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)) {
                homePageLocalStorageElementList.add(storageVolume.get(i));
                homePageLocalStorageFile.add(tmpVFiles[i]);
            }
        }

        mLocalStorageItemElements.clear();
        for (int i = 0; i < homePageLocalStorageElementList.size(); i++) {
            //storage not exist, change next
            if (homePageLocalStorageFile.get(i).getTotalSpace() == 0)
                continue;
            StorageListAdapger.StorageItemElement storageItemEelement = new StorageListAdapger.StorageItemElement();
            storageItemEelement.storageVolume = homePageLocalStorageElementList.get(i);
            storageItemEelement.vFile = homePageLocalStorageFile.get(i);
            storageItemEelement.storageType = StorageListAdapger.STORAGETYPE_LOCAL;
            mLocalStorageItemElements.add(storageItemEelement);
        }
    }

    private void updateChartFragments() {
        Log.i(TAG, "StorageAnalyzerActivity updateChartFragments");
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        //add chart fragments
        if (mLocalStorageItemElements.size() >= chartFragments.size()) {
            for (int i = chartFragments.size(); i < mLocalStorageItemElements.size(); i++) {
                AnalyzerChartFragment analyzerChartFragment = new AnalyzerChartFragment();
                if (i == 0)
                    analyzerChartFragment.setGaAction(GaStorageAnalyzer.ACTION_ALL_FILES_INTERNAL_PAGE);

                chartFragments.add(analyzerChartFragment);
                fragmentTransaction.add(R.id.activity_storage_analyzer_chart_container, analyzerChartFragment);
            }
        } else //remove chart fragments
        {
            for (int i = chartFragments.size() - 1; i > mLocalStorageItemElements.size() - 1; i--) {
                fragmentTransaction.remove(chartFragments.get(i));
                chartFragments.remove(i);
            }
        }
        fragmentTransaction.commit();

    }

    private void notifyFragmentsStorageChanged() {
        Log.i(TAG, "StorageAnalyzerActivity notifyFragmentsStorageChanged");
        OnStorageChangedListener storageChangedListener;
        //for chart
        for (int i = 0; i < mLocalStorageItemElements.size(); i++) {
            //notify chartFragment onStorageChanged
            if (chartFragments.get(i) instanceof OnStorageChangedListener && !chartFragments.get(i).isDetached()) {
                storageChangedListener = ((OnStorageChangedListener) chartFragments.get(i));
                storageChangedListener.onStorageChanged(mLocalStorageItemElements.get(i));
                Log.i(TAG, "StorageAnalyzerActivity notify chartFragments:" + i);
            }
        }

        //for others
        for (int i = 0; i < ANALYZER_FRAGMENT_IDS.length; i++) {
            //notify chartFragment onStorageChanged
            try {
                Fragment fragment = getFragmentManager().findFragmentById(ANALYZER_FRAGMENT_IDS[i]);
                if(fragment!=null && !fragment.isDetached()) {
                    storageChangedListener = (OnStorageChangedListener) fragment;
                    if (storageChangedListener != null) {
                        storageChangedListener.onStorageChanged(mLocalStorageItemElements);
                        Log.i(TAG, "StorageAnalyzerActivity notify others:" + i);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initialScanAllFiles() {
        allFilesDatabase = new AllFilesDatabase(this);
        //check time
        //if ScanAllFiles task not finish, avoid calling allFilesDatabase.getUpdateTimeMillis(), it will occur ANR
        if(fileManagerApplication.isScanAllFilesFinished()) {
            if ((System.currentTimeMillis() - allFilesDatabase.getUpdateTimeMillis()) > RESCAN_TIME_MILLIS) {
                forceRescanAllFiles();
            } else {
                startScanMountedFiles();
            }
        }
    }

    public void startScanMountedFiles() {
        Log.i(TAG, "StorageAnalyzerActivity startScanMountedFiles");
        List<String> groupRootPaths = allFilesDatabase.getGroupRootPaths(false);
        for (int i = 0; i < mLocalStorageItemElements.size(); i++) {
            String path;
            try {
                path = FileUtility.getCanonicalPath(mLocalStorageItemElements.get(i).vFile);
            } catch (IOException e) {
                path = mLocalStorageItemElements.get(i).vFile.getAbsolutePath();
                e.printStackTrace();
            }
            //path not in group scan it
            if (!groupRootPaths.contains(path)) {
                if(DebugLog.DEBUG) {
                    Log.i(TAG, "Start scan db not exist path:" + path);
                }
                fileManagerApplication.startScanAllFiles(null, path);
            }
        }
    }

    public void forceRescanAllFiles() {
        if (fileManagerApplication.isScanAllFilesFinished() && fileManagerApplication.isScanDupFilesFinished(this)) {
            Log.i(TAG, "StrageAnalyzerActivity start force rescan task");
            new ForceRescanTask().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_storage_analyzer, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        //when call invalidateOptionsMenu() will trigger this function
        MenuItem refresh = menu.findItem(R.id.menu_storage_analyzer_refresh);
        if(refresh ==null)
            return true;

        if(fileManagerApplication.isScanAllFilesFinished() && fileManagerApplication.isScanDupFilesFinished(this) && !forceLoadingMenuFlag){
            refresh.setActionView(null);
        }else{
            ProgressBar progressBar = new ProgressBar(this);
            refresh.setActionView(progressBar);
        }
        forceLoadingMenuFlag = false;

        ThemeUtility.setMenuIconColor(getApplicationContext(), menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.menu_storage_analyzer_refresh:
                forceRescanAllFiles();
                GaStorageAnalyzer.getInstance().sendEvents(this, GaStorageAnalyzer.CATEGORY_NAME, GaStorageAnalyzer.ACTION_ANALYZER_REFRESH_ALL, null, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDuplicateFileProgress(int progress) {

    }

    @Override
    public void OnDuplicateFileResult(List<List<VFile>> duplicateFileResultList) {
        invalidateOptionsMenu();
        notifyFragmentsStorageChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KEY_NOTIFY_CHANGED && resultCode == Activity.RESULT_OK) {
            previousEvent = null;
            updateLocalStorageItemElements();
            updateChartFragments();
            notifyFragmentsStorageChanged();

        }
    }

    public class ForceRescanTask extends AsyncTask<String, Integer, Boolean> {

        protected void onPreExecute (){
            forceLoadingMenuFlag = true;
            invalidateOptionsMenu();
        }

        @Override
        public Boolean doInBackground(String... rootPaths) {
            // TODO Auto-generated method stub
            allFilesDatabase.deleteAll();
            startScanMountedFiles();
            return true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) return;

        TextView colorfulView = (TextView) findViewById(R.id.textview_colorful);
        if(colorfulView == null) return;
        // device that api level below 19 would change the height of actionbar when rotate screen
        // need to reset colorfullayout height
        colorfulView.setHeight(ColorfulLinearLayout.getColorfulLayoutHeight(this));
    }

}
