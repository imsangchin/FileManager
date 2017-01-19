package com.asus.filemanager.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.AnalyzerAllFilesListAdapter;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.delete.DeleteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.ui.PathIndicatorView;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ScanAllFilesTask;
import com.asus.filemanager.utility.ScanDirFilesTask;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.UpdateDeleteFilesTask;
import com.asus.filemanager.utility.VFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AnalyzerAllFilesFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AnalyzerAllFilesListAdapter.OnCheckBoxChangedListener,ScanAllFilesTask.OnScanAllFilesResultListener, ScanDirFilesTask.OnScanDirResultListener,UpdateDeleteFilesTask.OnUpdateDeleteFilesResultListener, PathIndicatorView.OnPathIndicatorListener, Observer {

    public static final String TAG = "AnalyzerAllFilesFrag";

    private String rootPath;

    private PathIndicatorView indicatorContainer;
    private HorizontalScrollView indicatorScrollView;
    private LinearLayout loadingLayout;
    private TextView emptyHint, usedView,loadingHint;

    private ListView listView;
    private AnalyzerAllFilesListAdapter analyzerAllFilesListAdapter;

    private ScanDirFilesTask scanDirFilesTask;
//    private long totalStorageBytes;
    private long usageSizes;
    private VFile currentParentFile;
    private String previousPath,previousEvent;

    private String mActionToolbarTitle;
    private Toolbar mActionToolbar;
    private Toolbar mEditToolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "AnalyzerAllFilesFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_analyser_allfiles, container, false);
        findViews(rootView);
        initialList();
        setClickListener();
        initialPathIndicator();
        initActionToolbar(rootView);
        initEditToolbar(rootView);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((FileManagerApplication) getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "AnalyzerAllFilesFragment onDetach");
        ((FileManagerApplication) getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
        if (scanDirFilesTask != null)
            scanDirFilesTask.cancel(true);

    }

    private void findViews(View rootView) {
        indicatorContainer = (PathIndicatorView) rootView.findViewById(R.id.fragment_analyser_allfiles_indicator_container);
        indicatorScrollView = (HorizontalScrollView) rootView.findViewById(R.id.fragment_analyser_allfiles_indicator_scrollView);
        loadingLayout = (LinearLayout) rootView.findViewById(R.id.fragment_analyser_allfiles_loading);
        listView = (ListView) rootView.findViewById(R.id.fragment_analyser_allfiles_listview);
        emptyHint = (TextView) rootView.findViewById(R.id.fragment_analyser_allfiles_empty);
        usedView = (TextView) rootView.findViewById(R.id.fragment_analyser_allfiles_used);
        loadingHint = (TextView) rootView.findViewById(R.id.fragment_analyser_allfiles_loading_hint);
    }

    private void setClickListener() {
        // TODO Auto-generated method stub
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        analyzerAllFilesListAdapter.setOnCheckBoxChangedListener(this);
    }

    private void initialList() {
        analyzerAllFilesListAdapter = new AnalyzerAllFilesListAdapter(getActivity(), new ArrayList<VFile>());
        listView.setAdapter(analyzerAllFilesListAdapter);
    }

    public void setUsed(long sizes) {
        usageSizes = sizes;
        usedView.setText(FileUtility.bytes2String(getActivity(), sizes, 1));
    }

    public void setRootName(String... rootName) {
        if (indicatorContainer != null)
            indicatorContainer.setRootName(rootName);
    }

    public void setTitle(String title)
    {
        if(title==null)
            return;

        mActionToolbarTitle = title;
        if(mActionToolbar!=null)
            mActionToolbar.setTitle(title);
    }

    public void initial(String rootPath, long totalStorageBytes) {
        Log.i(TAG, "AnalyzerAllFilesFragment initial");

        this.rootPath = rootPath;
//        this.totalStorageBytes = totalStorageBytes;
        currentParentFile = new VFile(rootPath);
        indicatorContainer.setPath(rootPath, currentParentFile);

        if(((FileManagerApplication)getActivity().getApplication()).isScanAllFilesFinished(rootPath,this))
            startScanDirFilesTask();
    }

    private void startScanDirFilesTask() {
        Log.i(TAG, "AnalyzerAllFilesFragment startScanDirFilesTask");
        scanDirFilesTask = new ScanDirFilesTask(getActivity(), currentParentFile, this);
        scanDirFilesTask.execute();

    }

    private void initialPathIndicator() {
        indicatorContainer.setOnPathIndicatorListener(this);
    }


    private void initActionToolbar(View rootView) {
        mActionToolbar = (Toolbar)rootView.findViewById(R.id.toolbar);
        mActionToolbar.setVisibility(View.VISIBLE);
        mActionToolbar.setNavigationIcon(R.drawable.asussl_ic_ab_back);
        ThemeUtility.setItemIconColor(getActivity(), mActionToolbar.getNavigationIcon());
        setTitle(mActionToolbarTitle);
        mActionToolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });
    }

    private void initEditToolbar(View rootView)
    {
        mEditToolbar = (Toolbar)rootView.findViewById(R.id.fragment_analyser_allfiles_edit_toolbar);
        mEditToolbar.setNavigationIcon(R.drawable.asussl_ic_ab_back);
        ThemeUtility.setItemIconColor(getActivity(), mEditToolbar.getNavigationIcon());
        mEditToolbar.inflateMenu(R.menu.allfiles_edit);
        String itemSelected = getResources().getQuantityString(R.plurals.number_selected_items, 1, 1);
        mEditToolbar.setTitle(itemSelected);
        mEditToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.select_all_action:
                        analyzerAllFilesListAdapter.setAllLongClicked();
                        updateEditToolbarMenu();
                        updateEditToolbarTitle();
                        break;
                    case R.id.deselect_all_action:
                        onBackPressed();
                        break;
                    case R.id.delete_action:
                        deleteFileInPopup();
                        break;
                }
                return true;
            }
        });
        mEditToolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mEditToolbar.setVisibility(View.GONE);
    }

    private void movePathIndicatorRight() {
        indicatorScrollView.post(new Runnable() {
            @Override
            public void run() {
                indicatorScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    @Override
    public void onScanAllFilesProgress(String path) {
        loadingHint.setText(path);
    }

    @Override
    public void onScanAllFilesResult(boolean success) {
        if(getActivity()==null || isDetached())
            return;
        //if ScanAllFilesTask is running when enter this page, wait for finish to startScanDirFilesTask()
        startScanDirFilesTask();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        Log.i(TAG, "AnalyzerAllFilesFragment onItemClick");
        VFile clickVFile = analyzerAllFilesListAdapter.getItem(position);
        //start analysis
        if (analyzerAllFilesListAdapter.isLongClickMode()) {
            analyzerAllFilesListAdapter.setOnItemClick(view,position);
        } else if (clickVFile.isDirectory() && clickVFile.canRead()) {
            currentParentFile = clickVFile;
            startScanDirFilesTask();
            indicatorContainer.setPath(rootPath, currentParentFile);
            movePathIndicatorRight();

        } else if (!clickVFile.isDirectory()) {
            //open file
            FileUtility.openFile(getActivity(), clickVFile, false, false);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        if(!analyzerAllFilesListAdapter.isLongClickMode()) {
            startEditToolbar();
        }
        analyzerAllFilesListAdapter.setLongClickMode(true);
        analyzerAllFilesListAdapter.setOnItemClick(view,position);
        return true;
    }

    @Override
    public void onCheckBoxChanged(int position, boolean isChecked) {

        if (analyzerAllFilesListAdapter.isLongClickMode()) {
            updateEditToolbarTitle();
            updateEditToolbarMenu();
        } else{
            stopEditToolbar();
        }
    }

    @Override
    public void onScanDirStart() {
        // TODO Auto-generated method stub
        loadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanDirResult(boolean isSuccess, List<VFile> parentVFileList, long usageSizes) {
        Log.i(TAG, "AnalyzerAllFilesFragment onScanDirResult");

        //close loading
        loadingLayout.setVisibility(View.GONE);

        //show no file hint
        if (parentVFileList.size() > 0)
            emptyHint.setVisibility(View.GONE);
        else
            emptyHint.setVisibility(View.VISIBLE);
        analyzerAllFilesListAdapter.setFiles(parentVFileList);

        setUsed(usageSizes);

    }

    @Override
    public void onUpdateDeleteStart() {
        loadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUpdateDeleteResult(boolean isSuccess,long deleteSizes) {
        if(getActivity()==null || isDetached())
            return;

        loadingLayout.setVisibility(View.GONE);
        setUsed(usageSizes - deleteSizes);
    }

    @Override
    public void onPathClick(String path) {
        // TODO Auto-generated method stub
        Log.i(TAG, "AnalyzerAllFilesFragment onPathClick");
        if (!FileUtility.getCanonicalPathNoException(currentParentFile).equals(path)) {
            if (analyzerAllFilesListAdapter.isLongClickMode()) {
                onBackPressed();
            }
            currentParentFile = new VFile(path);
            startScanDirFilesTask();
            indicatorContainer.setPath(rootPath, currentParentFile);
            movePathIndicatorRight();
        }
    }

    public boolean onBackPressed() {
        Log.i(TAG, "AnalyzerAllFilesFragment onBackPressed");
        if (analyzerAllFilesListAdapter.isLongClickMode()) {
            analyzerAllFilesListAdapter.resetAllLongClicked();
            stopEditToolbar();
        } else if (FileUtility.getCanonicalPathNoException(currentParentFile).equals(rootPath)) {
            //back to previous page
            analyzerAllFilesListAdapter.release();
            getActivity().finish();
        } else {
            //back to previous path
            currentParentFile = currentParentFile.getParentFile();
            startScanDirFilesTask();
            indicatorContainer.setPath(rootPath, currentParentFile);
            movePathIndicatorRight();
        }
        return true;
    }

    //delete handler
    private Handler deleteHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FileListFragment.MSG_DELET_COMPLETE:
                    Log.i(TAG, "AnalyzerAllFilesFragment MSG_DELET_COMPLETE");
                    boolean deleteSuccess = (1 == msg.arg1);
                    VFile[] files = (VFile[]) msg.obj;
                    if(deleteSuccess && files!=null) {
                        updateDeleteSizes(files);
                        analyzerAllFilesListAdapter.removeDeletedFiles(files);
                    }
                    //dismiss dialog
                    DeleteDialogFragment deleteProgressDialogFragment = (DeleteDialogFragment) getFragmentManager().findFragmentByTag("DeleteDialogFragment");
                    if (deleteProgressDialogFragment != null)
                        deleteProgressDialogFragment.dismissAllowingStateLoss();
                    break;
            }
            EditorUtility.sEditIsProcessing = false;
            getActivity().setResult(Activity.RESULT_OK);
            stopEditToolbar();

        }
    };

    public Handler getHandler() {
        return deleteHandler;
    }

    private void updateDeleteSizes(VFile[] deletedArray) {
        Log.i(TAG, "AnalyzerAllFilesFragment updateDeleteSizes");
        UpdateDeleteFilesTask updateDeleteFilesTask = new UpdateDeleteFilesTask(getActivity(),deletedArray,this);
        updateDeleteFilesTask.execute();
    }

    @Override
    public void update(Observable observable, Object data) {
        if (null != data) {
            Bundle aBundle = (Bundle) data;
            String event;
            String path;
            event = aBundle.getString(VolumeStateObserver.KEY_EVENT);
            path = aBundle.getString(VolumeStateObserver.KEY_PATH);
            if (path == null || event == null) {
                return;
            }
            if(path.equals(previousPath) && event.equals(previousEvent))
                return;

            getActivity().setResult(Activity.RESULT_OK);
            if (event.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Log.i(TAG, "AnalyzerAllFilesFragment ACTION_MEDIA_UNMOUNTED");
                if (currentParentFile != null &&
                        (FileUtility.getCanonicalPathNoException(currentParentFile)
                        .startsWith(FileUtility.getCanonicalPathNoException(new VFile(path))))
                )
                {
                    getActivity().finish();
                }
            }else if (event.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                //rescan path
                ((FileManagerApplication)getActivity().getApplication()).startScanAllFiles(null, FileUtility.getCanonicalPathNoException(new VFile(path)));
            }

            previousPath = path;
            previousEvent = event;
        }
    }



    private void startEditToolbar()
    {
        mActionToolbar.setVisibility(View.GONE);
        mEditToolbar.setVisibility(View.VISIBLE);
        AlphaAnimation fadeInAnimation = new AlphaAnimation(0, 1);
        fadeInAnimation.setDuration(400);
        mEditToolbar.startAnimation(fadeInAnimation);
    }

    private void stopEditToolbar()
    {
        mActionToolbar.setVisibility(View.VISIBLE);
        mEditToolbar.setVisibility(View.GONE);
    }
    private void updateEditToolbarTitle() {
        int longClickCount = analyzerAllFilesListAdapter.getLongClickedMap().size();
        String itemSelected = getResources().getQuantityString(R.plurals.number_selected_items, longClickCount, longClickCount);
        mEditToolbar.setTitle(itemSelected);
    }

    private void updateEditToolbarMenu() {
        int longClickCount = analyzerAllFilesListAdapter.getLongClickedMap().size();
        MenuItem selectItem = mEditToolbar.getMenu().findItem(R.id.select_all_action);
        MenuItem deselectItem = mEditToolbar.getMenu().findItem(R.id.deselect_all_action);
        if (selectItem != null && deselectItem!=null) {
            if(longClickCount == analyzerAllFilesListAdapter.getCount()){
                selectItem.setVisible(false);
                deselectItem.setVisible(true);
            }else{
                selectItem.setVisible(true);
                deselectItem.setVisible(false);
            }
        }

        ThemeUtility.setMenuIconColor(getActivity(), mEditToolbar.getMenu());
    }

    public void deleteFileInPopup(){
        VFile[] deleteFiles = analyzerAllFilesListAdapter.getLongClickedMap().values().toArray(new VFile[analyzerAllFilesListAdapter.getLongClickedMap().size()]);
        deleteFileInPopup(deleteFiles);
    }

    public void deleteFileInPopup(VFile[] deleteFiles) {
        if (deleteFiles.length < 1)
            return;
        Log.i(TAG, "AnalyzerAllFilesFragment deleteFileInPopup");
        EditPool mDeleteFilePool = new EditPool();
        mDeleteFilePool.setFiles(deleteFiles, true);

        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(getActivity()).isNeedToWriteSdToAppFolder(deleteFiles[0].getAbsolutePath());
        if (bNeedWriteToAppFolder) {
            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                    .newInstance();
            warnDialog.show(AnalyzerAllFilesFragment.this.getFragmentManager(), "WarnKKSDPermissionDialogFragment");
        } else if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(deleteFiles[0].getAbsolutePath())) {
            mDeleteFilePool.clear();
            ((AnalyzerAllFilesActivity) getActivity()).callSafChoose(SafOperationUtility.ACTION_DELETE);
        } else {
            DeleteDialogFragment deleteDialogFragment = DeleteDialogFragment.newInstance(mDeleteFilePool, DeleteDialogFragment.Type.TYPE_DELETE_DIALOG);
            if (!deleteDialogFragment.isAdded()) {
                deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
            }
        }
    }

}
