package com.asus.filemanager.adapter;

import java.io.File;
import java.util.Stack;

import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FileUtility.FileInfo;
import com.asus.filemanager.utility.VFile;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ItemInfoView {

    private Thread mWorkerThread = null;
    private LoadInfoRunnable mRunnable = null;
    private WaitingStack mWaitingStack;
    private Context mContext;
    final Object mLock = new Object();

    private View mInfoView;

    private static final String TAG = "ItemInfoView";

    private class Info {
        public VFile file;
        public FileInfo fileInfo;
        public InfoCallback callback;
    }

    private class WaitingStack {
        private Stack<Info> mStackInfo = new Stack<Info>();
        private Stack<String> mStackPath = new Stack<String>();

        public void push(Info info) {
            mStackPath.push(info.file.getAbsolutePath());
            mStackInfo.push(info);
        }

        public Info pop() {
            mStackPath.pop();
            return mStackInfo.pop();
        }

        public Info remove(int idx) {
            mStackPath.remove(idx);
            return mStackInfo.remove(idx);
        }

        public int indexOf(Info info) {
            return mStackPath.indexOf(info.file.getAbsolutePath());
        }

        public boolean empty() {
            return mStackInfo.empty();
        }

        public void clear() {
            mStackInfo.clear();
            mStackPath.clear();
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_INFO:
                    Info info = (Info) msg.obj;
                    if (info.callback != null) {
                        info.callback.onGetInfo(info.fileInfo);
                    }
                    break;
            }
        }
    };


    public ItemInfoView() {
        mWaitingStack = new WaitingStack();
        mRunnable = new LoadInfoRunnable(mWaitingStack);
    }

    private class LoadInfoRunnable implements Runnable {
        private WaitingStack mStack;
        private boolean mSkip = false;

        public LoadInfoRunnable(WaitingStack stack) {
            mStack = stack;
        }

        public void skipLocked() {
            mSkip = true;
        }

        private FileInfo calculateSize(File file, FileInfo fileInfo) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    synchronized (mLock) {
                        if (mSkip) {
                            Log.e(TAG, "calculateSize skip");
                            break;
                        }
                    }
                    if (!files[i].isDirectory()) {
                        fileInfo.numSize = fileInfo.numSize + files[i].length();
                        fileInfo.numFiles++;
                    } else {
                        fileInfo.numSize = calculateSize(files[i], fileInfo).numSize;
                    }
                }
            }

            return fileInfo;
        }

        @Override
        public void run() {
            Info info;
            while(true) {
                synchronized (mLock) {
                    if (!mStack.empty()) {
                        info = mStack.pop();
                    } else {
                        Log.e(TAG, "exit thread");
                        mWorkerThread = null;
                        break;
                    }
                    mSkip = false;
                }
                info.fileInfo= calculateSize(info.file, new FileInfo());
                synchronized (mLock) {
                    if (!mSkip) {
                        mHandler.removeMessages(UPDATE_INFO);
                        mHandler.sendMessage(mHandler.obtainMessage(UPDATE_INFO, info));
                    }
                }

            }
        }
    }

    public void setInfoView(VFile f, boolean skip, InfoCallback callback) {
        synchronized (mLock) {
            if (skip) {
                mWaitingStack.clear();
                mRunnable.skipLocked();
            }
            Info info = new Info();
            info.file = f;
            info.callback = callback;
            if (!f.isDirectory()) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.numFiles = 0;
                fileInfo.numSize = f.length();
                info.fileInfo = fileInfo;
                mHandler.removeMessages(UPDATE_INFO);
                mHandler.sendMessage(mHandler.obtainMessage(UPDATE_INFO, info));
            } else {
                mWaitingStack.push(info);
                startLoadLock();
            }
        }
    }

    private void startLoadLock() {
        if (mWorkerThread == null) {
            mWorkerThread = new Thread(mRunnable);
            mWorkerThread.setName("Info-Thread");
            mWorkerThread.setPriority(Thread.MIN_PRIORITY);
            mWorkerThread.start();
        }
    }

    static final private int UPDATE_INFO = 0;
}
