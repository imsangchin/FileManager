package com.asus.filemanager.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.adapter.ClickIconItem;
import com.asus.filemanager.adapter.LocalStorageGridAdapter;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.adapter.StorageListAdapger.StorageItemElement;
import com.asus.filemanager.adapter.grouper.categoryparser.AppInstallReceiver;
import com.asus.filemanager.adapter.grouper.categoryparser.GameUtils;
import com.asus.filemanager.dialog.CreateShortCutDialogFragment;
import com.asus.filemanager.dialog.CtaDialogFragment;
import com.asus.filemanager.ga.GaBrowseFile;
import com.asus.filemanager.ga.GaCategorySorting;
import com.asus.filemanager.ga.GaHiddenCabinet;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.hiddenzone.activity.HiddenZoneIntroActivity;
import com.asus.filemanager.hiddenzone.activity.UnlockActivity;
import com.asus.filemanager.hiddenzone.encrypt.PinCodeAccessHelper;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.GameAppDbHelper;
import com.asus.filemanager.provider.GameAppProvider;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.ui.AutoResizeTextView;
import com.asus.filemanager.ui.EditCategoryHintLayout;
import com.asus.filemanager.ui.HintLayout;
import com.asus.filemanager.ui.ShortCutHintLayout;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.ViewUtility;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.updatesdk.ZenUiFamily;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class HomePageFragment extends Fragment implements OnClickListener, CategoryPreference.OnLoadCallback, Observer {

    private static final String TAG = HomePageFragment.class.getSimpleName();


    private static class CategoryCountTask extends AsyncTask<Object, Object, Void> {
        @Override
        protected Void doInBackground(Object... values) {
            int itemId = (Integer)values[0];
            Activity activity = (Activity)values[1];
            String count = calculateCategoryCount(itemId, activity);
            publishProgress(itemId, activity, count);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int itemId = (Integer)values[0];
            Activity activity = (Activity)values[1];
            String count = (String)values[2];

            AutoResizeTextView countView = (AutoResizeTextView)CategoryTableLayout.findCountViewByTag(activity, R.id.tablelayout, itemId);
            if (countView != null) {
                countView.setText(activity.getString(R.string.category_count, count));
            }
        }
    };

    public FileManagerActivity mActivity = null;
    private static final LocalVFile DIRECTORY_DOWNLOADS = new LocalVFile(FileManagerActivity.PATH_DOWNLOAD);

    private MenuItem mSearchItem;

    private GridView mLocalStorageListView;
    private LocalStorageGridAdapter mLocalStorageGridAdapter;
    private LinkedList<StorageItemElement> mLocalStorageItemElementList = new LinkedList<StorageItemElement>();
    PopupWindow mPopupWindow;

    List<CategoryItem> mCategorys = null;

    private boolean mHasTemporaryCtaPermission;
    private boolean mIsAttachToActivity; // isDetach() doesn't work well as we hope
    private PinCodeAccessHelper mPinCodeAccessHelper;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "HomePageFragment onAttach");

        mActivity = (FileManagerActivity) activity;
        mIsAttachToActivity = true;

        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HomePageFragment onCreate");
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "HomePageFragment onCreateView");
        return inflater.inflate(R.layout.home_page, container, false);
    }

     @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "HomePageFragment onDetach");
        mIsAttachToActivity = false;
        ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "HomePageFragment onActivityCreated");
        initView(null);
    }

    private void initView(List<CategoryItem> categorys) {
        if (!mIsAttachToActivity) {
            // when the fragment is detached from activity, we don't need to handle UI work.
            return;
        }
        View container = getView();
        if (null == categorys) {
            container.findViewById(R.id.category_edit).setOnClickListener(this);
            updateNewFeatureIcon(new String[] {
                CategorySortingActivity.kKeyNewFeatureIconCategoryDocument, CategorySortingActivity.kKeyNewFeatureIconCategoryGame
            }, false);
            CategoryPreference.postLoadFromPreference(mActivity, this);
            mLocalStorageListView = (GridView)  container.findViewById(R.id.local_storage_list);
            initLocalStorageList();
        } else {
            CategoryTableLayout.setTableLayoutContentView(mActivity, this, R.id.tablelayout, categorys, 3, this);
            loadAllCategoryCount();
        }

        ToolTableLayout.setTableLayoutContentView(getActivity(), R.id.home_page_tool_tablelayout, ToolPreference.createDefaultTools(getActivity()), 2, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.search_mode, menu);

        MenuItem searchItem = menu.findItem(R.id.search_action);

        // FIXME:
        // workaround for asus support library set icon fail in xml
        searchItem.setIcon(mActivity.getResources().getDrawable(R.drawable.asus_ic_search_grey));
        mSearchItem = searchItem;

        if (searchItem != null) {
            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    return true; // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                            GaSearchFile.ACTION_CLICK_SEARCH_ICON, null, null);
                    return true;
                }
            });

            SearchView mSearchView = (SearchView)MenuItemCompat.getActionView(searchItem);
            if (null != mSearchView) {
                //mSearchView.setMaxWidth(Integer.MAX_VALUE);
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
            mActivity.setupSearchViewExternal(mSearchView);

        }

        ThemeUtility.setMenuIconColor(getActivity(), menu);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu");

        MenuItem tempHideMenu = menu.findItem(R.id.search_action);
        if (tempHideMenu != null)
            tempHideMenu.setVisible(true);
        collapseSearchView();

        SharedPreferences mSharePrefence = mActivity.getSharedPreferences("MyPrefsFile", 0);
        boolean bNewFeature_settings = mSharePrefence.getBoolean("newfeature_settings", true);
        boolean bEnableInsider = mSharePrefence.getBoolean("EnableInsiderProgram", false);

        //
        if(ItemOperationUtility.getInstance().enableCtaCheck() || WrapEnvironment.IS_VERIZON
                || !"asus".equalsIgnoreCase(Build.BRAND)) {
            ViewUtility.hideMenuItem(menu,R.id.action_rateus);
        }
        //chenhsin--
        //mMenuStrs.add(getResources().getString(R.string.action_share));
        //mMenuID.add(R.id.action_share);
        if(WrapEnvironment.IS_VERIZON) {
            ViewUtility.hideMenuItem(menu,R.id.action_tell_a_friend);
        }
        if (!ItemOperationUtility.getInstance().enableCtaCheck() && !WrapEnvironment.IS_VERIZON) {
            int title_instant_update = ZenUiFamily.getZenUiFamilyTitle();
            menu.findItem(R.id.action_instant_update).setTitle(mActivity.getResources().getString(title_instant_update));
        }else{
            ViewUtility.hideMenuItem(menu,R.id.action_instant_update);
        }
        if(WrapEnvironment.IS_VERIZON) {
            ViewUtility.hideMenuItem(menu,R.id.action_bug_report);
        }

        if(ItemOperationUtility.getInstance().enableCtaCheck() || !bEnableInsider || WrapEnvironment.IS_VERIZON) {
            ViewUtility.hideMenuItem(menu,R.id.action_invite_betauser);
        }

        if (!ItemOperationUtility.getInstance().enableCtaCheck()) {
            ViewUtility.hideMenuItem(menu,R.id.cta_dialog);
        }

        boolean needSaf = ((FileManagerApplication)mActivity.getApplication()).isNeedSafPermission();
        if (!needSaf){
            ViewUtility.hideMenuItem(menu,R.id.saf_tutorial_action);
        }

        if (bNewFeature_settings) {
            ViewUtility.addNewIcon(mActivity, menu.findItem(R.id.action_settings));
        }

        boolean hasNewFeature = bNewFeature_settings;
        ThemeUtility.setThemeOverflowButton(mActivity, hasNewFeature);
    }

    public void collapseSearchView(){
       if((!mActivity.isPadMode()) && (mSearchItem != null)){
           mSearchItem.collapseActionView();
           mActivity.hideSoftKeyboard();
       }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (mActivity.isSeachViewIsShow()) {
            Log.w(TAG, "searchview is show");
            return ;
        }
        updateLocalStorageList();
        resume();
        merryXmasAndHappyNewYear();


        View cmView = ToolTableLayout.findViewByTag(mActivity,
                R.id.home_page_tool_tablelayout, ToolItem.CLEAN_MASTER);
        if (cmView == null) {
            // no clean master tool
            if (ToolPreference.showCleanMaster(mActivity)) {
                // need to show it
                ToolTableLayout.setTableLayoutContentView(getActivity(),
                        R.id.home_page_tool_tablelayout,
                        ToolPreference.createDefaultTools(getActivity()), 2,
                        this);
            }
        } else {
            // had clean master tool
            if (!ToolPreference.showCleanMaster(mActivity)) {
                // don't show it
                ToolTableLayout.setTableLayoutContentView(getActivity(),
                        R.id.home_page_tool_tablelayout,
                        ToolPreference.createDefaultTools(getActivity()), 2,
                        this);
            }
        }

        View recommendView = ToolTableLayout.findRecommendViewByTag(mActivity, R.id.home_page_tool_tablelayout, ToolItem.CLEAN_MASTER);
        if (recommendView!=null){
            if (WrapEnvironment.isCMPackagePreloadedAndExist(mActivity)){
                recommendView.setVisibility(View.INVISIBLE);
            }else {
                recommendView.setVisibility(View.VISIBLE);
                }
            }
        }

    private void merryXmasAndHappyNewYear() {
        View container = getView();

        if (container == null) {
            return;
        }

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date xmasHead = df.parse("2015-12-24");
            Date xmasTail = df.parse("2015-12-27");
            Date newYearHead = df.parse("2016-01-01");
            Date newYearTail = df.parse("2016-01-03");
            Date currentDate =  new Date();

            ImageView eventSpirit = (ImageView) container.findViewById(R.id.category_edit);
            if (eventSpirit != null) {
                if ((currentDate.after(xmasHead) && currentDate.before(xmasTail))) {
                    eventSpirit.setImageDrawable(getResources().getDrawable(R.drawable.xmas));
                } else if (currentDate.after(newYearHead) && currentDate.before(newYearTail)) {
                    eventSpirit.setImageDrawable(getResources().getDrawable(R.drawable.newyear));
                } else {
                    eventSpirit.setImageResource(R.drawable.ic_icon_listview);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        mActivity = (FileManagerActivity) getActivity();
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null && mActivity.mIsShowHomePageFragment) {
            fileListFragment.setmIndicatorFile(mActivity.CATEGORY_HOME_PAGE_FILE);
        }

        loadAllCategoryCount();
        // force to show animation
        if (mLocalStorageGridAdapter != null) {
            mLocalStorageGridAdapter.resume();
        }
    }

    public void setTemporaryCtaPermission(boolean grantTemporaryCtaPermission) {
        mHasTemporaryCtaPermission =  grantTemporaryCtaPermission;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        checkNeedReDrawHintLayout();
    }

    private void checkNeedReDrawHintLayout(){
        RelativeLayout hintLayout = (RelativeLayout) getActivity().findViewById(R.id.hint_layout);
        if(hintLayout != null && hintLayout.isShown()){
            ViewGroup vg = (ViewGroup)(getActivity().getWindow().getDecorView().getRootView());
            if(vg == null) {
                Log.e(TAG, "vg null");
                return;
            }

            vg.removeView(hintLayout);

            // check whether show editCategory or shortcut hint
            Button btn = (Button) hintLayout.findViewById(R.id.hint_shortcut_ok);
            if(btn != null){
                // delay 0.1 s to wait completion of drawing tablelayout
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    ShortCutHintLayout hintlayout = new ShortCutHintLayout(getActivity(), HomePageFragment.this, mCategorys.get(0).id);
                    if(hintlayout != null)
                        hintlayout.show();
                    }
                }, 500);
                return;
            }

            btn = (Button) hintLayout.findViewById(R.id.hint_categoryedit_ok);
            if(btn != null){
                // delay 0.1 s to wait completion of drawing tablelayout
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    EditCategoryHintLayout hintlayout = new EditCategoryHintLayout(getActivity(), HomePageFragment.this);
                    if(hintlayout != null)
                        hintlayout.show();
                    }
                }, 500);
            }
        }
    }

    private static String calculateCategoryCount(int itemId, Context context) {
        switch (itemId) {
        case CategoryItem.IMAGE:
            return "" + MediaProviderAsyncHelper.getImageFilesCount(context, false);
        case CategoryItem.VIDEO:
            return "" + MediaProviderAsyncHelper.getVideoFilesCount(context, false);
        case CategoryItem.MUSIC:
            return "" + MediaProviderAsyncHelper.getMusicFilesCount(context, false);
        case CategoryItem.DOWNLOAD:
            File[] files = DIRECTORY_DOWNLOADS.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (FileListFragment.sShowHidden || !pathname.isHidden()) {
                        return true;
                    }
                    return false;
                }
            });
            return "" + ((files == null) ? 0 : files.length) + (containsFolder(files) ? "+" : "");
        case CategoryItem.FAVORITE:
            return "" + ProviderUtility.FavoriteFiles.getCount(context.getContentResolver(), FileListFragment.sShowHidden);
        case CategoryItem.APP:
            return "" + MediaProviderAsyncHelper.getFilesCountByExtension(context, new String[] {"apk"}, FileListFragment.sShowHidden);
        case CategoryItem.DOCUMENT:
            // FIXME:
            // For TT-686562: aosp device can't recognize ppt/pptx files (mime_type = null)
            // we query ppt/pptx files again by extName
            return "" + MediaProviderAsyncHelper.getFilesCountByMimeTypeAndExtName(context,
                FileManagerActivity.SUPPORT_MIMETYPE_IN_DOCUMENTS_CATEGORY,
                FileManagerActivity.SUPPORT_EXTENSION_IN_PPT_CATEGORY,
                FileListFragment.sShowHidden, false
            );
        case CategoryItem.COMPRESSED:
            return "" + MediaProviderAsyncHelper.getFilesCountByExtension(context,
                FileManagerActivity.SUPPORT_EXTENSION_IN_COMPRESS_CATEGORY, FileListFragment.sShowHidden);
        case CategoryItem.RECENT:
            return "" + MediaProviderAsyncHelper.getRecentFiles(context, FileListFragment.sShowHidden).size();
        case CategoryItem.LARGE_FILE:
            return "" + MediaProviderAsyncHelper.getFilesCountBySize(
                context, FileManagerActivity.SUPPORT_LARGE_FILES_THRESHOLD,
                FileListFragment.sShowHidden, false
            );
        case CategoryItem.PDF:
            return "" + MediaProviderAsyncHelper.getFilesCountByMimeType(context,
                FileManagerActivity.SUPPORT_EXTENSION_IN_PDF_CATEGORY, FileListFragment.sShowHidden, false);
        case CategoryItem.GAME:
            return "" + GameAppDbHelper.queryGameLaunchFileFromDb(context).size();
        }
        return "0";
    }

    private void loadAllCategoryCount() {
        if (!new PermissionManager(mActivity).checkPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}))
            return;
        Log.d(TAG, "loadAllCategoryCount");
        TableLayout tableLayout = (TableLayout)mActivity.findViewById(R.id.tablelayout);
        boolean hasGameCategoryInHomePage = false;
        final boolean ctaAccept = mHasTemporaryCtaPermission
                ||(CtaDialogFragment.checkCtaPermission(mActivity, false) == CtaDialogFragment.CTA_REMEMBER_AGREE);
        if (tableLayout != null) {
            int tableSize = tableLayout.getChildCount();
            int rowSize = 0;
            TableRow tableRow = null;
            RelativeLayout item = null;
            for (int i = 0; i < tableSize; ++i) {
                tableRow = (TableRow)tableLayout.getChildAt(i);
                rowSize = tableRow.getChildCount();
                for (int j = 0; j < rowSize; ++j) {
                    item = (RelativeLayout)tableRow.getChildAt(j);
                    new CategoryCountTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR,
                        item.getTag(),
                        mActivity
                    );

                    if (((int)item.getTag()) == CategoryItem.GAME) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (ctaAccept) {
                                    GameUtils.parseAppCategoryForInstalledApp(mActivity);
                                }
                            }
                        }).start();
                        hasGameCategoryInHomePage = true;
                    }
                }
            }
        }
        // make sure GameApp content provider always enabled.
        // AppInstall receiver is enabled only if Game category exists in homepage.
        GameUtils.setAppComponentEnabled(mActivity, GameAppProvider.class, true);
        GameUtils.setAppComponentEnabled(mActivity, AppInstallReceiver.class, hasGameCategoryInHomePage && ctaAccept);
    }

    private static boolean containsFolder(File[] files) {
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    private int measureContentWidth(ListAdapter adapter) {
        // Menus don't tend to be long, so this is more sane than it looks.
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
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
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showPopup(View anchorView) {
        mPopupWindow = new ListPopupWindow(this.mActivity);
        mMenuStrs = new ArrayList<String>();
        mMenuID = new ArrayList<Integer>();
        ArrayList<Integer> newFeatureList = new ArrayList<Integer>();


        mMenuStrs.add(getResources().getString(R.string.action_clear_history));
        mMenuID.add(R.id.clear_history_action);

        if(!ItemOperationUtility.getInstance().enableCtaCheck() && !WrapEnvironment.IS_VERIZON) {
            mMenuStrs.add(getResources().getString(R.string.toolbar_item_title_encourage_us));
            mMenuID.add(R.id.action_rateus);

            SharedPreferences mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
            boolean bNewFeature= mSharePrefence.getBoolean("newfeature_rateus", true);
            if (bNewFeature)
                newFeatureList.add(mMenuID.size()-1);
        }
		//chenhsin--
        //mMenuStrs.add(getResources().getString(R.string.action_share));
        //mMenuID.add(R.id.action_share);
        if(!WrapEnvironment.IS_VERIZON) {
            mMenuStrs.add(getResources().getString(R.string.invite_friends));
            mMenuID.add(R.id.action_tell_a_friend);
        }
        if (!ItemOperationUtility.getInstance().enableCtaCheck() && !WrapEnvironment.IS_VERIZON) {
            int title_instant_update = ZenUiFamily.getZenUiFamilyTitle();
            mMenuStrs.add(getResources().getString(title_instant_update));
            mMenuID.add(R.id.action_instant_update);
        }
        if(!WrapEnvironment.IS_VERIZON) {
            mMenuStrs.add(getResources().getString(R.string.uf_sdk_report_bug));
            mMenuID.add(R.id.action_bug_report);
        }

        SharedPreferences mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
        boolean bEnableInsider = mSharePrefence.getBoolean("EnableInsiderProgram", false);

        if(!ItemOperationUtility.getInstance().enableCtaCheck() && bEnableInsider && !WrapEnvironment.IS_VERIZON) {
            mMenuStrs.add(getResources().getString(R.string.recruit_beta_user));
            mMenuID.add(R.id.action_invite_betauser);

            boolean bNewFeature= mSharePrefence.getBoolean("newfeature_beta_user", true);
            if (bNewFeature)
                newFeatureList.add(mMenuID.size()-1);
        }
        if (ItemOperationUtility.getInstance().enableCtaCheck()) {
            mMenuStrs.add(getResources().getString(R.string.cta_show_menu));
            mMenuID.add(R.id.cta_dialog);
        }

        boolean needSaf = ((FileManagerApplication)mActivity.getApplication()).isNeedSafPermission();
        if (needSaf){
            mMenuStrs.add(getResources().getString(R.string.saf_tutorial_title));
            mMenuID.add(R.id.saf_tutorial_action);
        }

        // sylvia++ 20160322
        mMenuStrs.add(getResources().getString(R.string.action_settings));
        mMenuID.add(R.id.action_settings);

        mMenuStrs.add(getResources().getString(R.string.action_about));
        mMenuID.add(R.id.about_action);
        listpopupAdapter adapter = new listpopupAdapter(this.mActivity, R.layout.popup_menu_item_layout,mMenuStrs);

        adapter.setIsNewFeature(newFeatureList);

        mPopupWindow.setAdapter(adapter);
        mPopupWindow.setModal(true);
        if (!mHasContentWidth) {
            mContentWidth = measureContentWidth(adapter);
            mHasContentWidth = true;
        }
        mPopupWindow.setContentWidth(mContentWidth);
        mPopupWindow.setAnchorView(anchorView);
        mPopupWindow.setOnItemClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mPopupWindow.setDropDownGravity(GravityCompat.END);
        mPopupWindow.show();

    }
    */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult requestCode=" + requestCode + ", resultCode=" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileManagerActivity.REQUEST_CATEGORY_EDIT) {
            updateNewFeatureIcon(new String[] {
                CategorySortingActivity.kKeyNewFeatureIconCategoryDocument, CategorySortingActivity.kKeyNewFeatureIconCategoryGame
            }, true);
            if (resultCode == Activity.RESULT_OK) {
                CategoryPreference.postLoadFromPreference(mActivity, this);
                GaCategorySorting.getInstance().sendEvents(mActivity, GaCategorySorting.CATEGORY_NAME,
                        GaCategorySorting.ACTION_SORTING_RESULT_OK, null, null);
            } else {
                GaCategorySorting.getInstance().sendEvents(mActivity, GaCategorySorting.CATEGORY_NAME,
                        GaCategorySorting.ACTION_SORTING_RESULT_CANCEL, null, null);
            }
        } else if (requestCode == FileManagerActivity.REQUEST_REGISTER_PASSWORD && resultCode == Activity.RESULT_OK) {
            mActivity.switchFragmentTo(FragmentType.HIDDEN_ZONE);
            Toast.makeText(mActivity, R.string.hidden_zone_set_password_success,
                    Toast.LENGTH_SHORT).show();

            PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
            GaHiddenCabinet.getInstance().sendSetupPasswordEvent(
                    mActivity, pinCodeAccessHelper.hasRecoveryAccount());
        } else if (requestCode == FileManagerActivity.REQUEST_UNLOCK && resultCode == Activity.RESULT_OK) {
            mActivity.switchFragmentTo(FragmentType.HIDDEN_ZONE);
            Toast.makeText(mActivity, getResources().getString(
                    R.string.toast_auto_lock_notify_message), Toast.LENGTH_SHORT).show();

            PinCodeAccessHelper pinCodeAccessHelper = PinCodeAccessHelper.getInstance();
            boolean unlockByFingerprint = data != null ?
                    data.getBooleanExtra(UnlockActivity.KEY_UNLOCK_VIA_FINGERPRINT, false) : false;
            GaHiddenCabinet.getInstance().sendUnlockFromHomepageEvent(
                    mActivity, unlockByFingerprint);
        }
    }

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        if(tag != null){
            // case of hint layout
            if(tag.toString() == ConstantsUtil.LAYOUT_HINT){
                HintLayout hintLayout = HintLayout.getCurrentInstance(getActivity());
                if(hintLayout == null) return;
                if(view.getId() == R.id.hint_categoryedit_ok){
                    hintLayout.removeHintLayout();
                    // show shortcut hint layout
                    ShortCutHintLayout hintlayout = new ShortCutHintLayout(getActivity(), HomePageFragment.this, mCategorys.get(0).id);
                    hintlayout.show();
                }
                else if(view.getId() == R.id.hint_shortcut_ok){
                    hintLayout.removeHintLayout();
                }
                return;
            }
        }
        collapseSearchView();
        int viewId = view.getId();
        if (R.id.category_edit == viewId) {
            startActivityForResult(new Intent(mActivity, CategorySortingActivity.class), FileManagerActivity.REQUEST_CATEGORY_EDIT);
            return ;
        }

        if(viewId == R.id.tool_button_enter)
            viewId = (Integer)view.getTag();
        if (ToolItem.HTTP_FILE_TRANSFER == viewId) {
            Intent httpServerIntent = new Intent();
            httpServerIntent.setClass(mActivity, HttpServerActivity.class);
            mActivity.startActivity(httpServerIntent);
            return ;
        } else if (ToolItem.CLEAN_MASTER == viewId) {
//            if (WrapEnvironment.isCMPackagePreloadedAndExist(mActivity)){
//                WrapEnvironment.launchCM(mActivity);
//                GaPromote.getInstance().sendEvents(mActivity, GaPromote.PROMOTE_CM_LAUNCH,
//                    GaPromote.PROMOTE_CLICK_ACTION, null, null);
//            }else{
//                SharedPreferences mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
//                boolean bRECOMMEND_CM_DIALOG= mSharePrefence.getBoolean("RECOMMEND_CM_DIALOG", false);
//
//                if (bRECOMMEND_CM_DIALOG){
//                    mActivity.displayDialog(FileManagerActivity.DialogType.TYPE_RECOMMEND_CM_DIALOG, 0);
//                }else{
//                    mActivity.show_cm_download();
//                    GaPromote.getInstance().sendEvents(mActivity, GaPromote.PROMOTE_CM_REDIRECT_TO_STORE,
//                        GaPromote.PROMOTE_CLICK_ACTION, null, null);
//
////                    new DataCollectionTask(getActivity(),DataCollectionTask.PAGE_ID_CM_DOWNLOAD).execute();
//                }
//            }
            Toast.makeText(mActivity, "接垃圾清理接口", Toast.LENGTH_SHORT).show();
            return ;
        } else if (ToolItem.STORAGE_ANALYZER == viewId) {
            Intent storageAnalyzerIntent = new Intent(mActivity, StorageAnalyzerActivity.class);
            mActivity.startActivity(storageAnalyzerIntent);
            GaStorageAnalyzer.getInstance().sendEvents(mActivity,GaStorageAnalyzer.CATEGORY_NAME,GaStorageAnalyzer.ACTION_ANALYZER_PAGE,null,null);
            return;
        } else if (ToolItem.ZENUI_UPDATE == viewId) {
            if (Utility.isMonkeyRunning()) {
                return;
            }
            ZenUiFamily.launchZenUiFamily(getActivity());
            GaMenuItem.getInstance().sendEvents(mActivity, GaMenuItem.CATEGORY_NAME,
                    GaMenuItem.ACTION_INSTANT_UPDATE, null, null);
            return;
        } else if (ToolItem.RECYCLE_BIN == viewId) {
            //RecycleBinFragment recycleBinFragment = (RecycleBinFragment) getFragmentManager().findFragmentById(R.id.recyclebin);
            //if (recycleBinFragment != null && recycleBinFragment.isHidden()) {
                mActivity.switchFragmentTo(FragmentType.RECYCLE_BIN);
            //}
            //GaStorageAnalyzer.getInstance(mActivity).sendEvents(mActivity,GaStorageAnalyzer.CATEGORY_NAME,GaStorageAnalyzer.ACTION_ANALYZER_PAGE,null,null);
            return;
        } else if (ToolItem.HIDDEN_ZONE == viewId) {
            mPinCodeAccessHelper = PinCodeAccessHelper.getInstance();
            if (mPinCodeAccessHelper.hasPinCode()) {
                Intent intent = new Intent(mActivity, UnlockActivity.class);
                intent.putExtra(UnlockActivity.KEY_SHOULD_SHOW_MENU, true);
                startActivityForResult(intent, FileManagerActivity.REQUEST_UNLOCK);
            } else {
                startActivityForResult(new Intent(mActivity, HiddenZoneIntroActivity.class),
                        FileManagerActivity.REQUEST_REGISTER_PASSWORD);
            }
            return;
        }

        LocalVFile category = null;
        String label = null;
        if (R.id.item == viewId) {
            viewId = (Integer)view.getTag();
        }
        switch (viewId) {
        case CategoryItem.IMAGE:
            category = mActivity.CATEGORY_IMAGE_FILE;
            label = GaBrowseFile.LABEL_IMAGE_SHORTCUT;
            break;
        case CategoryItem.MUSIC:
            category = mActivity.CATEGORY_MUSIC_FILE;
            label = GaBrowseFile.LABEL_MUSIC_SHORTCUT;
            break;
        case CategoryItem.VIDEO:
            category = mActivity.CATEGORY_VIDEO_FILE;
            label = GaBrowseFile.LABEL_VIDEO_SHORTCUT;
            break;
        case CategoryItem.APP:
            category = mActivity.CATEGORY_APK_FILE;
            label = GaBrowseFile.LABEL_APP_SHORTCUT;
            break;
        case CategoryItem.FAVORITE:
            category = mActivity.CATEGORY_FAVORITE_FILE;
            label = GaBrowseFile.LABEL_FAVORITE_SHORTCUT;
            break;
        case CategoryItem.DOWNLOAD:
            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            if (fileListFragment != null && fileListFragment.isHidden()) {
                mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);
            }
            fileListFragment.startScanFile(DIRECTORY_DOWNLOADS, ScanType.SCAN_CHILD);
            label = GaBrowseFile.LABEL_DOWNLOAD_SHORTCUT;
            break;
        case CategoryItem.DOCUMENT:
            category = mActivity.CATEGORY_DOCUMENT_FILE;
            label = GaBrowseFile.LABEL_DOCUMENTS_SHORTCUT;
            break;
        case CategoryItem.COMPRESSED:
            category = mActivity.CATEGORY_COMPRESS_FILE;
            label = GaBrowseFile.LABEL_COMPRESS_SHORTCUT;
            break;
        case CategoryItem.RECENT:
            category = mActivity.CATEGORY_RECENT_FILE;
            label = GaBrowseFile.LABEL_RECENT_SHORTCUT;
            break;
        case CategoryItem.LARGE_FILE:
            category = mActivity.CATEGORY_LARGE_FILE;
            label = GaBrowseFile.LABEL_LARGE_FILE_SHORTCUT;
            break;
        case CategoryItem.PDF:
            category = mActivity.CATEGORY_PDF_FILE;
            label = GaBrowseFile.LABEL_PDF_SHORTCUT;
            break;
        case CategoryItem.GAME:
            category = mActivity.CATEGORY_GAME_FILE;
            label = GaBrowseFile.LABEL_GAME_SHORTCUT;
            break;
        }

        mActivity.mIsShowHomePageFragment = false;

        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            if(fileListFragment.isHidden())
                mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);
            fileListFragment.startScanCategory(category);
        }

        GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,
                GaBrowseFile.ACTION_BROWSE_FROM_SHORTCUT, label, null);
    }

    public void initLocalStorageList(){
        mLocalStorageGridAdapter = new LocalStorageGridAdapter(mActivity);
        mLocalStorageListView.setAdapter(mLocalStorageGridAdapter);
        mLocalStorageListView.setOnItemClickListener(mLocalStorageGridAdapter);
        registerForContextMenu(mLocalStorageListView);
    }

    /*public void updateLocalStorageList(LinkedList<StorageItemElement> storageItemElementList){
        mLocalStorageItemElementList.clear();

        for(int i = 0; i < storageItemElementList.size(); i ++){
            StorageItemElement storageItemElement = storageItemElementList.get(i);
            if(storageItemElement.storageType == StorageListAdapger.STORAGETYPE_LOCAL)
                mLocalStorageItemElementList.add(storageItemElement);
        }

        mLocalStorageGridAdapter.updateAdapter(mLocalStorageItemElementList, false);
    }*/

    private void updateLocalStorageList(){
        ArrayList<Object> homePageLocalStorageElementList = new ArrayList<Object>();
        ArrayList<VFile> homePageLocalStorageFile = new ArrayList<VFile>();

        final StorageManager mStorageManager = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume = ((FileManagerApplication)mActivity.getApplication()).getStorageVolume();

        if(storageVolume.size() == 1) {
            mLocalStorageListView.setNumColumns(1);
        } else {
            mLocalStorageListView.setNumColumns(2);
        }

        VFile[] tmpVFiles = ((FileManagerApplication)mActivity.getApplication()).getStorageFile();
        for (int i = 0; i < storageVolume.size(); i ++) {
            String path = reflectionApis.volume_getPath(storageVolume.get(i));
            if (mStorageManager != null && (reflectionApis.getVolumeState(mStorageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)
                  /*   mark the code to prevent adding the item of MICROSD which in un-mount state
                   *   [TT-839686]
                  || WrapEnvironment.MICROSD_CANONICAL_PATH.equals(path)
                  || "/storage/sdcard1".equals(path)
                  */
             )){
              homePageLocalStorageElementList.add(storageVolume.get(i));
              homePageLocalStorageFile.add(tmpVFiles[i]);
            }
        }

        mLocalStorageItemElementList.clear();
        for(int i = 0; i < homePageLocalStorageElementList.size(); i ++){
            StorageItemElement storageItemEelement = new StorageItemElement();
            storageItemEelement.storageVolume = homePageLocalStorageElementList.get(i);
            storageItemEelement.vFile = homePageLocalStorageFile.get(i);
            storageItemEelement.storageType = StorageListAdapger.STORAGETYPE_LOCAL;
            mLocalStorageItemElementList.add(storageItemEelement);
        }
        mLocalStorageGridAdapter.updateAdapter(mLocalStorageItemElementList, true);
    }

    @Override
    public void onLoadDone(List<CategoryItem> categorys) {
        Log.d(TAG, "onLoadDone");
        mCategorys = CategoryPreference.extractSelectedCategoryItems(categorys);
        initView(mCategorys);
    }

    private void updateNewFeatureIcon(String[] keys, boolean isForceRemoveIcon) {
        if (isForceRemoveIcon) {
            SharedPreferences.Editor prefEdit = mActivity.getSharedPreferences(CategorySortingActivity.TAG, Context.MODE_PRIVATE).edit();
            for (String key: keys) {
                prefEdit.putBoolean(key, false);
            }
            prefEdit.commit();
        }
        /*
        ImageView categoryEditNewFeatureIcon = (ImageView)mActivity.findViewById(R.id.category_edit_new_feature_icon);
        if (categoryEditNewFeatureIcon != null) {
            // Note: getSharedPreferences name should sync with CategorySortingActivity.TAG
            // relative method: CategorySortingActivity.updateNewFeatureIcon
            SharedPreferences pref = mActivity.getSharedPreferences(CategorySortingActivity.TAG, Context.MODE_PRIVATE);
            for (String key: keys) {
                if (pref.getBoolean(key, true)) {
                    categoryEditNewFeatureIcon.setVisibility(View.VISIBLE);
                    return ;
                }
            }
            categoryEditNewFeatureIcon.setVisibility(View.GONE);
        }
        */
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info != null && info.targetView.getId() == R.id.local_storage_grid_item) {
            ClickIconItem iconItem = mLocalStorageGridAdapter.getClickedIconItem(info.targetView);
            //CreateShortcutUtil.createFolderShortcut(mActivity, iconItem.file.getPath(), iconItem.storageName);

            CreateShortCutDialogFragment shortcutDialog = CreateShortCutDialogFragment.newInstance(iconItem.file.getPath(), iconItem.storageName);
            shortcutDialog.show(getFragmentManager(), "");
        }else{
            int viewId = v.getId();
            if (R.id.item == viewId) {
                viewId = (Integer)v.getTag();
            }

            CreateShortCutDialogFragment shortcutDialog = CreateShortCutDialogFragment.newInstance(viewId);
            shortcutDialog.show(getFragmentManager(), "");
        }

        //menu.setHeaderTitle(R.string.action_edit);
        //menu.add(Menu.NONE, viewId, Menu.NONE, R.string.create_shortcut_dialog);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info != null && info.targetView.getId() == R.id.local_storage_grid_item) {
            mLocalStorageGridAdapter.createShortcutByView(info.targetView);
            GaShortcut.getInstance().sendEvents(mActivity,GaShortcut.CATEGORY_NAME,
                    GaShortcut.ACTION_CREATE_FROM_HOMEPAGE, GaShortcut.LABEL_NON_CATEGORY, null);
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {
        if(getActivity() != null) {
            updateLocalStorageList();
            loadAllCategoryCount();
        }
    }

}
