package com.asus.filetransfer.send;

import com.asus.filetransfer.filesystem.IInputFile;

import java.io.IOException;

/**
 * Created by Yenju_Lai on 2015/10/23.
 */
public interface IClient {
    String sendFileList(String fileListJson) throws IOException;

    void sendFile(int id, IInputFile file) throws IOException;

    long checkFileStatus(int id, IInputFile file) throws IOException;

    void stopSendFile();
}
