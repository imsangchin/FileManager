
package com.asus.filemanager.utility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.AnalyzerDupFilesActivity;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.ViewPagerActivity;
import com.asus.filemanager.dialog.OpenTypeDialogFragment;
import com.asus.filemanager.dialog.UnZipDialogFragment.UnZipData;
import com.asus.filemanager.functionaldirectory.DisplayVirtualFile;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility.DirectoryType;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.provider.OpenFileProvider;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.ConstantsUtil.OpenType;
import com.asus.filemanager.utility.SortUtility.SortType;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.filemanager.wrap.WrapMediaScanner;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtility {

    private static final String TAG = FileUtility.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    public static final int MAX_FOLDER_DEPTH = 32;
    public static final int MIN_FOLDER_SIZE = 4096;

    private static int folder_depth = 0;

    public static String EXTERNAL_STORAGE_PATH_FOR_USER;
    public static String LEGACY_EXTERNAL_STORAGE_PATH;
    public static String SCAN_FILE_INFO_NAME = "currentFileInfo";
    public static String SCAN_FILE_ATTACHOP_INFO_NAME = "attachOpFileInfo";
    public static final String FILE_TYPE = "file_type";
    public static final String FILE_PATH = "file_path";
    public static final String FILE_SORTTYPE = "file_sortType";
    public static final String FIRST_STARTUP = "first_startup";
    public static final String FIRST_SDPERMISSION = "first_sdpermission";
    public static boolean IS_EXTERNALSTORAGEEMULATED = true;

    public static final String VERIZON_TIPS_SETTING = "content://com.asus.vzwhelp/tipSettings/com.asus.filemanager";

    static {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {

          /* UserEnvironment env = new UserEnvironment(ActivityManager.getCurrentUser());

           EXTERNAL_STORAGE_PATH_FOR_USER = env.getExternalStorageDirectory().toString();*/
           EXTERNAL_STORAGE_PATH_FOR_USER =Environment.getExternalStorageDirectory().toString();
           LEGACY_EXTERNAL_STORAGE_PATH = reflectionApis.getLegacyExternalStorageDirectory().toString();
        }

        IS_EXTERNALSTORAGEEMULATED = Environment.isExternalStorageEmulated();
    }

    public static class FileInfo {
        public boolean PermissionRead = false;
        public boolean PermissionWrite = false;
        public int numFiles = 0;
        public int numFolders = 0;
        public double numSize = 0;
    }

    public static String bytes2String(Context context, double f, int point) {
        return Formatter.formatFileSize(context, (long) f);
    }

    public static void openFile(Activity activity, VFile file, boolean isAttachOp, boolean isRemoteFile) {
       openFile(activity, file, isAttachOp, isRemoteFile, true, false);
    }

    public static void openFile(Activity activity, VFile file, boolean isAttachOp, boolean isRemoteFile, boolean bPreferPrebuilt) {
        openFile(activity, file, isAttachOp, isRemoteFile, bPreferPrebuilt, false);
     }

    public static void openFile(Activity activity, VFile file, boolean isAttachOp, boolean isRemoteFile, boolean bPreferPrebuilt, boolean isShowSingleFile) {
        if (file == null || activity == null)
            return;
        boolean isFromFilePick = isAttachOp;

        if (!file.exists()){
            ToastUtility.show(activity, R.string.open_fail);
            return;
        }

        if (!isRemoteFile) {
            String fileName = file.getName();
            String filePath = file.getPath();
            if (fileName != null) {
                ContentResolver cr = activity.getContentResolver();
                long currentTimeMillis = System.currentTimeMillis();
                if (0 == ProviderUtility.RecentlyOpen.update(cr, fileName, filePath, currentTimeMillis)) {
                    ProviderUtility.RecentlyOpen.insert(cr, fileName, filePath, currentTimeMillis);
                    if (ProviderUtility.RecentlyOpen.getCount(cr) > FileManagerActivity.SUPPORT_RECENTLY_OPEN_SIZE) {
                        ProviderUtility.RecentlyOpen.deleteMinDateOpened(cr);
                    }
                }
            }
        }

        if(activity instanceof FileManagerActivity){
            isFromFilePick = ((FileManagerActivity)activity).isFromFilePick();
        }

        if (!isFromFilePick) {
            String mediaFile_mime = null;
            String mimeUtils_mime = null;
            boolean sendsuccess = false;
            String extName = file.getExtensiontName().toLowerCase();
            if (!FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(DirectoryType.HiddenZone, file)) {
            //open zip
            if ("zip".equals(extName)) {
                String unZipName = file.getNameNoExtension();
                long unZipSize = 0;
                String encode = activity.getString(R.string.default_encoding);
                String uriString = null;
                UnZipData unZipData = new UnZipData(file, unZipName, unZipSize, encode, uriString);
                if(activity instanceof FileManagerActivity){
                    ((FileManagerActivity)activity).displayDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
                    return;
                }
            }else if ("rar".equals(extName)) {
                String unZipName = file.getNameNoExtension();
                long unZipSize = 0;
                String encode = activity.getString(R.string.default_encoding);
                String uriString = null;
                UnZipData unZipData = new UnZipData(file, unZipName, unZipSize, encode, uriString);
                if(activity instanceof FileManagerActivity){
                    ((FileManagerActivity)activity).displayDialog(DialogType.TYPE_UNRAR_PREVIEW_DIALOG, unZipData);
                    return;
                }
            }

            //++start Felix_Zhang
            if(extName!=null&&(extName.indexOf("sn")!=-1)){
                try {
                    Intent sneIntent = new Intent(Intent.ACTION_VIEW);
                    sneIntent.setDataAndType(Uri.fromFile(file),"*.sn*");
                    activity.startActivity(sneIntent);
                    return;
               } catch (Exception e) {
                    Log.w(TAG, "sne file : " + e.getMessage() + " can't be handled");
               }
           }
            }
               //++end Felix_Zhang

            if (isDRM(file)){
                DrmManagerClient mDrmManager;
                mDrmManager = new DrmManagerClient(activity);
                mediaFile_mime = mDrmManager.getOriginalMimeType(file.getAbsolutePath());
                Log.i(TAG, "mediaFile is a drm file, and original mime type = " + mediaFile_mime);
                if (TextUtils.isEmpty(mediaFile_mime)){
                    mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
                }
            }else{
                mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
            }

            if ("video/mp4".equals(mediaFile_mime) || "video/3gpp".equals(mediaFile_mime)) {
                mediaFile_mime = checkMimeType(activity.getApplicationContext(), mediaFile_mime, file);
            }

            mimeUtils_mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extName);

            if ((mediaFile_mime == null && mimeUtils_mime == null)) {
                if(activity instanceof FileManagerActivity){
                    ((FileManagerActivity)activity).displayDialog(DialogType.TYPE_OPEN_TYPE_DIALOG, file);
                }else if(activity instanceof AnalyzerDupFilesActivity){
                    OpenTypeDialogFragment openTypeDialog = OpenTypeDialogFragment.newInstance(file);
                    openTypeDialog.show(activity.getFragmentManager(), "OpenTypeDialogFragment");
                }else{
                    ToastUtility.show(activity, R.string.open_fail);
                }
                return;
            }

            Uri u = null;
            u = getUri(activity.getApplicationContext(), file, mediaFile_mime, true);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            // Send different request codes for opening file between general case and unzip preview
            int requestCode = FileManagerActivity.FILE_MANAGER_NORMAL;
            if (file.getParent().equals(activity.getExternalCacheDir() + "/.pfile")) {
                requestCode = FileManagerActivity.FILE_MANAGER_UNZIP_PREVIEW;
            }

            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (!sendsuccess && mediaFile_mime != null) {
                try {
                    if (mediaFile_mime.startsWith("image")  && !isRemoteFile && bPreferPrebuilt &&
                        //TT- filter gif file since we don't support it
                        !mediaFile_mime.endsWith("gif")) {
                        if (isShowSingleFile || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            u = Uri.fromFile(file);
                        }
                        intent.setClassName("com.asus.filemanager", "com.asus.filemanager.activity.ViewPagerActivity");
                        intent.putExtra(ViewPagerActivity.KEY_IS_SHOW_SINGLE_FILE, isShowSingleFile);
                    }else if(mediaFile_mime.startsWith("video")){
                        Log.d(TAG,"is video file: "+  mediaFile_mime);
                        mediaFile_mime = "video/*";
                        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    }else{
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    }

                    Log.i(TAG, "mediaFile_mime open file *** u: " + u.getScheme()+"://"+u.getHost()+" *** mediaFile_mime:"+mediaFile_mime);

                    intent.setDataAndType(u, mediaFile_mime);
                    if (FileManagerActivity.FILE_MANAGER_NORMAL != requestCode)
                        activity.startActivityForResult(intent, requestCode);
                    else
                        activity.startActivity(intent);

                    sendsuccess = true;
                } catch (Exception e) {
                    //open epub as zip
                    if ("epub".equals(extName)) {
                        String unZipName = file.getNameNoExtension();
                        long unZipSize = 0;
                        String encode = activity.getString(R.string.default_encoding);
                        String uriString = null;
                        UnZipData unZipData = new UnZipData(file, unZipName, unZipSize, encode, uriString);
                        if(activity instanceof FileManagerActivity){
                            ((FileManagerActivity)activity).displayDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
                            return;
                        }
                    }

                    sendsuccess = false;
                    Log.w(TAG, "mediaFile_mime : " + mediaFile_mime + " can't be handled");
                }
            }

            if (!sendsuccess && mimeUtils_mime != null) {
                try {
                    if(mimeUtils_mime.startsWith("video")){
                        Log.d(TAG,"is video file: "+  mimeUtils_mime);
                        mimeUtils_mime = "video/*";
                        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
                    }
                    else if(mimeUtils_mime.equals("application/vnd.android.package-archive") &&
                            ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) || (file instanceof DisplayVirtualFile)))
                    {
                        /**
                         * 1.PackageManager only support file uri.
                         * 2.file need copy to internal storage, because PackageManager can only open internal storage file
                         */
                        if(!FileUtility.getCanonicalPath(file).startsWith(Environment.getExternalStorageDirectory().getCanonicalPath())) {
                            //file not in internal storage
                            VFile tmpFile = new VFile(activity.getApplicationContext().getExternalCacheDir(), file.getName());
                            if (!tmpFile.exists()) {
                                //tmp file not in internal, start CopyAndOpenFileTask
                                new CopyAndOpenFileTask(activity,file,tmpFile,isAttachOp,isRemoteFile,bPreferPrebuilt,isShowSingleFile).execute();
                                return;
                            }
                            //file exist, set file to tmp file
                            file = tmpFile;
                        }
                        if (file instanceof DisplayVirtualFile)
                            file = ((DisplayVirtualFile) file).getActualFile();
                        u = Uri.fromFile(file);
                    }

                    Log.i(TAG, "mimeUtils_mime open file *** u: " + u.getScheme()+"://"+u.getHost()+" *** mimeUtils_mime:"+mimeUtils_mime);

                    intent.setDataAndType(u, mimeUtils_mime);
                    if (FileManagerActivity.FILE_MANAGER_NORMAL != requestCode)
                        activity.startActivityForResult(intent, requestCode);
                    else
                        activity.startActivity(intent);
                    sendsuccess = true;
                } catch (Exception e) {
                    sendsuccess = false;
                    Log.w(TAG, "mimeUtils_mime : " + mimeUtils_mime + " can't be handled");
                }
            }

            if (!sendsuccess) {
                if(activity instanceof FileManagerActivity){
                    ((FileManagerActivity)activity).setFromOpenFileFail(true);
                }
                ToastUtility.show(activity, R.string.open_fail);
            }
        } else {
            String extName = file.getExtensiontName().toLowerCase();
            //get the media provider uri, if not exist : (version < android M 'file uri') (version >= android M 'OpenfileProvider uri')
            Uri u = getUri(activity.getApplicationContext(), file, reflectionApis.mediaFile_getMimeTypeForFile(file.getName()), false);
            //sn file using file uri
            if(extName!=null&&(extName.indexOf("sn")!=-1)){
                u = Uri.fromFile(file);
            }

            Log.i(TAG, "FileProvider open file *** u: " + u.getScheme()+"://"+u.getHost()+" *** extName:"+extName);

            Intent intent = new Intent();
            intent.setData(u);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // TF101-183830 bug fix
            if (activity.getIntent().getBooleanExtra("return-data", false) && activity.getIntent().getType().contains("image")) {
                Bitmap b = IconUtility.loadResizedBitmap(file.getAbsolutePath(), 96, 96);
                if (b != null)
                    intent.putExtra("data", b);
            }
            VFile tempVFile = file.getParentFile();
            if (tempVFile == null || !tempVFile.exists()) {
                tempVFile = new LocalVFile(FileListFragment.DEFAULT_INDICATOR_FILE);
            }else {
                tempVFile = new LocalVFile(tempVFile);
            }
            if(activity.getExternalCacheDir() != null && !tempVFile.getAbsolutePath().startsWith(activity.getExternalCacheDir().getAbsolutePath())){
                FileUtility.saveCurrentScanFileInfo(activity,tempVFile , SCAN_FILE_ATTACHOP_INFO_NAME);
            }
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    public static boolean isDRM(VFile file){
        String ext = "";
        ext = file.getExtensiontName().toLowerCase();
        if ("dcf".compareTo(ext) == 0 || "fl".compareTo(ext) == 0) {
            return true;
        }
        return false;
    }
    public static FileInfo getInfo(VFile f) {
        FileInfo info = new FileInfo();
        if (f == null || folder_depth > MAX_FOLDER_DEPTH)
            return info;

        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(f.getAbsolutePath());
        DocumentFile targetFile = null;

        if(useSaf){
            targetFile = SafOperationUtility.getInstance().getDocFileFromPath(f.getAbsolutePath());
            if (targetFile != null && !f.getHasRetrictFiles())
                return getInfo(targetFile);
        }

        if (f.isDirectory()) {
            folder_depth++;
            if (useSaf && null != targetFile){
                info.PermissionRead = targetFile.canRead();
                info.PermissionWrite = targetFile.canWrite();
            }else{
                info.PermissionRead = f.canRead();
                info.PermissionWrite = f.canWrite();
            }
            info.numFiles = 0;
            info.numFolders = 1;
            info.numSize = f.length();
            VFile[] childs = f.listVFiles();
            if (childs != null) {
                for (int i = 0; i < childs.length && childs[i] != null; i++) {
                    FileInfo t = getInfo(childs[i]);
                    info.PermissionRead = info.PermissionRead && t.PermissionRead;
                    info.PermissionWrite = info.PermissionWrite && t.PermissionWrite;
                    info.numFiles += t.numFiles;
                    info.numFolders += t.numFolders;
                    info.numSize += t.numSize;
                }
            }
            folder_depth--;
        } else {
            if (useSaf && null != targetFile) {
                info.PermissionRead = targetFile.canRead();
                info.PermissionWrite = targetFile.canWrite();
            }else{
                info.PermissionRead = f.canRead();
                info.PermissionWrite = f.canWrite();
            }
            info.numFiles = 1;
            info.numFolders = 0;
            info.numSize = f.length();
        }
        return info;
    }

    public static FileInfo getInfo(DocumentFile f) {
        FileInfo info = new FileInfo();
        if (f == null || folder_depth > MAX_FOLDER_DEPTH)
            return info;

        //Log.d(TAG, "file uri: " + f.getUri());
        if (f.isDirectory()) {
            folder_depth++;
            info.PermissionRead = f.canRead();
            info.PermissionWrite = f.canWrite();
            info.numFiles = 0;
            info.numFolders = 1;
            info.numSize = 0;//f.length();
            DocumentFile[] childs = f.listFiles();
            if (childs != null) {
                for (int i = 0; i < childs.length && childs[i] != null; i++) {
                    FileInfo t = getInfo(childs[i]);
                    info.PermissionRead = info.PermissionRead && t.PermissionRead;
                    info.PermissionWrite = info.PermissionWrite && t.PermissionWrite;
                    info.numFiles += t.numFiles;
                    info.numFolders += t.numFolders;
                    info.numSize += t.numSize;
                }
            }
            folder_depth--;
        } else {
            info.PermissionRead = f.canRead();
            info.PermissionWrite = f.canWrite();
            info.numFiles = 1;
            info.numFolders = 0;
            info.numSize = f.length();
        }
        return info;
    }

    public static long getArrayTotalLength(VFile[] fa) {
        long totalSize = 0;
        for (int i = 0; fa != null && i < fa.length && fa[i] != null; i++) {
            totalSize += calculateTotalLength(fa[i], false);
        }
        return totalSize;
    }

    public static void applySelectedFiles(Activity activity, VFile[] files) {
        Intent intent = new Intent();
        ArrayList<Parcelable> list = new ArrayList<Parcelable>();
        if(files != null && files.length > 0){
            for (VFile file : files) {
                Uri u = null;
                //u = Uri.fromFile(file);
                String mediaFile_mime = null;
                mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
                if ("video/mp4".equals(mediaFile_mime) || "video/3gpp".equals(mediaFile_mime)) {
                    mediaFile_mime = checkMimeType(activity.getApplicationContext(), mediaFile_mime, file);
                }

                u = getUri(activity.getApplicationContext(), file, mediaFile_mime, true);
                list.add(u);
            }
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    public static void shareFile(Activity activity, VFile[] files, boolean isRemoteFile) {
        Intent shareIntent;

        if (files.length == 1) {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String mime = reflectionApis.mediaFile_getMimeTypeForFile(files[0].getName());

            if ("video/mp4".equals(mime) || "video/3gpp".equals(mime)) {
                mime = checkMimeType(activity.getApplicationContext(), mime, files[0]);
            } else if ("application/epub+zip".equals(mime)) {
                mime = "application/zip";
            }

            if (mime == null)
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(files[0].getExtensiontName().toLowerCase());

            Uri u = null;
            u = getUri(activity.getApplicationContext(), files[0], mime, false);
            Log.d("Jack", "*******************u = *************" + u);
            shareIntent.putExtra(Intent.EXTRA_STREAM, u);
            if (mime != null) {
                // TT-130576 Google Messenger cannot attach the txt file when
                // mime type is set as text/*, we will use text/plain as mime type
                // to fix this problem.
                if (mime.equals("text/plain")) {
                    shareIntent.setType(mime);
                } else {
                    int end = mime.indexOf("/");
                    if (end > 0) {
                        String prefix = mime.substring(0, end);
                        mime = prefix + "/*";
                        shareIntent.setType(mime);
                    } else {
                        shareIntent.setType("*/*");
                    }
                }
            } else if("mts".equals(files[0].getExtensiontName().toLowerCase())) {
                shareIntent.setType("video/*");
            } else {
                shareIntent.setType("*/*");
            }
        } else {

            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String mime = getFilesMimeType(activity.getApplicationContext(), files);
            shareIntent.setType(mime);

            ArrayList<Parcelable> list = new ArrayList<Parcelable>();
            for (VFile file : files) {
                Uri u = null;

                u = getUri(activity.getApplicationContext(), file, mime, false);

                list.add(u);
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        }
        try{
            activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.action_share)));
        }catch(Exception e){
            Log.d(TAG,"=share File fail==" + e);
        }
    }

    public static String getPathFromMediaUri(ContentResolver cr, Uri contentUri) {
        String path = null;
        String[] proj = {
                MediaStore.MediaColumns.DATA
        };
        Cursor cursor = cr.query(contentUri, proj, null, null, null);
        if (cursor != null)
            try {
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(column_index);
                }
            } finally {
                cursor.close();
            }
        return path;
    }

    public static String getNameFromEmailUri(ContentResolver cr, Uri contentUri) {
        String path = null;
        String[] proj = {
                "_display_name"
        };
        Cursor cursor = cr.query(contentUri, proj, null, null, null);
        if (cursor != null)
            try {
                int column_index = cursor
                        .getColumnIndexOrThrow("_display_name");
                if (cursor.moveToFirst()) {
                    path = cursor.getString(column_index);
                }
            } finally {
                cursor.close();
            }
        return path;
    }

    public static boolean isExternalStorageAvailable() {
        boolean state = false;
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            state = true;
        }
        return state;
    }


    private static String checkMimeType(Context context, String mime, File file) {
        try {
            String path = FileUtility.getCanonicalPath(file);
            Uri uri = MediaStore.Files.getContentUri("external");
            Uri query = uri.buildUpon().appendQueryParameter("limit", "1").build();
            String[] DATA_PROJECTION = new String[] {
                    MediaStore.MediaColumns.MIME_TYPE
            };
            String[] selectArgs = new String[] {
                    path
            };

            Cursor cur = context.getContentResolver().query(query, DATA_PROJECTION, "_data=?", selectArgs, null);

            if (cur != null) {
                try {
                    if (cur.getCount() > 0 && cur.moveToFirst()) {
                        mime = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                    }
                } finally {
                    cur.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        Log.d(TAG, "the vedio type is : " + mime);
        return mime;
    }

    private static String getFilesMimeType(Context context, VFile[] files) {
        String result = "*/*";

        for (int i = 0; i < files.length; i++) {
            String mime = reflectionApis.mediaFile_getMimeTypeForFile(files[i].getName());

            if ("video/mp4".equals(mime) || "video/3gpp".equals(mime)) {
                mime = checkMimeType(context, mime, files[0]);
            }

            if (mime == null)
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(files[i].getExtensiontName().toLowerCase());

            if (mime == null) {
                result = "*/*";
                break;
            } else {
                if (i == 0) {
                    result = mime;
                } else {
                    //if (mime.compareToIgnoreCase(result) != 0) {
                        int end = result.indexOf("/");
                        String prefix = result.substring(0, end);
                        if (mime.startsWith(prefix)) {
                            result = prefix + "/*";
                        } else {
                            result = "*/*";
                            break;
                        }
                    //}
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "return result : " + result);
        }
        return result;
    }

    public static Uri getUri(Context context, VFile file, String mimeType, boolean checkARN) {
        boolean fromHiddenZone = FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(DirectoryType.HiddenZone, file);
        Uri result = null;
        try {
            if (!fromHiddenZone && FileUtility.isPathInScanDirectories(file)) {
                result = getMediaContentUri(context, file, mimeType, checkARN);
                if (DEBUG) {
                    Log.i(TAG, " *** uri *** content : " + result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (result != null){
                if (!(result.getScheme().isEmpty()) && !result.getScheme().equalsIgnoreCase("content")) {
                    result = OpenFileProvider.getUriForFile(new File(file.getAbsolutePath()), fromHiddenZone);
                    if (DEBUG) {
                        Log.i(TAG, " *** uri *** N : " + result);
                    }
                }
            }else{
                result = OpenFileProvider.getUriForFile(new File(file.getAbsolutePath()), fromHiddenZone);
                if (DEBUG) {
                    Log.i(TAG, " *** uri *** N : " + result);
                }
            }
        }

        if (result == null) {
            if (!fromHiddenZone && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                result = Uri.fromFile(file);
                Log.i(TAG, " *** uri *** < M: " + result);
            }
            else {
                result = OpenFileProvider.getUriForFile(file, fromHiddenZone);
                Log.i(TAG, " *** uri *** open uri for file: " + result);
            }
            if (DEBUG) {
                Log.d(TAG, "u fromFile");
            }
        }

        if (DEBUG) {
            Log.i(TAG, " *** uri *** : " + result);
        }
        return result;
    }

    private static Uri getMediaContentUri(Context context, File file, String mime, boolean checkARN) {
        Uri uri = null;

        if (mime != null) {
            int fileType = reflectionApis.mediaFile_getFileTypeForMimeType(mime);
            Uri mediaUri = getMediaUri(fileType, mime);
            if(MediaStore.Files.getContentUri("external").toString().equals(mediaUri.toString())){
                uri = Uri.fromFile(file);
            }else{
                uri = getMediaStoreContentUri(context, file, mediaUri , checkARN);
            }
            if (uri == null) {
                if (DEBUG) {
                    Log.d(TAG, "someting erro and try to query MediaStore.Files");
                }
                uri = getMediaStoreContentUri(context, file, MediaStore.Files.getContentUri("external") , checkARN);
            }
        }

        return uri;
    }

    private static Uri getMediaUri(int fileType, String mime) {
        Uri uri = null;
        if (reflectionApis.mediaFile_isAudioFileType(fileType) || "audio/3gpp".equals(mime)) {
            if (DEBUG) {
                Log.d(TAG, "isAudioFileType : true");
            }
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (reflectionApis.mediaFile_isImageFileType(fileType)) {
            if (DEBUG) {
                Log.d(TAG, "isImageFileType : true");
            }
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (reflectionApis.mediaFile_isVideoFileType(fileType)) {
            if (DEBUG) {
                Log.d(TAG, "isVideoFileType : true");
            }
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            if (DEBUG) {
                Log.d(TAG, "isFileType : true");
            }
            uri = MediaStore.Files.getContentUri("external");
        }
        return uri;
    }

    private static Uri getMediaStoreContentUri(Context context, File file, Uri uri, boolean checkARN) {
        boolean needToCheckARN= uri.equals(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) && checkARN;
        String[] DATA_PROJECTION ;
        String path = null;

        try {
            path = FileUtility.getCanonicalPath(file);
        } catch (IOException e) {
            Log.w(TAG, "getMediaContentUri : get Canonical Path error");
        }

        Uri query = uri.buildUpon().appendQueryParameter("limit", "1").build();
        if(needToCheckARN) {
            DATA_PROJECTION = new String[] {
                    MediaStore.MediaColumns._ID,
                    MediaStore.Audio.Media.IS_RINGTONE,
                    MediaStore.Audio.Media.IS_ALARM,
                    MediaStore.Audio.Media.IS_NOTIFICATION,
                    MediaStore.Audio.Media.IS_MUSIC
            };
        } else {
            DATA_PROJECTION = new String[] {
                    MediaStore.MediaColumns._ID
            };
        }
        String[] selectArgs = new String[] {
                path
        };

        Cursor cur = context.getContentResolver().query(query, DATA_PROJECTION, "_data=?", selectArgs, null);

        if (cur != null) {
            try {
                if (cur.getCount() > 0 && cur.moveToFirst()) {
                    if (needToCheckARN) {
                        if ((cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM)) != 0
                                || cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE)) != 0
                                || cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION)) != 0)
                                && cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)) == 0) {
                            return Uri.fromFile(file);
                        }

                    }
                    String id = cur.getString(0);
                    return uri.buildUpon().appendPath(id).build();
                }
            } finally {
                cur.close();
            }
        }
        return null;
    }

    public static String getCanonicalPathNoException(File file) {
        try {
            return getCanonicalPath(file);
        }catch (IOException e)
        {
            return file.getAbsolutePath();
        }
    }

    public static String getCanonicalPath(File file) throws IOException {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return getCanonicalPathForUser(file);
        } else {
            return file.getCanonicalPath();
        }
    }

    public static String getCanonicalPathForUser(File file) throws IOException {
        return getCanonicalPathForUser(file.getCanonicalPath());
    }

    public static String getCanonicalPathForUser(String canonicalPath) {
        if ((IS_EXTERNALSTORAGEEMULATED) && (LEGACY_EXTERNAL_STORAGE_PATH != null)) {	//+++ tsungching_lin
            // Splice in user-specific path when legacy path is found
            if (canonicalPath.startsWith(LEGACY_EXTERNAL_STORAGE_PATH)) {
                int legacy_length = LEGACY_EXTERNAL_STORAGE_PATH.length();
                if (canonicalPath.length() == legacy_length) {
                    return EXTERNAL_STORAGE_PATH_FOR_USER;
                } else {
                    return EXTERNAL_STORAGE_PATH_FOR_USER + canonicalPath.substring(legacy_length);
                }
            }
        }
        return canonicalPath;
    }

    public static boolean isPathInScanDirectories(File file) throws IOException {

        return WrapMediaScanner.isPathInScanDirectoriesWithUID(getCanonicalPath(file));

    }

    /**
     * Check the file path is in the path of any storage volume.
     *
     * @param context The application context.
     * @param file The specified file.
     * @return True means yes, false otherwise.
     * @throws IOException
     */
    public static boolean isPathInStorageVolume(@NonNull Context context, @NonNull File file)
            throws IOException
    {
        StorageManager storageManager =
                (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Object[] storageVolumes = reflectionApis.getVolumeList(storageManager);

        for(Object storageVolume : storageVolumes)
        {
            String path = reflectionApis.volume_getPath(storageVolume);

            if(getCanonicalPath(file).startsWith(path))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isExistApp(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /*public static boolean isAvailableOpenWithMusic(Context context, String fileName){
        if (isExistApp(context, ConstantsUtil.ASUS_MUSIC_PACKAGENAME)) {
            if (fileName != null && fileName.length()>0) {
                String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1,fileName.length());
                if (isPlayWidthMusic(fileExtension)) {
                    return true;
                }
            }
        }
        return false;
    }*/

    public static int isAvailableOpenWithOtherApp(Context context, String fileName){
        if (fileName != null && fileName.length()>0) {
            String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1,fileName.length());
            if (fileExtension != null ) {
                fileExtension = fileExtension.toLowerCase();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if(mimeType != null){
                    if (mimeType.startsWith("audio/") && isExistApp(context, ConstantsUtil.ASUS_MUSIC_PACKAGENAME)) {
                        return OpenType.OPEN_TYPE_MUSIC;
                    }else if (mimeType.startsWith("video/")) {// && (isExistApp(context, ConstantsUtil.ASUS_GALLERY_PACKAGENAME) || isExistApp(context, ConstantsUtil.ASUS_GALLERY_NEW_PACKAGENAME)
                        return OpenType.OPEN_TYPE_GALLERY;
                    }
                }
            }
            String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(fileName);
            if(mediaFile_mime != null){
                if (mediaFile_mime.startsWith("audio/") && isExistApp(context, ConstantsUtil.ASUS_MUSIC_PACKAGENAME)) {
                    return OpenType.OPEN_TYPE_MUSIC;
                }else if (mediaFile_mime.startsWith("video/")) {// && (isExistApp(context, ConstantsUtil.ASUS_GALLERY_PACKAGENAME) || isExistApp(context, ConstantsUtil.ASUS_GALLERY_NEW_PACKAGENAME))
                    return OpenType.OPEN_TYPE_GALLERY;
                }
            }
        }
        return OpenType.OPEN_TYPE_DEFAULT;
    }

    public static int isMultiMediaFile(Context context, String fileName){
        if (fileName != null && fileName.length()>0) {
            String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1,fileName.length());
            if (fileExtension != null ) {
                fileExtension = fileExtension.toLowerCase();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if(mimeType != null){
                    if (mimeType.startsWith("audio/")) {
                        return OpenType.OPEN_TYPE_MUSIC;
                    }else if (mimeType.startsWith("video/")) {
                        return OpenType.OPEN_TYPE_GALLERY;
                    }
                }
            }
            String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(fileName);
            if(mediaFile_mime != null){
                if (mediaFile_mime.startsWith("audio/")) {
                    return OpenType.OPEN_TYPE_MUSIC;
                }else if (mediaFile_mime.startsWith("video/")) {
                    return OpenType.OPEN_TYPE_GALLERY;
                }
            }
        }
        return OpenType.OPEN_TYPE_DEFAULT;
    }

    public static  void openStreamFileWidth(Activity mActivity,RemoteVFile file,int openType){
        RemoteFileUtility.getInstance(mActivity).sendCloudStorageMsg(file.getStorageName(), file, null, file.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_URI, "");
        /*Intent intent = new Intent();
        if (openType == OpenType.OPEN_TYPE_MUSIC) {
            intent.setAction(ConstantsUtil.ASUS_MUSIC_OPEN_ACTION);
            intent.setType("audio/*");
        }else if (openType == OpenType.OPEN_TYPE_GALLERY) {
            intent.setAction(ConstantsUtil.ASUS_GALLERY_OPEN_ACTION);
            intent.setClassName("com.asus.ephoto", "com.asus.ephoto.app.MovieActivity");
            intent.setType("video/*");
        }else {
            Log.d(TAG,"openStreamFileWidth error! ");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putInt(ConstantsUtil.ASUS_MUSIC_CLOUDTYPE_KEY, file.getMsgObjType());
        bundle.putString(ConstantsUtil.ASUS_MUSIC_DEVICEID_KEY, file.getmDeviceId());
        bundle.putString(ConstantsUtil.ASUS_MUSIC_FILEID_key, file.getFileID());
        bundle.putString(ConstantsUtil.ASUS_MUSIC_CLOUD_PATH_KEY,file.getAbsolutePath());
        bundle.putString(ConstantsUtil.AUUS_MUSIC_CLOUD_ACCOUNT_KEY, file.getStorageName());
        intent.putExtra(ConstantsUtil.ASUS_MUSIC_PARAMS_KEY, bundle);



        mActivity.startActivity(intent);*/

    }
    public static void openMusicOrGalleryWithUri(Activity mActivity,String fileName,String uri,int msgObjType,String storageName){
        if (fileName != null && fileName.length()>0) {
            String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1,fileName.length());
            if (fileExtension != null) {
                fileExtension = fileExtension.toLowerCase();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if(mimeType != null){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Bundle bundle = new Bundle();
                    bundle.putInt(ConstantsUtil.ASUS_MUSIC_CLOUDTYPE_KEY, msgObjType);
                    intent.putExtra(ConstantsUtil.ASUS_MUSIC_GALLERY_FILENAME__KEY,  fileName);
                    bundle.putString(ConstantsUtil.AUUS_MUSIC_CLOUD_ACCOUNT_KEY, storageName);
                    intent.putExtra(ConstantsUtil.ASUS_MUSIC_PARAMS_KEY, bundle);

                    if (mimeType.startsWith("audio/")  || mimeType.equals("application/x-flac")) {
                        int musicVersionCode = 14062700;
                        try {
                            musicVersionCode = mActivity.getPackageManager().getPackageInfo("com.asus.music", 0).versionCode;
                        } catch (NameNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if(musicVersionCode >= 14062700){
                            intent.setAction(ConstantsUtil.ASUS_MUSIC_OPEN_ACTION);
                        }

                        intent.setDataAndType(Uri.parse(uri), "audio/*");
                    }else if (mimeType.startsWith("video/") ) {
                        //intent.setAction(ConstantsUtil.ASUS_GALLERY_OPEN_ACTION);
                        intent.setDataAndType(Uri.parse(uri), "video/*");
                        intent.putExtra(Intent.EXTRA_TITLE, fileName);
                    }
                    if (mActivity != null ) {
                        try {
              Log.d(TAG, "" + intent);
                            mActivity.startActivity(intent);
                        } catch (ActivityNotFoundException  e) {
                            ToastUtility.show(mActivity, R.string.open_fail);
                        }
                        return ;
                    }
                }
            }

            String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(fileName);
            if(mediaFile_mime != null){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Bundle bundle = new Bundle();
                bundle.putInt(ConstantsUtil.ASUS_MUSIC_CLOUDTYPE_KEY, msgObjType);
                intent.putExtra(ConstantsUtil.ASUS_MUSIC_GALLERY_FILENAME__KEY,  fileName);
                bundle.putString(ConstantsUtil.AUUS_MUSIC_CLOUD_ACCOUNT_KEY, storageName);
                intent.putExtra(ConstantsUtil.ASUS_MUSIC_PARAMS_KEY, bundle);

                if (mediaFile_mime.startsWith("audio/")  || mediaFile_mime.equals("application/x-flac")) {
                    int musicVersionCode = 14062700;
                    try {
                        musicVersionCode = mActivity.getPackageManager().getPackageInfo("com.asus.music", 0).versionCode;
                    } catch (NameNotFoundException e) {
                            // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(musicVersionCode >= 14062700){
                        intent.setAction(ConstantsUtil.ASUS_MUSIC_OPEN_ACTION);
                    }
                    intent.setDataAndType(Uri.parse(uri), "audio/*");
                }else if (mediaFile_mime.startsWith("video/") ) {
                    //intent.setAction(ConstantsUtil.ASUS_GALLERY_OPEN_ACTION);
                    intent.setDataAndType(Uri.parse(uri), "video/*");
                    intent.putExtra(Intent.EXTRA_TITLE, fileName);
                }
                if (mActivity != null ) {
                    try {
                        Log.d(TAG, "" + intent);
                        mActivity.startActivity(intent);
                    } catch (ActivityNotFoundException  e) {
                        ToastUtility.show(mActivity, R.string.open_fail);
                    }
                    return ;
                }
            }
        }
        if (mActivity != null) {
            ToastUtility.show(mActivity, R.string.open_fail);
        }
    }
/*    public static boolean isPlayWidthMusic (String extension){
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType != null && (mimeType.startsWith("audio/") || mimeType.startsWith("video/"))) {
            return true;
        }
        return false;
    }*/

    public static void saveCurrentScanFileInfo(Activity mActivity,VFile file,String editFileName){
      if (file != null) {
          SharedPreferences fileInfo =    mActivity.getSharedPreferences(editFileName, Context.MODE_PRIVATE);
          Editor editor = fileInfo.edit();
        if (file.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            String filePath = file.getAbsolutePath();
            editor.putString(FILE_PATH, filePath);
        }else {
            editor.putString(FILE_PATH, "");
        }
        editor.putInt(FILE_TYPE, file.getVFieType());
        editor.commit();
      }
    }
    public static void saveCurrentSortType(Activity mActivity,int sortType){
            SharedPreferences fileInfo =    mActivity.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
            Editor editor = fileInfo.edit();
            editor.putInt(FILE_SORTTYPE, sortType);
            editor.commit();
        MediaProviderAsyncHelper.setSortType(sortType);
    }
    public static int getCurrentSortType(Context context){
        SharedPreferences sharedPrefences = context.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
        //int type = sharedPrefences.getInt(FILE_SORTTYPE, SortType.SORT_DATE_DOWN);
        int type = sharedPrefences.getInt(FILE_SORTTYPE, SortType.SORT_NAME_DOWN);
        MediaProviderAsyncHelper.setSortType(type);
        return type;
    }
    public static boolean getIsHideSystemFile(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefsFile", 0);
        return sharedPreferences.getBoolean("mShowHidden", false);
    }
    public static VFile getFileFromSharedPreferences(Activity mActivity,String editFileName){
        LocalVFile file = null;
        if (mActivity != null) {
            SharedPreferences fileInfo = mActivity.getSharedPreferences(editFileName, Context.MODE_PRIVATE);
            int fileType = fileInfo.getInt(FILE_TYPE, -1);
            String filePath = fileInfo.getString(FILE_PATH, "");
            if (fileType == VFileType.TYPE_LOCAL_STORAGE && filePath != null && filePath.length()>0) {
                file = new LocalVFile(filePath);
            }
        }
        return file;
    }

    public static void saveMediaFilesToProvider(MsgObj msgObj,MsgObj newMsgObj){
        if (msgObj != null ) {
            String parentPath = msgObj.getFileObjPath().getFullPath();
            FileObj[] fileObjs = newMsgObj.getFileObjFiles();

            for (FileObj fileObj : fileObjs) {
                String fileName = fileObj.getFileName();
                String fullPath = parentPath + File.separator + fileName;
                Log.d(TAG, "fullPath:"+fullPath);
                VFile file = new LocalVFile(fullPath);
                if (file != null && file.exists()) {
                    if (!file.isDirectory()) {
                        MediaProviderAsyncHelper.addFile(file, true);
                    }else {
                        MediaProviderAsyncHelper.addFolder(file    , true);
                    }
                }
            }
        }
    }

    public static boolean isMusic(File file){
        boolean is_music = false;

        String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
        if (mediaFile_mime != null) {
            if (mediaFile_mime.substring(0, 5).equals("audio") || mediaFile_mime.equals("application/ogg") || mediaFile_mime.equals("application/vnd.android.package-archive")){
                is_music = true;
            }
        }

        if (!is_music) {
            String extName = new LocalVFile(file).getExtensiontName();
            String mimeUtils_mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extName);
            if(mimeUtils_mime != null && mimeUtils_mime.startsWith("audio")){
                is_music = true;
            }
        }

        return is_music;
    }

    public static boolean isImage(File file){
        String mimeType = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
        if (mimeType == null) {
            String extName = new LocalVFile(file).getExtensiontName();
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extName);
        }
        return mimeType != null && mimeType.startsWith("image/");
    }

    public static void saveFirstStartup(Context context){
        SharedPreferences fileInfo = context.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
        Editor editor = fileInfo.edit();
        editor.putBoolean(FIRST_STARTUP, false);
        editor.commit();
    }

    public static void saveVerizonTipsStartup(Context context,boolean enable){
        try {
            int status = enable ? 1 : 0;
            ContentValues contentValues = new ContentValues();
            contentValues.put("TIP_STATUS", status); // Value(int) 0: Disable tip;  1: Enable tip
            Uri uri = Uri.parse(VERIZON_TIPS_SETTING);
            context.getContentResolver().update(uri, contentValues, null, null);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public static boolean isFirstStartup(Context context){
        SharedPreferences fileInfo = context.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
        return fileInfo.getBoolean(FIRST_STARTUP, true);
    }

    public static boolean isVerizonTipsStartup(Context context) {
        if (!WrapEnvironment.IS_VERIZON)
            return false;

        //default null, don't show tip
        boolean enable = false;

        Uri uri = Uri.parse(VERIZON_TIPS_SETTING); // PackageName (String)
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null)
        {
            cursor.moveToFirst();
            // status(int) 0: Disable tip;  1: Enable tip
            enable = (cursor.getInt(1) == 1 ? true : false);
            cursor.close();
        }

        return enable;
    }

    public static void saveFirstSDPermission(Context context){
        SharedPreferences fileInfo = context.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
        Editor editor = fileInfo.edit();
        editor.putBoolean(FIRST_SDPERMISSION, false);
        editor.commit();
    }

    public static boolean isFirstSDPermission(Context context){
        return true;
        /* always show SDPermission Tutorial
        SharedPreferences fileInfo = context.getSharedPreferences(FileUtility.SCAN_FILE_ATTACHOP_INFO_NAME, Context.MODE_PRIVATE);
        return fileInfo.getBoolean(FIRST_SDPERMISSION, true);
        */
    }
    public static boolean isLowStorage(Intent intent){
        boolean bLowStorage = intent.getBooleanExtra("intent.key.lowstorage",false);
        return bLowStorage;
    }
    public static VFile[] filterHiddenFile(VFile[] fa)
    {
        if (fa == null || fa.length == 0)
            return fa;
        ArrayList<VFile> pool = new ArrayList<VFile>();
        for (int i = 0; i < fa.length && fa[i] != null; i++)
        {
            if (!fa[i].isHidden())
                pool.add(fa[i]);
        }

        return pool.toArray(new VFile[pool.size()]);
    }

    @TargetApi(19)
    public static String getSecondaryStoragePath(Context context, File aFile){
        String path = null;
        File[] possible_mounts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            possible_mounts = context.getExternalFilesDirs(null);
        }
        String aTestPath = null;
        try {
            aTestPath = aFile.getCanonicalPath();
        } catch (IOException e1) {
            aTestPath = aFile.getAbsolutePath();
        }
        for (File aRoot : possible_mounts){
            String aAppPath = null;
            try {
                aAppPath = aRoot.getCanonicalPath();
            } catch (IOException e) {
                aAppPath = aRoot.getAbsolutePath();
            }
            if (aAppPath.startsWith(aTestPath)){
                path = aAppPath;
                break;
            }
        }
        return path;
    }

    public static String changeToStoragePath(String sdcardPath) {
        if (sdcardPath.startsWith("/sdcard")) {
            return sdcardPath.replaceFirst("/sdcard", Environment.getExternalStorageDirectory().toString());
        } else if (WrapEnvironment.SUPPORT_STORAGE_SD_OR_USB) {
            if (sdcardPath.startsWith("/Removable")) {
                return sdcardPath.replaceFirst("/Removable", "/storage");
            }
        }
        return sdcardPath;
    }

    public static String changeToSdcardPath(String storagePath) {
        if (storagePath.startsWith(Environment.getExternalStorageDirectory().toString())) {
            return storagePath.replaceFirst(Environment.getExternalStorageDirectory().toString(), "/sdcard");
        } else if (WrapEnvironment.SUPPORT_REMOVABLE) {
            if(storagePath.startsWith("/storage/MicroSD")){
                return storagePath.replaceFirst("/storage/MicroSD", "/Removable/MicroSD");
            } else if (storagePath.startsWith("/storage/sdcard1")){
                return storagePath.replaceFirst("/storage/sdcard1", "/Removable/MicroSD");
            } else if(storagePath.startsWith("/storage/USBdisk1") ||
                      storagePath.startsWith("/storage/USBdisk2") ||
                      storagePath.startsWith("/storage/USBdisk3") ||
                      storagePath.startsWith("/storage/USBdisk4") ||
                      storagePath.startsWith("/storage/USBdisk5")
                ) {
                return storagePath.replaceFirst("/storage", "/Removable");
            } else{
                return storagePath;
            }
        }
        return storagePath;
    }
    public static boolean delete(File deleteFile) {
        if (deleteFile.isDirectory())
            return deleteDirectory(deleteFile);
        else
            return deleteFile(deleteFile);
    }

    public static boolean deleteDirectory(File deleteFile) {
        File[] files = deleteFile.listFiles();
        if(files!=null)
            for (File file : files) {
                if (!delete(file))
                    return false;
            }
        return deleteFile(deleteFile);
    }

    public static boolean deleteFile(File deleteFile) {
        if (!deleteFile.delete())
            return false;
        return true;
    }


    public static void checkUsableSpace(Long lengthNeeded, File dstFile) {
        File parentFile = dstFile.getParentFile();
        while (!parentFile.exists())
            parentFile = parentFile.getParentFile();
        if (lengthNeeded > parentFile.getUsableSpace()) {
            throw new InsufficientStorageException(
                    "Usable space: " + parentFile.getUsableSpace() + ", but need: " + lengthNeeded);
        }
    }

    public static long calculateTotalLength(VFile targetFile, boolean onlyCalculateDirectory) {
        if (targetFile.isFile())
            return onlyCalculateDirectory? 0 : targetFile.length();
        long totalLength = targetFile.length();
        if (targetFile.listVFiles() != null) {
            for (VFile file : targetFile.listVFiles())
                totalLength += calculateTotalLength(file, onlyCalculateDirectory);
        }
        return totalLength;
    }
    static List<String> ZF3 = Arrays.asList("ASUS_A001", "ASUS_Z017D_1", "ASUS_Z012D", "Z016", "Z016_1");
    public static void sync(){
        String command = "sync";
        try {
            if (ZF3.contains(WrapEnvironment.MODEL_NAME)) {
                Process process = Runtime.getRuntime().exec(command);
            }
            //process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        } catch (Throwable ignore){
            ignore.printStackTrace();
        }
    }
}
