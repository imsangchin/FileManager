package com.asus.filemanager.samba;

import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.remote.utility.RemoteFileUtility.CopyTypeArgument;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.util.transport.TransportException;

public class SambaMessageHandle extends HandlerThread implements Callback{

    private final static String TAG = "SambaMessageHandle";

    public final static int MSG_SAMBA_LOGIN = 0;

    public final static int COPY = -1;
    public final static int CUT = -2;
    public final static String SMB = "smb";

    public final static int FILE_DELETE = 1;
    public final static int FILE_RENAME = 2;
    public final static int ADD_NEW_FOLDER = 3;
    public final static int SAMBA_ACTION_RESULT = 4;
    public final static int FILE_PASTE = 5;
    public final static int FILE_PASTE_CANCEL = 6;
    public final static int FILE_OPEN = 7;
    public final static int LOGIN_WITHOUT_PASSWORD = 8;
    public final static int FILE_SHARE = 9;
    public final static int FILE_DOWNLOAD = 10;
    //public final static int FILE_PASTE_SAMBA_REMOTE = 8;
    public final static int ACTION_SUCCESS = 0;
    public final static int ACTION_FAILED = 1;
    public final static int ACTION_UPDATE = 2;



    final static String SELECT_NAME_LIST = "select_name_list";
    final static String SELECT_NAME = "select_name";
    public final static String NEW_NAME = "new_name";
    public final static String SOURCE_PARENT_PATH = "source_parent_path";
    final static String DEST_PARENT_PATH = "dest_parent_path";
    final static String PASTE_TYPE = "paste_type";
    public static String PASTE_PROGRESS_SIZE = "paste_progress_size";
    public static String PASTE_TOTAL_SIZE = "paste_total_size";
    public static String PASTE_CURRENT_FILE_NAME = "paste_current_file_name";
    public static String LOGIN_URL = "login_url";
    public static String MSGOBJ_ID = "msgobj_id";

    public static String SAMBA_REMOTE_PASTE = "samba_remote_paste";
    public final static String ERROR_MESSAGE = "error_message";

    public static final String NO_SHARE_FILE_ERROR = "the user has not been granted the requested logon";
    public static final String NO_SPACE = "NO_ENOUTH_SPACE";

    private String ErrorMsg = null;

    private Handler mCallBackHandler = null;
    private long writeDataSize = 0;
    private long TotalDataSize = 0;

    private int actionType = -1;
    private int pasteType = -3;

    private boolean needShowDialog = false;

    public SambaMessageHandle(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public void setCallbackHandler(Handler handler){
        mCallBackHandler = handler;
    }

    @Override
    public boolean handleMessage(Message msg){
        ErrorMsg = null;
        Bundle result = new Bundle();

        Bundle data = msg.getData();
        String[] mNameList = data.getStringArray(SELECT_NAME_LIST);
        String mSelcetName = data.getString(SELECT_NAME, null);
        String mNewName = data.getString(NEW_NAME);
        String srcPath = data.getString(SOURCE_PARENT_PATH);
        String destPath = data.getString(DEST_PARENT_PATH);
        String serverUrl = data.getString(LOGIN_URL);
        int type = data.getInt(PASTE_TYPE);
        String msgId = data.getString(MSGOBJ_ID,null);

        pasteType = type;
        actionType = data.getInt(SAMBA_REMOTE_PASTE, -1);

        needShowDialog = false;

        Message message  = new Message();
        switch(msg.what){
            case LOGIN_WITHOUT_PASSWORD:
                loginInServer(serverUrl);
                message.arg1 = LOGIN_WITHOUT_PASSWORD;
                break;
            case MSG_SAMBA_LOGIN:
                loginInServer(serverUrl);
                message.arg1 = MSG_SAMBA_LOGIN;
                break;
            case FILE_PASTE:
                Log.d(TAG," FILE_PASTE");
                writeDataSize = 0;
                TotalDataSize = 0;
                needShowDialog = true;

                if(mSelcetName == null){
                    calculateMultiFileLength(mNameList,srcPath);
                    pasteTo(mNameList,srcPath,destPath,type);
                }else{
                    calculateFileContentSize(mSelcetName,srcPath);
                    pasteTo(mSelcetName,srcPath,destPath,type);
                }
                message.arg1 = FILE_PASTE;
                result.putString(SOURCE_PARENT_PATH, srcPath);
                if (actionType > -1) {
                    result.putInt(SAMBA_REMOTE_PASTE, actionType);
                }
                if (msgId != null) {
                    result.putString(MSGOBJ_ID, msgId);
                }
                result.putInt(PASTE_TYPE, type);
                break;
            case FILE_OPEN:
                if(!(new File(destPath + File.separator + mNameList[0]).exists()))
                    openSmbFile(mNameList[0],srcPath,destPath);
//              if(mSelcetName == null){
//                  pasteTo(mNameList,srcPath,destPath,type);
//              }else{
//                  pasteTo(mSelcetName,srcPath,destPath,type);
//              }
                message.arg1 = FILE_OPEN;
                result.putString(SOURCE_PARENT_PATH, destPath);
                result.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, mNameList);
                break;
            case FILE_SHARE:
                pasteTo(mNameList,srcPath,destPath,type);
                message.arg1 = FILE_SHARE;
                result.putString(SOURCE_PARENT_PATH, destPath);
                result.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, mNameList);
                break;
            case FILE_DELETE:
                Log.d(TAG," FILE_DELETE");
                if(mSelcetName == null){
                    delete(mNameList,srcPath);
                }else{
                    delete(mSelcetName,srcPath);
                }
                message.arg1 = FILE_DELETE;
                result.putString(SOURCE_PARENT_PATH, srcPath);
                if (actionType >-1) {
                    result.putInt(SAMBA_REMOTE_PASTE, actionType);
                }
                if (msgId != null) {
                    result.putString(MSGOBJ_ID, msgId);
                }
                result.putInt(PASTE_TYPE, type);
                break;
            case FILE_RENAME:
                renameDocument(srcPath,mSelcetName,mNewName);
                message.arg1 = FILE_RENAME;
                result.putString(SOURCE_PARENT_PATH, srcPath);
                break;
            case ADD_NEW_FOLDER:
                addFolder(srcPath,mNewName);
                message.arg1 = ADD_NEW_FOLDER;
                result.putString(NEW_NAME, mNewName);
                result.putString(SOURCE_PARENT_PATH, srcPath);
                break;
            case FILE_PASTE_CANCEL:
                break;
            case FILE_DOWNLOAD:
                openSmbFile(mNameList[0],srcPath,destPath);
                message.arg1 = FILE_DOWNLOAD;
                result.putString(SOURCE_PARENT_PATH, destPath);
                result.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, mNameList);
                break;
            default:
                break;
        }

        message.what = ACTION_SUCCESS;
        if(ErrorMsg != null){
            message.what = ACTION_FAILED;
//          ErrorMsg = getErrorMessage(ErrorMsg);
            result.putString(ERROR_MESSAGE, ErrorMsg);
            Log.d(TAG,"==ERROR_MESSAGE==" + ErrorMsg);
        }
        message.setData(result);
        mCallBackHandler.sendMessage(message);

        return true;
    }

    private void loginInServer(String url){
        try {
            jcifs.Config.setProperty("smb.client.disablePlainTextPasswords",
                    "false");
            SmbFile rootFile = new SmbFile(url);
            rootFile.connect();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch(Exception e){
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }

    }


    private void pasteTo(String[] namelist,String srcPath,String destPath,int type){
        if(srcPath.startsWith(SMB) && destPath.startsWith(SMB)){
            paste(namelist,srcPath,destPath,type);
        }else if(srcPath.startsWith(SMB) && !destPath.startsWith(SMB)){
            pasteSmbToLocal(namelist,srcPath,destPath,type);
        }else{
            pasteLocalToSmb(namelist,srcPath,destPath,type);
        }
    }

    private void pasteTo(String name,String srcPath,String destPath,int type){
        if(srcPath.startsWith(SMB) && destPath.startsWith(SMB)){
            paste(name,srcPath,destPath,type);
        }else if(srcPath.startsWith(SMB) && !destPath.startsWith(SMB)){
            pasteSmbToLocal(name,srcPath,destPath,type);
        }else{
            pasteLocalToSmb(name,srcPath,destPath,type);
        }
    }

    private void pasteSmbFileToLocal(String name,String srcPath,String destPath,int type){
        InputStream ins = null;
        OutputStream out = null;
        File dest = null;
        long FileLen = 0;
        DocumentFile destDocFile = null;
        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(destPath);
        Log.d("OMG","=useSaf==" + useSaf);
        try {
            SmbFile src = new SmbFile(srcPath + name);
            dest = new File(destPath + File.separator + name);

            final int length = src.length() > 65536 ? 65536 : (int)src.length();
            FileLen = src.length();
            Log.d("OMG","==length==" + length);
            if(length <= 0){
                if(type == CUT && actionType != CopyTypeArgument.SAMB_TO_CLOUD){
                    src.delete();
                }
                if(useSaf){
                    DocumentFile DestparentFile = SafOperationUtility.getInstance().getDocFileFromPath(destPath);
                    if(DestparentFile != null){
                        DestparentFile.createFile("*/*", name);
                    }
                }else{
                    dest.createNewFile();
                }
                return;
            }

            if(!dest.exists()){
                if(useSaf){
                    DocumentFile Destparent = SafOperationUtility.getInstance().getDocFileFromPath(destPath);
                    if(Destparent != null){
                        destDocFile = Destparent.createFile("*/*", name);
                        Log.d("OMG","==destDocFile==" + destDocFile.getName());
                    }
                }else{
                    dest.createNewFile();
                }

                if(FileLen > dest.getUsableSpace()){
                    ErrorMsg = NO_SPACE;
                    return;
                }

                ins = new BufferedInputStream(new SmbFileInputStream(src));
                if(useSaf){
                    out = SafOperationUtility.getInstance().getDocFileOutputStream(destDocFile);
                }else{
                    out = new BufferedOutputStream(new FileOutputStream(dest));
                }
                byte[] buffer = new byte[length];
                int rd = -1;
                rd = ins.read(buffer, 0, length);
                while(rd != -1 && !SambaFileUtility.getInstance(null).getIsPasteCancel()){
                    out.write(buffer, 0, rd);
                    writeDataSize += rd;
                    if(needShowDialog){
                        sendPasteProgress(writeDataSize,name);
                    }
                    rd = -1;
                    rd = ins.read(buffer, 0, length);
                    if((FileLen - writeDataSize) > dest.getUsableSpace()){
                        ErrorMsg = NO_SPACE;
                        break;
                    }
                }
                out.flush();
                ins.close();
                out.close();
                if(useSaf){
                    SafOperationUtility.getInstance().closeParcelFile();
                }
                if(ErrorMsg == null && type == CUT && !SambaFileUtility.getInstance(null).getIsPasteCancel() && actionType != CopyTypeArgument.SAMB_TO_CLOUD){
                    src.delete();
                }
                if(ErrorMsg != null && ErrorMsg.equals(NO_SPACE)){
                    dest.delete();
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch(TransportException e){
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
            if(FileLen > dest.getUsableSpace()){
                ErrorMsg = NO_SPACE;
            }
        }
        if(!TextUtils.isEmpty(ErrorMsg) || SambaFileUtility.getInstance(null).getIsPasteCancel()){
            dest.delete();
        }
        if(ErrorMsg == null){
            MediaProviderAsyncHelper.addFile(new VFile(dest), false);
        }
    }

    private void openSmbFile(String name,String srcPath,String destPath){
         InputStream ins = null;
            OutputStream out = null;
            File destTemp = null;
            File dest = null;
            long FileLen = 0;
            try {
                String dstFileName = destPath + File.separator + name;
                SmbFile src = new SmbFile(srcPath + name);
                destTemp = new File(dstFileName+"tmp");
                dest =  new File(dstFileName);
                dest.getParentFile().mkdirs();

                if(dest.exists())
                    dest.delete();
                final int length = src.length() > 65536 ? 65536 : (int)src.length();
                FileLen = src.length();
                if(length <= 0){
                    destTemp.createNewFile();
                    return;
                }
                boolean deleteCacheFile = true;
                if(destTemp.exists()){
                    deleteCacheFile = destTemp.delete();
                    if(deleteCacheFile){
                        MediaProviderAsyncHelper.deleteFile(new VFile(destTemp));
                    }
                }
                if(deleteCacheFile){

                    if(destTemp.createNewFile()){
                        if(FileLen > destTemp.getUsableSpace()){
                            ErrorMsg = NO_SPACE;
                            destTemp.delete();
                            return;
                        }
                    }

                    ins = new BufferedInputStream(new SmbFileInputStream(src));
                    out = new BufferedOutputStream(new FileOutputStream(destTemp));
                    byte[] buffer = new byte[length];
                    int rd = -1;
                    rd = ins.read(buffer, 0, length);
                    while(rd != -1 && !SambaFileUtility.getInstance(null).getIsPasteCancel()){
                        out.write(buffer, 0, rd);
                        writeDataSize += rd;
                        rd = -1;
                        rd = ins.read(buffer, 0, length);
                        if((FileLen - writeDataSize) > destTemp.getUsableSpace()){
                            ErrorMsg = NO_SPACE;
                            break;
                        }
                    }
                    out.flush();
                    ins.close();
                    out.close();
                    destTemp.renameTo(dest);

                    if(ErrorMsg != null && ErrorMsg.equals(NO_SPACE)){
                        destTemp.delete();
                    }
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            } catch(TransportException e){
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
                if(FileLen > destTemp.getUsableSpace()){
                    ErrorMsg = NO_SPACE;
                }
            }
            if(!TextUtils.isEmpty(ErrorMsg) || SambaFileUtility.getInstance(null).getIsPasteCancel()){
                destTemp.delete();
            }

            if(ErrorMsg == null){
                 MediaProviderAsyncHelper.addFile(new VFile(destTemp), false);
            }
    }

    private void pasteSmbFolderToLocal(String name,String srcPath,String destPath,int type){
        try {
            SmbFile srcfile = new SmbFile(srcPath + name + File.separator);
            File destFile = new File(destPath + File.separator + name);
            boolean success = true;

             boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(destPath);
            if(!destFile.exists() || !destFile.isDirectory()){
                if(useSaf){
                    DocumentFile destparentFile = SafOperationUtility.getInstance().getDocFileFromPath(destPath);
                    if(destparentFile != null){
                        success = destparentFile.createDirectory(name) == null ? false : true;
                    }
                }else{
                    success = destFile.mkdir();
                }

                if(success){
                    MediaProviderAsyncHelper.addFile(new VFile(destFile), false);
                }
            }
            if(success){
                SmbFile[] subFile = srcfile.listFiles();
                if(subFile != null && subFile.length > 0){
                    for(int index = 0;index < subFile.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
                        if(subFile[index].isDirectory()){
                            pasteSmbFolderToLocal(subFile[index].getName(),subFile[index].getParent(),destFile.getPath(),type);
                        }else{
                            pasteSmbFileToLocal(subFile[index].getName(),subFile[index].getParent(),destFile.getPath(),type);
                        }
                    }
                }

                if(type == CUT && actionType != CopyTypeArgument.SAMB_TO_CLOUD){
                    srcfile.delete();
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }
    }

    private void pasteSmbToLocal(String[] namelist,String srcPath,String destPath,int type){
        for(int index = 0;index < namelist.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
            pasteSmbToLocal(namelist[index],srcPath,destPath,type);
        }
    }

    private void pasteSmbToLocal(String name,String srcPath,String destPath,int type){
        try {
            SmbFile src = new SmbFile(srcPath + name);
            if(src.isDirectory()){
                pasteSmbFolderToLocal(name,srcPath,destPath,type);
            }else{
                pasteSmbFileToLocal(name,srcPath,destPath,type);
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }

    }

    private void pasteLocalFileToSmb(String name,String srcPath,String destPath,int type){
        InputStream ins = null;
        OutputStream out = null;
        SmbFile dest = null;
        long FileLen = 0;
        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(srcPath);
        File src = new File(srcPath + File.separator + name);
        try {
            dest = new SmbFile(destPath + name);

            final int length = src.length() > 65536 ? 65536 : (int)src.length();
            FileLen = src.length();
            if(length <= 0){
                if(type == CUT){
                    if(useSaf){
                        DocumentFile srcDocFile = SafOperationUtility.getInstance().getDocFileFromPath(src.getAbsolutePath());
                        if(srcDocFile != null){
                            if(srcDocFile.delete()){
                                MediaProviderAsyncHelper.deleteFile(new VFile(src));
                            }
                        }
                    }else{
                        if(src.delete()){
                            MediaProviderAsyncHelper.deleteFile(new VFile(src));
                        }
                    }
                    dest.createNewFile();
                }

                return;
            }

            if(!dest.exists()){
                dest.createNewFile();
                if(FileLen > dest.getDiskFreeSpace()){
                    ErrorMsg = NO_SPACE;
                    dest.delete();
                    return;
                }

                ins = new BufferedInputStream(new FileInputStream(src));
                out = new BufferedOutputStream(new SmbFileOutputStream(dest));

                byte[] buffer = new byte[length];
                int rd = -1;
                rd = ins.read(buffer, 0, length);
                while(rd != -1 && !SambaFileUtility.getInstance(null).getIsPasteCancel()){
                    out.write(buffer, 0, rd);
                    writeDataSize += rd;
                    if(needShowDialog){
                        sendPasteProgress(writeDataSize,name);
                    }
                    rd = -1;
                    rd = ins.read(buffer, 0, length);
                    if((FileLen - writeDataSize) > dest.getDiskFreeSpace()){
                        ErrorMsg = NO_SPACE;
                        break;
                    }
                }
                out.flush();
                ins.close();
                out.close();
                if(type == CUT && !SambaFileUtility.getInstance(null).getIsPasteCancel()){
                    if(useSaf){
                        DocumentFile srcDocFile = SafOperationUtility.getInstance().getDocFileFromPath(src.getAbsolutePath());
                        if(srcDocFile != null){
                            if(srcDocFile.delete()){
                                MediaProviderAsyncHelper.deleteFile(new VFile(src));
                            }
                        }
                    }else{
                        if(src.delete()){
                            MediaProviderAsyncHelper.deleteFile(new VFile(src));
                        }
                    }
                }
                if(ErrorMsg != null && ErrorMsg.equals(NO_SPACE)){
                    dest.delete();
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch(TransportException e){
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
            try {
                if(FileLen > dest.getDiskFreeSpace()){
                    ErrorMsg = NO_SPACE;
                }
            } catch (SmbException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        if(!TextUtils.isEmpty(ErrorMsg) || SambaFileUtility.getInstance(null).getIsPasteCancel()){
            try {
                dest.delete();
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            }
        }
    }

    private void pasteLocalFolderToSmb(String name,String srcPath,String destPath,int type){
        File src = new File(srcPath + File.separator + name);
        try {

            SmbFile dest = new SmbFile(destPath + name + File.separator);
            if(!dest.exists() || !dest.isDirectory()){
                dest.mkdir();
            }

            if(dest.exists() && dest.isDirectory()){

                File[] subFiles = src.listFiles();

// todo
                if (FileListFragment.SAMBA_MAP_FOR_RESTRICFILES != null) {

                    List<String> list = FileListFragment.SAMBA_MAP_FOR_RESTRICFILES.get(srcPath + File.separator + name);

                    if (list != null) {
                        subFiles = new File[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            subFiles[i] = new File(list.get(i));
                        }
                    }
                }

                if(subFiles != null && subFiles.length > 0){
                    for(int index = 0;index < subFiles.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
                        if(subFiles[index].isDirectory()){
                            pasteLocalFolderToSmb(subFiles[index].getName(),subFiles[index].getParent(),dest.getPath(),type);
                        }else{
                            pasteLocalFileToSmb(subFiles[index].getName(),subFiles[index].getParent(),dest.getPath(),type);
                        }
                    }
                }
                if(type == CUT && ErrorMsg == null){
                    boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(srcPath);
                    if(useSaf){
                        DocumentFile srcDocFile = SafOperationUtility.getInstance().getDocFileFromPath(src.getAbsolutePath());
                        if(srcDocFile != null) {
                            if (srcDocFile.isDirectory()) {
                                // Cut file in category may keep some files in this folder
                                // we only delete this folder if there is no files in this folder.
                                if (srcDocFile.listFiles() != null) {
                                    MediaProviderAsyncHelper.deleteFile(new VFile(src));
                                }
                            } else {
                                if(srcDocFile.delete()){
                                    MediaProviderAsyncHelper.deleteFile(new VFile(src));
                                }
                            }
                        }
                    }else{
                        if(src.delete()){
                            MediaProviderAsyncHelper.deleteFile(new VFile(src));
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }
    }

    private void pasteLocalToSmb(String[] namelist ,String srcPath,String destPath,int type){
        for(int index = 0;index < namelist.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
            pasteLocalToSmb(namelist[index],srcPath,destPath,type);
        }
    }

    private void pasteLocalToSmb(String name,String srcPath,String destPath,int type){
        File src = new File(srcPath + File.separator + name);
        if(src.isDirectory()){
            pasteLocalFolderToSmb(name,srcPath,destPath,type);
        }else{
            pasteLocalFileToSmb(name,srcPath,destPath,type);
        }
    }

    private void paste(String name,String srcPath,String destPath,int type){
        Log.d(TAG,"    paste   name === " + name);
        try {
            SmbFile src = new SmbFile(srcPath + name);
            SmbFile dest = new SmbFile(destPath + name);
            if(src.isDirectory()){
                src = null;
                dest = null;
                src = new SmbFile(srcPath + name + File.separator);
                dest = new SmbFile(destPath + name + File.separator);
            }
            if(type == COPY){
                copySmbToSmb(name,srcPath,destPath);
            }else if(isTheSameServer(src,dest)){
                src.renameTo(dest);
            }else{
                copySmbToSmb(name,srcPath,destPath);
                src.delete();
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }

    }

    private void copySmbToSmb(String name,String srcPath,String destPath){
        try {
            SmbFile src = new SmbFile(srcPath + name);
            if(src.isDirectory()){
                copySmbFolderInside(name,srcPath,destPath);
            }else{
                copySmbFileInside(name,srcPath,destPath);
            }
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void copySmbFolderInside(String name,String srcPath,String destPath){
        try {
            SmbFile srcfile = new SmbFile(srcPath + name + File.separator);
            SmbFile destFile = new SmbFile(destPath + name + File.separator);

            if(!destFile.exists()){
                destFile.mkdir();
                SmbFile[] subFile = srcfile.listFiles();
                if(subFile != null && subFile.length > 0){
                    for(int index = 0;index < subFile.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
                        if(subFile[index].isDirectory()){
                            copySmbFolderInside(subFile[index].getName(),subFile[index].getParent(),destFile.getPath());
                        }else{
                            copySmbFileInside(subFile[index].getName(),subFile[index].getParent(),destFile.getPath());
                        }
                    }
                }
            }
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void copySmbFileInside(String name,String srcPath,String destPath){
        InputStream ins = null;
        OutputStream out = null;
        SmbFile dest = null;
        long FileLen = 0;
        try {
            SmbFile src = new SmbFile(srcPath + name);
            dest = new SmbFile(destPath + name);

            int length = src.length() > 65536 ? 65536 : (int)src.length();
            if(length <= 0){
                dest.createNewFile();
                return;
            }

            if(!dest.exists()){
                dest.createNewFile();
                if(FileLen > dest.getDiskFreeSpace()){
                    ErrorMsg = NO_SPACE;
                    dest.delete();
                    return;
                }
                ins = new BufferedInputStream(new SmbFileInputStream(src));
                out = new BufferedOutputStream(new SmbFileOutputStream(dest));
                byte[] buffer = new byte[length];
                int rd = -1;
                rd = ins.read(buffer, 0, length);
                while(rd != -1 && !SambaFileUtility.getInstance(null).getIsPasteCancel()){
                    out.write(buffer, 0, rd);
                    writeDataSize += rd;
                    if(needShowDialog){
                        sendPasteProgress(writeDataSize,name);
                    }
                    rd = -1;
                    rd = ins.read(buffer, 0, length);
                    if((FileLen - writeDataSize) > dest.getDiskFreeSpace()){
                        ErrorMsg = NO_SPACE;
                        break;
                    }
                }
                out.flush();
                ins.close();
                out.close();
                if(ErrorMsg != null && ErrorMsg.equals(NO_SPACE)){
                    dest.delete();
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
            try {
                if(FileLen > dest.getDiskFreeSpace()){
                    ErrorMsg = NO_SPACE;
                }
            } catch (SmbException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        if(!TextUtils.isEmpty(ErrorMsg) || SambaFileUtility.getInstance(null).getIsPasteCancel()){
            try {
                dest.delete();
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ErrorMsg = e.getMessage();
            }
        }
    }

    private void paste(String[] namelist ,String srcPath,String destPath,int type){
        for(int index = 0;index < namelist.length && !SambaFileUtility.getInstance(null).getIsPasteCancel();index ++){
            paste(namelist[index],srcPath,destPath,type);
        }
    }

    private void delete(String[] name,String path){
        for(int index = 0;index < name.length;index ++){
            delete(name[index],path);
        }
    }

    private void delete(String name,String path){
        try {
            SmbFile file = new SmbFile(path + name);
            if(file.isDirectory()){
                file = null;
                file = new SmbFile(path + name + File.separator);
            }
            file.delete();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
            Log.d(TAG,"  ErrorMsg== " + ErrorMsg);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
            Log.d(TAG,"  ErrorMsg== " + ErrorMsg);
        }
    }

    private void renameDocument(String path,String oldName,String newName){
        try {
            SmbFile old = new SmbFile(path + oldName);
            SmbFile newFile = new SmbFile(path + newName);
            old.renameTo(newFile);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }
    }

    private void addFolder(String path,String name){
        try {
            SmbFile newFolder = new SmbFile(path + name);
            newFolder.mkdir();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ErrorMsg = e.getMessage();
        }
    }

    private void sendPasteProgress(long writeSize,String fileName){
        Bundle result = new Bundle();
        if(actionType ==  CopyTypeArgument.SAMB_TO_CLOUD){
            if(pasteType == CUT){
                writeSize = writeSize/3;
            }else{
                writeSize = writeSize/2;
            }
        }else if(actionType ==  CopyTypeArgument.CLOUD_TO_SAMB){
            if(pasteType == CUT){
                writeSize = TotalDataSize/3 + writeSize/3;
            }else{
                writeSize = TotalDataSize/2 + writeSize/2;
            }
        }
        result.putLong(PASTE_PROGRESS_SIZE, writeSize);
        result.putLong(PASTE_TOTAL_SIZE, TotalDataSize);
        result.putString(PASTE_CURRENT_FILE_NAME, fileName);
        Message msg = new Message();
        msg.what = ACTION_UPDATE;
        msg.setData(result);
        mCallBackHandler.sendMessage(msg);
    }

    private boolean isTheSameServer(SmbFile src,SmbFile dest){
        boolean result = false;
        try {
            String mSrcSub = src.getParent().substring(6);
            String mDesSub = dest.getParent().substring(6);
            int index1 = mSrcSub.indexOf(File.separator);
            int index2 = mDesSub.indexOf(File.separator);
            mSrcSub = mSrcSub.substring(index1 + 1);
            mDesSub = mDesSub.substring(index2 + 1);
            index1 = mSrcSub.indexOf(File.separator);
            index2 = mDesSub.indexOf(File.separator);
            mSrcSub = mSrcSub.substring(0, index1);
            mDesSub = mDesSub.substring(0, index2);
            if(mSrcSub.equals(mDesSub)){
                result = true;
            }
        }catch (Throwable ignore){

        }
        return result;
    }

    public static String getErrorMessage(String errorMsg){
        String error = null;
        String subString = null;
        int start;
        int end;
        start = errorMsg.indexOf(":");
        subString = errorMsg.substring(start + 1);
        end = subString.indexOf("\n");
        if(end < 0 || end < start){
            error = subString;
        }else{
            error = subString.substring(0,end);
        }
        return error;
    }

    private void  calculateMultiFileLength(String[] namelist,String path){
        for(int index = 0;index < namelist.length; index ++){
            calculateFileContentSize(namelist[index],path);
        }
    }

    private void calculateFileContentSize(String name,String path){
        if(path.startsWith(SMB)){
            try {
                SmbFile file = new SmbFile(path + name);
                if(file.isDirectory()){
                    file = null;
                    file = new SmbFile(path + name + File.separator);
                    calculateFolderContentSize(null,file);
                }else{
                    TotalDataSize += file.length();
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            };

        }else{
            File mFile = new File(path + File.separator + name);
            if(mFile.isDirectory()){
                calculateFolderContentSize(mFile,null);
            }else{
                TotalDataSize += mFile.length();
            }
        }
    }

    private void calculateFolderContentSize(File local,SmbFile smb){
        if(local != null){
            File[] subFiles = local.listFiles();
            if(subFiles != null && subFiles.length > 0){
                for(int num = 0; num < subFiles.length;num ++){
                    if(subFiles[num].isDirectory()){
                        calculateFolderContentSize(subFiles[num],null);
                    }else{
                        TotalDataSize += subFiles[num].length();
                    }

                }
            }
        }else{
            try {
                SmbFile[] Files = smb.listFiles();
                if(Files != null && Files.length > 0){
                    for(int num = 0; num < Files.length;num ++){
                        if(Files[num].isDirectory()){
                            calculateFolderContentSize(null,Files[num]);
                        }else{
                            TotalDataSize += Files[num].length();
                        }
                    }
                }
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }



}
