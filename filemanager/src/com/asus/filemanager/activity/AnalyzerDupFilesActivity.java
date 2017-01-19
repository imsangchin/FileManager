package com.asus.filemanager.activity;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.DuplicateFilesListAdapter;
import com.asus.filemanager.dialog.InfoDialogFragment;
import com.asus.filemanager.dialog.RequestSDPermissionDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.delete.DeleteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.Editable;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ScanAllFilesTask;
import com.asus.filemanager.utility.ScanDuplicateFilesTask;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.UpdateDeleteFilesTask;
import com.asus.filemanager.utility.VFile;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by chenhsin_hsieh on 2016/4/25.
 */
public class AnalyzerDupFilesActivity extends BaseActivity implements Observer,ScanAllFilesTask.OnScanAllFilesResultListener,ScanDuplicateFilesTask.OnDuplicateFileResultListener,DuplicateFilesListAdapter.OnChildCheckedListener, RequestSDPermissionDialogFragment.OnRequestSDPermissionFragmentListener,UpdateDeleteFilesTask.OnUpdateDeleteFilesResultListener, Editable {

    public static final String TAG = "AnalyzerDupFilesAct";

    private ExpandableListView expandableListView;
    private DuplicateFilesListAdapter duplicateFilesListAdapter;
    private LinearLayout loadingLayout;
    private TextView emptyHint,loadingHint;

    private String previousPath,previousEvent;
    protected static final int EXTERNAL_STORAGE_PERMISSION_REQ = 1;
    protected static final int REQUEST_SDTUTORIAL = 2;
    private boolean isUpdateDeleteRunning = false;

    private VFile fileOpened = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "AnalyzerDupFilesActivity onCreate");
        super.onCreate(savedInstanceState);
        ColorfulLinearLayout.setContentView(this, R.layout.activity_analyzer_dupfiles, R.color.theme_color);
        initActionBar();
        findViews();
        initial();
        setClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "AnalyzerDupFilesActivity onResume");
        ((FileManagerApplication)getApplication()).mVolumeStateObserver.addObserver(this);

        if(fileOpened!=null && !fileOpened.exists())
        {
            UpdateDeleteFilesTask updateDeleteFilesTask = new UpdateDeleteFilesTask(AnalyzerDupFilesActivity.this,new VFile[]{fileOpened},AnalyzerDupFilesActivity.this,UpdateDeleteFilesTask.MODE_UPDATE_FILES_IN_SAME_FOLDER);
            updateDeleteFilesTask.execute();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.i(TAG, "AnalyzerDupFilesActivity onStop");
        ((FileManagerApplication)getApplication()).mVolumeStateObserver.deleteObserver(this);
    }

    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.duplicate_files);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void findViews(){
        expandableListView = (ExpandableListView) findViewById(R.id.activity_analyzer_dupfiles_expandable_listview);
        loadingLayout = (LinearLayout) findViewById(R.id.activity_analyzer_dupfiles_loading);
        emptyHint = (TextView) findViewById(R.id.activity_analyzer_dupfiles_empty);
        loadingHint = (TextView) findViewById(R.id.activity_analyzer_dupfiles_loading_hint);
    }

    private void initial()
    {
        loadingLayout.setVisibility(View.VISIBLE);
        if(duplicateFilesListAdapter!=null)
            duplicateFilesListAdapter.setDuplicateFilesList(null);

        FileManagerApplication fileManagerApplication = ((FileManagerApplication) getApplication());
        //set scan all file listener, due to get progress
        fileManagerApplication.isScanAllFilesFinished(this);
        fileManagerApplication.startScanDuplicateFiles(ScanDuplicateFilesTask.MODE_SCAN_RECORDED, this, null);
    }

    private void setClickListeners()
    {
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                duplicateFilesListAdapter.setChildChecked(groupPosition, childPosition);
                invalidateOptionsMenu();
                return true;
            }
        });
        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);

                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    int childPosition = ExpandableListView.getPackedPositionChild(id);
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    fileOpened = duplicateFilesListAdapter.getChild(groupPosition, childPosition);
                    InfoDialogFragment infoDialog = InfoDialogFragment.newInstance(fileOpened);
                    infoDialog.setShowOpenFileButton(true);
                    infoDialog.show(getFragmentManager(), "InfoDialogFragment");
                    return true;

                } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    return false;

                } else {
                    return false;
                }
            }});
    }

    @Override
    public void onChildChecked(int groupPosition, int childPosition) {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.menu_analyzer_duplicate_delete:
                List<VFile> deleteList = duplicateFilesListAdapter.getDeleteFilesList();
                deleteFileInPopup(deleteList.toArray(new LocalVFile[deleteList.size()]));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (null != data){
            Bundle aBundle = (Bundle)data;
            String event;
            String path;
            event = aBundle.getString(VolumeStateObserver.KEY_EVENT);
            path = aBundle.getString(VolumeStateObserver.KEY_PATH);
            if (path == null) {
                return;
            }
            if(path.equals(previousPath) && event.equals(previousEvent))
                return;
            setResult(RESULT_OK);
            initial();

            if (event.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                //rescan path
                ((FileManagerApplication)getApplication()).startScanAllFiles(null, FileUtility.getCanonicalPathNoException(new VFile(path)));
            } else if (event.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

            }

            previousPath = path;
            previousEvent = event;
        }
    }

    @Override
    public void onScanAllFilesProgress(String progressPath) {
        loadingHint.setText(progressPath);
    }

    @Override
    public void onScanAllFilesResult(boolean success) {

    }

    @Override
    public void onDuplicateFileProgress(int progress) {
        loadingHint.setText(progress + "%");
    }

    @Override
    public void OnDuplicateFileResult(List<List<VFile>> duplicateFileResultList) {
        duplicateFilesListAdapter = new DuplicateFilesListAdapter(this, duplicateFileResultList);
        duplicateFilesListAdapter.setOnChildCheckedListener(this);
        expandableListView.setAdapter(duplicateFilesListAdapter);
        loadingLayout.setVisibility(View.INVISIBLE);
        //show no files hint
        if(duplicateFileResultList.size()==0)
            emptyHint.setVisibility(View.VISIBLE);
        else
            emptyHint.setVisibility(View.GONE);

        //expand all group
        for(int i = 0 ;i<duplicateFileResultList.size();i++)
            expandableListView.expandGroup(i);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_analyzer_duplicate, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        //when call invalidateOptionsMenu() will trigger this function
        MenuItem delete = menu.findItem(R.id.menu_analyzer_duplicate_delete);
        if(delete ==null || duplicateFilesListAdapter==null)
            return true;


        if(duplicateFilesListAdapter.getDeleteFilesList().size()>0 && !isUpdateDeleteRunning)
            delete.setVisible(true);
        else
            delete.setVisible(false);

        ThemeUtility.setMenuIconColor(getApplicationContext(), menu);

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
                        UpdateDeleteFilesTask updateDeleteFilesTask = new UpdateDeleteFilesTask(AnalyzerDupFilesActivity.this,files,AnalyzerDupFilesActivity.this,UpdateDeleteFilesTask.MODE_UPDATE_FILES_IN_DIFFERENT_FOLDER);
                        updateDeleteFilesTask.execute();
                    }
                    //dismiss dialog
                    DeleteDialogFragment deleteProgressDialogFragment = (DeleteDialogFragment) getFragmentManager().findFragmentByTag("DeleteDialogFragment");
                    if (deleteProgressDialogFragment != null)
                        deleteProgressDialogFragment.dismissAllowingStateLoss();
                    break;
            }
            EditorUtility.sEditIsProcessing = false;
            setResult(Activity.RESULT_OK);
        }
    };

    public Handler getEditHandler() {
        return deleteHandler;
    }

    @Override
    public EditorUtility.RequestFrom getRequester() {
        return EditorUtility.RequestFrom.DuplicateFiles;
    }

    public void deleteFileInPopup(VFile[] deleteFiles) {
        if (deleteFiles.length < 1)
            return;
        Log.i(TAG, "AnalyzerDupFilesActivity deleteFileInPopup");
        EditPool mDeleteFilePool = new EditPool();
        mDeleteFilePool.setFiles(deleteFiles, true);

        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(this).isNeedToWriteSdToAppFolder(deleteFiles[0].getAbsolutePath());
        if (bNeedWriteToAppFolder) {
            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                    .newInstance();
            warnDialog.show(this.getFragmentManager(), "WarnKKSDPermissionDialogFragment");
        } else if (SafOperationUtility.getInstance(this).isNeedToShowSafDialog(deleteFiles[0].getAbsolutePath())) {
            callSafChoose(SafOperationUtility.ACTION_DELETE);
        } else {
            DeleteDialogFragment deleteDialogFragment = DeleteDialogFragment.newInstance(mDeleteFilePool, DeleteDialogFragment.Type.TYPE_DELETE_DIALOG);
            if (!deleteDialogFragment.isAdded()) {
                deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
            }
        }
    }

    public void callSafChoose(int action){
        Log.i(TAG, "AnalyzerDupFilesActivity callSafChoose");
        if(FileUtility.isFirstSDPermission(this)){
            RequestSDPermissionDialogFragment fragment = RequestSDPermissionDialogFragment.newInstance(action);
            fragment.setStyle(RequestSDPermissionDialogFragment.STYLE_NORMAL, R.style.FMAlertDialogStyle);
            fragment.show(getFragmentManager(), RequestSDPermissionDialogFragment.DIALOG_TAG);
        }else{
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
            SafOperationUtility.getInstance(this).setCallSafAction(action);
        }
    }

    @Override
    public void onRequestConfirmed(int action, String deskLabel) {
        Log.i(TAG,"AnalyzerDupFilesActivity onRequestConfirmed");
        Intent tutorialIntent = new Intent();
        tutorialIntent.setClass(this, TutorialActivity.class);
        tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
        startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
        SafOperationUtility.getInstance(this).setCallSafAction(action);
    }

    @Override
    public void onRequestDenied() {
        //do nothing here, just cancel action
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG,"AnalyzerDupFilesActivity EXTERNAL_STORAGE_PERMISSION_REQ");
                Uri treeUri = data.getData();
                DocumentFile rootFile = DocumentFile.fromTreeUri(this, treeUri);
                if (rootFile != null /*&& rootFile.getName().equals("MicroSD")*/) {
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //delete again
                    List<VFile> deleteList = duplicateFilesListAdapter.getDeleteFilesList();
                    deleteFileInPopup(deleteList.toArray(new LocalVFile[deleteList.size()]));
                }else {
                    callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                }

            }
        } else if (requestCode == REQUEST_SDTUTORIAL && resultCode == Activity.RESULT_OK) {
            Log.i(TAG,"AnalyzerDupFilesActivity REQUEST_SDTUTORIAL");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
        }
    }

    @Override
    public void onUpdateDeleteStart() {
        isUpdateDeleteRunning = true;
        invalidateOptionsMenu();

        loadingLayout.setVisibility(View.VISIBLE);
        if(duplicateFilesListAdapter!=null)
            duplicateFilesListAdapter.setDuplicateFilesList(null);
    }

    @Override
    public void onUpdateDeleteResult(boolean success, long deleteSizes) {
        isUpdateDeleteRunning = false;
        ((FileManagerApplication)getApplication()).startScanDuplicateFiles(ScanDuplicateFilesTask.MODE_SCAN_RECORDED, this, null);

        fileOpened = null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(duplicateFilesListAdapter!=null)
            duplicateFilesListAdapter.clearEllipsizeTextMap();
    }

}
