package com.asus.filetransfer.http.server.method;

import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/4.
 */
public class NotImplementedMethodHandler extends IHttpMethodHandler {

    public NotImplementedMethodHandler(NanoHTTPD.IHTTPSession session) {
        super(session);
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        Log.d(this.getClass().getName(), "method not implemented");
        return NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_IMPLEMENTED, null, null);
    }
}
