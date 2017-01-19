package com.asus.filemanager.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.dialog.CtaDialogFragment;
import com.asus.filemanager.dialog.FileExistDialogFragment;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.NotificationCompat.Builder;

public class ItemOperationUtility {
    private final static String TAG = "ItemOperationUtility";
    private static ItemOperationUtility mItemOperation = null;

    private boolean needToGoBack = false;
    private ArrayList<PositionItem> mScrollPositionList = new ArrayList<PositionItem>();
    private static ListView mListView = null;
    private  GridView mGridView = null;
    public final static String DRM_ITEM = ".fl";
    private static boolean hasDRM = false;
    public static boolean MicroHasDRM = false;
    public static boolean UsbHasDRM = false;

    /***for notification******/
    private static NotificationManager manager;
    private static Builder builder;
    private final static int LOCAL_NOTIFICATION_ID = 128;
    private static long lastTime = 0;
    private static int ToTalSize = 0;
    private static int CurrentSize = 0;
    private static boolean ShowLocalNotification = false;

    /**for filelistfragment****/
    public static boolean isReadyToPaste = false;

    /****for DRM file observer******/
    public static final String DEFAULT_INDICATOR_FILE =
            WrapEnvironment.getEpadInternalStorageDirectory().getAbsolutePath();

    /*****for get removable storage type******/
    public static class  MountedStorage{
        public static final int MICROSD = 0;
        public static final int USBDISK = 1;
        public static final int USBDISK_AND_MICROSD = 2;
    };

    /*****for show CTA check******************/
    private Handler mHandlder;
    private final static String ACCESS_NETWORK_USER_PERMISSION = "access_network_permission";

    /*****for View Switch*******/
//    public void
    public boolean isListView = true;


    /***for Back to Preview****/
    private Stack<VFile> mOpertaionStack = new Stack<VFile>();
    private boolean isFromBackPressed = false;

    public static ItemOperationUtility getInstance(){
        if(mItemOperation == null){
            mItemOperation = new ItemOperationUtility();
        }
        return mItemOperation;
    }

    public void switchViewMode(){
        isListView = !isListView;
    }

    public boolean isListViewMode(){
        return isListView;
    }

    public void saveViewModeToPreferences(Context context) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putBoolean("isListView", isListView).commit();
    }

    public void loadViewModeFromPreferences(Context context, boolean defValue) {
        isListView = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean("isListView", defValue);
    }

    public boolean containsViewModeInPreferences(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).contains("isListView");
    }

    public void setFirstLaunchTime(Context context, long time) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putLong("firstLaunchTime", time).commit();
    }

    public long getFirstLaunchTime(Context context, long defValue) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getLong("firstLaunchTime", defValue);
    }

    public boolean hasSetFirstLaunchTime(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).contains("firstLaunchTime");
    }

    /* We intent to use the same key for large file and recent files to prevent duplicate notification*/
    //begin large file retention
    public void setLastCheckLargeFileRetentionTime(Context context, long time) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putLong("lastCheckRetentionTime", time).commit();
    }

    public long getLastCheckLargeFileRetentionTime(Context context, long defValue) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getLong("lastCheckRetentionTime", defValue);
    }

    public boolean hasSetLastCheckLargeFileRetentionTime(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).contains("lastCheckRetentionTime");
    }
    //end large file retention

    //begin recent file retention
    public void setLastCheckRecentFileRetentionTime(Context context, long time) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putLong("lastCheckRetentionTime", time).commit();
    }

    public long getLastCheckRecentFileRetentionTime(Context context, long defValue) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getLong("lastCheckRetentionTime", defValue);
    }

    public boolean hasSetLastCheckRecentFileRetentionTime(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).contains("lastCheckRetentionTime");
    }
    //end recent file retention

    //begin retention period
    public void setMinRetentionPeriod(Context context, long time) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putLong("RETENTION_MIN_PERIOD", time).commit();
    }

    public long getMinRetentionPeriod(Context context, long defValue) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getLong("RETENTION_MIN_PERIOD", defValue);
    }
    public void setLastRetentionNotificationTime(Context context, long time) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putLong("lastRetentionNotificationTime", time).commit();
    }

    public long getLastRetentionNotificationTime(Context context, long defValue) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getLong("lastRetentionNotificationTime", defValue);
    }
    public boolean hasSetLastRetentionNotificationTime(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).contains("lastRetentionNotificationTime");
    }
    //end recent file retention

    public void setMoveToLastFileType(Context context, int vFileType) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putInt("MoveToLastFileType", vFileType).commit();
    }

    public void setMoveToLastPath(Context context, String path) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putString("MoveToLastFilePath", path).commit();
    }

    public void setMoveToLastAccountName(Context context, String accountName) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putString("MoveToLastAccountName", accountName).commit();
    }

    public int getMoveToLastFileType(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getInt("MoveToLastFileType", -1);
    }

    public String getMoveToLastPath(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString("MoveToLastFilePath", null);
    }

    public String getMoveToLastAccount(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getString("MoveToLastAccountName", null);
    }

    public void setPreviousFolderUsed(Context context, boolean isUsed) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putBoolean("isPreviousFolderUsed", isUsed).commit();
    }

    public void setCurrentFolderUsed(Context context, boolean isUsed) {
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putBoolean("isCurrentFolderUsed", isUsed).commit();
    }

    public boolean isPreviousFolderUsed(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean("isPreviousFolderUsed", false);
    }

    public boolean isCurrentFolderUsed(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean("isCurrentFolderUsed", false);
    }

    public void setLastViewPosition(AbsListView absView){
        if(absView instanceof ListView){
            mListView = (ListView)absView;
            if(mListView != null){
                int index = mListView.getFirstVisiblePosition();
                View v = mListView.getChildAt(0);
                int top = (v == null) ? 0 : v.getTop();
                PositionItem item = new PositionItem(index,top);
                mScrollPositionList.add(item);
                setNeedGoBack(false);
                mListView.setSelection(0);
             }
         }else if(absView instanceof GridView){
             mGridView = (GridView)absView;
             if(mGridView != null){
                 int index = mGridView.getFirstVisiblePosition();
                 View v = mGridView.getChildAt(0);
                 int top = (v == null) ? 0 : v.getTop();
                 PositionItem item = new PositionItem(index,top);
                 mScrollPositionList.add(item);
                 setNeedGoBack(false);
                 mGridView.setSelection(0);
             }
         }
     }



     public void showLastView(AbsListView adsView){
         Log.d(TAG,"mScrollPositionList.size() == " + mScrollPositionList.size() + "==adsView=" + adsView);
         if(mScrollPositionList.size() == 0){
             return;
         }
         int lastItemPos = mScrollPositionList.size() - 1;
         int index = mScrollPositionList.get(lastItemPos).getIndex();
         int top = mScrollPositionList.get(lastItemPos).getTop();
         if (adsView instanceof ListView) {
             Log.d(TAG,"=ListView  ==index==" + index + "==top=" + top);
             ((ListView)adsView).setSelectionFromTop(index, top);
         } else {
             Log.d(TAG,"=index==" + index + "==top=" + top);
             ((GridView)adsView).setSelection(index);
         }
         mScrollPositionList.remove(lastItemPos);
         setNeedGoBack(false);
     }

     public void dumpRightPosition(int level){
         if(level < 1){
             return;
         }
         for(int i = 0; i < (level - 1); i++){
             if(mScrollPositionList.size() == 0){
                 break;
             }
             mScrollPositionList.remove(mScrollPositionList.size() - 1);
         }
         setNeedGoBack(true);
     }

     public void setNeedGoBack(boolean need){
         needToGoBack = need;
     }

     public  void resetScrollPositionList(){
         if(mListView != null){
             mListView.setSelection(0);
         }
         if(mGridView != null){
             mGridView.setSelection(0);
         }
         mScrollPositionList.clear();
         needToGoBack = false;
     }

     public boolean getNeedGoBack(){
         return needToGoBack;
     }

     private static class PositionItem{
         private  int index = -1;
         private  int top = -1;

         private PositionItem(int indexPos,int topPos){
             this.index = indexPos;
             this.top = topPos;
         }

         private int getIndex(){
             return this.index;
         }

         private int getTop(){
             return this.top;
         }
     }

     public static void resetNotificationParams(){
         manager = null;
         builder = null;
     }

     public static void initNotificationBar(Activity context){
         if(context == null){
             return;
         }

         ShowLocalNotification = true;

         if(manager == null || builder == null){
             manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
             builder = new Builder(context);
         }

         Intent mShowDialogIntent = new Intent();
         mShowDialogIntent.setAction(Intent.ACTION_MAIN);
         mShowDialogIntent.setClass(context, FileManagerActivity.class);
         mShowDialogIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
         PendingIntent mIntent = PendingIntent.getActivity(context, 0, mShowDialogIntent, 0);

         builder.setSmallIcon(R.drawable.asus_ep_ic_internal_storage)
         .setProgress(100, 0, false)
         .setContentTitle(context.getResources().getString(R.string.paste_progress))
         .setContentIntent(mIntent);
         manager.notify(LOCAL_NOTIFICATION_ID, builder.build());
     }

     public static void cancelNotification(Activity context){
         if(manager == null || builder == null){
             manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
             builder = new Builder(context);
         }
         builder.setProgress(0, 0, false);
         manager.cancel(LOCAL_NOTIFICATION_ID);
         manager = null;
         builder = null;
         ToTalSize =0;
         CurrentSize = 0;
         ShowLocalNotification = false;
     }

     public static void updateNotificationBar(String fileName,int total,int currentSize,Activity context){
         if(!ShowLocalNotification){
             return;
         }
         CurrentSize = currentSize;
         ToTalSize = total;
         if(manager == null || builder == null){
             manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
             builder = new Builder(context);
             builder.setSmallIcon(R.drawable.asus_ep_ic_internal_storage);
         }
         builder.setProgress(total, currentSize, false);
         builder.setContentText(fileName);

         long currentTime = System.currentTimeMillis();
         if(currentTime - lastTime > 1000){
             manager.notify(LOCAL_NOTIFICATION_ID, builder.build());
             lastTime = currentTime;
         }

     }

     public static void resumeNotificationBar(EditPool pool,Activity context){

         if(manager != null && builder != null && pool.getSize() > 0){

             FileExistDialogFragment fileExistFragment = (FileExistDialogFragment)context.getFragmentManager().findFragmentByTag("VFileExistDialogFragment");
             if(fileExistFragment != null && fileExistFragment.getDialog().isShowing()){
                 return;
             }

             PasteDialogFragment pasteDialogFragment = (PasteDialogFragment)context.getFragmentManager().findFragmentByTag("PasteDialogFragment");
             if (pasteDialogFragment == null){
                 pasteDialogFragment = PasteDialogFragment.newInstance(pool);
                 if (pasteDialogFragment != null){
                     pasteDialogFragment.show(context.getFragmentManager(), "PasteDialogFragment");
                     int percent = (int)(((double)CurrentSize/(double)ToTalSize)*100);
                     pasteDialogFragment.setInitProgressByNotification(context,percent, (double)CurrentSize, (double)ToTalSize);
                 }
             }
         }
     }


     private static VFile[] getSelectFileArray(VFile[] source){
         VFile[] filtArray = null;
         ArrayList<VFile> array = new ArrayList<VFile>();
         if(source != null && source.length > 0){
             for(VFile file : source){
                 if(file.getChecked()){
                     array.add(file);
                 }
             }
         }
         if(array.size() > 0){
             filtArray = new VFile[array.size()];
             /*for(int i = 0;i < array.size();i++){
                 filtArray[i] = array.get(i);
             }*/
             array.toArray(filtArray);
         }
         return filtArray;
     }

     private static boolean isItemContainDrm(VFile[] source,boolean spinner){
         VFile[] fileArray = spinner ? source : getSelectFileArray(source);
         if(fileArray != null && fileArray.length>0 && fileArray[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE){
             try{
                 if(FileListFragment.sDrmPaths != null && !FileListFragment.sDrmPaths.isEmpty() && !FileListFragment.sDrmPaths.contains(File.separator + FileUtility.getCanonicalPath(fileArray[0].getParentFile())))
                     return false;
                 for(VFile file : fileArray){
                     if(file.getName().endsWith(DRM_ITEM) || (FileListFragment.sDrmPaths != null && !FileListFragment.sDrmPaths.isEmpty() && FileListFragment.sDrmPaths.contains(File.separator + FileUtility.getCanonicalPath(file)))){
                         return true;
                     }
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }
         return false;
     }

     public static boolean isItemContainDrm(VFile[] source,boolean spinner,boolean firstScan){
         return ConstantsUtil.IS_AT_AND_T ? isItemContainDrm(source,spinner) : false;

         /*boolean result = false;
         VFile[] fileArray = null;
         if(source != null){
             for(VFile file : source){
                 if(file.getVFieType() != VFileType.TYPE_LOCAL_STORAGE){
                     return result;
                 }
             }
         }else{
             return false;
         }

         if(spinner){
             fileArray = source;
         }else{
             fileArray  = getSelectFileArray(source);
         }

         if(fileArray != null){
             for(VFile file : fileArray){

                 String filePath = file.getAbsolutePath();

                 if(!firstScan && filePath.equals(DEFAULT_INDICATOR_FILE)){
                     return hasDRM;
                 }

                 if(!firstScan && filePath.startsWith("/Removable")){
                     if(filePath.endsWith("MicroSD")){
                         return MicroHasDRM;
                     }else if(filePath.endsWith("USBdisk1")){
                         return UsbHasDRM;
                     }else if(filePath.equals("/Removable")){
                         return (MicroHasDRM || UsbHasDRM);
                     }
                 }

                 if(file.isDirectory()){
                     Stack<VFile> mFolderStack = new Stack<VFile>();
                     mFolderStack.push(file);
                     while(!mFolderStack.isEmpty() && !result){
                         VFile mSubFile = mFolderStack.pop();
                         VFile[] files = mSubFile.listVFiles();
                         if(files != null){
                             for(VFile subVF : files){
                                 if(subVF.isDirectory()){
                                     mFolderStack.push(subVF);
                                 }else{
                                     if(subVF.getName().endsWith(DRM_ITEM)){
                                         result = true;
                                         break;
                                     }
                                 }
                             }
                         }

                     }

                     if(!mFolderStack.isEmpty()){
                         mFolderStack.clear();
                     }

                     if(result){
                         return result;
                     }
                 }else{
                     if(file.getName().endsWith(DRM_ITEM)){
                         result = true;
                         break;
                     }
                 }
             }
         }
         return result;*/
     }

     public static void disableItemMenu(MenuItem item,String name){
         if(item != null && !TextUtils.isEmpty(name)){
             SpannableString s = new SpannableString(name);
             s.setSpan(new ForegroundColorSpan(Color.GRAY) , 0, s.length(), 0);
             item.setTitle(s);
         }
     }

     public void ScanInterDiskDrm(final String path){
         Thread scanThread = new Thread(new Runnable(){

            @Override
            public void run() {
                 File rootFile = new File(path);
                 VFile sFile = new VFile(rootFile);
                 VFile[] fileArray = new VFile[1];
                 fileArray[0] = sFile;
                 boolean result = isItemContainDrm(fileArray,true,true);
                 if(path.equals(DEFAULT_INDICATOR_FILE)){
                     hasDRM = result;
                 }else if(path.equals("/Removable/MicroSD")){
                     MicroHasDRM = result;
                 }else{
                     UsbHasDRM = result;
                 }
            }

         });
         scanThread.start();
     }

     public void ScanAllStorageDiskForDrm(){
         String[] PathArray = new String[]{DEFAULT_INDICATOR_FILE,"/Removable/MicroSD","/Removable/USBdisk1"};
         for(String path : PathArray){
             ScanInterDiskDrm(path);
         }
     }

     public String getSuitableEncoding(Context context){

         String defaultValue = context.getString(R.string.default_encoding);
         String lastValue = System.getProperty("prop.unzip.encode", "");
         if(!TextUtils.isEmpty(lastValue)){
             defaultValue = lastValue;
//             String[] CountryArray = new String[]{"CN","TW","JP"};
//             String country = context.getResources().getConfiguration().locale.getCountry();
//             for(String cty : CountryArray){
//                 if(cty.equals(country)){
//                     defaultValue = "GBK";
//                 }
//             }
//             System.setProperty("prop.unzip.encode",defaultValue);
         }

         return defaultValue;
     }

//     public static int getMountedStorageType(){
//         int type = -1;
//         final StorageManager mStorageManager = (StorageManager) mContext.getSystemService(
//                 Context.STORAGE_SERVICE);
//         FileManagerApplication application = (FileManagerApplication) FileManagerApplication.getAppContext();
//         ArrayList<StorageVolume> mVolumeList = application.getStorageVolume();
//         for(StorageVolume volume : mVolumeList){
//             String folderPath = volume.getPath();
//             if(!folderPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath()) && mStorageManager.getVolumeState(folderPath).equals(Environment.MEDIA_MOUNTED)){
//                  if(folderPath.equals("/storage/USBdisk1") || folderPath.equals("/storage/USBdisk2")){
//
//                  }
//             }
//
//         }
//     }
//



     public boolean checkCtaPermission(Context context){
         boolean agree = false;
         if(context == null){
             return agree;
         }
         if(enableCtaCheck()){

             HandlerThread thread = new HandlerThread("CTA_SERVICE");
             thread.start();
             mHandlder = new Handler(thread.getLooper());
             CtaPermissonChecker checker = new CtaPermissonChecker(context);
             synchronized(checker.checkCtaLock){
                 mHandlder.post(checker);
                 try {
                     checker.checkCtaLock.wait();
                     agree = checker.getResult();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
             }

         }else{
             agree = true;
         }

         if(!agree){
             ToastUtility.show(context, context.getResources().getString(R.string.network_cta_hint));
         }

         return agree;
     }

     public void showCtaDialog(Context context){
         if(context == null){
             return;
         }
         CtaPermissonChecker checker = new CtaPermissonChecker(context);
         checker.showDialog(context, true);
     }

     private class CtaPermissonChecker implements Runnable{
        Object checkCtaLock = new Object();
        Context mContext;
        boolean result = false;
        CtaPermissonChecker(Context context){
            mContext = context;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            synchronized(checkCtaLock){
                boolean exist = queryPermission(mContext);
                if(exist){
                    checkCtaLock.notifyAll();
                }else{
                    showDialog(mContext,false);
                }
            }
        }

        private boolean queryPermission(Context context){
            boolean hasRecord = true;
            int status = getUserPermission(context);// -1 noRecord; 0 remember refuse ;1 remember agree
            if (status == CtaDialogFragment.CTA_NO_RECORD) {
                hasRecord = false;
            }
            result = status == CtaDialogFragment.CTA_REMEMBER_AGREE ? true : false;
            return hasRecord;
        }

        public void showDialog(final Context context,final boolean fromMenu){
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = 30;
            layout.setPadding(padding, padding, padding, padding);

            TextView content = new TextView(context);
            content.setText(context.getResources().getString(R.string.cta_dialog_content));
            content.setTextSize(18);
            content.setTextColor(context.getResources().getColor(R.color.black));
            layout.addView(content);

            final CheckBox checkBox = new CheckBox(context);
            checkBox.setText(context.getResources().getString(R.string.cta_check_title));
            checkBox.setTextSize(15);
            if(getUserPermission(context) != CtaDialogFragment.CTA_NO_RECORD){
                checkBox.setChecked(true);
                //checkBox.setClickable(false);
                // checkBox.setEnabled(false);
            }
            layout.addView(checkBox);

            String title = context.getResources().getString(R.string.cta_dialog_title);
            SpannableString s = new SpannableString(title);
            s.setSpan(new ForegroundColorSpan(Color.BLACK) , 0, s.length(), 0);
            AlertDialog.Builder builder = new AlertDialog.Builder(context, ThemeUtility.getAsusAlertDialogThemeId());
            builder.setView(layout)
                   .setTitle(s)
                   .setCancelable(false)
                   .setPositiveButton(context.getResources().getString(R.string.cta_agree), new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           result = true;
                           if(!fromMenu){
                               synchronized(checkCtaLock){
                                   checkCtaLock.notifyAll();
                               }
                           }
                            setUserPermission(context, checkBox.isChecked() ?
                                    CtaDialogFragment.CTA_REMEMBER_AGREE : CtaDialogFragment.CTA_NO_RECORD);
                       }
                   })
                   .setNegativeButton(context.getResources().getString(R.string.cta_disagree), new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           result = false;
                           if(!fromMenu){
                               synchronized(checkCtaLock){
                                   checkCtaLock.notifyAll();
                               }
                           }
                           setUserPermission(context, checkBox.isChecked() ?
                                   CtaDialogFragment.CTA_REMEMBER_REFUSE : CtaDialogFragment.CTA_NO_RECORD);
                       }
                   });
            final AlertDialog alert = builder.create();
            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    if (checkBox.isChecked()) {
                        alert.getButton(
                                DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                    }
                }
            });
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                    if (isChecked) {
                        alert.getButton(
                                DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                    } else {
                        alert.getButton(
                                DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                    }
                }
            });
            alert.show();
        }

        public boolean getResult(){
            return result;
        }

     }

     public boolean enableCtaCheck(){
         return WrapEnvironment.IS_CN_DEVICE;
     }

     private void setUserPermission(Context context,int value){
         SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
         if(sp != null){
             Editor ed = sp.edit();
             ed.putInt(ACCESS_NETWORK_USER_PERMISSION, value);
             ed.commit();
         }
     }

     private int getUserPermission(Context context){
         int value = CtaDialogFragment.CTA_NO_RECORD;
         SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
         if(sp != null){
             value = sp.getInt(ACCESS_NETWORK_USER_PERMISSION, CtaDialogFragment.CTA_NO_RECORD);
         }
         return value;
     }


     public static int getScreenWidth(Context context)
     {
         WindowManager wm = (WindowManager) context
                 .getSystemService(Context.WINDOW_SERVICE);
         DisplayMetrics outMetrics = new DisplayMetrics();
         wm.getDefaultDisplay().getMetrics(outMetrics);
         return outMetrics.widthPixels;
     }

     public static int getScreenHeight(Context context)
     {
         WindowManager wm = (WindowManager) context
                 .getSystemService(Context.WINDOW_SERVICE);
         DisplayMetrics outMetrics = new DisplayMetrics();
         wm.getDefaultDisplay().getMetrics(outMetrics);
         return outMetrics.heightPixels;
     }

     public void saveOperationPath(VFile file){
        mOpertaionStack.add(file);
     }

     public VFile getLastPath(){
        if(!mOpertaionStack.isEmpty()){
            return mOpertaionStack.pop();
        }
        return null;
     }

     public void clearOperationStack(){
        mOpertaionStack.clear();
     }

     public void backKeyPressed(boolean press){
         isFromBackPressed = press;
     }

     public boolean isFromBackPress(){
         return isFromBackPressed;
     }




}
