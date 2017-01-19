
package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.adapter.UnZipPreviewAdapter;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.UnZipPreviewData;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

public class UnZipDialogFragment extends DialogFragment implements TextWatcher, OnClickListener {

    public static final int TYPE_UNZIP_PREVIEW_DIALOG = 0;
    public static final int TYPE_UNZIP_DIALOG = 1;
    public static final int TYPE_PROGRESS_DIALOG = 2;
    public static final int TYPE_CHARACTER_ENCODING_DIALOG = 3;

    private static final boolean DEBUG = false;
    private static final String TAG = "UnZipDialogFragment";

    protected static final int MSG_UPDATE_PROGRESS = 0;
    public static final int MSG_COMPLETE = 1;
    public static final int MSG_BUILD_COMPLETE = 2;
    public static final int MSG_STOP_PROGRESS = 3;

    private static final int ZIP_TREE_ROOT = 0;
    private static final int BUFFER = 4096;
    private static final String NO_NAME = "..";
    private static final String PATH_DOWNLOAD =
            WrapEnvironment.getEpadInternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    private static final int OPEN_SUCCESS = 0;
    private static final int OPEN_FAIL = 1;

    private UnZipDialogFragmentListener mListener;

    public interface UnZipDialogFragmentListener {
        public void onUnZipSuccess(VFile extractedFile);
    }

    public static class UnZipData implements Serializable {
        public VFile zipFile;
        public String unZipName;
        public long unZipSize;
        public String charEncode;
        public String uriString;

        public UnZipData(VFile file, String name, long size, String encode, String uString) {
            zipFile = file;
            unZipName = name;
            unZipSize = size;
            charEncode = encode;
            uriString = uString;
        }
    }

    private class ProgressInfo {
        public int progress;
        public double writeSize;
    }

    private int mType;
    private VFile mZipFile;
    private String mZipName;
    private LocalVFile mCacheFile;
    private String mUnZipName;
    private String mEntryName;
    private String mCacheFileName;
    private String mCharEncode;
    private String mUriString;
    private int mEncodeNumber;
    private int mUnZipCount = 0;
    private long mUnZipSize = 0;

    private View mUnZipInfoView;
    private TextView mPathTextView;
    private TextView mSizeTextView;
    private ImageView mBackButton;
    private ProgressBar mProgressBar;
    private ListView mListView;

    private View mUnZipView;
    private TextView editToast;

    private boolean cancel = false;
    private boolean isFirst = true;
    private boolean isFinish = false;
    private boolean isLoading = false;

    private UnZipPreviewRunnable mPreviewRunnable;
    private SinglePreviewRunnable mSingleRunnable;
    private UnZipRunnable mRunnable;
    private Thread mThread;
    private int mProgressValue = 0;

    private UnZipPreviewAdapter mAdapter;
    private List<UnZipPreviewData> mZipTree;

    private String[] mCharsets;
    private String mPathExternalCache;

    private class UnZipRunnable implements Runnable {

        private boolean isStop = false;

        private double mWriteSize = 0;

        @Override
        public void run() {
            Log.d(TAG, " Extract zipFile : " + mZipFile.getAbsolutePath() + " to " + mUnZipName);

            EditResult result = new EditResult();

            // If zip file is temporarily saved in the cache storage,
            // the unzipped file will be created in the folder "download".
            LocalVFile unZipDir;
            if (mPathExternalCache == null) {
                unZipDir = new LocalVFile(mZipFile.getParentFile(), mUnZipName);
            } else if (mZipFile.getParent().equals(mPathExternalCache)) {
                unZipDir = new LocalVFile(PATH_DOWNLOAD, mUnZipName);
            } else {
                unZipDir = new LocalVFile(mZipFile.getParentFile(), mUnZipName);
            }

            if (isStop) {
                result.ECODE = EditResult.E_FAILURE;
            }

            if (unZipDir.exists()) {
                result.ECODE = EditResult.E_EXIST;
            } else {
                Log.i(TAG, "start decompressing ...");

                result = unZip(unZipDir, mZipFile);

                Log.i(TAG, "finish compress ...");

                if (result.ECODE == EditResult.E_FAILURE) {
                    LocalVFile[] deleteFile = {
                            unZipDir
                    };
                    EditorUtility.doDeleteFiles(deleteFile, false);
                }
            }

            if (result.ECODE == EditResult.E_SUCCESS) {
                MediaProviderAsyncHelper.addFolder(unZipDir, true);
            }

            Message msg = mHandler.obtainMessage(MSG_COMPLETE);
            msg.arg1 = result.ECODE;
            msg.obj =  unZipDir;
            mHandler.sendMessage(msg);
            isFinish = true;
            Log.i(TAG, "unzip thread is terminated");

        }

        public EditResult unZip(VFile directory, VFile zipFile) {

            EditResult result = new EditResult();
            result.ECODE = EditResult.E_SUCCESS;

            try {
                ZipFile zipfile = new ZipFile(zipFile,mCharEncode);
//                ZipFile zipfile = new ZipFile(zipFile);
                if(SafOperationUtility.getInstance().isNeedToWriteSdBySaf(directory.getAbsolutePath())){
                	DocumentFile parentFile = SafOperationUtility.getInstance().getDocFileFromPath(directory.getParent());
                    if(parentFile != null){
                    	parentFile.createDirectory(directory.getName());
                    }
                }else{
                    directory.mkdirs();
                }
                fileUnZip(zipfile, directory);
                zipfile.close();
            } catch (Exception e) {
            	if(e.getMessage() != null && e.getMessage().equals(EditResult.Error.ENOSPC)){
//                if (e.getMessage().equals(EditResult.Error.ENOSPC)) {
                    Log.w(TAG, "no space");
                    result.ECODE = EditResult.E_NOSPC;
                } else {
                    e.printStackTrace();
                    result.ECODE = EditResult.E_FAILURE;
                }
            }

            return result;
        }

        private void fileUnZip(ZipFile zipfile, File file) throws Exception {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry = null;
            ProgressInfo progressInfo = new ProgressInfo();

            Enumeration<? extends ZipEntry> e = zipfile.getEntries();
//            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while (e.hasMoreElements() && !isStop) {
                entry = (ZipEntry) e.nextElement();

                if (isStop) {
                    throw new IOException("cancel thread");
                }

//                String name = entry.getNameByCharset(Charset.forName(mCharEncode));
                String name = entry.getName();
                File f = new File(file.getAbsolutePath() + "/" + name);
                Log.d(TAG, "file path : " + file.getAbsolutePath() + "/" + name);
                boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(file.getAbsolutePath());
                if (entry.isDirectory()) {
                	if(useSaf){
                		if (!f.getParentFile().exists()) {
                			SafOperationUtility.getInstance().createNotExistFolder(f);
                		}else{
                		    if(!f.exists() || f.isFile()){
                		        DocumentFile parentEntry = SafOperationUtility.getInstance().getDocFileFromPath(f.getParent());
                		        if(parentEntry != null){
                	    	        parentEntry.createDirectory(f.getName());
                	            }
                		    }
                		}
                	}else{
                        f.mkdirs();
                	}
                } else {
                	
                    if (!f.getParentFile().exists()) {
                    	
                     	if(useSaf){
                     		SafOperationUtility.getInstance().createNotExistFolder(f);         
                     	}else{
                            f.getParentFile().mkdirs();
                    	}
                    }

                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                   
                    if(useSaf){
                    	DocumentFile pFile = SafOperationUtility.getInstance().getDocFileFromPath(f.getParent());
                    	DocumentFile destFile = null;
                    	if(pFile != null){
                    		destFile = pFile.createFile("*/*", f.getName());
                    	}
                    	dest = SafOperationUtility.getInstance().getDocFileOutputStream(destFile);
                    }else{
                        dest = new BufferedOutputStream(new FileOutputStream(f), BUFFER);
                    }
                    int count;
                    byte data[] = new byte[BUFFER];

                    try {
                        while ((count = is.read(data, 0, BUFFER)) != -1 && !isStop) {
                            dest.write(data, 0, count);
                            mWriteSize = mWriteSize + count;
                            Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                            progressInfo.progress = (int) ((mWriteSize / mUnZipSize) * 100);
                            progressInfo.writeSize = mWriteSize;
                            msg.obj = progressInfo;
                            mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                            mHandler.sendMessage(msg);
                        }
                    } finally {
                        dest.flush();
                        dest.close();
                        is.close();
                        if(useSaf){
                        	SafOperationUtility.getInstance().closeParcelFile();
                        }
                    }
                }
            }
            
            if (isStop) {
                throw new IOException("cancel thread");
            }
        }

        public void terminate() {
            isStop = true;
        }

    }

    private class UnZipPreviewRunnable implements Runnable {

        private boolean isStop = false;
        private int errorMessage = OPEN_SUCCESS;

        @Override
        public void run() {
            Log.d(TAG, "previewing zip file : " + mZipName);
            try {
                if (mZipFile == null) {
                    writeFile();
                }
                getUnZipData(mZipFile);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = OPEN_FAIL;
            }

            Message msg = mHandler.obtainMessage(MSG_BUILD_COMPLETE);
            msg.arg1 = errorMessage;
            mHandler.sendMessage(msg);
            isFinish = true;
            Log.i(TAG, "preview thread is terminated");
        }

        private void writeFile() throws Exception {

            if (mPathExternalCache != null) {
                if (!mUnZipName.equals("")) {
                    mZipFile = new LocalVFile(mPathExternalCache, mUnZipName + ".zip");
                } else {
                    String temp = String.valueOf(System.currentTimeMillis());
                    mZipFile = new LocalVFile(mPathExternalCache, temp);
                }
                mZipFile.createNewFile();
                LocalVFile nomedia = new LocalVFile(mPathExternalCache, ".nomedia");
                if (!nomedia.exists())
                    nomedia.createNewFile();
            } else {
                 throw new Exception("external cache storage is not available");
            }

            ParcelFileDescriptor pfd = null;
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                Uri uri = Uri.parse(mUriString);
                pfd = getActivity().getContentResolver().openFileDescriptor(uri, "r");
                bis = new BufferedInputStream(new FileInputStream(pfd.getFileDescriptor()));
                bos = new BufferedOutputStream(new FileOutputStream(mZipFile), BUFFER);
                int count;
                byte data[] = new byte[BUFFER];
                while ((count = bis.read(data, 0, BUFFER)) != -1) {
                    if (isStop) {
                        throw new IOException("cancel thread");
                    }
                    bos.write(data, 0, count);
                }
                bos.flush();
            } finally {
                pfd.close();
                bis.close();
                bos.close();
            }
        }

        private void getUnZipData(VFile zipFile) throws Exception {

            mZipTree.add(new UnZipPreviewData("/", ZIP_TREE_ROOT));
            mZipTree.get(ZIP_TREE_ROOT).setName(zipFile.getNameNoExtension());
            mZipTree.get(ZIP_TREE_ROOT).setUTF8Name(zipFile.getNameNoExtension());

            ZipEntry entry;
            ZipFile zipfile = new ZipFile(zipFile,mCharEncode);
//            ZipFile zipfile = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> e = zipfile.getEntries();
//            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while (e.hasMoreElements()) {
                if (isStop) {
                    throw new IOException("cancel thread");
                }

                entry = (ZipEntry) e.nextElement();

                boolean match = false;
	        	for (int i = 0; i < mZipTree.size(); i++) {
                    if (entry.getName().equals(mZipTree.get(i).getPath())) {
                    	match = true;
                    }
	        	}
	        	if (!match) {
                UnZipPreviewData newData = new UnZipPreviewData(
                        entry.getName(),
                        mZipTree.size());

                newData.setInfo(
                        entry.getCompressedSize(),
                        entry.getSize(),
                        entry.getTime(),
                        entry.getCrc());
                newData.setUTF8Path(entry.getName());

                mZipTree.add(newData);

                constructDataStructure(newData);

                if (entry.getSize() != -1)
                    mUnZipSize += entry.getSize();

                if (!newData.isFolder()){

                    mUnZipCount++;
                }
	        	}
            }
        }

        private void constructDataStructure(UnZipPreviewData data) {

            String noSuffixSlash = data.getPathNoSuffixSlash();

            //name includes parent folder, ex. /aaa/bbb
            if (noSuffixSlash.lastIndexOf("/") > 0) {
                String parentPath = data.getParentPath();
                boolean match = false;
                for (int i = 0; i < mZipTree.size(); i++) {
                    if (parentPath.equals(mZipTree.get(i).getPath())) {
                        data.setParent(mZipTree.get(i));
                        data.getParent().addChild(data);
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    while (parentPath != null) {
                        UnZipPreviewData newData = new UnZipPreviewData(parentPath, mZipTree.size());
                        mZipTree.add(newData);

                        data.setParent(newData);
                        data.getParent().addChild(data);

                        parentPath = newData.getParentPath();
                        if (parentPath == null) {
                            newData.setParent(mZipTree.get(ZIP_TREE_ROOT));
                            newData.getParent().addChild(newData);
                        } else {
                            for(UnZipPreviewData d : mZipTree) {
                                if (parentPath.equals(d.getPath())) {
                                    newData.setParent(d);
                                    newData.getParent().addChild(newData);
                                    parentPath = null;
                                    break;
                                }
                            }
                            data = newData;
                        }
                    }
                }
            }
            //under root folder, ex. /ccc
            else {
                if (data.getId() != ZIP_TREE_ROOT) {
                	int sameFileCount = 0;
                    for(UnZipPreviewData subF : mZipTree){
                    	if(data.getPath().equals(subF.getPath())){
                    		sameFileCount ++;
                    	}
                    }
                    if(sameFileCount < 2){
                        data.setParent(mZipTree.get(ZIP_TREE_ROOT));
                        data.getParent().addChild(data);
                    }
                }
            }
        }

        public void terminate() {
            isStop = true;
        }
    }

    private class SinglePreviewRunnable implements Runnable {

        private boolean isStop = false;

        @Override
        public void run() {
            isLoading = true;
            singleUnzip(mZipFile);
            Log.i(TAG, "loading thread is terminated");
        }

        private void singleUnzip(VFile zipFile) {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            try {
                ZipFile zipfile = new ZipFile(zipFile,mCharEncode);
                ZipEntry entry = zipfile.getEntry(mEntryName);

                if (mPathExternalCache != null) {
                    mCacheFile = new LocalVFile(mPathExternalCache + "/.pfile", mCacheFileName);
                    if (!mCacheFile.getParentFile().exists()) {
                        mCacheFile.getParentFile().mkdirs();
                        new LocalVFile(mPathExternalCache, ".nomedia").createNewFile();
                    }

                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    dest = new BufferedOutputStream(new FileOutputStream(mCacheFile), BUFFER);

                    int count;
                    byte data[] = new byte[BUFFER];
                    while ((count = is.read(data, 0, BUFFER)) != -1) {
                        if (isStop) {
                            throw new IOException("cancel thread");
                        }
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                } else {
                    throw new IOException("external cache storage is not available");
                }
                zipfile.close();
                dest.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Message msg = mHandler.obtainMessage(MSG_STOP_PROGRESS);
                mHandler.sendMessage(msg);
            }
        }

        public void terminate() {
            isStop = true;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BUILD_COMPLETE:
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mProgressBar.clearAnimation();
                    if (msg.arg1 == OPEN_FAIL) {
                    	  mBackButton.setEnabled(false);
                        ToastUtility.show(FileManagerApplication.getAppContext(), R.string.open_fail, Toast.LENGTH_LONG);
                    } else {
                        if (getDialog() != null) {
                            mAdapter.updateAdapter(mZipTree.get(ZIP_TREE_ROOT));
                            updateDialog(mZipTree.get(ZIP_TREE_ROOT));
                            mSizeTextView.setText(getResources().getQuantityString(R.plurals.dialog_unzip_preview_size_text, mUnZipCount, mUnZipCount, Formatter.formatFileSize(
                                    getActivity().getApplicationContext(), mUnZipSize)));
                            mListView.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case MSG_UPDATE_PROGRESS:
                    ProgressDialog dialog = (ProgressDialog) getDialog();
                    if (getDialog() != null) {
                        ProgressInfo progressInfo = (ProgressInfo) msg.obj;
                        dialog.setProgressNumberFormat(FileUtility.bytes2String(getActivity().getApplicationContext(), progressInfo.writeSize, 2) + " / " + FileUtility.bytes2String(getActivity().getApplicationContext(), mUnZipSize, 2));
                        dialog.setProgress(progressInfo.progress);
                    }
                    break;
                case MSG_COMPLETE:
                    int result = msg.arg1;
                    showUnZipFileResult(FileManagerApplication.getAppContext(), result);
                    onCancel(getDialog());
                    deleteTempFile();
                    // Fix TT-805889 avoid app crash when activity is destroyed.
                    if (getActivity() != null) {
                        dismissAllowingStateLoss();
                    }
                    if ((result == EditResult.E_SUCCESS) && (mListener != null)
                            && (msg.obj instanceof VFile)) {
                        mListener.onUnZipSuccess(
                                (VFile) msg.obj);
                    }
                    break;
                case MSG_STOP_PROGRESS:
                    if (getDialog() != null) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        FileUtility.openFile(getActivity(), mCacheFile, false, false);
                        isLoading = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public static UnZipDialogFragment newInstance(UnZipData arg, int typeDialog) {
        UnZipDialogFragment fragment = new UnZipDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("unzipdata", arg);
        args.putInt("type", typeDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");

        mType = getArguments().getInt("type");

        mZipFile = ((UnZipData) getArguments().getSerializable("unzipdata")).zipFile;
        mUnZipName = ((UnZipData) getArguments().getSerializable("unzipdata")).unZipName;
        mUnZipSize = ((UnZipData) getArguments().getSerializable("unzipdata")).unZipSize;
        mCharEncode = ((UnZipData) getArguments().getSerializable("unzipdata")).charEncode;
        mUriString = ((UnZipData) getArguments().getSerializable("unzipdata")).uriString;

        // We get the zip file name from file, but sometimes the file is not already created.
        // Therefore we give ".." instead of the real name since we can't get the name of this zip file.
        // The name of zip file which comes from email attachment is a special case which
        // we handled it in particular.
        if (mZipFile == null && !mUnZipName.equals("")) {
            mZipName = mUnZipName + ".zip";
        } else if (mZipFile != null){
            mZipName = mZipFile.getName();
        } else {
            mZipName = NO_NAME;
        }

        if (mType == TYPE_UNZIP_PREVIEW_DIALOG) {
            mZipTree = new ArrayList<UnZipPreviewData>();
            mAdapter = new UnZipPreviewAdapter(this);
            setRetainInstance(true);
        } else if (mType == TYPE_PROGRESS_DIALOG) {
            if (DEBUG)
                Log.d(TAG, "set RetainInstance");
            setRetainInstance(true);
        } else if (mType == TYPE_CHARACTER_ENCODING_DIALOG) {
            mCharsets = getResources().getStringArray(R.array.encoding_charsets);
            int i = 0;
            for(String charset: mCharsets) {
                if (mCharEncode.equals(charset)) {
                    mEncodeNumber = i;
                }
                i++;
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
        Log.d(TAG, "onActivityCreated");

        if (FileUtility.isExternalStorageAvailable() && getActivity().getExternalCacheDir() != null) {
            mPathExternalCache = getActivity().getExternalCacheDir().getAbsolutePath();
        }

        if (mType == TYPE_UNZIP_PREVIEW_DIALOG) {
            // The progress bar is set to be invisible after the threads were terminated.
            if (!isFirst && !isLoading) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mProgressBar.clearAnimation();
            }

            if (isFirst) {
                mPreviewRunnable = new UnZipPreviewRunnable();
                mThread = new Thread(mPreviewRunnable);
                mThread.start();
                isFirst = false;
            } else {
            //keep information of dialog unchanged after configuration changed
                updateDialog(mAdapter.getCurrentFolder());
                try {
                    mSizeTextView.setText(getResources().getQuantityString(R.plurals.dialog_unzip_preview_size_text, mUnZipCount, mUnZipCount, Formatter.formatFileSize(
                            getActivity().getApplicationContext(), mUnZipSize)));
                } catch (Exception e) {
                    e.getStackTrace();
                }
                mListView.setVisibility(View.VISIBLE);
            }
        }

        if (mType == TYPE_PROGRESS_DIALOG) {
            Window w = getDialog().getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            if (isFirst) {
                mRunnable = new UnZipRunnable();
                mThread = new Thread(mRunnable);
                mThread.start();
                isFirst = false;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, mType + " onResume");

        if (mType == TYPE_PROGRESS_DIALOG) {
            if (isFinish) {
                Log.d(TAG, "isFinish");
                onCancel(getDialog());
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG)
            Log.d(TAG, mType + " onCancel");

        cancel = true;
        if (mRunnable != null)
            mRunnable.terminate();

        if (mPreviewRunnable != null)
            mPreviewRunnable.terminate();

        if (mSingleRunnable != null)
            mSingleRunnable.terminate();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (DEBUG)
            Log.d(TAG, mType + " onDismiss");
        if (getDialog() != null && getRetainInstance() && !cancel) {
            return;
        }

        super.onDismiss(dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (DEBUG)
            Log.d(TAG, "onCreateDialog");

        int type = getArguments().getInt("type");

        Dialog dialog = null;

        if (type == TYPE_UNZIP_PREVIEW_DIALOG) {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mUnZipInfoView = inflater.inflate(R.layout.dialog_unzip_preview, null);

            mBackButton = (ImageView) mUnZipInfoView.findViewById(R.id.unzip_preview_back_button);
            mPathTextView = (TextView) mUnZipInfoView.findViewById(R.id.unzip_preview_current_folder_path);
            mSizeTextView = (TextView) mUnZipInfoView.findViewById(R.id.unzip_preview_total_size_text);
            mProgressBar = (ProgressBar) mUnZipInfoView.findViewById(R.id.unzip_preview_progress);
            mListView = (ListView) mUnZipInfoView.findViewById(R.id.unzip_preview_file_list_item);

            mBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFinish && !isLoading) {
                        UnZipPreviewData curFolder = mAdapter.getCurrentFolder();
                        if(curFolder != null){
                            if (curFolder.getParent() != null) {
                                updateDialog(curFolder.getParent());
                                mAdapter.updateAdapter(curFolder.getParent());
                            }
                        }
                    }
                }
            });

            mListView.setAdapter(mAdapter);
            mListView.setItemsCanFocus(true);

            String title;
            if (mZipName.equals(NO_NAME)) {
                title = getString(R.string.dialog_unzip_preview_title, "zip file");
            } else {
                title = getString(R.string.dialog_unzip_preview_title, mZipName);
            }

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(title)
                    .setView(mUnZipInfoView)
                    .setPositiveButton(R.string.extract, this)
                    .setNeutralButton(R.string.encode, this)
                    .setNegativeButton(R.string.cancel, this)
                    .create();
        } else if (type == TYPE_UNZIP_DIALOG) {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mUnZipView = inflater.inflate(R.layout.dialog_unzip, null);

            EditText editText = (EditText) mUnZipView.findViewById(R.id.edit_name);
            editText.setText(mUnZipName);
            editText.selectAll();
            //editText.setSelection(mUnZipName.length());
            editText.addTextChangedListener(this);

            editToast = (TextView) mUnZipView.findViewById(R.id.edit_toast);

            String title;
            if (mZipName.equals(NO_NAME)) {
                title = getString(R.string.dialog_extract_zip_title, "zip file");
            } else {
                title = getString(R.string.dialog_extract_zip_title, mZipName);
            }

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();

            ((AlertDialog)dialog).setView(mUnZipView, spacing_left, spacing_top, spacing_right, spacing_bottom);
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        } else if (type == TYPE_PROGRESS_DIALOG) {
            dialog = new ProgressDialog(getActivity(), ThemeUtility.getAsusAlertDialogThemeId());
            ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.unzip_progress_msg));
            ((ProgressDialog) dialog).setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            ((ProgressDialog) dialog).setMax(100);
            ((ProgressDialog) dialog).setProgress(mProgressValue);
            ((ProgressDialog) dialog).setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), this);
            setCancelable(false);
        } else if (type == TYPE_CHARACTER_ENCODING_DIALOG) {
            String title = getString(R.string.dialog_text_encoding_title);

            dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .setTitle(title)
                    .setSingleChoiceItems(R.array.charset_array, mEncodeNumber, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            mEncodeNumber = item;
                            mCharEncode = mCharsets[mEncodeNumber];
                            getDialog().cancel();
                            UnZipData unZipData = new UnZipData(mZipFile, mUnZipName, 0, mCharEncode, mUriString);
                            ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
                            System.setProperty("prop.unzip.encode",mCharEncode);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            setCancelable(false);
        }

        return dialog;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (getDialog() != null) {
        	String text = s.toString().trim();
        	boolean isSpecialChar = EditorUtility.isSpecialChar(text);
        	boolean isNameTooLong = EditorUtility.isNameTooLong(text);

        	s.setFilters(isNameTooLong ? new InputFilter[]{new InputFilter.LengthFilter(s.length())} : new InputFilter[]{});

            Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(!isSpecialChar && !isNameTooLong);

            if(!text.isEmpty() && isSpecialChar){
            	editToast.setText(getResources().getString(R.string.edit_toast_special_char));
            	editToast.setVisibility(View.VISIBLE);
            }
            else if(isNameTooLong){
            	editToast.setText(getResources().getString(R.string.edit_toast_name_too_long));
            	editToast.setVisibility(View.VISIBLE);
            }
            else{
            	editToast.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        UnZipData unZipData = null;

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                if (mType == TYPE_UNZIP_PREVIEW_DIALOG) {
                    getDialog().cancel();
                    unZipData = new UnZipData(mZipFile, mUnZipName, mUnZipSize, mCharEncode, mUriString);
                    ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_UNZIP_DIALOG, unZipData);
                } else {
                    EditText editText = (EditText) mUnZipView.findViewById(R.id.edit_name);

                    unZipData = new UnZipData(mZipFile, editText.getText().toString().trim(), mUnZipSize, mCharEncode, mUriString);
                    ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_UNZIP_PROGRESS_DIALOG, unZipData);
                }

                break;
            case Dialog.BUTTON_NEUTRAL:
                getDialog().cancel();
                unZipData = new UnZipData(mZipFile, mUnZipName, mUnZipSize, mCharEncode, mUriString);
                ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_CHARACTER_ENCODING_DIALOG, unZipData);
                break;
            case Dialog.BUTTON_NEGATIVE:
                dialog.cancel();
                if (mType == TYPE_CHARACTER_ENCODING_DIALOG) {
                    unZipData = new UnZipData(mZipFile, mUnZipName, 0, mCharEncode, mUriString);
                    ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
                } else if (mType != TYPE_PROGRESS_DIALOG) {
                    // The temp file always be deleted when thread is terminated on progress dialog
                    deleteTempFile();
                }
                break;
            default:
                break;
        }
    }

    public static void showUnZipFileResult(Context context, int result) {

        switch (result) {
            case EditResult.E_EXIST:
                ToastUtility.show(context, R.string.target_exist, Toast.LENGTH_LONG);
                break;
            case EditResult.E_FAILURE:
                ToastUtility.show(context, R.string.unzip_fail, Toast.LENGTH_LONG);
                break;
            case EditResult.E_PERMISSION:
                ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
                break;
            case EditResult.E_SUCCESS:
                ToastUtility.show(context, R.string.unzip_success, Toast.LENGTH_LONG);
                break;
            case EditResult.E_NOSPC:
                ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
                break;
        }
    }

    public void updateDialog(UnZipPreviewData curFolder) {
        if (curFolder.getId() != ZIP_TREE_ROOT) {
            mPathTextView.setText(mZipName + "/" + curFolder.getPath());
            mBackButton.setEnabled(true);
        } else {
            mPathTextView.setText(mZipName + "/");
            mBackButton.setEnabled(false);
        }
    }

    public void singlePreview(UnZipPreviewData previewData) {

        mEntryName = previewData.getUTF8Path();
        mCacheFileName = previewData.getName();
        mCacheFile = null;

        mProgressBar.setVisibility(View.VISIBLE);
        mSingleRunnable = new SinglePreviewRunnable();
        mThread = new Thread(mSingleRunnable);
        mThread.start();

    }

    public boolean getLoadingState() {
        return isLoading;
    }

    private void deleteTempFile() {
        if (mZipFile != null && mPathExternalCache != null) {
            if (mZipFile.getParent().equals(mPathExternalCache)) {
                if (DEBUG)
                    Log.i(TAG, "delete " + mZipFile.getName());
                mZipFile.delete();
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof UnZipDialogFragmentListener) {
            mListener = (UnZipDialogFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
