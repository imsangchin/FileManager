package com.asus.filemanager.utility;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.SpannableString;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileListFragment.PathIndicatorClickListener;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.dialog.MoveToDialogFragment.MoveToPathIndicatorClickListener;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;

import java.io.File;
import java.util.Stack;

public class PathIndicator {
    // for remote storage
    private static String mRealIndicatorPath = "";
    private static int mIndicatorVFileType = -1;
    private static int mIndicatorVfileStorageType = -1;
    
    //for move to dialog
    private static String mMoveToRealIndicatorPath = "";
    private static int mMoveToIndicatorVFileType = -1;
    private static int mMoveToIndicatorVfileStorageType = -1;

    private static class TouchListener implements View.OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            LinearLayout t = (LinearLayout)v;
            TextView textView;
            for (int i=0;i<t.getChildCount();i++) {
                textView = (TextView) t.getChildAt(i);
                if(action == MotionEvent.ACTION_DOWN)
                    textView.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                    textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
            }
            return false;
        }

    }
    public static void setPathIndicator(final LinearLayout pathContainer,final String showName){
        pathContainer.removeAllViews();
        Stack<LinearLayout> viewStack = new Stack<LinearLayout>();
        String oldPath = (String)pathContainer.getTag();
        if(oldPath != null && oldPath.equals(showName))
            return;
        Context mContext = pathContainer.getContext();
        //TextView view = new TextView(mContext);
        SpannableString ss;
        if(showName.equals(SambaFileUtility.SCAN_ROOT_PATH)){
            ss = new SpannableString(showName);
            mIndicatorVfileStorageType = StorageType.TYPE_NETWORK_PLACE;
            mIndicatorVFileType = VFileType.TYPE_SAMBA_STORAGE;
            mRealIndicatorPath = showName;
        }else if(showName.equals(RecentFileUtil.RECENT_SCAN_FILES)){
            String showRecentFilesName = File.separator + " " + showName + " " +File.separator;
            ss = new SpannableString(showRecentFilesName);
            mIndicatorVfileStorageType = StorageType.TYPE_RECENT_FILES;
            mIndicatorVFileType = VFileType.TYPE_LOCAL_STORAGE;
            mRealIndicatorPath = File.separator + showName + File.separator;
        }else {
            String showHomeCloudName = File.separator + " " + showName + " " +File.separator;
            ss = new SpannableString(showHomeCloudName);
            mIndicatorVfileStorageType = StorageType.TYPE_HOME_CLOUD;
            mIndicatorVFileType = VFileType.TYPE_CLOUD_STORAGE;
            mRealIndicatorPath = File.separator + showName + File.separator;
        }
        LinearLayout view = new LinearLayout(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        /*((FileManagerActivity) pathContainer.getContext()).setTextViewFont(view, FileManagerActivity.FontType.ROBOTO_REGULAR);
        view.setText(ss);
        view.setTextColor(mContext.getResources().getColor(R.color.pathcolor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mContext.getResources().getDimension(R.dimen.path_font_size));*/
        getLinearLayoutBySpannableString(view,ss);

        if (!showName.equals(SambaFileUtility.SCAN_ROOT_PATH)) {
            view.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    FileListFragment fileListFragment = (FileListFragment)((Activity)(pathContainer.getContext())).getFragmentManager().findFragmentById(R.id.filelist);
                    if (showName.equals(RecentFileUtil.RECENT_SCAN_FILES)) {
                        mIndicatorVfileStorageType = StorageType.TYPE_RECENT_FILES;
                        mRealIndicatorPath = File.separator + showName;
                        fileListFragment.showRecentFiles();
                    }else {
                        if (fileListFragment != null) {
                            fileListFragment.setListShown(false);
                        }
                        mIndicatorVfileStorageType = StorageType.TYPE_HOME_CLOUD;
                        mRealIndicatorPath = File.separator + showName;
                        RemoteFileUtility.getInstance(null).sendCloudStorageMsg(showName, null, null, MsgObj.TYPE_HOMECLOUD_STORAGE, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                    }
                }
            });
            view.setOnTouchListener(new TouchListener());
            view.setFocusable(true);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (hasFocus) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                }
            });
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    int action = event.getAction();
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (action == KeyEvent.ACTION_DOWN) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                        } else if ((action == KeyEvent.ACTION_UP && t.isFocused())) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                    return false;
                }
            });
        }
        viewStack.push(view);
        pathContainer.addView(view);
    }
/*    public static void setPathIndicator(final LinearLayout pathContainer,final String showName){
        pathContainer.removeAllViews();
        Stack<TextView> viewStack = new Stack<TextView>();
        String oldPath = (String)pathContainer.getTag();
        if(oldPath != null && oldPath.equals(showName))
            return;
        Context mContext = pathContainer.getContext();
        TextView view = new TextView(mContext);
        SpannableString ss;
        if(showName.equals(SambaFileUtility.SCAN_ROOT_PATH)){
            ss = new SpannableString(showName);
            mIndicatorVfileStorageType = StorageType.TYPE_NETWORK_PLACE;
        }else if(showName.equals(RecentFileUtil.RECENT_SCAN_FILES)){
            String showRecentFilesName = File.separator + " " + showName + " " +File.separator;
            ss = new SpannableString(showRecentFilesName);
            mIndicatorVfileStorageType = StorageType.TYPE_RECENT_FILES;
        }else {
            String showHomeCloudName = File.separator + " " + showName + " " +File.separator;
            ss = new SpannableString(showHomeCloudName);
            mIndicatorVfileStorageType = StorageType.TYPE_HOME_CLOUD;
        }

        ((FileManagerActivity) pathContainer.getContext()).setTextViewFont(view, FileManagerActivity.FontType.ROBOTO_REGULAR);
        view.setText(ss);
        view.setTextColor(mContext.getResources().getColor(R.color.pathcolor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mContext.getResources().getDimension(R.dimen.path_font_size));

        viewStack.push(view);
        if (!showName.equals(SambaFileUtility.SCAN_ROOT_PATH)) {
            view.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    FileListFragment fileListFragment = (FileListFragment)((Activity)(pathContainer.getContext())).getFragmentManager().findFragmentById(R.id.filelist);
                    if (showName.equals(RecentFileUtil.RECENT_SCAN_FILES)) {
                        mIndicatorVFileType = StorageType.TYPE_RECENT_FILES;
                        mRealIndicatorPath = File.separator + showName;
                        fileListFragment.showRecentFiles();
                    }else {
                        if (fileListFragment != null) {
                            fileListFragment.setListShown(false);
                        }
                        mIndicatorVFileType = StorageType.TYPE_HOME_CLOUD;
                        mRealIndicatorPath = File.separator + showName;
                        RemoteFileUtility.sendCloudStorageMsg(showName, null, null, MsgObj.TYPE_HOMECLOUD_STORAGE, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                    }
                }
            });
            view.setOnTouchListener(new TouchListener());
            view.setFocusable(true);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    TextView t = (TextView)v;
                    if (hasFocus) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                    } else {
                        t.setTextColor(v.getResources().getColor(R.color.pathcolor));
                    }
                }
            });
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    TextView t = (TextView)v;
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                    } else if ((event.getAction() == KeyEvent.ACTION_UP && t.isFocused())) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                    } else {
                        t.setTextColor(v.getResources().getColor(R.color.pathcolor));
                    }
                    return false;
                }
            });
        }
        pathContainer.addView(view);
    }
*/    public static void setSambaHostPathIndicator(LinearLayout pathContainer,String showName){
        setPathIndicator(pathContainer,showName);
    }

    public static void setPathIndicator(LinearLayout pathContainer,VFile file, PathIndicatorClickListener clickListener) {
        if (null == pathContainer)
            return;

        String oldPath = (String)pathContainer.getTag();
        if(file==null || (oldPath !=null && oldPath.equals(file.getAbsolutePath())))
            return;

        Stack<LinearLayout> viewStack = new Stack<LinearLayout>();

        TouchListener touchListener = new TouchListener();

        pathContainer.removeAllViews();
        int vfileType = file.getVFieType();
        String indicatorPath;
        switch(vfileType) {
        case VFileType.TYPE_SAMBA_STORAGE:
            mIndicatorVFileType = vfileType;
            mIndicatorVfileStorageType = StorageType.TYPE_SAMBA;
            int smbcount = 0;
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
            indicatorPath = (new SambaVFile(sambaFileUtility.getRootScanPath())).getName() + ((SambaVFile)file).getIndicatorPath();
            if (((SambaVFile)file).getIndicatorPath() != null){
                String[] folderStrs = ((SambaVFile)file).getIndicatorPath().split(String.valueOf(File.separatorChar));
                if (folderStrs != null && folderStrs.length > 0){
                    smbcount = folderStrs.length;
                    while(smbcount > 0){
                        String tmp = folderStrs[0];
                        if (smbcount == 1) {
                            VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath());
                            viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        } else {
                            for (int i=0;i <smbcount;i++){
                                tmp += (folderStrs[i] + File.separatorChar);
                            }
                            tmp = tmp.replaceFirst(String.valueOf(File.separatorChar), "");
                            VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath() + tmp);
                            viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        }
                        smbcount--;
                    }
                }else{
                    VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath());
                    viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                }
            }
            break;
        case VFileType.TYPE_REMOTE_STORAGE:
        case VFileType.TYPE_CLOUD_STORAGE:
            mIndicatorVFileType = vfileType;

            int count = 0;
            int storageType = ((RemoteVFile)file).getStorageType();
            mIndicatorVfileStorageType = storageType;
            switch(storageType) {
            case StorageType.TYPE_GOOGLE_DRIVE:
            case StorageType.TYPE_SKYDRIVE:
            case StorageType.TYPE_ASUSWEBSTORAGE:
//                viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
//                break;
            case StorageType.TYPE_WIFIDIRECT_STORAGE:
            case StorageType.TYPE_DROPBOX:
            case StorageType.TYPE_YANDEX:
            case StorageType.TYPE_BAIDUPCS:
            case StorageType.TYPE_HOME_CLOUD:
                // count remote storage shared path level
                if (storageType == StorageType.TYPE_WIFIDIRECT_STORAGE) {
                    indicatorPath = ((RemoteVFile)file).getIndicatorPath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                } else {
                    indicatorPath = ((RemoteVFile)file).getAbsolutePath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (i != indicatorPath.length() -1 && indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                }

                // depend on the above level, we create the related view
                while (count > 0) {
                    if (count == 1) {
                        viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
                        VFile tempVFile = file;
                        while(file != null) {
                            tempVFile = file;
                            file = file.getParentFile();
                        }
                    } else {
                        //Log.d("Jack", "file absolute path = " + ((RemoteVFile)file).getmAbsolutePath());
                        viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, false, storageType));
                        RemoteVFile tmpFile = ((RemoteVFile)file.getParentFile());
                        indicatorPath = indicatorPath.substring(0, indicatorPath.lastIndexOf('/'));
                        if(tmpFile == null){
                            tmpFile = new RemoteVFile(indicatorPath);
                            tmpFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                            tmpFile.setStorageName(((RemoteVFile)file).getStorageName(indicatorPath));
                            tmpFile.setStorageType(((RemoteVFile)file).getStorageType());
                        }
                        //tmpFile.setFileID(((RemoteVFile)file).getParentFileID());
                        tmpFile.setName(tmpFile.getFolderName(indicatorPath));
                        tmpFile.setAbsolutePath(indicatorPath);
                        file = tmpFile;
                    }
                    count--;
                }

                break;
            }

            break;
        case VFileType.TYPE_LOCAL_STORAGE:
            mIndicatorVFileType = VFileType.TYPE_LOCAL_STORAGE;
            mIndicatorVfileStorageType = -1;
            while(file != null) {
                viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, false, -1));
                file = file.getParentFile();
            }
            break;
        case VFileType.TYPE_CATEGORY_STORAGE:
             mIndicatorVFileType = VFileType.TYPE_LOCAL_STORAGE;
             mIndicatorVfileStorageType = -1;
             while(file != null && !file.getAbsolutePath().equals("/")) {
                 viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, false, -1));
                 file = file.getParentFile();
             }
             break;
        }

        mRealIndicatorPath = "";
        String temp="";
        while(!viewStack.isEmpty()) {
            LinearLayout view = viewStack.pop();
          // temp = getFolderName(view.getText());
        /*    if (!temp.equals("")) {
                if (vfileType==VFileType.TYPE_CLOUD_STORAGE&&mRealIndicatorPath.trim().equals(File.separator)) {
                    mRealIndicatorPath += temp;
                }else {
                    mRealIndicatorPath += ("/" + temp);
                }
            }*/
           // temp = getFolderName(view.getText());
            LinearLayout t = (LinearLayout)view;
            TextView textView;
            String tempFileName = "";
            for (int i=0;i<t.getChildCount();i++) {
                textView = (TextView) t.getChildAt(i);
                tempFileName += textView.getText();
            }
            temp = tempFileName;
           // temp = view.getText();
            if (!temp.equals("")) {
                mRealIndicatorPath += temp;
                /*if (temp.equals(File.separator)) {
                }else {
                    mRealIndicatorPath += "/" + temp;
                }*/
            }
            pathContainer.addView(view);
        }
        // highlight the current path
        if (pathContainer.getChildCount() != 0) {
            LinearLayout currentPathLayout = (LinearLayout)pathContainer.getChildAt(pathContainer.getChildCount()-1);
            if (currentPathLayout.getChildCount() != 0) {
                TextView currentPathTextView = (TextView)currentPathLayout.getChildAt(currentPathLayout.getChildCount()-1);
                currentPathLayout.setFocusable(false);
                currentPathLayout.setClickable(false);
                currentPathLayout.setOnFocusChangeListener(null);
                currentPathLayout.setOnClickListener(null);
                currentPathLayout.setOnKeyListener(null);
                currentPathLayout.setOnTouchListener(null);
                currentPathTextView.setTextColor(currentPathLayout.getContext().getResources().getColor(R.color.path_background));
            }
        }
        //Log.d("PathIndicator","mRealIndicatorPath:"+mRealIndicatorPath);
    }
   /* public static void setPathIndicator(LinearLayout pathContainer,VFile file, PathIndicatorClickListener clickListener) {
        String oldPath = (String)pathContainer.getTag();
        if(oldPath !=null && oldPath.equals(file.getAbsolutePath()))
            return;

        Stack<TextView> viewStack = new Stack<TextView>();

        TouchListener touchListener = new TouchListener();

        pathContainer.removeAllViews();
        int vfileType = file.getVFieType();
        String indicatorPath;
        switch(vfileType) {
        case VFileType.TYPE_SAMBA_STORAGE:
            mIndicatorVFileType = vfileType;
            mIndicatorVfileStorageType = StorageType.TYPE_SAMBA;
            int smbcount = 0;
            indicatorPath = (new SambaVFile(SambaFileUtility.getRootScanPath())).getName() + ((SambaVFile)file).getIndicatorPath();
            if (((SambaVFile)file).getIndicatorPath() != null){
                String[] folderStrs = ((SambaVFile)file).getIndicatorPath().split(String.valueOf(File.separatorChar));
                if (folderStrs != null && folderStrs.length > 0){
                    smbcount = folderStrs.length;
                    while(smbcount > 0){
                        String tmp = folderStrs[0];
                        if (smbcount == 1) {
                            VFile tempVFile = new SambaVFile(SambaFileUtility.getRootScanPath());
                            viewStack.push(creatView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        } else {
                            for (int i=0;i <smbcount;i++){
                                tmp += (folderStrs[i] + File.separatorChar);
                            }
                            tmp = tmp.replaceFirst(String.valueOf(File.separatorChar), "");
                            VFile tempVFile = new SambaVFile(SambaFileUtility.getRootScanPath() + tmp);
                            viewStack.push(creatView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        }
                        smbcount--;
                    }
                }else{
                    VFile tempVFile = new SambaVFile(SambaFileUtility.getRootScanPath());
                    viewStack.push(creatView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                }
            }
            break;
        case VFileType.TYPE_REMOTE_STORAGE:
        case VFileType.TYPE_CLOUD_STORAGE:
            mIndicatorVFileType = vfileType;

            int count = 0;
            int storageType = ((RemoteVFile)file).getStorageType();
            mIndicatorVfileStorageType = storageType;
            switch(storageType) {
            case StorageType.TYPE_GOOGLE_DRIVE:
            case StorageType.TYPE_SKYDRIVE:
            case StorageType.TYPE_ASUSWEBSTORAGE:
//                viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
//                break;
            case StorageType.TYPE_WIFIDIRECT_STORAGE:
            case StorageType.TYPE_DROPBOX:
            case StorageType.TYPE_BAIDUPCS:
            case StorageType.TYPE_HOME_CLOUD:
                // count remote storage shared path level
                if (storageType == StorageType.TYPE_WIFIDIRECT_STORAGE) {
                    indicatorPath = ((RemoteVFile)file).getIndicatorPath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                } else {
                    indicatorPath = ((RemoteVFile)file).getmAbsolutePath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (i != indicatorPath.length() -1 && indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                }

                // depend on the above level, we create the related view
                while (count > 0) {
                    if (count == 1) {
                        viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
                        VFile tempVFile = file;
                        while(file != null) {
                            tempVFile = file;
                            file = file.getParentFile();
                        }
                    } else {
                        Log.d("Jack", "file absolute path = " + ((RemoteVFile)file).getmAbsolutePath());
                        viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, false, storageType));
                        RemoteVFile tmpFile = ((RemoteVFile)file.getParentFile());
                        indicatorPath = indicatorPath.substring(0, indicatorPath.lastIndexOf('/'));
                        if(tmpFile == null){
                            tmpFile = new RemoteVFile(indicatorPath);
                            tmpFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                            tmpFile.setStorageName(((RemoteVFile)file).getStorageName(indicatorPath));
                            tmpFile.setStorageType(((RemoteVFile)file).getStorageType());
                        }
                        //tmpFile.setFileID(((RemoteVFile)file).getParentFileID());
                        tmpFile.setName(tmpFile.getFolderName(indicatorPath));
                        tmpFile.setAbsolutePath(indicatorPath);
                        file = tmpFile;
                    }
                    count--;
                }

                break;
            }

            break;
        case VFileType.TYPE_LOCAL_STORAGE:
            mIndicatorVFileType = VFileType.TYPE_LOCAL_STORAGE;
            mIndicatorVfileStorageType = -1;
            while(file != null) {
                viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, false, -1));
                file = file.getParentFile();
            }
            break;
        }

        mRealIndicatorPath = "";
        CharSequence temp;
        while(!viewStack.isEmpty()) {
            TextView view = viewStack.pop();
            // temp = getFolderName(view.getText());
            if (!temp.equals("")) {
                if (vfileType==VFileType.TYPE_CLOUD_STORAGE&&mRealIndicatorPath.trim().equals(File.separator)) {
                    mRealIndicatorPath += temp;
                }else {
                    mRealIndicatorPath += ("/" + temp);
                }
            }
            // temp = getFolderName(view.getText());
            temp = view.getText();
            if (!temp.equals("")) {
                mRealIndicatorPath += temp;
                if (temp.equals(File.separator)) {
                }else {
                    mRealIndicatorPath += "/" + temp;
                }
            }
            pathContainer.addView(view);
        }
        Log.d("PathIndicator","mRealIndicatorPath:"+mRealIndicatorPath);
    }*/

   /* private static TextView creatView(VFile file, Context context, TouchListener touchListener, PathIndicatorClickListener clickListener, boolean isRemoteRoot, int type) {
        TextView view = new TextView(context);
        SpannableString ss;
            if (isRemoteRoot) {
                ss = new SpannableString("/" + ((RemoteVFile)file).getStorageName()+ "/");
            } else {
                if(file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                    ss = new SpannableString(((RemoteVFile)file).getName()+ "/");
                    Log.d("Jack", "indicator file name = " + ((RemoteVFile)file).getName());
                }else if(file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
                    String fullPath = file.getAbsolutePath();
                    if(fullPath.equals(SambaFileUtility.getRootScanPath())){
                        String username = SambaFileUtility.getPcUserName();
                        ss = new SpannableString(File.separator + username +  File.separator);
                    }else{
                        ss = new SpannableString(file.getName()+ "/");
                        }
                }else{
                    ss = new SpannableString(file.getName()+ "/");
                }
            }
            if(!file.getName().equalsIgnoreCase(""))
                ss.setSpan(new UnderlineSpan(), 0, ss.length()-3, 0);

            if(isRemoteRoot)
                ss.setSpan(new UnderlineSpan(), 3, ss.length()-3, 0);
//        }
        ((FileManagerActivity)context).setTextViewFont(view, FileManagerActivity.FontType.ROBOTO_REGULAR);
        view.setText(ss);
        view.setTextColor(context.getResources().getColor(R.color.pathcolor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.path_font_size));

        // for remote storage , this is error case
        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE && ((RemoteVFile)file).getStorageName() == RemoteVFile.DEFAULT_REMOTE_STORAGE_NAME) {
            view.setClickable(false);
            view.setFocusable(false);
        }
        else {
            view.setTag(file);
            view.setOnClickListener(clickListener);
            view.setOnTouchListener(touchListener);
            view.setFocusable(true);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    TextView t = (TextView)v;
                    if (hasFocus) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                    } else {
                        t.setTextColor(v.getResources().getColor(R.color.pathcolor));
                    }
                }
            });
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    TextView t = (TextView)v;
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                    } else if ((event.getAction() == KeyEvent.ACTION_UP && t.isFocused())) {
                        t.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                    } else {
                        t.setTextColor(v.getResources().getColor(R.color.pathcolor));
                    }
                    return false;
                }
            });
        }
        return view;
    }*/
    private static LinearLayout creatLinearLayoutView(VFile file, Context context, TouchListener touchListener, PathIndicatorClickListener clickListener, boolean isRemoteRoot, int type) {
        LinearLayout view = new LinearLayout(context);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        SpannableString ss;

        if (isRemoteRoot) {
            ss = new SpannableString("/" + ((RemoteVFile)file).getStorageName()+ "/");
        } else {
            if(file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                ss = new SpannableString(((RemoteVFile)file).getName()+ "/");
                Log.d("PathIndicator", "indicator file name = " + ((RemoteVFile)file).getName());
            }else if(file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
                String fullPath = file.getAbsolutePath();
                SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                if(fullPath.equals(sambaFileUtility.getRootScanPath())){
                    String username = sambaFileUtility.getPcUserName();
                    ss = new SpannableString(File.separator + username +  File.separator);
                }else{
                    ss = new SpannableString(file.getName()+ "/");
                }
            }else{
                ss = new SpannableString(file.getName()+ "/");
            }
        }

   /* 	((FileManagerActivity)context).setTextViewFont(view, FileManagerActivity.FontType.ROBOTO_REGULAR);
        view.setText(ss);
        view.setTextColor(context.getResources().getColor(R.color.pathcolor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.path_font_size));*/

        // for remote storage , this is error case
        getLinearLayoutBySpannableString(view, ss);

        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE && ((RemoteVFile)file).getStorageName() == RemoteVFile.DEFAULT_REMOTE_STORAGE_NAME) {
            view.setClickable(false);
            view.setFocusable(false);
        }
        else {
            view.setTag(file);
            view.setOnClickListener(clickListener);
            view.setOnTouchListener(touchListener);
            view.setFocusable(true);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (hasFocus) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                }
            });
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    int action = event.getAction();
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (action == KeyEvent.ACTION_DOWN) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                        } else if ((action == KeyEvent.ACTION_UP && t.isFocused())) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                    return false;
                }
            });
        }
        return view;
    }
    private static LinearLayout getLinearLayoutBySpannableString(LinearLayout tempLayout,SpannableString ss){
        if (ss != null && ss.length()>0) {
            String ssStr = ss.toString();
            Context context = tempLayout.getContext();
            TextView textView = null;
            if (File.separator.equals(ssStr)) {
                addTextViewToLinearLayout(tempLayout,context,context.getResources().getString(R.string.device_root_path));
            }else {
                if (File.separator.equals( ssStr.charAt(0)+"")) {
                //	addTextViewToLinearLayout(tempLayout,context,File.separator);
                }
                String[] tempViewStr = ssStr.split(File.separator);
                for (String temp : tempViewStr) {
                    if (temp!=null&&temp.trim().length()>0) {
                        addTextViewToLinearLayout(tempLayout,context,temp);
                    }
                }
                if (File.separator.equals( (ssStr.charAt(ssStr.length()-1)+""))) {
                //	addTextViewToLinearLayout(tempLayout,context,File.separator);
                }
            }
        }
        return tempLayout;
    }
    public static void addTextViewToLinearLayout(LinearLayout layout,Context context,String text){
        TextView textView = null;
        textView = new TextView(context);
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_arrow_right_normal, 0, 0, 0);
        textView.setText(text);
        textView.setTextColor(context.getResources().getColor(R.color.pathcolor));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.path_font_size));
        if(context instanceof FileManagerActivity) {
            ((FileManagerActivity) context).setTextViewFont(textView, FileManagerActivity.FontType.ROBOTO_REGULAR);
        } else if(context instanceof ContextThemeWrapper) {
            ((FileManagerActivity)(((ContextWrapper) context).getBaseContext())).setTextViewFont(textView, FileManagerActivity.FontType.ROBOTO_REGULAR);
        }
        textView.setClickable(false);
        textView.setFocusable(false);
        layout.addView(textView);
    }
    private static CharSequence getFolderName(CharSequence value) {
        CharSequence name = "";
        // indicator display format is / xxxx /
        if (value.length() > 3) {
            name = value.subSequence(0, value.length() - 3);
        }
        if (name.length()>3) {
            if((name.subSequence(0, 3).toString()).equals(" / ")){
                name=name.subSequence(3, name.length());
            }
        }
        return name;
    }

    public static String getRealIndiatorPath() {
        return mRealIndicatorPath;
    }

    public static int getIndicatorVFileType() {
        return mIndicatorVFileType;
    }
    public static int getIndicatorVfileStorageType(){
        return mIndicatorVfileStorageType;
    }

    /**add for move to & copy to dialog***/
    public static void setMoveToPathIndicator(LinearLayout pathContainer,VFile file, MoveToPathIndicatorClickListener clickListener) {
        String oldPath = (String)pathContainer.getTag();
        if(file==null || (oldPath !=null && oldPath.equals(file.getAbsolutePath())))
            return;

        Stack<LinearLayout> viewStack = new Stack<LinearLayout>();

        TouchListener touchListener = new TouchListener();

        pathContainer.removeAllViews();
        int vfileType = file.getVFieType();
        String indicatorPath;
        switch(vfileType) {
        case VFileType.TYPE_SAMBA_STORAGE:
            mMoveToIndicatorVFileType = vfileType;
            mMoveToIndicatorVfileStorageType = StorageType.TYPE_SAMBA;
            int smbcount = 0;
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
            indicatorPath = (new SambaVFile(sambaFileUtility.getRootScanPath())).getName() + ((SambaVFile)file).getIndicatorPath();
            if (((SambaVFile)file).getIndicatorPath() != null){
                String[] folderStrs = ((SambaVFile)file).getIndicatorPath().split(String.valueOf(File.separatorChar));
                if (folderStrs != null && folderStrs.length > 0){
                    smbcount = folderStrs.length;
                    while(smbcount > 0){
                        String tmp = folderStrs[0];
                        if (smbcount == 1) {
                            VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath());
                            viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        } else {
                            for (int i=0;i <smbcount;i++){
                                tmp += (folderStrs[i] + File.separatorChar);
                            }
                            tmp = tmp.replaceFirst(String.valueOf(File.separatorChar), "");
                            VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath() + tmp);
                            viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                        }
                        smbcount--;
                    }
                }else{
                    VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath());
                    viewStack.push(creatLinearLayoutView(tempVFile,pathContainer.getContext(),touchListener,clickListener, false, StorageType.TYPE_SAMBA));
                }
            }
            break;
        case VFileType.TYPE_REMOTE_STORAGE:
        case VFileType.TYPE_CLOUD_STORAGE:
            mMoveToIndicatorVFileType = vfileType;

            int count = 0;
            int storageType = ((RemoteVFile)file).getStorageType();
            mMoveToIndicatorVfileStorageType = storageType;
            switch(storageType) {
            case StorageType.TYPE_GOOGLE_DRIVE:
            case StorageType.TYPE_SKYDRIVE:
            case StorageType.TYPE_ASUSWEBSTORAGE:
//                viewStack.push(creatView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
//                break;
            case StorageType.TYPE_WIFIDIRECT_STORAGE:
            case StorageType.TYPE_DROPBOX:
            case StorageType.TYPE_YANDEX:
            case StorageType.TYPE_BAIDUPCS:
            case StorageType.TYPE_HOME_CLOUD:
                // count remote storage shared path level
                if (storageType == StorageType.TYPE_WIFIDIRECT_STORAGE) {
                    indicatorPath = ((RemoteVFile)file).getIndicatorPath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                } else {
                    indicatorPath = ((RemoteVFile)file).getAbsolutePath();
                    for (int i=0 ; i < indicatorPath.length() ; i++) {
                        if (i != indicatorPath.length() -1 && indicatorPath.charAt(i) == File.separatorChar) {
                            count++;
                        }
                    }
                }

                // depend on the above level, we create the related view
                while (count > 0) {
                    if (count == 1) {
                        viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, true, storageType));
                        VFile tempVFile = file;
                        while(file != null) {
                            tempVFile = file;
                            file = file.getParentFile();
                        }
                    } else {
                        //Log.d("Jack", "file absolute path = " + ((RemoteVFile)file).getmAbsolutePath());
                        viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, false, storageType));
                        RemoteVFile tmpFile = ((RemoteVFile)file.getParentFile());
                        indicatorPath = indicatorPath.substring(0, indicatorPath.lastIndexOf('/'));
                        if(tmpFile == null){
                            tmpFile = new RemoteVFile(indicatorPath);
                            tmpFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                            tmpFile.setStorageName(((RemoteVFile)file).getStorageName(indicatorPath));
                            tmpFile.setStorageType(((RemoteVFile)file).getStorageType());
                        }
                        //tmpFile.setFileID(((RemoteVFile)file).getParentFileID());
                        tmpFile.setName(tmpFile.getFolderName(indicatorPath));
                        tmpFile.setAbsolutePath(indicatorPath);
                        file = tmpFile;
                    }
                    count--;
                }

                break;
            }

            break;
        case VFileType.TYPE_LOCAL_STORAGE:
            mMoveToIndicatorVFileType = VFileType.TYPE_LOCAL_STORAGE;
            mMoveToIndicatorVfileStorageType = -1;
            while(file != null) {
                viewStack.push(creatLinearLayoutView(file,pathContainer.getContext(),touchListener,clickListener, false, -1));
                file = file.getParentFile();
            }
            break;
        }

        mMoveToRealIndicatorPath = "";
        String temp="";
        while(!viewStack.isEmpty()) {
            LinearLayout view = viewStack.pop();
          // temp = getFolderName(view.getText());
        /*    if (!temp.equals("")) {
                if (vfileType==VFileType.TYPE_CLOUD_STORAGE&&mRealIndicatorPath.trim().equals(File.separator)) {
                    mRealIndicatorPath += temp;
                }else {
                    mRealIndicatorPath += ("/" + temp);
                }
            }*/
           // temp = getFolderName(view.getText());
            LinearLayout t = (LinearLayout)view;
            TextView textView;
            String tempFileName = "";
            for (int i=0;i<t.getChildCount();i++) {
                textView = (TextView) t.getChildAt(i);
                tempFileName += textView.getText();
            }
            temp = tempFileName;
           // temp = view.getText();
            if (!temp.equals("")) {
                mMoveToRealIndicatorPath += temp;
                /*if (temp.equals(File.separator)) {
                }else {
                    mRealIndicatorPath += "/" + temp;
                }*/
            }
            pathContainer.addView(view);
        }
        //Log.d("PathIndicator","mRealIndicatorPath:"+mRealIndicatorPath);
    }

    private static LinearLayout creatLinearLayoutView(VFile file, Context context, TouchListener touchListener, MoveToPathIndicatorClickListener clickListener, boolean isRemoteRoot, int type) {
        LinearLayout view = new LinearLayout(context);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        SpannableString ss;

        if (isRemoteRoot) {
            ss = new SpannableString("/" + ((RemoteVFile)file).getStorageName()+ "/");
        } else {
            if(file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                ss = new SpannableString(((RemoteVFile)file).getName()+ "/");
                Log.d("PathIndicator", "indicator file name = " + ((RemoteVFile)file).getName());
            }else if(file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
                String fullPath = file.getAbsolutePath();
                SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                if(fullPath.equals(sambaFileUtility.getRootScanPath())){
                    String username = sambaFileUtility.getPcUserName();
                    ss = new SpannableString(File.separator + username +  File.separator);
                }else{
                    ss = new SpannableString(file.getName()+ "/");
                }
            }else{
                ss = new SpannableString(file.getName()+ "/");
            }
        }

   /* 	((FileManagerActivity)context).setTextViewFont(view, FileManagerActivity.FontType.ROBOTO_REGULAR);
        view.setText(ss);
        view.setTextColor(context.getResources().getColor(R.color.pathcolor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.path_font_size));*/

        // for remote storage , this is error case
        getLinearLayoutBySpannableString(view, ss);
        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE && ((RemoteVFile)file).getStorageName() == RemoteVFile.DEFAULT_REMOTE_STORAGE_NAME) {
            view.setClickable(false);
            view.setFocusable(false);
        }
        else {
            view.setTag(file);
            view.setOnClickListener(clickListener);
            view.setOnTouchListener(touchListener);
            view.setFocusable(true);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (hasFocus) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                }
            });
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    LinearLayout t = (LinearLayout)v;
                    TextView textView;
                    int action = event.getAction();
                    for (int i=0;i<t.getChildCount();i++) {
                        textView = (TextView) t.getChildAt(i);
                        if (action == KeyEvent.ACTION_DOWN) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_press));
                        } else if ((action == KeyEvent.ACTION_UP && t.isFocused())) {
                            textView.setTextColor(v.getResources().getColor(R.color.path_indicator_focus));
                        } else {
                            textView.setTextColor(v.getResources().getColor(R.color.pathcolor));
                        }
                    }
                    return false;
                }
            });
        }
        return view;
    }

    public static String getMoveToRealIndiatorPath() {
        return mMoveToRealIndicatorPath;
    }

    public static int getMoveToIndicatorVFileType() {
        return mMoveToIndicatorVFileType;
    }
    public static int getMoveToIndicatorVfileStorageType(){
        return mMoveToIndicatorVfileStorageType;
    }
}
