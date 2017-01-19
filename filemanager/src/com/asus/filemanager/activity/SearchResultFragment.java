
package com.asus.filemanager.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment.OnShowDialogListener;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerActivity.FontType;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.adapter.BasicFileListAdapter.CheckResult;
import com.asus.filemanager.adapter.SearchListAdapter;
import com.asus.filemanager.adapter.listpopupAdapter;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.ZipDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.provider.SearchHistoryProvider;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.CreateShortcutUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FixedListFragment;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.SortUtility.SortType;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.ViewUtility;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.updatesdk.ZenUiFamily;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class SearchResultFragment extends FixedListFragment implements OnClickListener, OnItemClickListener, SafOperationUtility.SafActionHandler {


    private static final boolean DEBUG = ConstantsUtil.DEBUG;
    private static final String TAG = "SearchResultFragment";

    private static final int SORT_TYPE = 0;
    private static final int SORT_NAME = 1;
    private static final int SORT_SIZE = 2;
    private static final int SORT_DATE = 3;
    private static final int SORT_LOCATION = 4;

    private SearchListAdapter mAdapter;

    private TextView mSearchTile;
    private String mTitle;
    public OnShowDialogListener mShowDialogListener;

    private View nameContainer;
    private View sizeContainer;
    private View typeContainer;
    private View dateContainer;
    private View locationContainer;
    private LinearLayout searchTitleContainer;
    private ImageView[] mSortImage = new ImageView[5];
    private int mSortType = SortType.SORT_LOCATION_DOWN;

    // ++ Alex
    private FileManagerActivity mActivity = null;
    // --

    // +++ cloud storage
    private ListView mListView;
    private boolean mIsScrollingList = false;
    // ---

    private SearchView mSearchView;
    private MenuItem mSearchItem;

    //edit result view
    private EditPool mDeleteFilePool = new EditPool();
    private VFile mRenameFile = null;
    private VFile mOldFile = null;

    private ActionMode mEditMode;

    private ListPopupWindow mPopupWindow;
    boolean mHasContentWidth = false;
    int mContentWidth;
    ArrayList<String> mMenuStrs;
    ArrayList<Integer> mMenuID;

    private ListPopupWindow mEditModePopupWindow;
    boolean mHasEditModeContentWidth = false;
    int mEditModeContentWidth;
    ArrayList<String> mEditModeMenuStrs;
    ArrayList<Integer> mEditModeMenuID;
    ArrayList<Integer> mEditModeNewFeatureList;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mShowDialogListener = (OnShowDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
        mActivity = (FileManagerActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.bForceListMode = true;
        if (DEBUG)
            Log.d(TAG, "SearchResultFragment onCreate");
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            Log.d(TAG, "SearchResultFragment onCreateView");
        return inflater.inflate(R.layout.searchlist_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

            Log.d(TAG, "SearchResultFragment onActivityCreated");

        setHasOptionsMenu(true);

        initView();

        if (mAdapter == null) {
            mAdapter = new SearchListAdapter(this, null);
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(mAdapter);
        getListView().setOnItemLongClickListener(mAdapter);

        mAdapter.setOrientation(getResources().getConfiguration().orientation);

    }

    @Override
    public void onResume() {
        super.onResume();

            Log.d(TAG, "SearchResultFragment onResume");
    }

    @Override
    public void onPause() {
        super.onPause();

            Log.d(TAG, "SearchResultFragment onPause");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_mode, menu);

        /*
        View aBadge = MenuItemCompat.getActionView(menu.findItem(R.id.action_menu)).findViewById(R.id.ImgView_badge);
        if (bNewFeature_rateus || (bNewFeature_betauser && bEnableInsider)){
            aBadge.setVisibility(View.VISIBLE);
        }else{
            aBadge.setVisibility(View.GONE);
        }
        */

        MenuItem searchItem = menu.findItem(R.id.search_action);

        mSearchItem = searchItem;

        if (searchItem != null) {
            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    // Do something when collapsed
                    Log.i(TAG, "onMenuItemActionCollapse " + item.getItemId());

                    Log.i(TAG, "onMenuItemActionCollapse " + item.getItemId());
                    FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    if (fileListFragment != null && fileListFragment.isHidden()) {
                        mActivity.showSearchFragment(FileManagerActivity.FragmentType.NORMAL_SEARCH, false);

                        VFile indicatorFile = fileListFragment.getIndicatorFile();
                        if (indicatorFile != null) {
                            fileListFragment.startScanFile(indicatorFile, ScanType.SCAN_CHILD, false);
                        }
                    }

                    return true; // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    Log.i(TAG, "onMenuItemActionExpand " + item.getItemId());
                    return true;
                }
            });

            mSearchView = (SearchView)MenuItemCompat.getActionView(searchItem);
            mActivity.setupSearchViewExternal(mSearchView);

           if (!mActivity.isPadMode()) {
               searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM
                        | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            }
        }
        /*
        MenuItem moreItem = menu.findItem(R.id.action_menu);
        View actionView = moreItem.getActionView();
        if (null != actionView){
            actionView.setOnClickListener(this);
        }
        */
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu");

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
        ThemeUtility.setMenuIconColor(mActivity, menu);
    }

    public MenuItem getSearchItem() {
        return mSearchItem;
    }

    public void onBackPressed() {

            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            if (fileListFragment != null && fileListFragment.isHidden()) {
                mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);

                VFile indicatorFile = fileListFragment.getIndicatorFile();
                if (indicatorFile != null) {
                    fileListFragment.startScanFile(indicatorFile, ScanType.SCAN_CHILD, false);
                }
            } else {

                if (fileListFragment == null || fileListFragment.onBackPressed()) {
                    mActivity.onBackPressed();
                }
            }

    }

    public void clearSearchViewFocus() {
        if (mSearchView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }

            mSearchView.clearFocus();
            Log.i(TAG,"onQueryTextSubmit");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean consume = false;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!mActivity.isPadMode()) {
                    //When in phone UI, the behavior of pressing home button is the same
                    //as press back button.
                    mActivity.onBackPressed();
                }
                consume = true;
                break;
            case R.id.about_action:
                showDialog(DialogType.TYPE_ABOUT_DIALOG, null);
                consume = true;
                break;
            case R.id.clear_history_action:
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                        SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
                suggestions.clearHistory();
                consume = true;
                break;
            default:
                break;
        }

        return consume || super.onOptionsItemSelected(item);
    }

    public void showDialog(int type, Object arg) {
        if (mShowDialogListener != null)
            mShowDialogListener.displayDialog(type, arg);
    }
    public void showLocationOrDate(boolean isRemoteFile) {
        if (isRemoteFile) {
            dateContainer.setVisibility(View.VISIBLE);
            locationContainer.setVisibility(View.GONE);
        } else {
            locationContainer.setVisibility(View.VISIBLE);
            dateContainer.setVisibility(View.VISIBLE);
        }
    }
    private void initView() {
        View container = getView();

        mSearchTile = (TextView) container.findViewById(R.id.search_result_title);
        mSearchTile.setText(mTitle);

        sizeContainer = container.findViewById(R.id.sort_size_container);
        //sizeContainer.setOnClickListener(this);
        mSortImage[SORT_SIZE] = (ImageView) sizeContainer.findViewById(R.id.sizeImage);
        mSortImage[SORT_SIZE].setVisibility(View.GONE);

        dateContainer = container.findViewById(R.id.sort_date_container);
        //dateContainer.setOnClickListener(this);
        mSortImage[SORT_DATE] = (ImageView) dateContainer.findViewById(R.id.dateImage);
        mSortImage[SORT_DATE].setVisibility(View.GONE);

       if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            sizeContainer.setVisibility(View.GONE);
            dateContainer.setVisibility(View.GONE);
        } else {
            sizeContainer.setVisibility(View.VISIBLE);
            dateContainer.setVisibility(View.VISIBLE);
        }
        typeContainer = container.findViewById(R.id.sort_type_container);
        //typeContainer.setOnClickListener(this);
        mSortImage[SORT_TYPE] = (ImageView) typeContainer.findViewById(R.id.typeImage);
        mSortImage[SORT_TYPE].setVisibility(View.GONE);

        nameContainer = container.findViewById(R.id.sort_name_container);
        //nameContainer.setOnClickListener(this);
        mSortImage[SORT_NAME] = (ImageView) nameContainer.findViewById(R.id.nameImage);
        mSortImage[SORT_NAME].setVisibility(View.GONE);

        locationContainer = container.findViewById(R.id.sort_location_container);
        //locationContainer.setOnClickListener(this);
        mSortImage[SORT_LOCATION] = (ImageView) locationContainer.findViewById(R.id.locationImage);
        mSortImage[SORT_LOCATION].setVisibility(View.GONE);

        //mSortImage[mSortType / 2].setVisibility(View.VISIBLE);
        //mSortImage[mSortType / 2].getDrawable().setLevel(mSortType % 2);
        if (!mActivity.isPadMode()) {
            View sortRoot = (View) container.findViewById(R.id.sort_container_root);
            sortRoot.setVisibility(View.GONE);
             searchTitleContainer = (LinearLayout)container.findViewById(
                    R.id.search_result_title_container);
            mSearchTile.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            mSearchTile.setSingleLine(true);
            //searchTitleContainer.setBackgroundColor(getResources().getColor(R.color.path_background));
        }

        // +++ cloud storage
        mListView = (ListView) container.findViewById(android.R.id.list);
        mListView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    if (mAdapter != null) {
                        mAdapter.updateAdapter(mAdapter.getFiles(), false);
                    }
                    mIsScrollingList = false;
                } else {
                    mIsScrollingList = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }
        });
        // ---
    }

    public void updateSearchView(final String key) {
        if (mSearchView != null) {
            // FIXME:
            // workaround for v7 searchview setQuery is not work
            mSearchView.post(new Runnable() {
                @Override
                public void run() {
                    MenuItemCompat.expandActionView(getSearchItem());
                    mSearchView.setQuery(key,false);
                    mSearchView.clearFocus();
                }
            });
        }
    }
    public void updateResult(VFile[] files, String key, String path) {
        updateSearchView(key);
        if (path !=null && path.startsWith("smb")) {
            path = getRelativePath(path);
        }
        if (files != null) {
            try {
                String fileNums = getResources().getQuantityString(R.plurals.number_search_items, files.length, files.length);
                mTitle = getString(R.string.search_result_title, "\"" + key + "\"", path) + fileNums;
            } catch (Exception e) {
                mTitle = getString(R.string.search_result_title, "\"" + key + "\"", path, files.length);
            }
          //++yiqiu_huang, display number format with windows style, e.g. 1303 -> 1,303
            int length = files.length;
            if (length > 1000) {
                       String numberStr = new String();
                       while(length > 1000) {
                               DecimalFormat df = new DecimalFormat("000");
                               String tmp = "," + df.format(length%1000);
                               numberStr =  tmp+ numberStr;
                               length = length/1000;
                       }
                       numberStr= (String.valueOf(length)) + numberStr;
                       mTitle = mTitle.replace(String.valueOf(files.length), numberStr);
            }
            //++yiqiu_huang
        } else {
            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            VFile searchFolder = null;

            if (fileListFragment != null) {
                searchFolder = fileListFragment.getIndicatorFile();

            String RootFilePath = searchFolder.getAbsolutePath();
            if (searchFolder.getAbsolutePath().startsWith("smb")) {
                RootFilePath = getRelativePath(RootFilePath);
            }
            try {
                String fileNums = getResources().getQuantityString(R.plurals.number_search_items, 0, 0);
                mTitle = getString(R.string.search_result_title, "\"" + key + "\"", RootFilePath) + fileNums;
            } catch (Exception e) {
                mTitle = getString(R.string.search_result_title, "\"" + key + "\"", RootFilePath, 0);
            }
            }
        }
        mSearchTile.setText(mTitle);
        if (mActivity != null)
            mActivity.setTextViewFont(mSearchTile, FontType.ROBOTO_REGULAR);
        mSearchTile.invalidate();

        String[] favoriteFilePaths = ProviderUtility.FavoriteFiles.getPaths(getActivity().getContentResolver());
        if (favoriteFilePaths != null) {
            Arrays.sort(favoriteFilePaths);
            files = setFileInfo(files, favoriteFilePaths);
        }

        if (mActivity.CATEGORY_FAVORITE_FILE.getPath().equals(path)) {
            ((SearchListAdapter) getListAdapter()).updateAdapter(files, key, true);
        } else {
            ((SearchListAdapter) getListAdapter()).updateAdapter(files, key, false);
        }
    }

    private VFile[] setFileInfo(VFile[] vFiles, String[] favoriteFilePaths) {
        if (vFiles == null || vFiles.length == 0
                || favoriteFilePaths == null || favoriteFilePaths.length == 0) {
            return vFiles;
        }

        ArrayList<VFile> pool = new ArrayList<VFile>();

        for (int i = 0; i < vFiles.length && vFiles[i] != null; i++) {

            String path;

            try {
                path = FileUtility.getCanonicalPathForUser(vFiles[i].getCanonicalPath());
            } catch (IOException e2) {
                path = vFiles[i].getAbsolutePath();
                e2.printStackTrace();
            }

            int index = Arrays.binarySearch(favoriteFilePaths, path);

            if (vFiles[i].isDirectory() && index > -1) {
                vFiles[i].setFavoriteName(ProviderUtility.FavoriteFiles
                        .getFavoriteNameByPath(getActivity().getContentResolver(), path));
            }

            pool.add(vFiles[i]);
        }

        VFile[] r = new VFile[pool.size()];

        for (int i = 0; i < r.length; i++) {
            r[i] = pool.get(i);
        }

        return r;
    }
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
    /*
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
        if(!ItemOperationUtility.getInstance().enableCtaCheck() && !WrapEnvironment.IS_VERIZON) {
            mMenuStrs.add(getResources().getString(R.string.invite_friends));
            mMenuID.add(R.id.action_tell_a_friend);

            int title_instant_update = ZenUiFamily.getZenUiFamilyTitle();
            mMenuStrs.add(getResources().getString(title_instant_update));
            mMenuID.add(R.id.action_instant_update);
        }

        if (!WrapEnvironment.IS_VERIZON){
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
    public void onClick(View view) {
        /*
        if (view.getId() == R.id.menuitem_more){
            showPopup(view);
            return;
        }
        */
        int type = -1;
        switch (view.getId()) {
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
            case R.id.sort_location_container:
                type = SORT_LOCATION;
            default:
                break;
        }

        if (type > -1) {
            if (type != (mSortType / 2)) {
                mSortImage[type].setVisibility(View.VISIBLE);
                mSortImage[mSortType / 2].setVisibility(View.GONE);
                mSortType = type * 2 + 1;
                mSortImage[type].getDrawable().setLevel(1);
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
    }

    // +++ cloud storage
    public boolean isScrolling() {
        return mIsScrollingList;
    }

    public void setRemoteThumbnailList(VFile[] files) {
        RemoteFileUtility.getInstance(getActivity()).setRemoteThumbnailList(files);
    }

    public VFile[] getFileList() {
        VFile[] files = null;
        if (mAdapter != null) {
            files = mAdapter.getFiles();
        } else {
            Log.w(TAG, "mAdapter is null when calling remoteUpdateThumbnail");
        }
        return files;
    }

    public void remoteUpdateThumbnail(VFile[] files) {
        if (mAdapter != null) {
            mAdapter.updateAdapter(files, false);
        } else {
            Log.w(TAG, "mAdapter is null when calling remoteUpdateThumbnail");
        }
    }
    // ---

    public static String getRelativePath(String path) {
        String newPath = path;
        SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
        String rootPath = sambaFileUtility.getRootScanPath();
        if (rootPath.contains("*")) {
            path = path.replaceAll("\\*", "s");
            rootPath = rootPath.replaceAll("\\*","s");
        }
        if (path.startsWith(rootPath)) {

                String[] pathContent = path.split(rootPath);
                if (pathContent.length == 0) {
                    newPath = File.separator + sambaFileUtility.getPcUserName();
                } else if (pathContent.length > 1) {
                    newPath = File.separator + sambaFileUtility.getPcUserName() + File.separator + pathContent[1];
                }
        }
        return newPath;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        if (newConfig.smallestScreenWidthDp>=600) {
            if (getListAdapter() != null && getListAdapter() instanceof SearchListAdapter) {
                if (mAdapter != null) {
                    setListAdapter(mAdapter);
                    //mAdapter.updateAdapter(mAdapter.getFiles(), true, mSortType);
                }
            }
            if (searchTitleContainer!=null) {
                LayoutParams params = (LayoutParams) searchTitleContainer.getLayoutParams();
                params.height = getResources().getDimensionPixelSize(R.dimen.path_view_height);
                searchTitleContainer.setLayoutParams(params);
            }

        }
    }

    public SearchListAdapter getAdapter() {
        return this.mAdapter;
    }

    public void addFavoriteFile(VFile file) {
        ContentResolver cr = getActivity().getContentResolver();
        boolean isFileExists = ProviderUtility.FavoriteFiles.exists(cr, file);
        if (isFileExists) {
            return;
        }

        boolean isNameExists = ProviderUtility.FavoriteFiles.exists(cr, file.getName());
        if (isNameExists) {
            mActivity.displayDialog(DialogType.TYPE_FAVORITE_RENAME_NOTICE_DIALOG, file);
        } else {
            try {
                if (ProviderUtility.FavoriteFiles.insertFile(cr, file.getName(), FileUtility.getCanonicalPathForUser(file.getCanonicalPath())) != null) {
                    GaAccessFile.getInstance()
                            .sendEvents(mActivity, GaAccessFile.CATEGORY_NAME,
                                    GaAccessFile.ACTION_ADD_TO_FAVORITE,
                                    GaAccessFile.LABEL_FROM_MENU, Long.valueOf(1));
                    ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.add_favorite_success));
                    ((FileManagerActivity)getActivity()).reSearch(mActivity.getSearchQueryKey());
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeFavoriteFile(VFile[] files, boolean isNeedCheck) {
        EditPool removeFilePool = new EditPool();
        removeFilePool.setFiles(files, isNeedCheck);
        mActivity.displayDialog(DialogType.TYPE_FAVORITE_ROMOVE_DIALOG, removeFilePool);
        onDeselectAll();
    }

    private void renameFavoriteFile(VFile file) {
        mActivity.displayDialog(DialogType.TYPE_FAVORITE_RENAME_DIALOG, file);
        onDeselectAll();
    }

    public void deleteFileInPopup(VFile[] deleteFile) {
        mDeleteFilePool.setFiles(deleteFile, true);

        deleteFile = mDeleteFilePool.getFiles();
        boolean bNeedPermission = false;
        for (int i = 0; i < deleteFile.length; i++) {
            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(deleteFile[i].getAbsolutePath());
        if (bNeedWriteToAppFolder) {
            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                .newInstance();
            warnDialog.show(mActivity.getFragmentManager(),
                "WarnKKSDPermissionDialogFragment");
                bNeedPermission = true;
                break;
            }
            if (SafOperationUtility.getInstance(getActivity()).isNeedToShowSafDialog(deleteFile[i].getAbsolutePath())) {
                mActivity.callSafChoose(SafOperationUtility.ACTION_DELETE);
                bNeedPermission = true;
                break;
            }
        }
        if (!bNeedPermission) {
              showDialog(DialogType.TYPE_DELETE_DIALOG, mDeleteFilePool);
        }
        onDeselectAll();
    }

    public void deleteComplete(boolean isCancel) {
        if (DEBUG) {
            Log.i(TAG, "deleteComplete");
        }
        onDeselectAll();//   multiselect will be use
        if (!isCancel) {
            ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_DELETEPROGRESS_DIALOG);
            // updateSearchDeleteView(mDeleteFilePool);
        }
        mDeleteFilePool.clear();
//        if (isVisible()) {
//          getActivity().invalidateOptionsMenu();
//        }
    }

    public void saveRenameFile(VFile oldFile,VFile renameFile) {
        if ( oldFile != null ) {
            mOldFile = oldFile;
        }
        if (renameFile != null) {
            mRenameFile = renameFile;
        }
    }

    public void RenamingComplete() {
        if (DEBUG) {
            Log.i(TAG, "RenamingComplete");
        }
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_RENAME_PROGRESS_DIALOG);
    }

    public void onSelectAll() {
        if ((mAdapter == null) || !mAdapter.isItemsSelected()) {
            return;
        }
        mAdapter.setSelectAll();
        getListView().invalidateViews();
        getGridView().invalidateViews();
    }

    public void onDeselectAll() {
        if ((mAdapter == null) || !mAdapter.isItemsSelected()) {
            return;
        }
        mAdapter.clearItemsSelected();
        getListView().invalidateViews();
        getGridView().invalidateViews();
        if (isInEditMode()) {
            finishEditMode();
        }
    }

    private void updateSearchRenameView(VFile vFile, String newName) {
        if (mAdapter != null) {
            VFile[] allResult = mAdapter.getFiles();
            ArrayList<VFile> dynArray = new ArrayList<VFile>();
            int fileType = vFile.getVFieType();
            String keyword = ((FileManagerActivity) getActivity()).getSearchQueryKey().toLowerCase();
            switch(fileType) {
            case VFileType.TYPE_LOCAL_STORAGE:
                for (VFile file : allResult) {
                    if (file.getAbsolutePath().equalsIgnoreCase(vFile.getAbsolutePath())) {
                        int index = file.getAbsolutePath().lastIndexOf(file.getName());
                        if (index != -1) {
                            if (newName.toLowerCase().contains(keyword)) {
                                VFile renamedFile = new LocalVFile(file.getAbsolutePath().substring(0, index) + newName, file.getVFieType());
                                dynArray.add(renamedFile);
                                continue;
                            }
                        }
                    }
                    dynArray.add(file);
                }
            }
            VFile[] resultFiles = new VFile[dynArray.size()];
            dynArray.toArray(resultFiles);
            mAdapter.updateAdapter(resultFiles, true);
        }
    }

    private void updateSearchDeleteView(EditPool pool) {
        if (mAdapter != null) {
            VFile[] AllResult = mAdapter.getFiles();
            VFile[] changeFiles = pool.getFiles();
            VFile[] resultFiles = new VFile[AllResult.length - changeFiles.length];
            ArrayList<String> dynArray = new ArrayList<String>();
            int fileType = pool.getFile().getVFieType();
            switch(fileType) {
                case VFileType.TYPE_CLOUD_STORAGE:
                    for(VFile file : changeFiles) {
                        dynArray.add(((RemoteVFile)file).getFileID());
                    }
                    break;
                case VFileType.TYPE_LOCAL_STORAGE:
                    for(VFile file : changeFiles) {
                        dynArray.add(file.getAbsolutePath());
                    }
                    break;
                case VFileType.TYPE_SAMBA_STORAGE:
                    for(VFile file : changeFiles) {
                        dynArray.add(((SambaVFile)file).getAbsolutePath());
                    }
                    break;
                default:
                    break;
            }

            int num = 0;
            switch(fileType) {
                case VFileType.TYPE_CLOUD_STORAGE:
                    for(VFile sfile : AllResult) {
                        if (!dynArray.contains(((RemoteVFile)sfile).getFileID())) {
                            resultFiles[num] = sfile;
                            num ++;
                        }
                    }
                    break;
                case VFileType.TYPE_LOCAL_STORAGE:
                    for(VFile sfile : AllResult) {
                        if (!dynArray.contains(sfile.getAbsolutePath())) {
                            resultFiles[num] = sfile;
                            num ++;
                        }
                    }
                    break;
                case VFileType.TYPE_SAMBA_STORAGE:
                    for(VFile sfile : AllResult) {
                        if (!dynArray.contains(((SambaVFile)sfile).getAbsolutePath())) {
                            resultFiles[num] = sfile;
                            num ++;
                        }
                    }
                    break;
                default:
                    break;
            }

            mAdapter.updateAdapter(resultFiles, true);
        }
    }

    public void updateSearchAddFileView(VFile newfile) {
        if (newfile == null) {
            return;
        }
        if (mAdapter != null) {
            VFile[] AllResult = mAdapter.getFiles();
            VFile[] newResult = null;
            String keyword = ((FileManagerActivity) getActivity()).getSearchQueryKey().toLowerCase();
            if (newfile.getName().toLowerCase().contains(keyword)) {
                newResult = new VFile[AllResult.length + 1];
                newResult[0] = newfile;
                for(int i = 0; i < AllResult.length; i++) {
                    newResult[i + 1] = AllResult[i];
                }
            } else {
                newResult = AllResult;
            }
            mAdapter.updateAdapter(newResult, true);
        }
    }

    public void updateSearchRenameView() {

        if (mRenameFile == null || mOldFile == null) {
            return;
        }
        if (mAdapter != null) {
            VFile[] AllResult = mAdapter.getFiles();
            VFile[] newResult = null;
            ArrayList<VFile> tempList = new ArrayList<VFile>();
            String keyword = ((FileManagerActivity) getActivity()).getSearchQueryKey().toLowerCase();

            int Filetype = mOldFile.getVFieType();
            String mSFileId = "";
            switch(Filetype) {
                case VFileType.TYPE_CLOUD_STORAGE:
                    mSFileId = ((RemoteVFile)mOldFile).getFileID();
                    for(VFile sFile : AllResult) {
                        if (!((RemoteVFile)sFile).getFileID().equals(mSFileId)) {
                            tempList.add(sFile);
                        }
                    }
                    break;
                case VFileType.TYPE_LOCAL_STORAGE:
                    mSFileId = mOldFile.getAbsolutePath();
                    for(VFile sFile : AllResult) {
                        if (!sFile.getAbsolutePath().equals(mSFileId)) {
                            tempList.add(sFile);
                        }
                    }
                    break;
                case VFileType.TYPE_SAMBA_STORAGE:
                    mSFileId = ((SambaVFile)mOldFile).getAbsolutePath();
                    for(VFile sFile : AllResult) {
                        if (!((SambaVFile)sFile).getAbsolutePath().equals(mSFileId)) {
                            tempList.add(sFile);
                        }
                    }
                    break;
                default:
                    break;
            }

            if (mRenameFile.getName().toLowerCase().contains(keyword)) {
                newResult = new VFile[AllResult.length];
                newResult[0] = mRenameFile;
                for(int i = 0; i < tempList.size(); i++) {
                    newResult[i + 1] = tempList.get(i);
                }
            } else {
                newResult = new VFile[AllResult.length - 1];
                for(int i = 0; i < tempList.size(); i++) {
                    newResult[i] = tempList.get(i);
                }
            }

            mAdapter.updateAdapter(newResult, true);
        }
    }

    @Override
    public void handleAction(int action) {
        switch(action) {
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
            default:
                break;
        }
    }

    public void updateEditMode() {
        final boolean isItemSelected = isItemsSelected();

        if ((isItemSelected == false)) {
            if (mEditMode != null) {
                mEditMode.finish();
            }
            return;
        }

        if (isInEditMode()) {
            updateEditModeView();
        } else {
            EditModeCallback mLastEditModeCallback = new EditModeCallback();
            getActivity().startActionMode(mLastEditModeCallback);
        }
    }

    public void finishEditMode() {
        if (isInEditMode()) {
            mEditMode.finish();
        }
    }

    public boolean isItemsSelected() {
        return mAdapter.isItemsSelected();
    }

    private class EditModeCallback implements ActionMode.Callback {
        private TextView mSelectedCountText;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // FIXME:
            // workaround for black status bar, force reset its color
            ColorfulLinearLayout.changeStatusbarColor(mActivity, R.color.theme_color);
            mEditMode = mode;
            LayoutInflater inflaterView = mActivity.getLayoutInflater();
            View view = inflaterView.inflate(R.layout.editmode_actionbar, null);
            mode.setCustomView(view);
            mSelectedCountText =  (TextView)view.findViewById(R.id.actionbar_text);
            CheckResult result = mAdapter.getSelectedCount();
            String itemSelectedStr = getResources()
                    .getQuantityString(R.plurals.number_selected_items, result.count, result.count);
            mSelectedCountText.setText(itemSelectedStr);
            ((FileManagerActivity)getActivity()).setTextViewFont(mSelectedCountText, FontType.ROBOTO_REGULAR);

            MenuInflater inflater = getActivity().getMenuInflater();

            inflater.inflate(R.menu.edit_mode, menu);
            updateEditMode();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem shareItem = menu.findItem(R.id.share_action);

            CheckResult result = mAdapter.getSelectedCount();

            String itemSelected = getResources()
                .getQuantityString(R.plurals.number_selected_items, result.count, result.count);
            mSelectedCountText.setText(itemSelected);

            MenuItem item = menu.findItem(R.id.select_all_action);
            if (result.count == mAdapter.getCount()) {
                item.setTitle(R.string.deselect_all);
                item.setIcon(R.drawable.asus_ep_ic_unselect_all);
            } else {
                item.setTitle(R.string.select_all);
                item.setIcon(R.drawable.asus_ep_ic_select_all);
            }
            // FIXME:
            // workaround for invisible select_all icon
            item = menu.findItem(R.id.delete_action);
            item.setIcon(R.drawable.asus_ep_ic_delete);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return handleItemClick(item.getItemId());
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onDeselectAll();
            mEditMode = null;
        }
    }

    public boolean isInEditMode() {
        return mEditMode != null;
    }

    private void updateEditModeView() {

        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        VFile indicatorFile = fileListFragment == null ? null : fileListFragment.getIndicatorFile();

        EditPool selectFilePool = new EditPool();
        selectFilePool.setFiles(mAdapter.getFiles(), true);

        CheckResult checkResult = mAdapter.getSelectedCount();

        boolean isOneFile = checkResult.count == 1;
        boolean isOneDir = checkResult.count == 1 && checkResult.hasDir;
        boolean hasDir = checkResult.hasDir;
        boolean isLocalFile = selectFilePool.getFile().getVFieType() == VFileType.TYPE_LOCAL_STORAGE;
        boolean isFavoriteFolder = selectFilePool.getFile().isFavoriteFile();

        boolean isInFavoriteFolder
                = mActivity.CATEGORY_FAVORITE_FILE.equals(indicatorFile);

        Menu menu = mEditMode.getMenu();

        if (menu == null) {
            Log.w(TAG, "do not update edite mode view, menu == null");
            return;
        }

        MenuItem renameItem = menu.findItem(R.id.rename_action);
        if (renameItem != null) {
            renameItem.setVisible(isOneFile && !belongToCategoryMedia());
        }

        MenuItem infoItem = menu.findItem(R.id.info_action);
        if (infoItem != null) {
            infoItem.setVisible(isOneFile);
        }

        MenuItem moveToItem =  menu.findItem(R.id.move_to_action);
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
        }

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
                hiddenZoneItem.setVisible(false);
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

        updateMenuIndicator();

        mEditMode.invalidate();
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
            mEditModePopupWindow.setOnItemClickListener(editModeMenuItemClickListener);
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


    private boolean handleItemClick(int itemId) {
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);

        EditPool editPool = new EditPool();
        editPool.setFiles(mAdapter.getFiles(), true);

        CheckResult result = mAdapter.getSelectedCount();
        int selectcount = result.count;
        if (selectcount == 0) {
            return true;
        }

        switch (itemId) {
        case R.id.select_all_action:
            CheckResult selectItems = mAdapter.getSelectedCount();

            if (selectItems.count == mAdapter.getCount()) {
                onDeselectAll();
                updateEditMode();
            } else {
                onSelectAll();
                updateEditMode();
            }
            break;

        case R.id.delete_action:
            deleteFileInPopup(mAdapter.getFiles());
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_DELETE, null);
            break;

        case R.id.move_to_action:
            if (ItemOperationUtility.isItemContainDrm(editPool.getFiles(),true,false)) {
                if (selectcount == 1) {
                    ToastUtility.show(mActivity, R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                } else {
                    ToastUtility.show(mActivity, R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                }
            } else {
                if (fileListFragment != null) {
                    fileListFragment.cutFileInPopup(editPool.getFiles(), true);
                }
            }
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_MOVE_TO, null);
            break;

        case R.id.copy_to_action:
            if (ItemOperationUtility.isItemContainDrm(editPool.getFiles(),true,false)) {
                if (selectcount == 1) {
                    ToastUtility.show(mActivity, R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                } else {
                    ToastUtility.show(mActivity, R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                }
            } else {
                if (fileListFragment != null) {
                    fileListFragment.copyFileInPopup(editPool.getFiles(), true);
                }
            }
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_COPY_TO, null);
            break;

        case R.id.share_action:
            if (ItemOperationUtility.isItemContainDrm(mAdapter.getFiles(), false,false)) {
                if (selectcount == 1) {
                    ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                } else {
                    ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                }
            } else {
                if (mAdapter.getCount() > 0 && ((VFile)mAdapter.getItem(0)).getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    // share cloud storage file, we temporarily save it to cache
                    editPool.setFiles(mAdapter.getFiles(), true);
                    VFile[] srcVFile = editPool.getFiles();
                    String account = ((RemoteVFile)srcVFile[0]).getStorageName();
                    VFile dstVFile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                    int type = ((RemoteVFile)srcVFile[0]).getMsgObjType();

                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageCopyMessage(account, srcVFile, dstVFile, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, RemoteFileUtility.REMOTE_SHARE_ACTION, false);
                    fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
                } else if (mAdapter.getCount() > 0 && ((VFile)mAdapter.getItem(0)).getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                    editPool.setFiles(mAdapter.getFiles(), true);
                    VFile[] srcVFile = editPool.getFiles();
                    VFile dstVFile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                    SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_SHARE, srcVFile, dstVFile.getAbsolutePath(), false,-1,null);
                    fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
                } else {
                    editPool.setFiles(mAdapter.getFiles(), true);
                    FileUtility.shareFile(getActivity(), editPool.getFiles(), false);
                    onDeselectAll();
               }
            }
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_SHARE, null);
            GaAccessFile.getInstance().sendEvents(mActivity,
                    GaAccessFile.ACTION_SHARE, ((VFile)mAdapter.getItem(0)).getVFieType(), -1, editPool.getSize());
            break;

        case R.id.add_favorite_action:
            editPool.setFiles(mAdapter.getFiles(), true);
            addFavoriteFile(editPool.getFile());
            updateEditMode();
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_ADD_TO_FAVORITE, null);
            break;

        case R.id.remove_favorite_action:
            editPool.setFiles(mAdapter.getFiles(), true);
            removeFavoriteFile(editPool.getFiles(), false);
            updateEditMode();
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_REMOVE_FROM_FAVORITE, null);
            break;

        case R.id.create_shortcut_action:
            EditPool addShortcutPool = new EditPool();
            addShortcutPool.setFiles(mAdapter.getFiles(), true);
            CreateShortcutUtil.createFolderShortcut(getActivity().getApplicationContext(),
                    addShortcutPool.getFile().getPath(), addShortcutPool.getFile().getName());
            onDeselectAll();
            SharedPreferences mSharePrefence = mActivity.getSharedPreferences("MyPrefsFile", 0);
            mSharePrefence.edit().putBoolean("newfeature_createshortcut_editmode", false).commit();
            updateEditMode();
            GaShortcut.getInstance().sendEvents(mActivity,GaShortcut.CATEGORY_NAME,
                    GaShortcut.ACTION_CREATE_FROM_NON_HOMEPAGE, null, null);
            GaSearchFile.getInstance().sendEvents(mActivity,GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_CREATE_SHORTCUT, null);
            break;

        case R.id.compress_action:
            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mAdapter.getFiles()[0].getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                warnDialog.show(mActivity.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
            } else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(mAdapter.getFiles()[0].getAbsolutePath())) {
                if (mActivity instanceof FileManagerActivity) {
                    ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_ZIP);
                }
            } else {
                ZipDialogFragment.showZipDialog(SearchResultFragment.this, mAdapter.getFiles(), true);
            }
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_COMPRESS, null);
            break;

        case R.id.rename_action:
            editPool.setFiles(mAdapter.getFiles(), true);
            VFile file = editPool.getFile();

            if (fileListFragment != null && fileListFragment.getIndicatorFile().equals(mActivity.CATEGORY_FAVORITE_FILE)) {
                    renameFavoriteFile(file);
            } else {
                if (ItemOperationUtility.isItemContainDrm(new VFile[]{file}, true, false)) {
                    if (file.isFile()) {
                        ToastUtility.show(getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    } else {
                        ToastUtility.show(getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    }
                } else {
                    bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(file.getAbsolutePath());
                    if (bNeedWriteToAppFolder) {
                        WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                .newInstance();
                        warnDialog.show(mActivity.getFragmentManager(),
                                "WarnKKSDPermissionDialogFragment");
                    } else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(file.getAbsolutePath())) {
                        if (mActivity instanceof FileManagerActivity) {
                            ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_RENAME);
                        }
                    } else {
                        showDialog(DialogType.TYPE_RENAME_DIALOG, file);
                    }
                }
            }
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_RENAME, null);
            break;

        case R.id.info_action:
            VFile vFile = editPool.getFile();
            showDialog(DialogType.TYPE_INFO_DIALOG, vFile);
            onDeselectAll();
            GaSearchFile.getInstance().sendEvents(mActivity, GaSearchFile.CATEGORY_NAME,
                    GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_INFORMATION, null);
            break;

        default:
            return false;
        }

        return true;
    }


    OnItemClickListener editModeMenuItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mEditModePopupWindow.dismiss();
            handleItemClick(mEditModeMenuID.get(position));
    }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        mPopupWindow.dismiss();
        int menu_id = (Integer)mMenuID.get(position).intValue();
        mActivity.onPopupMenuItemClick(menu_id);
        mActivity.invalidateOptionsMenu();
    }

    private boolean belongToCategoryMedia() {
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        return fileListFragment == null ? false : fileListFragment.belongToCategoryMedia();
    }

    private boolean isInCategory() {
        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        return fileListFragment == null ? false : fileListFragment.isInCategory();
    }
}
