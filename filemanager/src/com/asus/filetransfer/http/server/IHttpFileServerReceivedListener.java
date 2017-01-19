package com.asus.filetransfer.http.server;

import java.io.InputStream;

/**
 * Created by Yenju_Lai on 2015/9/14.
 */
public interface IHttpFileServerReceivedListener {

    public void onReceiveFileList(InputStream in);

    public boolean onFileUploadStarted(String path, String actualPath);

    public boolean onProgressUpdated(String actualPath, long received);

    public void onFileUploadCancelled(String actualPath);

    public void onFileUploadFinished(String actualPath);
}
