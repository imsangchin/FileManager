package com.asus.filetransfer.send;

/**
 * Created by Yenju_Lai on 2015/10/5.
 */
public interface ISendTaskStateListener {
    void onSendTaskStarted();
    void onSendTaskStopped();
    void onSendTaskCancelled();
    void onSendTaskCompleted();
    void onSendTaskPaused();
    void onFileCancelled(int cancelledCount);
    void onFileSendedCountUpdated(int totalSendedFileCount);
    void onSendProgressUpdated(long totalSendedLength);
}
