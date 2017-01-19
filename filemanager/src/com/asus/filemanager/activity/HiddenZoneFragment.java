package com.asus.filemanager.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.adapter.DisplayItemAdapter;
import com.asus.filemanager.adapter.HiddenZoneAdapter;
import com.asus.filemanager.adapter.RecycleBinAdapter;
import com.asus.filemanager.dialog.FileExistDialogFragment;
import com.asus.filemanager.dialog.InfoDialogFragment;
import com.asus.filemanager.dialog.MoveDstExistDialogFragment;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.dialog.RenameDialogFragment;
import com.asus.filemanager.dialog.RestoreDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.functionaldirectory.CalculateUsableSpaceTask;
import com.asus.filemanager.functionaldirectory.MoveFileTask;
import com.asus.filemanager.functionaldirectory.ScanFunctionalDirectoryTask;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneDisplayItem;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.ga.GaHiddenCabinet;
import com.asus.filemanager.ga.GaHiddenCabinet.FingerprintToUnlockStatus;
import com.asus.filemanager.ga.GaRecycleBin;
import com.asus.filemanager.hiddenzone.activity.SetupAccountActivity;
import com.asus.filemanager.hiddenzone.activity.SetupFingerprintDialog;
import com.asus.filemanager.hiddenzone.activity.SetupFingerprintDialog.SetupFingerprintDialogListener;
import com.asus.filemanager.hiddenzone.activity.SetupPasswordActivity;
import com.asus.filemanager.hiddenzone.activity.UnlockActivity;
import com.asus.filemanager.hiddenzone.encrypt.FingerprintUtils;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.ui.PathIndicatorView;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.ViewUtility;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public class HiddenZoneFragment extends Fragment implements AdapterView.OnItemClickListener
        , AdapterView.OnItemLongClickListener,RecycleBinAdapter.OnSelectedItemsChangedListener
        , ScanFunctionalDirectoryTask.OnScanFunctionalDirectoryResultListener<HiddenZoneDisplayItem>, SafOperationUtility.SafActionHandler
        , PathIndicatorView.OnPathIndicatorListener, View.OnClickListener
        , MoveToDialogFragment.OnFolderSelectListener , Observer {

    public static final String TAG = "HiddenZoneFragment";

    private HashMap<String, List<VFile>> fileMap = new HashMap<String, List<VFile>>();

    private PathIndicatorView indicatorContainer;
    private HorizontalScrollView indicatorScrollView;
    private LinearLayout loadingLayout;
    private TextView emptyHint;
    private ImageView homeButton;

    private ListView listView;
    private HiddenZoneAdapter hiddenZoneAdapter;

    private ScanFunctionalDirectoryTask<HiddenZoneDisplayItem> scanFunctionalDirectoryTask;
    private ScanFunctionalDirectoryTask<HiddenZoneDisplayItem> mockScanHiddenZoneFilesTask;
    private List<HiddenZoneDisplayItem> hiddenZoneDisplayItems;

    private ActionMode mEditMode;
    private FileManagerActivity mActivity = null;
    private List<HiddenZoneDisplayItem> browseRoute = null;
    private boolean mHasPendingRequestAutoLock;
    private boolean mIsAlreadyRequestAutoLock;
    private boolean mIsGuidingUserToRegisterFingerprint;

    private static final int REQUEST_UNLOCK_FROM_AUTO_LOCK = 1;
    private static final int REQUEST_UNLOCK_TO_SETUP_ACCOUNT = 2;
    private static final int REQUEST_SETUP_ACCOUNT = 3;
    private static final int REQUEST_UNLOCK_TO_CHANGE_PASSWORD = 4;
    private static final int REQUEST_REGISTER_NEW_PASSWORD = 5;

    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: " + intent);

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (mActivity != null && isVisible()) {
                    if (isResumed()) {
                        requestAutoLock();
                    } else if (!mIsAlreadyRequestAutoLock) {
                        // request Auto Lock when fragment onStart() is called.
                        mHasPendingRequestAutoLock = true;
                    }
                }
            }

        }
    };

    void setMockScanHiddenZoneFilesTask(ScanFunctionalDirectoryTask mockTask) {
        mockScanHiddenZoneFilesTask = mockTask;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Register screen off receiver for this fragment is shown first time.
        // For the second/third... time we will register this receiver after
        // onHiddenChanged is called.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mActivity.registerReceiver(mScreenOffReceiver, intentFilter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.hidden_zone_menu, menu);
        ThemeUtility.setThemeOverflowButton(mActivity, false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ThemeUtility.setThemeOverflowButton(mActivity, false);

        MenuItem setupAccount = menu.findItem(R.id.setup_account);
        if (setupAccount != null) {
            PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
            if (!pinCodeAccessHelper.hasRecoveryAccount()
                    && Utility.isEnabledAndInstalledPackage(
                            mActivity, GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)) {
                setupAccount.setVisible(true);
            } else {
                setupAccount.setVisible(false);
            }
        }
        MenuItem menuFpToUnlock = menu.findItem(R.id.fingerprint_to_unlock);
        if (menuFpToUnlock != null) {
            if (FingerprintUtils.isSupportFingerprintFeature(mActivity)) {
                boolean isFingerprintToUnlockOn
                        = FingerprintUtils.getUserHasAndAllowFingerprint(mActivity);
                menuFpToUnlock.setChecked(isFingerprintToUnlockOn);
                GaHiddenCabinet.getInstance().sendFingerprintToEnableEvent(mActivity,
                        isFingerprintToUnlockOn ? FingerprintToUnlockStatus.ENABLE.ordinal()
                                : FingerprintToUnlockStatus.DISABLE.ordinal());
            } else {
                menuFpToUnlock.setVisible(false);
                GaHiddenCabinet.getInstance().sendFingerprintToEnableEvent(mActivity,
                        FingerprintToUnlockStatus.NOT_SUPPORT.ordinal());
            }
        }

        ThemeUtility.setMenuIconColor(mActivity, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "HiddenZoneFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_hidden_zone, container, false);
        findViews(rootView);
        initialList();
        setClickListener();
        updatePathIndicator();
        browse(browseRoute.size() - 1);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mActivity != null && isVisible()) {
            if (mHasPendingRequestAutoLock) {
                requestAutoLock();
            } else {
                mIsAlreadyRequestAutoLock = false;
            }

            // Keep in HiddenZone
            browse(browseRoute.size() - 1);

            if (mIsGuidingUserToRegisterFingerprint) {
                if (FingerprintUtils.hasEnrolledFingerprints(mActivity)) {
                    FingerprintUtils.setUserAllowFingerprint(mActivity, true);
                    GaHiddenCabinet.getInstance().sendFingerprintToEnableEvent(
                            mActivity, FingerprintToUnlockStatus.ENABLE.ordinal());
                }
                mIsGuidingUserToRegisterFingerprint = false;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (FileManagerActivity) activity;
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (FileManagerActivity) context;
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "HiddenZoneFragment onDetach");
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
        if(scanFunctionalDirectoryTask !=null)
            scanFunctionalDirectoryTask.cancel(true);
    }

    public void release() {
        Log.i(TAG, "HiddenZoneFragment release");
        if (fileMap != null)
            fileMap.clear();
    }

    private void findViews(View rootView) {
        indicatorContainer = (PathIndicatorView) rootView.findViewById(R.id.fragment_hidden_zone_indicator_container);
        indicatorScrollView = (HorizontalScrollView) rootView.findViewById(R.id.fragment_hidden_zone_indicator_scrollView);
        loadingLayout = (LinearLayout) rootView.findViewById(R.id.fragment_hidden_zone_loading);
        listView = (ListView) rootView.findViewById(R.id.fragment_hidden_zone_listview);
        emptyHint = (TextView) rootView.findViewById(R.id.fragment_hidden_zone_empty);
        homeButton = (ImageView) rootView.findViewById(R.id.path_home);
    }

    private void setClickListener() {
        // TODO Auto-generated method stub
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        hiddenZoneAdapter.setOnSelectedItemsChangedListener(this);
        homeButton.setOnClickListener(this);
        indicatorContainer.setOnPathIndicatorListener(this);
    }

    private void initialList() {
        if (hiddenZoneDisplayItems == null)
        hiddenZoneDisplayItems = new ArrayList<>();
        if (hiddenZoneAdapter == null)
        hiddenZoneAdapter = new HiddenZoneAdapter(getActivity(), hiddenZoneDisplayItems);
        listView.setAdapter(hiddenZoneAdapter);
        updateEmptyView();
        if (browseRoute == null)
        browseRoute = new ArrayList<>();
    }

    private void updateAnalyserListAdapter() {
        Log.i(TAG, "HiddenZoneFragment updateAnalyserListAdapter");
        hiddenZoneAdapter.setFiles(hiddenZoneDisplayItems);
        hiddenZoneAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        //show empty hint
        if (hiddenZoneDisplayItems.size() > 0)
            emptyHint.setVisibility(View.GONE);
        else
            emptyHint.setVisibility(View.VISIBLE);
    }

    private void executeScanHiddenZoneFilesTask(HiddenZoneDisplayItem displayItem) {
        if (mockScanHiddenZoneFilesTask != null) return;
        if (scanFunctionalDirectoryTask != null) {
            scanFunctionalDirectoryTask.cancel(true);
        }
        scanFunctionalDirectoryTask = new ScanFunctionalDirectoryTask<HiddenZoneDisplayItem>(HiddenZoneUtility.getInstance(), displayItem, this) {
        };
        if (displayItem == null)
            scanFunctionalDirectoryTask.execute(((FileManagerApplication) getActivity().getApplication()).getStorageFile());
        else
            scanFunctionalDirectoryTask.execute(displayItem.getOriginalFile());
    }

    private void updatePathIndicator() {
        indicatorContainer.setRootName("");
        indicatorContainer.setPath("", getCurrentPath());
        movePathIndicatorRight();
    }

    private String getCurrentPath() {
        String path = getString(R.string.tools_hidden_zone);
        for (HiddenZoneDisplayItem browsed : browseRoute)
            path = path + "/" + browsed.getName();
        return path;
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
    public void onPathClick(String path) {
        String[] route = path.split("/");
        browse(route.length - 3);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            browse(browseRoute.size() - 1);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            mActivity.registerReceiver(mScreenOffReceiver, intentFilter);
        }
        else {
            hiddenZoneAdapter.deselectAllItems();
            browseRoute.clear();
            try {
                mActivity.unregisterReceiver(mScreenOffReceiver);
            } catch (IllegalArgumentException e) {
                // catch the exception of non-paired register/unregister receiver
                // java.lang.IllegalArgumentException: Receiver not registered
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        Log.i(TAG, "HiddenZoneFragment onItemClick");
        if (hiddenZoneAdapter.isLongClickMode())
            updateItemSelection(position);
        else {
            HiddenZoneDisplayItem selectItem = (HiddenZoneDisplayItem) hiddenZoneDisplayItems.get(position);
            VFile fileClicked = selectItem.getCurrentVFile();
            if (fileClicked.isDirectory()) {
                browseRoute.add(selectItem);
                browse(browseRoute.size() - 1);
            }
            else {
                /*String extension = fileClicked.getExtensiontName().toLowerCase();
                if (extension.compareTo("rar") == 0 || extension.compareTo("zip") == 0)
                    ToastUtility.show(mActivity, R.string.can_not_open_in_hidden_zone, Toast.LENGTH_LONG);
                else*/
                    FileUtility.openFile(getActivity(), fileClicked, false, false, true, false);
            }
        }
        //start analysis
        /*if (hiddenZoneAdapter.isLongClickMode()) {
            RecycleBinDisplayItem recycleBinFile = hiddenZoneDisplayItems.get(position);
            recycleBinFile.setChecked(!recycleBinFile.isChecked());
            if (!recycleBinFile.isChecked() && hiddenZoneAdapter.getSelectedItemMap().size() == 1)
                hiddenZoneAdapter.deselectAllItems();

            hiddenZoneAdapter.notifyDataSetChanged();
        } else if (fileMap.get(rec.getAbsolutePath()).get(position).isDirectory() &&
                fileMap.get(rec.getAbsolutePath()).get(position).canRead()) {
            rec = fileMap.get(rec.getAbsolutePath()).get(position);
            executeScanHiddenZoneFilesTask();
            indicatorContainer.setPath(rootPath, rec);
            movePathIndicatorRight();
        } else if (!fileMap.get(rec.getAbsolutePath()).get(position).isDirectory()){
            //open file
            FileUtility.openFile(getActivity(),fileMap.get(rec.getAbsolutePath()).get(position),false,false);
        }*/
    }

    private void browse(int index) {
        HiddenZoneDisplayItem selectItem = null;
        if (index < 0) {
            browseRoute.clear();
        }
        else {
            for (int i = browseRoute.size() - 1; i > index ; i--)
                browseRoute.remove(i);
            selectItem = browseRoute.get(index);
        }
        updatePathIndicator();
        executeScanHiddenZoneFilesTask(selectItem);
    }

    private void requestAutoLock() {
        PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
        if (pinCodeAccessHelper.hasPinCode() && !mIsAlreadyRequestAutoLock) {
            startActivityForResult(new Intent(mActivity, UnlockActivity.class),
                    REQUEST_UNLOCK_FROM_AUTO_LOCK);
            mIsAlreadyRequestAutoLock = true;
        }
        mHasPendingRequestAutoLock = false;
    }

    private void showHiddenZoneInfo(DisplayItemAdapter.DisplayItem item) {
        if (item == null)
            return;
        InfoDialogFragment.newInstance(item.getOriginalFile(), getCurrentPath() + "/" + item.getName())
                .show(getFragmentManager(), "RecycleBinInfoDialogFragment");
        //GaRecycleBin.getInstance(getActivity()).sendAction(getActivity(), GaRecycleBin.Action.Information, 1);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        /*updateItemSelection(hiddenZoneDisplayItems.get(position));
        if (hiddenZoneAdapter.isLongClickMode() && mEditMode == null) {
            getActivity().startActionMode(new EditModeCallback());
        }*/
        updateItemSelection(position);
        return true;
    }

    private void updateItemSelection(int position) {
        HiddenZoneDisplayItem itemClicked = hiddenZoneDisplayItems.get(position);
        if (itemClicked.isChecked())
            hiddenZoneAdapter.deselectItem(position, itemClicked);
        else
            hiddenZoneAdapter.selectItem(position, itemClicked);
    }

    @Override
    public void onSelectedItemsChanged(){
        Log.i("test", "HiddenZoneFragment onSelectedItemsChanged");
        if (hiddenZoneAdapter.isLongClickMode()) {
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
        Log.i(TAG, "HiddenZoneFragment onScanStart");
        loadingLayout.setVisibility(View.VISIBLE);
        hiddenZoneAdapter.deselectAllItems();
    }

    @Override
    public void onScanResult(List<HiddenZoneDisplayItem> hiddenZoneFiles) {
        Log.i(TAG, "HiddenZoneFragment onFilesSizesResult: " + hiddenZoneFiles.size());
        //close loading
        loadingLayout.setVisibility(View.GONE);
        this.hiddenZoneDisplayItems = hiddenZoneFiles;
        updateAnalyserListAdapter();

        // only send total files count event to GA server when scan the root of Hidden Cabinet.
        String currentPath = getCurrentPath();
        if (currentPath != null
                && currentPath.equals(getString(R.string.tools_hidden_zone))) {
            GaHiddenCabinet.getInstance().sendTotalFilesEvent(mActivity, hiddenZoneFiles.size());
        }
    }

    public boolean onBackPressed() {
        Log.i(TAG, "HiddenZoneFragment onBackPressed");
        if (hiddenZoneAdapter.isLongClickMode()) {
            hiddenZoneAdapter.deselectAllItems();
        } else if (browseRoute.size() > 0){
            browse(browseRoute.size() - 2);
        } else {
            mActivity.switchFragmentTo(FileManagerActivity.FragmentType.HOME_PAGE);
        }
        return true;
    }

    //delete handler
    private Handler editHandler = new Handler() {
        private double mTotalSize;
        private double mCountSize;
        public int mPercent;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FileListFragment.MSG_DELET_COMPLETE:
                    closeEditFragmentByTag("DeleteDialogFragment");
                    break;
                case FileListFragment.MSG_RESTORE_COMPLETE:
                    closeEditFragmentByTag("RestoreDialogFragment");
                    break;
                case FileListFragment.MSG_RENAME_COMPLETE:
                    closeEditFragmentByTag("RenameDialogFragment");
                    break;
                case MoveFileTask.MSG_SHOW_MOVE_DST_EXIST_FRAGMENT:
                    MoveDstExistDialogFragment fragment = (MoveDstExistDialogFragment) msg.obj;
                    if(!fragment.isAdded()){
                        fragment.show(getFragmentManager(), "MoveDstExistDialogFragment");
                    }
                    break;
                case FileListFragment.MSG_PASTE_INIT:
                    Log.d(TAG, "handleMessage: MSG_PASTE_INIT");
                    PasteDialogFragment currentDialog = (PasteDialogFragment)getFragmentManager().findFragmentByTag("PasteDialogFragment");
                    PasteDialogFragment initDialog = ((FileManagerActivity)getActivity()).getVisiblePasteDialog();
                    if (initDialog != null && !initDialog.equals(currentDialog)
                            && initDialog.getDialog()!=null && initDialog.getDialog().isShowing()) {
                        initDialog.dismissAllowingStateLoss();
                    }
                    mTotalSize = (long) msg.obj;
                    mCountSize = 0;
                    mPercent = -1;
                    setProgress(0, 0);
                    break;
                case FileListFragment.MSG_PASTE_PROG_FILE:
                    EditResult r = (EditResult) msg.obj;
                    mCountSize += r.numbytes;
                    setProgress(mTotalSize == 0 ? 0 : (int) ((mCountSize) * 100 / mTotalSize), mCountSize);
                    break;
                case FileListFragment.MSG_PASTE_PROG_SIZE:
                    EditResult editResult = (EditResult) msg.obj;
                    //calculate when files changed
                    /*if (mTotalSize<(mCountSize + editResult.numbytes) && mEditPool.getFiles()!=null && ((mEditPool.getFiles())[0]).getVFieType()!= VFile.VFileType.TYPE_CLOUD_STORAGE) {
                        mTotalSize = FileUtility.getArrayTotalLength(mEditPool.getFiles()).numSize;
                    }*/
                    setProgress(mTotalSize == 0 ? 0 : (int) ((mCountSize + editResult.numbytes) * 100 / mTotalSize), mCountSize + editResult.numbytes);
                    ItemOperationUtility.updateNotificationBar(editResult.mCurrentFileName, (int) (mTotalSize / 1024), (int) (mCountSize + editResult.numbytes) / 1024, getActivity());
                    break;
                case FileListFragment.MSG_PASTE_PAUSE:
                    PasteDialogFragment pasteDialog = (PasteDialogFragment) getFragmentManager().findFragmentByTag("PasteDialogFragment");

                    FileExistDialogFragment fileExistFragment = FileExistDialogFragment.newInstance((EditorUtility.ExistPair) msg.obj);
                    fileExistFragment.show(getFragmentManager(), "VFileExistDialogFragment");

                    if (pasteDialog != null) {
                        pasteDialog.dismissAllowingStateLoss();
                    }
                    break;
                case FileListFragment.MSG_PASTE_COMPLETE: {
                    Log.d(TAG, "handleMessage: MSG_PASTE_COMPLETE");
                    EditorAsyncHelper.WorkerArgs args = (EditorAsyncHelper.WorkerArgs) msg.obj;
                    closeEditFragmentByTag("PasteDialogFragment");
                    //pasteComplete();
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    ItemOperationUtility.cancelNotification(getActivity());
                } break;
            }
        }

        public void pasteComplete() {
            /*if (!ItemOperationUtility.isReadyToPaste) {
                mEditPool.clear();
                getActivity().invalidateOptionsMenu();
            }
            getCurrentUsingAdapter().notifyDataSetChanged();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);*/
        }

        public void setProgress(int n, double countSize) {
            if (getFragmentManager() != null) {
                PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getFragmentManager().findFragmentByTag("PasteDialogFragment");
                // Fix TT-222081
                if (mPercent != n && pasteDialogFragment != null) {
                    mPercent = n;
                    pasteDialogFragment.setProgress(mPercent, countSize,mTotalSize);
                }
            }
        }
    };

    private void closeEditFragmentByTag(String tag) {
        if (getFragmentManager() != null) {
        DialogFragment progressFragment = (DialogFragment)getFragmentManager().findFragmentByTag(tag);
        if (progressFragment != null)
            progressFragment.dismissAllowingStateLoss();
        }
        EditorUtility.sEditIsProcessing = false;
        browse(browseRoute.size() - 1);
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
                Log.i(TAG, "HiddenZoneFragment ACTION_MEDIA_UNMOUNTED");
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
                deleteFileInPopup(collectSelectedVFile(hiddenZoneDisplayItems.iterator(), hiddenZoneDisplayItems.size()));
                break;
            case R.id.restore_all_action:
                restoreFiles(collectSelectedVFile(hiddenZoneDisplayItems.iterator(), hiddenZoneDisplayItems.size()));
                break;
            case R.id.setup_account: {
                Intent intent = new Intent(mActivity, UnlockActivity.class);
                startActivityForResult(intent, REQUEST_UNLOCK_TO_SETUP_ACCOUNT);
                break;
            }
            case R.id.change_password: {
                Intent intent = new Intent(mActivity, UnlockActivity.class);
                startActivityForResult(intent, REQUEST_UNLOCK_TO_CHANGE_PASSWORD);
                break;
            }
            case R.id.fingerprint_to_unlock: {
                if (FingerprintUtils.hasEnrolledFingerprints(mActivity)) {
                    boolean isEnableFpToUnlock = item.isChecked();
                    FingerprintUtils.setUserAllowFingerprint(
                            mActivity, !isEnableFpToUnlock);
                    GaHiddenCabinet.getInstance().sendFingerprintToEnableEvent(
                            mActivity, (!isEnableFpToUnlock) ? GaHiddenCabinet.FingerprintToUnlockStatus.ENABLE.ordinal()
                                    : GaHiddenCabinet.FingerprintToUnlockStatus.DISABLE.ordinal());
                } else {
                    DialogFragment newFragment = SetupFingerprintDialog.newInstance(
                            new SetupFingerprintDialogListener() {
                                @Override
                                public void onSetupFingerprintPressed() {
                                    mIsGuidingUserToRegisterFingerprint = true;
                                }
                            });
                    newFragment.show(getFragmentManager(), SetupFingerprintDialog.TAG);
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_UNLOCK_FROM_AUTO_LOCK) {
                boolean unlockByFingerprint = data != null ?
                        data.getBooleanExtra(UnlockActivity.KEY_UNLOCK_VIA_FINGERPRINT, false) : false;
                GaHiddenCabinet.getInstance().sendUnlockFromAutoLockEvent(
                        mActivity, unlockByFingerprint);
            } else if (requestCode == REQUEST_UNLOCK_TO_SETUP_ACCOUNT) {
                startActivity(new Intent(mActivity, SetupAccountActivity.class));
            } else if (requestCode == REQUEST_UNLOCK_TO_CHANGE_PASSWORD) {
                Intent intent = new Intent(mActivity, SetupPasswordActivity.class);
                startActivityForResult(intent, REQUEST_REGISTER_NEW_PASSWORD);
            } else if (requestCode == REQUEST_REGISTER_NEW_PASSWORD) {
                Toast.makeText(mActivity,
                        R.string.hidden_zone_set_password_success,Toast.LENGTH_SHORT).show();
                PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
                GaHiddenCabinet.getInstance().sendChangePasswordEvent(
                        mActivity, pinCodeAccessHelper.hasRecoveryAccount());
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_UNLOCK_FROM_AUTO_LOCK) {
                mActivity.switchFragmentTo(FragmentType.HOME_PAGE);
            }
        }

        // We don't care about SetupAccountActivity resultCode for GA.
        if (requestCode == REQUEST_SETUP_ACCOUNT) {
            PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
            GaHiddenCabinet.getInstance().sendSetupAccountEvent(
                    mActivity, pinCodeAccessHelper.hasRecoveryAccount());
        }
    }

    @Override
    public void handleAction(int action) {
        //do nothing now
    }

    @Override
    public void onFolderSelected(VFile selectedVFile, Bundle data) {
        Log.d("test", "folder selected: " + selectedVFile.getPath());
        EditPool editPool = (EditPool)data.getSerializable("file");
        editPool.setExtraBoolean(data.getInt(MoveToDialogFragment.DIALOG_TYPE) == R.id.move_to_action);
        editPool.setPasteVFile(selectedVFile);
        editPool.setTargetDataType(selectedVFile.getVFieType()); // for remote storage
        editPool.setPastePath(selectedVFile.getAbsolutePath());
        EditorAsyncHelper.setPasteFileTerminate();
        EditorAsyncHelper.pasteFile(editPool, null, getHandler());
        PasteDialogFragment.newInstance(editPool).show(getFragmentManager(), PasteDialogFragment.TAG);
        ItemOperationUtility.initNotificationBar(getActivity());
    }

    private class EditModeCallback implements ActionMode.Callback {

        private TextView mSelectedCountText;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.i(TAG, "HiddenZoneFragment EditModeCallback onCreateActionMode");
            // FIXME:
            // workaround for black status bar, force reset its color
            ColorfulLinearLayout.changeStatusbarColor(mActivity, R.color.theme_color);

            //add select count view
            LayoutInflater inflaterView = getActivity().getLayoutInflater();
            View view = inflaterView.inflate(R.layout.editmode_actionbar, null);
            mode.setCustomView(view);
            mSelectedCountText = (TextView) view.findViewById(R.id.actionbar_text);

            //add items
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.hiddenzone_edit, menu);

            mEditMode = mode;

            updateSelectHint();
            updateMenuItemBySelectedCount(menu);

            return true;
        }

        private void updateSelectHint() {
            int longClickCount = hiddenZoneAdapter.getSelectedItemMap().size();
            String itemSelected = getResources().getQuantityString(R.plurals.number_selected_items, longClickCount, longClickCount);
            mSelectedCountText.setText(itemSelected);
        }

        private void updateSelectAction() {
            Log.i(TAG, "HiddenZoneFragment EditModeCallback updateSelectAction");
            int longClickCount = hiddenZoneAdapter.getSelectedItemMap().size();
            if (longClickCount == hiddenZoneAdapter.getCount()) {
                hiddenZoneAdapter.deselectAllItems();
            } else {
                hiddenZoneAdapter.selectAllItems();
            }
        }

        private boolean needTriggerOnBackPressed()
        {
            int longClickCount = hiddenZoneAdapter.getSelectedItemMap().size();
            if (longClickCount == 0)
            {
                return false;
            }
            return true;
        }

        private void updateMenuItemBySelectedCount(Menu menu) {
            Log.i(TAG, "HiddenZoneFragment EditModeCallback updateMenuItemBySelectedCount");
            int longClickCount = hiddenZoneAdapter.getSelectedItemMap().size();
            MenuItem selectItem = menu.findItem(R.id.select_all_action);
            MenuItem deselectItem = menu.findItem(R.id.deselect_all_action);
            MenuItem renameItem = menu.findItem(R.id.rename_action);
            MenuItem infoItem = menu.findItem(R.id.info_action);

            selectItem.setVisible(longClickCount != hiddenZoneAdapter.getCount());
            deselectItem.setVisible(longClickCount == hiddenZoneAdapter.getCount());
            renameItem.setVisible(longClickCount == 1);
            infoItem.setVisible(longClickCount == 1);
        }
        //mEditMode.invalidate() will call this function
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.i(TAG, "HiddenZoneFragment onPrepareActionMode");
            updateMenuItemBySelectedCount(menu);
            updateSelectHint();
            ThemeUtility.setThemeOverflowButton(mActivity, false);
            ThemeUtility.setMenuIconColor(getActivity().getApplicationContext(), menu);
            return true;
        }

        // @Override
        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
            Log.i(TAG, "HiddenZoneFragment onActionItemClicked");
            switch (item.getItemId()) {
                case R.id.select_all_action:
                case R.id.deselect_all_action:
                    updateSelectAction();
                    break;
                case R.id.delete_action:
                    deleteFileInPopup(collectSelectedVFile(hiddenZoneAdapter.getSelectedItemMap().values().iterator(), hiddenZoneAdapter.getSelectedItemMap().size()));
                    break;
                case R.id.info_action:
                    showHiddenZoneInfo(hiddenZoneAdapter.getSelectedItemMap().values().iterator().next());
                    break;
                case R.id.copy_to_action:
                    selectFolder(R.id.copy_to_action);
                    break;
                case R.id.move_to_action:
                    selectFolder(R.id.move_to_action);
                    break;
                case R.id.rename_action:
                    rename(hiddenZoneAdapter.getSelectedItemMap().values().iterator().next());
                    break;
            }
            return true;
        }

        // @Override
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.i(TAG, "HiddenZoneFragment onDestroyActionMode");
            //mDeleteFilePool.clear();
            if(needTriggerOnBackPressed())
                onBackPressed();
        }
    }

    private void rename(HiddenZoneDisplayItem item) {
        if (!mActivity.isNeedPermission(new VFile[] {item.getCurrentVFile() }, SafOperationUtility.ACTION_RENAME)) {
            RenameDialogFragment renameDialog =
                    RenameDialogFragment.newInstance(item.getCurrentVFile(), RenameDialogFragment.TYPE_RENAME_DIALOG);
            renameDialog.show(getFragmentManager(), "RenameDialogFragment");
        }
    }

    private void selectFolder(int actionType) {
        EditPool filePool = new EditPool();
        filePool.setFiles(
                collectSelectedVFile(
                        hiddenZoneAdapter.getSelectedItemMap().values().iterator()
                        , hiddenZoneAdapter.getSelectedItemMap().size())
                , true);

        Bundle data = new Bundle();
        data.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.move_to));
        data.putInt(MoveToDialogFragment.DIALOG_MODE, MoveToDialogFragment.MODE_HIDDEN_ZONE);
        data.putInt(MoveToDialogFragment.DIALOG_TYPE, actionType);
        data.putSerializable("file", filePool);
        MoveToDialogFragment.newInstance(data).show(getFragmentManager(), MoveToDialogFragment.DIALOG_TAG);
    }

    private VFile[] collectSelectedVFile(Iterator<HiddenZoneDisplayItem> iterator, int deleteFileCount) {
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
        Log.i(TAG, "HiddenZoneFragment restoreFiles");
        if (!mActivity.isNeedPermission(restoreFiles, SafOperationUtility.ACTION_RESTORE)) {
            RestoreDialogFragment restoreDialogFragment = RestoreDialogFragment.newInstance();
            if (!restoreDialogFragment.isAdded()) {
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
        Log.i(TAG, "HiddenZoneFragment deleteFileInPopup");

        EditPool deleteFilePool = new EditPool();
        deleteFilePool.setFiles(deleteFiles, true);

        VFile[] filesToDelete = deleteFilePool.getFiles();
        if (!mActivity.isNeedPermission(filesToDelete, SafOperationUtility.ACTION_DELETE))
            mActivity.displayDialog(FileManagerActivity.DialogType.TYPE_DELETE_DIALOG, deleteFilePool);
    }
}
