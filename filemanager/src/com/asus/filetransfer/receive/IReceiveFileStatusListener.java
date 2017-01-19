package com.asus.filetransfer.receive;

/**
 * Created by Yenju_Lai on 2015/9/25.
 */
public interface IReceiveFileStatusListener {
    int onFileReceived(String fileName, boolean isDirectory, long totalLength);
    void onFileProgressUpdated(int index, int received);
    void onFileCancalled(int index);
}
