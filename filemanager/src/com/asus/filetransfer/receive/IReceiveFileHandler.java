package com.asus.filetransfer.receive;

/**
 * Created by Yenju_Lai on 2015/9/22.
 */
public interface IReceiveFileHandler {
    boolean checkFileCancelled(String sessionId, int fileId);
    boolean onFileListReceived(String sessionId, String fileList);
    boolean onReceiveProgressUpdated(String sessionId, int fileId, int received);
    void onSessionDisconnect(String sessionId);
    void stopReceiveItemAt(int index);
}
