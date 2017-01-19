package com.asus.filemanager.dialog;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseActivity;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.loader.ScanFileLoader;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.SortUtility.SortType;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FolderSelection extends BaseActivity implements LoaderManager.LoaderCallbacks<VFile[]> {
    private static final boolean DEBUG = true;
    private static final String TAG = "FolderSelection";
    private static final int UPDATE_UI = 0;
    private static final int FINISHED = 1;
    private static final String[] VISIABLEFILE = { "Removable", "sdcard"};
    private static final String DEFAULT_INDICATOR_PATH = (new LocalVFile(WrapEnvironment.getEpadInternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))).getAbsolutePath();
    private static int sLastCheckedId;
    private static boolean sRefreshList = false;
    private static boolean sIsLoading = false;
    private static List<LocalVFile> sFileList;
    private static List<String> sFilesName;
    private static FileSelectionAdapter sArrayAdapter;
    private static String sDefaultPath;
    private static final String KEY_SCAN_PATH = "scan_path";
    private static final String KEY_SCAN_TYPE = "scan_type";
    private static final String KEY_SORT_TYPE = "sort_type";
    private static final String KEY_VFILE_TYPE = "vfile_type";
    private static final String KEY_HIDDEN_TYPE = "hidden_type";
    private static final String KEY_CHECK_POOL = "check_pool";
    private static final int SCAN_FILE_LOADER = 0;
    private boolean mIsSelectedCancel = false;
    private boolean mIsNeedInputDialog = false;
    private boolean mIsBackKeyEvent = true;
    private boolean mIsOnlyMPSupport = false; // only support the storages which MediaProvider can scan
    private InputAlertDialogFragment mInputDialogFragment = null;
    private SelectAlertDialogFragment mSelectDialogFragment = null;
    private String mSaveDisplayPath = "";
    private String mSelectedFileName = null;
    private File mCurrentDirPath = null;
    private MyFileObserver mObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // +++ Willie
        // Set theme to Asus theme style for PadFone
        // The Asus theme only be applied if the resource id has been retrieved.
        int themeAsusDialogId = ThemeUtility.sThemeAsusDialogId;
        if (themeAsusDialogId != 0) {
            setTheme(themeAsusDialogId);
            Theme theme = getTheme();
            theme.applyStyle(R.style.FolderSelectionStyle, true);
        }
        // ---

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.folder_selector_main);

        // avoid font size changed
        //final Configuration conf = getResources().getConfiguration();
        //conf.fontScale = 1;
        //getResources().updateConfiguration(conf, getResources().getDisplayMetrics());

        // get intent data
        String defaultPath = this.getIntent().getStringExtra("DEFAULT_PATH");
        if (defaultPath != null) {
            sDefaultPath = defaultPath;
        } else {
            sDefaultPath = DEFAULT_INDICATOR_PATH;
        }
        mIsNeedInputDialog = this.getIntent().getBooleanExtra("NEED_INPUT_DIALOG", true);
        mIsOnlyMPSupport = this.getIntent().getBooleanExtra("ONLY_MP_SUPPORT", false);

        // initialize data
        sArrayAdapter = new FileSelectionAdapter(this);
        sFileList = new ArrayList<LocalVFile>();
        sFilesName = new ArrayList<String>();
        mCurrentDirPath = null;
        sLastCheckedId = -1;
        showDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSelectDialogFragment != null && mInputDialogFragment != null) {
            if (DEBUG) Log.v(TAG, "Select Dialog is visible and stop Input Dialog");
            mInputDialogFragment.onStop();
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mReviver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReviver);
    }

    void showDialog() {
        // Create the fragment and show it as a dialog.
        if (mIsNeedInputDialog) {
            if (DEBUG) Log.v(TAG, "show Input Dialog");
            mInputDialogFragment = InputAlertDialogFragment.newInstance(getResources().getString(R.string.select_folder));
            mInputDialogFragment.show(getFragmentManager(), "input_dialog");
        } else {
            if (DEBUG) Log.v(TAG, "show Select Dialog");
            sRefreshList = true;
            if (!(isFilePath(sDefaultPath))) {
                if (DEBUG) Log.v(TAG, "get the invalid directory path: "+sDefaultPath);
                sDefaultPath = DEFAULT_INDICATOR_PATH;
            }
            parseInputPath(sDefaultPath);
            saveToSelectPath(sDefaultPath);
            initSelectDialog();
        }
    }

    public static class SelectAlertDialogFragment extends DialogFragment {
        TextView selectFilePath;
        ListView listView;
        AlertDialog alertDialog;
        ProgressBar progressBar;
        public static SelectAlertDialogFragment newInstance(String title) {
            SelectAlertDialogFragment frag = new SelectAlertDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View textEntryView = factory.inflate(R.layout.folder_selector_select_dialog, null);
            progressBar = (ProgressBar) textEntryView.findViewById(R.id.folder_selection_progress);
            selectFilePath = (TextView) textEntryView.findViewById(R.id.select_file_path);
            selectFilePath.setText(((FolderSelection)getActivity()).getSelectPath());
            ImageButton backBtn = (ImageButton) textEntryView.findViewById(R.id.folder_select_backbtn);
            backBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // initialize data, and if the current path is root, we don't do anything
                    if (!selectFilePath.getText().equals("/")) {
                        sLastCheckedId = -1;
                        sRefreshList = true;
                        ((FolderSelection)getActivity()).setDisplayPath(true, false, null);
                        scrollToTop();
                    } else {
                        Log.v(TAG, "current path is root, we don't do anything when clicking back button");
                    }

                }
            });
            listView = (ListView) textEntryView.findViewById(R.id.folder_select_list);
            listView.setAdapter(sArrayAdapter);
            listView.setItemsCanFocus(true);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            if (!sIsLoading) {
                if (DEBUG) Log.w(TAG, "initialize view and correct view state");
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.clearAnimation();
                listView.setVisibility(View.VISIBLE);
            }
            String ok = ((FolderSelection)getActivity()).getResources().getString(android.R.string.ok);
            String cancel = ((FolderSelection)getActivity()).getResources().getString(R.string.cancel);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            int themeAsusDialogAlertId = ThemeUtility.sThemeAsusDialogAlertId;

            alertDialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setIcon(R.drawable.asus_ep_folder_selection_dialog_title)
                    .setTitle(title)
                    .setPositiveButton(ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((FolderSelection) getActivity()).doSelectCompletedClick();
                                }
                            })
                    .setNegativeButton(cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((FolderSelection) getActivity()).doSelectCancelClick();
                                }
                            }).create();

            alertDialog.setView(textEntryView, spacing_left, spacing_top, spacing_right, spacing_bottom);

            return alertDialog;
        }

        public void isloading(boolean isLoading) {
            if (isLoading) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    // use default animation, duration is 3500ms
                    ObjectAnimator anim = ObjectAnimator.ofFloat(progressBar, "alpha", 0.0f, 1.0f);
                    anim.setInterpolator(new AccelerateInterpolator());
                    anim.setDuration(3500);
                    anim.start();
                } else {
                    if (DEBUG) Log.w(TAG, "progressBar is null when calling isloading");
                }
                if (listView != null) {
                    listView.setVisibility(View.INVISIBLE);
                } else {
                    if (DEBUG) Log.w(TAG, "listView is null when calling isloading");
                }
            } else {
                if (progressBar != null) {
                    progressBar.setVisibility(View.INVISIBLE);
                    progressBar.clearAnimation();
                } else {
                    if (DEBUG) Log.w(TAG, "progressBar is null when calling isloading");
                }
                if (listView != null) {
                    listView.setVisibility(View.VISIBLE);
                } else {
                    if (DEBUG) Log.w(TAG, "listView is null when calling isloading");
                }
            }

        }

        public void scrollToTop() {
            listView.setSelectionAfterHeaderView();
        }

        public void setSelectPath(String string) {
            selectFilePath.setText(string);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (((FolderSelection) getActivity()).isSelectedCancel() || ((FolderSelection) getActivity()).isNeedInputDialog()) {
                if (DEBUG) Log.v(TAG, "Select Dialog called onDetach and restart input dialog");
                ((FolderSelection) getActivity()).startInputDialog();
            } else if (((FolderSelection) getActivity()).isBackKeyEvent()) {
                if (DEBUG) Log.v(TAG, "Select Dialog called onDetach and finish this activity");
                ((FolderSelection) getActivity()).finishActivity();
            }
        }

    }

    public static class InputAlertDialogFragment extends DialogFragment {
        EditText editText;
        View v;
        public static InputAlertDialogFragment newInstance(String title) {
            InputAlertDialogFragment frag = new InputAlertDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View textEntryView = factory.inflate(R.layout.folder_selector_input_dialog, null);

            editText = (EditText) textEntryView.findViewById(R.id.input_folder_path);
            editText.setText(sDefaultPath);
            editText.requestFocus(); // show soft keyboard

            ImageButton imgBtn = (ImageButton) textEntryView.findViewById(R.id.select_button);
            imgBtn.setOnClickListener(new ImageButton.OnClickListener() {
                public void onClick(View v) {
                    String path = "";
                    editText.clearFocus();
                    sRefreshList = true;
                    InputMethodManager imm = (InputMethodManager)((FolderSelection)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    // if user input wrong, we use default path to select folder...
                    if ( !((FolderSelection)getActivity()).isFilePath(getDisplayText()) ) {
                        path = DEFAULT_INDICATOR_PATH;
                    } else {
                        path = getDisplayText();
                    }
                    ((FolderSelection)getActivity()).parseInputPath(path);
                    ((FolderSelection)getActivity()).saveToSelectPath(path);
                    ((FolderSelection)getActivity()).initSelectDialog();
                    onStop(); //Input Dialog should be invisible
                }
            });

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);


            AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                        .setIcon(R.drawable.asus_ep_folder_selection_dialog_title)
                        .setTitle(title)
                        .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((FolderSelection)getActivity()).doInputPositiveClick();
                                }
                            }
                        )
                        .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((FolderSelection)getActivity()).doInputNegativeClick();
                                }
                            }
                        ).create();

            dialog.setView(textEntryView, spacing_left, spacing_top, spacing_right, spacing_bottom);

            return dialog;
        }

        public void setDisplayText(String string) {
            editText.setText(string);
        }

        public String getDisplayText() {
            return editText.getText().toString();
        }

        public void requestFocusEditText() {
            editText.requestFocus();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (DEBUG) Log.v(TAG, "Input Dialog called onDetach");
            if (((FolderSelection)getActivity()).isBackKeyEvent()) {
                if (DEBUG) Log.v(TAG, "finish activity directly...");
                ((FolderSelection)getActivity()).finishActivity();
            }
        }
    }

    public boolean isFilePath(String path) {
        File file = new File(path);
        int separatorIndex = -1;
        // consider path is "/xxxx/xxx//xxx" case
        for (int i=0 ; i<path.length() ; i++) {
            if (path.charAt(i) == File.separatorChar) {
                if (separatorIndex == -1 || separatorIndex+1 < i) {
                    separatorIndex = i;
                } else {
                    if (separatorIndex == i-1) {
                        return false;
                    }
                }
            }
        }
        if (file.exists()) {
            if (DEBUG) Log.v(TAG, "user input file path exists: " + path);
            return true;
        }
        return false;
    }

    public void initSelectDialog() {
        if (DEBUG) Log.v(TAG, "initialize Select Dialog");
        mIsSelectedCancel = false;
        mSelectDialogFragment = SelectAlertDialogFragment.newInstance(getResources().getString(R.string.select_folder));
        mSelectDialogFragment.show(getFragmentManager(), "select_dialog");
    }

    public void doInputPositiveClick() {
        if (DEBUG) Log.v(TAG, "finish Input Dialog and return path");
        mIsBackKeyEvent = false;
        File file = new File(mInputDialogFragment.getDisplayText());
        Intent intent = new Intent();
        if (mInputDialogFragment.getDisplayText().equals("") || !file.exists()) {
            if (DEBUG) Log.v(TAG, "Return null since path doesn't exist or input nothing");
            Toast.makeText(getApplicationContext(), R.string.invalid_directory_path, Toast.LENGTH_SHORT).show();
            String path = null;
            intent.putExtra("FILE_PATH", path);
        } else if (mIsOnlyMPSupport) {
            // only support the current Media Provider can scan storages
            if (DEBUG) Log.v(TAG, "Only return MediaProvider can scan storages' path");
            try {
                if (FileUtility.isPathInScanDirectories(file)) {
                    intent.putExtra("FILE_PATH", mInputDialogFragment.getDisplayText());
                } else {
                    if (DEBUG) Log.v(TAG, "Return null since the current path: "+mInputDialogFragment.getDisplayText());
                    Toast.makeText(getApplicationContext(), R.string.invalid_directory_path, Toast.LENGTH_SHORT).show();
                    String path = null;
                    intent.putExtra("FILE_PATH", path);
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (DEBUG) Log.v(TAG, "Return the selected path: "+ mInputDialogFragment.getDisplayText());
            intent.putExtra("FILE_PATH", mInputDialogFragment.getDisplayText());
        }
        if (file != null && file.exists()) {
        	 if (DEBUG) Log.v(TAG, "file selected path: "+file.getAbsolutePath());
        	LocalVFile temFile = new LocalVFile(file);
			FileUtility.saveCurrentScanFileInfo(this, temFile, FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME);

		}
        this.setResult(Activity.RESULT_OK, intent);
        finishActivity();
    }

    public void doInputNegativeClick() {
        if (DEBUG) Log.v(TAG, "cancel Input Dialog");
        mIsBackKeyEvent = false;
        Intent intent = new Intent();
        // user cancel action, so we return null
        String path = null;
        intent.putExtra("FILE_PATH", path);
        this.setResult(Activity.RESULT_OK, intent);
        finishActivity();
    }

    public void doSelectCompletedClick() {
        mIsBackKeyEvent = false;
        mSelectedFileName = null;
        String path = "";

        if (sLastCheckedId != -1 && mSaveDisplayPath.equals("/")) {
            if (sArrayAdapter.getCount() == 0) {
                path = mSaveDisplayPath;
            } else {
                path = mSaveDisplayPath + sArrayAdapter.getItem(sLastCheckedId);
            }
            sLastCheckedId = -1;
        } else if (sLastCheckedId != -1) {
            if (sArrayAdapter.getCount() == 0) {
                path = mSaveDisplayPath;
            } else {
                path = mSaveDisplayPath + "/" + sArrayAdapter.getItem(sLastCheckedId);
            }
            sLastCheckedId = -1;
        } else if (mSaveDisplayPath.equals("")) {
            path = mSaveDisplayPath + "/";
        } else {
            path = mSaveDisplayPath;
        }

        if (DEBUG) Log.v(TAG, "Return the selected path: " + path);
        sRefreshList = false;
        mIsSelectedCancel = false;
        if (mIsNeedInputDialog) {
            // we don't need Input Dialog any more...
            mInputDialogFragment.onDestroyView();
        }
        mSelectDialogFragment.onDetach();
        Intent intent = new Intent();
        intent.putExtra("FILE_PATH", path);
        if (path != null && path.length()>0) {
        	File vFile = new File(path);
        	if (vFile != null && vFile.exists()) {
        		Log.d(TAG, "save saveCurrentScanFileInfo path"+vFile.getAbsolutePath());
        		LocalVFile tempFile = new LocalVFile(vFile);
        		FileUtility.saveCurrentScanFileInfo(this, tempFile, FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME);
			}
		}
        this.setResult(Activity.RESULT_OK, intent);
        finishActivity();
    }

    public void doSelectCancelClick() {
        if (DEBUG) Log.v(TAG, "cancel Select Dialog");
        mSelectedFileName = null;
        if (mInputDialogFragment != null) {
            mIsSelectedCancel = true;
            mSelectDialogFragment.onDetach();
        } else {
            mIsBackKeyEvent = false;
            mIsSelectedCancel = false;
            Intent intent = new Intent();
            // user cancel action, so we return null
            String path = null;
            intent.putExtra("FILE_PATH", path);
            this.setResult(Activity.RESULT_OK, intent);
            finishActivity();
        }
    }

    public void updateFileList() {
        sArrayAdapter.notifyDataSetInvalidated();
        sArrayAdapter.notifyDataSetChanged();
    }

    private boolean checkStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        if (DEBUG) Log.w(TAG, "get wrong external storage state: "+state);
        return false;
    }

    private boolean updateFilePath(boolean back, String path) {
        boolean result = false;
        boolean isRoot = false;
        File[] subDir;
        FileFilter dirFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        if (back) {
            if (mCurrentDirPath.getParent() != null) {
                subDir = mCurrentDirPath.getParentFile().listFiles(dirFilter);
                mCurrentDirPath = mCurrentDirPath.getParentFile();
            } else {
                subDir = null;
            }
        } else {
            mCurrentDirPath = null;
            mCurrentDirPath = new File(path);
            subDir = mCurrentDirPath.listFiles(dirFilter);
        }
        if (subDir == null) {

            if (sArrayAdapter != null) {
                updateFileList();
            }
            registerObserver(mCurrentDirPath.toString());
            if (DEBUG) Log.v(TAG, "update current file path is: "+mCurrentDirPath);
            return result;
        } else {
            result = true;
            sFileList.clear();

            if (mCurrentDirPath.getParent() == null) {
                isRoot = true;
            }

            if (isRoot) {
                LocalVFile file;
                for (int i=0 ; i<VISIABLEFILE.length ; i++) {
                    file = new LocalVFile(VISIABLEFILE[i]);
                    sFileList.add(file);
                }
            } else if (result) {
                startSortFolders(mCurrentDirPath.getAbsolutePath());
            }
        }
        if (sArrayAdapter != null) {
            updateFileList();
        }
        registerObserver(mCurrentDirPath.toString());
        if (DEBUG) Log.v(TAG, "update current file path is: "+mCurrentDirPath);
        return result;
    }

    private void setDisplayPath(boolean back, boolean update, String fileName) {
        if (back) {
            if (!sFilesName.isEmpty()) {
                int size = sFilesName.size();
                sFilesName.remove(size-1);
                mSaveDisplayPath = "";
                for (int i=0 ; i<sFilesName.size() ; i++) {
                    mSaveDisplayPath += "/"+sFilesName.get(i);
                    mSelectDialogFragment.setSelectPath(mSaveDisplayPath);
                }
                if (sFilesName.size() == 0) {
                    mSelectDialogFragment.setSelectPath("/");
                }
                updateFilePath(true, null);

            }
            if (DEBUG) Log.v(TAG, "set Select Dialog's textview is (back case): " + mSaveDisplayPath);
        } else if (update) {
            if (fileName !=null && fileName.length() > 0) {
                sFilesName.add(fileName);
                if (mSaveDisplayPath.equals(File.separator)) {
                    mSaveDisplayPath += fileName;
                } else {
                    mSaveDisplayPath += "/" + fileName;
                }

            }
            mSelectDialogFragment.setSelectPath(mSaveDisplayPath);
            if (DEBUG) Log.v(TAG, "set Select Dialog's textview is (update folder list): "+mSaveDisplayPath);
        }
    }

    private String getSelectPath() {
        return mSaveDisplayPath;
    }

    private void saveToSelectPath(String path) {
        mSaveDisplayPath = path;
        if (checkStorageState()) {
            updateFilePath(false, mSaveDisplayPath);
        }
    }

    // File Observer +++
    private class MyFileObserver extends FileObserver {

        public static final int mask = FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_TO | FileObserver.MOVED_FROM;
        private String mWatchPath;

        public MyFileObserver(String path) {
            super(path,mask);
            mWatchPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            int mask = event & 0x0000ffff;

            boolean re_Scan = false;
            boolean re_Set = false;
            switch(mask) {
                case FileObserver.MODIFY:
                    re_Set = true;
                    break;
                case FileObserver.MOVED_FROM:
                    re_Set = true;
                    re_Scan = true;
                    break;
                case FileObserver.MOVED_TO:
                    re_Scan = true;
                    break;
                case FileObserver.CREATE:
                    re_Scan = true;
                    break;
                case FileObserver.DELETE:
                    re_Set = true;
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
            if (DEBUG) Log.v(TAG, "MyFileObserver set rescan: " + re_Scan + " and uncheck: " + re_Set);
            if (re_Set && !(new File(mWatchPath + "/" + mSelectedFileName).exists())) {
                mSelectedFileName = null;
                sRefreshList = true;
            }
            if (re_Scan) {
                final Message msg = mHandler.obtainMessage(UPDATE_UI, mWatchPath);
                mHandler.sendMessage(msg);
            }
        }
    }

    public void registerObserver(String path) {
        if(DEBUG)Log.d(TAG,"registerObserver : " + path);
        if(mObserver != null) {
            if(mObserver.mWatchPath.equals(path))
                return;
            mObserver.stopWatching();
            mObserver = new MyFileObserver(path);
            mObserver.startWatching();
        } else {
            mObserver = new MyFileObserver(path);
            mObserver.startWatching();
        }
    }

    public void unregisterObserver (){
        if(DEBUG)Log.d(TAG,"unregisterObserver");
        if(mObserver!=null){
            mObserver.stopWatching();
            mObserver = null;
        }
    }
    // File Observer ---

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UPDATE_UI:
                if (DEBUG) Log.v(TAG, "update UI because files change in the current path");
                boolean update = updateFilePath(false, (String)msg.obj); //refresh list and return result
                // only special case will enter
                if ( detectPathHasSubDir(mCurrentDirPath.toString()) && mCurrentDirPath.toString().equals(mSaveDisplayPath+"/"+getFinalFileName())) {
                    setDisplayPath(false, update, getFinalFileName());
                }
                break;
            case FINISHED:
                finish();
                break;
            }
        }
    };

    private void parseInputPath(String path) {
        int start, end, temp = 0, count = 0;
        sFilesName.clear();
        for (int i=0 ; i<path.length() ; i++) {
            if (path.charAt(i) == File.separatorChar) {
                count++;
            }
        }

        for (int i=0 ; i<count ; i++) {
            start = -1;
            end = 0;
            for (int j=temp ; j<path.length() ; j++) {
                if (path.charAt(j) == File.separatorChar) {
                    if (end > start) {
                        start = j;
                    } else {
                        end = j;
                        temp = end;
                        sFilesName.add(path.substring(start+1, end));
                        break;
                    }
                } else if (j == path.length()-1) {
                    end =  path.length();
                    sFilesName.add(path.substring(start+1, end));
                    break;
                }
            }
        }
    }

    private String getFinalFileName() {
        String FileName = null;
        int index = -1;
        for (int i=0 ; i<mCurrentDirPath.toString().length() ; i++) {
            if (mCurrentDirPath.toString().charAt(i) == File.separatorChar) {
                index = i;
            }
        }
        if (index != -1) {
            FileName = mCurrentDirPath.toString().substring(index+1, mCurrentDirPath.toString().length());
        }
        return FileName;
    }

    private boolean detectPathHasSubDir(String path) {
        FileFilter dirFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        if (path != null) {
            File file = new File(path);
            return (file.exists()) ? file.listFiles(dirFilter).length > 0 : false;
        }
        return false;
    }

    public List<LocalVFile> getFilesList() {
        return sFileList;
    }

    private void startInputDialog() {
        if (mInputDialogFragment != null && mInputDialogFragment.isAdded()) {
            mInputDialogFragment.onStart();
        }
    }

    private void unCheckList() {
        for (int i=0 ; i<sFileList.size() ; i++) {
            if (sFileList.get(i).getChecked()) {
                sFileList.get(i).setChecked(false);
                break;
            }
        }
    }

    private boolean isSelectedCancel() {
        return mIsSelectedCancel;
    }

    private boolean isBackKeyEvent() {
        return mIsBackKeyEvent;
    }

    private boolean isNeedInputDialog() {
        return mIsNeedInputDialog;
    }

    private void finishActivity() {
        final Message msg = mHandler.obtainMessage(FINISHED);
        mHandler.sendMessage(msg);
    }

    public class FileSelectionAdapter extends BaseAdapter implements OnClickListener {
        private LayoutInflater mInflater;
        private ViewHolder holder;
        private FileSelectionAdapter(Activity context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return getFilesList().size();
        }

        @Override
        public Object getItem(int position) {
            return (sFileList == null) ? null : sFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.folder_selector_listview, null);

                holder.img = (ImageView) convertView.findViewById(R.id.select_list_img);
                holder.fileName = (TextView) convertView.findViewById(R.id.select_list_file_name);
                holder.radioBtn = (RadioButton) convertView.findViewById(R.id.folder_list_select);

                holder.container = (View) convertView.findViewById(R.id.folder_selector_container);
                holder.container.setOnClickListener(this);
                holder.radioBtn.setOnClickListener(this);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (holder.container != null) {
                holder.container.setTag(position);
            }
            if (holder.fileName != null) {
                holder.fileName.setTag(position);
                holder.fileName.setText(getFilesList().get(position).toString());
            }

            if (holder.radioBtn != null) {
                holder.radioBtn.setTag(position);
            }

            if (sFileList.get(position).getChecked() == true) {
                holder.radioBtn.setChecked(true);
                mSelectedFileName = getFilesList().get(position).toString();
            } else if (!sRefreshList && mSelectedFileName != null && mSelectedFileName.equals(getFilesList().get(position).toString())) {
                holder.radioBtn.setChecked(true);
                sLastCheckedId = position;
            } else if (sRefreshList && position == 0) {
                // don't select the default item
                mSelectedFileName = null;
                sLastCheckedId = -1;
                sRefreshList = false;
            } else {
                holder.radioBtn.setChecked(false);
            }

            return convertView;
        }

        public final class ViewHolder {
            ImageView  img;
            TextView fileName;
            RadioButton radioBtn;
            View container;
        }

        public void onClick(View v) {
            int checkPosition;
            checkPosition = (Integer) v.getTag();

            switch (v.getId()) {
                case R.id.folder_selector_container:
                    boolean update;
                    if (DEBUG) Log.v(TAG, "folder selector containerView get Postion: "+checkPosition);
                    sLastCheckedId = -1;
                    sRefreshList = true;
                    mSelectedFileName = null;
                    String dirName = sFileList.get(checkPosition).toString();
                    if (getSelectPath().equals(File.separator)) {
                        update = updateFilePath(false, getSelectPath() + dirName);
                    } else {
                        update = updateFilePath(false, getSelectPath() + "/" + sFileList.get(checkPosition));
                    }
                    setDisplayPath(false, update, dirName);
                    updateFileList();
                    mSelectDialogFragment.scrollToTop();
                    break;
                case R.id.folder_list_select:
                    if (DEBUG) Log.v(TAG, "folder selector radiobutton get Postion: "+checkPosition);
                    unCheckList();
                    sRefreshList = false;
                    mSelectedFileName = null;
                    sLastCheckedId = checkPosition;
                    sFileList.get(checkPosition).setChecked(true);
                    updateFileList();
                    break;
                default:
                    break;
            }
        }
    }

    public BroadcastReceiver mReviver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            	Object storageVolume = (Object) intent.getParcelableExtra(reflectionApis.EXTRA_STORAGE_VOLUME);
            	String path = reflectionApis.volume_getPath(storageVolume);
                if (mCurrentDirPath != null && mCurrentDirPath.getAbsolutePath().startsWith(path)) {
                    if (isFilePath(sDefaultPath)) {
                        sRefreshList = true;
                        boolean update = updateFilePath(false, sDefaultPath);
                        parseInputPath(sDefaultPath);
                        saveToSelectPath(sDefaultPath);
                        setDisplayPath(false, update, null);
                    } else {
                        if (DEBUG) Log.v(TAG, "Media unmount and default path doesn't exist");
                        sRefreshList = true;
                        boolean update = updateFilePath(false, "/");
                        parseInputPath("/");
                        saveToSelectPath("/");
                        setDisplayPath(false, update, null);
                    }

                }

            }
        }
    };

    @Override
    public Loader<VFile[]> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.d(TAG, "onCreateLoader");
        String scanPath = args.getString(KEY_SCAN_PATH);
        int scanType = args.getInt(KEY_SCAN_TYPE);
        int sortType = args.getInt(KEY_SORT_TYPE);
        int vfileType = args.getInt(KEY_VFILE_TYPE);
        boolean showHidden = args.getBoolean(KEY_HIDDEN_TYPE);
        EditPool checkPool = (EditPool) args.getSerializable(KEY_CHECK_POOL);

        return new ScanFileLoader(this, scanPath, scanType, sortType, vfileType, showHidden, checkPool, null);

    }

    @Override
    public void onLoadFinished(Loader<VFile[]> loader, VFile[] data) {
        if (DEBUG) Log.d(TAG, "onLoadFinished");
        sIsLoading = false;
        if ( data != null) {
            LocalVFile file;
            for (int i=0 ; i<data.length ; i++) {
                file = new LocalVFile(data[i].getName());
                if (mIsOnlyMPSupport && mCurrentDirPath.getName().equals(VISIABLEFILE[0])) {
                    // only support the current Media Provider can scan storages
                    if (DEBUG) Log.v(TAG, "Only add MediaProvider can scan storages");
                    try {
                        if (FileUtility.isPathInScanDirectories(new File(mCurrentDirPath.getAbsolutePath() + "/" + file.getName()))) {
                            sFileList.add(file);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    sFileList.add(file);
                }
            }
        }
        if (sArrayAdapter != null) {
            updateFileList();
        }
//        registerObserver(mCurrentDirPath.toString());
        if (mSelectDialogFragment != null) {
            mSelectDialogFragment.isloading(false);
        } else {
            if (DEBUG) Log.w(TAG, "mSelectDialogFragment is null when calling onLoadFinished");
        }

    }

    @Override
    public void onLoaderReset(Loader<VFile[]> loader) {
        if (DEBUG) Log.d(TAG, "onLoaderReset");
    }

    private void startSortFolders(String path) {
        if (DEBUG) Log.d(TAG, "startSortFolders");
        sIsLoading = true;
        Bundle args = new Bundle();
        args.putString(KEY_SCAN_PATH, path);
        args.putInt(KEY_SCAN_TYPE, ScanType.SCAN_FOLDER_ONLY);
        args.putInt(KEY_SORT_TYPE, SortType.SORT_NAME_DOWN);
        args.putInt(KEY_VFILE_TYPE, VFileType.TYPE_LOCAL_STORAGE);
        args.putBoolean(KEY_HIDDEN_TYPE, false);
        if (mSelectDialogFragment != null) {
            mSelectDialogFragment.isloading(true);
        } else {
            if (DEBUG) Log.w(TAG, "mSelectDialogFragment is null when calling startSortFolders");
        }
        getLoaderManager().restartLoader(SCAN_FILE_LOADER, args, this);
    }
}