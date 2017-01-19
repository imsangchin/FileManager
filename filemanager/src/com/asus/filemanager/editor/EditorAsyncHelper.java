
package com.asus.filemanager.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FileUtility.FileInfo;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;

public class EditorAsyncHelper {

    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------
    public static final class WorkerArgs {
        public VFile[] files;
        public Handler handler;
        public boolean isDelete;
        public boolean isDraggingItems; // Johnson
        public boolean isInCategory;
        public String pastePath;
        public int targetDataType;
        public VFile pasteVFile; // for remote storage
    }

    private class WorkerHandler extends Handler {

        private WorkerArgs args;

        PowerManager.WakeLock mWaleLock;

        public WorkerHandler(Looper looper) {
            super(looper);
            PowerManager pm = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
            mWaleLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        }

        private Map<String, List<String>> prepareRestrictList(VFile[] vFiles) {

            Map<String, List<String>> map = new HashMap<String, List<String>>();

            if (vFiles == null) {
                return null;
            }

            for (VFile vFile : vFiles) {
                List<String> restrictFileList = new ArrayList<String>();

                if (vFile.getHasRetrictFiles()) {

                    VFile[] subFiles = vFile.listVFiles();

                    if (subFiles != null) {
                        for (VFile subFile :subFiles) {
                            restrictFileList.add(subFile.getPath());
                        }
                    }
                } else {
                    return null;
                }
                map.put(vFile.getPath(), restrictFileList);
            }

            return map;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PASTE_FILE:
                    try {
                        mWaleLock.acquire();
                        Log.d(TAG, "handle PASTE_FILE");
                        args = (WorkerArgs) msg.obj;

                        EditorUtility.sQuit = false;
                        EditorUtility.sOverWrite = false;
                        EditorUtility.sApplyAll = false;
                        EditorUtility.sEditIsProcessing = true;

                        if(args.files==null || args.files.length<1)
                            return;

                        long length = 0;
                        if(args.files!=null&&((args.files)[0]).getVFieType()==VFileType.TYPE_CLOUD_STORAGE ){
                            Log.d("felix_zhang:","copy cloud storage");
                        }else {
                            length  = FileUtility.getArrayTotalLength(args.files);
                            Log.d("felix_zhang:","not copy cloud storage");
                        }
                        //handle samba case and update map for restrict files.
                        int targetDataType = args.targetDataType;
                        int srcDateType = args.files[0].getVFieType() ;
                        if ((targetDataType == VFileType.TYPE_SAMBA_STORAGE && srcDateType != VFileType.TYPE_CLOUD_STORAGE) ||
                                (srcDateType == VFileType.TYPE_SAMBA_STORAGE && targetDataType != VFileType.TYPE_CLOUD_STORAGE)){
                            FileListFragment.SAMBA_MAP_FOR_RESTRICFILES = prepareRestrictList(args.files);
                            SambaFileUtility.getInstance(null).sendSambaMessage(SambaMessageHandle.FILE_PASTE, args.files, args.pastePath, args.isDelete, -1,null);
                        }else{
                            boolean pasteResult = false;
                            if (!EditorUtility.sQuit) {
                                args.handler.sendMessage(args.handler.obtainMessage(FileListFragment.MSG_PASTE_INIT, 0,
                                        0, length));
                            }

                            if (!EditorUtility.sQuit) {
                                pasteResult = EditorUtility.pasteFile(sContext, args.files, args.handler,
                                        args.isDelete, args.isDraggingItems, args.pasteVFile, args.isInCategory);
                            }

                            //if target dst  is local  and source is remote, we finish past dialog right now
                            //fixed felix_zhang
                            if ((targetDataType == VFileType.TYPE_LOCAL_STORAGE ) &&
                                    (srcDateType == VFileType.TYPE_LOCAL_STORAGE ) /*|| (args.files[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE)*/) {
                                //Log.e("felix_zhang","args.targetDataType == VFileType.TYPE_LOCAL_STORAGE");
                                args.handler.sendMessage(args.handler.obtainMessage(FileListFragment.MSG_PASTE_COMPLETE, (pasteResult ? 1 : 0), 0, args));
                            }
                        }

                    } finally {
                        mWaleLock.release();
                    }
                    break;
                case DELETE_FILE:
                case MOVE_FILE_TO_RECYCLEBIN:
                    try {
                        mWaleLock.acquire();
                        Log.d(TAG, "handle DELETE_FILE");
                        args = (WorkerArgs) msg.obj;

                        EditorUtility.sEditIsProcessing = true;
                        boolean deleteResult =
                                msg.what == DELETE_FILE? EditorUtility.deleteFile(sContext, args.files, args.isInCategory)
                                        : EditorUtility.moveFileToRecycleBin(sContext, args.files, args.isInCategory) ;

                        args.handler.sendMessage(args.handler.obtainMessage(FileListFragment.MSG_DELET_COMPLETE, (deleteResult ? 1 : 0), 0, args.files));

                    } finally {
                        mWaleLock.release();
                    }
                    break;
                case RESTORE_FILE:
                    try {
                        mWaleLock.acquire();
                        Log.d(TAG, "handle RESTORE_FILE");
                        args = (WorkerArgs) msg.obj;

                        EditorUtility.sEditIsProcessing = true;
                        boolean deleteResult =
                                EditorUtility.restoreFile(sContext, args.files, args.handler, false);

                        args.handler.sendMessage(args.handler.obtainMessage(FileListFragment.MSG_RESTORE_COMPLETE, (deleteResult ? 1 : 0), 0, args.files));

                    } finally {
                        mWaleLock.release();
                    }
                    break;
                case MOVE_TO_HIDDEN_ZONE:
                    try {
                        mWaleLock.acquire();
                        Log.d(TAG, "handle MOVE_TO_HIDDEN_ZONE");
                        args = (WorkerArgs) msg.obj;

                        EditorUtility.sEditIsProcessing = true;
                        boolean moveResult =
                                EditorUtility.moveToHiddenZone(sContext, args.files, args.isInCategory);

                        args.handler.sendMessage(args.handler.obtainMessage(HiddenZoneUtility.MSG_HIDE_COMPLETE, (moveResult ? 1 : 0), 0, args.files));

                    } finally {
                        mWaleLock.release();
                    }
                    break;
                default:
                    break;
            }
        }

        public void setPasteFileTerminate() {
            EditorUtility.sQuit = true;
        }

        public void setPasteFileOverWrite(boolean isOverWrite, boolean isApplyAll) {
            EditorUtility.sOverWrite = isOverWrite;
            EditorUtility.sApplyAll = isApplyAll;
        }
    }

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------

    private static final String TAG = "EditorAysncHelper";
    private static EditorAsyncHelper sInstance = null;
    private static WorkerHandler sThreadHandler;
    private static FileManagerApplication sContext;

    public static final int PASTE_FILE = 0;
    public static final int DELETE_FILE = 1;
    public static final int MOVE_FILE_TO_RECYCLEBIN = 2;
    public static final int RESTORE_FILE = 3;
    private static final int MOVE_TO_HIDDEN_ZONE = 4;

    private EditorAsyncHelper() {
        HandlerThread thread = new HandlerThread("EditorAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
    }

    public static void Init(FileManagerApplication context) {
        sContext = context;
    }

    public static void deletFile(VFile[] vFiles, Handler handler, boolean isInCategory, boolean permanentlyDelete) {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.files = vFiles;
        args.handler = handler;
        args.isInCategory = isInCategory;

        Message msg = sThreadHandler.obtainMessage(permanentlyDelete?DELETE_FILE : MOVE_FILE_TO_RECYCLEBIN);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);
    }

    public static void moveToHiddenZone(VFile[] vFiles, Handler handler, boolean isInCategory) {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.files = vFiles;
        args.handler = handler;
        args.isInCategory = isInCategory;

        Message msg = sThreadHandler.obtainMessage(MOVE_TO_HIDDEN_ZONE);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);
    }

    public static void restoreFile(VFile[] vFiles, Handler handler) {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.files = vFiles;
        args.handler = handler;

        Message msg = sThreadHandler.obtainMessage(RESTORE_FILE);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);
    }

    public static void pasteFile(EditPool editPool, FileListFragment fileListFragment, Handler handler) {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.files = editPool.getFiles();
        args.isDelete = editPool.getExtraBoolean();
        args.pastePath = editPool.getPastePath();
        args.handler = handler;
        args.isDraggingItems = fileListFragment == null? false : fileListFragment.isDraggingItems(); // Johnson
        args.isInCategory = fileListFragment == null? false : fileListFragment.belongToCategoryFromMediaStore();
        args.targetDataType = editPool.getTargetDataType(); // for remote storage
        args.pasteVFile = editPool.getPasteVFile();

        Message msg = sThreadHandler.obtainMessage(PASTE_FILE);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);
    }

    public static void setPasteFileTerminate() {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }
        sThreadHandler.setPasteFileTerminate();
    }
    public static void setPasteFileOverWrite(boolean isOverWrite, boolean isApplyAll) {
        if (sInstance == null) {
            sInstance = new EditorAsyncHelper();
        }
        sThreadHandler.setPasteFileOverWrite(isOverWrite, isApplyAll);
    }

}
