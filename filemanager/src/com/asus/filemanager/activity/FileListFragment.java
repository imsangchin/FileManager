
package com.asus.filemanager.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.SearchRecentSuggestions;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.commonui.syncprogress.SyncProgressTracker;
import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerActivity.FontType;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.adapter.BasicFileListAdapter;
import com.asus.filemanager.adapter.BasicFileListAdapter.CheckResult;
import com.asus.filemanager.adapter.DeviceListAdapter;
import com.asus.filemanager.adapter.ExpandableFileListAdapter;
import com.asus.filemanager.adapter.FileListAdapter;
import com.asus.filemanager.adapter.MoveToNaviAdapter;
import com.asus.filemanager.adapter.listpopupAdapter;
import com.asus.filemanager.apprecommend.GameLaunchFile;
import com.asus.filemanager.dialog.AddToHiddenZoneDialogFragment;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.ZipDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorAsyncHelper.WorkerArgs;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.editor.EditorUtility.ExistPair;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.ga.GaCloudStorage;
import com.asus.filemanager.ga.GaHiddenCabinet;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.ga.GaMoveToDialog;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.ga.GaUserPreference;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.loader.ScanFileLoader;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.GameAppDbHelper;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.provider.SearchHistoryProvider;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaItem;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.samba.ScanLanNetworkPC;
import com.asus.filemanager.samba.provider.PcInfoDbHelper;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.ui.ContextualActionBar;
import com.asus.filemanager.ui.SlideListView;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.CreateShortcutUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FileUtility.FileInfo;
import com.asus.filemanager.utility.FixedListFragment;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.RecentFileUtil;
import com.asus.filemanager.utility.SortUtility;
import com.asus.filemanager.utility.SortUtility.SortType;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.UpdateDeleteFilesTask;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.ViewUtility;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;
import com.asus.remote.dialog.CloudStorageRemoveHintDialogFragment;
import com.asus.remote.dialog.RemoteFilePasteDialogFramgment;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteActionEntry;
import com.asus.remote.utility.RemoteDataEntry;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteFileUtility.RemoteUIAction;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.ListArgument;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.updatesdk.ZenUiFamily;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class FileListFragment extends FixedListFragment implements OnClickListener
                                        ,LoaderManager.LoaderCallbacks<VFile[]>
                                        ,OnDragListener ,OnTouchListener
                                        , MoveToDialogFragment.OnFolderSelectListener, AdapterView.OnItemClickListener
                                        , Observer, SafOperationUtility.SafActionHandler, ContextualActionBar.ContextualActionButtonListener {

    private static final String TAG = "FileListFragment";
    private static final boolean DEBUG = ConstantsUtil.DEBUG;

    private static final int SCAN_FILE_LOADER = 100;
    //private static final int RESCAN_FILE_LOADER = 101;
    private static final int RESCAN_FILE_LOADER = SCAN_FILE_LOADER;

    // file list state
    public static final int NO_FILE = 0;
    public static final int NETWORK_INVALID = 1;
    public static final int NO_DEVICES = 2;
    public static final int TOKEN_INVALIDATED = 3;
    public static final int PERMISION_DENY = 4;
    public static final int ACCOUNT_INVALID = 5;
    public static final int HOMECLOUD_ACCESS_EEROR = 6;

    private static final String KEY_SCAN_PATH = "scan_path";
    private static final String KEY_SCAN_TYPE = "scan_type";
    private static final String KEY_SORT_TYPE = "sort_type";
    private static final String KEY_VFILE_TYPE = "vfile_type"; // for remote storage
    private static final String KEY_HIDDEN_TYPE = "hidden_type";
    private static final String KEY_CHECK_POOL = "check_pool";

    public static final int SORT_TYPE = 0;
    private static final int SORT_NAME = 1;
    private static final int SORT_DATE = 2;
    private static final int SORT_SIZE = 3;
  /*  private static final int SORT_DATE_ASCENDING = 4;
    private static final int SORT_DATE_DESCENDING = 5;*/

    public static final int SORT_NAME_ASCENDING = 2;
    public static final int SORT_NAME_DESCENDING = 3;
    public static final int SORT_DATE_ASCENDING = 4;
    public static final int SORT_DATE_DESCENDING = 5;
    public static final int SORT_SIZE_ASCENDING = 6;
    public static final int SORT_SIZE_DESCENDING = 7;

    private static final int MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED = 1;

    public static Map<String, List<String>> SAMBA_MAP_FOR_RESTRICFILES;

    private static final int[] SORT_TYPE_STRING_RES = new int[]{
        R.string.type,
        R.string.name_ascending,
        R.string.name_descending,
        R.string.date_ascending,
        R.string.date_descending,
        R.string.size_ascending,
        R.string.size_descending
    };

    public static final VFile DEFAULT_INDICATOR_HOME = new LocalVFile("/", VFileType.TYPE_CATEGORY_STORAGE);
    public static final String DEFAULT_INDICATOR_FILE =
            WrapEnvironment.getEpadInternalStorageDirectory().getAbsolutePath();
    private static final String PATH_REMOVABLE =
            WrapEnvironment.getEpadExternalStorageDirectory().getAbsolutePath();

    private static String DEFAULT_INDICATOR_FILE_CANONICAL_PATH;

    public static boolean sShowHidden = false;
    public static boolean sIsDeleteComplete = true;

    public static String sDrmPaths = null;

    private ContentResolver mCr;
    private int mScreenWidth;
    PopupWindow mPopupWindow;

    @Override
    public void update(Observable observable, Object data) {
        if (null != data){
            Bundle aBundle = (Bundle)data;
            String event;
            String path;
            event = aBundle.getString(VolumeStateObserver.KEY_EVENT);
            path = aBundle.getString(VolumeStateObserver.KEY_PATH);
            if (path == null) {
                Log.d(TAG, "onReceive path is null");
                return;
            }
            if (event.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Log.d(TAG, "onReceive unmount: " + path);
                if (mIndicatorFile != null && (mIndicatorFile.getAbsolutePath().startsWith(PATH_REMOVABLE) || mIndicatorFile.getAbsolutePath().startsWith(path))) {
                    dismissDialogFragmentByTag(getFragmentManager(), "UnZipDialogFragment");
                    dismissDialogFragmentByTag(getFragmentManager(), "UnRarDialogFragment");
                    startScanFile(DEFAULT_INDICATOR_HOME, ScanType.SCAN_CHILD);
                } else if (isLocalRootFolder(mIndicatorFile)) {//mIndicatorFile.getPath().equals("/")
                    startScanFile(mIndicatorFile,ScanType.SCAN_CHILD);
                }

                if (path!=null && path.contains("USBdisk1")) {
                    ItemOperationUtility.UsbHasDRM = false;
                } else {
                    ItemOperationUtility.MicroHasDRM = false;
                }

            } else {
                if (mIndicatorFile != null && (mIndicatorFile.getAbsolutePath().startsWith(PATH_REMOVABLE) || mIndicatorFile.getAbsolutePath().startsWith(path)) || isLocalRootFolder(mIndicatorFile)) {
//                if (mIndicatorFile != null) {
                    startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
                }
                if (path!=null && path.contains("USB")) {
                    ItemOperationUtility.getInstance().ScanInterDiskDrm("/Removable/USBdisk1");
                } else {
                    ItemOperationUtility.getInstance().ScanInterDiskDrm("/Removable/MicroSD");
                }
            }
        }
    }

    @Override
    public void onContextualActionButtonClick(View v) {
        switch (v.getId()) {
        case R.id.cab_move_to:
            handleMoveToAction();
            break;
        case R.id.cab_copy_to:
            // handleCopyToAction();
            prepareCopyToAction();
            break;
        case R.id.cab_share:
            handleShareAction();
            break;
        case R.id.cab_delete:
            handleDeleteAction();
            break;
        case R.id.cab_number_of_select:
            handleShoppingCartClicked();
            break;
        }
    }

    private void handleMoveToAction() {
        CheckResult result = getCurrentUsingAdapter().getSelectedCount();
        int selectcount = result.count;
        ItemOperationUtility.isReadyToPaste = true;
        if (ItemOperationUtility.isItemContainDrm(getCurrentUsingAdapter().getFiles(), false, false)) {
            if (selectcount == 1 && !result.hasDir) {
                ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            } else {
                ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            }
        } else {
            mEditPool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), true);
            mEditPool.setExtraBoolean(true);
            PasteDialogFragment.showCopyFileReady(getActivity(), mEditPool.getFiles(), true);
            onDeselectAll();
            getActivity().invalidateOptionsMenu();
        }

        //play safe
        if (mEditPool.getFile() == null) {
            return;
        }
        //play safe

        int mode = isCategoryMediaTopDir() ?
                mEditPool.getSize() > 1 ? MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP : MoveToDialogFragment.MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP
                : MoveToDialogFragment.MODE_MOVE_TO;

        VFile source = null;
        if (mEditPool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            // change to sdcard path when cut file in category.
            source = new LocalVFile(FileUtility.changeToSdcardPath(mEditPool.getFile().getPath()));
        } else {
            source = mEditPool.getFile();
        }

        Bundle date = new Bundle();
        date.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.move_to));
        date.putInt(MoveToDialogFragment.DIALOG_MODE, mode);
        date.putParcelable(MoveToDialogFragment.CURRENT_FOLDER, source.getParentFile());
        showDialog(DialogType.TYPE_MOVE_TO_DIALOG, date);
        //mActivity.initCloudService();
    }

    private void enterToSecondLayerActionMode() {
        if (mActionModeWrapper != null) {
            mActionModeWrapper.enterToSecondLayer();
            if (mCab != null) {
                mCab.setVisibility(View.GONE);
            }
        }
    }

    private void prepareCopyToAction() {
        enterToSecondLayerActionMode();
        getCurrentUsingAdapter().notifyDataSetChanged();
        mActivity.initCloudService();
    }

    private void handleCopyToAction() {
        Log.e(TAG, "handleCopyToAction");

        CheckResult result = getCurrentUsingAdapter().getSelectedCount();
        int selectcount = result.count;
        ItemOperationUtility.isReadyToPaste = true;
        if (ItemOperationUtility.isItemContainDrm(getCurrentUsingAdapter().getFiles(), false, false)) {
            if (selectcount == 1 && !result.hasDir) {
                ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            } else {
                ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            }
        } else {
            mEditPool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), /*true*/ false);
            mEditPool.setExtraBoolean(false);
            PasteDialogFragment.showCopyFileReady(getActivity(), mEditPool.getFiles(), false);
            // onDeselectAll();
            getActivity().invalidateOptionsMenu();
        }
        Bundle bd = new Bundle();

        int mode = isCategoryMediaTopDir() ?
                mEditPool.getSize() > 1 ? MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP : MoveToDialogFragment.MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP
                : MoveToDialogFragment.MODE_MOVE_TO;

        //play safe
        if (mEditPool.getFile() == null) {
            return;
        }
        //play safe

        VFile source = null;
        if (mEditPool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            // change to sdcard path when copy file in category.
            source = new LocalVFile(FileUtility.changeToSdcardPath(mEditPool.getFile().getPath()));
        } else {
            source = mEditPool.getFile();
        }

        bd.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.copy_to));
        bd.putInt(MoveToDialogFragment.DIALOG_MODE, mode);
        bd.putParcelable(MoveToDialogFragment.CURRENT_FOLDER, source.getParentFile());
        // showDialog(DialogType.TYPE_MOVE_TO_DIALOG, bd);

        onFolderSelected(mIndicatorFile, null);
    }

    private void handleShareAction() {
        CheckResult result = getCurrentUsingAdapter().getSelectedCount();
        int selectcount = result.count;
        if (ItemOperationUtility.isItemContainDrm(getCurrentUsingAdapter().getFiles(), false, false)) {
            if (selectcount == 1 && !result.hasDir) {
                ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            } else {
                ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
            }
        } else {
            if (getCurrentUsingAdapter().getCount() > 0 && ((VFile) getCurrentUsingAdapter().getItem(0)).getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                mShareFilePool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), true);
                VFile[] srcVFile = mShareFilePool.getFiles();
                String account = ((RemoteVFile) srcVFile[0]).getStorageName();
                VFile dstVFile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                int type = ((RemoteVFile) srcVFile[0]).getMsgObjType();

                RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(account, srcVFile, dstVFile, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, RemoteFileUtility.REMOTE_SHARE_ACTION, false);
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
            } else if (getCurrentUsingAdapter().getCount() > 0 && ((VFile) getCurrentUsingAdapter().getItem(0)).getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                mShareFilePool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), true);
                VFile[] srcVFile = mShareFilePool.getFiles();
                VFile dstVFile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_SHARE, srcVFile, dstVFile.getAbsolutePath(), false, -1, null);
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
            } else {
                mShareFilePool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), true);
                FileUtility.shareFile(getActivity(), mShareFilePool.getFiles(), false);
                mShareFilePool.clear();
                onDeselectAll();
            }
        }
    }

    private void handleDeleteAction() {
        VFile[] filesToDelete = mShoppingCart.getFiles().toArray(new VFile[mShoppingCart.getFiles().size()]); // getCurrentUsingAdapter().getFiles();
        mDeleteFilePool.setFiles(filesToDelete, true);
        filesToDelete = mDeleteFilePool.getFiles();
        if (!mActivity.isNeedPermission(filesToDelete, SafOperationUtility.ACTION_DELETE))
            showDialog(DialogType.TYPE_DELETE_DIALOG, mDeleteFilePool);
    }

    private void handleShoppingCartClicked() {
        Intent intent = new Intent(mActivity, ShoppingCartDetailActivity.class);
        ArrayList<VFile> list = mShoppingCart.getFiles();
        for (VFile vFile : list) {
            Log.e("^DUMP^", vFile.getPath());
        }
        intent.putParcelableArrayListExtra("a", list);
        startActivityForResult(intent, 5566);
    }

    public class PathIndicatorClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mActivity.isPadMode()) {
                ((FileManagerActivity)getActivity()).searchViewIconified(true);
            } else {
                collapseSearchView();
            }

            // +++ Johnson, don't do anything when user is moving divider
            if (isMovingDivider()) {
                return;
            }
            // ---
            if (v.getTag() instanceof RemoteVFile) {
                RemoteVFile tempVfile = (RemoteVFile)(v.getTag());
                if (tempVfile.getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    if ((tempVfile.getName()==null||tempVfile.getName().equals(""))&&tempVfile.getAbsolutePath().equals(File.separator+tempVfile.getStorageName())) {
                         RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(tempVfile.getStorageName(), null, null, tempVfile.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                         SetScanHostIndicatorPath(tempVfile.getStorageName());
                         return;
                    }
                }
            }
            if ((v.getTag() instanceof VFile) && ((VFile) v.getTag()).getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
                startScanCategory((VFile) v.getTag());
            }
            if ((v.getTag() instanceof VFile) && mIndicatorFile != null) {
            /**********add for GoBackToSelectItem**********/
            int level = getDumpLevel(((VFile) v.getTag()).getAbsolutePath(),mIndicatorFile.getAbsolutePath());
            ItemOperationUtility.getInstance().dumpRightPosition(level);
            /**********add for GoBackToSelectItem**********/
            startScanFile((VFile) v.getTag(), ScanType.SCAN_CHILD);
            }
        }

    }

    /*
    public BroadcastReceiver mReviver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Object storageVolume = (Object) intent.getParcelableExtra(reflectionApis.EXTRA_STORAGE_VOLUME);
            if (storageVolume!=null) {
                String path = reflectionApis.volume_getPath(storageVolume);
                if (path == null) {
                    Log.d(TAG, "onReceive path is null");
                    return;
                }
                if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    if (DEBUG)
                        Log.d(TAG, "onReceive unmount: " + path);
                    if (mIndicatorFile != null && (mIndicatorFile.getAbsolutePath().startsWith(PATH_REMOVABLE) || mIndicatorFile.getAbsolutePath().startsWith(path))) {
                        dismissDialogFragmentByTag(getFragmentManager(), "UnZipDialogFragment");
                        dismissDialogFragmentByTag(getFragmentManager(), "UnRarDialogFragment");
                        startScanFile(DEFAULT_INDICATOR_HOME, ScanType.SCAN_CHILD);
                    } else if (isLocalRootFolder(mIndicatorFile)) {//mIndicatorFile.getPath().equals("/")
                        startScanFile(mIndicatorFile,ScanType.SCAN_CHILD);
                    }

                    if (path!=null && path.contains("USBdisk1")) {
                         ItemOperationUtility.UsbHasDRM = false;
                    } else {
                        ItemOperationUtility.MicroHasDRM = false;
                    }

                } else {
                    if (mIndicatorFile != null && (mIndicatorFile.getAbsolutePath().startsWith(PATH_REMOVABLE) || mIndicatorFile.getAbsolutePath().startsWith(path)) || isLocalRootFolder(mIndicatorFile)) {
//                if (mIndicatorFile != null) {
                        startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
                    }
                    if (path!=null && path.contains("USB")) {
                        ItemOperationUtility.getInstance().ScanInterDiskDrm("/Removable/USBdisk1");
                    } else {
                        ItemOperationUtility.getInstance().ScanInterDiskDrm("/Removable/MicroSD");
                    }
                }
            }
        }
    };
    */

    public static void dismissDialogFragmentByTag(FragmentManager manager, String tag){
        Fragment prev = manager.findFragmentByTag(tag);
        if (prev != null && prev instanceof DialogFragment) {
            DialogFragment df = (DialogFragment) prev;
            df.dismissAllowingStateLoss();
        }
    }

    public interface OnShowDialogListener {
        public void displayDialog(int type, Object arg);
    }

    private FileListAdapter mAdapter;
    private ExpandableFileListAdapter mExpandableListAdapter;

    private View sizeContainer;
    private View nameContainer;
    private View typeContainer;
    private View dateContainer;
    private HorizontalScrollView mScrollView;
    private ImageView[] mSortImage = new ImageView[4];
    private RelativeLayout mPathContainerRoot;
    private LinearLayout mPathContainer;
    private View mPathIndicator;

    public boolean mIsAttachOp = false;
    public boolean mIsMultipleSelectionOp = false;

    private static final String KEY_FILE_FILTER = "file_filter";
    public static boolean sIsMIMEFilter = true;
    private String[] mFileFilter = null;
    private boolean mFirstInitByFilePicker = false;

    private VFile mIndicatorFile;

    private PathIndicatorClickListener mPathIndicatorClickListener;
    public OnShowDialogListener mShowDialogListener;
    private SharedPreferences mSharePrefence;

    private EditModeCallback mLastEditModeCallback;
    private ActionMode mEditMode;
    private DoubleLayerActionModeWrapper mActionModeWrapper;

    private EditPool mEditPool = new EditPool();
    private EditPool mCheckPool = new EditPool();
    private EditPool mDeleteFilePool = new EditPool();
    private EditPool mShareFilePool = new EditPool();

    private int mSortType = SortType.SORT_DATE_DOWN;
    public boolean mIsComFromSearch = false;
    private VFile mOldIndicatorFile;
    /****add to save useful file path****/
    private VFile mUsefulPathFile = null;

    // +++ Johnson
    private boolean mIsHiddenDate = false;
    private boolean mIsMovingDivider = false;
    private boolean mIsDraggingItems = false;
    private SlideListView mListView;
    private ExpandableListView mExpandableListView;
    private GridView mGridView;
    private FloatingActionButton mFab;
    private ContextualActionBar mCab;
    private ShoppingCart mShoppingCart;
    private View mSortRoot;
    private View mListBottom;
    private TextView mCloudStorageUsageView;
    private int mDragItemHeight;
    private int mDropTargetAdapterPosition = -1;
    private int mDropScreenAdapterPosition = -1;
    private static final int UPDATE_LIST_VIEW = 0;
    private static final int SCROLL_UP = 0;
    private static final int SCROLL_DOWN = 1;
    private static final int DRAG_SCROLL_STEP = 8;
    private static final int SCROLL_DELAY_TIME = 150;
    // ---

    // ++ Alex
    public FileManagerActivity mActivity = null;
    private ImageView mPathHome;
    private ImageView mViewSwitcher;
    private ImageView mViewBadge;

    private boolean mIsShowMenu;
    // --
    private MenuItem mSearchItem;

    // WIFI-Direct
    private boolean mIsScrollingList = false;
    public static final String DEFAULT_INTERNAL_STORAGE_PATH = WrapEnvironment.getEpadInternalStorageDirectory().getAbsolutePath();

    private boolean mIsBackEvent = false;

    //++ yiqiu_huang
    private boolean isLoadingSambaFolder= false;


    private String mAccountNameStorageType = "";

    /*********add for samba begin****************/
    private DeviceListAdapter mDeviceAdapter;
    /***********end******************************/

    //++Kelly2_Zhou
    private RemoteVFile remoteVFile;

    //for actionmode spinner
    private TextView mSelectedCountText;
    private boolean mbPopup = false;
    private PopupMenu popup;

    /**********add for pull to refresh common ui********/
    private boolean enbalePull = false;
    private boolean isRefreshing = false;
    private SyncProgressTracker mSyncProgressTracker;
    private SyncProgressTracker.SyncProgressTrackerListener mSyncProgressTrackerListener;


    private ListPopupWindow mEditModePopupWindow;
    boolean mHasEditModeContentWidth = false;
    int mEditModeContentWidth;
    ArrayList<String> mEditModeMenuStrs;
    ArrayList<Integer> mEditModeMenuID;
    ArrayList<Integer> mEditModeNewFeatureList;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "FileListFragment onAttach");
        try {
            mShowDialogListener = (OnShowDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }

        mActivity = (FileManagerActivity) activity;
        ShortCutFragment.currentTokenFile=null;
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }
    public VFile getmIndicatorFile() {
        return mIndicatorFile;
    }

    public void setmIndicatorFile(VFile indicatorFile) {
        this.mIndicatorFile = indicatorFile;
        if (mIndicatorFile != null && mIndicatorFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE) {
            ItemOperationUtility.getInstance().clearOperationStack();
        }
        PathIndicator.setPathIndicator(mPathContainer, indicatorFile, mPathIndicatorClickListener);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "FileListFragment onCreate");
        setRetainInstance(true);
        Log.d(TAG,"onCreate before mSortType:"+mSortType);
        mSortType = FileUtility.getCurrentSortType(mActivity);
        Log.d(TAG,"onCreate mSortType:"+mSortType);
        mCr = this.getActivity().getContentResolver();

        Intent intent = this.getActivity().getIntent();
        String path = intent.getStringExtra("path");
        if (path == null) {
            VFile file = FileUtility.getFileFromSharedPreferences(getActivity(),FileUtility.SCAN_FILE_INFO_NAME);
            if (file != null ) {
                mIndicatorFile = file;
            }
       }
       ItemOperationUtility.isReadyToPaste = false;

       if (HandleSearchIntentActivity.OPEN_FM_FOLDER_ACTION.equals(intent.getAction())) {
           handleIntent(intent);
       }

       mFirstInitByFilePicker = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mUsefulPathFile != null) {
            FileUtility.saveCurrentScanFileInfo(getActivity(), mUsefulPathFile,FileUtility.SCAN_FILE_INFO_NAME);
            mUsefulPathFile = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "FileListFragment onCreateView");
        return inflater.inflate(R.layout.filelist_fragment, container, false);
    }

     @Override
    public void onDestroyView() {
        closePopup();
        EditorUtility.sEditIsProcessing = false;
        Log.d(TAG, "onDestroyView");
        Log.d(TAG,"onDestroyView mSortType:"+mSortType);
        super.onDestroyView();
    }
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDestroyView");
        if (mUsefulPathFile != null) {
            FileUtility.saveCurrentScanFileInfo(getActivity(), mUsefulPathFile,FileUtility.SCAN_FILE_INFO_NAME);
            mUsefulPathFile = null;
        }
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "FileListFragment onActivityCreated");

        if (getActivity().getIntent().getAction() != null && getActivity().getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
            mIsAttachOp = true;
        }

        if (getActivity().getIntent().getAction() != null && getActivity().getIntent().getAction().equals(FileManagerActivity.ACTION_MULTIPLE_SELECTION)) {
            mIsMultipleSelectionOp = true;
        }

        mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
        sShowHidden = mSharePrefence.getBoolean("mShowHidden", false);

        ItemOperationUtility.getInstance().loadViewModeFromPreferences(mActivity, true);
        initView();
        boolean isAttachOpFile = false;
        if (mIsAttachOp && mFirstInitByFilePicker) {
            VFile vFile = FileUtility.getFileFromSharedPreferences(mActivity, FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME);
            if (vFile != null) {
                mIndicatorFile = vFile;
                isAttachOpFile = true;
            }
            mFirstInitByFilePicker = false;
        }

        if (mIndicatorFile == null && !isAttachOpFile) {
            String path = getActivity().getIntent().getStringExtra("path");
            if (path != null) {
                if (WrapEnvironment.SUPPORT_REMOVABLE) {
                    if (path.equalsIgnoreCase("/storage/MicroSD") || path.equalsIgnoreCase("/storage/sdcard1")) {
                        mIndicatorFile = new LocalVFile("/Removable/MicroSD");
                    } else if (path.equalsIgnoreCase("/storage/USBdisk1")) {
                        mIndicatorFile = new LocalVFile("/Removable/USBdisk1");
                    } else if (path.equalsIgnoreCase("/storage/USBdisk2")) {
                        mIndicatorFile = new LocalVFile("/Removable/USBdisk2");
                    } else {
                        mIndicatorFile = new LocalVFile(path);
                    }
                } else {
                    mIndicatorFile = new LocalVFile(path);
                }
            } else {
                mIndicatorFile = new LocalVFile(DEFAULT_INDICATOR_FILE);
            }
        }

        // init PathIndicator
        PathIndicator.setPathIndicator(mPathContainer, mIndicatorFile, mPathIndicatorClickListener);

        if (mFileFilter == null) {
            if (getActivity().getIntent().getBooleanExtra("mime_filter", true)) {
                mFileFilter = getActivity().getIntent().getStringArrayExtra("mime");
                sIsMIMEFilter = true;
            } else {
                mFileFilter = getActivity().getIntent().getStringArrayExtra("ext");
                sIsMIMEFilter = false;
            }
        }

        if (mAdapter == null) {
            mAdapter = new FileListAdapter(this, null, mIsAttachOp);
            setFileListAdapter();
            getListView().setItemsCanFocus(true);
        } else if (getListAdapter() != null) {
             if (getListAdapter() instanceof FileListAdapter) {
                 setOnItemClickAndLongClickListener();
//                 if (ItemOperationUtility.getInstance().isListViewMode()) {
//                    mListView.setOnItemClickListener(mAdapter);
//                    mListView.setOnItemLongClickListener(mAdapter);
//                } else {
//                    mGridView.setOnItemClickListener(mAdapter);
//                    mGridView.setOnItemLongClickListener(mAdapter);
//                }

             } else {
                 getListView().setOnItemClickListener(mDeviceAdapter);
                 getListView().setOnItemLongClickListener(mDeviceAdapter);
             }
        }

        setListAdapter(mAdapter, true);

        getCurrentUsingAdapter().setOrientation(getResources().getConfiguration().orientation);
        mListView.setSlideOutListener(mAdapter);


        setHasOptionsMenu(true);

        // reset the Edit mode
        finishEditMode();
        updateEditMode();
        /*****add for remember select item*****/
        ItemOperationUtility.getInstance().resetScrollPositionList();

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        ItemOperationUtility.getInstance().ScanAllStorageDiskForDrm();

        int pathIndicatorType = PathIndicator.getIndicatorVfileStorageType();

        boolean isIndicateToSambaOrHomeCloud
                = (pathIndicatorType == StorageType.TYPE_NETWORK_PLACE
                        || pathIndicatorType == StorageType.TYPE_HOME_CLOUD);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefsFile", 0);
        sShowHidden = sharedPreferences.getBoolean("mShowHidden", false);

        if (getListAdapter() != null) {
            SambaFileUtility.updateHostIp = (getListAdapter() instanceof DeviceListAdapter)
                        && (((DeviceListAdapter)getListAdapter()).getDeviceType() == DeviceListAdapter.DEVICE_SAMAB)
                                && isIndicateToSambaOrHomeCloud ? true : false;
            RemoteFileUtility.isShowDevicesList = (getListAdapter() instanceof DeviceListAdapter)
                        && (((DeviceListAdapter)getListAdapter()).getDeviceType() == DeviceListAdapter.DEVICE_HOMEBOX)
                                && isIndicateToSambaOrHomeCloud ? true : false;
        }

        ItemOperationUtility.resumeNotificationBar(mEditPool,getActivity());
       // initView();
        ((FileManagerActivity) getActivity()).clearCloudStorageAccountList();
          if ( !(mIsAttachOp || mIsMultipleSelectionOp)) {
            if (FileManagerActivity.isLocaleChanged) {
                //RemoteAccountUtility.initAvaliableCloudsInfo();
                FileManagerActivity.isLocaleChanged = false;
            } else {
                if (((FileManagerActivity)getActivity()).isFromOpenFileFail()) {
                    ((FileManagerActivity)getActivity()).setFromOpenFileFail(false);
                } else {
                    RemoteAccountUtility.getInstance(getActivity()).initAccountsChange(((FileManagerActivity) getActivity()).isFromFirst());
                }
            }
        }

        /*  Log.d(TAG,"mIsShowHomePageFragment = " + mActivity.mIsShowHomePageFragment);
          if (mActivity.mIsShowHomePageFragment) {
              return ;
          }
          if (mActivity.isSeachViewIsShow()) {
              Log.w(TAG, "searchview is show");
              return ;
          }
        if (mActivity.getCurrentFragmentType() == FragmentType.RECYCLE_BIN) {
            return ;
        }*/
        if (mActivity.getCurrentFragmentType() != FragmentType.FILE_LIST) {
            Log.w(TAG, "current fragment: " + mActivity.getCurrentFragmentType());
              return ;
          }

        Log.i(TAG,"onresume isSearching:"+FileManagerActivity.isSearchIng);
        if (!FileManagerActivity.isSearchIng) {

            // firstly handle remote file because they don't exist
            if (SambaFileUtility.updateHostIp) {
                setDeviceListAdapter();
                mDeviceAdapter.notifyDataSetChanged();
                SetScanHostIndicatorPath(SambaFileUtility.SCAN_ROOT_PATH);
            } else if (RemoteFileUtility.isShowDevicesList) {
                int cloudType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                if (RemoteAccountUtility.getInstance(getActivity()).clouds.get(cloudType)==null || RemoteAccountUtility.getInstance(getActivity()).clouds.get(cloudType).length<1) {
                    backToDefaultPath();
                }

                if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                     String accountType = RemoteAccountUtility.getInstance(getActivity()).clouds.get(cloudType)[0];
                     boolean isLoginAccountRemoved = RemoteAccountUtility.getInstance(getActivity()).isLoginAccountRemoved(getActivity(), accountType, ((RemoteVFile)mIndicatorFile).getStorageName());
                     if (isLoginAccountRemoved) {
                         startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
                         mActivity.setCurrentActionBarTitle(getResources().getString(R.string.internal_storage_title));
                     }
                }
                /*     String accountType = RemoteAccountUtility.clouds.get(cloudType)[0];
                     AccountManager accountManager = AccountManager.get(getActivity());
                     Account[] accounts = accountManager.getAccountsByType(accountType);
                     if (accounts == null || accounts.length <=0) {
                         startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
                         mActivity.setCurrentActionBarTitle(getResources().getString(R.string.internal_storage_title));
               } else {
                        boolean isAccountChanged = true;
                        for (int i = 0; i < accounts.length; i++) {
                            if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && accounts[i].name.equals(((RemoteVFile)mIndicatorFile).getStorageName())) {
                                isAccountChanged = false;
                                break;
                            }
                        }
                        if (isAccountChanged) {
                             startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
                             mActivity.setCurrentActionBarTitle(getResources().getString(R.string.internal_storage_title));
                        }
                    }*/
            } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE ) {
                startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);

            } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE||(ShortCutFragment.currentTokenFile!=null && ShortCutFragment.currentTokenFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE)) {
                // startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
                // RemoteVFile tempFile = (RemoteVFile)FmIndicatorFile;
                if (!(ShortCutFragment.currentTokenFile!=null && ShortCutFragment.currentTokenFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE)) {
                    if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                         RemoteVFile tempFile = (RemoteVFile)mIndicatorFile;
                     int cloudType = tempFile.getMsgObjType();
                     String[] cloudTypeArray = RemoteAccountUtility.getInstance(getActivity()).clouds.get(cloudType);
                     if (cloudTypeArray == null) {
                        backToDefaultPath();
                     } else {
                         String accountType =     cloudTypeArray[0];
                         String accountName = tempFile.getStorageName();
                         boolean isLoginAccountRemoved = RemoteAccountUtility.getInstance(getActivity()).isLoginAccountRemoved(getActivity(), accountType, accountName);
                         if (isLoginAccountRemoved) {
                             String key = accountName + "_" + cloudType;
                             RemoteAccountUtility.getInstance(getActivity()).accountsMap.remove(key);
                             backToDefaultPath();
                         }

                         /*AccountManager accountManager = AccountManager.get(getActivity());
                         Account[] accounts = accountManager.getAccountsByType(accountType);
                         String accountName = tempFile.getStorageName();
                         if (accounts == null || accounts.length <=0) {
                             backToDefaultPath() ;
                         } else {
                             boolean isHashCurrentAccount = false;
                             for (int i = 0; i < accounts.length; i++) {
                                 if (accounts[i].name.equals(accountName)) {
                                     isHashCurrentAccount = true;
                                     break;
                                 }
                             }
                             if (!isHashCurrentAccount) {
                                 String key = accountName + "_" + cloudType;
                                 RemoteAccountUtility.accountsMap.remove(key);
                                 backToDefaultPath();
                             }
                         }*/
                    }
                }
                }


           /* } else if ( mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                 //startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
                 RemoteVFile tempFile = (RemoteVFile)mIndicatorFile;
                 int cloudType = tempFile.getMsgObjType();
                 String accountType =     RemoteAccountUtility.clouds.get(cloudType)[0];
                 AccountManager accountManager = AccountManager.get(getActivity());
                 Account[] accounts = accountManager.getAccountsByType(accountType);
                 if (accounts == null || accounts.length <=0) {
                     backToDefaultPath() ;
                 }*/

            } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                if (!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
                    mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                }
//                startScanFile(new SambaVFile(mIndicatorFile.getAbsolutePath()), ScanType.SCAN_CHILD);
            } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
                startScanCategory(mIndicatorFile);
            } else if (!mIndicatorFile.exists()) {
                startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
            } else {
                startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
            }
            modifyListAndGridVisibility();
        }

            registerObserver(mIndicatorFile.getAbsolutePath());
//      updateEditMode();
    }
    public void validateIsTheLocaleChangeedOnResume() {
        if (mIndicatorFile!=null) {
        if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE||(ShortCutFragment.currentTokenFile!=null && ShortCutFragment.currentTokenFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE)) {
            // startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
            // RemoteVFile tempFile = (RemoteVFile)mIndicatorFile;
            if (!(ShortCutFragment.currentTokenFile!=null && ShortCutFragment.currentTokenFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE)) {
                     RemoteVFile tempFile = (RemoteVFile)mIndicatorFile;
                 int cloudType = tempFile.getMsgObjType();
                 String[] cloudTypeArray = RemoteAccountUtility.getInstance(getActivity()).clouds.get(cloudType);
                 if (cloudTypeArray == null || cloudTypeArray.length<1) {
                    backToDefaultPath();
                 } else {
                     String accountType =     cloudTypeArray[0];
                     String accountName = tempFile.getStorageName();
                     boolean isLoginAccountRemoved = RemoteAccountUtility.getInstance(getActivity()).isLoginAccountRemoved(getActivity(), accountType, accountName);
                     if (isLoginAccountRemoved) {
                         String key = accountName + "_" + cloudType;
                         RemoteAccountUtility.getInstance(getActivity()).accountsMap.remove(key);
                         backToDefaultPath();
                     }

                     /*AccountManager accountManager = AccountManager.get(getActivity());
                     Account[] accounts = accountManager.getAccountsByType(accountType);
                     String accountName = tempFile.getStorageName();
                     if (accounts == null || accounts.length <=0) {
                         backToDefaultPath() ;
                     } else {
                         boolean isHashCurrentAccount = false;
                         for (int i = 0; i < accounts.length; i++) {
                             if (accounts[i].name.equals(accountName)) {
                                 isHashCurrentAccount = true;
                                 break;
                             }
                         }
                         if (!isHashCurrentAccount) {
                             String key = accountName + "_" + cloudType;
                             RemoteAccountUtility.accountsMap.remove(key);
                             backToDefaultPath();
                         }
                     }*/
                }
            }
        }
    }
    }
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent()");
        String path = intent.getStringExtra("path");
        if (intent.getBooleanExtra("mime_filter", true)) {
            mFileFilter = intent.getStringArrayExtra("mime");
            sIsMIMEFilter = true;
        } else {
            mFileFilter = intent.getStringArrayExtra("ext");
            sIsMIMEFilter = false;
        }
        Log.d(TAG, "onNewIntent() + path = " + path);
        if (path != null) {
            String removePath;
            if (WrapEnvironment.SUPPORT_REMOVABLE) {
                if (path.equalsIgnoreCase("/storage/MicroSD") || path.equalsIgnoreCase("/storage/sdcard1")) {
                    removePath = "/Removable/MicroSD";
                } else if (path.equalsIgnoreCase("/storage/USBdisk1")) {
                    removePath = "/Removable/USBdisk1";
                } else if (path.equalsIgnoreCase("/storage/USBdisk2")) {
                    removePath = "/Removable/USBdisk2";
                } else {
                    removePath = path;
                }
            } else {
                removePath = path;
            }
            startScanFile(new LocalVFile(removePath), ScanType.SCAN_CHILD);
        }
    }

    // ++ Willie
    public void onUnZipIntent(VFile file) {
        if (!file.equals(getIndicatorFile().getPath()))
            startScanFile(file, ScanType.SCAN_CHILD);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        FileUtility.saveCurrentSortType(getActivity(), mSortType);
        unregisterObserver();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
        Log.d(TAG, "FileListFragment onDetach");
    }

    private void initView() {
        View container = getView();

        //setEmptyText(getText(R.string.empty_folder_title));
        setEmptyText(getResources().getString(R.string.fileList_Nofiles));

        mPathIndicator = container.findViewById(R.id.path_indicator);

        mPathContainerRoot = (RelativeLayout) container.findViewById(R.id.path_container_root);

        mPathContainer = (LinearLayout) container.findViewById(R.id.pathContainer);
        mPathIndicatorClickListener = new PathIndicatorClickListener();

        mScrollView = (HorizontalScrollView) getView().findViewById(R.id.scroll_container);

        mPathHome = (ImageView)container.findViewById(R.id.path_home);
        mPathHome.setOnClickListener(this);

        mViewSwitcher = (ImageView)container.findViewById(R.id.view_switcher);

        ItemOperationUtility.getInstance().loadViewModeFromPreferences(mActivity, true);
        updateViewModeIcon(ItemOperationUtility.getInstance().isListViewMode());
        mViewSwitcher.setOnClickListener(this);

        sizeContainer = container.findViewById(R.id.sort_size_container);
        sizeContainer.setOnClickListener(this);
        mSortImage[SORT_SIZE] = (ImageView) sizeContainer.findViewById(R.id.sizeImage);
        mSortImage[SORT_SIZE].setVisibility(View.GONE);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            sizeContainer.setVisibility(View.GONE);
        } else {
            sizeContainer.setVisibility(View.VISIBLE);
        }

        nameContainer = container.findViewById(R.id.sort_name_container);
        nameContainer.setOnClickListener(this);
        mSortImage[SORT_NAME] = (ImageView) nameContainer.findViewById(R.id.nameImage);
        mSortImage[SORT_NAME].setVisibility(View.GONE);

        typeContainer = container.findViewById(R.id.sort_type_container);
        typeContainer.setOnClickListener(this);
        mSortImage[SORT_TYPE] = (ImageView) typeContainer.findViewById(R.id.typeImage);
        mSortImage[SORT_TYPE].setVisibility(View.GONE);

        dateContainer = container.findViewById(R.id.sort_date_container);
        dateContainer.setOnClickListener(this);
        isHiddenDate(false); // Johnson, make sure the date will be initialized when creating view ...
        mSortImage[SORT_DATE] = (ImageView) dateContainer.findViewById(R.id.dateImage);
        mSortImage[SORT_DATE].setVisibility(View.GONE);

        mSortImage[mSortType / 2].setVisibility(View.VISIBLE);
        mSortImage[mSortType / 2].getDrawable().setLevel(mSortType % 2);

        // +++ Johnson
        mCloudStorageUsageView = (TextView) container.findViewById(R.id.cloudstorageUsage);


        mListView = (SlideListView) container.findViewById(android.R.id.list);
        mExpandableListView = (ExpandableListView) container.findViewById(R.id.expand_list);
        mExpandableListView.setEmptyView(container.findViewById(R.id.empty_for_expandable));
        if (mExpandableListAdapter == null) {
            mExpandableListAdapter = new ExpandableFileListAdapter(mActivity, FileListFragment.this, null);
        }
        mExpandableListView.setAdapter(mExpandableListAdapter);
        mExpandableListView.setOnGroupClickListener(mExpandableListAdapter);
        mExpandableListView.setOnChildClickListener(mExpandableListAdapter);
        mExpandableListView.setOnItemLongClickListener(mExpandableListAdapter);
        mGridView = (GridView) container.findViewById(R.id.content_gird);
        mFab = (FloatingActionButton) container.findViewById(R.id.fab);
        mCab = (ContextualActionBar) container.findViewById(R.id.cab_bar);
        resizeGridViewSpacing(mActivity, mGridView, ViewUtility.dp2px(mActivity,102), ConstantsUtil.GRID_MODE_NORMAL);
        WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;

        ViewGroup mListParent = ((ViewGroup)container.findViewById(R.id.listContainer));
        initSyncProgressTracker(mListParent);
        mListView.setFileListFrament(this);


        mSortRoot = container.findViewById(R.id.sort_container_root);
        mListBottom = container.findViewById(R.id.list_bottom);
        mListBottom.setOnDragListener(this);

//        mListView.setPullLoadEnable(false);
//        mListView.setPullRefreshEnable(false);
//        mListView.setPullFreshListViewListener(this);
        mListView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    BasicFileListAdapter adapter = getCurrentUsingAdapter();
                    if (adapter != null) {
                        adapter.updateAdapter(adapter.getFiles(), false, mSortType, null);
                    }
                    mIsScrollingList = false;
                } else {
                    mIsScrollingList = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                if (!ItemOperationUtility.getInstance().isListViewMode() && !SambaFileUtility.updateHostIp && !RemoteFileUtility.isShowDevicesList && !isForceListMode()) {
                    if (mListView.isShown()) {
                        mListView.setVisibility(View.GONE);
                    }
                }
            }
        });

        mGridView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    BasicFileListAdapter adapter = getCurrentUsingAdapter();
                    if (adapter != null) {
                        adapter.updateAdapter(adapter.getFiles(), false, mSortType, null);
                    }
                    mIsScrollingList = false;
                } else {
                    mIsScrollingList = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                if (ItemOperationUtility.getInstance().isListViewMode() || isForceListMode()) {
                    if (mGridView.isShown()) {
                        mGridView.setVisibility(View.GONE);
                    }
                }
            }
        });
        if (mFab != null) {
            mFab.setOnClickListener(this);
        }
        // ---

        // +++ Alex
        mIsShowMenu = true;
        if (mActivity.isPadMode()) {
            mPathIndicator.setBackgroundColor(getResources().getColor(R.color.white));
            mListView.setOnDragListener(this);
            mGridView.setOnDragListener(this);
            mSortRoot.setOnDragListener(this);
            mListBottom.setOnDragListener(this);
        } else {
            //mPathIndicator.setBackgroundColor(getResources().getColor(R.color.path_background));
            mSortRoot.setVisibility(View.GONE);
        }
    }

    // ActionBar menu +++

    //+++ Alex
    public void setShowMenu(boolean isShow) {
        mIsShowMenu = isShow;
        mActivity.invalidateOptionsMenu();
    }
    //---

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mIsShowMenu) {
            menu.clear();
            return;
        }
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.normal_mode, menu);
        // In multiple selection mode, these buttons with functions
        // will be disabled.
        if (mIsAttachOp || mIsMultipleSelectionOp) {
            menu.findItem(R.id.add_folder_action).setVisible(false);
            menu.findItem(R.id.clear_history_action).setVisible(false);
            menu.findItem(R.id.about_action).setVisible(false);

            if (mIsAttachOp) {
                menu.findItem(R.id.select_all_action).setVisible(false);
            }
            if (!mActivity.isPadMode()) {
                menu.findItem(R.id.search_action).setVisible(false);
            }
            menu.findItem(R.id.cta_dialog).setVisible(false);

        }

        if (!ItemOperationUtility.getInstance().enableCtaCheck()) {
            menu.findItem(R.id.cta_dialog).setVisible(false);
        }

        boolean needSaf = ((FileManagerApplication)mActivity.getApplication()).isNeedSafPermission();
        if (!needSaf){
            menu.findItem(R.id.saf_tutorial_action).setVisible(false);
        }

        menu.findItem(R.id.select_all_action).setVisible(false);
        //menu.findItem(R.id.about_action).setVisible(false);

        // cloud storage case
        boolean hasAuthenticator = (!WrapEnvironment.isAZSEnable(getActivity()) && mIndicatorFile!=null && mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE);
        menu.findItem(R.id.logout_account).setVisible(hasAuthenticator);

        MenuItem resetSambaAccounts = menu.findItem(R.id.reset_samba_account);
        if (resetSambaAccounts != null) {
            if (SambaFileUtility.updateHostIp) {
                resetSambaAccounts.setVisible(true);
            } else {
                resetSambaAccounts.setVisible(false);
            }
        }

        // when in root folder, we will try to hide the add-folder button
        if (isRootOrMountDir(mIndicatorFile.getPath()) ||
                (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) || mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getInstance(getActivity()).getRootScanPath())) {
            menu.findItem(R.id.add_folder_action).setVisible(false);
        }

//        MenuItem pasteItem = menu.findItem(R.id.paste_action);
        if ((mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) || SambaFileUtility.updateHostIp || RemoteFileUtility.isShowDevicesList) {
            menu.findItem(R.id.cloud_refresh_action).setVisible(true);
        } else {
            menu.findItem(R.id.cloud_refresh_action).setVisible(false);
        }
        //MenuItem addAccountItem = menu.findItem(R.id.cloud_add_google_action);
     /*   if (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE&&((RemoteVFile)mIndicatorFile).getMsgObjType()==MsgObj.TYPE_GOOGLE_DRIVE_STORAGE) {
            menu.findItem(R.id.cloud_add_google_action).setVisible(true);
        } else {
            menu.findItem(R.id.cloud_add_google_action).setVisible(false);
        }*/
//        if (mEditPool == null || mEditPool.getSize() == 0) {
//            pasteItem.setVisible(false);
//        } else {
//            // only support paste action for remote storage
//            if (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
//                String connectedDeviceName = RemoteFileUtility.getConnectedWifiDirectStorageName();
//                if (!connectedDeviceName.equals("")) {
//                    // remote storage root cannot do paste action or source is the same as remote storage type
//                    if (mIndicatorFile.getAbsolutePath().equals("/" + connectedDeviceName + "/sdcard") ||
//                        mIndicatorFile.getAbsolutePath().equals("/" + connectedDeviceName + DEFAULT_INTERNAL_STORAGE_PATH)
//                        /*|| mEditPool.getFile().getVFieType() == VFileType.TYPE_REMOTE_STORAGE*/) {
//                        pasteItem.setVisible(false);
//                    } else {
//                        pasteItem.setVisible(true);
//                    }
//                } else {
//                    Log.d(TAG, "onCreateOptionsMenu to get remote deviceName is empty");
//                }
//            } else {
//                // Local storage case
//                pasteItem.setVisible(true);
//            }
//        }

        MenuItem searchItem = menu.findItem(R.id.search_action);
        // FIXME:
        // workaround for asus support library set icon fail in xml
        //searchItem.setIcon(getResources().getDrawable(R.drawable.asus_ep_ic_search));
        mSearchItem = searchItem;
        if (searchItem != null) {
            if (mIsAttachOp || mIsMultipleSelectionOp) {
                searchItem.setVisible(false);
            } else {
                mActivity.setupSearchViewExternal((SearchView) MenuItemCompat
                        .getActionView(searchItem));
                MenuItemCompat.setOnActionExpandListener(searchItem,
                    new MenuItemCompat.OnActionExpandListener() {
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            if (!FileListFragment.this.isListShown()) {
                                return false;
                            } else {
                                GaSearchFile
                                        .getInstance()
                                        .sendEvents(
                                                mActivity,
                                                GaSearchFile.CATEGORY_NAME,
                                                GaSearchFile.ACTION_CLICK_SEARCH_ICON,
                                                null, null);
                                return true;
                            }
                        }

                        public boolean onMenuItemActionCollapse(
                                MenuItem item) {
                            // TODO Auto-generated method stub
                            return true;
                        }
                    }
                );
            }
            SearchView mSearchView = (SearchView)MenuItemCompat.getActionView(searchItem);
            if (null != mSearchView) {
                mSearchView.setMaxWidth(Integer.MAX_VALUE);
            }

            if(WrapEnvironment.SUPPORT_FEATURE_ASUS_PEN){
                if (null != mSearchView){
                    final AppCompatImageView searchIcon = (AppCompatImageView)mSearchView.findViewById(android.support.v7.appcompat.R.id.search_button);
                    if (null != searchIcon) {
                        searchIcon.setOnHoverListener(new View.OnHoverListener() {
                            @Override
                            public boolean onHover(View v, MotionEvent event) {
                                int what = event.getAction();
                                switch (what) {
                                    case MotionEvent.ACTION_HOVER_ENTER:
                                        String title = getContext().getResources().getString(R.string.action_search);
                                        mPopupWindow = ViewUtility.showTooltip(title,searchIcon,getContext(),mPopupWindow);
                                        break;
                                    case MotionEvent.ACTION_HOVER_MOVE:
                                        break;
                                    case MotionEvent.ACTION_HOVER_EXIT:
                                        if (null != mPopupWindow && mPopupWindow.isShowing()){
                                            mPopupWindow.dismiss();
                                        }
                                        break;
                                }
                                return false;
                            }
                        });
                    }
                }
            }
        }

        // only support local file can search
        if (!(mIsAttachOp || mIsMultipleSelectionOp) && mIndicatorFile != null) {
        /*    if (mIndicatorFile.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                getActivity().getActionBar().setDisplayShowCustomEnabled(true);
            } else {
                getActivity().getActionBar().setDisplayShowCustomEnabled(true);
            }*/
        }

    }

    public void collapseSearchView() {
       if ((mSearchItem != null)) {
           mSearchItem.collapseActionView();
           mActivity.hideSoftKeyboard();
       }
    }

    private void addGoogleAccountFromMenu() {
        if (!ItemOperationUtility.getInstance().checkCtaPermission(mActivity)) {
            ToastUtility.show(mActivity, mActivity.getResources().getString(R.string.network_cta_hint));
            return;
        }
        if (!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
            mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
        } else {
            RemoteAccountUtility.getInstance(getActivity()).addGoogleAccount();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // +++ Johnson, don't do anything when user is moving divider
        if (isMovingDivider() || !super.isListShown()) {
            return false;
        }
        // ---
        boolean consume = false;
        switch (item.getItemId()) {
//            case android.R.id.home:
//                startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
//
//                consume = true;
//                /*
//                if (!mActivity.isPadMode() && !isInEditMode()) {
//                    mActivity.switchShortCutView();
//                } else {
//                    startScanFile(new VFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
//                }
//                */
//                if (mActivity.isPadMode()) {
//                    startScanFile(new VFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
//                }
//                break;
            case R.id.add_folder_action:

                boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mIndicatorFile.getAbsolutePath());
                if (bNeedWriteToAppFolder) {
                    WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                    warnDialog.show(mActivity.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
                    consume = true;
                    break;
                }
                if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(mIndicatorFile.getAbsolutePath())) {
                    mActivity.callSafChoose(SafOperationUtility.ACTION_MKDIR);
                    consume = true;
                    break;
                }
                showDialog(DialogType.TYPE_ADD_NEW_FOLDER, mIndicatorFile);
                consume = true;
                break;
            case R.id.sort_item:
                String[] sortTypeString = {
                        getString(SORT_TYPE_STRING_RES[SORT_TYPE]),
                        getString(SORT_TYPE_STRING_RES[SORT_NAME_ASCENDING -1]),
                        getString(SORT_TYPE_STRING_RES[SORT_NAME_DESCENDING -1]),
                        getString(SORT_TYPE_STRING_RES[SORT_DATE_ASCENDING -1]),
                        getString(SORT_TYPE_STRING_RES[SORT_DATE_DESCENDING -1]),
                        getString(SORT_TYPE_STRING_RES[SORT_SIZE_ASCENDING -1]),
                        getString(SORT_TYPE_STRING_RES[SORT_SIZE_DESCENDING -1])
                };

                Bundle args = new Bundle();
                if (mSortType == SortType.SORT_DATE_UP) {
                    args.putInt("initialValue", 3);
                } else {
                    args.putInt("initialValue",( mSortType == SortType.SORT_TYPE_DOWN || mSortType == SortType.SORT_TYPE_UP)?0:mSortType -1);
                }

                args.putStringArray("options", sortTypeString);
                showDialog(DialogType.TYPE_SORT_TYPE_DIALOG, args);
                break;
            case R.id.clear_history_action:
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                        SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
                suggestions.clearHistory();
                consume = true;
                GaMenuItem.getInstance().sendEvents(mActivity, GaMenuItem.CATEGORY_NAME,
                            GaMenuItem.ACTION_CLEAR_SEARCH_HISTORY, null, null);
                break;
            case R.id.cloud_refresh_action:
                //if (remoteVFile!=null&&remoteVFile.getVFieType()==VFileType.TYPE_CLOUD_STORAGE) {
                refreshCloudFileFromMenu();
                consume = true;
                break;
            case R.id.select_all_action:
                getCurrentUsingAdapter().setSelectAll();
                updateEditMode();
                consume = true;
                break;
            case R.id.logout_account:
                CloudStorageRemoveHintDialogFragment removeCloudStorageDialog = CloudStorageRemoveHintDialogFragment.newInstance(mIndicatorFile);
                removeCloudStorageDialog.show(getFragmentManager(), "CloudStorageRemoveHintDialogFragment");
                break;
//            case R.id.settings_action:
//                Intent intent = new Intent(this.getActivity(), SettingActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//                // get storage list
//                ShortCutFragment shortCutFragment = (ShortCutFragment) this.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                if (shortCutFragment != null) {
//                    LinkedList<StorageElement> storageList = shortCutFragment.getStorageList();
//                    if (storageList != null) {
//
//                        ArrayList<String> accountNames = new ArrayList<String>();
//                        ArrayList<String> accountIds = new ArrayList<String>();
//                        ArrayList<Integer> accountTypes = new ArrayList<Integer>();
//                        for (int i=0 ; i<storageList.size() ; i++) {
//                            if (storageList.get(i).mStorageElement.getStorageType() >= StorageType.TYPE_ASUSWEBSTORAGE &&
//                                    storageList.get(i).mStorageElement.getStorageType() <= StorageType.TYPE_GOOGLE_DRIVE) {
//                                accountNames.add(storageList.get(i).mStorageElement.getStorageName());
//                                accountIds.add(storageList.get(i).mStorageElement.getStorageAddress());
//                                accountTypes.add(storageList.get(i).mStorageElement.getStorageType());
//                            }
//                        }
//                        intent.putStringArrayListExtra("accountNames", accountNames);
//                        intent.putStringArrayListExtra("accountIds", accountIds);
//                        intent.putIntegerArrayListExtra("accountTypes", accountTypes);
//                    }
//                }
//
//                this.startActivity(intent);
            case R.id.reset_samba_account:
                PcInfoDbHelper.deleteAccountInfo(null, null);
                ScanLanNetworkPC.getInstance(mActivity).clearSavedAccounts();
                break;
            /*
            case R.id.feedback_action:
                if (ItemOperationUtility.getInstance().checkCtaPermission(mActivity)) {
                    mActivity.initFeedBackResource(mActivity);
                    UserVoice.launchUserVoice(mActivity);
                }
                break;
            */
            case R.id.cta_dialog:
                ItemOperationUtility.getInstance().showCtaDialog(mActivity);
                break;
            default:
                break;
        }

        return consume || super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ArrayList<VFile> list = mShoppingCart.getFiles();
        ArrayList<VFile> list2 = data.getParcelableArrayListExtra("b");

        mShoppingCart.setFiles(list2);
        if (mCab != null) {
            mCab.notifyDataSetChanged();
        }
        getCurrentUsingAdapter().syncWithShoppingCart();
        getCurrentUsingAdapter().notifyDataSetChanged();
    }

    private void refreshCloudFileFromMenu() {
        if (!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
            mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
            return;
        }

        if (SambaFileUtility.updateHostIp) {
            if (SambaFileUtility.ScanFinish) {
                SambaFileUtility.getInstance(getActivity()).startScanNetWorkDevice(true);
            } else {
                ToastUtility.show(getActivity(), R.string.pullfreshlistview_header_hint_loading);
            }
        } else if (RemoteFileUtility.isShowDevicesList) {
            if (remoteVFile == null) {
                return;
            }
            setListShown(false);
            SetScanHostIndicatorPath(remoteVFile.getStorageName());
            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg((remoteVFile).getStorageName(), null, null, (remoteVFile).getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
        } else
            if (mIndicatorFile!=null && mIndicatorFile.getVFieType()==VFileType.TYPE_CLOUD_STORAGE) {
                startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
            }
    }
    private static boolean isRootOrMountDir(String path) {
        return TextUtils.isEmpty(path)
                || path.equals(File.separator)
                || path.equals(WrapEnvironment.getEpadExternalStorageDirectory().getPath());
    }

    @Override
    protected void modifyListAndGridVisibility() {
        if (isCategoryUsingExpandableListView()) {
            mExpandableListView.setVisibility(View.VISIBLE);
        } else {
            mExpandableListView.setVisibility(View.GONE);
            super.modifyListAndGridVisibility();
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        setListAdapter(adapter, false);
    }

    public void setListAdapter(ListAdapter adapter, boolean isOnActivityCreated) {
        // Fix TT-851468: When user switch to DeviceList page
        // we should remove the empty text of ExpandableListView
        if (adapter instanceof DeviceListAdapter) {
            if (mExpandableListView != null) {
                View emptyView = mExpandableListView.getEmptyView();
                if ((emptyView != null) && (emptyView instanceof TextView)) {
                    ((TextView) emptyView).setText("");
                }
            }
        }

        if (!isCategoryUsingExpandableListView() || isOnActivityCreated) {
            super.setListAdapter(adapter);
        }
    }

    @Override
    public void setListShown(boolean shown) {
        //NOTE!!!! dirty fix for TT-775967 and 775950, the error seems comes from between tag 191 ~ 192,
        //but if we trigger invalidate layout, the problem would be solved magically
        try {
            if (isCategoryUsingExpandableListView()) {
                ((TextView) mListView.getEmptyView()).setText("");
                ((TextView) mGridView.getEmptyView()).setText("");
                mListView.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
            } else {
                if (!(Looper.getMainLooper().getThread() == Thread.currentThread())){
                    if (null != mActivity){
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) mExpandableListView.getEmptyView()).setText("");
                            }
                        });
                    }
                }else{
                    ((TextView) mExpandableListView.getEmptyView()).setText("");
                }
            }
        }catch (Throwable ignore){};
        super.setListShown(shown);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu");
        // +++ dynamically handle remote storage case
        //menu.findItem(R.id.logout_account).setVisible(mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE);

        // now support paste action
        // 1. remote to local
        // 2. local to remote
        // 3. remote to remote (cloud storage only)

//        if (mEditPool != null && mEditPool.getSize() > 0) {
//            if (mEditPool.getFile().getVFieType() == VFileType.TYPE_REMOTE_STORAGE
//                    && mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
//                menu.findItem(R.id.paste_action).setVisible(false);
//            } else if (((FileManagerActivity) getActivity()).getDrawerSlideOffset() > 0) {
//                menu.findItem(R.id.paste_action).setVisible(false);
//            } else if ((mIndicatorFile != null) && (isRootOrMountDir(mIndicatorFile.getPath()) || mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getRootScanPath())
//                     || mIndicatorFile.getPath().equals(RemoteFileUtility.getHomeCloudRootPath()))) {
//                   menu.findItem(R.id.paste_action).setVisible(false);
//            } else {
//                menu.findItem(R.id.paste_action).setVisible(true);
//            }
//        }


        SharedPreferences mSharePrefence = mActivity.getSharedPreferences("MyPrefsFile", 0);
        boolean bNewFeature_settings = mSharePrefence.getBoolean("newfeature_settings", true);
        boolean bEnableInsider = mSharePrefence.getBoolean("EnableInsiderProgram", false);
        boolean isAsusDevice = "asus".equalsIgnoreCase(Build.BRAND);

        if ((mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) || SambaFileUtility.updateHostIp || RemoteFileUtility.isShowDevicesList ) {
            menu.findItem(R.id.cloud_refresh_action).setVisible(true);
        } else {
            menu.findItem(R.id.cloud_refresh_action).setVisible(false);
        }
        if (((FileManagerActivity)getActivity()).getDrawerSlideOffset()!=0) {
            menu.findItem(R.id.cloud_refresh_action).setVisible(false);
        }
        //for Samba or HomeBox device list don't support Action menu
        if (SambaFileUtility.updateHostIp) {
            MenuItem tempHideMenu;
//            tempHideMenu = menu.findItem(R.id.paste_action);
//            if (tempHideMenu!=null) tempHideMenu.setVisible(false);
            tempHideMenu = menu.findItem(R.id.search_action);
            if (tempHideMenu != null)tempHideMenu.setVisible(false);
            tempHideMenu = menu.findItem(R.id.reset_samba_account);
            if (tempHideMenu != null) {
                tempHideMenu.setVisible(true);
            }
        } else if (RemoteFileUtility.isShowDevicesList) {
            menu.close();
            MenuItem tempHideMenu;
//            tempHideMenu = menu.findItem(R.id.paste_action);
//            if (tempHideMenu!=null) tempHideMenu.setVisible(false);
            tempHideMenu = menu.findItem(R.id.search_action);
            if (tempHideMenu != null)tempHideMenu.setVisible(false);
        } else if (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile)mIndicatorFile).getStorageType() == StorageType.TYPE_YANDEX) {
            //for Yandex  don't support Action menu
            MenuItem tempHideMenu;
            tempHideMenu = menu.findItem(R.id.clear_history_action);
            if (tempHideMenu!=null) tempHideMenu.setVisible(false);
            tempHideMenu = menu.findItem(R.id.search_action);
            if (tempHideMenu != null)tempHideMenu.setVisible(false);
            tempHideMenu = menu.findItem(R.id.cta_dialog);
            if (tempHideMenu!=null) tempHideMenu.setVisible(false);
        } else if (isInCategory()) {
//            menu.findItem(R.id.paste_action).setVisible(false);
             menu.findItem(R.id.add_folder_action).setVisible(false);
        } else {
            MenuItem tempHideMenu = menu.findItem(R.id.search_action);
            if (tempHideMenu != null)tempHideMenu.setVisible(true);
        }
        // add_foler_action will show by FAB instead of menu
        menu.findItem(R.id.add_folder_action).setVisible(false);

        // disallow sort function when we are category large file and category recent
        menu.findItem(R.id.sort_item).setVisible((isCategoryLargeFile() || isCategoryRecent() || isCategoryGame()) ? false : true);

        if(ItemOperationUtility.getInstance().enableCtaCheck() || !bEnableInsider || WrapEnvironment.IS_VERIZON) {
            ViewUtility.hideMenuItem(menu,R.id.action_invite_betauser);
        }

        int title_instant_update = ZenUiFamily.getZenUiFamilyTitle();
        menu.findItem(R.id.action_instant_update).setTitle(getResources().getString(title_instant_update));
        if(ItemOperationUtility.getInstance().enableCtaCheck() || WrapEnvironment.IS_VERIZON) {
            ViewUtility.hideMenuItem(menu,R.id.action_invite_betauser);

            ViewUtility.hideMenuItem(menu,R.id.action_rateus);

            ViewUtility.hideMenuItem(menu,R.id.action_instant_update);
        }
        if (!isAsusDevice) {
            ViewUtility.hideMenuItem(menu,R.id.action_rateus);
        }

        if(WrapEnvironment.IS_VERIZON) {
            menu.findItem(R.id.action_tell_a_friend).setVisible(false);
            menu.findItem(R.id.action_bug_report).setVisible(false);
        }
        //
        boolean needSaf = ((FileManagerApplication)mActivity.getApplication()).isNeedSafPermission();
        if (!needSaf){
            ViewUtility.hideMenuItem(menu,R.id.saf_tutorial_action);
        }
        if (bNewFeature_settings) {
            ViewUtility.addNewIcon(mActivity, menu.findItem(R.id.action_settings));
        }

        boolean hasNewFeature = bNewFeature_settings;
        ThemeUtility.setThemeOverflowButton(mActivity, hasNewFeature);
        ThemeUtility.setMenuIconColor(mActivity, menu);
        collapseSearchView();
    }

    // Actionbar

    // +++ Action Bar in Edit mode +++
    private class EditModeCallback implements ActionMode.Callback {

        // If action mode is finished by FileManager or by user,
        // this boolean value will be set to false.
        boolean mCloseBySystem = true;

        // @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.d(TAG, "onCreateActionMode mode:" + mode);
            // FIXME:
            // workaround for black status bar, force reset its color
            ColorfulLinearLayout.changeStatusbarColor(mActivity, R.color.theme_color);
            mEditMode = mode;
            mActionModeWrapper = new DoubleLayerActionModeWrapper(mActivity, mEditMode);

            mShoppingCart = new ShoppingCart();

            // show contextual action bar
            if (mCab != null) {
                mCab.setVisibility(View.VISIBLE);
                mCab.setShoppingCart(mShoppingCart);
                mCab.setContextualActionButtonListener(FileListFragment.this);
            }
            getCurrentUsingAdapter().setShoppingCart(mShoppingCart);
            showFAB(false);

            LayoutInflater inflaterView = mActivity.getLayoutInflater();
            View view = inflaterView.inflate(R.layout.editmode_actionbar, null);
            mode.setCustomView(view);
            mSelectedCountText =  (TextView)view.findViewById(R.id.actionbar_text);
            CheckResult result = mShoppingCart.getSelectedCount(); // getCurrentUsingAdapter().getSelectedCount();
            Log.e("WESLEY", "[1] result = " + result.count);
            String itemSelectedStr = getResources()
                    .getQuantityString(R.plurals.number_selected_items, result.count, result.count);
            mSelectedCountText.setText(itemSelectedStr);
            ((FileManagerActivity)getActivity()).setTextViewFont(mSelectedCountText, FontType.ROBOTO_REGULAR);

//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                        mbPopup = true;
//                        showPopupMenu(v);
//                }
//            });

            MenuInflater inflater = getActivity().getMenuInflater();

            if (mIsMultipleSelectionOp) {
                inflater.inflate(R.menu.multiple_selection_mode, menu);
            } else {
                inflater.inflate(R.menu.edit_mode, menu);
            }

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    final View mSelectAll = mActivity.findViewById(R.id.select_all_action);
                    final View mDelete = mActivity.findViewById(R.id.delete_action);
                    if(WrapEnvironment.SUPPORT_FEATURE_ASUS_PEN) {
                        if (null != mSelectAll) {
                            mSelectAll.setOnHoverListener(new View.OnHoverListener() {
                                @Override
                                public boolean onHover(View v, MotionEvent event) {
                                    int what = event.getAction();
                                    switch (what) {
                                        case MotionEvent.ACTION_HOVER_ENTER:
                                            String title = getContext().getResources().getString(R.string.select_all);
                                            mPopupWindow = ViewUtility.showTooltip(title, mSelectAll, getContext(), mPopupWindow);
                                            break;
                                        case MotionEvent.ACTION_HOVER_MOVE:
                                            break;
                                        case MotionEvent.ACTION_HOVER_EXIT:
                                            if (null != mPopupWindow && mPopupWindow.isShowing()) {
                                                mPopupWindow.dismiss();
                                            }
                                            break;
                                    }
                                    return false;
                                }
                            });
                        }
                        if (null != mDelete){
                            mDelete.setOnHoverListener(new View.OnHoverListener() {
                                @Override
                                public boolean onHover(View v, MotionEvent event) {
                                    int what = event.getAction();
                                    switch (what) {
                                        case MotionEvent.ACTION_HOVER_ENTER:
                                            String title = getContext().getResources().getString(R.string.delete);
                                            mPopupWindow = ViewUtility.showTooltip(title, mDelete, getContext(), mPopupWindow);
                                            break;
                                        case MotionEvent.ACTION_HOVER_MOVE:
                                            break;
                                        case MotionEvent.ACTION_HOVER_EXIT:
                                            if (null != mPopupWindow && mPopupWindow.isShowing()) {
                                                mPopupWindow.dismiss();
                                            }
                                            break;
                                    }
                                    return false;
                                }
                            });
                        }
                    }
                }
            });


            updateEditMode();

            return true;
        }

        private void showPopupMenu(View v) {
            popup = new PopupMenu(v.getContext(), v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.popup_menu, popup.getMenu());
            Menu menu = popup.getMenu();
            CheckResult result = getCurrentUsingAdapter().getSelectedCount();
            if (result.count == getCurrentUsingAdapter().getCount() || (mIsMultipleSelectionOp && (result.count == getCurrentUsingAdapter().getFilesCount().count))) {
                menu.getItem(0).setTitle(R.string.deselect_all);
            } else {
                menu.getItem(0).setTitle(R.string.select_all);
            }

            menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getTitle().equals(mActivity.getResources().getText(R.string.select_all))) {
                        getCurrentUsingAdapter().setSelectAll();
                        updateEditMode();
                    }
                    else {
                        onDeselectAll();
                    }
                    return false;
                }
            });
            popup.show();
        }

        // @Override
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//            MenuItem clearAllitem = menu.findItem(R.id.clear_all_action);
//            MenuItem selectAllitem = menu.findItem(R.id.select_all_action);
            Log.i(TAG, "onPrepareActionMode");
            MenuItem shareItem = menu.findItem(R.id.share_action);

            if (mShoppingCart == null) {
                return true;
            }

            CheckResult result = mShoppingCart.getSelectedCount(); // getCurrentUsingAdapter().getSelectedCount();
            Log.e("WESLEY", "[2] result = " + result.count);
            String itemSelected = getResources()
                .getQuantityString(R.plurals.number_selected_items, result.count, result.count);
            mSelectedCountText.setText(itemSelected);
//            if (result.count == getCurrentUsingAdapter().getCount()) {
//                clearAllitem.setVisible(true);
//                selectAllitem.setVisible(false);
//            } else {
//                clearAllitem.setVisible(false);
//                selectAllitem.setVisible(true);
//            }
//            if (!mIsMultipleSelectionOp) {
//                if (result.hasDir) {
//                    shareItem.setVisible(false);
//                } else {
//                    shareItem.setVisible(true);
//                }
//            }

            // for remote storage action: don't support share, cut and delete action
            if (isRootOrMountDir(mIndicatorFile.getPath()) ||
                    (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE)
                            || (mIndicatorFile != null && mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getInstance(getActivity()).getRootScanPath()))) {
                if (shareItem != null) {
                    shareItem.setVisible(false);
                }

                MenuItem menuItem = menu.findItem(R.id.move_to_action);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
                menuItem = menu.findItem(R.id.delete_action);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
            }

//            if (!mIsMultipleSelectionOp) {
//                if (isInCategory() && (mIndicatorFile.equals(mActivity.CATEGORY_FAVORITE_FILE))) {
//                    menu.findItem(R.id.share_action).setVisible(false);
//                } else {
//                    if (ItemOperationUtility.isItemContainDrm(getCurrentUsingAdapter().getFiles(),false,false)) {
//                        ItemOperationUtility.disableItemMenu(menu.findItem(R.id.move_to_action),getString(R.string.move_to));
//                        ItemOperationUtility.disableItemMenu(menu.findItem(R.id.share_action),getString(R.string.action_share));
//                    } else {
//                        menu.findItem(R.id.move_to_action).setTitle(getString(R.string.move_to));
//                        menu.findItem(R.id.share_action).setTitle(getString(R.string.action_share));
//                    }
//                }
//            }
            mActivity.setDrawerBackGroundColor(Color.WHITE);

            MenuItem sItem = menu.findItem(R.id.select_all_action);
            if (sItem != null) {
                if (result.count == getCurrentUsingAdapter().getCount() || (mIsMultipleSelectionOp && (result.count == getCurrentUsingAdapter().getFilesCount().count))) {
                    sItem.setTitle(R.string.deselect_all);
                    sItem.setIcon(R.drawable.asus_ep_ic_unselect_all);
                } else {
                    sItem.setTitle(R.string.select_all);
                    sItem.setIcon(R.drawable.asus_ep_ic_select_all);
                }
            }
            // FIXME:
            // workaround for invisible select_all icon
//            sItem = menu.findItem(R.id.delete_action);
//            sItem.setIcon(R.drawable.asus_ep_ic_delete);


            ThemeUtility.setMenuIconColor(getActivity().getApplicationContext(), menu);
            return true;
        }

        // @Override
        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem item)
        {
            return handleItemClick(item, item.getItemId());
        }

        // @Override
        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            Log.d(TAG, "onDestroyActionMode mode:" + mode + ", mCloseBySystem: " + mCloseBySystem);

            // hide contextual action bar
            if (mCab != null) {
                mCab.setVisibility(View.GONE);
                mCab.removeContextualActionButtonListeners();
            }
            mShoppingCart = null;
            getCurrentUsingAdapter().setShoppingCart(null);
            showFAB(true);

            // menu should be dismissed when edit mode is removed.
            if (mEditModePopupWindow != null) {
                mEditModePopupWindow.dismiss();
            }

            // Deselect all checkbox only if action mode is finished by system.
            // Generally, onDeselectAll() is called before finishing action mode
            // if we want to deselect all check box, thus we don't need to call it again.
            // Sometimes it's necessary to refresh the action mode, but still has
            // to retain the checked state, thus onDeselectAll() will not be called.
            if (mCloseBySystem) {
                onDeselectAll();
            }

            mEditMode = null;
            mActionModeWrapper = null;
            mActivity.setDrawerBackGroundColor(mActivity.DRAWER_BACKGROUD_COLOR);
        }


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mEditModePopupWindow.dismiss();
        handleItemClick(null, mEditModeMenuID.get(position));
    }

    private boolean handleItemClick(MenuItem item, int itemId) {
        Log.d(TAG, "onActionItemClicked item:" + itemId);
            // +++ Johnson, don't do anything when user is moving divider
        if (isMovingDivider()) {
            return false;
        }
            /****for drm file select*****/
        CheckResult result = getCurrentUsingAdapter().getSelectedCount();
        int selectcount = result.count;
            // ---
        switch (itemId) {
//                case R.id.select_all_action:
//                    getCurrentUsingAdapter().setSelectAll();
//                    updateEditMode();
//                    break;
//                case R.id.clear_all_action:
//                    onDeselectAll();
//                    break;
            case R.id.ok_action:
                if (mEditPool.getExtraBoolean()) {
                    handleMoveToAction();
                } else {
                    handleCopyToAction();
                }
                finishEditMode();
                break;
            case R.id.cancel_action:
                finishEditMode();
                break;
            case R.id.apply_selected_file:
                mEditPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                FileUtility.applySelectedFiles(getActivity(), mEditPool.getFiles());
                break;
            case R.id.select_all_action:
                CheckResult selectItems = getCurrentUsingAdapter().getSelectedCount();
                if (selectItems.count == getCurrentUsingAdapter().getCount() || (mIsMultipleSelectionOp && (selectItems.count == getCurrentUsingAdapter().getFilesCount().count))) {
//                        item.setTitle(R.string.select_all);
//                        item.setIcon(R.drawable.asus_ep_ic_select_all);
                    onDeselectAll();
                } else {
                    item.setTitle(R.string.deselect_all);
                    item.setIcon(R.drawable.asus_ep_ic_unselect_all);
                    getCurrentUsingAdapter().setSelectAll();
                    updateEditMode();
                }
                break;
            case R.id.info_action: {
                EditPool infoPool = new EditPool();
                infoPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                VFile file = infoPool.getFile();
                if (file != null) {
                    showDialog(DialogType.TYPE_INFO_DIALOG, file);
                }
            }
                break;
            case R.id.rename_action: {
                EditPool renamePool = new EditPool();
                renamePool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                VFile file = renamePool.getFile();
                if (null == file)
                    break;
                if (getmIndicatorFile().equals(mActivity.CATEGORY_FAVORITE_FILE)) {
                    renameFavoriteFile(file);
                } else {
                    if (ItemOperationUtility.isItemContainDrm(new VFile[]{file}, true, false)) {
                        if (file.isFile()) {
                            ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                        } else {
                            ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                        }
                    } else {
                        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(file.getAbsolutePath());
                        if (bNeedWriteToAppFolder) {
                            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                .newInstance();
                            warnDialog.show(mActivity.getFragmentManager(),
                                "WarnKKSDPermissionDialogFragment");
                        } else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(file.getAbsolutePath())) {
                            if (mActivity instanceof FileManagerActivity) {
                                ((FileManagerActivity) mActivity).callSafChoose(SafOperationUtility.ACTION_RENAME);
                            }
                        } else {
                            showDialog(DialogType.TYPE_RENAME_DIALOG, file);
                        }
                    }
                }
            }
                break;
            case R.id.add_favorite_action:
                EditPool addFavoritePool = new EditPool();
                addFavoritePool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                if (null == addFavoritePool.getFile()){
                    break;
                }
                addFavoriteFile(addFavoritePool.getFile());
                GaAccessFile.getInstance().sendEvents(mActivity, GaAccessFile.CATEGORY_NAME,
                    GaAccessFile.ACTION_ADD_TO_FAVORITE, GaAccessFile.LABEL_FROM_MENU, Long.valueOf(1));
                updateEditMode();
                break;
            case R.id.remove_favorite_action:
                EditPool removeFavoritePool = new EditPool();
                removeFavoritePool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                if (null == removeFavoritePool.getFile()){
                    break;
                }

                removeFavoriteFile(removeFavoritePool.getFiles(), false);
                updateEditMode();
                break;
            case R.id.create_shortcut_action:
                EditPool addShortcutPool = new EditPool();
                addShortcutPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                if (null == addShortcutPool.getFile()){
                    break;
                }
                CreateShortcutUtil.createFolderShortcut(getActivity().getApplicationContext(),
                    addShortcutPool.getFile().getPath(), addShortcutPool.getFile().getName());
                onDeselectAll();
                SharedPreferences mSharePrefence = mActivity.getSharedPreferences("MyPrefsFile", 0);
                mSharePrefence.edit().putBoolean("newfeature_createshortcut_editmode", false).commit();
                updateEditMode();
                GaAccessFile.getInstance().sendEvents(mActivity, GaAccessFile.CATEGORY_NAME,
                    GaAccessFile.ACTION_CREATE_SHORTCUT, null, null);
                GaShortcut.getInstance().sendEvents(mActivity, GaShortcut.CATEGORY_NAME,
                    GaShortcut.ACTION_CREATE_FROM_NON_HOMEPAGE, null, null);
                break;
            case R.id.compress_action:
                EditPool compressPool = new EditPool();
                compressPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                VFile[] array = compressPool.getFiles();

                if (null == compressPool.getFile())
                    break;

                boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(compressPool.getFile().getAbsolutePath());
                if (bNeedWriteToAppFolder) {
                    WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                    warnDialog.show(mActivity.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
                } else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(compressPool.getFile().getAbsolutePath())) {
                    if (mActivity instanceof FileManagerActivity) {
                        ((FileManagerActivity) mActivity).callSafChoose(SafOperationUtility.ACTION_ZIP);
                    }
                } else {
                    ZipDialogFragment.showZipDialog(FileListFragment.this, array, false);
                }
                break;
            case R.id.hidden_zone_action:
                EditPool editPool = new EditPool();
                VFile[] filesToMove = getCurrentUsingAdapter().getFiles();
                editPool.setFiles(filesToMove, true);
                filesToMove = editPool.getFiles();
                if (!mActivity.isNeedPermission(filesToMove, SafOperationUtility.ACTION_MOVE_TO_HIDDEN_ZONE)) {
                    HiddenZoneUtility.moveToHiddenZone(mActivity, getHandler(), editPool, isInCategory());
                    GaAccessFile.getInstance().sendEvents(mActivity, GaAccessFile.CATEGORY_NAME,
                            GaAccessFile.ACTION_MOVE_TO_HIDDEN_CABINET, null, null);
                    GaHiddenCabinet.getInstance().sendMoveToHiddenCabinetEvent(mActivity);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    public void copyFileInPopup(VFile[] copyFile,boolean isFromSearch) {
        mEditPool.setFiles(copyFile, false);
        mEditPool.setExtraBoolean(false);
        PasteDialogFragment.showCopyFileReady(getActivity(), copyFile, false);

        if (!isFromSearch) {
            onDeselectAll();
            getActivity().invalidateOptionsMenu();
        }

        Bundle bd = new Bundle();
        int mode = isCategoryMediaTopDir() ?
                mEditPool.getSize() > 1 ? MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP : MoveToDialogFragment.MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP
                : MoveToDialogFragment.MODE_MOVE_TO;

        VFile source = null;
        if (copyFile[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            // change to sdcard path when copy file in category.
            source = new LocalVFile(FileUtility.changeToSdcardPath(copyFile[0].getPath()));
        } else {
            source = copyFile[0];
        }

        bd.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.copy_to));
        bd.putInt(MoveToDialogFragment.DIALOG_MODE, mode);
        bd.putParcelable(MoveToDialogFragment.CURRENT_FOLDER, source.getParentFile());
        showDialog(DialogType.TYPE_MOVE_TO_DIALOG,bd);
        mActivity.initCloudService();
    }

    public void cutFileInPopup(VFile[] cutFile,boolean isFromSearch) {
        mEditPool.setFiles(cutFile, false);
        mEditPool.setExtraBoolean(true);
        PasteDialogFragment.showCopyFileReady(getActivity(), cutFile, true);

        if (!isFromSearch) {
            onDeselectAll();
            getActivity().invalidateOptionsMenu();
        }

        Bundle bd = new Bundle();
        int mode = isCategoryMediaTopDir() ?
                mEditPool.getSize() > 1 ? MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP : MoveToDialogFragment.MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP
                : MoveToDialogFragment.MODE_MOVE_TO;

        VFile source = null;
        if (cutFile[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            // change to sdcard path when cut file in category.
            source = new LocalVFile(FileUtility.changeToSdcardPath(cutFile[0].getPath()));
        } else {
            source = cutFile[0];
        }

        bd.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.move_to));
        bd.putInt(MoveToDialogFragment.DIALOG_MODE, mode);
        bd.putParcelable(MoveToDialogFragment.CURRENT_FOLDER, source.getParentFile());
        showDialog(DialogType.TYPE_MOVE_TO_DIALOG,bd);
        mActivity.initCloudService();

    }

    public void deleteFileInPopup(VFile[] deleteFile) {
//        mEditPool.setFiles(deleteFile, false);

        VFile[] filesToDelete = getCurrentUsingAdapter().getFiles();
        mDeleteFilePool.setFiles(filesToDelete, false);
        boolean bNeedPermission= false;
        for (int i=0;i<filesToDelete.length;i++){
            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(filesToDelete[i].getParentFile().getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                    .newInstance();
                warnDialog.show(mActivity.getFragmentManager(),
                    "WarnKKSDPermissionDialogFragment");
                bNeedPermission = true;
                break;
            }

            if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(filesToDelete[i].getParentFile().getAbsolutePath())) {
                mActivity.callSafChoose(SafOperationUtility.ACTION_DELETE);
                bNeedPermission = true;
                break;
            }
        }
        if (!bNeedPermission)
            showDialog(DialogType.TYPE_DELETE_DIALOG, mDeleteFilePool);

    }

    // temp function
    public void updateEditMode(int updatePosition) {
        Log.e(TAG, "updateEditMode: " + updatePosition);
        if (isInEditMode()) {
            VFile vFile = (VFile) getCurrentUsingAdapter().getItem(updatePosition);
            if (vFile.getChecked()) {
                mShoppingCart.addVFile(vFile);
            } else {
                mShoppingCart.removeVFile(vFile);
            }
            if (mCab != null) {
                mCab.notifyDataSetChanged();
            }
        }
        updateEditMode();
    }

    public void updateEditMode() {
        final boolean isItemSelected = isItemsSelected();
        Log.d(TAG, "numSelected : " + isItemSelected);
        Log.e(TAG, "mActionModeWrapper: " + mActionModeWrapper);
        if (mActionModeWrapper != null) {
            Log.e(TAG, "isInSecondLayer: " + mActionModeWrapper.isInSecondLayer());
        }

        // prevent user long click in second layer
        if (mActionModeWrapper != null && mActionModeWrapper.isInSecondLayer()) {
            return;
        }

        if ((isItemSelected == false)) {
            // finishEditMode();
            return;
        } else {
            //mPathContainerRoot.setVisibility(View.GONE);
//            mPathHome.setVisibility(View.GONE);
//            mScrollView.setVisibility(View.GONE);
//            mPathIndicator.setBackgroundColor(getResources().getColor(R.color.actionmode_bg));
            //actionBar.setBackgroundDrawable(new ColorDrawable(Color.rgb(0x35,0x35,0x35)));
        }
        Log.d(TAG, "isInEditMode : " + isInEditMode());

        if (isInEditMode()) {
            updateEditModeView();
        } else {
            mLastEditModeCallback = new EditModeCallback();
            getActivity().startActionMode(mLastEditModeCallback);
        }

        setViewSwitcherVisibility(View.GONE);
    }

    public void onDeselectAll() {
        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        if ((adapter == null) /*|| !adapter.isItemsSelected()*/) {
            return;
        }
        adapter.clearItemsSelected();
        getListView().invalidateViews();
        getGridView().invalidateViews();
        if (mExpandableListView != null) {
            mExpandableListView.invalidateViews();
        }
        if (isInEditMode()) {
            finishEditMode();
        }
    }

    private BasicFileListAdapter getCurrentUsingAdapter() {
        if (isCategoryUsingExpandableListView()) {
            return mExpandableListAdapter;
        } else {
            return mAdapter;
        }
    }

    private int measureContentWidth(ListAdapter adapter) {
        // Menus don't tend to be long, so this is more sane than it looks.
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        final Resources res = getResources();
        int popupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
                res.getDimensionPixelSize(R.dimen.menu_popup_window_dialog_width));
        FrameLayout measureParent = null;
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (measureParent == null) {
                measureParent = new FrameLayout(this.mActivity);
            }

            itemView = adapter.getView(i, itemView, measureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();
            if (itemWidth >= popupMaxWidth) {
                return popupMaxWidth;
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    private void updateEditModeView() {

        EditPool selectFilePool = new EditPool();
        selectFilePool.setFiles(/*getCurrentUsingAdapter().getFiles()*/ mShoppingCart.getFiles(), false);

        CheckResult checkResult = mShoppingCart.getSelectedCount(); // getCurrentUsingAdapter().getSelectedCount();

        boolean isOneFile = checkResult.count == 1;
        boolean isOneDir = checkResult.count == 1 && checkResult.hasDir;
        boolean hasDir = checkResult.hasDir;
        boolean isLocalFile = selectFilePool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE;
        boolean isFavoriteFolder = selectFilePool.getFile().isFavoriteFile();

        boolean isInFavoriteFolder
                = mActivity.CATEGORY_FAVORITE_FILE.equals(mIndicatorFile);

        Menu menu = mEditMode.getMenu();

        if (menu == null) {
            Log.w(TAG, "do not update edite mode view, menu == null");
            return;
        }

        MenuItem renameItem = menu.findItem(R.id.rename_action);
        if (renameItem != null) {
            renameItem.setVisible(isOneFile && !isCategoryMedia());
        }

        MenuItem infoItem = menu.findItem(R.id.info_action);
        if (infoItem != null) {
            infoItem.setVisible(isOneFile);
        }

        if (mCab != null) {
            int flag = 0;
            if (hasDir) {
                flag = flag | ContextualActionBar.FLAG_HAS_DIRECTORY;
            }
            if (isInFavoriteFolder) {
                flag = flag | ContextualActionBar.FLAG_IS_IN_FAVORITE_CATEGORY;
            }
            mCab.updateItemVisibility(flag);
            mCab.notifyDataSetChanged();
        }
        /*MenuItem moveToItem =  menu.findItem(R.id.move_to_action);
        if (moveToItem != null) {
            moveToItem.setVisible(isInFavoriteFolder ? false : true);
        }

        MenuItem copyToItem = menu.findItem(R.id.copy_to_action);
        if (copyToItem != null) {
            copyToItem.setVisible(isInFavoriteFolder ? false : true);
        }

        MenuItem shareItem = menu.findItem(R.id.share_action);
        if (shareItem != null) {
            shareItem.setVisible(isInFavoriteFolder ? false : (hasDir ? false : true));
        }

        MenuItem deleteItem = menu.findItem(R.id.delete_action);
        if (deleteItem != null) {
            deleteItem.setVisible(isInFavoriteFolder ? false : true);
        }*/

        MenuItem addFavoriteItem = menu.findItem(R.id.add_favorite_action);
        MenuItem removeFavoriteItem = menu.findItem(R.id.remove_favorite_action);
        MenuItem createShortcutItem = menu.findItem(R.id.create_shortcut_action);
        MenuItem compressItem = menu.findItem(R.id.compress_action);
        MenuItem hiddenZoneItem = menu.findItem(R.id.hidden_zone_action);

        if (isLocalFile) {
            if (addFavoriteItem != null) {
                addFavoriteItem.setVisible(isOneDir && !isFavoriteFolder && !belongToCategoryMedia());
            }
            if (removeFavoriteItem != null) {
                removeFavoriteItem.setVisible((isOneDir || isInFavoriteFolder) && isFavoriteFolder && !belongToCategoryMedia());
            }
            if (createShortcutItem != null) {
                // Favorite category can show create shortcut in menu.
                createShortcutItem.setVisible(isOneDir && !belongToCategoryMedia());
            }
            if (compressItem != null) {
                compressItem.setVisible(!isInCategory());
            }
            if (hiddenZoneItem != null) {
                boolean hasSetupHiddenZoneComplete = PinCodeAccessHelper.getInstance().hasPinCode();
                hiddenZoneItem.setVisible(hasSetupHiddenZoneComplete && !isInFavoriteFolder);
            }
        } else {
            if (addFavoriteItem != null) {
                addFavoriteItem.setVisible(false);
            }
            if (removeFavoriteItem != null) {
                removeFavoriteItem.setVisible(false);
            }
            if (createShortcutItem != null) {
                createShortcutItem.setVisible(false);
            }
            if (compressItem != null) {
                compressItem.setVisible(false);
            }
            if (hiddenZoneItem != null) {
                hiddenZoneItem.setVisible(false);
            }
        }

        // for remote storage action: don't support share, cut and delete action
        /*if (isRootOrMountDir(mIndicatorFile.getPath()) ||
                (mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE)
                        || (mIndicatorFile != null && mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getInstance(getActivity()).getRootScanPath()))) {
            if (shareItem != null) {
                shareItem.setVisible(false);
            }
            if (moveToItem != null) {
                moveToItem.setVisible(false);
            }
            if (deleteItem != null) {
                deleteItem.setVisible(false);
            }
        }*/

        updateMenuIndicator();

        mEditMode.invalidate();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void updateMenuIndicator() {
        MenuItem menuIndicator = mEditMode.getMenu()
                .findItem(R.id.menu_overflow);

        if (menuIndicator != null) {
            SubMenu subMenu = menuIndicator.getSubMenu();

            if (subMenu == null) {
                return;
            }

            mEditModePopupWindow = new ListPopupWindow(mActivity);
            mEditModeMenuStrs = new ArrayList<String>();
            mEditModeMenuID = new ArrayList<Integer>();
            mEditModeNewFeatureList = new ArrayList<Integer>();

            for (int i = 0; i < subMenu.size(); i++) {
                MenuItem item = subMenu.getItem(i);
                if (item.isVisible()) {
                    mEditModeMenuStrs.add(item.getTitle().toString());
                    mEditModeMenuID.add(item.getItemId());
                    checkNewFeature(item.getItemId());
                }
            }

            listpopupAdapter adapter = new listpopupAdapter(mActivity, R.layout.popup_menu_item_layout, mEditModeMenuStrs);

            adapter.setIsNewFeature(mEditModeNewFeatureList);

            mEditModePopupWindow.setAdapter(adapter);
            mEditModePopupWindow.setModal(true);
            if (!mHasEditModeContentWidth) {
                mEditModeContentWidth = measureContentWidth(adapter);
                mHasEditModeContentWidth = true;
            }
            mEditModePopupWindow.setContentWidth(mEditModeContentWidth);
            mEditModePopupWindow.setOnItemClickListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                mEditModePopupWindow.setDropDownGravity(GravityCompat.END);

            View menuIndicatorView = menuIndicator.getActionView();
            if (menuIndicatorView != null) {
                mEditModePopupWindow.setAnchorView(menuIndicatorView);
                View aBadge = menuIndicatorView.findViewById(R.id.ImgView_badge);
                aBadge.setVisibility(mEditModeNewFeatureList.size() > 0? View.VISIBLE : View.GONE);
                menuIndicatorView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mEditModePopupWindow.show();
                    }
                });
            }

            menuIndicator.setVisible(mEditModeMenuID != null && mEditModeMenuID.size() > 0);
        }
    }

    private void checkNewFeature(int itemId) {
        if (itemId == R.id.create_shortcut_action) {
            SharedPreferences mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
            /*
            boolean bNewFeature_createshortcut= mSharePrefence.getBoolean("newfeature_createshortcut_editmode", true);
            if (bNewFeature_createshortcut)
                mEditModeNewFeatureList.add(mEditModeMenuID.size() - 1);
            */
        }
    }

    public boolean isInEditMode() {
        return mEditMode != null;
    }

    public boolean isInSecondLayerEditMode() {
        return (mActionModeWrapper != null && mActionModeWrapper.isInSecondLayer());
    }

    public void finishEditMode() {
        if (isInEditMode()) {
            mLastEditModeCallback.mCloseBySystem = false;
            mEditMode.finish();
            setViewSwitcherVisibility(View.VISIBLE);

            getListView().invalidateViews();
            getGridView().invalidateViews();
        }
    }

    public boolean isItemsSelected() {
        return mShoppingCart != null ?
                mShoppingCart.getSize() != 0 : getCurrentUsingAdapter().isItemsSelected();
    }

    // --- End ---

    // back key action
    public boolean onBackPressed() {
        if (mIndicatorFile != null) {
            String CurrentPath = mIndicatorFile.getAbsolutePath();
            if (CurrentPath.equals(DEFAULT_INDICATOR_FILE) || CurrentPath.equals("/Removable/MicroSD") || CurrentPath.equals("/Removable/USBdisk1")) {
                 ItemOperationUtility.getInstance().ScanInterDiskDrm(CurrentPath);
            }
        }

        if (mIsComFromSearch) {
            // return orignal path
            if (mOldIndicatorFile != null) {
                int type = mOldIndicatorFile.getVFieType();
                if (type == VFileType.TYPE_LOCAL_STORAGE) {
                    startScanFile(mOldIndicatorFile, ScanType.SCAN_CHILD);
                    ((FileManagerActivity) getActivity()).reSearch(mActivity.getSearchQueryKey());
                } else if (type == VFileType.TYPE_CATEGORY_STORAGE) {
                    startScanCategory(mOldIndicatorFile, true /* research when load completed */);
                }
            }
            ((FileManagerActivity) getActivity()).showSearchFragment(FragmentType.NORMAL_SEARCH, true);
            mIsComFromSearch = false;
            return false;
        }

        // In multiple selection mode, the back button will not act if the parent of
        // current folder is root.
        /*****add for remember last select item*****/
        ItemOperationUtility.getInstance().setNeedGoBack(true);
        /*****add for remember last select item*****/

        if (mIsMultipleSelectionOp) {
            if (!isBlockedFolder(mIndicatorFile)) {
                startScanFile(new LocalVFile(mIndicatorFile.getParent()), ScanType.SCAN_CHILD);
                return false;
            }
            return true;
        }

        //when on device view for samba
        if (SambaFileUtility.updateHostIp||RemoteFileUtility.isShowDevicesList) {
            RemoteFileUtility.isShowDevicesList = false;
            SambaFileUtility.updateHostIp = false;
            // Don't return true, we need to resume the last path (homepage)
            // return true;
        }

        ItemOperationUtility.getInstance().backKeyPressed(true);
        VFile lastPathFile = ItemOperationUtility.getInstance().getLastPath();

        boolean savedPathEmpty = false;
        if (lastPathFile == null) {
            savedPathEmpty = true;
            lastPathFile = mIndicatorFile;
        }


        // handle cloud storage case
        if (lastPathFile != null && lastPathFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            // handle root folder case

            if (savedPathEmpty && ((RemoteVFile)lastPathFile).getName().equals("")) {
                return true;
            } else {
                // update back button event, when using file id to get the list
                switch(((RemoteVFile)lastPathFile).getStorageType()) {
                case StorageType.TYPE_ASUSWEBSTORAGE:
                case StorageType.TYPE_SKYDRIVE:
                case StorageType.TYPE_GOOGLE_DRIVE:
                case StorageType.TYPE_HOME_CLOUD:
                    RemoteVFile tempHomeFile = (RemoteVFile)lastPathFile;
                    if (tempHomeFile.getStorageType()==StorageType.TYPE_HOME_CLOUD) {
                        RemoteVFile tempParent = tempHomeFile.getParentFile();
                        if ((tempParent.getName()==null||tempParent.getName().equals(""))&&tempParent.getAbsolutePath().equals(File.separator+tempParent.getStorageName())) {
                            setListShown(false);
                            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(tempHomeFile.getStorageName(), null, null, tempHomeFile.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                            SetScanHostIndicatorPath(tempParent.getStorageName());
                            break;
                        }
                    }
                    mIsBackEvent = false;
                    startScanFile(lastPathFile , ScanType.SCAN_CHILD);
                    break;
                case StorageType.TYPE_DROPBOX:
                case StorageType.TYPE_YANDEX:
                case StorageType.TYPE_BAIDUPCS:
                    if (lastPathFile.getAbsoluteFile().equals("/"+ ((RemoteVFile)lastPathFile).getStorageName() + "/") ||
                            lastPathFile.getName().equals("")) {
                        break;
                    }
                    startScanFile(lastPathFile.getParentFile() , ScanType.SCAN_CHILD);
                    break;
                }
            }
            return false;
        } else if (lastPathFile != null && lastPathFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
            //handle samba storage case
            if (SambaFileUtility.getInstance(getActivity()).getRootScanPath().equals(lastPathFile.getAbsolutePath())) {
                SambaFileUtility.getInstance(getActivity()).startScanNetWorkDevice(false);
                return false;
            }
            String indicatorPath = ((SambaVFile)lastPathFile).getIndicatorPath();
            if (indicatorPath != null) {
                String[] folderStrs = indicatorPath.split(String.valueOf(File.separatorChar));
                if (folderStrs != null) {
                    int smbcount = folderStrs.length;
                    String tmp = "";
                    for (int i=1;i < smbcount - 1;i++) {
                        tmp += (folderStrs[i] + File.separatorChar);
                    }
                    if (!TextUtils.isEmpty(tmp)) {
                        tmp.trim().substring(1);
                        VFile tempVFile = new SambaVFile(SambaFileUtility.getInstance(getActivity()).getRootScanPath() + tmp);
                        startScanFile(tempVFile , ScanType.SCAN_CHILD);
                    } else {
                        VFile tempVFile = new SambaVFile(SambaFileUtility.getInstance(getActivity()).getRootScanPath());
                        startScanFile(tempVFile , ScanType.SCAN_CHILD);
                    }
                }
            }
            return false;
        } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
            /*if (mIndicatorFile.getParentFile().equals(mActivity.CATEGORY_IMAGE_FILE)) {
                startScanCategory(mActivity.CATEGORY_IMAGE_FILE);
                return false;
            }
            mActivity.showSearchFragment(FragmentType.HOME_PAGE, true);*/
            startScanCategory(mIndicatorFile.getParentFile());
            return false;
        }

        if (savedPathEmpty && !isRootFolder(lastPathFile)) {
            startScanFile(lastPathFile.getParentFile() , ScanType.SCAN_CHILD);
            return false;
        } else if (!savedPathEmpty) {
            if (VFileType.TYPE_CATEGORY_STORAGE == lastPathFile.getVFieType()) {
                unregisterObserver();
                startScanCategory(lastPathFile);
            } else {
                startScanFile(lastPathFile,ScanType.SCAN_CHILD);
            }
            return false;
        }

        return true;
    }

    private static boolean isRootFolder(VFile f) {
        return (f != null) && (f.getAbsolutePath().equals("/")) ||
        // consider remote storage path: /deviceName/
        ((f.getVFieType() == VFileType.TYPE_REMOTE_STORAGE || f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) && (((RemoteVFile)f).getPath().equals("/" + ((RemoteVFile)f).getStorageName()) || ((RemoteVFile)f).getPath().equals("/")));
    }

    // ++ Willie
    // Determine whether the current folder is the blocked folder that can't go back to parent or not.
    private static boolean isBlockedFolder(VFile f) {
        return (f != null) && (f.getAbsolutePath().equals(DEFAULT_INDICATOR_FILE)
                || f.getAbsolutePath().equals(PATH_REMOVABLE)
                || isRootFolder(f));
    }

    @Override
    public void onClick(View view) {
        collapseSearchView();
        // +++ Johnson, don't do anything when user is moving divider
        if (isMovingDivider()) {
            return;
        }
        // ---
        int type = -1;
        boolean isReturn = false;
        switch (view.getId()) {
            case R.id.path_home:
                if (RemoteFileUtility.isShowDevicesList||SambaFileUtility.updateHostIp) {
//                    mPathHome.setEnabled(false);
                    SambaFileUtility.getInstance(getActivity()).hideSambaDeviceListView();
                    SambaFileUtility.getInstance(getActivity()).stopOnlinePlayServer();// stop Http Server
                    mPathHome.setEnabled(true);
                }
//                onBackPressed();
                unregisterObserver();
                if (isInEditMode()) {
                    popupNavigationWindow(view);
                } else {
                    mActivity.showSearchFragment(FragmentType.HOME_PAGE, true);
                }
                isReturn = true;
                break;
            case R.id.sort_type_container:
                type = SORT_TYPE;
                break;
            case R.id.sort_name_container:
                type = SORT_NAME;
                break;
            case R.id.sort_size_container:
                type = SORT_SIZE;
                break;
            case R.id.sort_date_container:
                type = SORT_DATE;
                break;
            case R.id.view_switcher:
                isReturn = true;
                contentViewSwitch();
                break;
            case R.id.fab:
                if (isCategoryFavorite()) {
                    isReturn = true;
                    Bundle bundle = new Bundle();
                    bundle.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.add_favorite_folder));
                    showDialog(DialogType.TYPE_FILE_PICKER_DIALOG, bundle);
                } else {
                    isReturn = true;

                    boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mIndicatorFile.getAbsolutePath());
                    if (bNeedWriteToAppFolder) {
                        WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                            .newInstance();
                        warnDialog.show(mActivity.getFragmentManager(),
                            "WarnKKSDPermissionDialogFragment");
                        break;
                    }else if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(mIndicatorFile.getAbsolutePath())) {
                        mActivity.callSafChoose(SafOperationUtility.ACTION_MKDIR);
                        break;
                    }
                    showDialog(DialogType.TYPE_ADD_NEW_FOLDER, mIndicatorFile);
                }
                break;
            default:
                break;
        }

        if (isReturn)
            return;

        sortFiles(type);
    }

    private void popupNavigationWindow(View view) {
        int mode = 0;
        if (isInEditMode() && isInSecondLayerEditMode()) {
            mode = MoveToDialogFragment.MODE_NAVIGATION_NORMAL;
        } else {
            switch (mIndicatorFile.getVFieType()) {
                case VFileType.TYPE_LOCAL_STORAGE:
                    mode = MoveToDialogFragment.MODE_NAVIGATION_LOCAL_ONLY;
                    break;
                case VFileType.TYPE_REMOTE_STORAGE:
                    mode = MoveToDialogFragment.MODE_NAVIGATION_SAMBA_ONLY;
                    break;
                case VFileType.TYPE_CLOUD_STORAGE:
                    mode = MoveToDialogFragment.MODE_NAVIGATION_CLOUD_ONLY;
                    break;
                default:
                    mode = MoveToDialogFragment.MODE_NAVIGATION_NORMAL;
            }
        }

        final PathIndicatorPopupWindow p = new PathIndicatorPopupWindow(mActivity, view, mode);
        p.setListener(new PathIndicatorPopupWindow.PathIndicatorPopupWindowListener() {
            @Override
            public void onPopupWindowPress(MoveToNaviAdapter.ClickIconItem clickIconItem) {
                Log.e(TAG, "onPopupWindowPress: " + (clickIconItem == null ? "NULL" : clickIconItem.toString()));
                if (clickIconItem.getItemType() == MoveToNaviAdapter.ClickIconItem.LOCAL_STORAGE) {
                    startScanFile(clickIconItem.getFile(), ScanType.SCAN_CHILD, false);
                } else if (clickIconItem.getItemType() == MoveToNaviAdapter.ClickIconItem.NETWORK_STORAGE) {
                    SambaFileUtility.getInstance(mActivity).startScanNetWorkDevice(false);
                } else if (clickIconItem.getItemType() == MoveToNaviAdapter.ClickIconItem.CLOUD_STORAGE) {
                    if (!((FileManagerApplication) mActivity.getApplication())
                            .isNetworkAvailable()) {
                        mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                                VFileType.TYPE_CLOUD_STORAGE);
                        return;
                    }

                    if (!ItemOperationUtility.getInstance().checkCtaPermission(
                            mActivity)) {
                        return;
                    }

                    // MoveToDialogFragment f =
                    // (MoveToDialogFragment)mActivity.getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                    // f.switchFragmentView();

                    boolean isMounted = clickIconItem.getMounted();
                    int index = MoveToNaviAdapter.findStorageIndexByTitle(mActivity, clickIconItem.getStorageName());
                    int couldType = -1;
                    int storageType = -2;
                    switch (index) {
                        case MoveToNaviAdapter.CloudStorageIndex.ASUS_HOMEBOX:
                            couldType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                            storageType = StorageType.TYPE_HOME_CLOUD;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.ASUS_WEBSTORAGE:
                            couldType = MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
                            storageType = StorageType.TYPE_ASUSWEBSTORAGE;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.DROPBOX:
                            couldType = MsgObj.TYPE_DROPBOX_STORAGE;
                            storageType = StorageType.TYPE_DROPBOX;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.BAIDUPCS:
                            couldType = MsgObj.TYPE_BAIDUPCS_STORAGE;
                            storageType = StorageType.TYPE_BAIDUPCS;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.GOOGLDRIVE:
                            couldType = MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                            storageType = StorageType.TYPE_GOOGLE_DRIVE;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.SKYDRIVE:
                            couldType = MsgObj.TYPE_SKYDRIVE_STORAGE;
                            storageType = StorageType.TYPE_SKYDRIVE;
                            break;
                        case MoveToNaviAdapter.CloudStorageIndex.YANDEX:
                            couldType = 9;
                            storageType = StorageType.TYPE_YANDEX;
                            break;
                    }

            /*
             * if(index == CloudStorageIndex.ASUS_HOMEBOX){
             * RemoteFileUtility.isShowDevicesList = true; }else {
             * RemoteFileUtility.isShowDevicesList = false; }
             */

                    if (isMounted && index != 0) {
                        openCloudStorage(clickIconItem, storageType);
                    } else if (index == 0) {
                        GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                                GaCloudStorage.ACTION_OPEN_NETWORK_PLACE, null, null);
                    } else {
                        RemoteAccountUtility.getInstance(mActivity).addAccount(couldType);
                    }

//                            if (!isPreviousOrCurrentFolder) {
//                                GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
//                                        GaMoveToDialog.ACTION_GO_TO_CLOUD_STORAGE, null, null);
//                            }

                }
                p.dismiss();
            }
        });
    }

    private void openCloudStorage(MoveToNaviAdapter.ClickIconItem itemIcon, int storageType) {
        Log.d(TAG, "Open Cloud " + itemIcon.getStorageName());

        GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                itemIcon.getStorageName(), null, null);

        RemoteAccountUtility.AccountInfo accountInfo = itemIcon.getAccountInfo();

        if (accountInfo == null) {
            Log.w(TAG, "cannot open cloud storage since account == null, storageType = "
                    + storageType);
            return;
        }

        RemoteVFile cloudRootVFile = new RemoteVFile("/"
                + accountInfo.getAccountName(), VFileType.TYPE_CLOUD_STORAGE,
                accountInfo.getAccountName(), storageType, "");
        cloudRootVFile.setStorageName(accountInfo.getAccountName());
        cloudRootVFile.setFileID("root");
        cloudRootVFile.setFromFileListItenClick(true);
        if (!RemoteAccountUtility.getInstance(mActivity).validateToken(cloudRootVFile)) {
            Log.d(TAG, "valideToken failed, to get token");
            ShortCutFragment.currentTokenFile = cloudRootVFile;
            // if(cloudRootVFile instanceof RemoteVFile &&
            // (cloudRootVFile.getStorageType()==StorageType.TYPE_HOME_CLOUD)){
            // FileListFragment fileListFragment = (FileListFragment)
            // mActivity.getFragmentManager().findFragmentById(R.id.filelist);
            // if (fileListFragment!=null) {
            // fileListFragment.setRemoteVFile(cloudRootVFile);
            // }
            // } this is for pulltoRefresh
            RemoteAccountUtility.getInstance(mActivity).getToken(cloudRootVFile);
            return;
        } else {
            ShortCutFragment.currentTokenFile = null;
            if (cloudRootVFile instanceof RemoteVFile
                    && (cloudRootVFile.getStorageType() == StorageType.TYPE_HOME_CLOUD)) {
                MoveToDialogFragment MoveToListFragment = (MoveToDialogFragment) mActivity
                        .getFragmentManager().findFragmentByTag(
                                MoveToDialogFragment.DIALOG_TAG);
                if (MoveToListFragment != null) {
                    // fileListFragment.enablePullReFresh(true);
                    MoveToListFragment.switchFragmentView();
                    MoveToListFragment.setmIndicatorFile(cloudRootVFile);
                    MoveToListFragment.setListShown(false);
                    MoveToListFragment.SetScanHostIndicatorPath(cloudRootVFile
                            .getStorageName());
                    RemoteFileUtility.getInstance(mActivity).sendCloudStorageMsg(
                            (cloudRootVFile).getStorageName(),
                            null,
                            null,
                            (cloudRootVFile).getMsgObjType(),
                            CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                }
                return;
            }
        }

        startScanFile(cloudRootVFile, ScanType.SCAN_CHILD, false);
    }


    //+++ Alex
    public void sortFiles(int type) {
        if (mActivity.isPadMode() || (!mActivity.isPadMode() && (type <= SORT_SIZE_DESCENDING))) {
           /* if (type > SORT_TYPE) {
                if (type != mSortType) {
                    mSortImage[type/2].setVisibility(View.VISIBLE);
                    mSortImage[mSortType / 2].setVisibility(View.GONE);
                    mSortType = type ;
                    mSortImage[type/2].getDrawable().setLevel(type % 2);
                } else {
                    if (mSortImage[type].getDrawable().getLevel() == 0) {
                        mSortImage[type].getDrawable().setLevel(1);
                        mSortType = type * 2 + 1;
                    } else {
                        mSortImage[type].getDrawable().setLevel(0);
                        mSortType = type * 2;
                    }
                }
            }
        } else {
            if (type == 7) {
                 mSortType = SortType.SORT_DATE_DOWN;
            }
            else if (type == 6) {
                 mSortType = SortType.SORT_DATE_UP;
            }*/
            if (type > SORT_TYPE) {
                 mSortImage[type / 2].setVisibility(View.VISIBLE);
                 mSortImage[mSortType / 2].setVisibility(View.GONE);
                 mSortType = type ;
                 mSortImage[type/2].getDrawable().setLevel(type % 2);
            } else {
                 if (mSortImage[type / 2].getDrawable().getLevel() == 0) {
                     mSortImage[type].getDrawable().setLevel(1);
                     mSortType = type * 2 + 1;
                 } else {
                     mSortImage[type].getDrawable().setLevel(0);
                     mSortType = type * 2;
                 }
            }
        }
        sortFilesUpdateAdapter();
        //startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
    }

    private void sortFilesUpdateAdapter() {
        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        if (adapter != null && adapter instanceof BasicFileListAdapter) {
            VFile[] vFiles = adapter.getFiles();
            sortFilesBySortType(vFiles, mSortType);
            ItemOperationUtility.getInstance().resetScrollPositionList();
            adapter.updateAdapter(vFiles, false, mSortType, null);
        }
    }

    private void sortFilesBySortType(VFile[] files, int sortType) {
        if (files != null) {
            if (isCategoryRecent()) {
                Arrays.sort(files, SortUtility.getComparator(SortType.SORT_DATE_DOWN));
            } else if (isCategoryLargeFile()) {
                Arrays.sort(files, SortUtility.getComparator(SortType.SORT_SIZE_UP));
            } else if (isCategoryGame()) {
                // DO NOT ADJUST SORTING
            } else {
                Arrays.sort(files, SortUtility.getComparator(sortType));
            }
        }
    }

    public void goToLocation(VFile file, boolean isFromSearch) {
        mOldIndicatorFile = getIndicatorFile();
        ((FileManagerActivity) getActivity()).showSearchFragment(FragmentType.NORMAL_SEARCH, false);
        startScanFile(file, ScanType.SCAN_CHILD);
        mIsComFromSearch = isFromSearch;
    }

    public void goToLocation(VFile file) {
        goToLocation(file, true);
    }

    public void goToAlbum(VFile file) {
        mOldIndicatorFile = getIndicatorFile();
        ((FileManagerActivity) getActivity()).showSearchFragment(FragmentType.NORMAL_SEARCH, false);
        startScanAlbums(file);
        mIsComFromSearch = true;
    }

    public void startScanFile(VFile file, int scanType) {
        if (file != null && file.getVFieType() != VFileType.TYPE_LOCAL_STORAGE && file.getVFieType() != VFileType.TYPE_CATEGORY_STORAGE) {
            if (!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                return;
            }
        }
        startScanFile(file, scanType, true);
    }
    private boolean isNeedRefreshToken = false;
    public void setNeedRefreshToken(boolean isNeed) {
        isNeedRefreshToken = isNeed;
    }
    public boolean getNeedRefreshToken() {
        return isNeedRefreshToken ;
    }
    public void startScanFile(VFile file, int scanType, boolean scrollToSelect) {
        Log.d(TAG, "startScanFile");

        boolean isSamePath = false;
        if (file == null) {
            //setNeedRefreshToken(false);
            Log.e(TAG, "startScanFile, file == null");
            return;
        } else {
            if (mIndicatorFile != null && mIndicatorFile.getVFieType() == file.getVFieType() && mIndicatorFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                isSamePath = true;
            }
        }

        if (file.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
            startScanCategory(file);
            return;
        }

        adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);

        //if (isHidden() && !mActivity.mIsShowHomePageFragment) {
        if (isHidden()) {
            mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);
        }

        if (!isInEditMode()) {
            showFAB(true);
        }

        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            Log.d(TAG,"scan file :" + ((RemoteVFile)file).getName());
        } else {
            Log.d(TAG,"scan file path :" + file.getAbsolutePath());
        }
//        if (!ItemOperationUtility.getInstance().isFromBackPress() && !isSamePath) {
//            ItemOperationUtility.getInstance().saveOperationPath(mIndicatorFile);
//        }
//        if (ItemOperationUtility.getInstance().isFromBackPress())
//            ItemOperationUtility.getInstance().backKeyPressed(false);
        if (file.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            //get DRM for AT&T
            updateDrmPaths();

            RemoteFileUtility.isShowDevicesList = false;

            try {
                if (DEFAULT_INDICATOR_FILE_CANONICAL_PATH == null) {
                    DEFAULT_INDICATOR_FILE_CANONICAL_PATH = FileUtility.getCanonicalPath(WrapEnvironment.getEpadInternalStorageDirectory());
                }
                if (FileUtility.getCanonicalPath(file).equals(DEFAULT_INDICATOR_FILE_CANONICAL_PATH)) {
                    file = new LocalVFile(DEFAULT_INDICATOR_FILE);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            /*if (WrapEnvironment.SUPPORT_REMOVABLE) {
                String path = file.getAbsolutePath();
                if (path.equalsIgnoreCase("/storage/MicroSD") || path.equalsIgnoreCase("/storage/sdcard1")) {
                    file = new LocalVFile("/Removable/MicroSD");
                } else if (path.equalsIgnoreCase("/storage/USBdisk1")) {
                    file = new LocalVFile("/Removable/USBdisk1");
                } else if (path.equalsIgnoreCase("/storage/USBdisk2")) {
                    file = new LocalVFile("/Removable/USBdisk2");
                }
            }*/
        }

        //Kelly add: only enable pullrefresh in cloud and remote mode
        if (file.getVFieType() == VFileType.TYPE_LOCAL_STORAGE)
            enablePullReFresh(false);
        else
            enablePullReFresh(true);

        String accountNameStorageType="";
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            RemoteVFile tempFile = (RemoteVFile)file;
            String accountName = tempFile.getStorageName();
            int storageType = tempFile.getStorageType();
             accountNameStorageType = accountName + "_" + storageType;
            //Log.i("!!!!!!!!!!!!!!accountName!!!!!!!!!!!!!",""+accountName);
            //Log.i("!!!!!!!!!!!!!!storageType!!!!!!!!!!!!!",""+storageType);
            //Log.i("!!!!!!!!!!!!!!accountNameStorageType!!!!!!!!!!!!!",""+accountNameStorageType);
            //Log.i("!!!!!!!!!!!!!!getAbsolutePath!!!!!!!!!!!!!",""+tempFile.getAbsolutePath());
            //Log.i("!!!!!!!!!!!!!!getFileID!!!!!!!!!!!!!",""+tempFile.getFileID());
            if (!mAccountNameStorageType.equals(accountNameStorageType)) {

                if (RemoteVFile.AccountIdpathMap.containsKey(accountNameStorageType)) {

                    //RemoteVFile.IdPathMap = RemoteVFile.AccountIdpathMap.get(accountName + "_" + storageType);
                } else {
                    RemoteVFile.AccountIdpathMap.put(accountNameStorageType, new HashMap<String, String>());
                }
                if (RemoteVFile.AccountPathIdMap.containsKey(accountNameStorageType)) {
                    //RemoteVFile.PathIdMap = RemoteVFile.AccountPathIdMap.get(accountName + "_" + storageType);
                } else {
                    RemoteVFile.AccountPathIdMap.put(accountNameStorageType, new HashMap<String, String>());
                }
                mAccountNameStorageType = accountNameStorageType;
            }

             if ((tempFile.getFromFileListItenClick()) && (tempFile.getFileID() != null) && (!tempFile.getFileID().equals(""))) {
                if (tempFile.getFileID().equals("root")) {
                    if (RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get( tempFile.getAbsolutePath())!=null) {
                        tempFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempFile.getAbsolutePath()));
                    }
                } else {
                    RemoteVFile.AccountIdpathMap.get(accountNameStorageType).put(tempFile.getFileID(), tempFile.getAbsolutePath());
                    RemoteVFile.AccountPathIdMap.get(accountNameStorageType).put( tempFile.getAbsolutePath(),tempFile.getFileID());
                }
            } else {
                if (tempFile.getFileID() == null || tempFile.getFileID().equals("")) {
                    tempFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempFile.getAbsolutePath()));
                } else {
                     //RemoteVFile.AccountIdpathMap.get(accountNameStorageType).put(tempFile.getFileID(), tempFile.getAbsolutePath());
                     RemoteVFile.AccountPathIdMap.get(accountNameStorageType).put( tempFile.getAbsolutePath(),tempFile.getFileID());
                     tempFile.setAbsolutePath(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempFile.getFileID()));
                }
            }
        }
        // update indicator path
        // Google Drive case, we should update the indicator path to parent path
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            RemoteVFile tempVFile = (RemoteVFile)file;
//            if (mIsBackEvent) {
//                 tempVFile = ((RemoteVFile)file).getParentFile();
//                 tempVFile.setFileID(((RemoteVFile)file).getParentFileID());
//            }
            if (RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get( tempVFile.getAbsolutePath())!=null) {
                tempVFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempVFile.getAbsolutePath()));
            }
            if (!(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempVFile.getFileID()) == null)) {
                tempVFile.setAbsolutePath(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempVFile.getFileID()));
            }
            else {
                tempVFile.setAbsolutePath(tempVFile.getAbsolutePath());
                //Log.d("jack","file path:"+tempVFile.getAbsolutePath()+" file id:"+tempVFile.getFileID()+" file name:"+file.getName());
                //Log.d("Jack", "Get absolute path failed");
            }
            PathIndicator.setPathIndicator(mPathContainer, tempVFile, mPathIndicatorClickListener);
        } else if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE && mIsBackEvent) {
            isLoadingSambaFolder = true;
        }
        else {
            PathIndicator.setPathIndicator(mPathContainer, file, mPathIndicatorClickListener);
        }

        // remote storage don't support search action
       if (!(mIsAttachOp || mIsMultipleSelectionOp)) {
            //getActivity().getActionBar().setDisplayShowCustomEnabled(file.getVFieType() != VFileType.TYPE_REMOTE_STORAGE);
        }

        // remote storage case
        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
            // update action bar
            getActivity().invalidateOptionsMenu();
            // local device has no remote file list, we should query
            RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(getActivity());
            if (remoteFileUtility.getRemoteFileList() == null) {
                remoteFileUtility.mRemoteUpdateUI.add(new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                remoteFileUtility.sendRemoteMessage(file, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST);
                if (!isListRefreshing())
                    setListShown(false);
                //setNeedRefreshToken(false);
                return;
            }
            if (remoteFileUtility.isRemoteLoadingError()) {
                updateNofileLayout(NETWORK_INVALID);
            }
        } else if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            // update action bar
            getActivity().invalidateOptionsMenu();
            // local device has no remote file list, we should query
            //if (RemoteFileUtility.getRemoteFileList() == null) {
          if (RemoteFileUtility.getInstance(getActivity()).mRemoteFileListMap.get(file.getAbsolutePath()) == null) {

                int type = ((RemoteVFile)file).getStorageType();
                Log.d(TAG, "startScanFile to guery cloud storage type: " + type);
                int msgType = -1;
                switch(type) {
                case StorageType.TYPE_GOOGLE_DRIVE:
                    msgType = MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                    break;
                case StorageType.TYPE_DROPBOX:
                    msgType = MsgObj.TYPE_DROPBOX_STORAGE;
                    break;
                case StorageType.TYPE_BAIDUPCS:
                    msgType = MsgObj.TYPE_BAIDUPCS_STORAGE;
                    break;
                case StorageType.TYPE_SKYDRIVE:
                    msgType = MsgObj.TYPE_SKYDRIVE_STORAGE;
                    break;
                case StorageType.TYPE_ASUSWEBSTORAGE:
                    msgType = MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
                    break;
                case StorageType.TYPE_HOME_CLOUD:
                    msgType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                    break;
                case StorageType.TYPE_YANDEX:
                    msgType = 9;
                    break;
                default:
                    Log.w(TAG, "you should define remote storage type");
                }

                if (mIsBackEvent) {
                    mIsBackEvent = false;
                    RemoteVFile tempVFile = ((RemoteVFile)file).getParentFile();
                   // tempVFile.setFileID(RemoteVFile.PathIdMap.get(tempVFile.getAbsolutePath()));
                    tempVFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempVFile.getAbsolutePath()));
                   // RemoteFileUtility.mRemoteUpdateUI.add(new RemoteDataEntry(tempVFile, RemoteUIAction.FILE_LIST_UI));
                   // String key = tempVFile.getAbsolutePath();
                    String key = tempVFile.getAbsolutePath();
                    key = key.equals("")?File.separator+tempVFile.getStorageName():key;
                    key =  getmRemoteUpdateUIMapKey(key);
                  ///RemoteFileUtility.mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                  RemoteFileUtility.getInstance(getActivity()).mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry(tempVFile, RemoteUIAction.FILE_LIST_UI));

                   //RemoteFileUtility.getInstance(getActivity()).mRemoteUpdateUIMap.put(msgType+key,new RemoteDataEntry(tempVFile, RemoteUIAction.FILE_LIST_UI));
                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(((RemoteVFile) tempVFile).getStorageName(),
                            tempVFile, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST, ListArgument.FolderList);
                } else {
                   // RemoteFileUtility.getInstance(getActivity()).mRemoteUpdateUI.add(new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                    RemoteVFile tempRemoteVfile = (RemoteVFile)file;
                     String key = tempRemoteVfile.getAbsolutePath();
                      key = key.equals("")?File.separator+tempRemoteVfile.getStorageName():key;
                      key =  getmRemoteUpdateUIMapKey(key);
                    RemoteFileUtility.getInstance(getActivity()).mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                    int action = RemoteFileUtility.getInstance(getActivity()).getListUIAction();
                    String remoteAction = ListArgument.FolderList;
                    if (action != -1) {
                        switch(action) {
                        case StorageType.TYPE_STARRED_TITLE:
                            remoteAction = ListArgument.StartedList;
                            break;
                        case StorageType.TYPE_RECENTLY_USED_TITLE:
                            remoteAction = ListArgument.Recent;
                            break;
                        case StorageType.TYPE_TRASH_CAN_TITLE:
                            remoteAction = ListArgument.TrashCan;
                            break;
                        }
                    }

                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(((RemoteVFile) file).getStorageName(),
                            file, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST, remoteAction);
                }

                // get cloud storage usage
                String accountName = ((RemoteVFile)file).getStorageName();
               // RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(accountName, null, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE);

                if (!isListRefreshing())
                    setListShown(false);
                //setNeedRefreshToken(false);
                return;
            }
            if (RemoteFileUtility.getInstance(getActivity()).isHomeCloudFileListError()) {
                updateNofileLayout(HOMECLOUD_ACCESS_EEROR);
            } else if (RemoteFileUtility.getInstance(getActivity()).isRemoteLoadingError()) {
                if (RemoteFileUtility.getInstance(getActivity()).isRemoteAccessPermisionDeny()) {
                    updateNofileLayout(PERMISION_DENY);
                } else {
                    updateNofileLayout(NETWORK_INVALID);
                }
            }
        } else if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {

        }


        //if (DEBUG) {
        //    Log.d(TAG, "startScanFile : " + file.getAbsolutePath());
        //}
        /****************begin**add for store Back Press Operation*****************/
        if (!ItemOperationUtility.getInstance().isFromBackPress() && !isSamePath) {
            ItemOperationUtility.getInstance().saveOperationPath(mIndicatorFile);
        }

        if (ItemOperationUtility.getInstance().isFromBackPress()) {
            ItemOperationUtility.getInstance().backKeyPressed(false);
        }
        /****************end**add for store Back Press Operation*****************/

        VFile currentFile = mIndicatorFile;
        mIndicatorFile = file;
        mIsComFromSearch = false;

        // set default file list layout
        updateNofileLayout(NO_FILE);

        // update cloud storage file id
        if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            ((RemoteVFile)mIndicatorFile).setFileID(((RemoteVFile)file).getFileID());
        } else if (mIndicatorFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {

        }

        if (!currentFile.getAbsoluteFile().equals(mIndicatorFile.getAbsoluteFile())) {
//            if (!isInSecondLayerEditMode()) {
//                onDeselectAll();
//            }

//           //Folder Tree +++
//            ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//            if (shortcutFragment != null) {
//                shortcutFragment.notifyIndicatorPathChange(mIndicatorFile, scrollToSelect);
//                Log.i("!!!!!!!!!!!!!!mIndicatorFile.getAbsolutePath!!!!!!!!!!!!!",mIndicatorFile.getAbsolutePath());
//                Log.i("!!!!!!!!!!!!!!currentFile.getAbsolutePath!!!!!!!!!!!!!",currentFile.getAbsolutePath());
//            }
//            //Folder Tree ---
        }

        PathIndicator.setPathIndicator(mPathContainer, file, mPathIndicatorClickListener);
        //((FileManagerActivity)getActivity()).updateSpinner(file);
        ((FileManagerActivity)getActivity()).updateActionBarTitle(file);

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });

        registerObserver(mIndicatorFile.getAbsolutePath());

        // when in mount or root folder, we will try to hide the add-folder
        // button
        if (isRootOrMountDir(mIndicatorFile.getAbsolutePath()) || mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getInstance(getActivity()).getRootScanPath())
                || mIndicatorFile.getAbsolutePath().equals(RemoteFileUtility.getInstance(getActivity()).getHomeCloudRootPath())) {

            getActivity().invalidateOptionsMenu();
            mActivity.isRootDir(true);
        } else {
            mActivity.isRootDir(false);
            getActivity().invalidateOptionsMenu();
        }
        if (!mIsAttachOp && !mIsMultipleSelectionOp && !file.getAbsolutePath().equals(File.separator)) {
            mUsefulPathFile = file;
             RecentFileUtil.printData(getActivity());
        }
        Bundle args = new Bundle();
        args.putString(KEY_SCAN_PATH, file.getAbsolutePath());
        args.putInt(KEY_SCAN_TYPE, scanType);
        args.putInt(KEY_SORT_TYPE, mSortType);
        args.putInt(KEY_VFILE_TYPE, file.getVFieType());
        args.putBoolean(KEY_HIDDEN_TYPE, sShowHidden);
        args.putStringArray(KEY_FILE_FILTER, mFileFilter);

        //keep checked items
        //if (isInEditMode() && mSortType != getCurrentUsingAdapter().mSortType) {
        if (isInEditMode()) {
            mCheckPool.clear();
            mCheckPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
            args.putSerializable(KEY_CHECK_POOL, mCheckPool);
        }
                ShortCutFragment.currentTokenFile = null;

        LoaderManager manager = getLoaderManager();
        Loader<VFile[]> mFileLoader = manager.getLoader(SCAN_FILE_LOADER);

        Log.d(TAG,"===mFileLoader===" + mFileLoader + "===is same path =" + isSamePath);

        if (mFileLoader == null) {
            manager.initLoader(SCAN_FILE_LOADER, args, this);
        } else {
            if (!isSamePath) {
                mFileLoader.startLoading();
                manager.restartLoader(SCAN_FILE_LOADER, args, this);
            }

        }

        if (isListShown()) {
            setListShown(false);
        }

        closePopup();

        setViewSwitcherVisibility(View.VISIBLE);

//        if (file.getVFieType() != VFileType.TYPE_SAMBA_STORAGE) {
//            SambaFileUtility.stopOnlinePlayServer();
//        }
//        getLoaderManager().destroyLoader(SCAN_FILE_LOADER);
//        getLoaderManager().restartLoader(SCAN_FILE_LOADER, args, this);


        //setNeedRefreshToken(false);
    }
    private String getmRemoteUpdateUIMapKey(String key) {
        return key.endsWith("/")?key.substring(0,key.length()-1):key;
    }
    public void closePopup() {
        getCurrentUsingAdapter().clearCacheTag();
    }

    public void reScanFile() {
        reScanFile(mIndicatorFile);
    }

    private void reScanFile(VFile file) {
        reScanFile(file, false);
    }

    private void reScanFile(VFile file, boolean researchAfterComplete) {
        if (DEBUG) {
            Log.d(TAG, "file change and reScanFile : " + file.getAbsolutePath());
        }

        if (isInCategory()) {
            startScanCategory(mIndicatorFile, researchAfterComplete);
            return;
        }

        if (file.getAbsoluteFile().equals(mIndicatorFile.getAbsoluteFile())) {
            //((FileManagerActivity)getActivity()).updateActionBarTitle(file);
            Message message = mHandler.obtainMessage();
            message.obj = file;
            message.what = MSG_FILE_OBSERVER_UPDATE_DRAWER;
            mHandler.sendMessage(message);
            Bundle args = new Bundle();
            args.putString(KEY_SCAN_PATH, file.getAbsolutePath());
            args.putInt(KEY_SCAN_TYPE, ScanType.SCAN_SIBLING);
            args.putInt(KEY_SORT_TYPE, mSortType);
            args.putInt(KEY_VFILE_TYPE, file.getVFieType());
            args.putBoolean(KEY_HIDDEN_TYPE, sShowHidden);
            args.putStringArray(KEY_FILE_FILTER, mFileFilter);
            if (isInEditMode() && sIsDeleteComplete) {
                mCheckPool.clear();
                mCheckPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
                args.putSerializable(KEY_CHECK_POOL, mCheckPool);
            }
            if (getLoaderManager().getLoader(RESCAN_FILE_LOADER) == null) {
                getLoaderManager().initLoader(RESCAN_FILE_LOADER, args, this);
            } else {
                getLoaderManager().restartLoader(RESCAN_FILE_LOADER, args, this);
            }
//            getLoaderManager().destroyLoader(RESCAN_FILE_LOADER);
//            getLoaderManager().restartLoader(RESCAN_FILE_LOADER, args, this);

            if (isListShown()) {
                setListShown(false);
            }
        }
    }

    public VFile getIndicatorFile() {
        return mIndicatorFile;
    }

    @Override
    public Loader<VFile[]> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "ScanFileLoader onCreateLoader");
        String scanPath = args.getString(KEY_SCAN_PATH);
        int scanType = args.getInt(KEY_SCAN_TYPE);
        int sortType = args.getInt(KEY_SORT_TYPE);
        int vfileType = args.getInt(KEY_VFILE_TYPE);
        boolean showHidden = args.getBoolean(KEY_HIDDEN_TYPE);
        EditPool checkPool = (EditPool) args.getSerializable(KEY_CHECK_POOL);

        String[] filter = args.getStringArray(KEY_FILE_FILTER);
        return new ScanFileLoader(getActivity(), scanPath, scanType, sortType, vfileType, showHidden, checkPool, filter);
    }

    @Override
    public void onLoadFinished(Loader<VFile[]> loader, VFile[] data) {
        Log.d(TAG, "ScanFileLoader onLoadFinished");

        LoaderManager mManager = getLoaderManager();
        int id = loader.getId();
        mManager.destroyLoader(id);

//        if (mManager.getLoader(SCAN_FILE_LOADER) != null) {
//            mManager.destroyLoader(SCAN_FILE_LOADER);
//        } else if (mManager.getLoader(RESCAN_FILE_LOADER) != null) {
//            mManager.destroyLoader(RESCAN_FILE_LOADER);
//        }

        if (isLoadingSambaFolder) {
            isLoadingSambaFolder = false;
            SambaLoadingComplete();
        }

        if (isRootFolder(mIndicatorFile) && data != null) {
          ArrayList<VFile> dataList = new ArrayList<VFile>();

          for(VFile vfile: data) {
               if (!vfile.getName().equals("APD") || !vfile.isDirectory()) {
                       dataList.add(vfile);
               }
        }
          data = dataList.toArray(new VFile[dataList.size()]);
        }

        if (mIndicatorFile.getPath().equals("/Removable") && data != null) {
            ArrayList<VFile> dataList = new ArrayList<VFile>();
            for(VFile vfile: data) {
                if (!vfile.getName().equals("sdcard0") && !vfile.getName().equals("emulated")  && isExternalStorageMounted(vfile.getName()) ) {
                    dataList.add(vfile);
                }
            }
            data = dataList.toArray(new VFile[dataList.size()]);
        }

        ArrayList<VFile> dataList = new ArrayList<VFile>();
        if (data != null) {
            String[] favoriteFilePaths = ProviderUtility.FavoriteFiles.getPaths(mActivity.getContentResolver());
            if(favoriteFilePaths != null){
                Arrays.sort(favoriteFilePaths);
                data = setFileInfo(data, favoriteFilePaths);
            }
            for (VFile vfile : data) {
                File[] childFiles = vfile.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return (sShowHidden || !file.isHidden());
                    }
                });
                if (childFiles != null) {
                    vfile.setChildCount(childFiles != null ? childFiles.length : 0);
                } else {
                    vfile.setChildCount(0);
                }
                dataList.add(vfile);
            }
            data = dataList.toArray(new VFile[dataList.size()]);
        }
        //++Felix_Zhang
//        if (isBlockedFolder(mIndicatorFile)) {
//             Log.i("!!!!!!!!!!!!onLoadFinished!!!!!!!!!!!!",mIndicatorFile.getPath());
//                ((FileManagerActivity)getActivity()).updateSpinner(mIndicatorFile);
//        }
        //
        if (getListAdapter() != null && getListAdapter() instanceof DeviceListAdapter) {
            setFileListAdapter();// add for samba
        }

        //setListShown(true);
        getCurrentUsingAdapter().updateAdapter(data, false, mSortType, new BasicFileListAdapter.AdapterUpdateObserver() {
            @Override
            public void updateAdapterDone() {
                updateAdapterResult();
            }
        });
//        setListShownNoAnimation(true);
        //getCurrentUsingAdapter().updateAdapterResult();

        /*if (ItemOperationUtility.getNeedGoBack()) {
            ItemOperationUtility.showLastView(mListView);
        }

        updateEditMode();
        mPathHome.setEnabled(!isRootFolder(mIndicatorFile));
        hideRefresh();

        if (isVisible()) {
            getListView().requestFocus();
        }
        else if (getActivity() instanceof FileManagerActivity && ((FileManagerActivity)getActivity()).isSeachViewIsShow()) {
            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.clearSearchViewFocus();
        }*/
    }

    private boolean isExternalStorageMounted(String storageName) {
        boolean mounted = false;
        final StorageManager mStorageManager = (StorageManager) mActivity.getSystemService(
                Context.STORAGE_SERVICE);
        FileManagerApplication application = (FileManagerApplication) FileManagerApplication.getAppContext();
        ArrayList<Object> mVolumeList = application.getStorageVolume();
        for(Object volume : mVolumeList) {
            String folderPath = reflectionApis.volume_getPath(volume);
            if (!folderPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath()) && reflectionApis.getVolumeState(mStorageManager, volume).equals(Environment.MEDIA_MOUNTED)) {
                if (folderPath.contains(storageName)) {
                    mounted = true;
                    Log.d(TAG, "Mounted storage: " + storageName);
                    break;
                }
            }

        }

        return mounted;
    }

    //when onLoadFinished
    private void onLoadFinishedComplete() {
        AbsListView aView = null;
        try {
            aView = getShowView();
        }catch (Throwable ignore){

        }
        if (this.isDetached() || null == aView)
            return;
        setListShown(true);
        if (ItemOperationUtility.getInstance().getNeedGoBack()) {
            ItemOperationUtility.getInstance().showLastView(aView);
        }

        updateEditMode();
//        mPathHome.setEnabled(!isRootFolder(mIndicatorFile));
        mPathHome.setEnabled(true);
        hideRefresh();

        if (isVisible()) {
            getShowView().requestFocus();
        }
        else if (getActivity() instanceof FileManagerActivity && ((FileManagerActivity)getActivity()).isSeachViewIsShow()) {
            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.clearSearchViewFocus();
        }
    }

    private void updateAdapterResult() {
        Thread scanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                getCurrentUsingAdapter().updateAdapterResult();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_LOAD_FINISHED_COMPLETE));
            }

        });
        scanThread.start();
    }

    private void updateDrmPaths() {
        if (ConstantsUtil.IS_AT_AND_T) {
            Thread scanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        sDrmPaths = MediaProviderAsyncHelper.getPathsContainExtension(ItemOperationUtility.DRM_ITEM);
                    } catch (Exception e) {
                        Log.w(TAG, "getPathsContainExtension error");
                        e.printStackTrace();
                    }
                }

            });
            scanThread.start();
        }
    }

    @Override
    public void onLoaderReset(Loader<VFile[]> loader) {
        Log.d(TAG, "ScanFileLoader onLoaderReset");
    }

    public void showDialog(int type, Object arg) {
        if (mShowDialogListener != null)
            mShowDialogListener.displayDialog(type, arg);
    }

    // File Observer +++
    private MyFileObserver mObserver;

    private class MyFileObserver extends FileObserver {

        public static final int mask = FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_TO | FileObserver.MOVED_FROM;
        private String mWatchPath;

        public MyFileObserver(String path) {
            super(path, mask);
            mWatchPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            int mask = event & 0x0000ffff;

            boolean re_Scan = false;
            switch (mask) {
                case FileObserver.MODIFY:
                    break;
                case FileObserver.MOVED_FROM:
                    re_Scan = true;
                    break;
                case FileObserver.MOVED_TO:
                    re_Scan = true;
                    break;
                case FileObserver.CREATE:
                    re_Scan = true;
                    break;
                case FileObserver.DELETE:
                    re_Scan = true;
                    break;
                case FileObserver.ACCESS:
                case FileObserver.ATTRIB:
                case FileObserver.CLOSE_WRITE:
                case FileObserver.CLOSE_NOWRITE:
                case FileObserver.DELETE_SELF:
                case FileObserver.MOVE_SELF:
                    break;
                default:
                    break;
            }
            if (path!=null && path.startsWith("FileManager") && path.endsWith("tmp"))
                return;

            Log.d(TAG,"mWatchPath:"+mWatchPath+"+sEditIsProcessing:"+!EditorUtility.sEditIsProcessing);
            if (re_Scan && !EditorUtility.sEditIsProcessing) {
                Log.d(TAG,"re_scan:fileobserver:sEditIsProcessing:"+!EditorUtility.sEditIsProcessing);
                reScanFile(new LocalVFile(mWatchPath));
            }
        }
    }

    public void registerObserver(String path) {
        if (mObserver != null) {
            if (mObserver.mWatchPath.equals(path))
                return;
            mObserver.stopWatching();
            if (!isInCategory()) {
            mObserver = new MyFileObserver(path);
            mObserver.startWatching();
            }
        } else {
            if (!isInCategory()) {
            mObserver = new MyFileObserver(path);
            mObserver.startWatching();
            }
        }
        if (DEBUG)
            Log.d(TAG, "register FileObserver");
    }

    public void unregisterObserver() {
        if (DEBUG)
            Log.d(TAG, "unregister FileObserver");
        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver = null;
        }
    }

    // File Observer ---
    public static final int MSG_FILE_OBSERVER_UPDATE_DRAWER = 10001;
    // Delete & Paste Handler
    public static final int MSG_DELET_COMPLETE = 0;
    public static final int MSG_PASTE_COMPLETE = 1;
    public static final int MSG_PASTE_INIT = 2;
    public static final int MSG_PASTE_PROG_FILE = 3;
    public static final int MSG_PASTE_PROG_SIZE = 4;
    public static final int MSG_PASTE_PAUSE = 5;
    public static final int MSG_COPY_LOCAL_TO_REMOTE = 6;
    public static final int MSG_COPY_REMOTE_TO_LOCAL = 7;
    public static final int MSG_CONNECTING_REMOTE_DIALOG = 8;
    public static final int MSG_PASTE_REMOTE_PROG_SIZE = 9;
    public static final int MSG_COPY_REMOTE_TO_REMOTE = 10;
    public static final int MSG_PASTE_CLOUD_PROG_SIZE = 111;
    public static final int MSG_PASTE_CLOUD_COPY_STATUS = 112;
    public static final int MSG_COPY_REMOTE_FAIL_CLOSE_DIALOG = 113;
    public static final int MSG_REMOTE_PRIVIEW_PROG_SIZE = 114;
    public static final int MSG_RESTORE_COMPLETE = 115;
    public static final int MSG_RENAME_COMPLETE = 116;

    public static final int MSG_LOAD_FINISHED_COMPLETE = 200;

    //++felix_zhang
    public static final int MSG_COPY_REMOTE_TO_OTHER_REMOTE = 11;
    public static final int MSG_COPY_REMOTE_TO_OTHER_DEVICE = 12;
    public static final int MSG_COPY_DEVICE_TO_OTHER_REMOTE = 13;
    public static final int MSG_COPY_DEVICE_TO_OTHER_DEVICE = 14;
    public static final int MSG_COPY_SAMB_TO_CLOUD = 15;
    public static final int MSG_COPY_CLOUD_TO_SAMB = 16;



    public static final int MSG_APP_REQUEST_COPY_FILE_DEVICE_TO_OTHER_DEVICE = 1115;

    public static final int MSG_APP_REQUEST_COPY_FILE_SAMB_TO__CLOUD = 1116;
    public static final int MSG_APP_REQUEST_COPY_FILE_CLOUD_TO_SAMB = 1117;

    private static final int MSG_LOAD_FINISHED_CATEGORY = 20;

    private Handler mHandler = new Handler() {
        private double mTotalSize;
        private double mCountSize;
        public int mPercent;

        // for remote storage copy action, we should save source files, target
        private RemoteActionEntry remoteActionEntry;
        private VFile[] vfiles;
        private VFile pasteVFile;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_FINISHED_COMPLETE:
                    Log.d(TAG, "handleMessage: MSG_LOAD_FINISHED_COMPLETE");
                    onLoadFinishedComplete();
                    break;
                case MSG_DELET_COMPLETE: {
                    Log.d(TAG, "handleMessage: MSG_DELET_COMPLETE");
                    boolean deleteSuccess = (1 == msg.arg1);
                    VFile[] files = (VFile[]) msg.obj;
                    if(mActivity.isFromStorageAnalyzer)
                    {
                        Log.i(TAG,"Start UpdateDeleteFilesTask from FileList");
                        new UpdateDeleteFilesTask(mActivity,files,null,UpdateDeleteFilesTask.MODE_UPDATE_FILES_IN_DIFFERENT_FOLDER).execute();
                    }

                    if (deleteSuccess && files != null) {
                        deleteFromRecentlyOpen(files);
                    }

                    EditorUtility.sEditIsProcessing = false;
                    //++felix_zhang research files
                    if (getActivity() == null)
                        return;
                    deleteComplete();
                    SearchResultFragment mSFragment = (SearchResultFragment) mActivity.getFragmentManager().findFragmentById(R.id.searchlist);
                    if (mSFragment != null) {
                         mSFragment.deleteComplete(false);
                    }
                    rescanFile();
                    //for storage analyzer
                    getActivity().setResult(Activity.RESULT_OK);
                } break;
                case MSG_PASTE_COMPLETE: {
                    Log.d(TAG, "handleMessage: MSG_PASTE_COMPLETE");
                    boolean pasteSuccess = (1 == msg.arg1);
                    WorkerArgs args = (WorkerArgs) msg.obj;
                    if (pasteSuccess && args.files != null && args.isDelete) {
                        for (VFile file: args.files) {
                            ProviderUtility.RecentlyOpen.rename(mActivity.getContentResolver(),
                                file.getPath(), file.getName(), FilenameUtils.concat(args.pastePath, file.getName())
                            );
                        }
                    }
                    if (getFragmentManager() != null) {
                       reScanFile(mIndicatorFile);
                       pasteComplete();
                       ItemOperationUtility.cancelNotification(getActivity());
                    }
                } break;
                case MSG_PASTE_INIT:
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
                case MSG_PASTE_PROG_FILE:
                    EditResult r = (EditResult) msg.obj;
                    mCountSize += r.numbytes;
                    setProgress(mTotalSize == 0 ? 0 : (int) ((mCountSize) * 100 / mTotalSize), mCountSize);
                    break;
                case MSG_PASTE_PROG_SIZE:
                    EditResult editResult = (EditResult) msg.obj;
                    //calculate when files changed
                    if (mTotalSize<(mCountSize + editResult.numbytes) && mEditPool.getFiles()!=null && ((mEditPool.getFiles())[0]).getVFieType()!=VFileType.TYPE_CLOUD_STORAGE) {
                        mTotalSize = FileUtility.getArrayTotalLength(mEditPool.getFiles());
                    }
                    setProgress(mTotalSize == 0 ? 0 : (int) ((mCountSize + editResult.numbytes) * 100 / mTotalSize), mCountSize + editResult.numbytes);
                    ItemOperationUtility.updateNotificationBar(editResult.mCurrentFileName,(int)(mTotalSize/1024),(int)(mCountSize + editResult.numbytes)/1024,getActivity());
                    break;
                case MSG_PASTE_PAUSE:
                    showDialog(DialogType.TYPE_FILE_EXIST_DIALOG, (ExistPair) msg.obj);
                    break;
                case MSG_COPY_REMOTE_FAIL_CLOSE_DIALOG:
                    if (FileManagerActivity.currentPasteDialogType == DialogType.TYPE_REMOTE_PASTE_DIALOG) {
                        //EditorUtility.sEditIsProcessing = false;
                        pasteCloudComplete();
                    } else if (FileManagerActivity.currentPasteDialogType == DialogType.TYPE_PASTE_DIALOG) {
                        //EditorUtility.sEditIsProcessing = false;
                        pasteComplete();
                    }

                    break;
                // +++ remote action case
                case MSG_COPY_LOCAL_TO_REMOTE:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    boolean isAlreadyExpanded = remoteActionEntry.containsRetrictFiles();
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (pasteVFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        int msgObjType = ((RemoteVFile)pasteVFile).getMsgObjType();
                        String action = remoteActionEntry.action;
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) pasteVFile).getStorageName(),
                                vfiles, ((RemoteVFile) pasteVFile), msgObjType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE, action, isAlreadyExpanded);
                    } else {
                        RemoteFileUtility.getInstance(getActivity()).sendRemoteCopyMessage(vfiles, pasteVFile.getAbsolutePath(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE);
                    }
                    break;

                case MSG_COPY_REMOTE_TO_LOCAL:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (vfiles[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        int msgObjType = -1;
                        if (vfiles != null && vfiles.length > 0) {
                            msgObjType = ((RemoteVFile)vfiles[0]).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_REMOTE_TO_LOCAL but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("Johnson", "MSG_COPY_REMOTE_TO_LOCAL action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) vfiles[0]).getStorageName(),
                                vfiles, pasteVFile, msgObjType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, action, false);
                    } else {
                        RemoteFileUtility.getInstance(getActivity()).sendRemoteCopyMessage(vfiles, pasteVFile.getAbsolutePath(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE);
                    }
                    break;
                case MSG_CONNECTING_REMOTE_DIALOG:
                    VFile file = (VFile) msg.obj;
                    showDialog(DialogType.TYPE_CONNECTING_REMOTE_DIALOG, file);
                    break;
                case MSG_PASTE_REMOTE_PROG_SIZE:
                    EditResult remoteResult = (EditResult) msg.obj;
                    int percent = (remoteResult.copyTotalSize == 0 ? 0 : (int) (remoteResult.copySize * 100 / remoteResult.copyTotalSize));
                    setProgress(percent,remoteResult.copySize,remoteResult.copyTotalSize);
                    break;
                case MSG_REMOTE_PRIVIEW_PROG_SIZE:
                    EditResult preViewResult = (EditResult) msg.obj;
                    int per= (preViewResult.copyTotalSize == 0 ? 0 : (int) (preViewResult.copySize * 100 / preViewResult.copyTotalSize));
                    setPreViewProgress(per,preViewResult.copySize,preViewResult.copyTotalSize);
                    break;
                case MSG_PASTE_CLOUD_PROG_SIZE:
                    EditResult cloudResult = (EditResult) msg.obj;
                    //calculate when files changed
                    if (cloudResult.currentFileTotalSize<cloudResult.currentFileCopySize && mEditPool.getFiles()!=null && ((mEditPool.getFiles())[0]).getVFieType()!=VFileType.TYPE_CLOUD_STORAGE && cloudResult.fileCurrentCount<mEditPool.getFiles().length) {
                        cloudResult.currentFileTotalSize = FileUtility.getInfo(mEditPool.getFiles()[cloudResult.fileCurrentCount]).numSize;
                    }
                    setCloudProgress(cloudResult.fileCurrentCount, cloudResult.fileTotalCount, cloudResult.currentFileCopySize, cloudResult.currentFileTotalSize);
                    break;
                case MSG_PASTE_CLOUD_COPY_STATUS:
                    EditResult statusResult = (EditResult) msg.obj;
                    setCloudCopyStatus(statusResult.copyStatus);
                    break;

                case MSG_COPY_REMOTE_TO_REMOTE:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (vfiles[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                        int msgObjType = -1;
                        if (vfiles != null && vfiles.length > 0) {
                            msgObjType = ((RemoteVFile)vfiles[0]).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_REMOTE_TO_REMOTE but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("Johnson", "MSG_COPY_REMOTE_TO_REMOTE action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) vfiles[0]).getStorageName(),
                                vfiles, pasteVFile, msgObjType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_UPDATE_REMOTE, action, false);
                    }
                    break;
                case MSG_COPY_DEVICE_TO_OTHER_DEVICE:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (vfiles[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                        int msgObjType = -1;
                        if (vfiles != null && vfiles.length > 0) {
                            msgObjType = ((RemoteVFile)vfiles[0]).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_REMOTE_TO_REMOTE but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("Johnson", "MSG_COPY_REMOTE_TO_REMOTE action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) vfiles[0]).getStorageName(),
                                vfiles, pasteVFile, msgObjType, MSG_APP_REQUEST_COPY_FILE_DEVICE_TO_OTHER_DEVICE, action, false);
                    }
                    break;
                    //++felix_zhang copy file from remote to other remote
                case MSG_COPY_CLOUD_TO_SAMB:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (vfiles[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        int msgObjType = -1;
                        if (vfiles != null && vfiles.length > 0) {
                            msgObjType = ((RemoteVFile)vfiles[0]).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_CLOUD_TO_SAMB but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("felix_zhang", "MSG_COPY_CLOUD_TO_SAMB action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) vfiles[0]).getStorageName(),
                                vfiles, pasteVFile, msgObjType, MSG_APP_REQUEST_COPY_FILE_CLOUD_TO_SAMB, action, false);
                    }
                break;
                case MSG_COPY_SAMB_TO_CLOUD:
                       remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (pasteVFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        int msgObjType = -1;
                        if (pasteVFile != null ) {
                            msgObjType = ((RemoteVFile)pasteVFile).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_REMOTE_TO_REMOTE but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("felix_zhang", "MSG_COPY_SAMB_TO_CLOUD action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) pasteVFile).getStorageName(),
                                vfiles, pasteVFile, msgObjType, MSG_APP_REQUEST_COPY_FILE_SAMB_TO__CLOUD, action, false);
                    }
                break;
                case MSG_COPY_REMOTE_TO_OTHER_REMOTE:
                    remoteActionEntry = (RemoteActionEntry) msg.obj;
                    vfiles = remoteActionEntry.files;
                    pasteVFile = remoteActionEntry.pasteVFile;
                    if (vfiles[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        int msgObjType = -1;
                        if (vfiles != null && vfiles.length > 0) {
                            msgObjType = ((RemoteVFile)vfiles[0]).getMsgObjType();
                        } else {
                            Log.w(TAG, "send MSG_COPY_REMOTE_TO_REMOTE but cannot get msgobj type");
                        }
                        String action = remoteActionEntry.action;
                        Log.v("felix_zhang", "MSG_COPY_REMOTE_TO_OTHER_REMOTE action: " + action);
                        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(((RemoteVFile) vfiles[0]).getStorageName(),
                                vfiles, pasteVFile, msgObjType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_REMOTE_OTHER_REMOTE, action, false);
                    }
                 break;
                case MSG_FILE_OBSERVER_UPDATE_DRAWER:
                    VFile reScanFile =  (VFile)msg.obj;
                    ((FileManagerActivity)getActivity()).updateActionBarTitle(reScanFile);
                    break;
                case MSG_LOAD_FINISHED_CATEGORY:
                    loadCategoryFinished((VFile[]) msg.obj);
                    if (msg.arg1 == MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED) {
                        ((FileManagerActivity) getActivity()).reSearch(mActivity.getSearchQueryKey());
                    }
                    break;
                // ---
                case HiddenZoneUtility.MSG_HIDE_COMPLETE: {
                    Log.d(TAG, "handleMessage: MSG_HIDE_COMPLETE");
                    boolean success = (1 == msg.arg1);
                    VFile[] files = (VFile[]) msg.obj;
                    if(mActivity.isFromStorageAnalyzer)
                    {
                        Log.i(TAG,"Start UpdateDeleteFilesTask from FileList");
                        new UpdateDeleteFilesTask(mActivity,files,null,UpdateDeleteFilesTask.MODE_UPDATE_FILES_IN_DIFFERENT_FOLDER).execute();
                    }

                    if (success && files != null) {
                        deleteFromRecentlyOpen(files);
                    }

                    EditorUtility.sEditIsProcessing = false;

                    AddToHiddenZoneDialogFragment.dismissFragment(getActivity());
                    onDeselectAll();
                    if (isVisible()) {
                        getActivity().invalidateOptionsMenu();
                    }
                    SearchResultFragment mSFragment = (SearchResultFragment) mActivity.getFragmentManager().findFragmentById(R.id.searchlist);
                    if (mSFragment != null) {
                        mSFragment.onDeselectAll();
                    }
                    rescanFile();
                    //for storage analyzer
                    getActivity().setResult(Activity.RESULT_OK);
                } break;
                default:
                    break;
            }
        }

        private void deleteFromRecentlyOpen(VFile[] files) {
            for (VFile file: files) {
                ProviderUtility.RecentlyOpen.delete(
                        mActivity.getContentResolver(),
                        file.getPath()
                );
            }
        }

        private void rescanFile() {
            if (belongToCategoryFromMediaStore()) {
                reScanFile(mIndicatorFile, mActivity.getIsShowSearchFragment());
            } else {
                if (mActivity.getIsShowSearchFragment()) {
                    ((FileManagerActivity)getActivity()).reSearch(mActivity.getSearchQueryKey());
                } else {
                    reScanFile(mIndicatorFile);
                }
            }
        }

        public void setProgress(int n, double countSize) {
            if (getFragmentManager() != null) {
                PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getFragmentManager()
                        .findFragmentByTag("PasteDialogFragment");
                // Fix TT-222081
                if (mPercent != n && pasteDialogFragment != null) {
                    mPercent = n;
                    pasteDialogFragment.setProgress(mPercent, countSize,
                            mTotalSize);
                }
            }
        }

        public void setProgress(int n, double countSize,double totalSize) {
            if (getFragmentManager() != null) {
                PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getFragmentManager()
                        .findFragmentByTag("PasteDialogFragment");

                // Fix TT-222081
                if (pasteDialogFragment != null) {
                    pasteDialogFragment.setProgress(n, countSize, totalSize);
                }
            }
        }

        public void setPreViewProgress(int n, double countSize,double totalSize) {
            if (getFragmentManager() != null) {
                PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getFragmentManager()
                        .findFragmentByTag(
                                PasteDialogFragment.PREVIEW_DIALOG_PROCESS);

                // Fix TT-222081
                if (pasteDialogFragment != null) {
                    pasteDialogFragment.setProgress(n, countSize, totalSize);
                }
            }
        }

        public void setCloudProgress(int currentFileCount,int totalFileCount, double currentFileSize , double currentFileTotalSize ) {
            if (getFragmentManager() != null) {
                RemoteFilePasteDialogFramgment pasteDialogFragment = (RemoteFilePasteDialogFramgment) getFragmentManager()
                        .findFragmentByTag("RemoteFilePasteDialogFramgment");

                // Fix TT-222081
                if (pasteDialogFragment != null) {
                    pasteDialogFragment.setProgress(currentFileCount,
                            totalFileCount, currentFileSize,
                            currentFileTotalSize);
                }
            }
        }

        public void setCloudCopyStatus(int status) {
            if (getFragmentManager() != null) {
                RemoteFilePasteDialogFramgment pasteDialogFragment = (RemoteFilePasteDialogFramgment) getFragmentManager()
                        .findFragmentByTag("RemoteFilePasteDialogFramgment");

                // Fix TT-222081
                if (pasteDialogFragment != null) {
                    pasteDialogFragment.setOperateStatus(status);
                }
            }
        }
    };

    public Handler getHandler() {
        return mHandler;
    }

    public void pasteComplete() {
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_PASTE_DIALOG);
        if (!ItemOperationUtility.isReadyToPaste) {
            mEditPool.clear();
            getActivity().invalidateOptionsMenu();
        }
        getCurrentUsingAdapter().notifyDataSetChanged();
        EditorUtility.sEditIsProcessing = false;
        mIsDraggingItems = false; // Johnson
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void pasteCloudComplete() {
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_REMOTE_PASTE_DIALOG);
        if (!ItemOperationUtility.isReadyToPaste) {
            mEditPool.clear();
            getActivity().invalidateOptionsMenu();
        }
//        mEditPool.clear();
        getCurrentUsingAdapter().notifyDataSetChanged();
//        getActivity().invalidateOptionsMenu();
         EditorUtility.sEditIsProcessing = false;
        mIsDraggingItems = false; // Johnson
    }
    public void pasteComplete(boolean isfromSamba) {
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_PASTE_DIALOG);
//        mEditPool.clear();
        EditorUtility.sEditIsProcessing = false;
//        getActivity().invalidateOptionsMenu();
        if (!ItemOperationUtility.isReadyToPaste) {
            mEditPool.clear();
            getActivity().invalidateOptionsMenu();
        }
    }

    public void deleteComplete() {
        if (DEBUG) {
            Log.i(TAG, "deletComplete");
        }
        if (mDeleteFilePool.getFile() != null && mDeleteFilePool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            updateEditPool();
        }
        sIsDeleteComplete = true;
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_DELETEPROGRESS_DIALOG);
        onDeselectAll();
        mDeleteFilePool.clear();
        if (isVisible()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void updateEditPool() {
        if (mEditPool.getFile() != null && mEditPool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            VFile[] files = mEditPool.getFiles();

            if (files != null && files.length > 0) {
                ArrayList<VFile> editList = new ArrayList<VFile>();

                for(VFile vfile : files) {
                    if (vfile != null && vfile.exists()) {
                        editList.add(vfile);
                    }
                }

                VFile[] editFiles = editList.toArray(new VFile[editList.size()]);
                mEditPool.setFiles(editFiles, false);
            }
        }
    }

    public void addComplete() {
        if (DEBUG) {
            Log.i(TAG, "addComplete");
        }
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS);
        getActivity().invalidateOptionsMenu();
    }

    public void CloudStorageLoadingComplete() {
        if (DEBUG) {
            Log.i(TAG, "LoggingComplete");
        }
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING);
    }

    public void SambaLoadingComplete() {
        if (DEBUG) {
            Log.i(TAG, "LoadingComplete");
        }
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_SAMBA_SORAGE_DIALOG);
    }

    public void RenamingComplete() {
        if (DEBUG) {
            Log.i(TAG, "RenamingComplete");
        }
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_RENAME_PROGRESS_DIALOG);
    }

    // +++ Johnson
    protected void isHiddenDate(boolean hidden) {
        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        if (adapter != null) {
            mIsHiddenDate = hidden;
            adapter.isHiddenDate(hidden);
            if (hidden) {
                dateContainer.setVisibility(View.INVISIBLE);
            } else {
                dateContainer.setVisibility(View.VISIBLE);
            }
        } else {
            if (DEBUG) {
                Log.w(TAG, "mAdapter is null when calling isHiddenDate()");
            }
        }
    }

    public boolean getHiddenDate() {
        return mIsHiddenDate;
    }

    public void setMovingDivider(boolean isMoving) {
        mIsMovingDivider = isMoving;
    }

    public boolean isMovingDivider() {
        return mIsMovingDivider;
    }

    public void onDropSelectedItems() {
        if (DEBUG) {
            Log.i(TAG, "onDropSelectedItems");
        }
        onDeselectAll();
        mEditPool.clear();
        getActivity().invalidateOptionsMenu();
    }

    public void dropForAction(VFile file, int action) {
        mIsDraggingItems = true;
        if (action == DialogType.TYPE_DROP_FOR_COPY) {
            // copy
            mEditPool.setExtraBoolean(false);
            onDeselectAll();
        } else if (action == DialogType.TYPE_DROP_FOR_CUT) {
            // cut
            mEditPool.setExtraBoolean(true);
            onDeselectAll();
        }
        // paste
        mEditPool.setPasteVFile(file);
        mEditPool.setPastePath(file.getAbsolutePath());
        mEditPool.setTargetDataType(file.getVFieType()); // for remote file copy action, we need to save the target file type
        showDialog(DialogType.TYPE_PASTE_DIALOG, mEditPool);
    }

    public boolean isDraggingItems() {
        return mIsDraggingItems;
    }

    public VFile[] getEditPoolFiles() {
        VFile [] files = null;
        if (mEditPool != null) {
            mEditPool.setFiles(getCurrentUsingAdapter().getFiles(), true);
            files = mEditPool.getFiles();
        } else {
            Log.d(TAG, "mEditPool is null when calling getEditPoolFiles");
        }
        return files;
    }
    // ---

    @Override
    public boolean onDrag(View v, DragEvent event) {
        boolean result = false;
        Message msg;
        final int action = event.getAction();
        if (v == null) {
            Log.d(TAG, "onDrag get view is null");
            return result;
        }
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED: {
                result = true;
            } break;
            case DragEvent.ACTION_DRAG_ENTERED: {
                switch (v.getId()) {
                case R.id.sort_container_root:
                    cancelPressedStateForDrag(true);
                    break;
                case R.id.list_bottom:
                    cancelPressedStateForDrag(true);
                    break;
                }
                result = true;
            } break;
            case DragEvent.ACTION_DRAG_LOCATION: {
                switch (v.getId()) {
                case android.R.id.list:
                    onDragLocation(event, false);
                    break;
                case R.id.sort_container_root:
                    msg = mScrollHandler.obtainMessage(UPDATE_LIST_VIEW, SCROLL_UP);
                    mScrollHandler.sendMessage(msg);
                    break;
                case R.id.list_bottom:
                    msg = mScrollHandler.obtainMessage(UPDATE_LIST_VIEW, SCROLL_DOWN);
                    mScrollHandler.sendMessage(msg);
                    break;
                }
                result = true;
            } break;
            case DragEvent.ACTION_DRAG_EXITED: {
                cancelPressedStateForDrag(true);
                mScrollHandler.removeMessages(UPDATE_LIST_VIEW);
                result = true;
            } break;
            case DragEvent.ACTION_DROP: {
                switch (v.getId()) {
                case R.id.sort_container_root:
                    break;
                case R.id.list_bottom:
                    break;
                default:
                    if (mDropTargetAdapterPosition >= 0 && mDropTargetAdapterPosition < getCurrentUsingAdapter().getCount()) {
                        getCurrentUsingAdapter().onDrop(mDropTargetAdapterPosition);
                    }
                    break;
                }
                cancelPressedStateForDrag(true);
                mDropTargetAdapterPosition = -1;
                mDropScreenAdapterPosition = -1;
                mScrollHandler.removeMessages(UPDATE_LIST_VIEW);
                result = true;
            } break;
            case DragEvent.ACTION_DRAG_ENDED: {
                cancelPressedStateForDrag(true);
                mDropTargetAdapterPosition = -1;
                mDropScreenAdapterPosition = -1;
                mScrollHandler.removeMessages(UPDATE_LIST_VIEW);
                result = true;
                final boolean dropped = event.getResult();
                if (dropped) {
                    // drop success
                }
            } break;
        }
        return result;
    }

    private Handler mScrollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_LIST_VIEW:
                    if (mListView != null) {
                        int action = (Integer)msg.obj;
                        Message message;
                        mListView.setSmoothScrollbarEnabled(true);
                        if (action == SCROLL_UP) {
                            mListView.smoothScrollBy(-DRAG_SCROLL_STEP, 0);
                            message = mScrollHandler.obtainMessage(UPDATE_LIST_VIEW, SCROLL_UP);
                            mScrollHandler.sendMessageDelayed(message, SCROLL_DELAY_TIME);
                        } else if (action == SCROLL_DOWN) {
                            mListView.smoothScrollBy(DRAG_SCROLL_STEP, 0);
                            message = mScrollHandler.obtainMessage(UPDATE_LIST_VIEW, SCROLL_DOWN);
                            mScrollHandler.sendMessageDelayed(message, SCROLL_DELAY_TIME);
                        }
                    } else {
                        Log.d(TAG, "mScrollHandler get null mList object");
                    }
                    break;
            }
        }
    };

    private void onDragLocation(DragEvent event, boolean isBottom) {
        mDragItemHeight = (int) this.getResources().getDimension(R.dimen.file_list_height);
        if (mDragItemHeight <= 0) {
            // This shouldn't be possible, but avoid NPE
            return;
        }
        // Find out which item we're in and highlight as appropriate
        int rawTouchY = (int)event.getY();

        //since we add the list bottom view, we should correct the touch coordinate
        if (isBottom) {
            rawTouchY += mListBottom.getY();
        }
        int offset = 0;
        if (mListView.getCount() > 0) {
            offset = mListView.getChildAt(0).getTop();
        }
        int targetScreenPosition = (rawTouchY - offset) / mDragItemHeight;
        int firstVisibleItem = mListView.getFirstVisiblePosition();
        int targetAdapterPosition = firstVisibleItem + targetScreenPosition;

        if (targetScreenPosition != mDropScreenAdapterPosition) {
            if (mDropScreenAdapterPosition != -1) {
                View v = mListView.getChildAt(mDropScreenAdapterPosition);
                if (v != null) {
                    v.setPressed(false);
                }
            }
            mDropScreenAdapterPosition = targetScreenPosition;
        }
        if (targetAdapterPosition != mDropTargetAdapterPosition) {
            mDropTargetAdapterPosition = targetAdapterPosition;
        }

        View listChildView = mListView.getChildAt(targetScreenPosition);
        VFile[] files = getCurrentUsingAdapter().getFiles();
        if (mDropTargetAdapterPosition >= getCurrentUsingAdapter().getCount()) {
            // invalid zone, don't do anything
            return;
        }
        if (listChildView != null && files != null && files[mDropTargetAdapterPosition] != null
                && files[mDropTargetAdapterPosition].isDirectory() && !files[mDropTargetAdapterPosition].getChecked()) {
            listChildView.findViewById(R.id.file_list_item_container).setPressed(true);
        }
    }

    private void cancelPressedStateForDrag(boolean cancelAll) {
        if (cancelAll) {
            for (int i=0 ; i<mListView.getCount() ; i++) {
                View v = mListView.getChildAt(i);
                if (v != null) {
                    v.setPressed(false);
                }
            }
        } else {
            View v = mListView.getChildAt(mDropScreenAdapterPosition);
            if (v != null && v.isPressed()) {
                v.setPressed(false);
            }
        }
    }

    // +++ remote storage function
    public void remoteUpdateThumbnail(VFile[] files) {
        Log.d(TAG, "remoteUpdateThumbnail");
        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        if (adapter != null) {
            adapter.updateAdapter(files, false, mSortType, null);
        } else {
            Log.w(TAG, "mAdapter is null when calling remoteUpdateThumbnail");
        }
    }

    public boolean isScrolling() {
        return mIsScrollingList;
    }

    public VFile[] getFileList() {
        VFile[] files = null;
        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        if (adapter != null) {
            files = adapter.getFiles();
        } else {
            Log.w(TAG, "mAdapter is null when calling remoteUpdateThumbnail");
        }
        return files;
    }

    public void setRemoteThumbnailList(VFile[] files) {
        RemoteFileUtility.getInstance(getActivity()).setRemoteThumbnailList(files);
    }

    public void backToDefaultPath() {
        if (mIndicatorFile != null) {
            if (mIndicatorFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE ||
                    PathIndicator.getIndicatorVFileType() != VFileType.TYPE_LOCAL_STORAGE) {
                startScanFile(new LocalVFile(DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
                mActivity.setCurrentActionBarTitle(getResources().getString(R.string.internal_storage_title));
            }
        } else {
            Log.w(TAG, "mIndicatorFile is null when calling backToDefaultPath");
        }
    }

    public VFile getIndicatorPath() {
        return mIndicatorFile;
    }

    /**
     * Update file list layout in the following states: no file and network is unavailable
     *
     * @param state NO_FILE and NETWORK_INVALID
     */
    public void updateNofileLayout(int state) {
        //Drawable drawable = getResources().getDrawable(R.drawable.asus_ep_ic_empty_folder);;
        //TextView view = (TextView)getView().findViewById(android.R.id.empty);

        switch (state) {
        case NO_FILE:
            //setEmptyText(getText(R.string.empty_folder_title));
            //drawable = getResources().getDrawable(R.drawable.asus_ep_ic_empty_folder);
            if (isCategoryFavorite()) {
                String text = getResources().getString(R.string.click_plus_to_add_favorite_folder);
                Drawable drawable = getResources().getDrawable(R.drawable.fab_in_text);
                int index = text.indexOf("+");
                setEmptyText(text, drawable, index);
            } else {
                setEmptyText(getResources().getString(R.string.fileList_Nofiles));
            }
            break;
        case NETWORK_INVALID:
            setEmptyText(getText(R.string.remote_connected_error_hint));
            //drawable = getResources().getDrawable(R.drawable.asus_ep_ic_network_unavailable);
            //view.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            break;
        case NO_DEVICES:
            setEmptyText(getText(R.string.cloud_homebox_no_available_devices));
            //drawable = getResources().getDrawable(R.drawable.asus_ep_ic_network_unavailable);
            //view.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            break;
        case TOKEN_INVALIDATED:
            setEmptyText(getText(R.string.cloud_token_invalidate));
            //drawable = getResources().getDrawable(R.drawable.asus_ep_ic_network_unavailable);
            //view.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            break;
        case PERMISION_DENY:
            setEmptyText(getText(R.string.permission_deny));
            break;

        case ACCOUNT_INVALID:
            setEmptyText(getText(R.string.invalid_account));
            break;
        case HOMECLOUD_ACCESS_EEROR:
            setEmptyText(getText(R.string.homecloud_access_error));
            break;
        }

        if (isCategoryUsingExpandableListView()) {
            View emptyView = mExpandableListView.getEmptyView();
            if (emptyView != null && emptyView instanceof TextView) {
                ((TextView) emptyView).setText(getResources().getString(R.string.fileList_Nofiles));
            }
        }
    }

    public void updateCloudStorageUsage(boolean isDisplay, long usedQuota, long totalQuota) {
        if (isDisplay) {
            if (mCloudStorageUsageView != null) {
                String usage = Formatter.formatFileSize(getActivity(), usedQuota);
                String total = Formatter.formatFileSize(getActivity(), totalQuota);
                mCloudStorageUsageView.setVisibility(View.VISIBLE);
                mCloudStorageUsageView.setText(usage + "/" + total);
            } else {
                mCloudStorageUsageView.setText("");
                mCloudStorageUsageView.setVisibility(View.GONE);
                Log.w(TAG, "cannot update cloud storage usage because the view is null");
            }
        } else {
            mCloudStorageUsageView.setText("");
            mCloudStorageUsageView.setVisibility(View.GONE);
        }

    }

    public void openCloudStorageFile(VFile file) {
        FileUtility.openFile(this.getActivity(), file, false, true, false);
    }

    public void shareCloudStorageFiles(VFile[] files) {
        FileUtility.shareFile(getActivity(), files, true);
        mEditPool.clear();
        onDeselectAll();
        getActivity().invalidateOptionsMenu();
    }
    // ---

    /***add for samba begin**********/
    public void SetScanHostIndicatorPath(String showName) {
        PathIndicator.setSambaHostPathIndicator(mPathContainer, showName);
    }

    public void ScanHostPcFinish(ArrayList<SambaItem> item) {
        mDeviceAdapter.updateSambaHostAdapter(item);
        modifyListAndGridVisibility();

    }

    public void setFileListAdapter() {
        if (mActivity != null) {
            mActivity.showSortContainerView(true);
            mActivity.updateActionMenuView();
        }
        if (mAdapter == null) {
            mAdapter = new FileListAdapter(this, null, mIsAttachOp);
        }
        mPathHome.setEnabled(true);
        SambaFileUtility.updateHostIp = false;
        RemoteFileUtility.isShowDevicesList = false;
        setListAdapter(mAdapter);
//        setOnItemClickAndLongClickListener();
        getCurrentUsingAdapter().setOrientation(getResources().getConfiguration().orientation);
        setViewSwitcherVisibility(View.VISIBLE);
    }

    public void setDeviceListAdapter() {
        boolean isListViewMode = ItemOperationUtility.getInstance().isListViewMode();
//        if (!isListViewMode)
//            ItemOperationUtility.getInstance().switchViewMode();

        mPathHome.setEnabled(true);
        if (mActivity != null) {
            mActivity.showSortContainerView(false);
            mActivity.updateActionMenuView();
        }
        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceListAdapter(this);
        }

        setListAdapter(mDeviceAdapter);
        showFAB(false);

        enablePullReFresh(true);
        ItemOperationUtility.getInstance().resetScrollPositionList();
        hideListView(false);
        setViewSwitcherVisibility(View.GONE);

    }

    public void setDeviceType(int type) {
        mDeviceAdapter.setDeviceType(type);
    }

    /*****end***********************/

    /******for HomeBox device list view******************/
    public void getHomeBoxDeviceListFinish(VFile[] device) {
        if (device.length > 0) {
            String storageName = ((RemoteVFile)device[0]).getStorageName();
            mActivity.setSeclectedStorage(getResources().getString(R.string.asushomebox_storage_title) + storageName, null);
        }

        // Hide the home fragment
        if (!mActivity.isShowFileListFragment()) {
            mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);
        }
        setListShown(true);
        setDeviceListAdapter();
        SambaFileUtility.updateHostIp = false;
        mDeviceAdapter.updateHomeBoxDeviceAdapter(device);
        ShortCutFragment.currentTokenFile = null;
        updateNofileLayout(NO_DEVICES);
        hideRefresh();
    }

    /*******************++Kelly2_Zhou*******************/
//    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(getActivity());
        if (sambaFileUtility.updateHostIp) {
            if (sambaFileUtility.ScanFinish) {
                sambaFileUtility.startScanNetWorkDevice(true);
            }
        } else if (RemoteFileUtility.isShowDevicesList) {
            if (remoteVFile == null) {
                hideRefresh();
                return;
            }
            setListShown(false);
            SetScanHostIndicatorPath(remoteVFile.getStorageName());
            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg((remoteVFile).getStorageName(), null, null, (remoteVFile).getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
        } else
            startScanFile(mIndicatorFile, ScanType.SCAN_CHILD);
    }
    public void showRecentFiles() {
        setListShown(false);
        VFile[] files = RecentFileUtil.getRecentFiles(getActivity(), 10);
        SetScanHostIndicatorPath(RecentFileUtil.RECENT_SCAN_FILES);
        if (mActivity != null) {
            mActivity.showSortContainerView(true);
            mActivity.updateActionMenuView();
        }
        if (mAdapter == null) {
            mAdapter = new FileListAdapter(this, null, mIsAttachOp);
        }

//        mPathHome.setEnabled(false);
        mPathHome.setEnabled(true);
        setListAdapter(mAdapter);
        getListView().setItemsCanFocus(true);

        BasicFileListAdapter adapter = getCurrentUsingAdapter();
        adapter.setOrientation(getResources().getConfiguration().orientation);
        if (adapter != null) {
            adapter.updateAdapter(files, false, mSortType, null);
        } else {
            Log.w(TAG, "mAdapter is null when calling remoteUpdateThumbnail");
        }
        setListShownNoAnimation(true);


        updateEditMode();
//        mPathHome.setEnabled(false);
        mPathHome.setEnabled(true);
        hideRefresh();
    }

    public void enablePullReFresh(boolean enable) {
        enbalePull = enable;
    }

    private boolean isPullEnabled() {
       return enbalePull;
    }

    private boolean isListRefreshing() {
       return isRefreshing;
    }

    private void setIsRefreshing(boolean refreshing) {
       isRefreshing = refreshing;
    }

    private void showSyncStatusBar() {
       if (mSyncProgressTracker != null) {
           mSyncProgressTracker.showSyncStatusBar();
           setIsRefreshing(true);
       }
    }

    private void hideSyncStatusBar() {
       if (mSyncProgressTracker != null) {
          mSyncProgressTracker.hideSyncStatusBar();
          setIsRefreshing(false);
       }
    }

    public void hideRefresh() {
        mSyncProgressTracker.cancelMovementTracking();
        hideSyncStatusBar();
    }

    public void setRemoteVFile(RemoteVFile vf) {
        remoteVFile = vf;
    }




    private void initSyncProgressTracker(ViewGroup groupView) {
        groupView.setOnTouchListener(this);
        mListView.setOnTouchListener(this);
        mSyncProgressTrackerListener = new SyncProgressTracker.SyncProgressTrackerListener() {

            @Override
            public boolean isReadyToStartMovementTracking() {//need to add if list or grid
                if (!isListRefreshing() && (mListView.getChildCount() == 0
                        || mListView.getChildAt(0).getTop() == 0)) {
                return true;
            }
                return false;
            }

            @Override
            public void onCancelMovementTracking() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTriggerScale(float arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTriggerSync() {
                // TODO Auto-generated method stub
                //mSyncProgressTracker.cancelMovementTracking();
                if (!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
                    mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                    return;
                }
                showSyncStatusBar();
                onRefresh();
            }
        };
        mSyncProgressTracker = new SyncProgressTracker(mActivity, groupView,
        mSyncProgressTrackerListener, mActivity.getWindow());
        mSyncProgressTracker.setCheckingMessage(getString(R.string.pullfreshlistview_header_hint_loading));
        mSyncProgressTracker.setSyncMessage(getString(R.string.pullfreshlistview_header_hint_normal));
        mSyncProgressTracker.setMessageColor(Color.WHITE);
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        // TODO Auto-generated method stub
        if (isPullEnabled()) {
            mSyncProgressTracker.dispatchTouchEvent(event);
            if (mListView.getChildCount() == 0) {
                return true;
            }
        }
        return false;
    }
    /********************end***********************/

    public boolean isItemMoving() {

        return mListView.isItemMoving();

    }


    public void setSliding(boolean isSliding) {
        mListView.setSliding(isSliding);
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;

        int gridWidth = ViewUtility.dp2px(mActivity, 102);
        if (ConstantsUtil.mGridMode == ConstantsUtil.GRID_MODE_MEDIA){
            int mAvailWidth = mScreenWidth;
            gridWidth = ViewUtility.getGridWidth(mActivity, mAvailWidth, ViewUtility.dp2px(mActivity,7), ViewUtility.getMode(mActivity));
        }
        int currentWidth = mGridView.getColumnWidth();
        if (currentWidth != gridWidth){
            mGridView.setColumnWidth(gridWidth);
            resizeGridViewSpacing(mActivity, mGridView, gridWidth, ConstantsUtil.mGridMode);
            ListAdapter anAdapter = mGridView.getAdapter();
            if (anAdapter instanceof FileListAdapter){
                ((FileListAdapter)anAdapter).mGridWidth = gridWidth;
            }
            mGridView.invalidateViews();
        }

        if (newConfig.smallestScreenWidthDp>=600) {
            ListAdapter listAdapter = getListAdapter();
            if (listAdapter != null && listAdapter instanceof DeviceListAdapter) {
                if (mDeviceAdapter != null) {
                    setListAdapter(mDeviceAdapter);
                }
            } else {
                BasicFileListAdapter adapter = getCurrentUsingAdapter();
                if (adapter != null && (adapter instanceof BasicFileListAdapter)) {
                    setListAdapter(mAdapter);
                }
            }

            if (mPathContainerRoot!=null) {
                LayoutParams params = (LayoutParams) mPathContainerRoot.getLayoutParams();
                params.height = getResources().getDimensionPixelSize(R.dimen.path_view_height);
                mPathContainerRoot.setLayoutParams(params);

            }

        }
    }

    /**********add for GoBackToSelectItem**********/
    private int getDumpLevel(String selectPath,String prePath) {
        int level = 0;
        String pathDiv = "";
        selectPath = convertStarChar2Noraml(selectPath);
        prePath = convertStarChar2Noraml(prePath);
        if (!selectPath.equals(prePath)) {
            if (selectPath.endsWith(File.separator)) {
                pathDiv = selectPath;
            } else {
                pathDiv = selectPath + File.separator;
            }
            String[] divArray = prePath.split(pathDiv);
            if (divArray != null && divArray.length > 1) {
                String deletPath = divArray[1];
                if (deletPath.contains(File.separator)) {
                    String[] deletArray = deletPath.split(File.separator);
                    level = deletArray.length;
                } else {
                    level = 1;
                }
            }
        }
        return level;

    }

    private String convertStarChar2Noraml(String selectPath) {
        String result = selectPath;
        if (selectPath.contains("*")) {
            StringBuilder builder = new StringBuilder(selectPath);
            builder.replace(4, 9, "x");
            result = builder.toString();
        }
        return result;
    }

    private boolean targetDirIsChildOfSourceDir() {
        boolean isChild = false;
        if (mEditPool == null || mEditPool.getSize() == 0) {
            return false;
        }
        for(int i=0;i<mEditPool.getSize();i++)
        {
            VFile v = mEditPool.getFiles()[i];
            if (EditorUtility.IsSourceIncludeTarget(v, mIndicatorFile))
            {
                isChild = true;
                break;
            }
        }
        return isChild;
    }

    private void handleIntent(Intent intent) {
        String filePath = intent.getStringExtra(HandleSearchIntentActivity.FILE_PATH);
        if (filePath != null) {
            VFile file = new VFile(filePath);
            setmIndicatorFile(file);
        }
    }

    @Override
    public void handleAction(int action) {
        switch(action) {
            case SafOperationUtility.ACTION_PASTE:

                ItemOperationUtility.isReadyToPaste = false;
                EditPool tempPool = new EditPool();
                VFile targetVfile = mEditPool.getPasteVFile();
                tempPool.setFiles(mEditPool.getFiles(),false);
                tempPool.setExtraBoolean(mEditPool.getExtraBoolean());
                tempPool.setPasteVFile(targetVfile);
                tempPool.setPastePath(targetVfile.getAbsolutePath());
                tempPool.setTargetDataType(targetVfile.getVFieType()); // for remote file copy action, we need to save the target file type
                int fromVfileType =(tempPool.getFiles() != null)? tempPool.getFiles()[0].getVFieType():-1;
                int toVfileType = targetVfile.getVFieType();
                if (toVfileType == VFileType.TYPE_CLOUD_STORAGE && fromVfileType == VFileType.TYPE_LOCAL_STORAGE) {
                    tempPool.setPasteDialogType(RemoteFilePasteDialogFramgment.PASTE_UPLOAD);
                    showDialog(DialogType.TYPE_REMOTE_PASTE_DIALOG, tempPool);
                } else if (toVfileType == VFileType.TYPE_LOCAL_STORAGE && fromVfileType == VFileType.TYPE_CLOUD_STORAGE) {
                    tempPool.setPasteDialogType(RemoteFilePasteDialogFramgment.PASTE_DOWNLOAD);
                    showDialog(DialogType.TYPE_REMOTE_PASTE_DIALOG, tempPool);
                } else if ((fromVfileType == VFileType.TYPE_LOCAL_STORAGE) && (toVfileType == VFileType.TYPE_LOCAL_STORAGE)) {
                    showDialog(DialogType.TYPE_PASTE_DIALOG, tempPool);
                    ItemOperationUtility.initNotificationBar(getActivity());
                } else {
                    showDialog(DialogType.TYPE_PASTE_DIALOG, tempPool);
                    SambaFileUtility.getInstance(getActivity()).restorePasteEditPool(tempPool);
                }
                break;
            case SafOperationUtility.ACTION_MKDIR:
                showDialog(DialogType.TYPE_ADD_NEW_FOLDER, mIndicatorFile);
                break;
            case SafOperationUtility.ACTION_DELETE:
                showDialog(DialogType.TYPE_DELETE_DIALOG, mDeleteFilePool);
                break;
            case SafOperationUtility.ACTION_RENAME:
                VFile chooseFile = SafOperationUtility.getInstance().getChoosedFile();
                if (chooseFile != null) {
                    showDialog(DialogType.TYPE_RENAME_DIALOG, chooseFile);
                }
                break;
            case SafOperationUtility.ACTION_ZIP:
                VFile selectFile = SafOperationUtility.getInstance().getChoosedFile();
                VFile[] mArray = new VFile[1];
                mArray[0] = selectFile;
                if (selectFile != null) {
                     ZipDialogFragment.showZipDialog(this, mArray, false);
                }
                break;
            case SafOperationUtility.ACTION_EXTRACT:
                VFile extractFile = SafOperationUtility.getInstance().getChoosedFile();
                if (extractFile != null) {
                    FileUtility.openFile(getActivity(), extractFile, mIsAttachOp, false);
                }
                break;
            default:
                break;
        }
    }

    public void updateStatesWithoutSafPermission(int action) {
        switch(action) {
        case SafOperationUtility.ACTION_PASTE:
            if (!ItemOperationUtility.isReadyToPaste) {
                mEditPool.clear();
                getActivity().invalidateOptionsMenu();
            }
            getCurrentUsingAdapter().notifyDataSetChanged();
            EditorUtility.sEditIsProcessing = false;
            break;
        case SafOperationUtility.ACTION_DELETE:
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DELET_COMPLETE));
            break;
        default:
            break;
        }
        ToastUtility.show(getActivity(), R.string.permission_deny, Toast.LENGTH_LONG);
    }

    private void contentViewSwitch() {
        ItemOperationUtility.getInstance().switchViewMode();
        ItemOperationUtility.getInstance().saveViewModeToPreferences(mActivity);

        if (mViewBadge!= null ){
            mViewBadge.setVisibility(View.GONE);
        }
        boolean isListViewMode = ItemOperationUtility.getInstance().isListViewMode();
        hideListView(!isListViewMode);
        updateViewModeIcon(isListViewMode);
        setListAdapter(mAdapter);

        if (!isListViewMode){
            if (isCategoryMedia()) {
                adjustGridMode(ConstantsUtil.GRID_MODE_MEDIA);
            }else{
                adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            }
        }
//        getCurrentUsingAdapter().notifyDataSetChanged();
        String label = isListViewMode ? GaUserPreference.LABEL_LISTVIEW_MODE : GaUserPreference.LABEL_GRIDVIEW_MODE;

        GaUserPreference.getInstance().sendEvents(mActivity, GaUserPreference.CATEGORY_NAME,
            GaUserPreference.ACTION_SWITCH_DISPLAY_MODE, label, null);
    }

    private void updateViewModeIcon(boolean isListViewMode) {
        mViewSwitcher.setImageResource(isListViewMode ? R.drawable.ic_icon_gridview : R.drawable.ic_icon_listview);
        mViewSwitcher.setColorFilter(ContextCompat.getColor(getActivity(), R.color.home_line_icon));
    }

    private void setOnItemClickAndLongClickListener() {
        getShowView().setOnItemClickListener(mAdapter);
        getShowView().setOnItemLongClickListener(mAdapter);
    }

    private void hideListView(boolean hide) {
        if (isCategoryUsingExpandableListView()) {
            // IGNORE
            return;
        }
        if (hide) {
            mListView.setVisibility(View.GONE);
            mGridView.setVisibility(View.VISIBLE);
        } else {
            mGridView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    private void setViewSwitcherVisibility(int visibility) {
        if (isCategoryUsingExpandableListView()) {
            visibility = View.GONE;
        }

        if (mViewSwitcher != null) {
            mViewSwitcher.setVisibility(visibility);
        }
        if (mViewBadge != null){
            if (visibility == View.GONE) {
                mViewBadge.setVisibility(View.GONE);
            }else {
                boolean bViewModeSetted = ItemOperationUtility.getInstance().containsViewModeInPreferences(mActivity);
                if (!bViewModeSetted){
                    mViewBadge.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onFolderSelected(VFile selectedVFile, Bundle data) {

        // Add favorite folder case
        if (isCategoryFavorite()) {
            GaAccessFile.getInstance()
                    .sendEvents(getActivity(), GaAccessFile.CATEGORY_NAME,
                            GaAccessFile.ACTION_ADD_TO_FAVORITE,
                            GaAccessFile.LABEL_FROM_FAVORITE_PAGE, Long.valueOf(1));
            addFavoriteFile(selectedVFile);
            updateEditMode();
            return;
        }

        // MoveTo case
        if (EditorUtility.sEditIsProcessing) {
            ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.paste_progress_title));
            return;
        }
        if (targetDirIsChildOfSourceDir()) {
            ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.target_directory_is_child_of_source));
            return;
        }
        ItemOperationUtility.isReadyToPaste = false;

        mEditPool.setPasteVFile(selectedVFile);

        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(selectedVFile.getAbsolutePath());
        if (bNeedWriteToAppFolder) {
            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                .newInstance();
            warnDialog.show(mActivity.getFragmentManager(),
                "WarnKKSDPermissionDialogFragment");
            return;
        }else if ((SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(selectedVFile.getAbsolutePath()))) {
            mActivity.callSafChoose(SafOperationUtility.ACTION_PASTE);
            return;
        }

        if (mEditPool.getExtraBoolean()) {
            // move to case should check all of source file has right saf permission
            VFile[] filesToCut = mEditPool.getFiles();
            for (int i = 0; i < filesToCut.length; i++) {

                bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(filesToCut[i].getAbsolutePath());
                if (bNeedWriteToAppFolder) {
                    WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                    warnDialog.show(mActivity.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
                    return;
                }
                if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog((filesToCut[i].getAbsolutePath()))) {
                    mActivity.callSafChoose(SafOperationUtility.ACTION_PASTE);
                    return;
                }
            }
        }

        EditPool tempPool = new EditPool();
        tempPool.setFiles(mEditPool.getFiles(),false);
        tempPool.setExtraBoolean(mEditPool.getExtraBoolean());
        tempPool.setPasteVFile(selectedVFile);
        tempPool.setPastePath(selectedVFile.getAbsolutePath());
        tempPool.setTargetDataType(selectedVFile.getVFieType()); // for remote file copy action, we need to save the target file type
        int fromVfileType =(tempPool.getFiles() != null)? tempPool.getFiles()[0].getVFieType():-1;
        int toVfileType = selectedVFile.getVFieType();
        if (toVfileType == VFileType.TYPE_CLOUD_STORAGE && fromVfileType == VFileType.TYPE_LOCAL_STORAGE) {
            tempPool.setPasteDialogType(RemoteFilePasteDialogFramgment.PASTE_UPLOAD);
            showDialog(DialogType.TYPE_REMOTE_PASTE_DIALOG, tempPool);
        } else if (toVfileType == VFileType.TYPE_LOCAL_STORAGE && fromVfileType == VFileType.TYPE_CLOUD_STORAGE) {
            tempPool.setPasteDialogType(RemoteFilePasteDialogFramgment.PASTE_DOWNLOAD);
            showDialog(DialogType.TYPE_REMOTE_PASTE_DIALOG, tempPool);
        } else if ((fromVfileType == VFileType.TYPE_LOCAL_STORAGE) && (toVfileType == VFileType.TYPE_LOCAL_STORAGE)) {
            showDialog(DialogType.TYPE_PASTE_DIALOG, tempPool);
            ItemOperationUtility.initNotificationBar(getActivity());
        } else {
            showDialog(DialogType.TYPE_PASTE_DIALOG, tempPool);
            SambaFileUtility.getInstance(getActivity()).restorePasteEditPool(tempPool);
        }
        if (tempPool.getExtraBoolean()) {
            GaAccessFile.getInstance().sendEvents(getActivity(),
                    GaAccessFile.ACTION_MOVE_TO, fromVfileType, toVfileType, tempPool.getSize());
        } else {
            GaAccessFile.getInstance().sendEvents(getActivity(),
                    GaAccessFile.ACTION_COPY_TO, fromVfileType, toVfileType, tempPool.getSize());
        }
    }

    public void startScanAlbums(VFile imageFolder) {
        startScanAlbums(imageFolder, false);
    }

    public void startScanAlbums(VFile imageFolder, boolean researchAfterComplete) {
        Log.d(TAG, "startScanAlbums");
        if (imageFolder == null) {
            Log.d(TAG, "startScanAlbums imageFolder is null");
            return;
        }
        int bucket_id = imageFolder.getBucketId();
        if (bucket_id == 0) {
            Log.d(TAG, "startScanAlbums bucket_id is 0");
            return;
        }

        mIndicatorFile = imageFolder;

        mIsComFromSearch = false;
        RemoteFileUtility.isShowDevicesList = false;
        ShortCutFragment.currentTokenFile = null;
        enablePullReFresh(false);

        // set default file list layout
        updateNofileLayout(NO_FILE);

        onDeselectAll();

        PathIndicator.setPathIndicator(mPathContainer, imageFolder, mPathIndicatorClickListener);

//        ((FileManagerActivity)getActivity()).updateActionBarTitle(category);

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });

//        mIsInCategory = true;
        getActivity().invalidateOptionsMenu();



        if (isListShown()) {
            setListShown(false);
        }

        closePopup();

        if (mActivity.CATEGORY_IMAGE_FILE.equals(imageFolder.getParentFile())) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadImageFilesByBucketId(String.valueOf(bucket_id), researchAfterComplete);
        } else if (mActivity.CATEGORY_MUSIC_FILE.equals(imageFolder.getParentFile())) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadMusicFilesByBucketId(String.valueOf(bucket_id), researchAfterComplete);
        } else if (mActivity.CATEGORY_VIDEO_FILE.equals(imageFolder.getParentFile())) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadVideoFilesByBucketId(String.valueOf(bucket_id), researchAfterComplete);
        }
    }

    public void startScanCategory(VFile category) {
        startScanCategory(category, false);
    }

    public void startScanCategory(VFile category, boolean researchAfterComplete) {
        Log.d(TAG, "startScanCategory");
        if (category == null) {
            Log.d(TAG, "startScanCategory category is null");
            return;
        }
        if (ItemOperationUtility.getInstance().isFromBackPress()) {
            ItemOperationUtility.getInstance().backKeyPressed(false);
        }

        if (mActivity.CATEGORY_FAVORITE_FILE.equals(category)) {
            showFAB(true);
        } else {
            showFAB(false);
        }

        if (mActivity.CATEGORY_HOME_PAGE_FILE.equals(category)) {
            if (mActivity.getCurrentFragmentType() == FragmentType.HOME_PAGE ||
                    mActivity.getCurrentFragmentType() == FragmentType.FILE_LIST )
            mActivity.showSearchFragment(FragmentType.HOME_PAGE, true);
            return;
        }

        if (mActivity.CATEGORY_IMAGE_FILE.equals(category.getParentFile())
                || mActivity.CATEGORY_MUSIC_FILE.equals(category.getParentFile())
                || mActivity.CATEGORY_VIDEO_FILE.equals(category.getParentFile())) {
            startScanAlbums(category, researchAfterComplete);
            return;
        }

        if (!category.equals(mActivity.CATEGORY_IMAGE_FILE)
                && !category.equals(mActivity.CATEGORY_MUSIC_FILE)
                && !category.equals(mActivity.CATEGORY_VIDEO_FILE)
                && !category.equals(mActivity.CATEGORY_APK_FILE)
                && !category.equals(mActivity.CATEGORY_FAVORITE_FILE)
                && !category.equals(mActivity.CATEGORY_COMPRESS_FILE)
                && !category.equals(mActivity.CATEGORY_DOCUMENT_FILE)
                && !category.equals(mActivity.CATEGORY_RECENT_FILE)
                && !category.equals(mActivity.CATEGORY_LARGE_FILE)
                && !category.equals(mActivity.CATEGORY_PDF_FILE)
                && !category.equals(mActivity.CATEGORY_GAME_FILE)
                )
            return;

        mIndicatorFile = category;

        mIsComFromSearch = false;
        RemoteFileUtility.isShowDevicesList = false;
        ShortCutFragment.currentTokenFile = null;
        enablePullReFresh(false);

        // set default file list layout
        updateNofileLayout(NO_FILE);

        onDeselectAll();

        PathIndicator.setPathIndicator(mPathContainer, category, mPathIndicatorClickListener);

        ((FileManagerActivity)getActivity()).updateActionBarTitle(category);

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });

//        mIsInCategory = true;
        getActivity().invalidateOptionsMenu();

        if (isListShown()) {
            setListShown(false);
        }

        closePopup();

        setViewSwitcherVisibility(View.VISIBLE);

        loadCategoryFiles(category, researchAfterComplete);
    }

    private void adjustGridMode(int mode){
        int gridWidth = ViewUtility.dp2px(mActivity,102);
        if (mode == ConstantsUtil.GRID_MODE_NORMAL){
            //do nothing
        }else if (mode == ConstantsUtil.GRID_MODE_MEDIA){
            int mAvailWidth = mScreenWidth;
            gridWidth = ViewUtility.getGridWidth(mActivity, mAvailWidth, ViewUtility.dp2px(mActivity,7), ViewUtility.getMode(mActivity));
        }
        int currentWidth = mGridView.getColumnWidth();
        if (currentWidth != gridWidth || ConstantsUtil.mGridMode != mode){
            mGridView.setColumnWidth(gridWidth);
            resizeGridViewSpacing(mActivity, mGridView, gridWidth, mode);
            ListAdapter anAdapter = mGridView.getAdapter();
            if (anAdapter instanceof FileListAdapter){
                ((FileListAdapter)anAdapter).mGridWidth = gridWidth;
            }else if (null != mAdapter){
                mAdapter.mGridWidth = gridWidth;
            }
            mGridView.invalidateViews();
        }
        if (ConstantsUtil.mGridMode != mode){
            ConstantsUtil.mGridMode = mode;
        }
    }
    public void loadCategoryFiles(VFile category, boolean researchAfterComplete) {
        Log.d(TAG, "loadCategoryFiles");
        if (category.equals(mActivity.CATEGORY_IMAGE_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_MEDIA);
            loadImageFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_MUSIC_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_MEDIA);
            loadMusicFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_VIDEO_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_MEDIA);
            loadVideoFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_APK_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadApkFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_FAVORITE_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadFavoriteFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_COMPRESS_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadCompressFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_DOCUMENT_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadDocumentFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_RECENT_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadRecentFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_LARGE_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadLargeFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_PDF_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadPdfFiles(researchAfterComplete);
        } else if (category.equals(mActivity.CATEGORY_GAME_FILE)) {
            adjustGridMode(ConstantsUtil.GRID_MODE_NORMAL);
            loadGameFiles(researchAfterComplete);
        }
    }

    private void loadImageFiles(final boolean researchAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getImageAlbums(getActivity(), false);

                LocalVFile[] files = albumFiles.toArray(new LocalVFile[albumFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (researchAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadImageFilesByBucketId(final String bucket_id, final boolean researchAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getImageFilesByBucketId(getActivity(), bucket_id, 0);

                LocalVFile[] files = albumFiles.toArray(new LocalVFile[albumFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (researchAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadMusicFiles(final boolean researchAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getMusicAlbums(getActivity());
                ArrayList<LocalVFile> musicsFiles = MediaProviderAsyncHelper.getMusicFiles(getActivity(), false);

                LocalVFile[] files = musicsFiles.toArray(new LocalVFile[musicsFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (researchAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadMusicFilesByBucketId(final String bucket_id, final boolean researchAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getMusicFilesByBucketId(getActivity(), bucket_id, 0, false);

                LocalVFile[] files = albumFiles.toArray(new LocalVFile[albumFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (researchAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadVideoFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getVideoAlbums(getActivity(), false);

                LocalVFile[] files = albumFiles.toArray(new LocalVFile[albumFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadVideoFilesByBucketId(final String bucket_id, final boolean researchAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> albumFiles = MediaProviderAsyncHelper.getVideoFilesByBucketId(getActivity(), bucket_id, 0);

                LocalVFile[] files = albumFiles.toArray(new LocalVFile[albumFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (researchAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    /*private void loadMusicFiles() {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> musicFiles = MediaProviderAsyncHelper.getMusicFiles(getActivity());

                LocalVFile[] files = musicFiles.toArray(new LocalVFile[musicFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadVideoFiles() {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> videoFiles = MediaProviderAsyncHelper.getVideoFiles(getActivity());

                LocalVFile[] files = videoFiles.toArray(new LocalVFile[videoFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }*/

    private void loadApkFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> apkFiles = MediaProviderAsyncHelper.getFilesByExtension(getActivity(), new String[] {"apk"}, true);

                LocalVFile[] files = apkFiles.toArray(new LocalVFile[apkFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadFavoriteFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ArrayList<LocalVFile> favoriteFiles = ProviderUtility.FavoriteFiles.getFiles(mActivity.getContentResolver(), sShowHidden);
                if (favoriteFiles != null) {
                    for (LocalVFile file : favoriteFiles) {
                        if (file.listFiles() != null) {
                            file.setChildCount(file.listFiles().length);
                        } else {
                            file.setChildCount(0);
                        }
                    }
                }
                LocalVFile[] files = favoriteFiles.toArray(new LocalVFile[favoriteFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadCompressFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<LocalVFile> apkFiles = MediaProviderAsyncHelper.getFilesByExtension(getActivity(),
                        FileManagerActivity.SUPPORT_EXTENSION_IN_COMPRESS_CATEGORY, sShowHidden);

                LocalVFile[] files = apkFiles.toArray(new LocalVFile[apkFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadDocumentFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // FIXME:
                // For TT-686562: aosp device can't recognize ppt/pptx files (mime_type = null)
                // we query ppt/pptx files again by extName
                ArrayList<LocalVFile> documentFiles = MediaProviderAsyncHelper.getFilesByMimeTypeAndExtName(getActivity(),
                    FileManagerActivity.SUPPORT_MIMETYPE_IN_DOCUMENTS_CATEGORY,
                    FileManagerActivity.SUPPORT_EXTENSION_IN_PPT_CATEGORY,
                    sShowHidden, false
                );
                LocalVFile[] files = documentFiles.toArray(new LocalVFile[documentFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadRecentFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<LocalVFile> recentFiles = MediaProviderAsyncHelper.getRecentFiles(mActivity, sShowHidden);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY,
                    recentFiles.toArray(new LocalVFile[recentFiles.size()])
                );
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadLargeFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<LocalVFile> recentFiles = MediaProviderAsyncHelper.getFilesBySize(
                    getActivity(), FileManagerActivity.SUPPORT_LARGE_FILES_THRESHOLD,
                    sShowHidden, false
                );
                LocalVFile[] files = recentFiles.toArray(new LocalVFile[recentFiles.size()]);

                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);

                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }

                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadPdfFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<LocalVFile> documentFiles = MediaProviderAsyncHelper.getFilesByMimeType(getActivity(),
                        FileManagerActivity.SUPPORT_EXTENSION_IN_PDF_CATEGORY, sShowHidden, false);

                LocalVFile[] files = documentFiles.toArray(new LocalVFile[documentFiles.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    private void loadGameFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<GameLaunchFile> list = GameAppDbHelper.queryGameLaunchFileFromDb(mActivity);
                VFile[] files = list.toArray(new VFile[list.size()]);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files);
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }
        });
        countThread.start();
    }

    private void loadRecentlyOpenFiles(final boolean refreshAfterComplete) {
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<LocalVFile> files = ProviderUtility.RecentlyOpen.getFiles(mActivity.getContentResolver(), FileListFragment.sShowHidden);
                Message msg = mHandler.obtainMessage(MSG_LOAD_FINISHED_CATEGORY, files.toArray(new LocalVFile[files.size()]));
                if (refreshAfterComplete) {
                    msg.arg1 = MESSAGE_ARG_RESEARCH_AFTER_LOAD_FINISHED;
                }
                mHandler.sendMessage(msg);
            }

        });
        countThread.start();
    }

    public void loadCategoryFinished(VFile[] data) {
        Log.d(TAG, "loadCategoryFinished");
        if (isLoadingSambaFolder) {
            isLoadingSambaFolder = false;
            SambaLoadingComplete();
        }

        if (data != null && !sShowHidden) {
            data = FileUtility.filterHiddenFile(data);
        }
        sortFilesBySortType(data, mSortType);
        if (data != null) {
            String[] favoriteFilePaths = ProviderUtility.FavoriteFiles.getPaths(mActivity.getContentResolver());
            if(favoriteFilePaths != null) {
                Arrays.sort(favoriteFilePaths);
                data = setFileInfo(data, favoriteFilePaths);
            }
        }

        // switch to file list adapter for category
        if (getListAdapter() != null && getListAdapter() instanceof DeviceListAdapter && !isCategoryUsingExpandableListView()) {
            setFileListAdapter();
        }

        final BasicFileListAdapter adapter = getCurrentUsingAdapter();
        adapter.updateAdapter(data, false, mSortType, new BasicFileListAdapter.AdapterUpdateObserver() {
            @Override
            public void updateAdapterDone() {
                if (adapter instanceof ExpandableFileListAdapter) {
                    ((ExpandableFileListAdapter) adapter).expandGroupIfNeeded(mExpandableListView);
                    mExpandableListView.setSelectionAfterHeaderView();
                }
                updateAdapterResult();
            }
        });
    }

    public void sort(VFile[] data) {
        if (data != null) {
            Arrays.sort(data, SortUtility.getComparator(mSortType));
        }
    }

    private VFile[] setFileInfo(VFile[] fa, String[] favoriteFilePaths) {
        if (fa == null || fa.length == 0 || favoriteFilePaths == null || favoriteFilePaths.length == 0) {
            return fa;
        }

        ArrayList<VFile> pool = new ArrayList<VFile>();
        for (int i = 0; i < fa.length && fa[i] != null; i++) {
            String path;
            try {
                path = FileUtility.getCanonicalPathForUser(fa[i].getCanonicalPath());
            } catch (IOException e2) {
                path = fa[i].getAbsolutePath();
                e2.printStackTrace();
            }
            int index = Arrays.binarySearch(favoriteFilePaths, path);

            if (fa[i].isDirectory() && index > -1) {
                fa[i].setFavoriteName(ProviderUtility.FavoriteFiles.getFavoriteNameByPath(mCr, path));
            }
            pool.add(fa[i]);
        }

        VFile[] r = new VFile[pool.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = pool.get(i);
        }
        return r;
    }
    public void addFavoriteFile(VFile file) {
        ContentResolver cr = getActivity().getContentResolver();
        boolean isFileExists = ProviderUtility.FavoriteFiles.exists(cr, file);
        if (isFileExists) {
            return;
        }

        boolean isNameExists = ProviderUtility.FavoriteFiles.exists(cr, file.getName());
        if (isNameExists) {
            renameFavoriteFile(file);
        } else {
            try {
                if (ProviderUtility.FavoriteFiles.insertFile(cr, file.getName(), FileUtility.getCanonicalPathForUser(file.getCanonicalPath())) != null) {
                    onDeselectAll();
                    reScanFile();
                    ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.add_favorite_success));
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void renameFavoriteFile(VFile file) {
        mActivity.displayDialog(DialogType.TYPE_FAVORITE_RENAME_DIALOG, file);
        onDeselectAll();
    }

    public void removeFavoriteFile(VFile[] files, boolean isNeedCheck) {
        EditPool removeFilePool = new EditPool();
        removeFilePool.setFiles(files, isNeedCheck);
        mActivity.displayDialog(DialogType.TYPE_FAVORITE_ROMOVE_DIALOG, removeFilePool);
        onDeselectAll();
    }

    /*public void setInCategory(boolean isInCategory) {
        mIsInCategory = isInCategory;
    }*/

    // Notes:
    // isInCategory contains the files with TYPE_CATEGORY_STORAGE.
    // isCategory* only contains the top directory, not include the child.
    // belongToCategory* contains the top directory and the child.

    public boolean isInCategory() {
        return mIndicatorFile != null && mIndicatorFile.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE;
    }

    public boolean isCategoryMediaTopDir() {
        return isCategoryMedia()
                || isCategoryApk() || isCategoryDocument() || isCategoryCompress()
                || isCategoryRecent() || isCategoryLargeFile() || isCategoryPdf();
    }

    // Contains the category: image/video/music
    // Only the top directory, not include its child directory
    public boolean isCategoryMedia() {
        return (
            isCategory(mIndicatorFile, mActivity.CATEGORY_IMAGE_FILE) ||
            isCategory(mIndicatorFile, mActivity.CATEGORY_MUSIC_FILE) ||
            isCategory(mIndicatorFile, mActivity.CATEGORY_VIDEO_FILE)
        );
    }

    public boolean isCategoryFavorite() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_FAVORITE_FILE);
    }

    // Contains the category: image/video/music/apk/compress/document/recent
    // Include its child directory
    public boolean belongToCategoryFromMediaStore() {
        return (belongToCategoryMedia()
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_APK_FILE)
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_COMPRESS_FILE)
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_DOCUMENT_FILE)
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_RECENT_FILE)
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_LARGE_FILE)
            || belongToCategory(mIndicatorFile, mActivity.CATEGORY_PDF_FILE)
        );
    }

    // Contains the category: image/video/music
    // Include its child directory
    public boolean belongToCategoryMedia() {
        return (
            belongToCategory(mIndicatorFile, mActivity.CATEGORY_IMAGE_FILE) ||
            belongToCategory(mIndicatorFile, mActivity.CATEGORY_MUSIC_FILE) ||
            belongToCategory(mIndicatorFile, mActivity.CATEGORY_VIDEO_FILE)
        );
    }

    public boolean isCategoryImage() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_IMAGE_FILE);
    }

    public boolean isCategoryMusic() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_MUSIC_FILE);
    }

    public boolean isCategoryVideo() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_VIDEO_FILE);
    }

    public boolean isCategoryRecent() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_RECENT_FILE);
    }

    public boolean isCategoryLargeFile() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_LARGE_FILE);
    }

    public boolean isCategoryApk() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_APK_FILE);
    }

    public boolean isCategoryDocument() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_DOCUMENT_FILE);
    }

    public boolean isCategoryCompress() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_COMPRESS_FILE);
    }

    public boolean isCategoryPdf() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_PDF_FILE);
    }

    public boolean isCategoryGame() {
        return isCategory(mIndicatorFile, mActivity.CATEGORY_GAME_FILE);
    }

    public boolean belongToCategoryMusic() {
        return belongToCategory(mIndicatorFile, mActivity.CATEGORY_MUSIC_FILE);
    }

    private static boolean belongToCategory(VFile currentFile, VFile categoryFile) {
        return currentFile.getAbsolutePath().startsWith(categoryFile.getAbsolutePath());
    }

    private static boolean isCategory(VFile currentFile, VFile categoryFile) {
        if (currentFile == null) {
            return false;
        }
        return currentFile.equals(categoryFile);
    }

    private static boolean isLocalRootFolder(VFile f) {
        return (f != null) && f.getAbsolutePath().equals("/") && (f.getVFieType() == VFileType.TYPE_LOCAL_STORAGE);
    }

    private static void resizeGridViewSpacing(Context context, GridView gridView, int colWidth, int newGridMode) {
        Log.d(TAG, "resizeGridViewSpacing");
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // Log.v(TAG, "resize the horizontalSpacing/verticalSpacing of gridview");
        // Log.v(TAG, "width=" + width + ", height=" + height);
        int minWidth = Math.min(width, height);
        Point padding = new Point();

        //int colWidth = ViewUtility.dp2px(context, colWidthdp);
        if (newGridMode == ConstantsUtil.GRID_MODE_MEDIA){
            ViewUtility.calucateGridViewPadding(padding, context, width, colWidth);
        }else{
            ViewUtility.calucateGridViewPadding(padding, context, minWidth, colWidth);
        }
        // we get the padding result, in px: x is horizontalSpacing, y is paddingLeft/paddingRight
        // Log.v(TAG, "minWidth=" + minWidth +", padding=" + padding);
        gridView.setHorizontalSpacing(padding.x);
        gridView.setVerticalSpacing(Math.min(padding.x * 3, gridView.getVerticalSpacing()));
        gridView.setPadding(padding.y, gridView.getPaddingTop(), padding.y, gridView.getPaddingBottom());
    }

    private void showFAB(boolean isShow) {
        if (null == mFab) {
            Log.e(TAG, "can not show/hide null FAB");
            return ;
        }
        // Don't use the mFab.isShown(), Not our expected result
        if (isShow && mFab.getVisibility() != View.VISIBLE) {
            mFab.show();
        } else if (!isShow && View.VISIBLE == mFab.getVisibility()) {
            mFab.hide();
        }
    }

    public boolean isCategoryUsingExpandableListView() {
        return (isCategoryDocument() || isCategoryApk() || isCategoryGame()) && !(SambaFileUtility.updateHostIp || RemoteFileUtility.isShowDevicesList );
        //return (isCategoryDocument() || isCategoryApk());
    }

    protected boolean isForceListMode() {
        return RemoteFileUtility.isShowDevicesList ?
                true : super.isForceListMode();
    }

}