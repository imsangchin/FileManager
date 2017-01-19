package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.receive.IReceiveFileHandler;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;
import com.asus.filetransfer.utility.StringUtils;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/4.
 */
public class PostFileListHandler extends PostHandler {

    public PostFileListHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler handler) {
        super(session, fileManager, handler);
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        String sessionId;
        if (receiveFileHandler == null)
            return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
        if ((sessionId = parseSessionId(session.getHeaders())) == null)
            return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.BAD_REQUEST, null, null);
        try {
            return receiveFileHandler.onFileListReceived(sessionId,
                    StringUtils.getFixLengthStringFromInputStream(session.getInputStream(),
                            Long.parseLong(session.getHeaders().get(HttpHeaderField.CONTENT_LENGTH.toString()))))?
                NanoHTTPD.Response.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK, HttpConstants.HTTP_MIME_TYPE_PLAINTEXT, fileManager.getStorageLocation())
                    : NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.BAD_REQUEST, null, null);
    }
}
