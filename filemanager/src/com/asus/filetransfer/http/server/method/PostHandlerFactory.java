package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.receive.IReceiveFileHandler;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/10/15.
 */
public class PostHandlerFactory {
    public static IHttpMethodHandler createCommand(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler receiveFileHandler) {
        if (session.getUri().compareTo(HttpFileServer.URL_COMMAND_FILELIST) == 0)
            return new PostFileListHandler(session, fileManager, receiveFileHandler);
        else if (session.getUri().compareTo(HttpFileServer.URL_COMMAND_DISCONNECT) == 0)
            return new PostDisconnectHandler(session, fileManager, receiveFileHandler);
        else if (session.getUri().startsWith(HttpFileServer.URL_COMPRESS_PREFIX))
            return new PostCompressHandler(session, fileManager);
//        else if (session.getHeader(HttpConstants.HttpHeaderField.CUSTOM_SESSION_ID.toString()) == null)
//            return new PostFileHandler(session, fileManager, null);
        return new PostHandler(session, fileManager, receiveFileHandler);
    }
}
