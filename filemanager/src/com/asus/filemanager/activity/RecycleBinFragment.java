package com.asus.filemanager.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.DisplayItemAdapter.DisplayItem;
import com.asus.filemanager.adapter.RecycleBinAdapter;
import com.asus.filemanager.dialog.MoveDstExistDialogFragment;
import com.asus.filemanager.dialog.RecycleBinInfoDialogFragment;
import com.asus.filemanager.dialog.RestoreDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinUtility;
import com.asus.filemanager.ga.GaRecycleBin;
import com.asus.filemanager.functionaldirectory.CalculateUsableSpaceTask;
import com.asus.filemanager.functionaldirectory.MoveFileTask;
import com.asus.filemanager.functionaldirectory.ScanFunctionalDirectoryTask;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinDisplayItem;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.ui.PathIndicatorView;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.ViewUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public class RecycleBinFragment extends Fragment implements AdapterView.OnItemClickListener
        , AdapterView.OnItemLongClickListener,RecycleBinAdapter.OnSelectedItemsChangedListener
        , ScanFunctionalDirectoryTask.OnScanFunctionalDirectoryResultListener<RecycleBinDisplayItem>, SafOperationUtility.SafActionHandler
        , View.OnClickListener , Observer {

    public static final String TAG = "RecycleBinFragment";

    private String rootPath;
    private HashMap<String, List<VFile>> fileMap = new HashMap<String, List<VFile>>();

    private PathIndicatorView indicatorContainer;
    private HorizontalScrollView indicatorScrollView;
    private LinearLayout loadingLayout;
    private TextView emptyHint;
    private ImageView homeButton;

    private ListView listView;
    private RecycleBinAdapter recycleBinAdapter;

    private ScanFunctionalDirectoryTask<RecycleBinDisplayItem> scanRecycleBinFilesTask;
    private ScanFunctionalDirectoryTask<RecycleBinDisplayItem> mockScanRecycleBinFilesTask;
    private List<RecycleBinDisplayItem> recycleBinFiles;

    private ActionMode mEditMode;
    private FileManagerActivity mActivity = null;

    void setMockScanRecycleBinFilesTask(ScanFunctionalDirectoryTask mockTask) {
        mockScanRecycleBinFilesTask = mockTask;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recyclebin_normal, menu);
        ThemeUtility.setThemeOverflowButton(mActivity, false);
        ThemeUtility.setMenuIconColor(mActivity, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "RecycleBinFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_recycle_bin, container, false);
        findViews(rootView);
        initialList();
        setClickListener();
        initialPathIndicator();
            executeScanRecycleBinFilesTask();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (FileManagerActivity) activity;
        //((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "RecycleBinFragment onDetach");
        //((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
        if(scanRecycleBinFilesTask !=null)
            scanRecycleBinFilesTask.cancel(true);

    }

    public void release() {
        Log.i(TAG, "RecycleBinFragment release");
        if (fileMap != null)
            fileMap.clear();
    }

    private void findViews(View rootView) {
        indicatorContainer = (PathIndicatorView) rootView.findViewById(R.id.fragment_recycle_bin_indicator_container);
        indicatorScrollView = (HorizontalScrollView) rootView.findViewById(R.id.fragment_recycle_bin_indicator_scrollView);
        loadingLayout = (LinearLayout) rootView.findViewById(R.id.fragment_recycle_bin_loading);
        listView = (ListView) rootView.findViewById(R.id.fragment_recycle_bin_listview);
        emptyHint = (TextView) rootView.findViewById(R.id.fragment_recycle_bin_empty);
        homeButton = (ImageView) rootView.findViewById(R.id.path_home);
    }

    private void setClickListener() {
        // TODO Auto-generated method stub
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        recycleBinAdapter.setOnSelectedItemsChangedListener(this);
        homeButton.setOnClickListener(this);
    }

    private void initialList() {
        if (recycleBinFiles == null)
        recycleBinFiles = new ArrayList<>();
        if (recycleBinAdapter == null)
        recycleBinAdapter = new RecycleBinAdapter(getActivity(), recycleBinFiles);
        listView.setAdapter(recycleBinAdapter);
        updateEmptyView();
    }

    private void updateAnalyserListAdapter() {
        Log.i(TAG, "RecycleBinFragment updateAnalyserListAdapter");
        recycleBinAdapter.setFiles(recycleBinFiles);
        recycleBinAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        //show empty hint
        if (recycleBinFiles.size() > 0)
            emptyHint.setVisibility(View.GONE);
        else
            emptyHint.setVisibility(View.VISIBLE);
    }

    private void executeScanRecycleBinFilesTask() {
        if (mockScanRecycleBinFilesTask != null) return;
        if (scanRecycleBinFilesTask != null) {
            scanRecycleBinFilesTask.cancel(true);
        }
        scanRecycleBinFilesTask = new ScanFunctionalDirectoryTask(RecycleBinUtility.getInstance(), this);
        scanRecycleBinFilesTask.execute(((FileManagerApplication) getActivity().getApplication()).getStorageFile());
    }

    private void initialPathIndicator() {
        indicatorContainer.setRootName("");
        indicatorContainer.setPath("", getString(R.string.tools_recycle_bin));
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(TAG, "RecycleBinFragment onHiddenChanged, is hidden?" + hidden);
        if (!hidden) {
            executeScanRecycleBinFilesTask();
        }
        else {
            recycleBinAdapter.deselectAllItems();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        Log.i(TAG, "RecycleBinFragment onItemClick");
        if (recycleBinAdapter.isLongClickMode())
            updateItemSelection(position);
        else
            showRecycleBinInfo((RecycleBinDisplayItem)recycleBinFiles.get(position));
        //start analysis
        /*if (recycleBinAdapter.isLongClickMode()) {
            RecycleBinDisplayItem recycleBinFile = recycleBinFiles.get(position);
            recycleBinFile.setChecked(!recycleBinFile.isChecked());
            if (!recycleBinFile.isChecked() && recycleBinAdapter.getSelectedItemMap().size() == 1)
                recycleBinAdapter.deselectAllItems();

            recycleBinAdapter.notifyDataSetChanged();
        } else if (fileMap.get(rec.getAbsolutePath()).get(position).isDirectory() &&
                fileMap.get(rec.getAbsolutePath()).get(position).canRead()) {
            rec = fileMap.get(rec.getAbsolutePath()).get(position);
            executeScanRecycleBinFilesTask();
            indicatorContainer.setPath(rootPath, rec);
            movePathIndicatorRight();
        } else if (!fileMap.get(rec.getAbsolutePath()).get(position).isDirectory()){
            //open file
            FileUtility.openFile(getActivity(),fileMap.get(rec.getAbsolutePath()).get(position),false,false);
        }*/
    }

    private void showRecycleBinInfo(DisplayItem item) {
        if (item == null)
            return;
        RecycleBinInfoDialogFragment.newInstance(getActivity(), (RecycleBinDisplayItem)item).show(getFragmentManager(), "RecycleBinInfoDialogFragment");
        GaRecycleBin.getInstance().sendAction(getActivity(), GaRecycleBin.Action.Information, 1);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        /*updateItemSelection(recycleBinFiles.get(position));
        if (recycleBinAdapter.isLongClickMode() && mEditMode == null) {
            getActivity().startActionMode(new EditModeCallback());
        }*/
        updateItemSelection(position);
        return true;
    }

    private void updateItemSelection(int position) {
        RecycleBinDisplayItem itemClicked = recycleBinFiles.get(position);
        if (itemClicked.isChecked())
            recycleBinAdapter.deselectItem(position, itemClicked);
        else
            recycleBinAdapter.selectItem(position, itemClicked);
    }

    @Override
    public void onSelectedItemsChanged(){
        Log.i("test", "RecycleBinFragment onSelectedItemsChanged");
        if (recycleBinAdapter.isLongClickMode()) {
            if(mEditMode == null) {
                getActivity().startActionMode(new EditModeCallback());
            }
            else {
                mEditMode.invalidate();
            }
        } else if (mEditMode != null) {
            mEditMode.finish();
            mEditMode = null;
        }
    }

    @Override
    public void onScanStart() {
        // TODO Auto-generated method stub
        Log.i(TAG, "RecycleBinFragment onScanStart");
        loadingLayout.setVisibility(View.VISIBLE);
        recycleBinAdapter.deselectAllItems();
    }

    @Override
    public void onScanResult(List<RecycleBinDisplayItem> recycleBinFiles) {
        // TODO Auto-generated method stub
        Log.i(TAG, "RecycleBinFragment onFilesSizesResult: " + recycleBinFiles.size());
        //close loading
        loadingLayout.setVisibility(View.GONE);
        this.recycleBinFiles = recycleBinFiles;
        updateAnalyserListAdapter();
    }

    public boolean onBackPressed() {
        Log.i(TAG, "RecycleBinFragment onBackPressed");
        if (recycleBinAdapter.isLongClickMode()) {
            recycleBinAdapter.deselectAllItems();
        }
        return true;
    }

    //delete handler
    private Handler editHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FileListFragment.MSG_DELET_COMPLETE:
                    closeEditFragmentByTag("DeleteDialogFragment");
                    break;
                case FileListFragment.MSG_RESTORE_COMPLETE:
                    closeEditFragmentByTag("RestoreDialogFragment");
                    break;
                case MoveFileTask.MSG_SHOW_MOVE_DST_EXIST_FRAGMENT:
                    MoveDstExistDialogFragment fragment = (MoveDstExistDialogFragment) msg.obj;
                    if(!fragment.isAdded()){
                        fragment.show(getFragmentManager(), "MoveDstExistDialogFragment");
                    }
                    break;
            }
        }
    };

        private void closeEditFragmentByTag(String tag) {
            DialogFragment progressFragment = (DialogFragment)getFragmentManager().findFragmentByTag(tag);
            if (progressFragment != null)
                progressFragment.dismissAllowingStateLoss();
            EditorUtility.sEditIsProcessing = false;
            executeScanRecycleBinFilesTask();
        }

    public Handler getHandler()
    {
        return editHandler;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (null != data){
            /*Bundle aBundle = (Bundle)data;
            String event;
            String path;
            event = aBundle.getString(VolumeStateObserver.KEY_EVENT);
            path = aBundle.getString(VolumeStateObserver.KEY_PATH);
            if (path == null) {
                return;
            }
            getActivity().setResult(Activity.RESULT_OK);
            if (event.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Log.i(TAG, "RecycleBinFragment ACTION_MEDIA_UNMOUNTED");
                if (rec != null && (rec.getAbsolutePath().startsWith(path))) {
                    getActivity().finish();
                }
            }*/

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.path_home:
                mActivity.switchFragmentTo(FileManagerActivity.FragmentType.HOME_PAGE);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clean_all_action:
                deleteFileInPopup(collectSelectedVFile(recycleBinFiles.iterator(), recycleBinFiles.size()));
                break;
            case R.id.restore_all_action:
                restoreFiles(collectSelectedVFile(recycleBinFiles.iterator(), recycleBinFiles.size()));
                break;
        }
        return true;
    }

    @Override
    public void handleAction(int action) {
        //do nothing now
    }

    private class EditModeCallback implements ActionMode.Callback {

        private TextView mSelectedCountText;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.i(TAG, "RecycleBinFragment EditModeCallback onCreateActionMode");
            //add select count view
            LayoutInflater inflaterView = getActivity().getLayoutInflater();
            View view = inflaterView.inflate(R.layout.editmode_actionbar, null);
            mode.setCustomView(view);
            mSelectedCountText = (TextView) view.findViewById(R.id.actionbar_text);

            //add items
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.recyclebin_edit, menu);

            mEditMode = mode;

            updateSelectHint();
            updateMenuItemBySelectedCount(menu);
            return true;
        }

        private void updateSelectHint() {
            int longClickCount = recycleBinAdapter.getSelectedItemMap().size();
            String itemSelected = getResources().getQuantityString(R.plurals.number_selected_items, longClickCount, longClickCount);
            mSelectedCountText.setText(itemSelected);
        }

        private void updateSelectAction() {
            Log.i(TAG, "RecycleBinFragment EditModeCallback updateSelectAction");
            int longClickCount = recycleBinAdapter.getSelectedItemMap().size();
            if (longClickCount == recycleBinAdapter.getCount()) {
                recycleBinAdapter.deselectAllItems();
            } else {
                recycleBinAdapter.selectAllItems();
            }
        }

        private boolean needTriggerOnBackPressed()
        {
            int longClickCount = recycleBinAdapter.getSelectedItemMap().size();
            if (longClickCount == 0)
            {
                return false;
            }
            return true;
        }

        private void updateMenuItemBySelectedCount(Menu menu) {
            Log.i(TAG, "RecycleBinFragment EditModeCallback updateMenuItemBySelectedCount");
            int longClickCount = recycleBinAdapter.getSelectedItemMap().size();
            MenuItem selectItem = menu.findItem(R.id.select_all_action);
            MenuItem deselectItem = menu.findItem(R.id.deselect_all_action);
            MenuItem infoItem = menu.findItem(R.id.info_action);

            selectItem.setVisible(longClickCount != recycleBinAdapter.getCount());
            deselectItem.setVisible(longClickCount == recycleBinAdapter.getCount());
            infoItem.setVisible(longClickCount == 1);
        }
        //mEditMode.invalidate() will call this function
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.i(TAG, "RecycleBinFragment onPrepareActionMode");
            updateMenuItemBySelectedCount(menu);
            updateSelectHint();
            ThemeUtility.setThemeOverflowButton(mActivity, false);
            ThemeUtility.setMenuIconColor(getActivity().getApplicationContext(), menu);
            return true;
        }

        // @Override
        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
            Log.i(TAG, "RecycleBinFragment onActionItemClicked");
            switch (item.getItemId()) {
                case R.id.select_all_action:
                case R.id.deselect_all_action:
                    updateSelectAction();
                    break;
                case R.id.clean_action:
                    deleteFileInPopup(collectSelectedVFile(recycleBinAdapter.getSelectedItemMap().values().iterator(), recycleBinAdapter.getSelectedItemMap().size()));
                    break;
                case R.id.restore_action:
                    restoreFiles(collectSelectedVFile(recycleBinAdapter.getSelectedItemMap().values().iterator(), recycleBinAdapter.getSelectedItemMap().size()));
                    break;
                case R.id.info_action:
                    showRecycleBinInfo(recycleBinAdapter.getSelectedItemMap().values().iterator().next());
                    break;
            }
            return true;
        }

        // @Override
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.i(TAG, "RecycleBinFragment onDestroyActionMode");
            //mDeleteFilePool.clear();
            if(needTriggerOnBackPressed())
                onBackPressed();
        }
    }

    private VFile[] collectSelectedVFile(Iterator<RecycleBinDisplayItem> iterator, int deleteFileCount) {
        VFile[] selectedFiles = new VFile[deleteFileCount];
        for (int i = 0; iterator.hasNext() && i < selectedFiles.length; i++) {
            selectedFiles[i] = iterator.next().getCurrentVFile();
            selectedFiles[i].setChecked(true);
        }
        return selectedFiles;
    }

    public void restoreFiles(final VFile[] restoreFiles) {
        if(restoreFiles.length<1)
            return;
        Log.i(TAG, "RecycleBinFragment restoreFiles");
        if (!mActivity.isNeedPermission(restoreFiles, SafOperationUtility.ACTION_RESTORE)) {
        RestoreDialogFragment restoreDialogFragment = RestoreDialogFragment.newInstance();
        if(!restoreDialogFragment.isAdded()){
            restoreDialogFragment.show(getFragmentManager(), "RestoreDialogFragment");
        }
            new CalculateUsableSpaceTask(new CalculateUsableSpaceTask.OnSpaceCalculatedListener() {
                @Override
                public void onSpaceCalculated(boolean isSufficient) {
                    if (isSufficient) {
        EditorAsyncHelper.restoreFile(restoreFiles, editHandler);
                    } else {
                        ToastUtility.show(getActivity(), R.string.no_space_fail, Toast.LENGTH_LONG);
                        closeEditFragmentByTag("RestoreDialogFragment");
                    }
                }
            }).execute(restoreFiles);
            GaRecycleBin.getInstance().sendAction(getActivity(), GaRecycleBin.Action.Restore, restoreFiles.length);
    }
    }

    public void deleteFileInPopup(VFile[] deleteFiles) {
        if(deleteFiles.length<1)
            return;
        Log.i(TAG, "RecycleBinFragment deleteFileInPopup");

        EditPool deleteFilePool = new EditPool();
        deleteFilePool.setFiles(deleteFiles, true);

        VFile[] filesToDelete = deleteFilePool.getFiles();
        if (!mActivity.isNeedPermission(filesToDelete, SafOperationUtility.ACTION_DELETE))
            mActivity.displayDialog(FileManagerActivity.DialogType.TYPE_DELETE_DIALOG, deleteFilePool);
            /*boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(getActivity()).isNeedToWriteSdToAppFolder(deleteFiles[0].getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                warnDialog.show(RecycleBinFragment.this.getFragmentManager(),"WarnKKSDPermissionDialogFragment");
            }else if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(deleteFiles[0].getAbsolutePath())) {
                ((AnalyzerAllFilesActivity)getActivity()).callSafChoose(SafOperationUtility.ACTION_DELETE);
            } else {
                DeleteDialogFragment deleteDialogFragment = DeleteDialogFragment.newInstance(mDeleteFilePool, DeleteDialogFragment.TYPE_DELETE_DIALOG);
                if(!deleteDialogFragment.isAdded()){
                    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
                }
            }*/
    }
}
