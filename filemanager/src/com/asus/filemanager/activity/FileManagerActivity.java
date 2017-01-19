
package com.asus.filemanager.activity;


import android.Manifest;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.SearchView.OnSuggestionListener;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.azs.version.checker.VersionChecker;
import com.asus.azs.version.checker.VersionChecker.OnDialogEventListener;
import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment.OnShowDialogListener;
import com.asus.filemanager.adapter.MoveToNaviAdapter;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.dialog.AboutDialogFragment;
import com.asus.filemanager.dialog.AddFolderDialogFragment;
import com.asus.filemanager.dialog.CtaDialogFragment.OnCtaDialogFragmentListener;
import com.asus.filemanager.dialog.CtaPreGrantDialogFragment;
import com.asus.filemanager.dialog.FavoriteRemoveDialogFragment;
import com.asus.filemanager.dialog.FavoriteRenameDialogFragment;
import com.asus.filemanager.dialog.FileExistDialogFragment;
import com.asus.filemanager.dialog.FilePickerDialogFragment;
import com.asus.filemanager.dialog.GmsAlertDialogFragment;
import com.asus.filemanager.dialog.InfoDialogFragment;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.NewVersionDialogFragment;
import com.asus.filemanager.dialog.NewVersionDialogFragment.OnNewVersionDialogFragmentListener;
import com.asus.filemanager.dialog.OpenTypeDialogFragment;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.dialog.PermissionReasonDialogFragment;
import com.asus.filemanager.dialog.RecommendDialogFragment;
import com.asus.filemanager.dialog.RecommendDialogFragment.OnRecommendDialogFragmentListener;
import com.asus.filemanager.dialog.RenameDialogFragment;
import com.asus.filemanager.dialog.RequestSDPermissionDialogFragment;
import com.asus.filemanager.dialog.RequestSDPermissionDialogFragment.OnRequestSDPermissionFragmentListener;
import com.asus.filemanager.dialog.SearchDialogFragment;
import com.asus.filemanager.dialog.SortTypeSelectDialogFragment;
import com.asus.filemanager.dialog.UnRarDialogFragment;
import com.asus.filemanager.dialog.UnRarDialogFragment.UnRarDialogFragmentListener;
import com.asus.filemanager.dialog.UnZipDialogFragment;
import com.asus.filemanager.dialog.UnZipDialogFragment.UnZipData;
import com.asus.filemanager.dialog.UnZipDialogFragment.UnZipDialogFragmentListener;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.WhatsNewDialogFragment;
import com.asus.filemanager.dialog.ZipDialogFragment;
import com.asus.filemanager.dialog.delete.DeleteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.Editable;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.editor.EditorUtility.ExistPair;
import com.asus.filemanager.ga.GaActiveUser;
import com.asus.filemanager.ga.GaExperiment;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.ga.GaSettingsPage;
import com.asus.filemanager.gtm.GtmContainerAvailableListener;
import com.asus.filemanager.provider.ProviderUtility.Thumbnail;
import com.asus.filemanager.provider.SearchHistoryProvider;
import com.asus.filemanager.receiver.MediaScannerBroadcastReceiver;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.saf.SafOperationUtility.SafActionHandler;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.ui.DarkThemeToast;
import com.asus.filemanager.ui.EditCategoryHintLayout;
import com.asus.filemanager.ui.HudToastAnimation;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.PrefUtils;
import com.asus.filemanager.utility.ShortCutFrame;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToolbarUtil;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.VolumeInfoUtility;
import com.asus.filemanager.utility.permission.PermissionChecker;
import com.asus.filemanager.utility.permission.PermissionDialog;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;
import com.asus.remote.dialog.RemoteConnectingProgressDialogFragment;
import com.asus.remote.dialog.RemoteFilePasteDialogFramgment;
import com.asus.remote.dialog.RemoteWiFiTurnOnDialogFragment;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;
import com.asus.remote.utility.RemoteClientHandler;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.updatesdk.ZenUiFamily;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.uservoice.uservoicesdk.NewConfigInterface;
import com.uservoice.uservoicesdk.UserVoice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//import android.support.v4.app.ActionBarDrawerToggle;
//import android.support.v4.view.GravityCompat;
//import android.support.v4.widget.DrawerLayout;
//import com.asus.analytics.AnalyticsSettings;
//import android.support.v4.app.ActionBarDrawerToggle;
//import android.support.v4.view.GravityCompat;
//import android.support.v4.widget.DrawerLayout;
//import com.asus.remote.activity.WifiDirectSearchFragment;

public class FileManagerActivity extends BaseAppCompatActivity implements
    OnShowDialogListener,
    OnQueryTextListener,
    OnSuggestionListener,
    OnClickListener,
    OnTouchListener,
        OnRecommendDialogFragmentListener,
    OnNewVersionDialogFragmentListener,
    OnRequestSDPermissionFragmentListener,
    OnCtaDialogFragmentListener,
    UnZipDialogFragmentListener,
    UnRarDialogFragmentListener,
    ResultCallback<ContainerHolder>,
    PermissionChecker,
    WhatsNewDialogFragment.OnWhatsNewDialogFragmentListener, Editable,
        CtaPreGrantDialogFragment.OnCtaPreGrantDialogFragmentListener {

    @Override
    public Handler getEditHandler() {
        if (mCurrentFragmentType == FragmentType.RECYCLE_BIN)
            return ((RecycleBinFragment) findFragment(mCurrentFragmentType)).getHandler();
        else if (mCurrentFragmentType == FragmentType.HIDDEN_ZONE)
            return ((HiddenZoneFragment) findFragment(mCurrentFragmentType)).getHandler();
        else
            return ((FileListFragment) getFragmentManager().findFragmentById(R.id.filelist)).getHandler();

    }

    @Override
    public EditorUtility.RequestFrom getRequester() {
        return EditorUtility.RequestFrom.FileList;
    }

    boolean isNeedPermission(VFile[] selectedFiles, int safAction) {
        boolean bNeedPermission = false;
        for (int i = 0; i < selectedFiles.length; i++) {
            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(this).isNeedToWriteSdToAppFolder(selectedFiles[i].getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                warnDialog.show(getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
                bNeedPermission = true;
                break;
            }
            if (SafOperationUtility.getInstance(this).isNeedToShowSafDialog(selectedFiles[i].getAbsolutePath())) {
                callSafChoose(safAction);
                bNeedPermission = true;
                break;
            }
        }
        return bNeedPermission;
    }

    public static class FontType{
        public static final int ROBOTO_LIGHT = 1;
        public static final int ROBOTO_MEDIUM = 2;
        public static final int ROBOTO_REGULAR = 3;
    }

    public static class DialogType {
        public static final int TYPE_ABOUT_DIALOG = 0;
        public static final int TYPE_INFO_DIALOG = 1;
        public static final int TYPE_RENAME_DIALOG = 2;
        public static final int TYPE_ADD_NEW_FOLDER = 3;
        public static final int TYPE_DELETE_DIALOG = 4;
        public static final int TYPE_DELETEPROGRESS_DIALOG = 5;
        public static final int TYPE_PASTE_DIALOG = 6;
        public static final int TYPE_FILE_EXIST_DIALOG = 7;
        public static final int TYPE_ZIP_DIALOG = 8;
        public static final int TYPE_ZIP_PROGRESS_DIALOG = 9;
        public static final int TYPE_SEARCH_DIALOG = 10;
        public static final int TYPE_UNZIP_DIALOG = 11;
        public static final int TYPE_UNZIP_PROGRESS_DIALOG = 12;
        public static final int TYPE_REMOTE_PASTE_DIALOG = 13;
        public static final int TYPE_CTA_DIALOG = 14;

        // +++ Dragging and Dropping
        public static final int TYPE_DROP_FOR_COPY = 13;
        public static final int TYPE_DROP_FOR_CUT = 14;
        // ---

        // +++ Willie
        public static final int TYPE_UNZIP_PREVIEW_DIALOG = 15;
        public static final int TYPE_CHARACTER_ENCODING_DIALOG = 16;

        // +++ Alex
        public static final int TYPE_SORT_TYPE_DIALOG = 17;

        // +++ remote storage
        public static final int TYPE_CONNECTING_REMOTE_DIALOG = 18;
        public static final int TYPE_WIFI_TURN_ON_DIALOG = 19;
        public static final int TYPE_ADD_NEW_FOLDER_PROGRESS = 20;
        public static final int TYPE_CLOUD_STORAGE_LOADING = 21;
        public static final int TYPE_RENAME_PROGRESS_DIALOG = 22;
        // ---

        //++ yiqiu_huang
        public static final int TYPE_SAMBA_SORAGE_DIALOG = 23;
        public static final int TYPE_PREVIEW_PROCESS_DIALOG = 24;

        //++ select file type to open
        public static final int TYPE_OPEN_TYPE_DIALOG = 25;

        public static final int TYPE_UNRAR_DIALOG = 26;
        public static final int TYPE_UNRAR_PROGRESS_DIALOG = 27;
        public static final int TYPE_UNRAR_PREVIEW_DIALOG = 28;
        public static final int TYPE_UNRAR_CHARACTER_ENCODING_DIALOG = 29;

        /**Move to Dialog***/
        public static final int TYPE_MOVE_TO_DIALOG = 30;

        public static final int TYPE_FAVORITE_RENAME_DIALOG = 31;
        public static final int TYPE_FAVORITE_RENAME_NOTICE_DIALOG = 32;
        public static final int TYPE_FAVORITE_ROMOVE_DIALOG = 33;

        public static final int TYPE_RATE_US_DIALOG = 34;
        public static final int TYPE_FILE_PICKER_DIALOG = 35;
        public static final int TYPE_NEWVERSION_NOTIFY_DIALOG = 36;

        public static final int TYPE_GMS_ALERT_DIALOG = 37;

        public static final int TYPE_RECOMMEND_CM_DIALOG = 38;
        public static final int TYPE_MOVE_TO_RECYCLEBIN_PROGRESS_DIALOG = 39;
        public static final int TYPE_MOVE_TO_HIDDEN_ZONE_DIALOG = 40;
    }

    public class DisplayDialogData {
        int type;
        Object arg;

        public DisplayDialogData(int mtype, Object marg){
            type = mtype;
            arg = marg;
        }
    }

    //+++ Johnson
    private static void setLayoutParams(View v, int w, int h, int weight) {
        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params != null) {
            params.width = w;
            params.height = h;
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).weight = weight;
            }
            v.setLayoutParams(params);
        } else {
            if (v.getParent() instanceof LinearLayout) {
                v.setLayoutParams(new LinearLayout.LayoutParams(w, h, weight));
            } else if (v.getParent() instanceof RelativeLayout) {
                v.setLayoutParams(new RelativeLayout.LayoutParams(w, h));
            }
        }
    }
    //---

    public static final int FILE_MANAGER_NORMAL = 0;
    public static final int FILE_MANAGER_UNZIP_PREVIEW = 1;
    public static final int FILE_MANAGER_SETTING_PERMISSION= 200;
    public final  int COLOR_STATUS_ACTION_BAR = Color.parseColor("#007fa0");
    public final  int DRAWER_BACKGROUD_COLOR = Color.parseColor("#3a4040");

    public static final String ACTION_MULTIPLE_SELECTION = "com.asus.filemanager.action.MULTIPLE_SELECTION";

    private static final String TAG = "FileManagerActivity";
    private static final boolean DEBUG = true;

    public static final String PATH_DOWNLOAD =
            WrapEnvironment.getEpadInternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    private SearchView mSearchView;
    private String mQuery;

    private boolean mIsAttachOp;
    private boolean mIsMultipleSelectionOp = false;
    private boolean mIsFirstUnZipIntent = false;
    private boolean mIsCreateShortcutIntent = false;
    private String  mGoToPath = null;

    // ++ Alex
    private boolean mIsPadMode = false;
    private boolean mIsPadMode2 = false;
    // --
    // +++ Johnson
    private View mDividerLeft;
    private View mDividerRight;
    private ViewGroup mShortCutView;
    private LinearLayout mSortSizeView;
    private LinearLayout mSortDateView;
    private LinearLayout mSortNameView;
    private DrawerLayout mDrawerLayout;
    private StorageListAdapger mStorageListAdapter;
    private int mOldMoveX;
    private int mDraggingMinLeft_land;
    private int mDraggingMinLeft_port;
    private int mDraggingLeftBound;
    private boolean mInDragMode = false;
    private boolean mIsPaddingPic = true;
    private int DIVIDE_LIMITID;
    private int LEFT_BOUND_LAND;
    private int RIGHT_BOUND_LAND;
    private int LEFT_BOUND_PORT;
    private int RIGHT_BOUND_PORT;
    // ---

    // +++ remote storage
    private boolean mIsResume;
    private boolean mIsWiFiDirectPage = false;
    private Messenger mRemoteClientMessenger;
    private Messenger sRemoteService = null;
    private static int CHOOSE_ACCOUNT = 0;
    private static int REQUEST_TUTORIAL = 101;
    private static int REQUEST_SDTUTORIAL = 102;
    private static int REQUEST_ABOUT= 103;
    public static int REQUEST_CATEGORY_EDIT = 104;
    public static int REQUEST_REGISTER_PASSWORD = 105;
    public static int REQUEST_UNLOCK = 106;
    // ---

    private FrameLayout mRightContainer;

    private ObjectAnimator mSlideInAnimator = null;
    private ObjectAnimator mSlideOutAnimator = null;

    public static final String sShortCutFragmentTag = "ShortCutFragment";
    private WindowManager mWindowManager;
    private ShortCutFragment mShortCutFragment;

    public FragmentType getCurrentFragmentType() {
        return mCurrentFragmentType;
    }
    public Fragment getCurrentFragment() {
        return findFragment(mCurrentFragmentType);
    }

    private FragmentType mCurrentFragmentType;
    boolean mIsShowSearchFragment = false;
    public  boolean mIsShowHomePageFragment = false;
    private Intent mNewIntent = null;
    public static boolean isSearchIng = false;
    private ActionBarDrawerToggle mDrawerToggle;

    private float mDrawerSlideOffset;

    private LinearLayout mSortContainerRootView = null;
    TextView custom_titleTextView;
    private Typeface robotoLightTypeface = null;
    private Typeface robotoMediumTypeface = null;
    private Typeface robotoRegularTypeface = null;
    private String currentActionBarTitle = null;
    public static int currentPasteDialogType = -1;

    private String mSelectedStorage = null;

    private RenameDialogFragment renamingProgressDialog = null;
    private DeleteDialogFragment deleteProgressDialogFragment = null;
    private PasteDialogFragment pasteDialogFragment = null;

    private boolean mIsRootDir = false;

    private boolean isFromFirst = true;
    private boolean isFromOpenFileFail = false;
    public static boolean isLocaleChanged = false;

    private int statusBarHeight=0;
    private String saveKey = null;
    private String saveToken = null;


    private DisplayDialogData tempDisplayDialogData = null;
    private AnalyticsObserver mAnalyticsObserver;

    /****add to call feedback app*******/
    private static int topicId;
    private static int forumId;
    private final static String FEED_BACK_PKG = "com.asus.userfeedback";
    public static boolean FeedBackHasInit = false;

    /***add for SAF system********/
    private static final int EXTERNAL_STORAGE_PERMISSION_REQ = 42;

//    private RemoteAccountUtility mRemoteAccountUtility = new RemoteAccountUtility();

    private PopupMenu mPopupMenu;

    // theme promote toast
    private DarkThemeToast mDarkThemeToast = null;

    /****add for MoveToFragment****/
    private MoveToNaviAdapter mMoveToNaviAdapter;

    /**
     * {@link AsyncTask} used to refresh {@link Container}.
     */
    private TagContainerRefreshTask mContainerRefreshTask;

    /**
     * Delay millisecond used to refresh {@link Container} to avoid ANR.
     */
    private static final int REFRESH_TASK_DELAY = 5000;

    public LocalVFile CATEGORY_HOME_PAGE_FILE;
    public LocalVFile CATEGORY_IMAGE_FILE;
    public LocalVFile CATEGORY_MUSIC_FILE;
    public LocalVFile CATEGORY_VIDEO_FILE;
    public LocalVFile CATEGORY_APK_FILE;
    public LocalVFile CATEGORY_FAVORITE_FILE;
    public LocalVFile CATEGORY_COMPRESS_FILE;
    public LocalVFile CATEGORY_DOCUMENT_FILE;
    public LocalVFile CATEGORY_RECENT_FILE;
    public LocalVFile CATEGORY_LARGE_FILE;
    public LocalVFile CATEGORY_PDF_FILE;
    public LocalVFile CATEGORY_GAME_FILE;

    /* 2015/12/30 change to single click to exit
    private boolean doubleBackToExitPressedOnce;
    */

    String mFromNotification = null;
    String mFromShortcut = null;

    public static final String[] SUPPORT_EXTENSION_IN_COMPRESS_CATEGORY
            = new String[] {"zip", "rar"};

    public static final String[] SUPPORT_MIMETYPE_IN_DOCUMENTS_CATEGORY
            = new String[] {
                "text/plain",
                "application/pdf",
                "application/msword",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint"
              };

    public static final String[] SUPPORT_EXTENSION_IN_PPT_CATEGORY
        = new String[] {"ppt", "pptx"};

    public static final String[] SUPPORT_EXTENSION_IN_PDF_CATEGORY
        = new String[] {"application/pdf"};

    // threshold > 100 MB
    public static final long SUPPORT_LARGE_FILES_THRESHOLD = 100*1024*1024;

    // ttl file size to show notification, threshold > 100 MB
    public static final long SUPPORT_FILES_SIZE_THRESHOLD = 200*1024*1024;

    // file count to show notification
    public static final long LARGE_FILES_COUNT_THRESHOLD = 5;

    // recently open max size
    public static final long SUPPORT_RECENTLY_OPEN_SIZE = 100;

    private static final String CONTAINER_ID_PROD = "GTM-NXMR3H";
    private static final int    CONTAINER_RAW_PROD = R.raw.gtm_nxmr3h_v4;
    private static final String CONTAINER_ID_STAGING = "GTM-MF76XR";
    private static final int    CONTAINER_RAW_STAGING = R.raw.gtm_mf76xr;

    public static final String CONTAINER_ID = CONTAINER_ID_PROD;
    public static final int    CONTAINER_RAW = CONTAINER_RAW_PROD;

    private Container mContainer;

    public static final String KEY_FROM_STORAGE_ANALYZER = "KEY_FROM_STORAGE_ANALYZER";
    public static final int REQUEST_CODE_BACK_HOME_FRAGMENT = 1000;
    public static boolean isFromStorageAnalyzer = false;
    public static final String KEY_SWITCH_FRAGMENT_TYPE = "KEY_SWITCH_FRAGMENT_TYPE";

    public boolean isFromOpenFileFail() {
        return isFromOpenFileFail;
    }

    public void setFromOpenFileFail(boolean isFromOpenFileFail) {
        this.isFromOpenFileFail = isFromOpenFileFail;
    }
    public boolean isFromFirst() {
        return isFromFirst;
    }

    public void setFromFirst(boolean isFromFirst) {
        this.isFromFirst = isFromFirst;
    }


    public int getStatusBarH() {
        return statusBarHeight;
    }

    public void isRootDir(boolean isRootDir){

        mIsRootDir = isRootDir;
    }

    public boolean getIsShowSearchFragment(){
        return mIsShowSearchFragment;
    }

    public String getCurrentActionBarTitle() {
        return currentActionBarTitle;
    }

    public void setCurrentActionBarTitle(String currentActionBarTitle) {
        this.currentActionBarTitle = currentActionBarTitle;
    }

    private void initAnimator() {

        Display display = mWindowManager.getDefaultDisplay();
        int width = display.getWidth();

        if (mSlideInAnimator == null) {

            mSlideInAnimator = ObjectAnimator.ofFloat(mShortCutView, "translationX", width * (-1),
                    0);
            mSlideInAnimator.setDuration(250);
            mSlideInAnimator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mShortCutView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

            });
        }
        if (mSlideOutAnimator == null) {
            mSlideOutAnimator = ObjectAnimator.ofFloat(mShortCutView, "translationX", 0, width
                    * (-1));
            mSlideOutAnimator.setDuration(250);
            mSlideOutAnimator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mShortCutView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    public void closeShortCut() {
        if (mSlideOutAnimator != null && !mSlideOutAnimator.isRunning()) {
            mSlideOutAnimator.start();
        }
    }

    public enum FragmentType {
        NORMAL_SEARCH(R.id.searchlist),
        HOME_PAGE(R.id.homepage),
        FILE_LIST(R.id.filelist),
        RECYCLE_BIN("RecycleBinFragment"),
        HIDDEN_ZONE("HiddenZoneFragment");
        int fragmentId;
        String fragmentTag = null;
        String fragmentTitle = null;

        FragmentType(int id) {
            fragmentId = id;
        }
        FragmentType(String tag) {
            fragmentTag = tag;
        }

        public int getFragmentId() {
            return fragmentId;
        }

        public String getFragmentTag() {
            return fragmentTag;
        }

        void setFragmentTitle(String title) {
            fragmentTitle = title;
        }

        public String getFragmentTitle() {
            return fragmentTitle;
        }

        static void updateFragmentTitle(Context context) {
            HOME_PAGE.setFragmentTitle(context.getResources().getString(R.string.file_manager));
            RECYCLE_BIN.setFragmentTitle(context.getResources().getString(R.string.tools_recycle_bin));
            HIDDEN_ZONE.setFragmentTitle(context.getResources().getString(R.string.tools_hidden_zone));
        }
    }

    private PermissionManager permissionManager;

    public FileManagerActivity() {
        permissionManager = new PermissionManager(this);
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG)
            Log.d(TAG, "onCreate");
        FragmentType.updateFragmentTitle(this);
        // ---
        robotoLightTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        robotoMediumTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
        robotoRegularTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        super.onCreate(savedInstanceState);

        initCategoryFile();
        ColorfulLinearLayout.setContentView(this, R.layout.my_filemanager, R.color.theme_color);
        ToolbarUtil.setupToolbar(this);
        //setContentView(R.layout.my_filemanager);

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // avoid font size changed
        final Configuration conf = getResources().getConfiguration();
        //conf.fontScale = 1;
        //getResources().updateConfiguration(conf, getResources().getDisplayMetrics());

        //++ Alex
        mIsPadMode = conf.smallestScreenWidthDp > 1080;
        mIsPadMode2 = conf.smallestScreenWidthDp >= 600;
        if (DEBUG) {
            Log.d(TAG, "onCreate - IsPadMode = " + mIsPadMode);
        }
        //--

        // +++ Johnson
        //mDividerRight = findViewById(R.id.divider_right);
        mSortNameView = (LinearLayout) findViewById(R.id.sort_name_container);
        mSortSizeView = (LinearLayout) findViewById(R.id.sort_size_container);
        mSortDateView = (LinearLayout) findViewById(R.id.sort_date_container);
        mRightContainer = (FrameLayout) findViewById(R.id.fragment_container);
        mRightContainer.setOnTouchListener(this);
        // ---

        Intent intent = getIntent();

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
            mIsAttachOp = true;
        }

        if (intent.getAction() != null && intent.getAction().equals(ACTION_MULTIPLE_SELECTION)) {
            mIsMultipleSelectionOp = true;
        }

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            if(intent.getType() != null && intent.getType().equals("application/zip")){
                mIsFirstUnZipIntent = true;
            }
        }

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_CREATE_SHORTCUT)) {
            mIsCreateShortcutIntent = true;
        }

        String path = null;
        path = intent == null ? path : intent.getStringExtra("path");
        if (path != null){
            mGoToPath = new String (path);

            // [TT-751044] by Tim
            if(mGoToPath.startsWith(Environment.getExternalStorageDirectory().getPath())){
                   mGoToPath = mGoToPath.replace(Environment.getExternalStorageDirectory().getPath(), "sdcard");
            }

            //remove path from intent to prevent app resume show wrong page state
            getIntent().removeExtra("path");
            //we got new path
            mFromShortcut = GaPromote.PROMOTE_FROM_SHORTCUT;
        }

        mFromNotification = intent ==null ? mFromNotification : intent.getStringExtra("ga");

        if (savedInstanceState != null) {
            mIsShowSearchFragment = savedInstanceState.getBoolean("showSearchFragment");
            mIsFirstUnZipIntent = savedInstanceState.getBoolean("mIsFirstUnZipIntent");
            mIsCreateShortcutIntent = savedInstanceState.getBoolean("mIsCreateShortcutIntent");
            currentActionBarTitle = savedInstanceState.getString("currentActionBarTitle");
            mQuery = savedInstanceState.getString("mQuery");
           SambaFileUtility.updateHostIp = savedInstanceState.getBoolean("showPcInLan");// for samba

           mIsShowHomePageFragment = savedInstanceState.getBoolean("showHomePageFragment");
            mCurrentFragmentType = (FragmentType)savedInstanceState.getSerializable("currentFragment");
            if (mCurrentFragmentType == FragmentType.HIDDEN_ZONE) {
                mCurrentFragmentType = FragmentType.HOME_PAGE;
            }
        } else {
            mIsShowHomePageFragment = true;
            mCurrentFragmentType = FragmentType.HOME_PAGE;
        }

        initActionBar();

        if(!mIsPadMode){
            initDrawerLayout();
            initStorageDrawerList();
        }

        if (savedInstanceState != null) {
            currentActionBarTitle = savedInstanceState.getString("currentActionBarTitle");
            //++tim_hu
            mSelectedStorage = savedInstanceState.getString("mSelectedStorage");
        }
        if (currentActionBarTitle != null &&  !currentActionBarTitle.equals(getResources().getString(R.string.file_manager))) {
            Log.d(TAG, "currentActionBarTitle" + currentActionBarTitle);
            setActionBarTitle(currentActionBarTitle);
        }
        if(mSelectedStorage != null && mStorageListAdapter !=null){
            mStorageListAdapter.setSelectedStorage(mSelectedStorage, null);
        }
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mShortCutFragment = (ShortCutFragment) fragmentManager
                .findFragmentByTag(sShortCutFragmentTag);
        if (mShortCutFragment == null) {
            if (DEBUG) {
                Log.d(TAG, "Add ShortCutFragment()");
            }
            mShortCutFragment = new ShortCutFragment();
            fragmentTransaction.add(mShortCutFragment, sShortCutFragmentTag);
        }

        try {
            fragmentTransaction.commit();
        } catch (Exception e) {
                e.printStackTrace();
        }

        mSortSizeView.setVisibility(View.GONE);

        // +++ Johnson
        DIVIDE_LIMITID = (int) getResources().getDimension(R.dimen.divide_limitid);
        LEFT_BOUND_LAND = (int) getResources().getDimension(R.dimen.left_bound_land);
        RIGHT_BOUND_LAND = (int) getResources().getDimension(R.dimen.right_bound_land);
        LEFT_BOUND_PORT = (int) getResources().getDimension(R.dimen.left_bound_port);
        RIGHT_BOUND_PORT = (int) getResources().getDimension(R.dimen.right_bound_port);

        mDraggingMinLeft_land = (int) getResources().getDimension(R.dimen.dragging_min_left_land);
        mDraggingMinLeft_port = (int) getResources().getDimension(R.dimen.dragging_min_left_port);
        mDraggingLeftBound = (int) getResources().getDimension(R.dimen.dragging_left_bound);

        // bind remote server
        mRemoteClientMessenger = new Messenger(RemoteClientHandler.getInstance(this));
        //RemoteAccountUtility.initParams();
        if (savedInstanceState != null) {
            if(WrapEnvironment.isAZSEnable(this)){
                int azsCheckStatusCode = aZSVersionCheck();

                if(azsCheckStatusCode == VersionChecker.VERSION_STATUS.COMPATABLE || azsCheckStatusCode == VersionChecker.VERSION_STATUS.MATCH){
                    RemoteAccountUtility.getInstance(this).init();
                    doBindService();
                }
                saveKey = savedInstanceState.getString("accountInfoKey");
                saveToken = savedInstanceState.getString("accountInfoToken");

            }else{
                RemoteAccountUtility.getInstance(this).init();
                saveKey = savedInstanceState.getString("accountInfoKey");
                saveToken = savedInstanceState.getString("accountInfoToken");
                doBindService();
            }

        }

        FeedBackHasInit = false;

        SambaFileUtility.getInstance(this).init(this);

        mMoveToNaviAdapter = new MoveToNaviAdapter(this);


        SetNetworkEnabled(false);

        if (FileUtility.isLowStorage(getIntent())){
            GaPromote.getInstance().sendEvents(this, GaPromote.PROMOTE_LOW_STORAGE,
                GaPromote.PROMOTE_CLICK_ACTION,null , null);

            Intent tutorialIntent = new Intent();
            tutorialIntent.setClass(this, LowStorageTutorialActivity.class);
            startActivityForResult(tutorialIntent, REQUEST_TUTORIAL);
        } else if (FileUtility.isFirstStartup(this) || FileUtility.isVerizonTipsStartup(this)) {

            // If now is in multi window mode, we skip showing tutorial page,
            // and do the rest work directly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                if(isInMultiWindowMode())
                {
                    FileUtility.saveFirstStartup(this);
                    MediaScannerBroadcastReceiver.scheduleAlarm(this);
                    handleRequestTutorial();
                    return;
                }
            }

            Log.d(TAG, "isFirstStartup start TutorialActivity");
            Intent tutorialIntent = new Intent();
            tutorialIntent.setClass(this, TutorialActivity.class);
            startActivityForResult(tutorialIntent, REQUEST_TUTORIAL);
        } else {

//            int status = CtaDialogFragment.checkCtaPermission(this);
//            if (status == CtaDialogFragment.CTA_REMEMBER_AGREE) {
//                SetNetworkEnabled(true);
//            }else if (status == CtaDialogFragment.CTA_REMEMBER_REFUSE){
//                SetNetworkEnabled(false);
//            }

            int status = CtaPreGrantDialogFragment.checkCtaPreGrantPermission(this);
            if (status == CtaPreGrantDialogFragment.CTA_REMEMBER_AGREE) {
                SetNetworkEnabled(true);
            }else if (status == CtaPreGrantDialogFragment.CTA_REMEMBER_REFUSE){
                SetNetworkEnabled(false);
            }
        }
        MediaScannerBroadcastReceiver.scheduleAlarm(this);

        mDarkThemeToast = new DarkThemeToast(this);
    }

    private void SetNetworkEnabled(boolean isEnable) {

        mAnalyticsObserver = new AnalyticsObserver(null);
        AnalyticsReflectionUtility.registerContentObserver(this, mAnalyticsObserver);

        if(WrapEnvironment.IS_VERIZON || ItemOperationUtility.getInstance().enableCtaCheck()){
            isEnable = false;
        }

        boolean isGAEnable = false;
        if (!isEnable) {
            isGAEnable = false;
        } else {
            isGAEnable = AnalyticsReflectionUtility.getEnableAsusAnalytics(this);
        }

        Log.d(TAG, "Set GA enable? : " + isGAEnable);
        GoogleAnalytics.getInstance(this).setAppOptOut(!isGAEnable);
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(isGAEnable);

        if (isEnable) {
            if (null != mFromNotification){
                Log.d(TAG, "send ga  : " + mFromNotification);
                GaPromote.getInstance().sendEvents(this, mFromNotification,
                    GaPromote.PROMOTE_CLICK_ACTION, null, null);
            }
            if (null != mFromShortcut){
                Log.d(TAG, "send ga  : " + mFromShortcut);
                GaPromote.getInstance().sendEvents(this, mFromShortcut,
                        GaPromote.PROMOTE_CLICK_ACTION, null, null);
            }
            ZenUiFamily.setGAEnable(true);
            updateGTM();
        }else{
            ZenUiFamily.setGAEnable(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    VersionChecker.OnDialogEventListener onDialogEventListener = new OnDialogEventListener() {
        @Override
        public void onNagativeClick(int arg0) {

        }

        @Override
        public void onPositiveClick(int arg0) {
                switch (arg0) {
            case VersionChecker.VERSION_STATUS.APK_NOT_FOUND:
                 Intent aZSUpdateIntent = VersionChecker.getAZSUpdateIntent();
                 Log.d(TAG, "" + aZSUpdateIntent);
                 startActivity(aZSUpdateIntent);
                 break;
            case VersionChecker.VERSION_STATUS.CLIENT_NOT_SUPPORT:
                 Intent clientUpdateIntent = VersionChecker.getClientUpdateIntent(getPackageName());
                 Log.d(TAG, "" + clientUpdateIntent);
                 startActivity(clientUpdateIntent);
                 break;
            case VersionChecker.VERSION_STATUS.SERVER_NOT_SUPPORT:
                 aZSUpdateIntent = VersionChecker.getAZSUpdateIntent();
                 Log.d(TAG, "" + aZSUpdateIntent);
                 startActivity(aZSUpdateIntent);
                 break;
            default:
                 break;
            }

        }
  };

    public void  addSavedTokenToAccountsMap(){
        if (RemoteAccountUtility.getInstance(this).accountsMap != null && saveKey != null && saveToken != null) {
            AccountInfo info = RemoteAccountUtility.getInstance(this).accountsMap.get(saveKey);
            if (info != null && (info.getToken() == null || info.getToken().equals(""))) {
                info.setToken(saveToken);
                RemoteAccountUtility.getInstance(this).accountsMap.put(saveKey, info);
                saveKey = null;
                saveToken = null;
            }
        }
    }

    public void setTextViewFont(TextView view ,int fontType){
        Typeface typeface = null;
        switch (fontType) {
        case FontType.ROBOTO_LIGHT:
            typeface = robotoLightTypeface;
            break;
        case FontType.ROBOTO_MEDIUM:
            typeface = robotoMediumTypeface;
            break;
        case FontType.ROBOTO_REGULAR:
            typeface = robotoRegularTypeface;
            break;

        default:
            break;
        }
        if (view != null && typeface != null) {
            view.setTypeface(typeface);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(!mIsPadMode)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Pass any configuration change to the drawer toggls
        super.onConfigurationChanged(newConfig);

        if (!mIsPadMode) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
        // FIXME:
        // Need to setAdapter?
//      if(newConfig.smallestScreenWidthDp>=600){
//            if (mStorageDrawerList != null && mStorageListAdapter!=null) {
//                LayoutParams params = mStorageDrawerList.getLayoutParams();
//                params.width = getResources().getDimensionPixelSize(R.dimen.drawer_width);
//                mStorageDrawerList .setLayoutParams(params);
//                mStorageDrawerList.setAdapter(mStorageListAdapter);
//            }
//        }
        mDrawerLayout.requestLayout();

        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }

        WhatsNewDialogFragment whatsNewDialog = (WhatsNewDialogFragment) getFragmentManager().
                findFragmentByTag(WhatsNewDialogFragment.DIALOG_TAG);

        if (whatsNewDialog != null && whatsNewDialog.isAdded())
        {
            whatsNewDialog.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onAttachedToWindow() {
        if (!mIsPadMode) {
            attachShortCutOverlay();
            mShortCutView.setVisibility(View.INVISIBLE);
        }
        super.onAttachedToWindow();
    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume");

        // check whether need to switch theme on run time
        if(ThemeUtility.isNeedToSwitchTheme(this)) {
            finish();
            startActivity(getIntent());
        }

        /* 2015/12/30 change to single click to exit
        doubleBackToExitPressedOnce = false;
        */

        RemoteAccountUtility.getInstance(this).loadParameters();

        if (mShortCutView == null) {
            int paddingTop = getResources().getDimensionPixelSize(
                R.dimen.shortcut_paddingTop);
            mShortCutView = new ShortCutFrame(this);
            mShortCutView.addView(mShortCutFragment.getView());
            mShortCutView.setPadding(0, paddingTop, 0, 0);
        }
        initAnimator();

        if (! (FileUtility.isLowStorage(getIntent()) || FileUtility.isFirstStartup(this))){
            Map<String, Integer> map = new HashMap<>();
            if (!(PermissionManager.checkPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {
                map.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_essential);
                map.put(Manifest.permission.READ_EXTERNAL_STORAGE, R.string.permission_essential);
                //map.put(Manifest.permission.GET_ACCOUNTS, R.string.permission_essential);
            /* don't request contact permission when app started
            }else{
                if (!(PermissionManager.checkPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS})) && !mPermissionRequested) {
                    map.put(Manifest.permission.GET_ACCOUNTS, R.string.permission_essential);
                }
                */
            }
            PermissionDialog permissionDialog = (PermissionDialog) getFragmentManager().findFragmentByTag(PermissionDialog.TAG);
            if (map.size() > 0 && !(null != permissionDialog && permissionDialog.isAdded())) {
                if (permissionManager.requestPermissions(map, PermissionManager.REQUEST_PERMISSION)) {
                    //permission granted
                    PermissionDialog dialogFragment = (PermissionDialog) (getFragmentManager().findFragmentByTag(PermissionDialog.TAG));
                    if (null != dialogFragment && dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()){
                        dialogFragment.dismiss();
                    }
                    PermissionReasonDialogFragment dialogFragment2 = (PermissionReasonDialogFragment) (getFragmentManager().findFragmentByTag(PermissionReasonDialogFragment.TAG));
                    if (null != dialogFragment2 && dialogFragment2.getDialog() != null && dialogFragment2.getDialog().isShowing()){
                        dialogFragment2.dismiss();
                    }
                    postOnResume();
                    showWhatsNew();
                }
            }
        }
        if ((PermissionManager.checkPermissions(this,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {

            PermissionDialog dialogFragment = (PermissionDialog) (getFragmentManager().findFragmentByTag(PermissionDialog.TAG));
            PermissionReasonDialogFragment dialogFragment2 = (PermissionReasonDialogFragment) (getFragmentManager().findFragmentByTag(PermissionReasonDialogFragment.TAG));
            if (null != dialogFragment && dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()){
               if (dialogFragment.getRequest() == PermissionManager.RE_REQUEST_PERMISSION){
                   if (PermissionManager.checkPermissions(this,new String[]{Manifest.permission.GET_ACCOUNTS})){
                       dialogFragment.dismiss();
                       if (null != dialogFragment2 && dialogFragment2.getDialog() != null && dialogFragment2.getDialog().isShowing()){
                           dialogFragment2.dismiss();
                       }
                   }
               }else {
                   dialogFragment.dismiss();
                   if (null != dialogFragment2 && dialogFragment2.getDialog() != null && dialogFragment2.getDialog().isShowing()) {
                       dialogFragment2.dismiss();
                   }
               }
            }
            //we have minimum permissions to run this app
            postOnResume();
            showWhatsNew();
        }else{
            showSearchFragment(FragmentType.HOME_PAGE, true);
        }
    }

    private void postOnResume() {


        Intent intent = getIntent();
        if (null != intent){
        int categoryItemId = intent.getIntExtra("categoryItemId", -1);
        isFromStorageAnalyzer = intent.getBooleanExtra(KEY_FROM_STORAGE_ANALYZER,false);

            if (mIsFirstUnZipIntent) {
                processUnZipIntent(intent);
                mCurrentFragmentType = FragmentType.FILE_LIST;
            }else if (mIsCreateShortcutIntent) {
                Bundle bundle = new Bundle();
                bundle.putString(MoveToDialogFragment.DIALOG_TITLE, getResources().getString(R.string.create_shortcut));
                bundle.putBoolean(FilePickerDialogFragment.KEY_FROM_CREATE_SHORTCUT, true);
                displayDialog(DialogType.TYPE_FILE_PICKER_DIALOG, bundle);
            }else if (mIsAttachOp || mIsMultipleSelectionOp){
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                    fileListFragment.onNewIntent(intent);
                }
                showSearchFragment(FragmentType.NORMAL_SEARCH, false);
            } else if (!FileManagerActivity.isSearchIng && null != mGoToPath){
                FragmentManager manager = getFragmentManager();
                VFile file = new VFile(new File(mGoToPath));
                FileListFragment fileListFragment =(FileListFragment) manager.findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                    fileListFragment.goToLocation(file);
                }
                //reset mGoToPath to prevent app resume show wrong page state
                mGoToPath = null;
            } else if (!FileManagerActivity.isSearchIng && categoryItemId >= 0) {
                LocalVFile category = null;
                switch (categoryItemId) {
                case CategoryItem.IMAGE:
                    category = CATEGORY_IMAGE_FILE;
                    break;
                case CategoryItem.MUSIC:
                    category = CATEGORY_MUSIC_FILE;
                    break;
                case CategoryItem.VIDEO:
                    category = CATEGORY_VIDEO_FILE;
                    break;
                case CategoryItem.APP:
                    category = CATEGORY_APK_FILE;
                    break;
                case CategoryItem.FAVORITE:
                    category = CATEGORY_FAVORITE_FILE;
                    break;
                case CategoryItem.DOWNLOAD:
                    category = new LocalVFile(PATH_DOWNLOAD);
                    break;
                case CategoryItem.DOCUMENT:
                    category = CATEGORY_DOCUMENT_FILE;
                    break;
                case CategoryItem.COMPRESSED:
                    category = CATEGORY_COMPRESS_FILE;
                    break;
                case CategoryItem.RECENT:
                    category = CATEGORY_RECENT_FILE;
                    break;
                case CategoryItem.LARGE_FILE:
                    category = CATEGORY_LARGE_FILE;
                    break;
                case CategoryItem.PDF:
                    category = CATEGORY_PDF_FILE;
                    break;
                case CategoryItem.GAME:
                    category = CATEGORY_GAME_FILE;
                    break;
                }

                FragmentManager manager = getFragmentManager();
                FileListFragment fileListFragment =(FileListFragment) manager.findFragmentById(R.id.filelist);
                if (fileListFragment != null && category != null) {
                    fileListFragment.goToLocation(category);
                }
                //remove categoryItemId from intent to prevent app resume show wrong page state
                intent.removeExtra("categoryItemId");
            }

            if(isFromStorageAnalyzer) {

                Serializable serializable = intent.getSerializableExtra(KEY_SWITCH_FRAGMENT_TYPE);
                if(serializable!=null)
                {
                    mCurrentFragmentType = ((FragmentType)serializable);
                    intent.removeExtra(KEY_SWITCH_FRAGMENT_TYPE);
                }
                intent.removeExtra(KEY_FROM_STORAGE_ANALYZER);
            }
        }

        mIsResume = true;

        if (tempDisplayDialogData != null) {
            displayDialog(tempDisplayDialogData.type, tempDisplayDialogData.arg);
            tempDisplayDialogData = null;
        }

        switchFragmentTo(mCurrentFragmentType);

        // cloud storage case
        // RemoteFileUtility.startUpdateCloudStorage();
        SambaFileUtility.getInstance(this).onResume(mIsAttachOp, this);
        RemoteClientHandler.getInstance(this).onResume();
        RemoteAccountUtility.getInstance(this).onResume();

//        int status = CtaDialogFragment.checkCtaPermission(this, false);
//        if (status == CtaDialogFragment.CTA_REMEMBER_AGREE) {
//            GaActiveUser.getInstance(FileManagerActivity.this).sendEvents(FileManagerActivity.this,
//                    GaActiveUser.CATEGORY_NAME, GaActiveUser.ACTION_ON_START_ACTIVITY, null, null);
//            GaSettingsPage.getInstance()
//                    .sendPreferenceSwitchDefault(FileManagerActivity.this);
//            sendSecondaryStorageNameToGa();
//        }
        int status = CtaPreGrantDialogFragment.checkCtaPreGrantPermission(this,false);
        if (status == CtaPreGrantDialogFragment.CTA_REMEMBER_AGREE) {
            GaActiveUser.getInstance(FileManagerActivity.this).sendEvents(FileManagerActivity.this,
                    GaActiveUser.CATEGORY_NAME, GaActiveUser.ACTION_ON_START_ACTIVITY, null, null);
            GaSettingsPage.getInstance()
                    .sendPreferenceSwitchDefault(FileManagerActivity.this);
            sendSecondaryStorageNameToGa();
        }
    }

    public void validateIsTheLocaleChangeedOnResume(){
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment!=null) {
            fileListFragment.validateIsTheLocaleChangeedOnResume();
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG)
            Log.d(TAG,"onDestroy");
        deAttachShortCutOverlay();
        Thumbnail.updateDb(this);

        // unbind remote server

        try {
            doUnbindService(false);
//            if (!(mIsAttachOp || mIsMultipleSelectionOp)) {
                RemoteAccountUtility.getInstance(this).destory();
                sRemoteService = null;
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //destroy samba handler
        SambaFileUtility.getInstance(this).destroy(mIsAttachOp);
        AnalyticsReflectionUtility.unregisterContentObserver(this, mAnalyticsObserver);
        FeedBackHasInit = false;

        // If background container task is still running, maybe we meet the deadlock,
        // just cancel the task.
        if(mContainerRefreshTask != null && mContainerRefreshTask.getStatus() != Status.FINISHED)
        {
            Log.i(TAG, "onDestroy, task is running, cancel it");
            mContainerRefreshTask.cancel(true);
        }
        mContainerRefreshTask = null;

        // destroy theme promote toast
        if(mDarkThemeToast != null) {
            mDarkThemeToast.cancel();
            mDarkThemeToast = null;
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG,"onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (mStorageListAdapter != null && mStorageListAdapter.getSeclectedStorage() != null) {
            outState.putString("mSelectedStorage", mStorageListAdapter.getSeclectedStorage());
        }
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        // WifiDirectSearchFragment wifiSearchFragment = (WifiDirectSearchFragment) getFragmentManager().findFragmentById(R.id.wifidirectsearchlist);
        outState.putBoolean("showSearchFragment", mIsShowSearchFragment);
        // outState.putBoolean("showWifiDirectSearchFragment", !wifiSearchFragment.isHidden());
        outState.putBoolean("showHomePageFragment", mIsShowHomePageFragment);
        outState.putSerializable("currentFragment", mCurrentFragmentType);
        outState.putBoolean("mIsFirstUnZipIntent", mIsFirstUnZipIntent);
        outState.putBoolean("mIsCreateShortcutIntent", mIsCreateShortcutIntent);
        outState.putBoolean("showPcInLan", SambaFileUtility.updateHostIp);
        if (currentActionBarTitle != null) {
            Log.d(TAG, currentActionBarTitle);
            outState.putString("currentActionBarTitle", currentActionBarTitle);
        }

        outState.putString("mQuery", mQuery);
        VFile currentFile = fileListFragment.getmIndicatorFile();
        if (currentFile != null && currentFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            RemoteVFile tempVfile = (RemoteVFile)currentFile;
            String key = tempVfile.getStorageName() + "_" + tempVfile.getMsgObjType();
            //Log.d(TAG, "key:"+key);
            if (RemoteAccountUtility.getInstance(this).accountsMap.size()>0) {
                AccountInfo info = RemoteAccountUtility.getInstance(this).accountsMap.get(key);
                if (info != null && info.getToken() != null) {
                    outState.putString("accountInfoKey", key);
                    outState.putString("accountInfoToken", info.getToken());
                    //Log.d(TAG, "accountInfoKey:"+key);
                    //Log.d(TAG, "accountInfoToken:"+info.getToken());
                }
            }
        }

        RemoteAccountUtility.getInstance(this).saveParameters();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (DEBUG)
            Log.d(TAG, "onNewIntent");
        String action = intent.getAction();
        if(action == null){
            return;
        }
        Log.d(TAG, action);
        if (action.equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            FileManagerActivity.isSearchIng =true;
            mSearchView.setQuery(query,false);
            mSearchView.clearFocus();

            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.updateResult(null, query, null);
            showSearchFragment(FragmentType.NORMAL_SEARCH, true);
            displayDialog(DialogType.TYPE_SEARCH_DIALOG, query);
        } else if (action.equals(Intent.ACTION_VIEW ) && intent.getType().equals("application/zip")) {
            processUnZipIntent(intent);
        } else {
            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            mNewIntent = intent;
            if (fileListFragment != null) {
                fileListFragment.onNewIntent(intent);
            }
            /*if(mIsShowHomePageFragment){
                showSearchFragment(FragmentType.HOME_PAGE, true);
            }else{
                showSearchFragment(FragmentType.NORMAL_SEARCH, false);
            }*/
            /*if (getCurrentFragmentType() == FragmentType.NORMAL_SEARCH)
                switchFragmentTo(FragmentType.FILE_LIST);
            else*/
                switchFragmentTo(getCurrentFragmentType());
        }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        if(!mIsPadMode){
            //boolean drawerOpen = mDrawerView.isDrawerOpen(mStorageDrawerList);
            MenuItem searchItem = menu.findItem(R.id.search_action);
            // FIXME:
            // workaround for asus support library set icon fail in xml
            MenuItem addFolderItem = menu.findItem(R.id.add_folder_action);
            MenuItem sortItem = menu.findItem(R.id.sort_item);
            MenuItem clearHistoryItem = menu.findItem(R.id.clear_history_action);
            MenuItem bugreportItem = menu.findItem(R.id.action_bug_report);
            MenuItem ctaDialogItem = menu.findItem(R.id.cta_dialog);
            // sylvia++ 20160322
            MenuItem settingItem = menu.findItem(R.id.action_settings);

            if(searchItem != null){
                //searchItem.setIcon(getResources().getDrawable(R.drawable.asus_ep_ic_search));
                if (mIsAttachOp || mIsMultipleSelectionOp) {
                    searchItem.setVisible(false);
                }else {
                    searchItem.setVisible(mDrawerSlideOffset == 0);
                }
            }

            if(clearHistoryItem != null){
                if (mIsAttachOp || mIsMultipleSelectionOp) {
                    clearHistoryItem.setVisible(false);
                }else{
//                    clearHistoryItem.setVisible(mDrawerSlideOffset == 0);
                    //TT-825414
                    clearHistoryItem.setVisible(true);
                }
            }

            if( addFolderItem != null ){
                if(mIsRootDir || mIsAttachOp || mIsMultipleSelectionOp){
                    addFolderItem.setVisible(false);
                }else{
                    addFolderItem.setVisible(mDrawerSlideOffset == 0);
                }

            }

            if(sortItem != null){
                  if(mIsRootDir){
                    sortItem.setVisible(false);
                }else{
                    sortItem.setVisible(mDrawerSlideOffset == 0);
                }
            }

            if(bugreportItem != null){
                 if (mIsAttachOp || mIsMultipleSelectionOp) {
                     bugreportItem.setVisible(false);
                 }else{
                     bugreportItem.setVisible(true);
                 }
            }

            if(ctaDialogItem != null){
                if (mIsAttachOp || mIsMultipleSelectionOp || !ItemOperationUtility.getInstance().enableCtaCheck()) {
                    ctaDialogItem.setVisible(false);
                }else{
                    ctaDialogItem.setVisible(true);
                }
            }

            // sylvia++ 20160322
            if(settingItem != null){
                settingItem.setVisible(true);
            }
        }
        if(SambaFileUtility.updateHostIp || RemoteFileUtility.isShowDevicesList){
            MenuItem tempMenu ;
            tempMenu = menu.findItem(R.id.search_action);
            if(tempMenu!=null)tempMenu.setVisible(false);
            tempMenu = menu.findItem(R.id.add_folder_action);
            if (tempMenu != null )tempMenu.setVisible(false);
            tempMenu = menu.findItem(R.id.sort_item);
            if (tempMenu != null) tempMenu.setVisible(false);
            tempMenu = menu.findItem(R.id.clear_history_action);
            if (tempMenu != null) tempMenu.setVisible(false);
            tempMenu = menu.findItem(R.id.action_bug_report);
            if (tempMenu != null) tempMenu.setVisible(false);
            tempMenu = menu.findItem(R.id.cta_dialog);
            if (tempMenu != null) tempMenu.setVisible(false);
            //menu.findItem(R.id.hide_system_file_action).setVisible(false);
        }else {
            MenuItem tempMenu ;
            tempMenu = menu.findItem(R.id.search_action);
            if(tempMenu!=null)tempMenu.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (item.getItemId() == android.R.id.home) {
          Log.d("home","home");
        } else if (item.getItemId() == R.id.clear_history_action) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
            suggestions.clearHistory();
            GaMenuItem.getInstance().sendEvents(this,GaMenuItem.CATEGORY_NAME, GaMenuItem.ACTION_CLEAR_SEARCH_HISTORY, null, null);
            return true;
        }else if (item.getItemId() == R.id.action_rateus){
            displayDialog(DialogType.TYPE_RATE_US_DIALOG, null);
            SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_rateus", false).commit();
            GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_ENCOURAGE_US, null, null);
            invalidateOptionsMenu();
            return true;
            //show_rateus();
        /*
        }else if (item.getItemId() == R.id.action_share){
            InviteHelper.invite(this, getProfileDisplayname());
            GaMenuItem.getInstance(mFMActivity).sendEvents(mFMActivity, GaMenuItem.CATEGORY_NAME,
                    GaMenuItem.ACTION_TELL_FRIENDS, null, null);
        }else if (item.getItemId() == R.id.action_tell_a_friend){
            InviteHelper.inviteFB(this);
            GaMenuItem.getInstance(mFMActivity).sendEvents(mFMActivity, GaMenuItem.CATEGORY_NAME,
                GaMenuItem.ACTION_INVITEFB, null, null);
        */
        //chenhsin++
        }else if (item.getItemId() == R.id.action_tell_a_friend){
            startActivity(new Intent(FileManagerActivity.this,FileManagerInviteActivity.class));
        }else if (item.getItemId() == R.id.action_instant_update){
            if (Utility.isMonkeyRunning()) {
                return true;
            }
            //show_instant_update();
            ZenUiFamily.launchZenUiFamily(this);
            GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_INSTANT_UPDATE, null, null);
        }else if (item.getItemId() == R.id.action_bug_report){
            if (Utility.isMonkeyRunning()) {
                return true;
            }
            if(ItemOperationUtility.getInstance().checkCtaPermission(this)) {
                initFeedBackResource(this);
                zipLocatInfo();
                UserVoice.launchBugReport(this);
                GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_REPORT_BUG, null, null);
            }
            return true;
        // sylvia++ 20160322
        }else if (item.getItemId() == R.id.action_settings) {
            SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_settings", false).commit();
            invalidateOptionsMenu();
            startActivity(new Intent(FileManagerActivity.this, FileManagerSettingActivity.class));
            return true;
        }else if (item.getItemId() == R.id.about_action){
            Intent AboutIntent = new Intent();
            AboutIntent.setClass(this, FileManagerAboutActivity.class);
            startActivityForResult(AboutIntent, REQUEST_ABOUT);
            GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_ABOUT, null, null);
            return true;
            /*
            displayDialog(DialogType.TYPE_ABOUT_DIALOG, null);
            return true;
            */
        }else if (item.getItemId() == R.id.action_invite_betauser){
            show_recruit_beta_user();
            invalidateOptionsMenu();
            return true;
        }else if (item.getItemId()  == R.id.saf_tutorial_action){
            Intent tutorialIntent = new Intent();
            tutorialIntent.setClass(this, TutorialActivity.class);
            tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
            startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
            return true;
        }else if (item.getItemId() == R.id.cta_dialog) {
            ItemOperationUtility.getInstance().showCtaDialog(this);
            return true;
        }
        if(!mIsPadMode){
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onPopupMenuItemClick(int id){
        if (id == android.R.id.home) {
            Log.d("home","home");
        }else if (id == R.id.action_rateus){
            displayDialog(DialogType.TYPE_RATE_US_DIALOG, null);
            SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_rateus", false).commit();
            GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_ENCOURAGE_US, null, null);
            return ;
        /*
        }else if (id == R.id.action_tell_a_friend){
            InviteHelper.inviteFB(this);
            GaMenuItem.getInstance(mFMActivity).sendEvents(mFMActivity, GaMenuItem.CATEGORY_NAME,
                    GaMenuItem.ACTION_INVITEFB, null, null);
        }else if (id == R.id.action_share){
            InviteHelper.invite(this, getProfileDisplayname());
            GaMenuItem.getInstance(mFMActivity).sendEvents(mFMActivity, GaMenuItem.CATEGORY_NAME,
                GaMenuItem.ACTION_TELL_FRIENDS, null, null);
        */
        //chenhsin++
        }else if (id == R.id.action_tell_a_friend){
            startActivity(new Intent(FileManagerActivity.this,FileManagerInviteActivity.class));
        }else if (id == R.id.action_instant_update){
            //show_instant_update();
            if (Utility.isMonkeyRunning()) {
                return;
            }
            ZenUiFamily.launchZenUiFamily(this);
            GaMenuItem.getInstance().sendEvents(this, GaMenuItem.CATEGORY_NAME,GaMenuItem.ACTION_INSTANT_UPDATE, null, null);
            return;
        }else if (id == R.id.action_bug_report){
            if (Utility.isMonkeyRunning()) {
                return;
            }
            if(ItemOperationUtility.getInstance().checkCtaPermission(this)) {
                initFeedBackResource(this);
                zipLocatInfo();
                UserVoice.launchBugReport(this);
                GaMenuItem.getInstance().sendEvents(this,GaMenuItem.CATEGORY_NAME, GaMenuItem.ACTION_REPORT_BUG, null, null);
            }
            return ;
        }else if (id == R.id.clear_history_action){
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
            suggestions.clearHistory();
            GaMenuItem.getInstance().sendEvents(this,GaMenuItem.CATEGORY_NAME, GaMenuItem.ACTION_CLEAR_SEARCH_HISTORY, null, null);
            return;
        }else if (id == R.id.action_invite_betauser){
            show_recruit_beta_user();
            SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_beta_user", false).commit();
            return ;
        // sylvia++ 20160322
        }else if (id == R.id.action_settings){
            SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_settings", false).commit();
            invalidateOptionsMenu();
            startActivity(new Intent(FileManagerActivity.this, FileManagerSettingActivity.class));
            return;
        }else if (id == R.id.about_action){
            Intent aboutIntent = new Intent();
            aboutIntent.setClass(this, FileManagerAboutActivity.class);
            startActivityForResult(aboutIntent, REQUEST_ABOUT);
            GaMenuItem.getInstance().sendEvents(this,GaMenuItem.CATEGORY_NAME, GaMenuItem.ACTION_ABOUT, null, null);
            return ;
        }else if (id == R.id.cta_dialog){
            ItemOperationUtility.getInstance().showCtaDialog(this);
            return;
        }else if (id == R.id.saf_tutorial_action){
            Intent tutorialIntent = new Intent();
            tutorialIntent.setClass(this, TutorialActivity.class);
            tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
            startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
            return;
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            hideSoftKeyboard();
        }
    }

    private String getProfileDisplayname() {
        return "";
    }

    public void setReSearchQueryKey(String searchkey){
        this.mQuery = searchkey;
    }

    public String getSearchQueryKey(){
        return this.mQuery;
    }

    public void reSearch(String key){
    SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
        searchResultFragment.updateResult(null, key, null);
        showSearchFragment(FragmentType.NORMAL_SEARCH, true);
        if(mQuery != null){
        displayDialog(DialogType.TYPE_SEARCH_DIALOG, mQuery);
        }
        Log.d(TAG, "reSearch files mQuery:"+mQuery);

    }

    private void initDrawerLayout() {
        Log.d(TAG, "initDrawerLayout");
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                hideSoftKeyboard();
                mDrawerSlideOffset = 0;
                if (!mIsShowSearchFragment) {
                    // creates call to onPrepareOptionsMenu()
                    invalidateOptionsMenu();
                }
                Log.d("drawer", "closed");
                ActionBar actionbar = getSupportActionBar();
                if (actionbar != null) {
                    actionbar.setDisplayShowHomeEnabled(true);
                    actionbar.setHomeButtonEnabled(true);
                    actionbar.setDisplayHomeAsUpEnabled(true);
                    actionbar.setDisplayShowTitleEnabled(true);
                }
                setActionTitleWhenDrawerClosed();
            }

            public void onDrawerOpened(View drawerView) {
                hideSoftKeyboard();
                mDrawerSlideOffset = 1;
                if (!mIsShowSearchFragment) {
                    // creates call to onPrepareOptionsMenu()
                    invalidateOptionsMenu();
                }
                initCloudService();
                Log.d("drawer", "opened");
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
                    mDrawerLayout.setBackgroundColor(getResources().getColor(R.color.dark_theme_menu_bg)); // set dark background color of drawer
                } else {
                    mDrawerLayout.setBackgroundColor(DRAWER_BACKGROUD_COLOR); // set default background color of drawer
                }
            }

        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // setup drawer home icon
        mDrawerToggle.syncState();
    }

//    private void selectDrawerItem(String title) {
//      Log.d(TAG, "selectDrawerItem: " + title);
//      if (null == mNavigationView) {
//          Log.w(TAG, "navigation view is null");
//          return ;
//      }
//      Menu menu = mNavigationView.getMenu();
//      MenuItem item = null;
//      for (int i = 0; i < menu.size(); ++i) {
//          item = menu.getItem(i);
//          if (item.getTitle().equals(title)) {
//              menu.getItem(i).setChecked(true);
//          }
//      }
//    }

    private static void addItem(Context context, SubMenu submenu, int iconRes, String title) {
        MenuItem item = submenu.add(title);
        item.setIcon(iconRes);
        item.setCheckable(true);
    }

    public void closeStorageDrawerList() {
        closeNavDrawer();
    }

    public void setDrawerBackGroundColor(int color){
        mDrawerLayout.setBackgroundColor(color);
    }

    public int aZSVersionCheck(){
        int code = VersionChecker.checkVersion(this);
        //code = 3;
        VersionChecker.popupVersionResult(this, code, onDialogEventListener);
        Log.d(TAG, "Version Check code = " + code);
        return code;
    }

    public void updateAvaliableClouds(String[]titles,Drawable[]drawables, Drawable[]dialog_drawables){
        if (mStorageListAdapter != null) {
            ((StorageListAdapger)mStorageListAdapter).updateAvailableCloudsTitle(titles, drawables);
        }
        if (mMoveToNaviAdapter != null) {
            mMoveToNaviAdapter.updateAvailableCloudsTitle(titles, dialog_drawables);
        }
    }

    private void initStorageDrawerList(){
        mStorageListAdapter = new StorageListAdapger(this);
        mStorageListAdapter.setNavigationView((NavigationView) findViewById(R.id.navigation_view));
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
//        Fragment fragment = new PlanetFragment();
//        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);
//
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
//
//        // update selected item and title, then close the drawer
//        mDrawerList.setItemChecked(position, true);
//        setTitle(mPlanetTitles[position]);
//        mDrawerLayout.closeDrawer(mDrawerList);
    }

    public float getDrawerSlideOffset(){
        return mDrawerSlideOffset;
    }

    // +++ Action Bar : Normal Mode

    /** set ActionBar background and invisible title */
    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        //if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        //    actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_actionbar_bg));
        //} else {
        //    actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_actionbar_bg_portrait));
        //}

        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.rgb(0x00,0x7f,0xa0)));
       // getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E53D37")));
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        if (mIsPadMode) {
            actionBar.setCustomView(new SearchView(this), new ActionBar.LayoutParams(Gravity.CENTER_VERTICAL|Gravity.RIGHT));
            SearchView searchView = (SearchView) actionBar.getCustomView();
            setupSearchView(searchView);
        } else {
            /*LinearLayout drawer_title = (LinearLayout)View.inflate(this, R.layout.actionbar_custom_title, null);
            actionBar.setCustomView(drawer_title);
            custom_titleTextView = (TextView) drawer_title.findViewById(R.id.drawer_actionbar_title);
            actionBar.setDisplayShowCustomEnabled(false);*/
            actionBar.setDisplayShowTitleEnabled(true);
        /*    int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            TextView actionBarTitleTxt = (TextView) findViewById(titleId);
            if (actionBarTitleTxt != null && robotoLightTypeface != null) {
                //actionBarTitleTxt.setTypeface(robotoLightTypeface);
                actionBarTitleTxt.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.actionbar_title_size));
                if (custom_titleTextView != null ) {
                    //custom_titleTextView.setTypeface(robotoLightTypeface);
                    custom_titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.actionbar_title_size_drawer));
                }
            }*/
            actionBar.setTitle(this.getResources().getString(R.string.internal_storage_title));
            currentActionBarTitle = (String) actionBar.getTitle();
//            RelativeLayout buttonVew = (RelativeLayout) View.inflate(this, R.layout.actionbar_button, null);
//            mShortcutButton = (Spinner) buttonVew.findViewById(R.id.shortcut_button);
//            //mShortcutButton.setOnClickListener(this);
//            actionBar.setCustomView(buttonVew);
//               UpdateInternalStorageSpinner(null);
             actionBar.setIcon(android.R.color.transparent);
        }

        if (mIsAttachOp || mIsMultipleSelectionOp) {
            //actionBar.setDisplayShowCustomEnabled(!mIsPadMode);
        } else {
           // actionBar.setDisplayShowCustomEnabled(true);
        }
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(!mIsPadMode);
        actionBar.setDisplayHomeAsUpEnabled(!mIsPadMode);
    }


    public void setActionBarTitle(String title){
        if(!mIsPadMode){
            final ActionBar actionBar = getSupportActionBar();
    /*        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            TextView actionBarTitleTxt = (TextView) findViewById(titleId);
            if (actionBarTitleTxt != null && robotoLightTypeface != null) {
                //actionBarTitleTxt.setTypeface(robotoLightTypeface);
                //actionBarTitleTxt.setTextSize(TypedValue.COMPLEX_UNIT_DIP,getResources().getDimension(R.dimen.actionbar_title_size_set));
                actionBarTitleTxt.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.actionbar_title_size));
            }*/
            if (mDrawerSlideOffset > 0) {
                currentActionBarTitle = title;
            }else {
                currentActionBarTitle = title;
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(title);
            }

        }
    }

    public void setActionTitleWhenDrawerOpened(){
        if(!mIsPadMode){
            final ActionBar actionBar = getSupportActionBar();
        /*    int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            TextView actionBarTitleTxt = (TextView) findViewById(titleId);
            if (actionBarTitleTxt != null && robotoLightTypeface != null) {
                //actionBarTitleTxt.setTypeface(robotoLightTypeface);
                actionBarTitleTxt.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.actionbar_title_size_drawer));
            }*/
            //actionBar.setTitle("ffffff");
            /*actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);*/
            actionBar.setTitle(R.string.file_manager);
            /*if (custom_titleTextView != null ) {
                    custom_titleTextView.setText(R.string.file_manager);
            }
            getActionBar().setDisplayShowCustomEnabled(true);*/
        }
    }

    public void setActionTitleWhenDrawerClosed(){
        if(!mIsPadMode){
            final ActionBar actionBar = getSupportActionBar();
            if (currentActionBarTitle != null) {
                actionBar.setTitle(currentActionBarTitle);

            }
        }
    }


    void setupSearchViewExternal(SearchView searchView) {
        if (!mIsPadMode) {
            setupSearchView(searchView);
        }
    }

    private void setupSearchView(SearchView searchView) {

        mSearchView = searchView;
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        disableSearchViewActionMode(mSearchView);

        SearchView.SearchAutoComplete theTextArea = (SearchView.SearchAutoComplete) mSearchView.findViewById(R.id.search_src_text);
        if(theTextArea != null) {
            // set search text color
            if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
                theTextArea.setTextColor(Color.WHITE);
                theTextArea.setDropDownBackgroundDrawable(new ColorDrawable(Color.BLACK));
            } else {
                theTextArea.setTextColor(Color.BLACK);
                theTextArea.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
            }
            // set search hint color
            theTextArea.setHintTextColor(Color.GRAY);
        }
        // set close btn color
        ImageView closeBtn = (ImageView) mSearchView.findViewById(R.id.search_close_btn);
        if(closeBtn != null) {
            ThemeUtility.setItemIconColor(this, closeBtn.getDrawable());
        }
        // set voice btn color
        ImageView voiceBtn = (ImageView) mSearchView.findViewById(R.id.search_voice_btn);
        if(voiceBtn != null) {
            ThemeUtility.setItemIconColor(this, voiceBtn.getDrawable());
        }

        mSearchView.setSearchableInfo(info);
        mSearchView.setOnSearchClickListener(this);
        mSearchView.setOnSuggestionListener(this);
        mSearchView.setOnQueryTextListener(this);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void disableSearchViewActionMode(SearchView searchView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            EditText text = (EditText)searchView.findViewById(R.id.search_src_text);
            text.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }
            });
        }
    }

    // --- Action Bar

    // Override back-key action
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            if(isFromStorageAnalyzer) {
                Intent intent = new Intent(this, StorageAnalyzerActivity.class);
                startActivityForResult(intent, FileManagerActivity.REQUEST_CODE_BACK_HOME_FRAGMENT);
                isFromStorageAnalyzer=false;
                return;
            }
            if (mCurrentFragmentType == FragmentType.RECYCLE_BIN) {
                switchFragmentTo(FragmentType.HOME_PAGE);
                return;
            } else if (mCurrentFragmentType == FragmentType.HIDDEN_ZONE) {
                HiddenZoneFragment hiddenZoneFragment = (HiddenZoneFragment) getCurrentFragment();
                hiddenZoneFragment.onBackPressed();
                return;
            }
            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            if (mIsShowSearchFragment) {
                SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
                searchResultFragment.onBackPressed();
                showSearchFragment(FragmentType.NORMAL_SEARCH, false);
            }

                if (mIsShowHomePageFragment || fileListFragment == null || fileListFragment.onBackPressed()) {
                    /* 2015/12/30 change to single click to exit
                    if (doubleBackToExitPressedOnce) {
                        super.onBackPressed();
                        return;
                    }
                    this.doubleBackToExitPressedOnce = true;
                    Toast.makeText(this, R.string.tap_to_exit, Toast.LENGTH_SHORT).show();

                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce=false;
                        }
                    }, 2000);
                    */
                    super.onBackPressed();
                    return;
                }
            //}
        }
    }

    @Override
    public void displayDialog(int type, Object arg) {

        //save action after onSaveInstanceState
        if(!mIsResume) {
            tempDisplayDialogData = new DisplayDialogData(type,arg);
            return;
        }
        FileListFragment fileListFragment;

        switch (type) {
            case DialogType.TYPE_ABOUT_DIALOG:
                AboutDialogFragment aboutDialog = AboutDialogFragment.newInstance();
                aboutDialog.show(getFragmentManager(), "AboutDialogFragment");
                break;
            case DialogType.TYPE_INFO_DIALOG:
                InfoDialogFragment infoDialog = InfoDialogFragment.newInstance((VFile) arg);
                infoDialog.show(getFragmentManager(), "InfoDialogFragment");
                break;
            case DialogType.TYPE_RENAME_DIALOG:
                RenameDialogFragment renameDialog = RenameDialogFragment.newInstance((VFile) arg, RenameDialogFragment.TYPE_RENAME_DIALOG);
                renameDialog.show(getFragmentManager(), "RenameDialogFragment");
                break;
            case DialogType.TYPE_ADD_NEW_FOLDER:
                AddFolderDialogFragment addFolderDialog = AddFolderDialogFragment.newInstance((VFile) arg, AddFolderDialogFragment.TYPE_ADD_DIALOG);
                addFolderDialog.show(getFragmentManager(), "AddFolderDialogFragment");
                break;
            case DialogType.TYPE_DELETE_DIALOG:
                DeleteDialogFragment deleteDialogFragment = DeleteDialogFragment.newInstance((EditPool) arg, DeleteDialogFragment.Type.TYPE_DELETE_DIALOG);
                if(!deleteDialogFragment.isAdded()){
                    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
                }
                break;
            case DialogType.TYPE_DELETEPROGRESS_DIALOG:
            case DialogType.TYPE_MOVE_TO_RECYCLEBIN_PROGRESS_DIALOG:
//                if(deleteProgressDialogFragment == null){
                    deleteProgressDialogFragment = DeleteDialogFragment.newInstance((EditPool) arg,
                            type == DialogType.TYPE_DELETEPROGRESS_DIALOG?
                                    DeleteDialogFragment.Type.TYPE_PROGRESS_DIALOG : DeleteDialogFragment.Type.TYPE_RECYCLE_BIN_PROGRESS_DIALOG);
//                }
                deleteProgressDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
                fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                // delete local file case
                if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_LOCAL_STORAGE) {
                    EditorAsyncHelper.deletFile(((EditPool) arg).getFiles(), getEditHandler(), fileListFragment.belongToCategoryFromMediaStore(), type == DialogType.TYPE_DELETEPROGRESS_DIALOG);
                }
              /* if (mIsShowSearchFragment)
                    reSearch();*/
                break;
            case DialogType.TYPE_PASTE_DIALOG:
                pasteDialogFragment = PasteDialogFragment.newInstance((EditPool) arg);
                pasteDialogFragment.show(getFragmentManager(), "PasteDialogFragment");
                currentPasteDialogType = DialogType.TYPE_PASTE_DIALOG;
                if (arg != null) {
                    fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    EditorAsyncHelper.setPasteFileTerminate();
                    EditorAsyncHelper.pasteFile(((EditPool) arg), fileListFragment, fileListFragment.getHandler());
                }
                break;
            case DialogType.TYPE_REMOTE_PASTE_DIALOG:
                RemoteFilePasteDialogFramgment remotePasteDialogFragment = RemoteFilePasteDialogFramgment.newInstance((EditPool) arg);
                remotePasteDialogFragment.show(getFragmentManager(), "RemoteFilePasteDialogFramgment");
                currentPasteDialogType = DialogType.TYPE_REMOTE_PASTE_DIALOG;
                if (arg != null) {
                    fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    EditorAsyncHelper.setPasteFileTerminate();
                    EditorAsyncHelper.pasteFile(((EditPool) arg), fileListFragment, fileListFragment.getHandler());
                }
                break;

            case DialogType.TYPE_FILE_EXIST_DIALOG:
                PasteDialogFragment pasteDialog = (PasteDialogFragment) getFragmentManager().findFragmentByTag("PasteDialogFragment");

                FileExistDialogFragment fileExistFragment = FileExistDialogFragment.newInstance((ExistPair) arg);
                fileExistFragment.show(getFragmentManager(), "VFileExistDialogFragment");

                if (pasteDialog != null) {
                    pasteDialog.dismissAllowingStateLoss();
                }
                break;
            case DialogType.TYPE_ZIP_DIALOG:
                ZipDialogFragment zipDialogFragment = ZipDialogFragment.newInstance((EditPool) arg, ZipDialogFragment.TYPE_ZIP_DIALOG);
                zipDialogFragment.show(getFragmentManager(), "ZipDialogFragment");
                break;
            case DialogType.TYPE_ZIP_PROGRESS_DIALOG:
                ZipDialogFragment zipProgressFragment = ZipDialogFragment.newInstance((EditPool) arg, ZipDialogFragment.TYPE_PROGRESS_DIALOG);
                zipProgressFragment.show(getFragmentManager(), "ZipDialogFragment");
                break;
            case DialogType.TYPE_SEARCH_DIALOG:
                SearchDialogFragment searchDialogFragment = SearchDialogFragment.newInstance((String) arg);
                searchDialogFragment.show(getFragmentManager(), "SearchDialogFragment");
                break;
            case DialogType.TYPE_UNZIP_DIALOG:
                UnZipDialogFragment unZipDialogFragment = UnZipDialogFragment.newInstance((UnZipData) arg, UnZipDialogFragment.TYPE_UNZIP_DIALOG);
                unZipDialogFragment.show(getFragmentManager(), "UnZipDialogFragment");
                break;
            case DialogType.TYPE_UNZIP_PROGRESS_DIALOG:
                UnZipDialogFragment unZipProgressFragment = UnZipDialogFragment.newInstance((UnZipData) arg, UnZipDialogFragment.TYPE_PROGRESS_DIALOG);
                unZipProgressFragment.show(getFragmentManager(), "UnZipDialogFragment");
                break;
            case DialogType.TYPE_UNZIP_PREVIEW_DIALOG:
                UnZipDialogFragment unZipInfoDialogFragment = UnZipDialogFragment.newInstance((UnZipData) arg, UnZipDialogFragment.TYPE_UNZIP_PREVIEW_DIALOG);
                unZipInfoDialogFragment.show(getFragmentManager(), "UnZipDialogFragment");
                break;
            case DialogType.TYPE_CHARACTER_ENCODING_DIALOG:
                UnZipDialogFragment unZipCharEncodingDialogFragment = UnZipDialogFragment.newInstance((UnZipData) arg, UnZipDialogFragment.TYPE_CHARACTER_ENCODING_DIALOG);
                unZipCharEncodingDialogFragment.show(getFragmentManager(), "UnZipDialogFragment");
                break;

            case DialogType.TYPE_UNRAR_DIALOG:
                UnRarDialogFragment unRarDialogFragment = UnRarDialogFragment.newInstance((UnZipData) arg, UnRarDialogFragment.TYPE_UNZIP_DIALOG);
                unRarDialogFragment.show(getFragmentManager(), "UnRarDialogFragment");
                break;
            case DialogType.TYPE_UNRAR_PROGRESS_DIALOG:
                UnRarDialogFragment unRarProgressFragment = UnRarDialogFragment.newInstance((UnZipData) arg, UnRarDialogFragment.TYPE_PROGRESS_DIALOG);
                unRarProgressFragment.show(getFragmentManager(), "UnRarDialogFragment");
                break;
            case DialogType.TYPE_UNRAR_PREVIEW_DIALOG:
                UnRarDialogFragment unRarInfoDialogFragment = UnRarDialogFragment.newInstance((UnZipData) arg, UnRarDialogFragment.TYPE_UNZIP_PREVIEW_DIALOG);
                unRarInfoDialogFragment.show(getFragmentManager(), "UnRarDialogFragment");
                break;
            case DialogType.TYPE_UNRAR_CHARACTER_ENCODING_DIALOG:
                UnRarDialogFragment unRarCharEncodingDialogFragment = UnRarDialogFragment.newInstance((UnZipData) arg, UnRarDialogFragment.TYPE_CHARACTER_ENCODING_DIALOG);
                unRarCharEncodingDialogFragment.show(getFragmentManager(), "UnRarDialogFragment");
                break;
            case DialogType.TYPE_SORT_TYPE_DIALOG:
                fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                SortTypeSelectDialogFragment sortTypeSelectDialogFragment = SortTypeSelectDialogFragment.newInstance((Bundle)arg);
                sortTypeSelectDialogFragment.show(getFragmentManager(), "SortTypeSelectDialogFragment");
                break;
            case DialogType.TYPE_CONNECTING_REMOTE_DIALOG:
                RemoteConnectingProgressDialogFragment remoteConnectingProgressDialog = RemoteConnectingProgressDialogFragment.newInstance((VFile) arg);
                remoteConnectingProgressDialog.show(getFragmentManager(), "RemoteConnectingProgressDialogFragment");
                break;
            case DialogType.TYPE_WIFI_TURN_ON_DIALOG:
                RemoteWiFiTurnOnDialogFragment remoteWiFiTurnOnDialogFragment = RemoteWiFiTurnOnDialogFragment.newInstance((Integer) arg);
                remoteWiFiTurnOnDialogFragment.show(getFragmentManager(), "RemoteWiFiTurnOnDialogFragment");
                break;
            case DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS:
                AddFolderDialogFragment addFolderProgressDialog = AddFolderDialogFragment.newInstance((VFile) arg, AddFolderDialogFragment.TYPE_ROGRESS_DIALOG);
                addFolderProgressDialog.show(getFragmentManager(), AddFolderDialogFragment.DIALOG_TAG);
                break;
            case DialogType.TYPE_CLOUD_STORAGE_LOADING:
                CloudStorageLoadingDialogFragment cloudStorageLoadingProgressDialog = CloudStorageLoadingDialogFragment.newInstance((Integer)arg);
                cloudStorageLoadingProgressDialog.show(getFragmentManager(), "CloudStorageLoadingDialogFragment");
                break;
            case DialogType.TYPE_RENAME_PROGRESS_DIALOG:
                if(renamingProgressDialog == null){
                    renamingProgressDialog = RenameDialogFragment.newInstance((VFile) arg, RenameDialogFragment.TYPE_ROGRESS_DIALOG);
                }
                renamingProgressDialog.show(getFragmentManager(), "renamingProgressDialog");
                break;
            case DialogType.TYPE_OPEN_TYPE_DIALOG:
                OpenTypeDialogFragment openTypeDialog = OpenTypeDialogFragment.newInstance((VFile)arg);
                openTypeDialog.show(getFragmentManager(), "OpenTypeDialogFragment");
                break;
            case DialogType.TYPE_MOVE_TO_DIALOG:
                MoveToDialogFragment moveToDialog = MoveToDialogFragment.newInstance((Bundle)arg);
                moveToDialog.show(getFragmentManager(), MoveToDialogFragment.DIALOG_TAG);
                break;
            case DialogType.TYPE_FILE_PICKER_DIALOG:
                FilePickerDialogFragment filePickerDialogFragment = FilePickerDialogFragment.newInstance((Bundle) arg);
                filePickerDialogFragment.show(getFragmentManager(), FilePickerDialogFragment.DIALOG_TAG);
                break;
            case DialogType.TYPE_FAVORITE_RENAME_DIALOG:
                FavoriteRenameDialogFragment favoriteRenameDialog = FavoriteRenameDialogFragment.newInstance((VFile)arg, FavoriteRenameDialogFragment.TYPE_FAVORITE_RENAME_DIALOG);
                favoriteRenameDialog.show(getFragmentManager(), "FavoriteRenameDialogFragment");
                break;
            case DialogType.TYPE_FAVORITE_RENAME_NOTICE_DIALOG:
                FavoriteRenameDialogFragment favoriteRenameNoticeDialog = FavoriteRenameDialogFragment.newInstance((VFile)arg, FavoriteRenameDialogFragment.TYPE_FAVORITE_RENAME_NOTICE_DIALOG);
                favoriteRenameNoticeDialog.show(getFragmentManager(), "FavoriteRenameDialogFragment");
                break;
            case DialogType.TYPE_FAVORITE_ROMOVE_DIALOG:
                FavoriteRemoveDialogFragment favoriteRemoveDialog = FavoriteRemoveDialogFragment.newInstance((EditPool) arg, FavoriteRemoveDialogFragment.TYPE_DELETE_DIALOG);
                favoriteRemoveDialog.show(getFragmentManager(), "FavoriteRemoveDialogFragment");
                break;
            case DialogType.TYPE_RATE_US_DIALOG: {
                RecommendDialogFragment recommendDialogFragment= RecommendDialogFragment.newInstance();
                recommendDialogFragment.show(getFragmentManager(), "RecommendDialogFragment");
            }
                break;
            case DialogType.TYPE_NEWVERSION_NOTIFY_DIALOG:
                NewVersionDialogFragment newVersionDialogFragment = NewVersionDialogFragment.newInstance();
                newVersionDialogFragment.show(getFragmentManager(),NewVersionDialogFragment.DIALOG_TAG);
                break;
            case DialogType.TYPE_GMS_ALERT_DIALOG:
                GmsAlertDialogFragment gmsAlertDialogFragment = GmsAlertDialogFragment.newInstance((Integer) arg);
                gmsAlertDialogFragment.show(getFragmentManager(), "gmsAlertDialogFragment");
                break;
            case DialogType.TYPE_RECOMMEND_CM_DIALOG: {
                RecommendDialogFragment recommendDialogFragment = RecommendDialogFragment.newInstance(RecommendDialogFragment.mode_recommend_cm);
                recommendDialogFragment.show(getFragmentManager(), "RecommendDialogFragment");
            }
                break;
            default:
                break;
        }
    }

    public PasteDialogFragment getVisiblePasteDialog(){
        if(pasteDialogFragment == null){
            pasteDialogFragment = (PasteDialogFragment) getFragmentManager().findFragmentByTag("PasteDialogFragment");
        }
        return pasteDialogFragment;
    }

    public void closeDialog(int type) {
        switch (type) {
            case DialogType.TYPE_DELETEPROGRESS_DIALOG:
                if(deleteProgressDialogFragment == null){
                    deleteProgressDialogFragment = (DeleteDialogFragment) getFragmentManager().findFragmentByTag("DeleteDialogFragment");
                }
                if (deleteProgressDialogFragment != null)
                    deleteProgressDialogFragment.dismissAllowingStateLoss();
                break;
            case DialogType.TYPE_PASTE_DIALOG:
                pasteDialogFragment = (PasteDialogFragment) getFragmentManager().findFragmentByTag("PasteDialogFragment");
                if (pasteDialogFragment != null){
                    pasteDialogFragment.dismissAllowingStateLoss();
                    pasteDialogFragment = null;
                }
                break;
            case DialogType.TYPE_REMOTE_PASTE_DIALOG:
                RemoteFilePasteDialogFramgment remoteFilePasteDialogFramgment = (RemoteFilePasteDialogFramgment) getFragmentManager().findFragmentByTag("RemoteFilePasteDialogFramgment");
                if (remoteFilePasteDialogFramgment != null)
                    remoteFilePasteDialogFramgment.dismissAllowingStateLoss();
                break;
            case DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS:
                AddFolderDialogFragment addFolderProgressDialogFragment = (AddFolderDialogFragment) getFragmentManager().findFragmentByTag(AddFolderDialogFragment.DIALOG_TAG);
                if (addFolderProgressDialogFragment != null)
                    addFolderProgressDialogFragment.dismissAllowingStateLoss();
                break;
            case DialogType.TYPE_CLOUD_STORAGE_LOADING:
                CloudStorageLoadingDialogFragment cloudStorageLoadingDialogFragment = (CloudStorageLoadingDialogFragment) getFragmentManager().findFragmentByTag("CloudStorageLoadingDialogFragment");
                if (cloudStorageLoadingDialogFragment != null)
                    cloudStorageLoadingDialogFragment.dismissAllowingStateLoss();
                break;
            case DialogType.TYPE_RENAME_PROGRESS_DIALOG:
                if(renamingProgressDialog == null){
                    renamingProgressDialog = (RenameDialogFragment) getFragmentManager().findFragmentByTag("renamingProgressDialog");
                }
                if (renamingProgressDialog != null)
                    renamingProgressDialog.dismissAllowingStateLoss();
                break;
            case DialogType.TYPE_PREVIEW_PROCESS_DIALOG:
                PasteDialogFragment previewDialog = (PasteDialogFragment) getFragmentManager().findFragmentByTag(PasteDialogFragment.PREVIEW_DIALOG_PROCESS);
                if (previewDialog != null)
                    previewDialog.dismissAllowingStateLoss();
                break;
            default:
                break;

        }
    }


    public void hideSoftKeyboard() {
        if (mSearchView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
        }
    }

    // Search +++

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        if(mIsPadMode)
            mSearchView.setIconified(true);
        //updateSearchView(false);
        String keyword = getSuggestionValue(position);
        GaSearchFile.getInstance().sendEvents(this, GaSearchFile.CATEGORY_NAME,GaSearchFile.ACTION_SEARCH_KEYWORD, keyword, null);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        //mSearchView.setIconified(true);
        if (mSearchView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            isSearchIng = true;
            mSearchView.clearFocus();

            Log.i(TAG,"onQueryTextSubmit");
            GaSearchFile.getInstance().sendEvents(this, GaSearchFile.CATEGORY_NAME,GaSearchFile.ACTION_SEARCH_KEYWORD, query, null);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private String getSuggestionValue(int position) {
        String suggest = null;
        if (mSearchView != null) {
            Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
            suggest = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        }
        return suggest;
    }

    public void searchViewIconified(boolean iconified){
        mSearchView.setIconified(iconified);
    }

    public void showSearchFragment(FragmentType which, boolean isShow) {

        switch(which) {
        case NORMAL_SEARCH:
            if (isShow) {
                switchFragmentTo(FragmentType.NORMAL_SEARCH);
            } else {
                switchFragmentTo(FragmentType.FILE_LIST);
            }
            break;
        case HOME_PAGE:

            if (isShow) {
                switchFragmentTo(FragmentType.HOME_PAGE);
            } else {
                //no this case
            }
            break;
        }
    }

    public void switchFragmentTo(FragmentType switchFragment) {
        Log.d("test", "switchFragmentTo: " + switchFragment);
        FragmentManager manager = getFragmentManager();
        if (switchFragment != FragmentType.NORMAL_SEARCH)
            isSearchIng =false;
        if (switchFragment == FragmentType.HOME_PAGE) {
            SambaFileUtility.updateHostIp = false;
            RemoteFileUtility.isShowDevicesList = false;
            isFromStorageAnalyzer = false;
        }
        mCurrentFragmentType = switchFragment;
        mIsShowSearchFragment = mCurrentFragmentType == FragmentType.NORMAL_SEARCH;
        mIsShowHomePageFragment = mCurrentFragmentType == FragmentType.HOME_PAGE;

        FragmentTransaction ft = manager.beginTransaction();

        for (FragmentType fragmentType : FragmentType.values()) {
            Fragment fragment = findFragment(fragmentType);
            if (fragment == null) {
                if  (fragmentType == switchFragment)
                    fragment = createFragmentWithTag(fragmentType);
                else
                    continue;
            }
            if (fragmentType == switchFragment) ft.show(fragment);
            else ft.hide(fragment);
        }


        if (switchFragment == FragmentType.HOME_PAGE) {
            FileListFragment fileListFragment = (FileListFragment) manager.findFragmentById(R.id.filelist);
            HomePageFragment homePageFragment = (HomePageFragment) manager.findFragmentById(R.id.homepage);
                if(fileListFragment.isInEditMode()){
                    fileListFragment.finishEditMode();
                }

                homePageFragment.resume();
        }

        // Avoid HiddenZone thumbnail shows in recent app list
        if (switchFragment == FragmentType.HIDDEN_ZONE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (switchFragment.getFragmentTitle() != null) {
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(switchFragment.getFragmentTitle());
            currentActionBarTitle = (String) actionBar.getTitle();
            setSeclectedStorage(currentActionBarTitle, null);
        }
        try {
            ft.commit();
        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    private Fragment findFragment(FragmentType fragmentType) {
        if (fragmentType.getFragmentTag() == null)
            return getFragmentManager().findFragmentById(fragmentType.getFragmentId());
        else
            return getFragmentManager().findFragmentByTag(fragmentType.getFragmentTag());
    }

    private Fragment createFragmentWithTag(FragmentType fragmentType) {
        Fragment fragment = null;
        switch (fragmentType) {
            case RECYCLE_BIN:
                fragment = new RecycleBinFragment();
            break;
            case HIDDEN_ZONE:
                fragment = new HiddenZoneFragment();
                break;
        }
        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment, fragmentType.getFragmentTag())
                .commit();
        return fragment;
    }

    void attachShortCutOverlay() {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.gravity = Gravity.TOP;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.token = mShortCutView.getWindowToken();
        p.format = PixelFormat.TRANSPARENT;
        mWindowManager.addView(mShortCutView, p);

        Display display = mWindowManager.getDefaultDisplay();
        int width = display.getWidth();
        //mShortCutFragment.updateLayoutParams(width);

    }

    void deAttachShortCutOverlay() {
        if (!mIsPadMode && (mShortCutView != null) && (mShortCutView.getParent() != null)
                && ViewCompat.isAttachedToWindow(mShortCutView)) {
            mWindowManager.removeViewImmediate(mShortCutView);
        }
    }

    public void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.search_action:
                mSearchView.setQuery(null, false);
                break;
        }
    }

   public void updateLocalStorageList(ArrayList<Object> storageVolume){
       if(!mIsPadMode){
           ((StorageListAdapger)mStorageListAdapter).updateLocalStorageList(storageVolume);
           mMoveToNaviAdapter.updateLocalStorageList(storageVolume);
       }
   }

   public void updateCloudStorageAccountList(AccountInfo cloudStorageAccount){
       if(!mIsPadMode){
           ((StorageListAdapger)mStorageListAdapter).updateCloudStorageAccountList(cloudStorageAccount);
           mMoveToNaviAdapter.updateCloudStorageAccountList(cloudStorageAccount);
       }

   }

   public void removeCloudStorageAccountList(AccountInfo info){
       if(!mIsPadMode){
           ((StorageListAdapger)mStorageListAdapter).removeCloudStorageAccountList(info);
           mMoveToNaviAdapter.removeCloudStorageAccountList(info);
       }

   }

   public void clearCloudStorageAccountList(){
       if(!mIsPadMode){
           ((StorageListAdapger)mStorageListAdapter).clearCloudStorageAccountList();
           mMoveToNaviAdapter.clearCloudStorageAccountList();
       }

   }

  public void setSeclectedStorage(String selectedStorage, VFile vFile){
       if(!mIsPadMode){
           ((StorageListAdapger)mStorageListAdapter).setSelectedStorage(selectedStorage, vFile);
       }
   }

    public void updateActionBarTitle(VFile file){
       if (mIsPadMode || file == null) return;
       //boolean drawerOpen = mDrawerView.isDrawerOpen(LeftDrawView);
       if(isNavDrawerOpen()){
           closeNavDrawer();
       }
       final ActionBar actionBar = getSupportActionBar();

       int vFileType = file.getVFieType();
       if(vFileType == VFileType.TYPE_CATEGORY_STORAGE){
           actionBar.setTitle(this.getResources().getString(R.string.category_title_category));
             currentActionBarTitle = (String) actionBar.getTitle();
             setSeclectedStorage(currentActionBarTitle, null);
            return;
       }

       String path = null;
       try {
           path = FileUtility.getCanonicalPath(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

       if (path != null && (vFileType == VFileType.TYPE_LOCAL_STORAGE)){
          if(path.equals("/") || path.equals("/storage") || path.equals("/Removable")){
              actionBar.setTitle(this.getResources().getString(R.string.internal_storage_title));
              currentActionBarTitle = (String) actionBar.getTitle();
              setSeclectedStorage(currentActionBarTitle, null);
             return;
          }

        final StorageManager storageManager = (StorageManager) getSystemService(
        Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume;
        storageVolume = ((FileManagerApplication)this.getApplication()).getStorageVolume();
        VFile[] tempVFile = ((FileManagerApplication)this.getApplication()).getStorageFile();
        ArrayList<VFile> storageFile = new ArrayList<VFile>();
        ArrayList<String> mStorageTitle = new ArrayList<String>();

      for (int i = 0; i < storageVolume.size(); i ++) {

          if(StorageType.TYPE_INTERNAL_STORAGE != i){
             if (storageManager != null && reflectionApis.getVolumeState(storageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)){
//                 String desc = reflectionApis.volume_getDescription(storageVolume.get(i), this);
//                 if (!TextUtils.isEmpty(desc)){
//                     mStorageTitle.add(desc);
//                 }else{
//                     mStorageTitle.add(reflectionApis.volume_getMountPointTitle(storageVolume.get(i)));
//                 }
                 mStorageTitle.add(VolumeInfoUtility.getInstance(this).findStorageTitleByStorageVolume(storageVolume.get(i)));
                 storageFile.add(tempVFile[i]);

                 /*if(path.startsWith(storageVolume.get(i).getPath())){
                     actionBar.setTitle(storageVolume.get(i).getDescription(this));
                     currentActionBarTitle = storageVolume.get(i).getDescription(this);
                     setSeclectedStorage(currentActionBarTitle);
                     return;
                 }*/
             }
          }else{
              mStorageTitle.add(this.getResources().getString(R.string.internal_storage_title));
              storageFile.add(tempVFile[i]);
          }
      }

      String storagePath = null;
          for (int i = 0; i < storageFile.size(); i++){
              try {
                  storagePath = FileUtility.getCanonicalPath(storageFile.get(i));
              } catch (IOException e) {
                   e.printStackTrace();
                 }

              if (storagePath != null && path.startsWith(storagePath)){
//                  String title = VolumeInfoUtility.getInstance(this).findStorageTitleByStorageVolume(storageVolume);
//                  Toast.makeText(this,"title:"+title,Toast.LENGTH_SHORT).show();
                  actionBar.setTitle(mStorageTitle.get(i));
                  currentActionBarTitle = mStorageTitle.get(i);
//                  if (path.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)){
//                      actionBar.setTitle(getResources().getString(R.string.internal_storage_title));
//                      currentActionBarTitle = getResources().getString(R.string.internal_storage_title);
//                  } else if (path.startsWith(WrapEnvironment.MICROSD_CANONICAL_PATH)) {
//                      actionBar.setTitle(getResources().getString(R.string.microsd_storage_title));
//                      currentActionBarTitle = getResources().getString(R.string.microsd_storage_title);
//                  } else if (path.startsWith(WrapEnvironment.USBDISK1_CANONICAL_PATH)) {
//                      String title = selectUSBDiskTitle(mStorageTitle.get(i),getResources().getString(R.string.usbdisk1_storage_title));
//                      actionBar.setTitle(title);
//                      currentActionBarTitle = title;
//                  } else if (path.startsWith(WrapEnvironment.USBDISK2_CANONICAL_PATH)) {
//                      String title = selectUSBDiskTitle(mStorageTitle.get(i),getResources().getString(R.string.usbdisk2_storage_title));
//                      actionBar.setTitle(title);
//                      currentActionBarTitle = title;
//                  } else if (path.startsWith(WrapEnvironment.USBDISK3_CANONICAL_PATH)) {
//                      String title = selectUSBDiskTitle(mStorageTitle.get(i),getResources().getString(R.string.usbdisk3_storage_title));
//                      actionBar.setTitle(title);
//                      currentActionBarTitle = title;
//                  } else if (path.startsWith(WrapEnvironment.USBDISK4_CANONICAL_PATH)) {
//                      String title = selectUSBDiskTitle(mStorageTitle.get(i),getResources().getString(R.string.usbdisk4_storage_title));
//                      actionBar.setTitle(title);
//                      currentActionBarTitle = title;
//                  } else if (path.startsWith(WrapEnvironment.USBDISK5_CANONICAL_PATH)) {
//                      String title = selectUSBDiskTitle(mStorageTitle.get(i),getResources().getString(R.string.usbdisk5_storage_title));
//                      actionBar.setTitle(title);
//                      currentActionBarTitle = title;
//                  } else if (path.startsWith(WrapEnvironment.SDREADER_CANONICAL_PATH)) {
//                      actionBar.setTitle(getResources().getString(R.string.sdreader_storage_title));
//                      currentActionBarTitle = getResources().getString(R.string.sdreader_storage_title);
//                  } else {
//                      actionBar.setTitle(mStorageTitle.get(i));
//                      currentActionBarTitle = mStorageTitle.get(i);
//                  }
                  setSeclectedStorage(currentActionBarTitle, new VFile(FileUtility.changeToSdcardPath(storagePath)));
                  return;
              }
           }

          actionBar.setTitle(this.getResources().getString(R.string.internal_storage_title));
          currentActionBarTitle = (String) actionBar.getTitle();
          setSeclectedStorage(currentActionBarTitle, null);
          return;
       }

       if(vFileType == VFileType.TYPE_CLOUD_STORAGE){
           String storageName = ((RemoteVFile)file).getStorageName();
           int storageType = ((RemoteVFile)file).getStorageType();
           switch (storageType) {
           case StorageType.TYPE_GOOGLE_DRIVE:
               actionBar.setTitle(getResources().getString(R.string.googledrive_storage_title));
               setSeclectedStorage(getResources().getString(R.string.googledrive_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_DROPBOX:
               actionBar.setTitle(getResources().getString(R.string.dropbox_storage_title));
               setSeclectedStorage(getResources().getString(R.string.dropbox_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_BAIDUPCS:
               actionBar.setTitle(getResources().getString(R.string.baidu_storage_title));
               setSeclectedStorage(getResources().getString(R.string.baidu_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_SKYDRIVE:
               actionBar.setTitle(getResources().getString(R.string.skydrive_storage_title));
               setSeclectedStorage(getResources().getString(R.string.skydrive_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_ASUSWEBSTORAGE:
               actionBar.setTitle(getResources().getString(R.string.asuswebstorage_storage_title));
               setSeclectedStorage(getResources().getString(R.string.asuswebstorage_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_HOME_CLOUD:
               actionBar.setTitle(getResources().getString(R.string.asushomebox_storage_title));
               setSeclectedStorage(getResources().getString(R.string.asushomebox_storage_title) + storageName, null);
               break;
           case StorageType.TYPE_YANDEX:
               actionBar.setTitle(getResources().getString(R.string.yandex_storage_title));
               setSeclectedStorage(getResources().getString(R.string.yandex_storage_title) + storageName, null);
               break;
           default:
               ;
           }
           currentActionBarTitle = (String) actionBar.getTitle();
       }
    }


    // Search ---

    public boolean onTouch(View v, MotionEvent event) {
        final FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        if(mIsPadMode){
            searchViewIconified(true);
        }else{
            fileListFragment.collapseSearchView();
        }

        //final SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);

        //if (searchResultFragment.isVisible() && isPadMode()) {
        //    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        //        mDividerLeft.setBackgroundResource(R.drawable.asus_ep_filemanager_p_dragging_bar_normal_left);
        //        mDividerRight.setBackgroundResource(R.drawable.asus_ep_filemanager_p_dragging_bar_normal_right);
        //    } else {
        //        mDividerLeft.setBackgroundResource(R.drawable.asus_ep_filemanager_l_dragging_bar_normal_left);
        //        mDividerRight.setBackgroundResource(R.drawable.asus_ep_filemanager_l_dragging_bar_normal_right);
        //    }
        //    return false;
        //}

        return true;
    }

//    private void updateShortCutView(int x) {
//        setLayoutParams(mShortCutView, x, ViewGroup.LayoutParams.MATCH_PARENT, 0);
//
//        ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(sShortCutFragmentTag);
//        if (shortcutFragment != null) {
//            shortcutFragment.updateLayoutParams(x);
//        }
//
//    }

    private void updateRightListView(boolean isHidden) {
        final FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        float sort_label_height = getResources().getDimension(R.dimen.sort_label_height);
        if (fileListFragment.isVisible()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                float sort_size_width = this.getResources().getDimension(R.dimen.sort_size_width);

                if (isHidden) {
                    setLayoutParams(mSortSizeView, (int)sort_size_width, (int)sort_label_height, 1);
                    setLayoutParams(mSortDateView, 0, (int)sort_label_height, 0);
                } else {
                    setLayoutParams(mSortSizeView, (int)sort_size_width, (int)sort_label_height, 0);
                    setLayoutParams(mSortDateView, 0, (int)sort_label_height, 1);
                }
            } else {
                float sort_name_width = this.getResources().getDimension(R.dimen.sort_name_width);
                if (isHidden) {
                    setLayoutParams(mSortNameView, (int)sort_name_width, (int)sort_label_height, 1);
                    setLayoutParams(mSortDateView, 0, (int)sort_label_height, 0);
                } else {
                    setLayoutParams(mSortNameView, (int)sort_name_width, (int)sort_label_height, 0);
                    setLayoutParams(mSortDateView, 0, (int)sort_label_height, 1);
                }
            }
        }
    }

    // ---

    // Called when an intent of application/zip is received.
    // The fileListFragment will display the file list where the zip file has been saved.
    // Display the preview dialog of zip file.
    private void processUnZipIntent(Intent intent) {
        if (DEBUG)
            Log.i(TAG, intent.getDataString());

        Uri uri = intent.getData();
        String filePath = "";
        LocalVFile zipFile = null;
        String unZipName = "";
        long unZipSize = 0;
        String encode = getString(R.string.default_encoding);
        String uriString = uri.toString();
        UnZipData unZipData = null;

        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        LocalVFile scanFolder = null;

        if (intent.getScheme().equals("file")) {
            filePath = intent.getData().getPath();
            zipFile = new LocalVFile(filePath);
            unZipName = zipFile.getNameNoExtension();
            scanFolder = new LocalVFile(zipFile.getParent());

        } else {
            if (uri.getHost().equals("media")) {
                filePath = FileUtility.getPathFromMediaUri(getContentResolver(), uri);
                zipFile = new LocalVFile(filePath);
                unZipName = zipFile.getNameNoExtension();
                scanFolder = new LocalVFile(zipFile.getParent());

            } else if (uri.getHost().equals("com.android.email.attachmentprovider")) {
                String fileName = FileUtility.getNameFromEmailUri(getContentResolver(), uri);
                if (fileName != null) {
                    int index = fileName.toLowerCase().lastIndexOf(".zip");
                    unZipName = fileName.substring(0, index);
                }
                scanFolder = new LocalVFile(PATH_DOWNLOAD);
                if (DEBUG)
                    Log.i(TAG, "from email");

            } else {
                scanFolder = new LocalVFile(PATH_DOWNLOAD);
                if (DEBUG)
                    Log.i(TAG, "from others");
            }
        }

        if (fileListFragment != null) {
            fileListFragment.onUnZipIntent(scanFolder);
        }

        unZipData = new UnZipData(zipFile, unZipName, unZipSize, encode, uriString);
        displayDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
        mIsFirstUnZipIntent = false;
    }

    // Delete the previewed files which were saved in folder of ".pfile"
    // under the external cache directory.
    public void deleteCache() {
        if (FileUtility.isExternalStorageAvailable() && getExternalCacheDir() != null) {
            LocalVFile pfile = new LocalVFile(getExternalCacheDir(), ".pfile/");
            if (!pfile.exists()) {
                try {
                    pfile.mkdirs();
                    new LocalVFile(pfile, ".nomedia").createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LocalVFile[] cacheFiles = new LocalVFile(pfile).listVFiles();
                if (cacheFiles != null) {
                    for(LocalVFile file : cacheFiles) {
                        if (!file.getName().equals(".nomedia")) {
                            if (DEBUG)
                                Log.i(TAG, "delete " + file.getName());
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    public boolean isPadMode() {
        return mIsPadMode;
    }

    public boolean isPadMode2() {
        return mIsPadMode2;
    }

    @Override @TargetApi(19)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_MANAGER_UNZIP_PREVIEW) {
            deleteCache();
        }

        //+++ cloud storage
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_ACCOUNT && resultCode == Activity.RESULT_OK) {
            //if (data != null) {
            //    FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            //    fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOGGING_DIALOG);

           //     String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//                String accountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);

            //    RemoteFileUtility.sendCloudStorageMsg(accountName, null, null, MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, CloudStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE);
            //} else {
            //}
        }else if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQ){
            FileListFragment fl = getFileListFragment();

            if(resultCode == Activity.RESULT_OK){
                Uri treeUri = data.getData();
                Log.d(TAG,"---SAF -uri--" + treeUri.toString());
                DocumentFile rootFile = DocumentFile.fromTreeUri(this, treeUri);
                if(rootFile != null /*&& rootFile.getName().equals("MicroSD")*/){
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    SafOperationUtility safOperationUtility = SafOperationUtility.getInstance(this);
                    //request saf action file != null
                    if(safOperationUtility.getChoosedFile()!=null )
                    {
                        //check permission again
                        if(safOperationUtility.isNeedToShowSafDialog(safOperationUtility.getChoosedFile().getAbsolutePath())){
                            //no permission callSafChoose again
                            callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                            return;
                        }else {
                            //had permission
                            SafOperationUtility.getInstance(this).clearWriteCapMap();
                        }
                    }else{
                        //request saf action file = null(usually not happen) callSafChoose again
                        callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                        return;
                    }

//                    if (SafOperationUtility.getInstance().SECONDARY_STORAGE != null ){
//                        if (SafOperationUtility.getInstance().getDocFileFromPath(SafOperationUtility.getInstance().SECONDARY_STORAGE) == null){
//                            callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
//                            return;
//                        }
//                    }
                }else{
                    callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                    return;
                }

            }else{
                   if(fl != null){
                    fl.updateStatesWithoutSafPermission(SafOperationUtility.getInstance(this).getCallSafAction());
                    return;
                }
            }

            Fragment fragment = getCurrentFragment();
            if (fragment != null && fragment instanceof SafActionHandler)
                ((SafActionHandler)fragment).handleAction(SafOperationUtility.getInstance(this).getCallSafAction());
            /*if(getIsShowSearchFragment()){
                SearchResultFragment mSFragment = (SearchResultFragment)getFragmentManager().findFragmentById(R.id.searchlist);
                if(mSFragment != null){
                    mSFragment.handleAction(SafOperationUtility.getInstance(this).getCallSafAction());
                }
            }else{
                if(fl != null){
                    fl.handleAction(SafOperationUtility.getInstance(this).getCallSafAction());
                }
            }*/
        } else if (requestCode == REQUEST_TUTORIAL) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else if (resultCode == Activity.RESULT_OK) {
                handleRequestTutorial();
            }
        } else if (requestCode == REQUEST_SDTUTORIAL && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
        } else if (requestCode == REQUEST_CODE_BACK_HOME_FRAGMENT) {
            showSearchFragment(FragmentType.HOME_PAGE, true);
        }
    }

    // +++ remote storage function
    private ServiceConnection mRemoteConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sRemoteService = new Messenger(service);

            try {
                // register remote storage service
                Message msg = Message.obtain(null, CloudStorageServiceHandlerMsg.MSG_APP_REGISTER_CLIENT);
                msg.replyTo = mRemoteClientMessenger;
                sRemoteService.send(msg);

                // update the connected remote storage information
                Message msg2 = Message.obtain(null, CloudStorageServiceHandlerMsg.MSG_APP_UPDATE_DEVICE);
                sRemoteService.send(msg2);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sRemoteService = null;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        mIsResume = false;
    }

//    @Override
//    protected void onStop() {
//        // cloud storage
////        ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(sShortCutFragmentTag);
////        shortcutFragment.clearCloudStorageList();
//        super.onStop();
//    }

    private void doBindService() {
        Log.d(TAG, "do bind service to com.asus.remotestorage.RemoteStorageService");
        Intent bindIntent = new Intent();
        if(WrapEnvironment.isAZSEnable(this)){
            String sFSPackage = VersionChecker.getCFSPackage(this);
            Log.d(TAG, "sFSPackage: " + sFSPackage);
            if(sFSPackage  != null){
                Log.d(TAG, "do bind service to " + sFSPackage);
                bindIntent.setClassName(sFSPackage, "com.asus.service.cloudstorage.CloudStorageService");
            }else{
                Log.d(TAG, "do bind service to com.asus.service.cloudstorage");
                bindIntent.setClassName("com.asus.service.cloudstorage", "com.asus.service.cloudstorage.CloudStorageService");
            }
        }else{
            Log.d(TAG, "do bind service to AZS lib");
            bindIntent.setClassName(getPackageName(), "com.asus.service.cloudstorage.CloudStorageService");
        }

        bindService(bindIntent, mRemoteConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbind remote service
     *
     * @param force if set true will update remote storage list
     */
    private void doUnbindService(boolean force) {
     /*   if (sRemoteService != null) {
            try {
                Log.d(TAG, "do unbind service to com.asus.remotestorage.RemoteStorageService");
                Message msg = Message.obtain(null, CloudStorageServiceHandlerMsg.MSG_APP_UNREGISTER_CLIENT);
                msg.replyTo = mRemoteClientMessenger;
                sRemoteService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }*/
        if(mRemoteConn != null){
            try{
                unbindService(mRemoteConn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void triggerGoogleAccount() {
        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[] {"com.google"}, true, null, null, null, null);
        startActivityForResult(intent, CHOOSE_ACCOUNT);
    }

    public boolean isWiFiDirectPageNow() {
        return mIsWiFiDirectPage;
    }

    public Messenger getRemoteService() {
        return sRemoteService;
    }

    public Messenger getRemoteClientMessenger() {
        return mRemoteClientMessenger;
    }

    public boolean hasWIFIDirect() {
        //return mSupportWIFIDirect;
        return false;
    }
    // --- remote storage function

    //add for samba device list
    public  void showSortContainerView(boolean show){
        if(mSortContainerRootView != null){
            if(show){
                mSortContainerRootView.setVisibility(View.VISIBLE);
            }else{
                mSortContainerRootView.setVisibility(View.GONE);
            }
        }
    }

    public void updateActionMenuView(){
        invalidateOptionsMenu();
    }

    public boolean isSeachViewIsShow(){
        boolean isShow = false;
        FragmentManager manager = getFragmentManager();
        SearchResultFragment searchResultFragment = (SearchResultFragment) manager.findFragmentById(R.id.searchlist);
        if(searchResultFragment != null){
            isShow = searchResultFragment.isVisible();
        }
        return isShow;
    }


    public static void initFeedBackResource(final Context context){
        if(FeedBackHasInit){
            return;
        }
        FeedBackHasInit = true;

        NewConfigInterface configInterface = new NewConfigInterface() {

            @Override
            public int getTopicID() {
                return 0; // not used now, return 0;
            }

            @Override
            public int getForumID() {
                return 0; // not used now, return 0;
            }

            @Override
            public int getPrimaryColor() {
                return context.getResources().getColor(R.color.actionbar_background);
            }

            @Override
            public String getAttachmentPath() {
                File logDir = new File(context.getApplicationInfo().dataDir, LOG_FOLDER_NAME);
                if (!logDir.exists()) {
                    Log.w("GlobalVariable", "folder does not exist. mkdir");
                    logDir.mkdirs();
                }
                String filePath = logDir.getAbsolutePath() + "/log.txt";
                String zipFilePath = logDir.getAbsolutePath() + "/log.zip";
                String command = "logcat -v threadtime -d -f " + filePath;
                try {
                    Process process = Runtime.getRuntime().exec(command);
                    process.waitFor();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                zipLogFiles(new String[]{filePath}, zipFilePath);
                // remove log file
                //File logFile = new File(filePath);
                //if(logFile != null) logFile.delete();
                return zipFilePath;
            }

            @Override
            public String getAppCatalogName() {
                return "ZenUI-FileManager"; // return the catalog name of your app, ex ZenUI-CoverView.
            }
        };
        UserVoice.init(configInterface, context);
    }

    private void printVolumes(){
        final StorageManager storageManager = (StorageManager) getSystemService(
            Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume;
        storageVolume = ((FileManagerApplication)this.getApplication()).getStorageVolume();
        for (int i = 0; i < storageVolume.size(); i ++) {
            Log.d("FileManager_bug", "begin print StorageVolume " + i +"\n");
            if (storageManager != null && reflectionApis.getVolumeState(storageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)) {
                String desc = reflectionApis.volume_getDescription(storageVolume.get(i), this);
                String title = reflectionApis.volume_getMountPointTitle(storageVolume.get(i));
                String path= reflectionApis.volume_getPath(storageVolume.get(i));
                Log.d("FileManager_bug", "title = " + title);
                Log.d("FileManager_bug", "desc= " + desc);
                Log.d("FileManager_bug", "path= " + path);
            }
        }

        Object[] VolumeInfo = reflectionApis.getVolumes(storageManager);

        for (int i = 0; i < VolumeInfo.length; i ++) {
            Log.d("FileManager_bug", "Volumeinfo = " + VolumeInfo[i].toString());
        }
    }
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_FILE_SIZE = 5120; // 5MB
    private static final int MAX_ROTATED_FILE_COUNT = 5;
    public final static String LOG_FOLDER_NAME = "ZenLog/";

    private void printStatFsInfo(String aPath){
        try {
            if (!new File(aPath).exists())
                return;
            StatFs statFs = new StatFs(aPath);
            long avlBlock = statFs.getAvailableBlocksLong();
            long sizeBlock = statFs.getBlockSizeLong();
            long freeBlock = statFs.getFreeBlocksLong();
            Log.d("FileManager_bug", "PATH = " + aPath);
            Log.d("FileManager_bug", "avlBlock = " + avlBlock);
            Log.d("FileManager_bug", "freeBlock= " + freeBlock);
            Log.d("FileManager_bug", "BlockSize = " + sizeBlock);

            long retUSB = SafOperationUtility.getDirSizes(new File(aPath));
            Log.d("FileManager_bug", "retUSB = " + retUSB);
        }catch(Throwable ignore){};
    }


    public void zipLocatInfo(){
        //String dfresult = SafOperationUtility.getInstance().dfwrapper();
        //Log.d("FileManager_bug", dfresult);

        //String duresult = SafOperationUtility.getInstance().duwrapper();
        //Log.d("FileManager_bug", duresult);

        //printStatFsInfo("/storage/MicroSD");
        //printStatFsInfo("/storage/USBdisk1");
        //printStatFsInfo("/storage/USBdisk2");
        printVolumes();

        File logDir = new File(getApplicationInfo().dataDir, LOG_FOLDER_NAME);
        if (!logDir.exists()) {
            Log.w("GlobalVariable", "folder does not exist. mkdir");
            logDir.mkdirs();
        }
        String filePath = logDir.getAbsolutePath() + "/log.txt";
        String zipFilePath = logDir.getAbsolutePath() + "/log.zip";
        String command = "logcat -v threadtime -d -f " + filePath;
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        zipLogFiles(new String[]{filePath}, zipFilePath);
        // remove log file
        //File logFile = new File(filePath);
        //if(logFile != null) logFile.delete();
    }

    private static void zipLogFiles(String[] files, String zipFilePath) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFilePath);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < files.length; i++) {
                if (files[i] != null) {
                    File file = new File(files[i]);
                    if (!file.exists()) continue;
                    FileInputStream fi = new FileInputStream(files[i]);
                    origin = new BufferedInputStream(fi, BUFFER_SIZE);

                    ZipEntry entry = new ZipEntry(
                            files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count = 0;
                    while ((count = origin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    origin.close();
                    out.close();
                }
            }
            dest.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFromFilePick(){
        return this.mIsAttachOp;
    }

    public void setPopupMenu(PopupMenu popupMenu){
        mPopupMenu = popupMenu;
    }

    @Override
    public void onRequestConfirmed(int action, String diskLabel) {
        Intent tutorialIntent = new Intent();
        tutorialIntent.setClass(this, TutorialActivity.class);
        tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
        tutorialIntent.putExtra(TutorialActivity.TUTORIAL_DISKLABEL, diskLabel);
        startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
        SafOperationUtility.getInstance(this).setCallSafAction(action);
    }

    @Override
    public void onRequestDenied() {
        //do nothing here, just cancel action
    }
    public void callSafChoose(int action){
        if(FileUtility.isFirstSDPermission(this)){
            File aChoosedFile = SafOperationUtility.getInstance(this).getChoosedFile();
            String desc = null;
            if (null != aChoosedFile){
                Object storageVolume = SafOperationUtility.getInstance(this).getStorageVolumeFromFullPath(aChoosedFile.getAbsolutePath());
                desc = reflectionApis.volume_getDescription(storageVolume, this);
            }
            RequestSDPermissionDialogFragment fragment = RequestSDPermissionDialogFragment.newInstance(action,desc);
            fragment.setStyle(RequestSDPermissionDialogFragment.STYLE_NORMAL, R.style.FMAlertDialogStyle);
            fragment.show(getFragmentManager(), RequestSDPermissionDialogFragment.DIALOG_TAG);
        }else{
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
            SafOperationUtility.getInstance(this).setCallSafAction(action);
        }
    }

    public FileListFragment getFileListFragment(){
        return  (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
    }

    public MoveToNaviAdapter getMoveToNaviAdapter(){
        return mMoveToNaviAdapter;
    }

    public void initCloudService(){
        if (WrapEnvironment.isAZSEnable(this)) {
            int azsCheckStatusCode = aZSVersionCheck();

            if (azsCheckStatusCode == VersionChecker.VERSION_STATUS.COMPATABLE
                    || azsCheckStatusCode == VersionChecker.VERSION_STATUS.MATCH) {
                if (sRemoteService == null) {
                    doBindService();
                    RemoteAccountUtility.getInstance(this).init();
                } else if (mIsAttachOp || mIsMultipleSelectionOp) {
                    RemoteAccountUtility.getInstance(this)
                            .initAvaliableCloudsInfo();
                }
            }
        } else {
            if (sRemoteService == null) {
                doBindService();
                RemoteAccountUtility.getInstance(this).init();
            } else if (mIsAttachOp || mIsMultipleSelectionOp) {
                RemoteAccountUtility.getInstance(this)
                        .initAvaliableCloudsInfo();
            }
        }
    }

    public boolean isMoveToDialogShowing(){
        boolean isShow = false;
        MoveToDialogFragment moveToFragment = (MoveToDialogFragment)getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
        if(moveToFragment != null && moveToFragment.getDialog() != null){
            isShow = moveToFragment.getDialog().isShowing();
        }
        return isShow;
    }

    public boolean isAddFolderDialogShowing(){
        boolean isShow = false;
        AddFolderDialogFragment addFolderDialog= (AddFolderDialogFragment)getFragmentManager().findFragmentByTag(AddFolderDialogFragment.DIALOG_TAG);
        if(addFolderDialog != null && addFolderDialog.getDialog() != null){
            isShow = addFolderDialog.getDialog().isShowing();
        }
        return isShow;
    }
    public boolean isFilePickerDialogShowing(){
        boolean isShow = false;
        FilePickerDialogFragment filePickerDialog= (FilePickerDialogFragment)getFragmentManager().findFragmentByTag(FilePickerDialogFragment.DIALOG_TAG);
        if(filePickerDialog != null && filePickerDialog.getDialog() != null){
            isShow = filePickerDialog.getDialog().isShowing();
        }
        return isShow;
    }
    private void initCategoryFile(){
        CATEGORY_HOME_PAGE_FILE = new LocalVFile("/", VFileType.TYPE_CATEGORY_STORAGE);
        Resources res = getResources();
        CATEGORY_IMAGE_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.IMAGE), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.IMAGE);
        CATEGORY_MUSIC_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.MUSIC), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.MUSIC);
        CATEGORY_VIDEO_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.VIDEO), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.VIDEO);
        CATEGORY_APK_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.APP), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.APP);
        CATEGORY_FAVORITE_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.FAVORITE), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.FAVORITE);
        CATEGORY_COMPRESS_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.COMPRESSED), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.COMPRESSED);
        CATEGORY_DOCUMENT_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.DOCUMENT), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.DOCUMENT);
        CATEGORY_RECENT_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.RECENT), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.RECENT);
        CATEGORY_LARGE_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.LARGE_FILE), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.LARGE_FILE);
        CATEGORY_PDF_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.PDF), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.PDF);
        CATEGORY_GAME_FILE = new LocalVFile("/" + CategoryItem.findNameById(res, CategoryItem.GAME), VFileType.TYPE_CATEGORY_STORAGE, CategoryItem.GAME);
    }

    public boolean isShowFileListFragment(){
        boolean isShow = false;
        FileListFragment fileListFragment = getFileListFragment();
        if(fileListFragment != null){
            isShow = fileListFragment.isVisible();
        }
        return isShow;
    }

    private void show_recruit_beta_user(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("https://play.google.com/apps/testing/com.asus.filemanager"));

        List<ResolveInfo> result = getPackageManager().queryIntentActivities(intent, 0);
        if (result.size() == 0) {
            Log.d(TAG, "No activity can handle this intent:"+intent.toString());
        }else{
            startActivity(intent);

        }
    }

    private void show_mount_setting(){
        Intent intent = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);
        List<ResolveInfo> resInfo = this.getPackageManager().queryIntentActivities(intent,0);
        if (null != resInfo && !resInfo.isEmpty()){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void show_app_setting(){
        /* above code will find nothing
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        List<ResolveInfo> resInfo = this.getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL);
        if (null != resInfo && !resInfo.isEmpty()){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        */
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.putExtra(":settings:fragment_args_key", "permission_settings");
        intent.putExtra(":settings:fragment_args_key_highlight_times", 3);
        try {
            startActivityForResult(intent,FILE_MANAGER_SETTING_PERMISSION);
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void show_rateus(){
        boolean showToast = true;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=com.asus.filemanager"));
            intent.setClassName("com.android.vending", "com.google.android.finsky.activities.MainActivity");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast = false;

            Intent intent_temp1 = new Intent(Intent.ACTION_VIEW);
            intent_temp1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent_temp1.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.asus.filemanager"));

            List<ResolveInfo> result = getPackageManager().queryIntentActivities(intent_temp1, 0);
            if (result.size() == 0) {
                Intent intent_temp2 = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.asus.userfeedback"));
                intent_temp2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                result = getPackageManager().queryIntentActivities(intent_temp2, 0);
                if (result.size() == 0) {
                    Log.d(TAG, "No activity can handle this intent:"+intent_temp2.toString());
                } else {
                    startActivity(intent_temp2);
                }
            } else {
                startActivity(intent_temp1);
            }
        }
        if (showToast)
            new Handler().postDelayed(new HudToastAnimation(this), 1700);
    }
    public void show_move_up_animation(){
        if (!isPadMode2())
            new Handler().postDelayed(new HudToastAnimation(this,HudToastAnimation.TOAST_TUTORIAL), 200);
    }
    public void show_cm_download(){
        try {
            Intent intent_temp1 = new Intent(Intent.ACTION_VIEW);
            intent_temp1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isPadMode2()){
                intent_temp1.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.cleanmaster.mguard&referrer=utm_source%3D2010003495"));
            }else{
                intent_temp1.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.cleanmaster.mguard&referrer=utm_source%3D2010003494"));
            }
            startActivity(intent_temp1);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "No activity can handle this intent:");
        }
    }
    @Override
    public void onRecommendDialogDialogConfirmed(int mode) {
        if (mode == RecommendDialogFragment.mode_recommend_self){
        show_rateus();
        }else if (mode == RecommendDialogFragment.mode_recommend_cm){
            show_cm_download();

            GaPromote.getInstance().sendEvents(this, GaPromote.PROMOTE_CM_REDIRECT_TO_STORE,
                GaPromote.PROMOTE_CLICK_ACTION, null, null);

//            new DataCollectionTask(this,DataCollectionTask.PAGE_ID_CM_DOWNLOAD).execute();
        }
    }

    @Override
    public void onRecommendDialogDismissed(int mode) {
    }

    @Override
    public void onRecommendDialogNever(int mode) {
    }

    @Override
    public void onOnNewVersionDialogConfirmed(){
        show_instant_update();
    }

    @Override
    public void onOnNewVersionDismissed(){

    }

    private void show_instant_update(){
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //intent.setData(Uri.parse("market://search?q=pub:\"ZenUI,+ASUS+Computer+Inc.\""));
            intent.setData(Uri.parse("market://details?id=com.asus.filemanager"));
            intent.setClassName("com.android.vending", "com.google.android.finsky.activities.MainActivity");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            String weblink = "https://play.google.com/store/apps/details?id=com.asus.filemanager";

            if (WrapEnvironment.IS_CN_DEVICE) {
                weblink = "http://www.wandoujia.com/apps/com.asus.filemanager";
            }

            Intent intent_temp1 = new Intent(Intent.ACTION_VIEW);
            intent_temp1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent_temp1.setData(Uri.parse(weblink));

            List<ResolveInfo> result = getPackageManager().queryIntentActivities(intent_temp1, 0);
            if (result.size() == 0) {
                Intent intent_temp2 = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.asus.userfeedback"));
                intent_temp2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                result = getPackageManager().queryIntentActivities(intent_temp2, 0);
                if (result.size() == 0) {
                    Log.d(TAG, "No activity can handle this intent:"+intent_temp2.toString());
                } else {
                    startActivity(intent_temp2);
                }
            } else {
                startActivity(intent_temp1);
            }
        }
    }

    public void updateGTM(){
        TagManager tagManager = TagManager.getInstance(getApplicationContext());
        PendingResult<ContainerHolder> pending = tagManager
                .loadContainerPreferNonDefault(CONTAINER_ID, CONTAINER_RAW);
        pending.setResultCallback(this, 0, TimeUnit.SECONDS);
    }

    @Override
    public void onResult(final ContainerHolder containerHolder) {
        if (!containerHolder.getStatus().isSuccess()) {
            return;
        }

        containerHolder.getContainer();
        containerHolder.setContainerAvailableListener(new GtmContainerAvailableListener(this));

        if(mContainerRefreshTask != null && mContainerRefreshTask.getStatus() == Status.RUNNING)
        {
            mContainerRefreshTask.cancel(true);
            mContainerRefreshTask = null;
        }

        // Create a background thread to do the container refresh task.
        mContainerRefreshTask = new TagContainerRefreshTask();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run()
            {
                // Prevent NPE when user exit this page immediately.
                if(mContainerRefreshTask != null && mContainerRefreshTask.getStatus() == Status.PENDING)
                {
                    mContainerRefreshTask.execute(containerHolder);
                }
            }
        }, REFRESH_TASK_DELAY);
    }

    private void showWhatsNew(){

        long versionWhatsNew= PrefUtils.getLongVersionWhatsNew(this);

        PackageInfo pkgInfo;
        long myVersionCode= 0;
        try {
            pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            myVersionCode = pkgInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (versionWhatsNew == 0) {
            // don't show what's new dialog when first launch.
            PrefUtils.setLongVersionWhatsNew(this, myVersionCode);
        } else if (versionWhatsNew < myVersionCode){
            PrefUtils.setLongVersionWhatsNew(this, myVersionCode);
            WhatsNewDialogFragment whatsDialogFragment = WhatsNewDialogFragment.newInstance();
            whatsDialogFragment.setStyle(WhatsNewDialogFragment.STYLE_NORMAL, R.style.FMAlertDialogStyle);
            whatsDialogFragment.show(getFragmentManager(), WhatsNewDialogFragment.DIALOG_TAG);
        }
    }

    @Override
    public void onCtaDialogConfirmed(boolean bRemember) {
        SetNetworkEnabled(true);
        grantHomePageHasTemporaryCtaPermission(true);
    }

    @Override
    public void onCtaDialogDismissed(boolean bRemember) {
        SetNetworkEnabled(false);
    }

    private void grantHomePageHasTemporaryCtaPermission(boolean grantTemporaryCtaPermission) {
        FragmentManager fragmentManager = getFragmentManager();
        HomePageFragment homePageFragment = (HomePageFragment) fragmentManager.findFragmentById(R.id.homepage);
        if (homePageFragment != null) {
            homePageFragment.setTemporaryCtaPermission(grantTemporaryCtaPermission);
        }
    }

    private static final long GdriveRedeemCriteria = (long)100 * 1024 *1024 *1024;
    public void updateGDriveStorageUsage(String storageName, long totalQuota, long usedQuota){
        String total = Formatter.formatFileSize(this, totalQuota);

        for (int i =0;i<RemoteAccountUtility.getInstance(this).GDriveAccounts.size();i++) {
            if (RemoteAccountUtility.getInstance(this).GDriveAccounts.get(i).compareTo(storageName) == 0 ){
                RemoteAccountUtility.getInstance(this).totalGDriveQuotas[i] = totalQuota;
                break;
            }
        }
        for (int i=0;i<RemoteAccountUtility.getInstance(this).totalGDriveQuotas.length;i++){
           if (-1 == RemoteAccountUtility.getInstance(this).totalGDriveQuotas[i]) {
               RemoteAccountUtility.getInstance(this).getGDriveStorageUsage(this.getApplicationContext(), i);
               return;
           }
        }

        boolean bAllQuotaSmallerThanCriteria = true;
        for (int i=0;i<RemoteAccountUtility.getInstance(this).totalGDriveQuotas.length;i++){
            Log.d(TAG, "RemoteAccountInfo index = " + i + ", Quota = " + RemoteAccountUtility.getInstance(this).totalGDriveQuotas[i]);
            if (RemoteAccountUtility.getInstance(this).totalGDriveQuotas[i] >= GdriveRedeemCriteria || RemoteAccountUtility.getInstance(this).totalGDriveQuotas[i] == -1){
                bAllQuotaSmallerThanCriteria = false;
                break;
            }
        }

        String voucherStatus = reflectionApis.getSystemProperty("atd.voucher.intact","");
        Intent i= getPackageManager().getLaunchIntentForPackage("com.google.android.apps.docs");

        SharedPreferences mSharePrefence = this.getSharedPreferences("MyPrefsFile", 0);
        boolean bRedeemShowed = mSharePrefence.getBoolean("GDRIVE_REDEEM_SHOWED",false);

        if (bAllQuotaSmallerThanCriteria && voucherStatus.compareTo("1")==0 && null != i && !bRedeemShowed){
            mSharePrefence.edit().putBoolean("GDRIVE_REDEEM_SHOWED",true).commit();;

            android.support.v4.app.NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
            NotificationManager mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);


            i.putExtra("launchingAction", "SHOW_WELCOME");

            /*
            Intent i = new Intent(this, FileManagerActivity.class);
            i.addCategory(Intent.CATEGORY_LAUNCHER)
                .setAction(Intent.ACTION_MAIN);
                */
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT/*0*/);
            String strTitle = this.getString(R.string.notification_gdrive_redeem_title);
            String strContent = getString(R.string.notification_gdrive_redeem_content);

            Bitmap icon= BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_notification);
            int height = this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
            int width = this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

            int icon_width = Math.min(height, width);
            icon = Bitmap.createScaledBitmap(icon, icon_width, icon_width, false);

            mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle(mNotificationBuilder)
                .bigText(strContent)
                .setBigContentTitle(strTitle))
                .setContentTitle(strTitle)
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentText(strContent)
                    //.setColor(0xffffd73e)
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.ic_notification_filemanager);


            Notification aNotification = mNotificationBuilder.build();
            mNotificationManager.notify("update", 100, aNotification);

            GaPromote.getInstance().sendEvents(this, GaPromote.PROMOTE_REDEEM_GDRIVE,
                GaPromote.PROMOTE_CLICK_ACTION, null, null);
        }
    }

    @Override
    public PermissionManager getManager() {
        return permissionManager;
    }

    @Override
    public void permissionDeniedForever(ArrayList<String> permissions){
        // go to settings directly
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.putExtra(":settings:fragment_args_key", "permission_settings");
        intent.putExtra(":settings:fragment_args_key_highlight_times", 3);
        try {
            startActivityForResult(intent,FileManagerActivity.FILE_MANAGER_SETTING_PERMISSION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if ((requestCode != PermissionManager.REQUEST_PERMISSION  && requestCode != PermissionManager.RE_REQUEST_PERMISSION )|| grantResults.length == 0){
            return;
        }
        ArrayList<String> permToBeRequest = new ArrayList<>();
        ArrayList<Integer> reasonsToBeViewed = new ArrayList<>();

        boolean res = true;
        for (int i=0;i<grantResults.length;i++) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(permissions[i])) {
                res = false;
                reasonsToBeViewed.add(R.string.permission_essential);
                permToBeRequest.add(permissions[i]);
            }
        }
        if (requestCode == PermissionManager.REQUEST_PERMISSION){
            if (!(PermissionManager.checkPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {
                // we don't have necessary permission
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
                    //show popup and go to setting
                    PermissionDialog newFragment = PermissionDialog.newInstance(reasonsToBeViewed, permToBeRequest);
                    newFragment.show(getFragmentManager(), PermissionDialog.TAG);
                    return;
                }else{
                    //show reason and request permission again
                    permToBeRequest = new ArrayList<>();
                    reasonsToBeViewed = new ArrayList<>();

                    reasonsToBeViewed.add(R.string.permission_reason_storage);
                    permToBeRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    PermissionDialog newFragment = PermissionDialog.newInstance(reasonsToBeViewed, permToBeRequest,PermissionManager.REQUEST_PERMISSION);
                    newFragment.show(getFragmentManager(), PermissionDialog.TAG);
                    return;
                }
            }else{
                PermissionDialog dialogFragment = (PermissionDialog) (getFragmentManager().findFragmentByTag(PermissionDialog.TAG));
                if (null != dialogFragment && dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()){
                    dialogFragment.dismiss();
                }
                PermissionReasonDialogFragment dialogFragment2 = (PermissionReasonDialogFragment) (getFragmentManager().findFragmentByTag(PermissionReasonDialogFragment.TAG));
                if (null != dialogFragment2 && dialogFragment2.getDialog() != null && dialogFragment2.getDialog().isShowing()){
                    dialogFragment2.dismiss();
                }
            }
        }else if (requestCode == PermissionManager.RE_REQUEST_PERMISSION){
            if (!(PermissionManager.checkPermissions(this,
                new String[]{Manifest.permission.GET_ACCOUNTS}))) {
                // we don't have necessary permission
                if (!shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS) ) {
                    //show popup and go to setting
                    PermissionReasonDialogFragment newFragment = PermissionReasonDialogFragment.newInstance(
                        new String[]{Manifest.permission.GET_ACCOUNTS});
                    newFragment.show(getFragmentManager(), PermissionReasonDialogFragment.TAG);
                    return;
                }else{
                    //show reason and request permission again
                    permToBeRequest = new ArrayList<>();
                    reasonsToBeViewed = new ArrayList<>();

                    reasonsToBeViewed.add(R.string.permission_reason_contact);
                    permToBeRequest.add(Manifest.permission.GET_ACCOUNTS);

                    PermissionDialog newFragment = PermissionDialog.newInstance(reasonsToBeViewed, permToBeRequest,PermissionManager.RE_REQUEST_PERMISSION);
                    newFragment.show(getFragmentManager(), PermissionDialog.TAG);
                    return;
                }
            }else{
                //Contact Permission Granted
                startActivity(new Intent(this, AddCloudAccountActivity.class));

            }
        }


        SafOperationUtility.getInstance(this).clearWriteCapMap();

        postOnResume();
        showWhatsNew();
    }

    private void sendSecondaryStorageNameToGa() {
        String tag = "has_send_secondary_storage_name";

        // only send data for non-asus device
        if (((FileManagerApplication) getApplication()).isASUS()) {
            return;
        }

        SharedPreferences sharedPreference = getSharedPreferences("MyPrefsFile", 0);

         if (sharedPreference.getBoolean(tag, false)) {
             // only send this data once for each user.
         } else {
             String secondaryStorageName = null;
             try {
                 String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
                 // Add all secondary storages
                 if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                     // All Secondary SD-CARDs splitted into array
                     final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
                     if (null != rawSecondaryStorages && rawSecondaryStorages.length > 0) {
                         secondaryStorageName = new String(rawSecondaryStorages[0]);
                     }
                 }
            } catch (Throwable ignore){};

            String action = secondaryStorageName == null ? "empty" : "non-empty";

            GaExperiment.getInstance().sendEvents(this, GaExperiment.CATEGORY_SECONDARY_STORAGE,
                    action, secondaryStorageName, null);

            sharedPreference.edit().putBoolean(tag, true).commit();
         }
    }

    /**
     * Called this when show tutorial page to user is finished.
     */
    private void handleRequestTutorial()
    {
//     int status = CtaDialogFragment.checkCtaPermission(this);
//     if (status == CtaDialogFragment.CTA_REMEMBER_AGREE) {
//         SetNetworkEnabled(true);
//     }else if (status == CtaDialogFragment.CTA_REMEMBER_REFUSE){
//         SetNetworkEnabled(false);
//     }
        int status = CtaPreGrantDialogFragment.checkCtaPreGrantPermission(this);
        if (status == CtaPreGrantDialogFragment.CTA_REMEMBER_AGREE) {
            SetNetworkEnabled(true);
        } else if (status == CtaPreGrantDialogFragment.CTA_REMEMBER_REFUSE) {
            SetNetworkEnabled(false);
        }
    }

    public void requestContactPermission(){

        Map<String, Integer> map = new HashMap<>();
        map.put(Manifest.permission.GET_ACCOUNTS, R.string.permission_essential);
        PermissionDialog permissionDialog = (PermissionDialog) getFragmentManager().findFragmentByTag(PermissionDialog.TAG);

        if (map.size() > 0 && !(null != permissionDialog && permissionDialog.isAdded())) {
            if (permissionManager.requestPermissions(map, PermissionManager.RE_REQUEST_PERMISSION)) {
                //permission granted
            } else {
                //permission not granted
            }
        }

    }

    @Override
    public void onWhatsNewDialogConfirmed() {

    }

    @Override
    public void onWhatsNewDialogDismissed() {
        if(!mIsShowHomePageFragment) return;

        boolean NEEDTOSHOW_SHORTCUTHINT = PrefUtils.getBooleanNeedToShowShortcutHint(this);
        if(NEEDTOSHOW_SHORTCUTHINT){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                if(isInMultiWindowMode())
                {
                    PrefUtils.setBooleanNeedToShowShortcutHint(this, false);
                    return;
                }
            }
            HomePageFragment homePageFragment = (HomePageFragment) getFragmentManager().findFragmentById(R.id.homepage);
            if(homePageFragment == null) return;
            // start to show hint layout
            EditCategoryHintLayout hintlayout = new EditCategoryHintLayout(this, homePageFragment);
            if(hintlayout != null)
                hintlayout.show();
        }else{
            /* removed
            boolean NEEDTOSHOW_PULL_UP = mSharePrefence.getBoolean("NEEDTOSHOW_PULL_UP", true);
            if(NEEDTOSHOW_PULL_UP){
                show_move_up_animation();
                mSharePrefence.edit().putBoolean("NEEDTOSHOW_PULL_UP", false).commit();
            }
            */
        }

    }

    /** Refresh {@link Container} in background task to avoid ANR. */
    class TagContainerRefreshTask extends AsyncTask<ContainerHolder, Void, Void> {

        @Override
        protected Void doInBackground(@NonNull ContainerHolder... params)
        {
            Log.i(TAG, "TagContainerRefreshTask, ready to refresh container");
            // Only one thing to do, just call refresh.
            ContainerHolder holder = params[0];
            holder.refresh();

            return null;
        }
    }

    @Override
    public void onCtaPreGrantDialogConfirmed() {
        SetNetworkEnabled(true);
    }

    @Override
    public void onCtaPreGrantDialogDismissed() {
        SetNetworkEnabled(false);
        FileManagerActivity.this.finish();
    }

    @Override
    public void onUnRarSuccess(VFile extractedFile) {
        FileListFragment fileListFragment = getFileListFragment();
        if (fileListFragment != null) {
            fileListFragment.startScanFile(extractedFile, extractedFile.getVFieType());
        }
    }

    @Override
    public void onUnZipSuccess(VFile extractedFile) {
        FileListFragment fileListFragment = getFileListFragment();
        if (fileListFragment != null) {
            fileListFragment.startScanFile(extractedFile, extractedFile.getVFieType());
        }
    }
}
