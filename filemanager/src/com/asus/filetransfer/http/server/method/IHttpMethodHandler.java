package com.asus.filetransfer.http.server.method;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/1.
 */
public abstract class IHttpMethodHandler {

    NanoHTTPD.IHTTPSession session = null;
    public IHttpMethodHandler(NanoHTTPD.IHTTPSession session) {
        this.session = session;
    }

    public abstract NanoHTTPD.Response executeMethod();

    protected String getHashFilePath(String originPath, String sessionId, Integer fileId) {
        return originPath + fileId + sessionId.substring(0, 8) + ".tmp";
    }
}
