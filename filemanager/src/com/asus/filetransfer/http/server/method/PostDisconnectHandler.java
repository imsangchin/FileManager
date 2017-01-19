package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.receive.IReceiveFileHandler;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/10/15.
 */
public class PostDisconnectHandler extends PostHandler {

    public PostDisconnectHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler handler) {
        super(session, fileManager, handler);
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        String sessionId;
        if (receiveFileHandler == null)
            return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
        if ((sessionId = parseSessionId(session.getHeaders())) == null)
            return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.BAD_REQUEST, null, null);
        receiveFileHandler.onSessionDisconnect(sessionId);
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.OK, null, null);
    }
}
