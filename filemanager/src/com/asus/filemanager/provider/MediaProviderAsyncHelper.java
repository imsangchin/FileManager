
package com.asus.filemanager.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.utility.CharacterParser;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.MediaScannerHelper;
import com.asus.filemanager.utility.SortUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaProviderAsyncHelper {

    private static final int MSG_RENAME = 0;
    private static final int MSG_NEWFOLDER = 1;
    private static final int MSG_DELETE = 2;
    private static final int MSG_ADD_FILE = 3;

    public static final boolean DEBUG = ConstantsUtil.DEBUG;

    public static final String TAG = MediaProviderAsyncHelper.class.getSimpleName();

    private static Context sContext;
    private static MediaProviderAsyncHelper sInstance;
    private static WorkerHandler sThreadHandler;

    private static Uri mUri = MediaStore.Files.getContentUri("external");
    private static Uri mImagesUri = MediaStore.Images.Media.getContentUri("external");
    private static Uri mAudioUri = MediaStore.Audio.Media.getContentUri("external");
    private static Uri mVideoUri = MediaStore.Video.Media.getContentUri("external");

    private static String mSortQueryStr = MediaStore.MediaColumns.DISPLAY_NAME;

    public static void setSortType(int sortType){

        if (sortType == SortUtility.SortType.SORT_DATE_DOWN){
            mSortQueryStr = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";
        }else if (sortType == SortUtility.SortType.SORT_DATE_UP){
            mSortQueryStr = MediaStore.MediaColumns.DATE_MODIFIED + " ASC";
        }else if (sortType == SortUtility.SortType.SORT_NAME_DOWN){
            mSortQueryStr = MediaStore.MediaColumns.DISPLAY_NAME + "  COLLATE LOCALIZED ASC";
        }else if (sortType == SortUtility.SortType.SORT_NAME_UP){
            mSortQueryStr = MediaStore.MediaColumns.DISPLAY_NAME + "  COLLATE LOCALIZED DESC";
        }else if (sortType == SortUtility.SortType.SORT_SIZE_DOWN){
            mSortQueryStr = MediaStore.MediaColumns.SIZE + " ASC";
        }else if (sortType == SortUtility.SortType.SORT_SIZE_UP){
            mSortQueryStr = MediaStore.MediaColumns.SIZE + " DESC";
        }
    }
    public static String getSortStr(){
        return mSortQueryStr;
    }

    private static String[] ALL_projection = new String[] {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.PARENT,
            MediaStore.Files.FileColumns.SIZE,
    };

    private static String[] FILE_projection = new String[] {
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.TITLE,
    };

    private static String[] Image_Album_projection = new String[] {
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        String.format("substr(%s, length(%s)-length(%s), 1) as filename_prevchar",
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        ),
        "COUNT(1) AS count",
    };


    private class WorkerHandler extends Handler {

        private String[] tree_projection = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.PARENT,
                MediaStore.Files.FileColumns.DATA
        };

        private static final int FORMAT_ASSOCIATION = MtpConstants.FORMAT_ASSOCIATION;
        private static final String Files_FileColumns_FORMAT = "format";

        private WorkerArgs args;
        private ContentResolver mCr;

        public WorkerHandler(Looper looper) {
            super(looper);
            mCr = sContext.getContentResolver();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RENAME:
                    args = (WorkerArgs) msg.obj;
                    synchronized (args.locker) {
                        doRename(args.oldFile, args.newFile);
                        args.actionComplete.set(true);
                        args.locker.notify();
                    }
                    break;
                case MSG_NEWFOLDER:
                    args = (WorkerArgs) msg.obj;
                    //only one file
                    doAddFile(args.newFile, args.subTree);
                    break;
                case MSG_DELETE:
                    args = (WorkerArgs) msg.obj;
                    synchronized (args.locker) {
                        doDeleteFile(args.oldFile, false);
                        args.actionComplete.set(true);
                        args.locker.notify();
                    }
                    break;
                case MSG_ADD_FILE:
                    args = (WorkerArgs) msg.obj;
                    synchronized (args.locker) {
                        doAddFile(args.newFile, false);
                        args.actionComplete.set(true);
                        args.locker.notify();
                    }
                default:
                    break;
            }
        }


        private void doRename(VFile oldFile, VFile newFile) {
            if (newFile.isDirectory()) {
                String path = null;
                try {
                    path = FileUtility.getCanonicalPath(oldFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int numDelete = sContext.getContentResolver().delete(
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.Files.FileColumns.DATA + " LIKE ? or "
                                + MediaStore.Files.FileColumns.DATA + " LIKE ?",
                        new String[] {path /* it self */, path + "/%" /* its child */}
                    );

                if (DEBUG) {
                    Log.d(TAG, "RenameFolder: " + path + ", delete " + numDelete + " rows");
                }
            } else {
                doDeleteFile(oldFile,newFile.isDirectory());
            }

            doAddFile(newFile, newFile.isDirectory());
        }

        private void doAddFile(VFile file,boolean subTree) {
            if (!file.exists())
                return;
            try {
                addFile(file,subTree);
            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "doAddFile Exception : " + file);
            }
        }

        private void addFile(VFile file, boolean subTree) throws IOException {
            if (file.isDirectory()) {
                if (DEBUG)
                    Log.d(TAG, "AddFolder: " + FileUtility.getCanonicalPath(file));
                if (FileUtility.isPathInScanDirectories(file) ||
                    FileUtility.isPathInStorageVolume(sContext, file)) {
                    addFolder(file);
                }
                if (subTree) {
                    File[] fa = file.listFiles();
                    for (int i = 0; fa != null && i < fa.length; i++)
                        addFile(new VFile(fa[i]), true);
                }
            } else {
                if (DEBUG)
                    Log.d(TAG, "AddFile: " + FileUtility.getCanonicalPath(file));
                String path = FileUtility.getCanonicalPath(file);
                scanFilesAwait(new String[] {path}, false);
            }
        }

        private void addFolder(VFile file) throws IOException {
            if (!file.isDirectory()) {
                return;
            }

            String path = FileUtility.getCanonicalPath(file);

            MediaScannerConnection.scanFile(sContext, new String[] {path}, null,
                    new OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    if (DEBUG) {
                        Log.i(TAG, "onScanCompleted: " + path);
                    }

                    // when we trigger MediaScanner to scan an empty folder
                    // it's format column in media store may set as MtpConstants.FORMAT_UNDEFINED.
                    // we need to update the format column to MtpConstants.FORMAT_ASSOCIATION manually.
                    ContentValues values = new ContentValues();
                    values.put(Files_FileColumns_FORMAT, FORMAT_ASSOCIATION);

                    int updateCount = mCr.update(uri, values, null, null);
                    Log.i(TAG, "Num of update FORMAT_ASSOCIATION: " + updateCount);
                }
            });
        }

        private void doDeleteFile(VFile oldFile,boolean subTree) {
            /*int id = 0;
            try {
                id = getFileID(oldFile);
            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "GetFileID Exception");
            }
            if (id != 0)
                deleteFileID(id, subTree, oldFile);*/

            VFile[] listFiles;

            if (subTree) {
                // TODO: FIX ME
                // listVFiles always return null because FileManager always trigger this function
                // after physical file is deleted.
                listFiles = oldFile.listVFiles();
            } else {
                listFiles = null;
            }

            if (listFiles != null) {
               doDeleteFile(oldFile, subTree);
            } else {

                String path = null;

                try {
                    path = FileUtility.getCanonicalPath(oldFile);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // using content resolver to delete file is faster than scan file by MediaScanner
                scanFilesAwait(new String[] {path}, true);
            }

            if(oldFile != null && oldFile.isDirectory())
                ProviderUtility.FavoriteFiles.removeFile(mCr, oldFile);
        }

        private void scanFilesAwait(String[] path, boolean useContentResolverDelete) {

            final AtomicBoolean scanComplete = new AtomicBoolean();
            final Object locker = new Object();

            if (path != null) {
                if (DEBUG) {
                    Log.d(TAG, "scan file await: " + path[0]);
                }

                if (useContentResolverDelete) {
                    if (DEBUG) {
                        Log.v(TAG, "contentresolver delete");
                    }
                    // FIXME: samsung platform media scanner bug:
                    // MediaScannerConnection.scanFile would NOT delete data
                    // from mediastore file table on samsung devices(s4, s5).
                    // But Nexus5 will delete it.
                    sContext.getContentResolver().delete(
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.Files.FileColumns.DATA + "=?",
                        path
                    );
                } else {
                    MediaScannerConnection.scanFile(sContext, path
                            , null, new OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            synchronized (locker) {

                                if (DEBUG) {
                                    Log.i(TAG, "onScanCompleted: " + path);
                                }

                                scanComplete.set(true);
                                locker.notify();
                            }
                        }
                    });
                    synchronized (locker) {
                        if (!scanComplete.get()) {
                            try {
                                locker.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        private boolean deleteFileID(int id, boolean subTree, VFile oldFile) {
            if (DEBUG) {
                Log.d(TAG, "delete FileId isDir : " + subTree);
            }
            if (subTree) {
                Cursor c = null;
                c = mCr.query(mUri, tree_projection, "parent=\'" + Integer.toString(id) + "\'", null, null);
                if (DEBUG) {
                    Log.d(TAG, "course count : " + c.getCount());
                }
                try {
                    if (c != null && c.moveToFirst()) {
                        do {
                            String path = c.getString(2);
                            if (DEBUG) {
                                Log.d(TAG, "delete Media provider : " + path);
                            }
                            deleteFileID(c.getInt(0), true, new VFile(path));
                        } while (c.moveToNext());
                    }
                } finally {
                    if (c != null)
                        c.close();
                }
            }

            Uri u;
            try {
                u = Uri.parse("file://" + FileUtility.getCanonicalPath(oldFile));
                sContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, u));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int result = 1;//mCr.delete(mUri, "_id=\'" + Integer.toString(id) + "\'", null);
            if (DEBUG)
                Log.d(TAG, "DeleteFileID: " + Integer.toString(id) + ", r = " + Integer.toString(result));
            if (result == 1)
                return true;
            else
                return false;
        }

    }

    public static void Init(Context context) {
        sContext = context;
    }

    private MediaProviderAsyncHelper() {
        HandlerThread thread = new HandlerThread("MediaProviderAsyncHelper");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
    }

    public static void rename(VFile file, VFile renameFile, boolean isNeedToWaitMediaScanner) {
        if (file == null || renameFile == null)
            return;

        if (sInstance == null) {
            sInstance = new MediaProviderAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.oldFile = file;
        args.newFile = renameFile;
        args.locker = new Object();

        if (isNeedToWaitMediaScanner) {
            args.actionComplete = new AtomicBoolean(false);
        } else {
            args.actionComplete = new AtomicBoolean(true);
        }

        Message msg = sThreadHandler.obtainMessage(MSG_RENAME);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);

        awaitActionComplete(args.actionComplete, args.locker);
    }

    public static void addFolder(VFile newFolder) {
        addFolder(newFolder, false);
    }

    public static void addFolder(VFile newFolder,boolean subTree) {
        if(newFolder == null)
            return;

        if (sInstance == null) {
            sInstance = new MediaProviderAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.newFile = newFolder;
        args.subTree = true;

        Message msg = sThreadHandler.obtainMessage(MSG_NEWFOLDER);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);

    }

    public static void deleteFile(VFile file) {
        deleteFile(file, false);
    }

    public static void deleteFile(VFile file, boolean isNeedToWaitMediaScanner) {
        if(file == null)
            return ;

        if (sInstance == null) {
            sInstance = new MediaProviderAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.oldFile = file;
        args.locker = new Object();

        if (isNeedToWaitMediaScanner) {
            args.actionComplete = new AtomicBoolean(false);
        } else {
            args.actionComplete = new AtomicBoolean(true);
        }

        Message msg = sThreadHandler.obtainMessage(MSG_DELETE);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);

        awaitActionComplete(args.actionComplete, args.locker);
    }

    public static void addFile(VFile file, boolean isNeedToWaitMediaScanner) {
        if(file == null)
            return ;

        if (sInstance == null) {
            sInstance = new MediaProviderAsyncHelper();
        }

        WorkerArgs args = new WorkerArgs();
        args.newFile = file;
        args.locker = new Object();

        if (isNeedToWaitMediaScanner) {
            args.actionComplete = new AtomicBoolean(false);
        } else {
            args.actionComplete = new AtomicBoolean(true);
        }

        Message msg = sThreadHandler.obtainMessage(MSG_ADD_FILE);
        msg.obj = args;

        sThreadHandler.sendMessage(msg);

        awaitActionComplete(args.actionComplete, args.locker);
    }

    private static void awaitActionComplete(AtomicBoolean actionComplete, Object locker) {
        if (actionComplete.get()) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            synchronized (locker) {
                try {
                    if (!actionComplete.get()) {
                        locker.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static int getFileID(File f) throws IOException {
        int result = 0;

        ContentResolver mCr = sContext.getContentResolver();

        String pathindb = FileUtility.getCanonicalPath(f);
        String selection = "_data=?";

        Cursor c = null;
        if (pathindb != null) {

            c = mCr.query(mUri, ALL_projection, selection, new String[] {pathindb}, null);
            if (c != null && c.moveToFirst() && c.getCount() == 1) {

                String p = c.getString(1);
                result = c.getInt(0);

                if (DEBUG) {
                    Log.d(TAG, "=====================");
                    Log.d(TAG, c.getString(0) == null ? "" : c.getString(0));
                    Log.d(TAG, "PATH: " + (p == null ? "" : p));
                    Log.d(TAG, "NAME: " + (c.getString(2) == null ? "" : c.getString(2)));
                    Log.d(TAG, "TITLE: " + (c.getString(3) == null ? "" : c.getString(3)));
                    Log.d(TAG, c.getString(4) == null ? "" : c.getString(4));
                    Log.d(TAG, "=====================");
                }
            }
            if (c != null)
                c.close();
        }

        if (DEBUG)
            Log.d(TAG, "getFileID r = " + Integer.toString(result) + ", " + FileUtility.getCanonicalPath(f));
        return result;
    }

    /*public static String getDirsContainExtension(File dir,String extname) throws Exception {
        StringBuffer sb = new StringBuffer();

        String path = FileUtility.getCanonicalPath(dir);
        if(path.equals("/Removable"))
            path = File.separator;
        if(!path.endsWith(File.separator))
            path += File.separator;

        ContentResolver mCr = sContext.getContentResolver();
        //dir format=0x3001
        //String selection = "format=12288 and _data like \'"+path+"%"+File.separator+"%"+extname+"\'";
        String selection = "format<>? and _data like \'"+path+"%"+File.separator+"%"+extname+"\'";

        Cursor cursor = mCr.query(mUri, ALL_projection, selection, new String[] {Integer.toString(WorkerHandler.FORMAT_ASSOCIATION)}, null);
        if(cursor != null){
            String data;
            while(cursor.moveToNext()){
                data = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                //int index = data.indexOf(File.separatorChar,path.length());
                //data = data.substring(path.length()-1, index);
                if(data!=null && sb.indexOf(data) == -1)
                    sb.append(File.separator + data);
             }
            cursor.close();
        }

        if(sb.indexOf("//Removable")==-1 && (sb.indexOf("//storage/MicroSD")!=-1
                || sb.indexOf("//storage/sdcard1")!=-1
                || sb.indexOf("//storage/USBdisk1")!=-1
                || sb.indexOf("//storage/USBdisk2")!=-1
                )){
            sb.append("//Removable");
        }

        return sb.toString();
    }*/

    public static String getPathsContainExtension(String extname) throws Exception {
        StringBuffer sb = new StringBuffer();
        String selection = "format<>? and _data like \'%"+extname+"\'";

        ContentResolver mCr = sContext.getContentResolver();
        Cursor cursor = mCr.query(mUri, ALL_projection, selection, new String[] {Integer.toString(WorkerHandler.FORMAT_ASSOCIATION)}, null);
        if(cursor != null){
            String data;
            while(cursor.moveToNext()){
                data = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                if(data!=null && sb.indexOf(data) == -1)
                    sb.append(File.separator + data);
             }
            cursor.close();
        }

        if(sb.indexOf("//Removable")==-1 && (sb.indexOf("//storage/MicroSD")!=-1
                || sb.indexOf("//storage/sdcard1")!=-1
                || sb.indexOf("//storage/USBdisk1")!=-1
                || sb.indexOf("//storage/USBdisk2")!=-1
                )){
            sb.append("//Removable");
        }

        return sb.toString();
    }

    public static ArrayList<VFile> queryLocalDb(Context cotext,String key){

        ArrayList<VFile> resultList = new ArrayList<VFile>();

        ContentResolver cr =  cotext.getContentResolver();
        String selection = "(" + MediaStore.Files.FileColumns.TITLE + " like " + "'%" + key + "%'" +  ") or ("
                   + MediaStore.Files.FileColumns.DISPLAY_NAME + " like " + "'%" + key + "%'" + ") or ("
                         + MediaStore.Files.FileColumns.DATA + " like " + "'%" + key + "')";
        Cursor mCursor = cr.query(mUri, FILE_projection, selection, null, null);
        if(mCursor != null){
            while(mCursor.moveToNext()){
                String filePath = mCursor.getString(mCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                if(!TextUtils.isEmpty(filePath)){
                    VFile file = new VFile(filePath);
                    resultList.add(file);
                }
            }

            mCursor.close();
        }

        return resultList;
    }

    private void addFile2MediaStore(File file){
        try {
            ContentValues mNewFileValues = new ContentValues();
            VFile mFile = new VFile(file);
            String file_title = mFile.getName();
            String mimeType = reflectionApis.mediaFile_getMimeTypeForFile(file_title);
            String extName = mFile.getExtensiontName();

            String display_name = file_title;
            String path = FileUtility.getCanonicalPath(file);
            int index = file_title.indexOf(extName) - 1;
            if(index > 0){
                display_name =  file_title.substring(0, index);
            }

            mNewFileValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, display_name);
            mNewFileValues.put(MediaStore.Files.FileColumns.TITLE, file_title);
            mNewFileValues.put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType);
            mNewFileValues.put(MediaStore.Files.FileColumns.DATA, path);
            mNewFileValues.put(MediaStore.Audio.AudioColumns.IS_MUSIC, FileUtility.isMusic(file));

            ContentResolver mCr = sContext.getContentResolver();
            if (mCr.update(mUri, mNewFileValues, MediaStore.Files.FileColumns.DATA + "=?", new String[] {path}) == 0) {
                mCr.insert(mUri, mNewFileValues);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static ArrayList<LocalVFile> queryFiles(Context context, Uri uri, String[] projection,
                  String selection, String[] selectionArgs, String sortOrder, boolean bShowHidden, boolean ignoreFileExistCheck){
        ArrayList<LocalVFile> files = new ArrayList<LocalVFile>();
//实例化汉字转拼音类
        CharacterParser characterParser = CharacterParser.getInstance();
        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(uri, projection, selection, selectionArgs, sortOrder);
        if(cursor != null) {
            if (cursor.moveToFirst()){
                String data;
                int columnIndex = -1;
                do {
                    // Don't use FileUtility.changeToSdcardPath, performance issue!
                    // data = FileUtility.changeToSdcardPath(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    data = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));

                    LocalVFile file = null;
                    if (null != data)
                        file = new LocalVFile(data);
                    if (null != file) {
                        if(ignoreFileExistCheck || file.exists()) {
                            columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                            if (columnIndex != -1) {
                                file.setChildCount((int)(cursor.getLong(columnIndex)/1000));
                            }
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.setLength(0);
                            for( char c : file.getName().toCharArray()) {
                                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
                                if (pinyinArray != null) {
                                    for (String str : pinyinArray) {
                                        stringBuilder.append(str);
                                    }
                                } else {
                                    stringBuilder.append(c);
                                }
                            }
                            String pinyin = stringBuilder.toString().toUpperCase();
                            file.setPinyin(pinyin);
                            Log.i("FileManager", pinyin);
                            String sortString = pinyin.substring(0, 1);
                            if (sortString.matches("[A-Z]")) {
                                file.setSortLetters(sortString);
                            } else {
                                file.setSortLetters("#");
                            }
                            if (!bShowHidden) {
                                if (!file.isHidden())
                                    files.add(file);
                            } else if (!FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(file)) {
                                files.add(file);
                            }
                        } else {
                            // Fix TT-685863: category count is different to category content
                            deleteFileFromMediaStore(new String[] { file.getPath() }, true);
                        }
                    }
                } while (cursor.moveToNext());
            } // end of cursor.moveToFirst()
            cursor.close();
        } // end of cursor != null
        return files;
    }

    private static boolean isSamsungManufacturer() {
        return "samsung".equals(android.os.Build.MANUFACTURER);
    }

    private static void deleteFileFromMediaStore(String[] path, boolean useContentResolverDelete) {
        if (useContentResolverDelete) {
            // FIXME: samsung platform media scanner bug:
            // MediaScannerConnection.scanFile would NOT delete data
            // from mediastore file table on samsung devices(s4, s5).
            // But Nexus5 will delete it.
            sContext.getContentResolver().delete(
                MediaStore.Files.getContentUri("external"),
                MediaStore.Files.FileColumns.DATA + "=?",
                path
            );
        } else {
            MediaScannerConnection.scanFile(sContext, path, null, null);
        }
    }

    public static ArrayList<LocalVFile> getFilesByExtension(Context context, String[] ext, boolean bShowHidden){
        String selection = "";
        for (int i = 0; i < ext.length; i++) {
            if (i != 0) {
                selection = selection + " or ";
            }
            selection = selection + "(_data like \'%." + ext[i] + "\')";
        }
        selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        return queryFiles(context, mUri, ALL_projection, selection, null, null, bShowHidden, false);
    }

    public static int getFilesCountByExtension(Context context, String[] ext, boolean bShowHidden){
        ArrayList<LocalVFile> Files = getFilesByExtension(context, ext,bShowHidden);
        return Files.size();
    }

    public static ArrayList<LocalVFile> getFilesByMimeType(Context context, String[] mimeType, boolean bShowHidden, boolean containsFolder) {
        String selection = "mime_type=?";
        for (int i = 1; i < mimeType.length; i++) {
            selection = selection + " OR mime_type=?";
        }
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }
        return queryFiles(context, mUri, ALL_projection, selection, mimeType, null, bShowHidden, false);
    }

    public static ArrayList<LocalVFile> getFilesByMimeTypeAndExtName(Context context, String[] mimeType, String[] extName, boolean bShowHidden, boolean containsFolder) {
        String selection = "";
        if (mimeType != null) {
            for (int i = 0; i < mimeType.length; i++) {
                selection += (selection.isEmpty() ? "" : " or ") + "mime_type=\'" + mimeType[i] + "\'";
            }
        }
        if (extName != null) {
            for (int i = 0; i < extName.length; i++) {
                selection += (selection.isEmpty() ? "" : " or ") + "(_data like \'%." + extName[i] + "\')";
            }
        }
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }
        return queryFiles(context, mUri, ALL_projection, selection, null, null, bShowHidden, false);
    }

    public static int getFilesCountByMimeTypeAndExtName(Context context, String[] mimeType, String[] extName, boolean bShowHidden, boolean containsFolder) {
        ArrayList<LocalVFile> files = getFilesByMimeTypeAndExtName(context, mimeType, extName, bShowHidden, containsFolder);
        return files.size();
    }

    public static int getFilesCountByMimeType(Context context, String[] mimeType, boolean bShowHidden, boolean containsFolder) {
        ArrayList<LocalVFile> files = getFilesByMimeType(context, mimeType, bShowHidden, containsFolder);
        return files.size();
    }

    public static ArrayList<LocalVFile> getFilesByTime(Context context, long time, boolean bShowHidden, boolean containsFolder) {
        String selection = MediaStore.Files.FileColumns.DATE_MODIFIED + " > " + time;
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }

        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
        sortOrder += (" LIMIT " + 100);

        ArrayList<LocalVFile> list = queryFiles(context, mUri, ALL_projection, selection, null, sortOrder, bShowHidden, false);
        removeInvalidFiles(list);

        return list;
    }

    public static ArrayList<LocalVFile> getFilesByTimeAndSize(
        Context context, long time, boolean bShowHidden, boolean containsFolder, long size) {
        String selection = MediaStore.Files.FileColumns.DATE_ADDED + " > " + time +" AND "
            + MediaStore.Files.FileColumns.SIZE + " > " + size;
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }

        String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";
        sortOrder += (" LIMIT " + 100);

        ArrayList<LocalVFile> list = queryFiles(context, mUri, ALL_projection, selection, null, sortOrder, bShowHidden, false);
        removeInvalidFiles(list);

        return list;
    }

    public static long countTtlFilesSizeByTime(
        Context context, long time, boolean bShowHidden, boolean containsFolder) {
        String selection = MediaStore.Files.FileColumns.DATE_ADDED + " > " + time;
        if  (!bShowHidden){
            selection = "(" + selection + ") AND " + MediaStore.Files.FileColumns.TITLE + " NOT like '.%'";
        }
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }

        ContentResolver mCr = context.getContentResolver();
        String[] columns = new String[] { "sum(" + MediaStore.Files.FileColumns.SIZE+ ")" };
        Cursor cursor = mCr.query(mUri, columns, selection, null, null);

        long size = 0;
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                size = cursor.getLong(0);
            }
            cursor.close();
        }

        return size;
    }
    public static int getFilesCountByTime(Context context, long time, boolean bShowHidden, boolean containsFolder) {
        ArrayList<LocalVFile> files = getFilesByTime(context, time, bShowHidden, containsFolder);
        return files.size();
    }

    public static ArrayList<LocalVFile> getFilesBySize(Context context, long size, boolean bShowHidden, boolean containsFolder) {
        String selection = MediaStore.Files.FileColumns.SIZE + " > " + size;
        if (!containsFolder) {
            selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        }
        return queryFiles(context, mUri, ALL_projection, selection, null, null, bShowHidden, false);
    }

    public static int getFilesCountBySize(Context context, long size, boolean bShowHidden, boolean containsFolder) {
        ArrayList<LocalVFile> files = getFilesBySize(context, size, bShowHidden, containsFolder);
        return files.size();
    }

    private static void removeInvalidFiles(ArrayList<LocalVFile> list) {

        if (list == null) {
            Log.w(TAG, "do not remove invalid files since list == null");
            return;
        }

        Iterator<LocalVFile> it = list.iterator();

        String mediaFile_mime = null;
        String mimeUtils_mime = null;

        LocalVFile localVFile = null;

        while (it.hasNext()) {
              localVFile = it.next();

              mediaFile_mime = reflectionApis
                      .mediaFile_getMimeTypeForFile(localVFile.getName());

              mimeUtils_mime = MimeTypeMap.getSingleton()
                      .getMimeTypeFromExtension(localVFile.getExtensiontName().toLowerCase());

              if (mediaFile_mime == null && mimeUtils_mime == null) {
                  // Remove the files in the list which cannot get its MimeType
                  it.remove();
              }
        }
    }

    /*public static ArrayList<LocalVFile> getImageAlbums(Context context){
        ArrayList<LocalVFile> albums = new ArrayList<LocalVFile>();

        String selection = "1=1) group by (" + MediaStore.Images.Media.BUCKET_ID;
        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;

        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(mImagesUri, Image_Album_projection, selection, null, sortOrder);
        if(cursor != null){
            while(cursor.moveToNext()){
                LocalVFile file = new LocalVFile(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));

                LocalVFile albumFile = (LocalVFile) file.getParentFile();
                if(albumFile.exists()){
                    albumFile.setBucketId(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)));
                    albumFile.setImageCount(cursor.getInt(cursor.getColumnIndex("count")));
                    albums.add(albumFile);
                }

             }
            cursor.close();
        }

        return albums;
    }

    public static ArrayList<LocalVFile> getImageFilesByBucketId(Context context, String bucket_id){
        String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME;

        return queryFiles(context, mImagesUri, FILE_projection, selection, new String[] { bucket_id }, sortOrder);
    }*/

    public static long getImageFilesCount(Context context, boolean isShowHidden) {
        return getFilesCount(context, mImagesUri, isShowHidden);
    }

    public static ArrayList<LocalVFile> getMusicFiles(Context context, boolean isShowHidden){
        String[] Music_projection = new String[] {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TITLE,
        };
        //return queryFiles(context, mAudioUri, FILE_projection, isAudioSelection(), null, null, isShowHidden, false);
        return queryFiles(context, mAudioUri, Music_projection, isAudioSelection(), null, null, isShowHidden, false);
    }

    public static int getMusicFilesCount(Context context, boolean isShowHidden){
        return getMusicFiles(context, isShowHidden).size();
    }

    public static ArrayList<LocalVFile> getVideoFiles(Context context, boolean isShowHidden){
        return queryFiles(context, mVideoUri, FILE_projection, null, null, null, isShowHidden, false);
    }

    public static int getVideoFilesCount(Context context, boolean isShowHidden){
        return getVideoFiles(context, isShowHidden).size();
    }

    public static ArrayList<LocalVFile> getImageAlbums(Context context, boolean isShowHidden){
        return getAlbums(context, mImagesUri, isShowHidden);
    }

    public static ArrayList<LocalVFile> getImageFilesByBucketId(Context context, String bucket_id, int limit){
        return getFilesByBucketId(context, mImagesUri, bucket_id, limit);
    }

    public static LocalVFile getFirstImageFileByBucketId(Context context, String bucket_id){
        return getFirstFileByBucketId(context, mImagesUri, bucket_id, mSortQueryStr);
    }

    public static String isAudioSelection() {
        //return MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
        return MediaStore.Audio.AudioColumns.IS_MUSIC +"=1 or " +
            MediaStore.Audio.AudioColumns.IS_ALARM +"=1 or " +
            MediaStore.Audio.AudioColumns.IS_NOTIFICATION +"=1 or " +
            MediaStore.Audio.AudioColumns.IS_PODCAST +"=1 or " +
            MediaStore.Audio.AudioColumns.IS_RINGTONE +"=1";
    }

    public static ArrayList<LocalVFile> getMusicAlbums(Context context){
        ArrayList<LocalVFile> albums = new ArrayList<LocalVFile>();

        String selection = isAudioSelection() + ") group by (" + MediaStore.Images.Media.BUCKET_ID;
        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;

        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(mUri, Image_Album_projection, selection, null, sortOrder);
        if(cursor != null){
            while(cursor.moveToNext()){
                LocalVFile file = new LocalVFile(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));

                LocalVFile albumFile = (LocalVFile) file.getParentFile();
                if(null != albumFile && albumFile.exists() && !FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(albumFile)){
                    albumFile.setBucketId(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)));
                    albumFile.setRestrictFiles(getMusicFilesByBucketId(context, String.valueOf(
                            cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID))), 0, true));
                    albumFile.setChildCount(cursor.getInt(cursor.getColumnIndex("count")));
                    albums.add(albumFile);
                }

             }
            cursor.close();
        }
        return albums;
    }

    public static ArrayList<LocalVFile> getMusicFilesByBucketId(Context context, String bucket_id, int limit, boolean ignoreFileExitCheck){
        String[] Audio_bucket_projection = new String[] {
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Audio.Media.DURATION,
        };
        String selection = "(" + isAudioSelection() + ") and " + MediaStore.Images.Media.BUCKET_ID + " = ?";
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME;

        if (limit != 0)
            sortOrder += (" LIMIT " + limit);
        //return queryFiles(context, mUri, Audio_bucket_projection, selection, new String[] { bucket_id }, sortOrder, false, ignoreFileExitCheck);
        return queryFiles(context, mUri, Audio_bucket_projection, isAudioSelection(), null, null, false, ignoreFileExitCheck);
    }

    public static LocalVFile getFirstMusicFileByBucketId(Context context, String bucket_id){
        String selection = "(" + isAudioSelection() + ") and " + MediaStore.Images.Media.BUCKET_ID + " = ?";
        String sortOrder = mSortQueryStr;

        sortOrder += (" LIMIT " + 1);

        ArrayList<LocalVFile> queryResults;
        queryResults = queryFiles(context, mUri, FILE_projection, selection, new String[] { bucket_id }, sortOrder, false, false);
        if (queryResults.size() > 0)
            return queryResults.get(0);

        return null;
    }

    public static ArrayList<LocalVFile> getVideoAlbums(Context context, boolean isShowHidden){
        return getAlbums(context, mVideoUri, isShowHidden);
    }

    public static ArrayList<LocalVFile> getVideoFilesByBucketId(Context context, String bucket_id, int limit){
        return getFilesByBucketId(context, mVideoUri, bucket_id, limit);
    }

    public static LocalVFile getFirstVideoFileByBucketId(Context context, String bucket_id){
        return getFirstFileByBucketId(context, mVideoUri, bucket_id, mSortQueryStr);
    }

    private static String[] Count_projection = new String[] {
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DISPLAY_NAME,
        String.format("substr(%s, length(%s)-length(%s), 1) as filename_prevchar",
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        )
    };

    public static long getFilesCount(Context context, Uri uri, boolean isShowHidden){
        // FIXME:
        // if filename_prevchar is '.', _data should be hidden file
        // ex1:
        // _data = '/storage/emulated/0/.123.jpg'
        // _display_name = '123.jpg'
        // filename_prevchar = '.'
        // ex2:
        // _data = '/storage/emulated/0/123.jpg'
        // _display_name = '123.jpg'
        // filename_prevchar = '/'
        String selection = isShowHidden ? FunctionalDirectoryUtility.getInstance().getHiddenDirectorySelection("_data")
                : "filename_prevchar!='.' AND _display_name NOT LIKE '.%' AND " + FunctionalDirectoryUtility.getInstance().getHiddenDirectorySelection("_data");
        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(uri, Count_projection, selection, null, sortOrder);
        long size = 0;
        if(cursor != null){
            if (cursor.moveToFirst()) {
                size = cursor.getCount();
            }
            cursor.close();
        }
        return size;
    }

    public static ArrayList<LocalVFile> getAlbums(Context context, Uri uri, boolean isShowHidden){
        ArrayList<LocalVFile> albums = new ArrayList<LocalVFile>();
        // FIXME:
        // if filename_prevchar is '.', _data should be hidden file
        // ex1:
        // _data = '/storage/emulated/0/.123.jpg'
        // _display_name = '123.jpg'
        // filename_prevchar = '.'
        // ex2:
        // _data = '/storage/emulated/0/123.jpg'
        // _display_name = '123.jpg'
        // filename_prevchar = '/'
        String selection = (isShowHidden ? "1=1)" : "filename_prevchar!='.')") + " group by (" + MediaStore.Images.Media.BUCKET_ID;
        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;

        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(uri, Image_Album_projection, selection, null, sortOrder);
        if(cursor != null){
            while(cursor.moveToNext()){
                LocalVFile file = new LocalVFile(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));

                LocalVFile albumFile = (LocalVFile) file.getParentFile();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.setLength(0);
                for( char c : albumFile.getName().toCharArray()) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
                    if (pinyinArray != null) {
                        for (String str : pinyinArray) {
                            stringBuilder.append(str);
                        }
                    } else {
                        stringBuilder.append(c);
                    }
                }
                String pinyin = stringBuilder.toString().toUpperCase();
                albumFile.setPinyin(pinyin);
                Log.i("FileManager", pinyin);
                String sortString = pinyin.substring(0, 1);
                if (sortString.matches("[A-Z]")) {
                    albumFile.setSortLetters(sortString);
                } else {
                    albumFile.setSortLetters("#");
                }
                if(null != albumFile && albumFile.exists() && !FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(albumFile)){
                    albumFile.setBucketId(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)));
                    if (mVideoUri.equals(uri)) {
                        albumFile.setRestrictFiles(getVideoFilesByBucketId(context, String.valueOf(
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID))), 0));
                    } else if (mImagesUri.equals(uri)) {
                        albumFile.setRestrictFiles(getImageFilesByBucketId(context, String.valueOf(
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID))), 0));
                    }
                    albumFile.setChildCount(cursor.getInt(cursor.getColumnIndex("count")));
                    albums.add(albumFile);
                }

             }
            cursor.close();
        }

        return albums;
    }

    public static ArrayList<LocalVFile> getFilesByBucketId(Context context, Uri uri, String bucket_id, int limit){
        String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME;

        if (limit != 0)
            sortOrder += (" LIMIT " + limit);
        return queryFiles(context, uri, FILE_projection, selection, new String[] { bucket_id }, sortOrder, false, false);
    }

    public static LocalVFile getFirstFileByBucketId(Context context, Uri uri, String bucket_id, String order){
        String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME;

        if (order != null)
            sortOrder = order;

        sortOrder += (" LIMIT " + 1);
        ArrayList<LocalVFile> queryResults;
        queryResults = queryFiles(context, uri, FILE_projection, selection, new String[] { bucket_id }, sortOrder, false, false);
        if (queryResults.size() > 0)
            return queryResults.get(0);
        return null;
    }

    // combine the recently add and recently open
    public static List<LocalVFile> getRecentFiles(Context context, boolean isShowHidden) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -30 /* last 30 days*/);
        long beforeTime = c.getTimeInMillis() / 1000; // in seconds
        long firstLauchTime = ItemOperationUtility.getInstance().getFirstLaunchTime(context, 0);

        List<LocalVFile> recentlyAddFiles = MediaProviderAsyncHelper.getFilesByTime(
            context, Math.max(beforeTime, firstLauchTime), isShowHidden, false
        );
        List<LocalVFile> recentlyOpenFiles = ProviderUtility.RecentlyOpen.getFiles(context.getContentResolver(), isShowHidden);

        // remove the duplicate item from recently add
        List<LocalVFile> recentFiles = new ArrayList<LocalVFile>();
        LocalVFile openFile = null;
        boolean hasDuplicate = false;
        for (LocalVFile addFile: recentlyAddFiles) {
            hasDuplicate = false;
            if (!MediaScannerHelper.isNoMediaPath(addFile.getPath())) {
                for (Iterator<LocalVFile> recentOpenFilesIter = recentlyOpenFiles.iterator(); recentOpenFilesIter.hasNext();) {
                    openFile = recentOpenFilesIter.next();
                    if (addFile.equals(openFile)) {
                        hasDuplicate = true;
                        if (addFile.lastModified() < openFile.lastModified()) {
                            recentFiles.add(openFile);
                            recentOpenFilesIter.remove();
                        } else {
                            recentFiles.add(addFile);
                        }
                    }
                }
                if (!hasDuplicate) {
                    recentFiles.add(addFile);
                }
            }
        }
        recentFiles.addAll(recentlyOpenFiles);
        return recentFiles;
    }
private static String addRootPathSelection(String selection, String rootPath)
    {
        if(selection!=null)
            selection = "(" + selection + ") and ";
        else
            selection = "";

        selection =  selection + "(_data like '"+ rootPath+"%')";
        return selection;
    }

    public static long getImageFilesSizes(Context context,String rootPath) {
        return getFilesSizes(context, mImagesUri, addRootPathSelection(null, rootPath), null);
    }

    public static long getMusicFilesSizes(Context context,String rootPath){
        return getFilesSizes(context, mAudioUri, addRootPathSelection(null, rootPath), null);
    }

    public static long getVideoFilesSizes(Context context,String rootPath){
        return getFilesSizes(context, mVideoUri, addRootPathSelection(null, rootPath), null);
    }

    public static long getFilesSizesByMimeTypeAndExtName(Context context,String[] mimeTypes, String[] extensions,String rootPath){
        String selection = "";
        if (mimeTypes != null) {
            for (int i = 0; i < mimeTypes.length; i++) {
                selection += (selection.isEmpty() ? "" : " or ") + "mime_type=\'" + mimeTypes[i] + "\'";
            }
        }
        if (extensions != null) {
            for (int i = 0; i < extensions.length; i++) {
                selection += (selection.isEmpty() ? "" : " or ") + "(_data like \'%." + extensions[i] + "\')";
            }
        }
        selection = addRootPathSelection(selection, rootPath);

        selection = "(" + selection + ") and format <> " + WorkerHandler.FORMAT_ASSOCIATION;
        return getFilesSizes(context,mUri,selection,null);
    }

    public static long getFilesSizes(Context context, Uri uri, String selection, String[] selectionArgs){

        String[] projection = new String[] { "sum(" + MediaStore.Files.FileColumns.SIZE+ ")" };
        ContentResolver mCr = context.getContentResolver();
        Cursor cursor = mCr.query(uri, projection, selection, selectionArgs, null);

        long size = 0;
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                size = cursor.getLong(0);
            }
            cursor.close();
        }
        return size;
    }
}
