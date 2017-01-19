package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.receive.IReceiveFileHandler;
import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;

import java.io.FileNotFoundException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/3.
 */
public class HeadHandler extends GetHandler {

    private IReceiveFileHandler receiveFileHandler;
    private FileManager fileManager;

    public HeadHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager, IInputFile file, IReceiveFileHandler receiveFileHandler) {
        super(session, file);
        doNotSendBody();
        doNotSupportRange();
        this.fileManager = fileManager;
        this.receiveFileHandler = receiveFileHandler;
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        NanoHTTPD.Response response;
        if ((response = isCustomHeadMethod()) != null) {
            return response;
        }
        else
            return super.executeMethod();
    }

    private NanoHTTPD.Response isCustomHeadMethod() {
        final String sessionId = session.getHeaders().get(HttpHeaderField.CUSTOM_SESSION_ID.toString());
        final int fileId = session.getHeaders().get(HttpHeaderField.CUSTOM_FILE_ID.toString())== null?
                -1 :Integer.valueOf(session.getHeaders().get(HttpHeaderField.CUSTOM_FILE_ID.toString()));
        if (receiveFileHandler == null || sessionId == null || fileId == -1)
            return null;
        if (receiveFileHandler.checkFileCancelled(sessionId, fileId)) {
            NanoHTTPD.Response response =  NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
            response.addHeader(HttpHeaderField.CUSTOM_FILE_CANCELLED.toString()
                    , session.getHeaders().get(HttpHeaderField.CUSTOM_FILE_ID.toString()));
            return response;
        }
        else {
            try {
                IInputFile file = fileManager.getInputFile(getHashFilePath(
                        session.getUri().substring(HttpFileServer.URL_FILE_PREFIX.length()), sessionId, fileId));
                return handleGetFullFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, null, null);
        }
    }
}
