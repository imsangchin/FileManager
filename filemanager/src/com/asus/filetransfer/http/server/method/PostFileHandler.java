package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.filesystem.IOutputFile;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.receive.IReceiveFileHandler;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/4.
 */
public class PostFileHandler extends PostHandler {

    public PostFileHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler handler) {
        super(session, fileManager, handler);
        this.fileManager = fileManager;
    }

}
